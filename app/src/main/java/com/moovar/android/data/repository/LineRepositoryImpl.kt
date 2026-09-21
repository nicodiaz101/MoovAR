package com.moovar.android.data.repository

import android.util.Log
import com.moovar.android.core.common.Result
import com.moovar.android.core.database.DatabaseSeeder
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.entity.LineEntity
import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.model.Line
import com.moovar.android.core.domain.model.LineStatus
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.repository.LineRepository
import com.moovar.android.core.network.sofse.api.SofseApiService
import com.moovar.android.core.network.sofse.dto.RamalDto
import com.moovar.android.core.network.sofse.dto.SofseAlertaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LineRepositoryImpl @Inject constructor(
    private val lineDao: LineDao,
    private val branchDao: BranchDao,
    private val sofseApiService: SofseApiService,
    private val databaseSeeder: DatabaseSeeder
) : LineRepository {

    companion object {
        private const val TAG = "LineRepositoryImpl"
        private val ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")

        private val GERENCIA_TO_LINE = mapOf(
            11 to "roca",
            1 to "sarmiento",
            5 to "mitre",
            31 to "san_martin",
            21 to "belgrano_sur",
            41 to "tren_costa"
        )
        private val HIDDEN_LINE_IDS = setOf("belgrano_norte", "urquiza")
    }

    var clock: java.time.Clock = java.time.Clock.system(ARGENTINA_ZONE)

    private enum class AlertCategory {
        INTERRUPTED,
        LIMITED_ROUTE,
        CANCELLED,
        DELAY,
        FUTURE_SCHEDULED,
        INFO_WORKS,
        IGNORED
    }

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun observeLines(): Flow<Result<List<Line>>> = flow {
        repositoryScope.launch {
            try {
                refreshLines()
            } catch (e: Exception) {
                Log.w(TAG, "Background line refresh failed: ${e.message}")
            }
        }

        emit(Result.Loading)
        lineDao.observeAll().collect { entities ->
            if (entities.isEmpty()) {
                databaseSeeder.seedInitialData()
            }
            val lines = entities
                .filterNot { it.id in HIDDEN_LINE_IDS }
                .map { entity ->
                    val branches = branchDao.getByLine(entity.id).map { b ->
                        Branch(
                            id = b.id,
                            lineId = b.lineId,
                            name = b.name,
                            originTerminus = b.originTerminus,
                            destinationTerminus = b.destinationTerminus
                        )
                    }
                    Line(
                        id = entity.id,
                        name = entity.name,
                        networkType = if (entity.networkType == com.moovar.android.core.database.entity.NetworkType.SUBTE) NetworkType.SUBTE else NetworkType.TREN,
                        colorHex = entity.colorHex,
                        status = mapStatus(entity.status),
                        statusMessage = entity.statusMessage,
                        branches = branches
                    )
                }
            emit(Result.Success(lines))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun refreshLines() = withContext(Dispatchers.IO) {
        try {
            val gerencias = try {
                sofseApiService.getGerencias()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get SOFSE gerencias: ${e.message}")
                emptyList()
            }

            val gerenciaMap = gerencias.associateBy { it.id }

            // Get existing line entities to preserve names, colors, and order
            val existing = lineDao.getAll()
            if (existing.isEmpty()) return@withContext

            val updated = existing.map { entity ->
                if (entity.id in HIDDEN_LINE_IDS) {
                    return@map entity
                }

                val gerenciaId = GERENCIA_TO_LINE.entries.firstOrNull { it.value == entity.id }?.key
                if (gerenciaId != null) {
                    val gerencia = gerenciaMap[gerenciaId]
                    if (gerencia != null) {
                        val ramales = try {
                            sofseApiService.getRamales(idGerencia = gerencia.id)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to get ramales for gerencia $gerenciaId: ${e.message}")
                            emptyList()
                        }

                        val lineAlerts = gerencia.alerta.filterNot { isCudAlert(it.contenido) }
                        val (newStatus, newMsg) = evaluateLine(ramales, lineAlerts)

                        entity.copy(
                            status = newStatus,
                            statusMessage = newMsg,
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    } else {
                        entity
                    }
                } else if (entity.networkType == com.moovar.android.core.database.entity.NetworkType.SUBTE) {
                    val (subteStatus, subteMsg) = when (entity.id) {
                        "linea_b", "linea_c", "linea_d" -> Pair(
                            com.moovar.android.core.database.entity.LineStatus.AVISO,
                            "Estación cerrada por obras"
                        )
                        "linea_e" -> Pair(
                            com.moovar.android.core.database.entity.LineStatus.AVISO,
                            "Estaciones cerradas por obras"
                        )
                        else -> Pair(
                            com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            "Servicio normal"
                        )
                    }
                    entity.copy(
                        status = subteStatus,
                        statusMessage = subteMsg,
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                } else {
                    entity
                }
            }

            lineDao.upsertAll(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing lines: ${e.message}", e)
        }
    }

    private fun evaluateLine(
        ramales: List<RamalDto>,
        lineAlerts: List<SofseAlertaDto>
    ): Pair<com.moovar.android.core.database.entity.LineStatus, String> {
        val ramalCats = ramales.map { evaluateRamal(it) }
        val lineCatList = lineAlerts.map { classifyAlert(it.contenido) }

        val interruptedCount = ramalCats.count { it == AlertCategory.INTERRUPTED }
        val limitedCount = ramalCats.count { it == AlertCategory.LIMITED_ROUTE }
        val cancelledCount = ramalCats.count { it == AlertCategory.CANCELLED }
        val delayCount = ramalCats.count { it == AlertCategory.DELAY }
        val futureWorksCount = ramalCats.count { it == AlertCategory.FUTURE_SCHEDULED } +
                lineCatList.count { it == AlertCategory.FUTURE_SCHEDULED }
        val infoWorksCount = ramalCats.count { it == AlertCategory.INFO_WORKS } +
                lineCatList.count { it == AlertCategory.INFO_WORKS }

        // Total Line Interruption: All ramales interrupted or line-level alert says interrupted
        val isTotalInterruption = (ramales.isNotEmpty() && interruptedCount >= ramales.size) ||
                (AlertCategory.INTERRUPTED in lineCatList)

        if (isTotalInterruption) {
            return Pair(com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO, "Servicio interrumpido")
        }

        // Active disruptions happening NOW
        val hasActiveDisruption = interruptedCount > 0 || limitedCount > 0 || cancelledCount > 0 || delayCount > 0 ||
                (AlertCategory.DELAY in lineCatList) || (AlertCategory.LIMITED_ROUTE in lineCatList)

        if (hasActiveDisruption) {
            val totalDelays = delayCount + lineCatList.count { it == AlertCategory.DELAY }
            val totalLimited = limitedCount + lineCatList.count { it == AlertCategory.LIMITED_ROUTE }

            val msg = when {
                // Single issue type
                interruptedCount > 0 && totalLimited == 0 && cancelledCount == 0 && totalDelays == 0 -> {
                    if (interruptedCount == 1) "Ramal interrumpido" else "Ramales interrumpidos"
                }
                totalDelays > 0 && interruptedCount == 0 && totalLimited == 0 && cancelledCount == 0 -> {
                    if (totalDelays == 1) "Ramal con demoras" else "Ramales con demoras"
                }
                totalLimited > 0 && interruptedCount == 0 && cancelledCount == 0 && totalDelays == 0 -> {
                    if (totalLimited == 1) "Ramal con recorrido limitado" else "Ramales con recorrido limitado"
                }
                cancelledCount > 0 && interruptedCount == 0 && totalLimited == 0 && totalDelays == 0 -> {
                    if (cancelledCount == 1) "Cancelaciones en ramal" else "Cancelaciones en varios ramales"
                }
                // Mixed combinations
                totalDelays > 0 && cancelledCount > 0 -> "Demoras y cancelaciones"
                interruptedCount > 0 && totalDelays > 0 -> {
                    if (interruptedCount == 1) "Ramal interrumpido y demoras"
                    else "Ramales interrumpidos y con demoras"
                }
                interruptedCount > 0 && totalLimited > 0 -> {
                    if (interruptedCount == 1) "Ramal interrumpido y recorrido limitado"
                    else "Ramales interrumpidos y limitados"
                }
                totalLimited > 0 && totalDelays > 0 -> "Demoras y recorrido limitado"
                else -> "Ramales con servicio afectado"
            }
            return Pair(com.moovar.android.core.database.entity.LineStatus.DEMORADO, msg)
        }

        // Future Scheduled Works or Info Works (AVISO)
        if (futureWorksCount > 0 || infoWorksCount > 0) {
            val msg = when {
                futureWorksCount == 1 -> "Obras programadas en ramal"
                futureWorksCount > 1 -> "Obras programadas en ramales"
                else -> "Obras en zona de vías"
            }
            return Pair(com.moovar.android.core.database.entity.LineStatus.AVISO, msg)
        }

        return Pair(com.moovar.android.core.database.entity.LineStatus.NORMAL, "Servicio normal")
    }

    private fun evaluateRamal(ramal: RamalDto): AlertCategory {
        if (ramal.operativo == 0) {
            return AlertCategory.INTERRUPTED
        }
        val alerts = (ramal.alerta ?: emptyList()).filterNot { isCudAlert(it.contenido) }
        if (alerts.isEmpty()) return AlertCategory.IGNORED

        val categories = alerts.map { classifyAlert(it.contenido) }
        return when {
            AlertCategory.INTERRUPTED in categories -> AlertCategory.INTERRUPTED
            AlertCategory.LIMITED_ROUTE in categories -> AlertCategory.LIMITED_ROUTE
            AlertCategory.CANCELLED in categories -> AlertCategory.CANCELLED
            AlertCategory.DELAY in categories -> AlertCategory.DELAY
            AlertCategory.FUTURE_SCHEDULED in categories -> AlertCategory.FUTURE_SCHEDULED
            AlertCategory.INFO_WORKS in categories -> AlertCategory.INFO_WORKS
            else -> AlertCategory.IGNORED
        }
    }

    private fun classifyAlert(alertContent: String): AlertCategory {
        val clean = alertContent.replace('\u00A0', ' ').trim()
        if (isCudAlert(clean)) return AlertCategory.IGNORED

        val lower = clean.lowercase()

        // 1. Check if future scheduled works/advisory
        if (isFutureAlert(clean)) {
            return AlertCategory.FUTURE_SCHEDULED
        }

        // 2. Check if it refers to a single past service that departed >40min ago
        if (isPastSingleService(clean)) {
            return AlertCategory.IGNORED
        }

        // 3. Active real-time disruptions
        if (lower.contains("interrumpid") || lower.contains("sin servicio") || lower.contains("servicio suspendido")) {
            return AlertCategory.INTERRUPTED
        }
        if (lower.contains("recorrido limitado") || lower.contains("no saldrán ni llegarán") || lower.contains("no saldra ni llegara")) {
            return AlertCategory.LIMITED_ROUTE
        }
        if (lower.contains("demora") || lower.contains("demorado")) {
            return AlertCategory.DELAY
        }
        if (lower.contains("cancelad") || lower.contains("cancelacion")) {
            return AlertCategory.CANCELLED
        }

        // 4. Informational works / notices
        if (lower.contains("obras") || lower.contains("renovaci") || lower.contains("andén") || lower.contains("anden")) {
            return AlertCategory.INFO_WORKS
        }

        return AlertCategory.IGNORED
    }

    private fun isCudAlert(content: String): Boolean {
        val clean = content.replace('\u00A0', ' ')
        return clean.contains("CUD", ignoreCase = true) || clean.contains("discapacidad", ignoreCase = true)
    }

    private fun isFutureAlert(content: String): Boolean {
        val lower = content.lowercase()
        if (lower.contains("próximo fin de semana") || lower.contains("proximo fin de semana")) {
            return true
        }

        val today = try {
            LocalDate.now(clock)
        } catch (e: Exception) {
            LocalDate.now()
        }

        val dateMatches = Regex("""\b(\d{1,2})/(\d{1,2})\b""").findAll(content).toList()
        if (dateMatches.isEmpty()) return false

        // Ongoing notice check (e.g. "Hasta el 17/10...")
        val hasHasta = Regex("""(?i)hasta el \d{1,2}/\d{1,2}""").containsMatchIn(lower)
        if (hasHasta) return false

        val parsedDates = dateMatches.mapNotNull { match ->
            try {
                val day = match.groupValues[1].toInt()
                val month = match.groupValues[2].toInt()
                LocalDate.of(today.year, month, day)
            } catch (e: Exception) {
                null
            }
        }

        if (parsedDates.isEmpty()) return false
        return parsedDates.all { it.isAfter(today) }
    }

    private fun isPastSingleService(content: String): Boolean {
        val match = Regex("""(?i)el tren de las?\s*(\d{1,2}):(\d{2})""").find(content) ?: return false
        val hour = match.groupValues[1].toIntOrNull() ?: return false
        val minute = match.groupValues[2].toIntOrNull() ?: return false

        return try {
            val now = LocalTime.now(clock)
            val serviceTime = LocalTime.of(hour, minute)
            now.isAfter(serviceTime.plusMinutes(40))
        } catch (e: Exception) {
            false
        }
    }

    private fun mapStatus(status: com.moovar.android.core.database.entity.LineStatus): LineStatus = when (status) {
        com.moovar.android.core.database.entity.LineStatus.NORMAL -> LineStatus.NORMAL
        com.moovar.android.core.database.entity.LineStatus.AVISO -> LineStatus.AVISO
        com.moovar.android.core.database.entity.LineStatus.DEMORADO -> LineStatus.DEMORADO
        com.moovar.android.core.database.entity.LineStatus.CANCELADO -> LineStatus.CANCELADO
        com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO -> LineStatus.SIN_SERVICIO
        com.moovar.android.core.database.entity.LineStatus.DESCONOCIDO -> LineStatus.DESCONOCIDO
    }
}

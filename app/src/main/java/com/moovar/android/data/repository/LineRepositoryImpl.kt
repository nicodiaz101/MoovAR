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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                        val ramalAlertsWithRamal = ramales.flatMap { ramal ->
                            (ramal.alerta ?: emptyList())
                                .filterNot { isCudAlert(it.contenido) }
                                .map { alerta -> Pair(ramal, alerta) }
                        }
                        val inoperativeRamales = ramales.filter { it.operativo == 0 }

                        val (newStatus, newMsg) = when {
                            ramales.isNotEmpty() && inoperativeRamales.size == ramales.size -> {
                                Pair(com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO, "Servicio interrumpido")
                            }
                            ramalAlertsWithRamal.isNotEmpty() || lineAlerts.isNotEmpty() || inoperativeRamales.isNotEmpty() -> {
                                val firstRamal = ramalAlertsWithRamal.firstOrNull()
                                val firstLine = lineAlerts.firstOrNull()
                                val msg = when {
                                    firstRamal != null -> {
                                        summarizeAlert(firstRamal.first.nombre, firstRamal.second.contenido)
                                    }
                                    firstLine != null -> {
                                        summarizeAlert(null, firstLine.contenido)
                                    }
                                    inoperativeRamales.isNotEmpty() -> {
                                        "Ramal ${inoperativeRamales.first().nombre} no operativo"
                                    }
                                    else -> gerencia.estado?.mensaje ?: "Alertas en el servicio"
                                }
                                Pair(com.moovar.android.core.database.entity.LineStatus.DEMORADO, msg)
                            }
                            else -> {
                                Pair(com.moovar.android.core.database.entity.LineStatus.NORMAL, "Servicio normal")
                            }
                        }

                        entity.copy(
                            status = newStatus,
                            statusMessage = newMsg,
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    } else {
                        entity
                    }
                } else if (entity.networkType == com.moovar.android.core.database.entity.NetworkType.SUBTE) {
                    entity.copy(
                        status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                        statusMessage = "Servicio normal",
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

    private fun isCudAlert(content: String): Boolean {
        val clean = content.replace('\u00A0', ' ')
        return clean.contains("CUD", ignoreCase = true) || clean.contains("discapacidad", ignoreCase = true)
    }

    private fun summarizeAlert(ramalName: String?, alertContent: String): String {
        var clean = alertContent.replace('\u00A0', ' ')
            .replace(Regex("(?i)Disculp[áa] las molestias.*"), "")
            .replace(Regex("(?i)Consult[áa] las alternativas.*"), "")
            .replace(Regex("(?i)M[áa]s informaci[óo]n en.*"), "")
            .replace(Regex("(?i)trenesargentinos\\.gob\\.ar"), "")
            .trim()
        while (clean.endsWith(".") || clean.endsWith(" ")) {
            clean = clean.dropLast(1).trim()
        }

        val lower = clean.lowercase()
        val summary = when {
            lower.contains("demora") -> {
                val match = Regex("demoras?\\s*(?:de\\s*)?(\\d+\\s*minutos?)", RegexOption.IGNORE_CASE).find(clean)
                if (match != null) "Demoras de ${match.groupValues[1]} aprox."
                else "Demoras en el servicio"
            }
            lower.contains("cancelad") || lower.contains("cancelacion") -> {
                if (lower.contains("problemas operativos")) "Cancelaciones por problemas operativos"
                else if (lower.contains("problemas t")) "Cancelaciones por problemas técnicos"
                else "Servicios cancelados"
            }
            lower.contains("interrumpid") -> {
                if (lower.contains("obras") || lower.contains("renovaci")) "Interrumpido por obras"
                else "Servicio interrumpido"
            }
            lower.contains("recorrido limitado") || lower.contains("no saldrán ni llegarán") -> {
                val match = Regex("entre\\s+([^,]+?)\\s+y\\s+([^,]+?)(?:\\s*,|\\s+por|\\s+debido|\\s+a\\s+causa|$)", RegexOption.IGNORE_CASE).find(clean)
                if (match != null) {
                    val b1 = match.groupValues[1].trim()
                    val b2 = match.groupValues[2].trim()
                    "Recorrido limitado entre $b1 y $b2"
                } else {
                    "Recorrido limitado por obras"
                }
            }
            lower.contains("obras") -> "Obras en zona de vías"
            else -> {
                val firstSentence = clean.substringBefore(".").trim()
                if (firstSentence.length > 55) firstSentence.take(52).trim() + "..."
                else firstSentence
            }
        }

        return if (!ramalName.isNullOrBlank()) {
            val shortRamal = ramalName
                .replace(Regex("(?i)^Buenos Aires-"), "")
                .replace(Regex("(?i)^Retiro-"), "")
                .replace(Regex("(?i)^Constitución-"), "")
                .trim()
            "$shortRamal: $summary"
        } else {
            summary
        }
    }

    private fun mapStatus(status: com.moovar.android.core.database.entity.LineStatus): LineStatus = when (status) {
        com.moovar.android.core.database.entity.LineStatus.NORMAL -> LineStatus.NORMAL
        com.moovar.android.core.database.entity.LineStatus.DEMORADO -> LineStatus.DEMORADO
        com.moovar.android.core.database.entity.LineStatus.CANCELADO -> LineStatus.CANCELADO
        com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO -> LineStatus.SIN_SERVICIO
        com.moovar.android.core.database.entity.LineStatus.DESCONOCIDO -> LineStatus.DESCONOCIDO
    }
}

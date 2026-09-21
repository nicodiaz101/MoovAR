package com.moovar.android.data.repository

import android.util.Log
import com.moovar.android.core.common.Result
import com.moovar.android.core.database.dao.AlertDao
import com.moovar.android.core.database.entity.AlertEntity
import com.moovar.android.core.domain.model.AlertSeverity
import com.moovar.android.core.domain.model.ServiceAlert
import com.moovar.android.core.domain.repository.AlertRepository
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val sofseApiService: SofseApiService
) : AlertRepository {

    companion object {
        private const val TAG = "AlertRepositoryImpl"

        // Map SOFSE gerencia IDs to local line IDs
        private val GERENCIA_TO_LINE = mapOf(
            11 to "roca",
            1 to "sarmiento",
            5 to "mitre",
            31 to "san_martin",
            21 to "belgrano_sur",
            41 to "tren_costa"
        )
    }

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun observeAlerts(lineId: String?): Flow<Result<List<ServiceAlert>>> {
        repositoryScope.launch {
            try {
                refreshAlerts()
            } catch (e: Exception) {
                Log.w(TAG, "Background refresh alerts failed: ${e.message}")
            }
        }

        val effectiveLineId = if (lineId.isNullOrBlank()) null else lineId

        return alertDao.observeAlerts(effectiveLineId).map { entities ->
            val alerts = entities.map { entity ->
                ServiceAlert(
                    id = entity.id,
                    lineId = entity.lineId,
                    branchId = entity.branchId,
                    title = entity.title,
                    description = entity.description,
                    severity = when (entity.severity) {
                        com.moovar.android.core.database.entity.AlertSeverity.INFO -> AlertSeverity.INFO
                        com.moovar.android.core.database.entity.AlertSeverity.WARNING -> AlertSeverity.WARNING
                        com.moovar.android.core.database.entity.AlertSeverity.CRITICAL -> AlertSeverity.CRITICAL
                    }
                )
            }
            Result.Success(alerts)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun refreshAlerts(): Unit = withContext(Dispatchers.IO) {
        try {
            val entities = mutableListOf<AlertEntity>()
            val now = System.currentTimeMillis()

            // 1. Fetch real SOFSE gerencias (train lines) and line alerts
            val gerencias = try {
                sofseApiService.getGerencias()
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching SOFSE gerencias: ${e.message}")
                emptyList()
            }

            for (gerencia in gerencias) {
                val lineId = GERENCIA_TO_LINE[gerencia.id] ?: continue

                // Add line-level alerts (excluding purely informative CUD notifications)
                gerencia.alerta.forEach { alertaDto ->
                    val cleanText = alertaDto.contenido.replace('\u00A0', ' ').trim()
                    if (isCudAlert(cleanText)) {
                        return@forEach
                    }
                    entities.add(
                        AlertEntity(
                            id = "sofse_line_${alertaDto.id}",
                            lineId = lineId,
                            branchId = null,
                            title = "Línea ${gerencia.nombre}",
                            description = cleanText,
                            severity = mapSofseSeverity(cleanText, alertaDto.criticidadColorFondo, alertaDto.criticidadOrden),
                            publishedAt = now,
                            expiresAt = null,
                            cachedAt = now
                        )
                    )
                }

                // Query ramales for this gerencia to get real-time cancellation/disruption alerts
                try {
                    val ramales = sofseApiService.getRamales(idGerencia = gerencia.id)
                    for (ramal in ramales) {
                        val ramalAlerts = ramal.alerta?.filterNot { isCudAlert(it.contenido) } ?: emptyList()
                        for (alertaDto in ramalAlerts) {
                            val cleanText = alertaDto.contenido.replace('\u00A0', ' ').trim()
                            entities.add(
                                AlertEntity(
                                    id = "sofse_ramal_${ramal.id}_${alertaDto.id}",
                                    lineId = lineId,
                                    branchId = "${lineId}_${ramal.id}",
                                    title = "Línea ${gerencia.nombre} • ${ramal.nombre}",
                                    description = cleanText,
                                    severity = mapSofseSeverity(cleanText, alertaDto.criticidadColorFondo, alertaDto.criticidadOrden),
                                    publishedAt = now,
                                    expiresAt = null,
                                    cachedAt = now
                                )
                            )
                        }

                        // If branch is marked non-operational in SOFSE and has no specific alert text, synthesize an alert
                        if (ramal.operativo == 0 && ramalAlerts.isEmpty()) {
                            entities.add(
                                AlertEntity(
                                    id = "sofse_ramal_inop_${ramal.id}",
                                    lineId = lineId,
                                    branchId = "${lineId}_${ramal.id}",
                                    title = "Línea ${gerencia.nombre} • ${ramal.nombre}",
                                    description = "Servicio interrumpido en este ramal.",
                                    severity = com.moovar.android.core.database.entity.AlertSeverity.CRITICAL,
                                    publishedAt = now,
                                    expiresAt = null,
                                    cachedAt = now
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching ramales for ${gerencia.nombre}: ${e.message}")
                }
            }

            if (gerencias.isNotEmpty()) {
                
                if (entities.isNotEmpty()) {
                    alertDao.replaceAll(entities)
                }
                Log.d(TAG, "Refreshed ${entities.size} active SOFSE alerts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh alerts: ${e.message}", e)
        }
    }

    private fun isCudAlert(content: String): Boolean {
        val clean = content.replace('\u00A0', ' ')
        return clean.contains("CUD", ignoreCase = true) || clean.contains("discapacidad", ignoreCase = true)
    }

    private fun mapSofseSeverity(
        content: String,
        colorHex: String?,
        orden: Int?
    ): com.moovar.android.core.database.entity.AlertSeverity {
        val lower = content.lowercase()
        return when {
            orden == 1 || lower.contains("interrumpid") || lower.contains("sin servicio") ->
                com.moovar.android.core.database.entity.AlertSeverity.CRITICAL
            orden in listOf(2, 3) || colorHex?.lowercase() in listOf("#c9302c", "#a94442") ->
                com.moovar.android.core.database.entity.AlertSeverity.CRITICAL
            lower.contains("demora") || lower.contains("cancelad") || lower.contains("recorrido limitado") ||
            colorHex?.lowercase() in listOf("#d49532", "#f0ad4e", "#d9534f", "#faebcc") ->
                com.moovar.android.core.database.entity.AlertSeverity.WARNING
            else -> com.moovar.android.core.database.entity.AlertSeverity.INFO
        }
    }
}

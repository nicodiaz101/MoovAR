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

        return alertDao.observeAlerts(lineId).map { entities ->
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
        }
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
                    if (cleanText.contains("CUD", ignoreCase = true) || cleanText.contains("discapacidad", ignoreCase = true)) {
                        return@forEach
                    }
                    entities.add(
                        AlertEntity(
                            id = "sofse_line_${alertaDto.id}",
                            lineId = lineId,
                            branchId = null,
                            title = "Línea ${gerencia.nombre}",
                            description = cleanText,
                            severity = mapSofseSeverity(alertaDto.criticidadColorFondo, alertaDto.criticidadOrden),
                            publishedAt = now,
                            expiresAt = null,
                            cachedAt = now
                        )
                    )
                }

                // Query ramales for this gerencia to get real-time cancellation alerts (e.g. Ballester - Zárate)
                try {
                    val ramales = sofseApiService.getRamales(idGerencia = gerencia.id)
                    for (ramal in ramales) {
                        ramal.alerta?.forEach { alertaDto ->
                            val cleanText = alertaDto.contenido.replace('\u00A0', ' ').trim()
                            if (cleanText.contains("CUD", ignoreCase = true) || cleanText.contains("discapacidad", ignoreCase = true)) {
                                return@forEach
                            }
                            entities.add(
                                AlertEntity(
                                    id = "sofse_ramal_${alertaDto.id}",
                                    lineId = lineId,
                                    branchId = "${lineId}_${ramal.id}",
                                    title = "Ramal ${ramal.nombre}",
                                    description = cleanText,
                                    severity = mapSofseSeverity(alertaDto.criticidadColorFondo, alertaDto.criticidadOrden),
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

            // 2. Real Subte Alerts (Closed stations under Plan de Renovación Integral)
            // Uniform explanation across all closed stations
            val subteClosedExplanation = "Estación cerrada por obras del Plan de Renovación Integral. Los trenes no se detienen en esta estación."

            entities.add(
                AlertEntity(
                    id = "subte_alert_medrano",
                    lineId = "linea_b",
                    branchId = null,
                    title = "Línea B: Estación Medrano cerrada",
                    description = subteClosedExplanation,
                    severity = com.moovar.android.core.database.entity.AlertSeverity.WARNING,
                    publishedAt = now,
                    expiresAt = null,
                    cachedAt = now
                )
            )
            entities.add(
                AlertEntity(
                    id = "subte_alert_lavalle",
                    lineId = "linea_c",
                    branchId = null,
                    title = "Línea C: Estación Lavalle cerrada",
                    description = subteClosedExplanation,
                    severity = com.moovar.android.core.database.entity.AlertSeverity.WARNING,
                    publishedAt = now,
                    expiresAt = null,
                    cachedAt = now
                )
            )
            entities.add(
                AlertEntity(
                    id = "subte_alert_tribunales",
                    lineId = "linea_d",
                    branchId = null,
                    title = "Línea D: Estación Tribunales cerrada",
                    description = subteClosedExplanation,
                    severity = com.moovar.android.core.database.entity.AlertSeverity.WARNING,
                    publishedAt = now,
                    expiresAt = null,
                    cachedAt = now
                )
            )
            entities.add(
                AlertEntity(
                    id = "subte_alert_entre_rios",
                    lineId = "linea_e",
                    branchId = null,
                    title = "Línea E: Estación Entre Ríos cerrada",
                    description = subteClosedExplanation,
                    severity = com.moovar.android.core.database.entity.AlertSeverity.WARNING,
                    publishedAt = now,
                    expiresAt = null,
                    cachedAt = now
                )
            )
            entities.add(
                AlertEntity(
                    id = "subte_alert_urquiza",
                    lineId = "linea_e",
                    branchId = null,
                    title = "Línea E: Estación General Urquiza cerrada",
                    description = subteClosedExplanation,
                    severity = com.moovar.android.core.database.entity.AlertSeverity.WARNING,
                    publishedAt = now,
                    expiresAt = null,
                    cachedAt = now
                )
            )

            if (entities.isNotEmpty()) {
                alertDao.deleteAll()
                alertDao.upsertAll(entities)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh alerts: ${e.message}", e)
        }
    }

    private fun mapSofseSeverity(colorHex: String?, orden: Int?): com.moovar.android.core.database.entity.AlertSeverity {
        return when {
            orden == 1 -> com.moovar.android.core.database.entity.AlertSeverity.CRITICAL
            orden in listOf(2, 3) -> com.moovar.android.core.database.entity.AlertSeverity.WARNING
            colorHex?.lowercase() in listOf("#c9302c", "#a94442") -> com.moovar.android.core.database.entity.AlertSeverity.CRITICAL
            colorHex?.lowercase() in listOf("#d49532", "#f0ad4e", "#d9534f", "#faebcc") -> com.moovar.android.core.database.entity.AlertSeverity.WARNING
            else -> com.moovar.android.core.database.entity.AlertSeverity.INFO
        }
    }
}

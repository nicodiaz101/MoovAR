package com.moovar.android.data.repository

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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val sofseApiService: SofseApiService
) : AlertRepository {

    override fun observeAlerts(lineId: String?): Flow<Result<List<ServiceAlert>>> {
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

    override suspend fun refreshAlerts() = withContext(Dispatchers.IO) {
        try {
            val dtoList = sofseApiService.getAlertas()
            val entities = dtoList.map { dto ->
                AlertEntity(
                    id = dto.id,
                    lineId = "roca", // fallback if null
                    branchId = null,
                    title = dto.title,
                    description = dto.description,
                    severity = when (dto.severity.uppercase()) {
                        "WARNING", "ALERTA" -> com.moovar.android.core.database.entity.AlertSeverity.WARNING
                        "CRITICAL", "GRAVE" -> com.moovar.android.core.database.entity.AlertSeverity.CRITICAL
                        else -> com.moovar.android.core.database.entity.AlertSeverity.INFO
                    },
                    publishedAt = dto.publishedAt,
                    expiresAt = dto.expiresAt,
                    cachedAt = System.currentTimeMillis()
                )
            }
            alertDao.upsertAll(entities)
        } catch (_: Exception) {
            // Keep cached alerts
        }
    }
}

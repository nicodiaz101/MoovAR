package com.moovar.android.core.network.sofse

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.LineStatus
import com.moovar.android.core.domain.model.Line
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.StopState
import com.moovar.android.core.domain.model.ServiceAlert
import com.moovar.android.core.domain.model.AlertSeverity
import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.model.Coordinates
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SofseRemoteDataSource @Inject constructor(
    private val apiService: SofseApiService
) {
    suspend fun getLines(): Result<List<Line>> = safeApiCall {
        apiService.getLineas().map { dto ->
            Line(
                id = dto.id,
                name = dto.name,
                networkType = NetworkType.TREN,
                colorHex = dto.colorHex ?: "#000000",
                status = mapLineStatus(dto.status),
                statusMessage = dto.message
            )
        }
    }

    suspend fun getDepartures(originId: String, destinationId: String): Result<List<Departure>> = safeApiCall {
        apiService.getProximos(originId, destinationId).map { dto ->
            Departure(
                serviceId = dto.serviceId,
                branchName = dto.branchName,
                destination = dto.destination,
                minutesAway = dto.minutesAway,
                scheduledTime = dto.scheduledTime,
                platform = dto.platform,
                serviceType = dto.serviceType,
                status = dto.status,
                vehicleCoordinates = if (dto.latitude != null && dto.longitude != null) Coordinates(dto.latitude, dto.longitude) else null,
                networkType = NetworkType.TREN
            )
        }
    }

    suspend fun getJourney(serviceId: String): Result<List<JourneyStop>> = safeApiCall {
        val dto = apiService.getRecorrido(serviceId)
        dto.stops.map { stopDto ->
            JourneyStop(
                stationName = stopDto.name,
                scheduledTime = stopDto.scheduledTime,
                stopState = mapStopState(stopDto.state),
                isTerminus = stopDto.isTerminus
            )
        }
    }

    suspend fun getAlerts(lineId: String? = null): Result<List<ServiceAlert>> = safeApiCall {
        apiService.getAlertas(lineId).map { dto ->
            ServiceAlert(
                id = dto.id,
                lineId = lineId ?: "", // Assuming lineId mapping is handled higher up or API returns it
                branchId = null,
                title = dto.title,
                description = dto.description,
                severity = mapAlertSeverity(dto.severity)
            )
        }
    }
    
    suspend fun getBranches(lineId: String): Result<List<Branch>> = safeApiCall {
        apiService.getRamales(lineId).map { dto ->
            Branch(
                id = dto.id,
                lineId = lineId,
                name = dto.name,
                originTerminus = "", // Provided by local DB?
                destinationTerminus = ""
            )
        }
    }

    suspend fun getStations(branchId: String): Result<List<Station>> = safeApiCall {
        apiService.getEstaciones(branchId).map { dto ->
            Station(
                id = dto.id,
                name = dto.name,
                branchId = branchId,
                lineId = "", // Should be populated by DB or mapper
                networkType = NetworkType.TREN,
                gtfsStopId = null,
                sequenceInBranch = 0,
                isTerminus = false,
                coordinates = if (dto.latitude != null && dto.longitude != null) Coordinates(dto.latitude, dto.longitude) else null
            )
        }
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.Success(apiCall())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    private fun mapLineStatus(status: String): LineStatus = when (status.uppercase()) {
        "NORMAL" -> LineStatus.NORMAL
        "DEMORADO" -> LineStatus.DEMORADO
        "CANCELADO" -> LineStatus.CANCELADO
        "SIN SERVICIO", "SIN_SERVICIO" -> LineStatus.SIN_SERVICIO
        else -> LineStatus.DESCONOCIDO
    }

    private fun mapStopState(state: String): StopState = when (state.uppercase()) {
        "PASADO" -> StopState.PAST
        "ACTUAL" -> StopState.CURRENT
        "FUTURO" -> StopState.FUTURE
        else -> StopState.FUTURE
    }

    private fun mapAlertSeverity(severity: String): AlertSeverity = when (severity.uppercase()) {
        "INFO" -> AlertSeverity.INFO
        "WARNING", "ADVERTENCIA" -> AlertSeverity.WARNING
        "CRITICAL", "CRITICA" -> AlertSeverity.CRITICAL
        else -> AlertSeverity.INFO
    }
}

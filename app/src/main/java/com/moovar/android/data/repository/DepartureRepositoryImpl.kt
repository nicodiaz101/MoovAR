package com.moovar.android.data.repository

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.Coordinates
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.repository.DepartureRepository
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DepartureRepositoryImpl @Inject constructor(
    private val sofseApiService: SofseApiService
) : DepartureRepository {

    override suspend fun getDepartures(
        originId: String,
        destinationId: String,
        departureTime: LocalDateTime
    ): Result<List<Departure>> = withContext(Dispatchers.IO) {
        try {
            val dtoList = sofseApiService.getProximos(originId, destinationId)
            val departures = dtoList.map { dto ->
                    val lat = dto.latitude
                    val lon = dto.longitude
                    Departure(
                        serviceId = dto.serviceId,
                        branchName = dto.branchName,
                        destination = dto.destination,
                        minutesAway = dto.minutesAway,
                        scheduledTime = dto.scheduledTime,
                        platform = dto.platform,
                        serviceType = dto.serviceType,
                        status = dto.status,
                        vehicleCoordinates = if (lat != null && lon != null) {
                            Coordinates(lat, lon)
                        } else null,
                        networkType = NetworkType.TREN,
                        isTerminus = false
                    )
            }
            Result.Success(departures)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener próximos trenes")
        }
    }
}

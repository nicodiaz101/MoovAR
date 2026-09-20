package com.moovar.android.data.repository

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.StopState
import com.moovar.android.core.domain.repository.JourneyRepository
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepositoryImpl @Inject constructor(
    private val sofseApiService: SofseApiService
) : JourneyRepository {

    override suspend fun getJourney(serviceId: String): Result<List<JourneyStop>> = withContext(Dispatchers.IO) {
        try {
            val dto = sofseApiService.getRecorrido(serviceId)
            val stops = dto.stops.map { stop ->
                JourneyStop(
                    stationName = stop.name,
                    scheduledTime = stop.scheduledTime,
                    stopState = when (stop.state.uppercase()) {
                        "PASO", "PASADA", "PAST" -> StopState.PAST
                        "ACTUAL", "CURRENT" -> StopState.CURRENT
                        else -> StopState.FUTURE
                    },
                    isTerminus = stop.isTerminus
                )
            }
            Result.Success(stops)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener el recorrido")
        }
    }
}

package com.moovar.android.data.repository

import com.moovar.android.core.common.Result
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.dao.StationDao
import com.moovar.android.core.domain.model.JourneyDetails
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.StopState
import com.moovar.android.core.domain.repository.JourneyRepository
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepositoryImpl @Inject constructor(
    private val sofseApiService: SofseApiService,
    private val stationDao: StationDao,
    private val branchDao: BranchDao,
    private val lineDao: LineDao
) : JourneyRepository {

    override suspend fun getJourney(serviceId: String): Result<List<JourneyStop>> = withContext(Dispatchers.IO) {
        when (val result = getJourneyDetails(serviceId)) {
            is Result.Success -> Result.Success(result.data.stops)
            is Result.Error -> Result.Error(result.message)
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun getJourneyDetails(serviceId: String): Result<JourneyDetails> = withContext(Dispatchers.IO) {
        val decodedServiceId = try {
            URLDecoder.decode(serviceId, "UTF-8")
        } catch (_: Exception) {
            serviceId
        }

        // 1. Check if serviceId is an encoded local service: "srv___branchId___targetDest___originStationId___minutes___time___platform___serviceType___status___isCancelled"
        if (decodedServiceId.startsWith("srv___")) {
            val parts = decodedServiceId.split("___")
            if (parts.size >= 9) {
                val branchId = parts[1]
                val targetDest = parts[2]
                val originStationId = parts[3]
                val minutesAway = parts[4].toIntOrNull() ?: 5
                val departureTimeStr = parts[5]
                val platform = parts[6]
                val serviceType = parts[7]
                val currentStatus = parts[8]
                val isCancelled = parts.getOrNull(9)?.toBooleanStrictOrNull() ?: false

                val branch = branchDao.getById(branchId)
                val line = if (branch != null) lineDao.getById(branch.lineId) else null
                var stations = stationDao.getByBranch(branchId)

                if (stations.isEmpty()) {
                    stations = stationDao.search("", branchId)
                }

                if (stations.isNotEmpty()) {
                    // Check if train is travelling towards branch.originTerminus -> if so, reverse the station order
                    if (branch != null && targetDest.equals(branch.originTerminus, ignoreCase = true)) {
                        stations = stations.reversed()
                    }

                    // Find origin station index in this sequence
                    val originIdx = stations.indexOfFirst {
                        it.id == originStationId || it.name.equals(originStationId, ignoreCase = true)
                    }
                    val safeOriginIdx = if (originIdx >= 0) originIdx else 0

                    // Determine current train position index defensively
                    val currentTrainIndex = when {
                        isCancelled -> safeOriginIdx
                        safeOriginIdx == 0 -> 0 // Terminus origin: train is at platform 0
                        minutesAway <= 0 -> safeOriginIdx
                        minutesAway in 1..4 -> maxOf(0, safeOriginIdx - 1)
                        minutesAway in 5..9 -> maxOf(0, safeOriginIdx - 2)
                        else -> maxOf(0, safeOriginIdx - 3)
                    }.coerceIn(0, stations.lastIndex)

                    val isSubte = line?.networkType == com.moovar.android.core.database.entity.NetworkType.SUBTE
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val baseTime = LocalTime.now()

                    val stops = stations.mapIndexed { index, stn ->
                        val stopState = when {
                            index < currentTrainIndex -> StopState.PAST
                            index == currentTrainIndex -> StopState.CURRENT
                            else -> StopState.FUTURE
                        }
                        val diffFromCurrent = index - currentTrainIndex
                        val minutesDelta = if (isSubte) diffFromCurrent * 3 else diffFromCurrent * 4
                        val stopTime = baseTime.plusMinutes(minutesDelta.toLong()).format(timeFormatter)

                        JourneyStop(
                            stationName = stn.name,
                            scheduledTime = if (stopState == StopState.PAST) null else stopTime,
                            stopState = stopState,
                            isTerminus = (index == 0 || index == stations.lastIndex)
                        )
                    }

                    val branchDisplayName = branch?.name ?: line?.name ?: "Servicio"
                    return@withContext Result.Success(
                        JourneyDetails(
                            branchName = branchDisplayName,
                            serviceType = serviceType,
                            destination = targetDest,
                            platform = platform,
                            departureTime = departureTimeStr,
                            currentStatus = currentStatus,
                            stops = stops
                        )
                    )
                }
            }
        }

        // 2. Try remote SOFSE API if not an encoded ID or fallback
        try {
            val dto = sofseApiService.getRecorrido(decodedServiceId)
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
            Result.Success(
                JourneyDetails(
                    branchName = "Servicio Ferroviario",
                    serviceType = "Común",
                    destination = dto.stops.lastOrNull()?.name ?: "Terminal",
                    platform = "1",
                    departureTime = dto.stops.firstOrNull()?.scheduledTime ?: "A horario",
                    currentStatus = "En viaje",
                    stops = stops
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "No se pudo obtener el recorrido del servicio")
        }
    }
}

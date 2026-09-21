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
    private val lineDao: LineDao,
    private val journeyDetailsCache: JourneyDetailsCache
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

        // 1. Check in-memory cache first (populated from real SOFSE API or pre-calculated Subte)
        val cached = journeyDetailsCache.get(decodedServiceId) ?: journeyDetailsCache.get(serviceId)
        if (cached != null) {
            return@withContext Result.Success(cached)
        }

        // 2. Check if serviceId is an encoded local service: "srv___branchId___targetDest___originStationId___minutes___time___platform___serviceType___status___isCancelled"
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
                    val isHeadingOrigin = isHeadingTowardsOrigin(targetDest, branch?.originTerminus, branch?.destinationTerminus)
                    if (isHeadingOrigin) {
                        stations = stations.reversed()
                    }

                    // Find origin station index in this sequence
                    val originIdx = stations.indexOfFirst {
                        it.id == originStationId || it.name.equals(originStationId, ignoreCase = true) || it.name.contains(originStationId, ignoreCase = true)
                    }
                    val safeOriginIdx = if (originIdx >= 0) originIdx else 0

                    // Train is strictly at or before the origin station if minutesAway > 0
                    val currentTrainIndex = when {
                        isCancelled -> safeOriginIdx
                        safeOriginIdx == 0 -> 0 // Terminus origin: train is at platform 0
                        minutesAway <= 0 -> safeOriginIdx
                        minutesAway in 1..4 -> maxOf(0, safeOriginIdx - 1)
                        minutesAway in 5..9 -> maxOf(0, safeOriginIdx - 2)
                        else -> maxOf(0, safeOriginIdx - 3)
                    }.coerceIn(0, stations.lastIndex)

                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val baseOriginTime = try {
                        LocalTime.parse(departureTimeStr, timeFormatter)
                    } catch (_: Exception) {
                        LocalTime.now().plusMinutes(minutesAway.toLong())
                    }

                    val stops = stations.mapIndexed { index, stn ->
                        val stopState = when {
                            index < currentTrainIndex -> StopState.PAST
                            index == currentTrainIndex -> StopState.CURRENT
                            else -> StopState.FUTURE
                        }
                        val diffFromOrigin = index - safeOriginIdx
                        val minutesDelta = diffFromOrigin * 4
                        val stopTime = baseOriginTime.plusMinutes(minutesDelta.toLong()).format(timeFormatter)

                        JourneyStop(
                            stationName = stn.name,
                            scheduledTime = if (stopState == StopState.PAST) null else stopTime,
                            stopState = stopState,
                            isTerminus = (index == 0 || index == stations.lastIndex)
                        )
                    }

                    val branchDisplayName = branch?.name ?: line?.name ?: "Servicio"
                    val details = JourneyDetails(
                        branchName = branchDisplayName,
                        serviceType = serviceType,
                        destination = targetDest,
                        platform = platform,
                        departureTime = departureTimeStr,
                        currentStatus = currentStatus,
                        stops = stops
                    )
                    journeyDetailsCache.put(decodedServiceId, details)
                    return@withContext Result.Success(details)
                }
            }
        }

        // 3. Try remote SOFSE API if not an encoded ID or fallback
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
            val details = JourneyDetails(
                branchName = "Servicio Ferroviario",
                serviceType = "Común",
                destination = dto.stops.lastOrNull()?.name ?: "Terminal",
                platform = "1",
                departureTime = dto.stops.firstOrNull()?.scheduledTime ?: "A horario",
                currentStatus = "En viaje",
                stops = stops
            )
            journeyDetailsCache.put(decodedServiceId, details)
            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e.message ?: "No se pudo obtener el recorrido del servicio")
        }
    }

    private fun isHeadingTowardsOrigin(
        targetDest: String,
        originTerminus: String?,
        destinationTerminus: String?
    ): Boolean {
        if (originTerminus.isNullOrBlank()) return false
        val normDest = normalizeTerminus(targetDest)
        val normOrigin = normalizeTerminus(originTerminus)
        val normDestTerminus = destinationTerminus?.let { normalizeTerminus(it) } ?: ""

        if (normDestTerminus.isNotEmpty() && (normDest.contains(normDestTerminus) || normDestTerminus.contains(normDest))) {
            return false
        }
        return normDest.contains(normOrigin) || normOrigin.contains(normDest)
    }

    private fun normalizeTerminus(name: String): String {
        return name.lowercase()
            .replace(Regex("\\(.*?\\)"), "")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("plaza", "")
            .replace("gral.", "")
            .replace("general", "")
            .replace("dr.", "")
            .trim()
    }
}

package com.moovar.android.core.domain.repository

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.FavoriteRoute
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.Line
import com.moovar.android.core.domain.model.ServiceAlert
import com.moovar.android.core.domain.model.Station
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface LineRepository {
    fun observeLines(): Flow<Result<List<Line>>>
    suspend fun refreshLines()
}

interface DepartureRepository {
    suspend fun getDepartures(
        originId: String,
        destinationId: String? = null,
        departureTime: LocalDateTime = LocalDateTime.now()
    ): Result<List<Departure>>
}

interface JourneyRepository {
    suspend fun getJourney(serviceId: String): Result<List<JourneyStop>>
    suspend fun getJourneyDetails(serviceId: String): Result<com.moovar.android.core.domain.model.JourneyDetails>
}

interface AlertRepository {
    fun observeAlerts(lineId: String? = null): Flow<Result<List<ServiceAlert>>>
    suspend fun refreshAlerts()
}

interface StationRepository {
    suspend fun searchStations(query: String, branchId: String? = null): List<Station>
    suspend fun getBranchesByLine(lineId: String): List<Branch>
}

interface RecentStationRepository {
    suspend fun getRecentStations(): List<Station>
    suspend fun saveRecentStation(station: Station)
}

interface FavoriteRouteRepository {
    fun observeFavorites(): Flow<List<FavoriteRoute>>
    fun isFavorite(originId: String, destinationId: String): Flow<Boolean>
    suspend fun toggleFavorite(origin: Station, destination: Station)
}

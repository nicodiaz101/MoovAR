package com.moovar.android.data.repository

import com.moovar.android.core.database.dao.RecentStationDao
import com.moovar.android.core.database.dao.StationDao
import com.moovar.android.core.database.entity.RecentStationEntity
import com.moovar.android.core.domain.model.Coordinates
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.RecentStationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentStationRepositoryImpl @Inject constructor(
    private val recentStationDao: RecentStationDao,
    private val stationDao: StationDao
) : RecentStationRepository {

    override suspend fun getRecentStations(): List<Station> = withContext(Dispatchers.IO) {
        val recentEntities = recentStationDao.getRecent()
        val stationIds = recentEntities.map { it.stationId }
        val stationsMap = stationDao.getByIds(stationIds).associateBy { it.id }

        recentEntities.mapNotNull { recent ->
            val entity = stationsMap[recent.stationId]
            if (entity != null) {
                val lat = entity.latitude
                val lon = entity.longitude
                Station(
                    id = entity.id,
                    name = entity.name,
                    branchId = entity.branchId,
                    lineId = entity.lineId,
                    networkType = if (entity.networkType == com.moovar.android.core.database.entity.NetworkType.SUBTE) NetworkType.SUBTE else NetworkType.TREN,
                    gtfsStopId = entity.gtfsStopId,
                    sequenceInBranch = entity.sequenceInBranch,
                    isTerminus = entity.isTerminus,
                    coordinates = if (lat != null && lon != null) {
                        Coordinates(lat, lon)
                    } else null
                )
            } else null
        }
    }

    override suspend fun saveRecentStation(station: Station) = withContext(Dispatchers.IO) {
        val entity = RecentStationEntity(
            stationId = station.id,
            stationName = station.name,
            lineId = station.lineId,
            lineName = "",
            accessedAt = System.currentTimeMillis()
        )
        recentStationDao.upsert(entity)
        recentStationDao.pruneOld()
    }
}

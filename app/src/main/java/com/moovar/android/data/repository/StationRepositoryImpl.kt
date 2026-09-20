package com.moovar.android.data.repository

import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.StationDao
import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.model.Coordinates
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.StationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val stationDao: StationDao,
    private val branchDao: BranchDao
) : StationRepository {

    private fun String.unaccent(): String {
        val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
    }

    override suspend fun searchStations(query: String, branchId: String?): List<Station> = withContext(Dispatchers.IO) {
        val allEntities = stationDao.getByBranchOptional(branchId)
        val filtered = if (query.isBlank()) {
            allEntities
        } else {
            val normalizedQuery = query.trim().unaccent()
            allEntities.filter { entity ->
                entity.name.unaccent().contains(normalizedQuery, ignoreCase = true)
            }
        }

        filtered.map { entity ->
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
        }
    }

    override suspend fun getBranchesByLine(lineId: String): List<Branch> = withContext(Dispatchers.IO) {
        branchDao.getByLine(lineId).map { entity ->
            Branch(
                id = entity.id,
                lineId = entity.lineId,
                name = entity.name,
                originTerminus = entity.originTerminus,
                destinationTerminus = entity.destinationTerminus
            )
        }
    }
}

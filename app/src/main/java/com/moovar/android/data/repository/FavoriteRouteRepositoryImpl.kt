package com.moovar.android.data.repository

import com.moovar.android.core.database.dao.FavoriteRouteDao
import com.moovar.android.core.database.entity.FavoriteRouteEntity
import com.moovar.android.core.domain.model.FavoriteRoute
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.FavoriteRouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRouteRepositoryImpl @Inject constructor(
    private val favoriteRouteDao: FavoriteRouteDao
) : FavoriteRouteRepository {

    override fun observeFavorites(): Flow<List<FavoriteRoute>> =
        favoriteRouteDao.observeAll().map { entities ->
            entities.map { entity ->
                FavoriteRoute(
                    id = entity.id,
                    originStationId = entity.originStationId,
                    originStationName = entity.originStationName,
                    destinationStationId = entity.destinationStationId,
                    destinationStationName = entity.destinationStationName,
                    lineId = entity.lineId,
                    lineName = entity.lineName,
                    createdAt = entity.createdAt
                )
            }
        }.flowOn(Dispatchers.IO)

    override fun isFavorite(originId: String, destinationId: String): Flow<Boolean> =
        favoriteRouteDao.isFavorite(originId, destinationId)

    override suspend fun toggleFavorite(origin: Station, destination: Station) = withContext(Dispatchers.IO) {
        val currentlyFav = favoriteRouteDao.isFavorite(origin.id, destination.id).first()
        if (currentlyFav) {
            favoriteRouteDao.delete(origin.id, destination.id)
        } else {
            val entity = FavoriteRouteEntity(
                id = 0L,
                originStationId = origin.id,
                originStationName = origin.name,
                destinationStationId = destination.id,
                destinationStationName = destination.name,
                lineId = origin.lineId,
                lineName = "",
                createdAt = System.currentTimeMillis()
            )
            favoriteRouteDao.insert(entity)
        }
    }
}

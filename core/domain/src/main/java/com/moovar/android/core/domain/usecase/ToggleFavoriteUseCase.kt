package com.moovar.android.core.domain.usecase

import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.FavoriteRouteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRouteRepository: FavoriteRouteRepository
) {
    suspend operator fun invoke(origin: Station, destination: Station? = null, lineName: String = "") {
        favoriteRouteRepository.toggleFavorite(origin, destination, lineName)
    }
}

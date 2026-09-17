package com.moovar.android.core.domain.usecase

import com.moovar.android.core.domain.repository.FavoriteRouteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val favoriteRouteRepository: FavoriteRouteRepository
) {
    operator fun invoke(originId: String, destinationId: String): Flow<Boolean> =
        favoriteRouteRepository.isFavorite(originId, destinationId)
}

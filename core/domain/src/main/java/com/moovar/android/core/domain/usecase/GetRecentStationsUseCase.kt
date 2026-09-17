package com.moovar.android.core.domain.usecase

import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.RecentStationRepository
import javax.inject.Inject

class GetRecentStationsUseCase @Inject constructor(
    private val recentStationRepository: RecentStationRepository
) {
    suspend operator fun invoke(): List<Station> =
        recentStationRepository.getRecentStations()
}

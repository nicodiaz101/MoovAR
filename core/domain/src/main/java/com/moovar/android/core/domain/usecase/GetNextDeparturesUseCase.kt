package com.moovar.android.core.domain.usecase

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.DepartureRepository
import com.moovar.android.core.domain.repository.RecentStationRepository
import java.time.LocalDateTime
import javax.inject.Inject

class GetNextDeparturesUseCase @Inject constructor(
    private val departureRepository: DepartureRepository,
    private val recentStationRepository: RecentStationRepository
) {
    suspend operator fun invoke(
        origin: Station,
        destination: Station? = null,
        departureTime: LocalDateTime = LocalDateTime.now()
    ): Result<List<Departure>> {
        recentStationRepository.saveRecentStation(origin)
        if (destination != null) {
            recentStationRepository.saveRecentStation(destination)
        }

        return departureRepository.getDepartures(origin.id, destination?.id, departureTime)
    }
}

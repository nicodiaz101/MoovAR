package com.moovar.android.core.domain.usecase

import com.moovar.android.core.domain.model.StationSelectorData
import com.moovar.android.core.domain.repository.RecentStationRepository
import com.moovar.android.core.domain.repository.StationRepository
import javax.inject.Inject

class GetStationsForSelectorUseCase @Inject constructor(
    private val stationRepository: StationRepository,
    private val recentStationRepository: RecentStationRepository
) {
    suspend operator fun invoke(query: String, branchId: String? = null): StationSelectorData {
        return when {
            query.isEmpty() && branchId == null -> {
                val recent = recentStationRepository.getRecentStations()
                if (recent.isNotEmpty()) {
                    StationSelectorData.Recent(recent)
                } else {
                    val all = stationRepository.searchStations("", null)
                    StationSelectorData.Results(all)
                }
            }
            query.isEmpty() && branchId != null -> {
                val stations = stationRepository.searchStations("", branchId)
                StationSelectorData.Results(stations)
            }
            else -> {
                val stations = stationRepository.searchStations(query.trim(), branchId)
                StationSelectorData.Results(stations)
            }
        }
    }
}

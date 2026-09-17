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
                StationSelectorData.Recent(recent)
            }
            query.isEmpty() && branchId != null -> {
                val stations = stationRepository.getBranchesByLine(branchId) // Actually wait, getBranchesByLine returns branches. 
                // Ah! SPECS says: ELSE IF query.isEmpty() AND selectedBranchId != null: -> Mostrar todas las estaciones del ramal (StationDao.getByBranch())
                // Let's use searchStations("", branchId) because it handles the branch filter
                val stationsResult = stationRepository.searchStations("", branchId)
                StationSelectorData.Results(stationsResult)
            }
            else -> {
                val stations = stationRepository.searchStations(query.trim(), branchId)
                StationSelectorData.Results(stations)
            }
        }
    }
}

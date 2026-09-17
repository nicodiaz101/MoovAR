package com.moovar.android.core.domain.usecase

import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.repository.StationRepository
import javax.inject.Inject

class GetBranchesByLineUseCase @Inject constructor(
    private val stationRepository: StationRepository
) {
    suspend operator fun invoke(lineId: String): List<Branch> =
        stationRepository.getBranchesByLine(lineId)
}

package com.moovar.android.core.domain.usecase

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.repository.JourneyRepository
import javax.inject.Inject

class GetJourneyStopsUseCase @Inject constructor(
    private val journeyRepository: JourneyRepository
) {
    suspend operator fun invoke(serviceId: String): Result<List<JourneyStop>> =
        journeyRepository.getJourney(serviceId)
}

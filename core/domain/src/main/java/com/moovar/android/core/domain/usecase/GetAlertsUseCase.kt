package com.moovar.android.core.domain.usecase

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.ServiceAlert
import com.moovar.android.core.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository
) {
    operator fun invoke(lineId: String? = null): Flow<Result<List<ServiceAlert>>> =
        alertRepository.observeAlerts(lineId)
}

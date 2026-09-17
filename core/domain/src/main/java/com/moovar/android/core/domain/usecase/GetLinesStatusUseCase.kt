package com.moovar.android.core.domain.usecase

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.Line
import com.moovar.android.core.domain.repository.LineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLinesStatusUseCase @Inject constructor(
    private val lineRepository: LineRepository
) {
    operator fun invoke(): Flow<Result<List<Line>>> = lineRepository.observeLines()
}

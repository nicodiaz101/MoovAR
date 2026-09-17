package com.moovar.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.Line
import com.moovar.android.core.domain.usecase.GetLinesStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val lines: List<Line> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLinesStatusUseCase: GetLinesStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getLinesStatusUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { result ->
                    _uiState.update {
                        when (result) {
                            is Result.Success -> it.copy(lines = result.data, isLoading = false, error = null)
                            is Result.Error -> it.copy(error = result.message, isLoading = false)
                            is Result.Loading -> it.copy(isLoading = true)
                        }
                    }
                }
        }
    }
}

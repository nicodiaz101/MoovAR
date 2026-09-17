package com.moovar.android.feature.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.ServiceAlert
import com.moovar.android.core.domain.usecase.GetAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val allAlerts: List<ServiceAlert> = emptyList(),
    val filteredAlerts: List<ServiceAlert> = emptyList(),
    val filterQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lineId: String? = savedStateHandle["lineId"]

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            getAlertsUseCase(lineId)
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { result ->
                    _uiState.update { state ->
                        when (result) {
                            is Result.Success -> {
                                val data = result.data
                                state.copy(
                                    allAlerts = data,
                                    filteredAlerts = if (state.filterQuery.isEmpty()) data else data.filter {
                                        it.title.contains(state.filterQuery, ignoreCase = true) ||
                                        it.description.contains(state.filterQuery, ignoreCase = true)
                                    },
                                    isLoading = false,
                                    error = null
                                )
                            }
                            is Result.Error -> state.copy(error = result.message, isLoading = false)
                            is Result.Loading -> state.copy(isLoading = true)
                        }
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                filterQuery = query,
                filteredAlerts = if (query.isEmpty()) {
                    state.allAlerts
                } else {
                    state.allAlerts.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
                    }
                }
            )
        }
    }
}

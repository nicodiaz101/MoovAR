package com.moovar.android.feature.departures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moovar.android.core.domain.model.StationSelectorData
import com.moovar.android.core.domain.usecase.GetStationsForSelectorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StationSelectorUiState(
    val searchQuery: String = "",
    val selectedBranchId: String? = null,
    val stationData: StationSelectorData = StationSelectorData.Recent(emptyList()),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class StationSelectorViewModel @Inject constructor(
    private val getStationsForSelectorUseCase: GetStationsForSelectorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationSelectorUiState())
    val uiState: StateFlow<StationSelectorUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedBranchIdFlow = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                searchQueryFlow.debounce(300L),
                selectedBranchIdFlow
            ) { query, branchId -> query to branchId }
                .distinctUntilChanged()
                .collect { (query, branchId) ->
                    try {
                        val data = getStationsForSelectorUseCase(query, branchId)
                        _uiState.update { it.copy(stationData = data, isLoading = false) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isLoading = true) }
        searchQueryFlow.value = query
    }
    
    fun onBranchChipSelected(branchId: String?) {
        _uiState.update { it.copy(selectedBranchId = branchId, isLoading = true) }
        selectedBranchIdFlow.value = branchId
    }
}

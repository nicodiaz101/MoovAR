package com.moovar.android.feature.departures

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.model.Coordinates
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.DepartureMode
import com.moovar.android.core.domain.model.Line
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.usecase.GetAlertsUseCase
import com.moovar.android.core.domain.usecase.GetBranchesByLineUseCase
import com.moovar.android.core.domain.usecase.GetLinesStatusUseCase
import com.moovar.android.core.domain.usecase.GetNextDeparturesUseCase
import com.moovar.android.core.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class DeparturesUiState(
    val line: Line? = null,
    val branches: List<Branch> = emptyList(),
    val selectedBranch: Branch? = null,
    val originStation: Station? = null,
    val destinationStation: Station? = null,
    val departures: List<Departure> = emptyList(),
    val hasActiveAlerts: Boolean = false,
    val isLoadingDepartures: Boolean = false,
    val departureMode: DepartureMode = DepartureMode.NOW,
    val scheduledTime: LocalDateTime? = null,
    val error: String? = null
)

sealed class DeparturesUiEvent {
    data class NavigateToJourney(val serviceId: String, val line: Line) : DeparturesUiEvent()
    data class NavigateToMap(val coordinates: Coordinates) : DeparturesUiEvent()
    data class NavigateToAlerts(val lineId: String) : DeparturesUiEvent()
}

@HiltViewModel
class DeparturesViewModel @Inject constructor(
    private val getLinesStatusUseCase: GetLinesStatusUseCase,
    private val getBranchesByLineUseCase: GetBranchesByLineUseCase,
    private val getNextDeparturesUseCase: GetNextDeparturesUseCase,
    private val getAlertsUseCase: GetAlertsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lineId: String = checkNotNull(savedStateHandle["lineId"])

    private val _uiState = MutableStateFlow(DeparturesUiState())
    val uiState: StateFlow<DeparturesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DeparturesUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DeparturesUiEvent> = _events.asSharedFlow()

    init {
        loadLine()
        loadBranches()
        checkAlerts()
    }

    private fun loadLine() {
        viewModelScope.launch(Dispatchers.Default) {
            getLinesStatusUseCase()
                .catch { /* ignore */ }
                .collect { result ->
                    if (result is Result.Success) {
                        result.data.find { it.id == lineId }?.let { line ->
                            _uiState.update { it.copy(line = line) }
                        }
                    }
                }
        }
    }

    private fun loadBranches() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val branches = getBranchesByLineUseCase(lineId)
                _uiState.update { it.copy(branches = branches) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun checkAlerts() {
        viewModelScope.launch(Dispatchers.Default) {
            getAlertsUseCase(lineId)
                .catch { /* ignore */ }
                .collect { result ->
                    _uiState.update { 
                        it.copy(hasActiveAlerts = result is Result.Success && result.data.isNotEmpty()) 
                    }
                }
        }
    }

    fun onOriginSelected(station: Station) {
        _uiState.update { it.copy(originStation = station) }
        triggerSearchIfReady()
    }

    fun onDestinationSelected(station: Station) {
        _uiState.update { it.copy(destinationStation = station) }
        triggerSearchIfReady()
    }

    fun onBranchChipSelected(branch: Branch?) {
        _uiState.update { it.copy(selectedBranch = branch) }
    }

    fun onSwapStations() {
        _uiState.update { 
            val temp = it.originStation
            it.copy(
                originStation = it.destinationStation,
                destinationStation = temp
            )
        }
        triggerSearchIfReady()
    }

    private fun triggerSearchIfReady() {
        val origin = _uiState.value.originStation
        val destination = _uiState.value.destinationStation
        
        if (origin != null && destination != null) {
            _uiState.update { it.copy(isLoadingDepartures = true, error = null) }
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val result = getNextDeparturesUseCase(
                        origin = origin,
                        destination = destination,
                        departureTime = _uiState.value.scheduledTime ?: LocalDateTime.now()
                    )
                    
                    _uiState.update { state ->
                        when (result) {
                            is Result.Success -> state.copy(departures = result.data, isLoadingDepartures = false)
                            is Result.Error -> state.copy(error = result.message, isLoadingDepartures = false)
                            is Result.Loading -> state.copy(isLoadingDepartures = true)
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message, isLoadingDepartures = false) }
                }
            }
        }
    }

    fun onTicketClicked(departure: Departure) {
        viewModelScope.launch {
            _uiState.value.line?.let { line ->
                _events.emit(DeparturesUiEvent.NavigateToJourney(departure.serviceId, line))
            }
        }
    }

    fun onMapButtonClicked(departure: Departure) {
        viewModelScope.launch {
            departure.vehicleCoordinates?.let {
                _events.emit(DeparturesUiEvent.NavigateToMap(it))
            }
        }
    }
    
    fun onAlertsBannerClicked() {
        viewModelScope.launch {
            _events.emit(DeparturesUiEvent.NavigateToAlerts(lineId))
        }
    }
    
    fun onToggleFavorite() {
        val origin = _uiState.value.originStation
        val destination = _uiState.value.destinationStation
        if (origin != null && destination != null) {
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    toggleFavoriteUseCase(origin, destination)
                } catch (e: Exception) {
                    // Ignore error for now
                }
            }
        }
    }
}

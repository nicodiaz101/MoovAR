package com.moovar.android.feature.journey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.usecase.GetJourneyDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JourneyHeader(
    val branchName: String,
    val serviceType: String,
    val platform: String?,
    val departureTime: String,
    val currentStatus: String,
    val destination: String = ""
)

data class JourneyUiState(
    val lineInfo: String = "",
    val serviceHeader: JourneyHeader? = null,
    val stops: List<JourneyStop> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class JourneyViewModel @Inject constructor(
    private val getJourneyDetailsUseCase: GetJourneyDetailsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serviceId: String = checkNotNull(savedStateHandle["serviceId"])

    private val _uiState = MutableStateFlow(JourneyUiState())
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    init {
        loadJourney()
    }

    fun loadJourney() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = getJourneyDetailsUseCase(serviceId)
                _uiState.update { state ->
                    when (result) {
                        is Result.Success -> {
                            val details = result.data
                            state.copy(
                                serviceHeader = JourneyHeader(
                                    branchName = details.branchName,
                                    serviceType = details.serviceType,
                                    platform = details.platform,
                                    departureTime = details.departureTime,
                                    currentStatus = details.currentStatus,
                                    destination = details.destination
                                ),
                                stops = details.stops,
                                isLoading = false,
                                error = null
                            )
                        }
                        is Result.Error -> state.copy(error = result.message, isLoading = false)
                        is Result.Loading -> state.copy(isLoading = true)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}

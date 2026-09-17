package com.moovar.android.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.moovar.android.core.domain.model.Coordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class MapUiState(
    val coordinates: Coordinates?,
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lat: Double? = savedStateHandle.get<Float>("lat")?.toDouble()
    private val lon: Double? = savedStateHandle.get<Float>("lon")?.toDouble()

    private val _uiState = MutableStateFlow(
        MapUiState(
            coordinates = if (lat != null && lon != null) Coordinates(lat, lon) else null
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
}

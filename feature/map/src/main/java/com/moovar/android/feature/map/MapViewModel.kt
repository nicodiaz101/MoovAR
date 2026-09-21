package com.moovar.android.feature.map

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.moovar.android.core.domain.model.Coordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLDecoder
import javax.inject.Inject

@Immutable
data class MapUiState(
    val coordinates: Coordinates?,
    val title: String = "Ubicación del tren",
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lat: Double? = savedStateHandle.get<String>("lat")?.toDoubleOrNull()
        ?: savedStateHandle.get<Float>("lat")?.toDouble()
    private val lon: Double? = savedStateHandle.get<String>("lon")?.toDoubleOrNull()
        ?: savedStateHandle.get<Float>("lon")?.toDouble()
    private val rawTitle: String? = savedStateHandle.get<String>("title")
    private val title: String = try {
        if (!rawTitle.isNullOrBlank()) URLDecoder.decode(rawTitle, "UTF-8") else "Ubicación del tren"
    } catch (_: Exception) {
        rawTitle ?: "Ubicación del tren"
    }

    private val _uiState = MutableStateFlow(
        MapUiState(
            coordinates = if (lat != null && lon != null) Coordinates(lat, lon) else null,
            title = title
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
}

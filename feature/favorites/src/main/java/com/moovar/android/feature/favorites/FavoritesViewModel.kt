package com.moovar.android.feature.favorites

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moovar.android.core.domain.model.FavoriteRoute
import com.moovar.android.core.domain.repository.FavoriteRouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class FavoritesUiState(
    val favorites: List<FavoriteRoute> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRouteRepository: FavoriteRouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoriteRouteRepository.observeFavorites().collect { list ->
                _uiState.update { it.copy(favorites = list, isLoading = false) }
            }
        }
    }

    fun removeFavorite(route: FavoriteRoute) {
        viewModelScope.launch {
            favoriteRouteRepository.deleteFavoriteById(route.id)
        }
    }
}

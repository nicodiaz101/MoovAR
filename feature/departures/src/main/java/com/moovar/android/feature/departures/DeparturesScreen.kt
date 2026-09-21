package com.moovar.android.feature.departures

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moovar.android.core.domain.model.DepartureMode
import com.moovar.android.feature.departures.components.AlertsBanner
import com.moovar.android.feature.departures.components.DepartureTicketCard
import com.moovar.android.feature.departures.components.FilterChipsRow
import com.moovar.android.feature.departures.components.StationInputField

import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelector: (isOrigin: Boolean) -> Unit,
    onNavigateToAlerts: (lineId: String) -> Unit = {},
    onNavigateToJourney: (serviceId: String) -> Unit = {},
    viewModel: DeparturesViewModel = hiltViewModel(),
    selectedOriginId: String? = null,
    selectedOriginName: String? = null,
    selectedDestId: String? = null,
    selectedDestName: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DeparturesUiEvent.NavigateToAlerts -> onNavigateToAlerts(event.lineId)
                is DeparturesUiEvent.NavigateToJourney -> onNavigateToJourney(event.serviceId)
                is DeparturesUiEvent.NavigateToMap -> {}
            }
        }
    }

    LaunchedEffect(selectedOriginId, selectedOriginName) {
        if (!selectedOriginId.isNullOrEmpty() && !selectedOriginName.isNullOrEmpty()) {
            viewModel.onOriginSelected(selectedOriginId, selectedOriginName)
        }
    }

    LaunchedEffect(selectedDestId, selectedDestName) {
        if (!selectedDestId.isNullOrEmpty() && !selectedDestName.isNullOrEmpty()) {
            viewModel.onDestinationSelected(selectedDestId, selectedDestName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.line?.name ?: "Horarios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onToggleFavorite) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorito")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.hasActiveAlerts) {
                AlertsBanner(onBannerClick = viewModel::onAlertsBannerClicked)
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                StationInputField(
                    value = uiState.originStation?.name ?: "",
                    label = "Origen",
                    isDestination = false,
                    onClick = { onNavigateToSelector(true) },
                    onClearClick = viewModel::onClearOrigin
                )
                Spacer(modifier = Modifier.height(8.dp))
                StationInputField(
                    value = uiState.destinationStation?.name ?: "",
                    label = "Destino",
                    isDestination = true,
                    onClick = { onNavigateToSelector(false) },
                    onClearClick = viewModel::onClearDestination,
                    onSwapClick = viewModel::onSwapStations
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                RadioButton(selected = uiState.departureMode == DepartureMode.NOW, onClick = { /* TODO */ })
                Text("Salir ahora")
                RadioButton(selected = uiState.departureMode == DepartureMode.SCHEDULED, onClick = { /* TODO */ })
                Text("Programar")
            }

            when {
                uiState.isLoadingDepartures -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                uiState.originStation == null -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selecciona una estación de origen para consultar las próximas salidas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                uiState.error != null -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Error al cargar las próximas partidas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                uiState.departures.isEmpty() -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron servicios próximos en este momento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    val groupedDepartures = androidx.compose.runtime.remember(uiState.departures) {
                        uiState.departures.groupBy { it.direction }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                    ) {
                        groupedDepartures.forEach { (direction, departuresInGroup) ->
                            if (direction.isNotEmpty()) {
                                item(key = "header_$direction") {
                                    androidx.compose.material3.Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = direction.uppercase(),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            items(departuresInGroup, key = { it.serviceId }) { departure ->
                                DepartureTicketCard(
                                    departure = departure,
                                    onCardClick = { viewModel.onTicketClicked(departure) },
                                    onMapClick = { viewModel.onMapButtonClicked(departure) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.moovar.android.feature.departures

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelector: (isOrigin: Boolean) -> Unit,
    viewModel: DeparturesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    onClearClick = { /* TODO */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
                StationInputField(
                    value = uiState.destinationStation?.name ?: "",
                    label = "Destino",
                    isDestination = true,
                    onClick = { onNavigateToSelector(false) },
                    onClearClick = { /* TODO */ },
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

            FilterChipsRow(
                branches = uiState.branches,
                selectedBranch = uiState.selectedBranch,
                onBranchSelected = viewModel::onBranchChipSelected
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.departures) { departure ->
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

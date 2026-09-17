package com.moovar.android.feature.departures

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.model.StationSelectorData
import com.moovar.android.feature.departures.components.StationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSelectorScreen(
    onStationSelected: (Station) -> Unit,
    viewModel: StationSelectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Seleccionar estación") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar estación...") },
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val data = uiState.stationData
                if (data is StationSelectorData.Recent) {
                    item {
                        Text(
                            text = "Recientes",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(data.stations) { station ->
                        StationItem(
                            station = station,
                            onClick = { onStationSelected(station) }
                        )
                    }
                } else if (data is StationSelectorData.Results) {
                    items(data.stations) { station ->
                        StationItem(
                            station = station,
                            onClick = { onStationSelected(station) }
                        )
                    }
                }
            }
        }
    }
}

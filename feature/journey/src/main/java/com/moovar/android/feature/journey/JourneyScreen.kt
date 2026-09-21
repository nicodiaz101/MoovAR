package com.moovar.android.feature.journey

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moovar.android.feature.journey.components.JourneyHeaderCard
import com.moovar.android.feature.journey.components.StopTimelineList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: (lat: Double, lon: Double, title: String?) -> Unit = { _, _, _ -> },
    viewModel: JourneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trainTitle = uiState.serviceHeader?.let { "Tren a ${it.destination} (${it.branchName})" } ?: "Tren en tiempo real"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recorrido del servicio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (uiState.trainCoordinates != null) {
                        IconButton(
                            onClick = {
                                val coords = uiState.trainCoordinates!!
                                onNavigateToMap(coords.latitude, coords.longitude, trainTitle)
                            }
                        ) {
                            Icon(Icons.Outlined.Map, contentDescription = "Ver en mapa")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.stops.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "Error al cargar el recorrido del servicio.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::loadJourney) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        uiState.serviceHeader?.let {
                            JourneyHeaderCard(header = it)
                        }
                        StopTimelineList(
                            stops = uiState.stops,
                            trainCoordinates = uiState.trainCoordinates,
                            onMapClick = { coords ->
                                onNavigateToMap(coords.latitude, coords.longitude, trainTitle)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

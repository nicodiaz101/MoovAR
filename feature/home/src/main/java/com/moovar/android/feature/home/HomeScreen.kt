package com.moovar.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.feature.home.components.LineStatusItem
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToDepartures: (String) -> Unit,
    navigateToAlerts: (String?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf<NetworkType?>(null) }
    
    val greeting = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 6..12 -> "¡Buenos días!"
            in 13..19 -> "¡Buenas tardes!"
            else -> "¡Buenas noches!"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estado de líneas en tiempo real",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        val filteredLines = remember(uiState.lines, selectedFilter) {
            val base = if (selectedFilter != null) {
                uiState.lines.filter { it.networkType == selectedFilter }
            } else {
                uiState.lines
            }
            base.sortedBy { if (it.networkType == NetworkType.SUBTE) 1 else 0 }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Todas las líneas") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == NetworkType.TREN,
                        onClick = { selectedFilter = NetworkType.TREN },
                        label = { Text("Trenes") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == NetworkType.SUBTE,
                        onClick = { selectedFilter = NetworkType.SUBTE },
                        label = { Text("Subtes") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredLines) { line ->
                    LineStatusItem(
                        line = line,
                        onLineClick = navigateToDepartures,
                        onStatusClick = navigateToAlerts
                    )
                }
            }
        }
    }
}

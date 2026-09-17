package com.moovar.android.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 6..12 -> "¡Buenos días!"
        in 13..19 -> "¡Buenas tardes!"
        else -> "¡Buenas noches!"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = greeting) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO: Nav to favorites */ },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") },
                    label = { Text("Favoritos") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navigateToAlerts(null) },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alertas") },
                    label = { Text("Alertas") }
                )
            }
        }
    ) { padding ->
        val sortedLines = uiState.lines.sortedBy { if (it.networkType == NetworkType.SUBTE) 1 else 0 }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(sortedLines) { line ->
                LineStatusItem(
                    line = line,
                    onLineClick = navigateToDepartures,
                    onStatusClick = navigateToAlerts
                )
                Divider()
            }
        }
    }
}

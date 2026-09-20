package com.moovar.android

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.HiltAndroidApp
import com.moovar.android.feature.home.HomeScreen
import com.moovar.android.feature.home.theme.TransportArTheme
import com.moovar.android.feature.departures.DeparturesScreen
import com.moovar.android.feature.departures.StationSelectorScreen
import com.moovar.android.feature.alerts.AlertsScreen
import com.moovar.android.feature.journey.JourneyScreen
import com.moovar.android.feature.map.MapScreen

@HiltAndroidApp
class MoovArApp : Application()

@Composable
fun MoovArAppContent() {
    val navController = rememberNavController()
    TransportArTheme {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    navigateToDepartures = { lineId -> navController.navigate("departures/$lineId") },
                    navigateToAlerts = { lineId -> navController.navigate(if (lineId != null) "alerts/$lineId" else "alerts") }
                )
            }
            
            composable("departures/{lineId}") { backStackEntry ->
                val lineId = backStackEntry.arguments?.getString("lineId") ?: ""
                DeparturesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSelector = { isOrigin -> navController.navigate("stationSelector/$lineId") }
                )
            }

            composable("stationSelector/{lineId}") { backStackEntry ->
                val lineId = backStackEntry.arguments?.getString("lineId") ?: ""
                StationSelectorScreen(
                    onStationSelected = { station -> 
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_station_id", station.id)
                        navController.popBackStack()
                    }
                )
            }

            composable("alerts") {
                AlertsScreen()
            }
            
            composable("alerts/{lineId}") {
                AlertsScreen()
            }
            
            composable("journey/{serviceId}") {
                JourneyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable("map") {
                MapScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

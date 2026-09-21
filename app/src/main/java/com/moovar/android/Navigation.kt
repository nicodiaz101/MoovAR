package com.moovar.android

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moovar.android.feature.alerts.AlertsScreen
import com.moovar.android.feature.departures.DeparturesScreen
import com.moovar.android.feature.departures.StationSelectorScreen
import com.moovar.android.feature.favorites.FavoritesScreen
import com.moovar.android.feature.home.HomeScreen
import com.moovar.android.feature.journey.JourneyScreen
import com.moovar.android.feature.map.MapScreen
import com.moovar.android.feature.home.theme.TransportArTheme

@Composable
fun MoovArAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    TransportArTheme {
        Scaffold(
            bottomBar = {
                if (currentRoute == "home" || currentRoute == "favorites" || currentRoute == "alerts" || currentRoute == "alerts/{lineId}") {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                            label = { Text("Inicio") },
                            selected = currentRoute == "home",
                            onClick = {
                                if (currentRoute != "home") {
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") },
                            label = { Text("Favoritos") },
                            selected = currentRoute == "favorites",
                            onClick = {
                                if (currentRoute != "favorites") {
                                    navController.navigate("favorites") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Notifications, contentDescription = "Alertas") },
                            label = { Text("Alertas") },
                            selected = currentRoute == "alerts" || currentRoute == "alerts/{lineId}",
                            onClick = {
                                if (currentRoute != "alerts") {
                                    navController.navigate("alerts") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable("home") {
                    HomeScreen(
                        navigateToDepartures = { lineId -> navController.navigate("departures/$lineId") },
                        navigateToAlerts = { lineId -> navController.navigate(if (lineId != null) "alerts/$lineId" else "alerts") }
                    )
                }

                composable("favorites") {
                    FavoritesScreen(
                        onNavigateToDepartures = { lineId, originId, originName, destId, destName ->
                            navController.navigate("departures/$lineId")
                            navController.currentBackStackEntry?.savedStateHandle?.set("selected_origin_id", originId)
                            navController.currentBackStackEntry?.savedStateHandle?.set("selected_origin_name", originName)
                            navController.currentBackStackEntry?.savedStateHandle?.set("selected_dest_id", destId)
                            navController.currentBackStackEntry?.savedStateHandle?.set("selected_dest_name", destName)
                        }
                    )
                }

                composable(
                    route = "departures/{lineId}",
                    arguments = listOf(navArgument("lineId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val lineId = backStackEntry.arguments?.getString("lineId") ?: ""
                    val selectedOriginId by backStackEntry.savedStateHandle.getStateFlow<String?>("selected_origin_id", null).collectAsStateWithLifecycle()
                    val selectedOriginName by backStackEntry.savedStateHandle.getStateFlow<String?>("selected_origin_name", null).collectAsStateWithLifecycle()
                    val selectedDestId by backStackEntry.savedStateHandle.getStateFlow<String?>("selected_dest_id", null).collectAsStateWithLifecycle()
                    val selectedDestName by backStackEntry.savedStateHandle.getStateFlow<String?>("selected_dest_name", null).collectAsStateWithLifecycle()

                    DeparturesScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSelector = { isOrigin -> navController.navigate("stationSelector/$lineId/$isOrigin") },
                        onNavigateToAlerts = { alertLineId -> navController.navigate("alerts/$alertLineId") },
                        onNavigateToJourney = { serviceId ->
                            val safeId = java.net.URLEncoder.encode(serviceId, "UTF-8")
                            navController.navigate("journey/$safeId")
                        },
                        selectedOriginId = selectedOriginId,
                        selectedOriginName = selectedOriginName,
                        selectedDestId = selectedDestId,
                        selectedDestName = selectedDestName
                    )
                }

                composable(
                    route = "stationSelector/{lineId}/{isOrigin}",
                    arguments = listOf(
                        navArgument("lineId") { type = NavType.StringType },
                        navArgument("isOrigin") { type = NavType.BoolType }
                    )
                ) { backStackEntry ->
                    val isOrigin = backStackEntry.arguments?.getBoolean("isOrigin") ?: true
                    StationSelectorScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onStationSelected = { station ->
                            val prevHandle = navController.previousBackStackEntry?.savedStateHandle
                            if (isOrigin) {
                                prevHandle?.set("selected_origin_id", station.id)
                                prevHandle?.set("selected_origin_name", station.name)
                            } else {
                                prevHandle?.set("selected_dest_id", station.id)
                                prevHandle?.set("selected_dest_name", station.name)
                            }
                            navController.popBackStack()
                        }
                    )
                }

                composable("alerts") {
                    AlertsScreen(
                        onBackClick = null
                    )
                }

                composable(
                    route = "alerts/{lineId}",
                    arguments = listOf(navArgument("lineId") { type = NavType.StringType })
                ) {
                    AlertsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "journey/{serviceId}",
                    arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
                ) {
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
}

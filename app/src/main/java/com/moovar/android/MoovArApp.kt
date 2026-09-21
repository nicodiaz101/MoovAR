package com.moovar.android

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moovar.android.core.database.DatabaseSeeder
import com.moovar.android.feature.alerts.AlertsScreen
import com.moovar.android.feature.departures.DeparturesScreen
import com.moovar.android.feature.departures.StationSelectorScreen
import com.moovar.android.feature.favorites.FavoritesScreen
import com.moovar.android.feature.home.HomeScreen
import com.moovar.android.feature.home.theme.TransportArTheme
import com.moovar.android.feature.journey.JourneyScreen
import com.moovar.android.feature.map.MapScreen
import com.moovar.android.core.domain.repository.AlertRepository
import com.moovar.android.core.domain.repository.LineRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoovArApp : Application() {
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder
    @Inject
    lateinit var alertRepository: AlertRepository
    @Inject
    lateinit var lineRepository: LineRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            databaseSeeder.seedInitialData()
            try {
                lineRepository.refreshLines()
            } catch (_: Exception) {
            }
            try {
                alertRepository.refreshAlerts()
            } catch (_: Exception) {
            }
        }
    }
}


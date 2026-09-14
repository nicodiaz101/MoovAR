package com.transportar.android

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TransportArApp : Application()

@Composable
fun TransportArAppContent() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            // BottomNavigationBar placeholder
            Text("Bottom Navigation Placeholder")
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                Text("Home Screen Placeholder")
            }
        }
    }
}

package com.moovar.android.feature.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubicación del tren") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { context ->
                Configuration.getInstance().userAgentValue = "TransportAR/1.0"
                MapView(context).apply {
                    setMultiTouchControls(true)
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(14.0)
                }
            },
            update = { mapView ->
                uiState.coordinates?.let { coords ->
                    val geoPoint = GeoPoint(coords.latitude, coords.longitude)
                    mapView.controller.setCenter(geoPoint)

                    mapView.overlays.clear()
                    val marker = Marker(mapView).apply {
                        position = geoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Formación"
                    }
                    mapView.overlays.add(marker)
                    mapView.invalidate()
                }
            }
        )
    }
}

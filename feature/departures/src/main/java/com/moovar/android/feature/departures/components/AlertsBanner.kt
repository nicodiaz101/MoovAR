package com.moovar.android.feature.departures.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AlertsBanner(onBannerClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Hay alertas que pueden afectar tu viaje.",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onBannerClick) {
            Icon(Icons.Default.Add, contentDescription = "Ver alertas", tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

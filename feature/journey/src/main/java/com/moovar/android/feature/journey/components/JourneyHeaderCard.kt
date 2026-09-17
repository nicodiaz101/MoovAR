package com.moovar.android.feature.journey.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.moovar.android.feature.journey.JourneyHeader

@Composable
fun JourneyHeaderCard(header: JourneyHeader, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = header.departureTime,
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            color = Color.Gray,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
            )

            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Ramal: ${header.branchName}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Servicio: ${header.serviceType}", style = MaterialTheme.typography.bodyMedium)
                    if (header.platform != null) {
                        Text(text = "Andén: ${header.platform}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = header.currentStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { /* Share */ },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Compartir")
                }
            }
        }
    }
}

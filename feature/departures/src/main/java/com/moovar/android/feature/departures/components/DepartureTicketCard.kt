package com.moovar.android.feature.departures.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.NetworkType

@Composable
fun DepartureTicketCard(
    departure: Departure,
    onCardClick: () -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showMapButton = remember(departure) {
        !departure.isTerminus &&
        departure.vehicleCoordinates != null &&
        departure.networkType != NetworkType.SUBTE
    }

    ElevatedCard(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = departure.minutesAway.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "MINUTOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
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

            Box(modifier = Modifier.weight(0.65f).padding(16.dp)) {
                Column {
                    LabeledDetail(label = "Ramal:", value = departure.branchName)
                    LabeledDetail(label = "Destino:", value = departure.destination)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = departure.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                if (showMapButton) {
                    IconButton(
                        onClick = onMapClick,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(imageVector = Icons.Outlined.Map, contentDescription = "Ver en mapa")
                    }
                }
            }
        }
    }
}

@Composable
fun LabeledDetail(label: String, value: String) {
    Row {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

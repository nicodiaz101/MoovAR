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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
        !departure.isCancelled &&
        departure.vehicleCoordinates != null
    }

    val leftBgColor = when {
        departure.isCancelled -> MaterialTheme.colorScheme.errorContainer
        departure.isTerminus -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val leftTextColor = when {
        departure.isCancelled -> MaterialTheme.colorScheme.onErrorContainer
        departure.isTerminus -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    ElevatedCard(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(0.36f)
                    .background(leftBgColor)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        departure.isCancelled -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = leftTextColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "CANCELADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = leftTextColor
                            )
                        }
                        departure.isTerminus -> {
                            Text(
                                text = "SALE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = leftTextColor
                            )
                            Text(
                                text = departure.scheduledTime,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = leftTextColor
                            )
                            Text(
                                text = "EN CABECERA",
                                style = MaterialTheme.typography.labelSmall,
                                color = leftTextColor
                            )
                        }
                        else -> {
                            Text(
                                text = departure.minutesAway.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                color = leftTextColor
                            )
                            Text(
                                text = "MINUTOS",
                                style = MaterialTheme.typography.labelSmall,
                                color = leftTextColor
                            )
                            Text(
                                text = departure.scheduledTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = leftTextColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
            )

            Box(modifier = Modifier.weight(0.64f).padding(14.dp)) {
                Column {
                    LabeledDetail(label = "Ramal:", value = departure.branchName)
                    LabeledDetail(label = "Destino:", value = departure.destination)
                    val platform = departure.platform
                    if (platform != null && platform != "-") {
                        LabeledDetail(label = "Andén:", value = platform)
                    }
                    val serviceType = departure.serviceType
                    if (serviceType != null) {
                        LabeledDetail(label = "Servicio:", value = serviceType)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = departure.status,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (departure.isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
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

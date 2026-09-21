package com.moovar.android.feature.journey.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.StopState

@Composable
fun StopTimelineItem(
    stop: JourneyStop,
    isFirst: Boolean,
    isLast: Boolean,
    onMapClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCurrent = stop.stopState == StopState.CURRENT
    val isPast = stop.stopState == StopState.PAST
    val alpha = if (isPast) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = if (isCurrent) 8.dp else 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineNode(isFirst = isFirst, isLast = isLast, stopState = stop.stopState)
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.stationName,
                    style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.Bold else if (isPast) FontWeight.Normal else FontWeight.Medium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (stop.isTerminus) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Terminal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (isCurrent) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    onClick = { onMapClick?.invoke() },
                    enabled = onMapClick != null,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsTransit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (stop.isTerminus) "TREN EN ANDÉN" else "TREN EN ESTACIÓN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (onMapClick != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = "Ver mapa",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        val scheduledTime = stop.scheduledTime
        if (!isPast && scheduledTime != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = scheduledTime,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InTransitTimelineItem(
    nextStationName: String,
    nextScheduledTime: String?,
    onMapClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InTransitTimelineNode()
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "En camino hacia $nextStationName",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                onClick = { onMapClick?.invoke() },
                enabled = onMapClick != null,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsTransit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TREN EN TIEMPO REAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (onMapClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = "Ver mapa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (nextScheduledTime != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Próx. $nextScheduledTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun InTransitTimelineNode() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val infiniteTransition = rememberInfiniteTransition(label = "transitPulse")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val strokeWidth = 3.dp.toPx()
            val centerX = size.width / 2

            // Top segment connecting to previous station
            drawLine(
                color = Color.LightGray,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height / 2),
                strokeWidth = strokeWidth
            )
            // Bottom segment connecting to next station
            drawLine(
                color = Color.LightGray,
                start = Offset(centerX, size.height / 2),
                end = Offset(centerX, size.height),
                strokeWidth = strokeWidth
            )
        }

        // Soft pulsing glow
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(primaryContainer.copy(alpha = auraAlpha), CircleShape)
        )

        // Train node
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsTransit,
                contentDescription = "Tren en camino",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun TimelineNode(isFirst: Boolean, isLast: Boolean, stopState: StopState) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val strokeWidth = 3.dp.toPx()
            val centerX = size.width / 2

            if (!isFirst) {
                drawLine(
                    color = Color.LightGray,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height / 2),
                    strokeWidth = strokeWidth
                )
            }
            if (!isLast) {
                drawLine(
                    color = Color.LightGray,
                    start = Offset(centerX, size.height / 2),
                    end = Offset(centerX, size.height),
                    strokeWidth = strokeWidth
                )
            }

            if (stopState == StopState.FUTURE) {
                drawCircle(
                    color = Color.Gray,
                    radius = 5.dp.toPx(),
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        when (stopState) {
            StopState.PAST -> {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.Gray.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            StopState.CURRENT -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsTransit,
                        contentDescription = "Tren aquí",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            StopState.FUTURE -> {}
        }
    }
}

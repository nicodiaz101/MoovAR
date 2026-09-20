package com.moovar.android.feature.journey.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.StopState

@Composable
fun StopTimelineItem(
    stop: JourneyStop,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = if (stop.stopState == StopState.PAST) 0.4f else 1f
    val nodeColor = when (stop.stopState) {
        StopState.CURRENT -> MaterialTheme.colorScheme.primary
        StopState.FUTURE -> MaterialTheme.colorScheme.outline
        StopState.PAST -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineNode(isFirst = isFirst, isLast = isLast, color = nodeColor, stopState = stop.stopState)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = stop.stationName, modifier = Modifier.weight(1f))
        
        val scheduledTime = stop.scheduledTime
        if (stop.stopState != StopState.PAST && scheduledTime != null) {
            Text(text = scheduledTime, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TimelineNode(isFirst: Boolean, isLast: Boolean, color: Color, stopState: StopState) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val strokeWidth = 2.dp.toPx()
            val centerX = size.width / 2
            
            if (!isFirst) {
                drawLine(
                    color = Color.Gray,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height / 2),
                    strokeWidth = strokeWidth
                )
            }
            if (!isLast) {
                drawLine(
                    color = Color.Gray,
                    start = Offset(centerX, size.height / 2),
                    end = Offset(centerX, size.height),
                    strokeWidth = strokeWidth
                )
            }

            if (stopState == StopState.FUTURE) {
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    style = Stroke(width = strokeWidth)
                )
            } else {
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx()
                )
            }
        }

        when (stopState) {
            StopState.PAST -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            StopState.CURRENT -> {
                Icon(
                    imageVector = Icons.Default.Train,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            else -> {}
        }
    }
}

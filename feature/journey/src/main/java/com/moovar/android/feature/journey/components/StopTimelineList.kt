package com.moovar.android.feature.journey.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovar.android.core.domain.model.Coordinates
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.StopState

@Composable
fun StopTimelineList(
    stops: List<JourneyStop>,
    trainCoordinates: Coordinates? = null,
    onMapClick: ((Coordinates) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasCurrentStop = stops.any { it.stopState == StopState.CURRENT }
    val inTransitIdx = if (!hasCurrentStop) {
        stops.indexOfFirst { it.stopState == StopState.FUTURE }
    } else {
        -1
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        itemsIndexed(stops, key = { index, stop -> "${stop.stationName}_${stop.scheduledTime}_$index" }) { index, stop ->
            val targetCoords = if (stop.stopState == StopState.CURRENT) {
                trainCoordinates ?: stop.coordinates
            } else {
                stop.coordinates
            }

            StopTimelineItem(
                stop = stop,
                isFirst = index == 0,
                isLast = index == stops.lastIndex,
                onMapClick = if (targetCoords != null && onMapClick != null) {
                    { onMapClick(targetCoords) }
                } else null
            )

            // Step in-between stations when the train is in transit
            if (inTransitIdx > 0 && index == inTransitIdx - 1) {
                val nextStop = stops[inTransitIdx]
                val transitCoords = trainCoordinates ?: nextStop.coordinates ?: stop.coordinates
                InTransitTimelineItem(
                    nextStationName = nextStop.stationName,
                    nextScheduledTime = nextStop.scheduledTime,
                    onMapClick = if (transitCoords != null && onMapClick != null) {
                        { onMapClick(transitCoords) }
                    } else null
                )
            }
        }
    }
}

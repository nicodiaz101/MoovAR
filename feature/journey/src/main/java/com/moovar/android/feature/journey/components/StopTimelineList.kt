package com.moovar.android.feature.journey.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moovar.android.core.domain.model.JourneyStop

@Composable
fun StopTimelineList(stops: List<JourneyStop>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(stops, key = { _, stop -> stop.stationName }) { index, stop ->
            StopTimelineItem(
                stop = stop,
                isFirst = index == 0,
                isLast = index == stops.lastIndex
            )
        }
    }
}

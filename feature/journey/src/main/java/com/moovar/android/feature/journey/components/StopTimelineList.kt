package com.moovar.android.feature.journey.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovar.android.core.domain.model.JourneyStop

@Composable
fun StopTimelineList(stops: List<JourneyStop>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        itemsIndexed(stops, key = { index, stop -> "${stop.stationName}_${stop.scheduledTime}_$index" }) { index, stop ->
            StopTimelineItem(
                stop = stop,
                isFirst = index == 0,
                isLast = index == stops.lastIndex
            )
        }
    }
}

package com.moovar.android.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moovar.android.core.domain.model.Line

@Composable
fun LineStatusItem(
    line: Line,
    onLineClick: (String) -> Unit,
    onStatusClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(line.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onLineClick(line.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = parsedColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = line.name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = line.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        StatusBadge(
            status = line.status,
            onClick = { onStatusClick(line.id) }
        )
    }
}

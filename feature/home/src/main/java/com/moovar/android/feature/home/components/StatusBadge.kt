package com.moovar.android.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moovar.android.core.domain.model.LineStatus
import com.moovar.android.feature.home.theme.StatusCancelado
import com.moovar.android.feature.home.theme.StatusDemorado
import com.moovar.android.feature.home.theme.StatusNormal
import com.moovar.android.feature.home.theme.StatusSinServicio

@Composable
fun StatusBadge(
    status: LineStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val (color, text) = when (status) {
        LineStatus.NORMAL -> StatusNormal to "Normal"
        LineStatus.DEMORADO -> StatusDemorado to "Demoras"
        LineStatus.CANCELADO -> StatusCancelado to "Cancelado"
        LineStatus.SIN_SERVICIO -> StatusSinServicio to "Interrumpido"
        LineStatus.DESCONOCIDO -> Color.Gray to "Sin datos"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = color.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

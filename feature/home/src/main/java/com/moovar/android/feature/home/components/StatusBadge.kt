package com.moovar.android.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    val (color, icon, text) = when (status) {
        LineStatus.NORMAL -> Triple(StatusNormal, Icons.Default.CheckCircle, "Normal")
        LineStatus.DEMORADO -> Triple(StatusDemorado, Icons.Default.Warning, "Demorado")
        LineStatus.CANCELADO -> Triple(StatusCancelado, Icons.Default.Error, "Cancelado")
        LineStatus.SIN_SERVICIO -> Triple(StatusSinServicio, Icons.Default.Info, "Sin servicio")
        LineStatus.DESCONOCIDO -> Triple(Color.Gray, Icons.Default.Info, "Desconocido")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

package com.moovar.android.feature.departures.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StationInputField(
    value: String,
    label: String,
    isDestination: Boolean,
    onClick: () -> Unit,
    onClearClick: () -> Unit,
    onSwapClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clickable { onClick() }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false, 
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClearClick) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                } else if (isDestination && onSwapClick != null) {
                    IconButton(onClick = onSwapClick) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Intercambiar")
                    }
                }
            }
        )
    }
}

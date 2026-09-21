package com.moovar.android.core.domain.model

import androidx.compose.runtime.Immutable

@androidx.compose.runtime.Immutable
data class VehicleLocation(
    val vehicleId: String,
    val tripId: String,
    val stopId: String,
    val currentStatus: String,
    val hasCoordinates: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

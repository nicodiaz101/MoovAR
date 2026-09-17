package com.moovar.android.core.domain.model

data class SubteArrival(
    val stopId: String,
    val tripId: String,
    val arrivalDelay: Int,
    val arrivalTime: Long,
    val scheduleRelationship: String
)

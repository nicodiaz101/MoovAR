package com.moovar.android.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recent_stations", indices = [Index("accessedAt")])
data class RecentStationEntity(
    @PrimaryKey val stationId: String,
    val stationName: String,
    val lineId: String,
    val lineName: String,
    val accessedAt: Long
)

package com.transportar.android.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_routes",
    indices = [Index(value = ["originStationId", "destinationStationId"], unique = true)]
)
data class FavoriteRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originStationId: String,
    val originStationName: String,
    val destinationStationId: String,
    val destinationStationName: String,
    val lineId: String,
    val lineName: String,
    val createdAt: Long
)

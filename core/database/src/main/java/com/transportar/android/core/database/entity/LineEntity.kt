package com.transportar.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lines")
data class LineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val networkType: NetworkType,
    val iconResName: String,
    val colorHex: String,
    val status: LineStatus,
    val statusMessage: String?,
    val lastUpdatedAt: Long,
    val sortOrder: Int
)

enum class NetworkType { TREN, SUBTE }
enum class LineStatus { NORMAL, DEMORADO, CANCELADO, SIN_SERVICIO, DESCONOCIDO }

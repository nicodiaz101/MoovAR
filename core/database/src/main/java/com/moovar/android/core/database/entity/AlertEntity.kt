package com.moovar.android.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alerts",
    foreignKeys = [ForeignKey(
        entity = LineEntity::class,
        parentColumns = ["id"],
        childColumns = ["lineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lineId"), Index("cachedAt")]
)
data class AlertEntity(
    @PrimaryKey val id: String,
    val lineId: String,
    val branchId: String?,
    val title: String,
    val description: String,
    val severity: AlertSeverity,
    val publishedAt: Long,
    val expiresAt: Long?,
    val cachedAt: Long
)

enum class AlertSeverity { INFO, WARNING, CRITICAL }

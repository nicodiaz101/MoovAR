package com.transportar.android.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "branches",
    foreignKeys = [ForeignKey(
        entity = LineEntity::class,
        parentColumns = ["id"],
        childColumns = ["lineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lineId")]
)
data class BranchEntity(
    @PrimaryKey val id: String,
    val lineId: String,
    val name: String,
    val originTerminus: String,
    val destinationTerminus: String,
    val subteLineColor: String?
)

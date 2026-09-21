package com.moovar.android.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stations",
    foreignKeys = [ForeignKey(
        entity = BranchEntity::class,
        parentColumns = ["id"],
        childColumns = ["branchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("branchId"), Index("name"), Index("lineId")]
)
data class StationEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val lineId: String,
    val name: String,
    val networkType: NetworkType,
    val gtfsStopId: String?,
    val sequenceInBranch: Int,
    val isTerminus: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

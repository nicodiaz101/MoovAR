package com.transportar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.transportar.android.core.database.entity.StationEntity

@Dao
interface StationDao {
    @Query("""
        SELECT * FROM stations 
        WHERE (:branchId IS NULL OR branchId = :branchId)
        AND name LIKE '%' || :query || '%'
        ORDER BY sequenceInBranch ASC
    """)
    suspend fun search(query: String, branchId: String?): List<StationEntity>

    @Query("SELECT * FROM stations WHERE branchId = :branchId ORDER BY sequenceInBranch ASC")
    suspend fun getByBranch(branchId: String): List<StationEntity>

    @Upsert
    suspend fun upsertAll(stations: List<StationEntity>)
}

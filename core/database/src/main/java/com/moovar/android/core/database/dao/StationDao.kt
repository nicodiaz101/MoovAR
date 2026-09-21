package com.moovar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.moovar.android.core.database.entity.StationEntity

@Dao
interface StationDao {
    @Query("SELECT * FROM stations")
    suspend fun getAll(): List<StationEntity>
    @Query("""
        SELECT * FROM stations 
        WHERE (:branchId IS NULL OR branchId = :branchId)
        AND name LIKE '%' || :query || '%'
        ORDER BY sequenceInBranch ASC
    """)
    suspend fun search(query: String, branchId: String?): List<StationEntity>

    @Query("SELECT * FROM stations WHERE branchId = :branchId ORDER BY sequenceInBranch ASC")
    suspend fun getByBranch(branchId: String): List<StationEntity>

    @Query("SELECT * FROM stations WHERE (:branchId IS NULL OR branchId = :branchId) ORDER BY sequenceInBranch ASC")
    suspend fun getByBranchOptional(branchId: String?): List<StationEntity>

    @Query("SELECT * FROM stations WHERE lineId = :lineId ORDER BY sequenceInBranch ASC")
    suspend fun getByLine(lineId: String): List<StationEntity>

    @Query("SELECT * FROM stations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StationEntity?

    @Query("SELECT * FROM stations WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<StationEntity>

    @Upsert
    suspend fun upsertAll(stations: List<StationEntity>)
}

package com.moovar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.moovar.android.core.database.entity.BranchEntity

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE lineId = :lineId")
    suspend fun getByLine(lineId: String): List<BranchEntity>

    @Upsert
    suspend fun upsertAll(branches: List<BranchEntity>)
}

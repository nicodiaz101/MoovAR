package com.transportar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.transportar.android.core.database.entity.LineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineDao {
    @Query("SELECT * FROM lines ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<LineEntity>>

    @Upsert
    suspend fun upsertAll(lines: List<LineEntity>)
}

package com.moovar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.moovar.android.core.database.entity.LineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineDao {
    @Query("SELECT * FROM lines ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<LineEntity>>

    @Query("SELECT * FROM lines ORDER BY sortOrder ASC")
    suspend fun getAll(): List<LineEntity>

    @Query("SELECT COUNT(*) FROM lines")
    suspend fun count(): Int

    @Query("SELECT * FROM lines WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LineEntity?

    @Upsert
    suspend fun upsertAll(lines: List<LineEntity>)
}

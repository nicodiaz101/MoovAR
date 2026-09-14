package com.moovar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.moovar.android.core.database.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE (:lineId IS NULL OR lineId = :lineId) ORDER BY publishedAt DESC")
    fun observeAlerts(lineId: String?): Flow<List<AlertEntity>>

    @Query("DELETE FROM alerts WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)

    @Upsert
    suspend fun upsertAll(alerts: List<AlertEntity>)
}

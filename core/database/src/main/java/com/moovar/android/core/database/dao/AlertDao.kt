package com.moovar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.moovar.android.core.database.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("""
        SELECT * FROM alerts 
        WHERE (:lineId IS NULL OR :lineId = '' OR lineId = :lineId) 
        ORDER BY 
            CASE severity 
                WHEN 'CRITICAL' THEN 1 
                WHEN 'WARNING' THEN 2 
                ELSE 3 
            END ASC, 
            publishedAt DESC
    """)
    fun observeAlerts(lineId: String?): Flow<List<AlertEntity>>

    @Query("DELETE FROM alerts WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(alerts: List<AlertEntity>)
    @Transaction
    suspend fun replaceAll(alerts: List<AlertEntity>) {
        deleteAll()
        upsertAll(alerts)
    }
}

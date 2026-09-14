package com.moovar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.moovar.android.core.database.entity.RecentStationEntity

@Dao
interface RecentStationDao {
    @Query("SELECT * FROM recent_stations ORDER BY accessedAt DESC LIMIT 5")
    suspend fun getRecent(): List<RecentStationEntity>

    @Upsert
    suspend fun upsert(station: RecentStationEntity)

    @Query("DELETE FROM recent_stations WHERE stationId NOT IN (SELECT stationId FROM recent_stations ORDER BY accessedAt DESC LIMIT 10)")
    suspend fun pruneOld()
}

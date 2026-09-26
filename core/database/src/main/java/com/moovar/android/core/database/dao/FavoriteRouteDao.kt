package com.moovar.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moovar.android.core.database.entity.FavoriteRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRouteDao {
    @Query("SELECT * FROM favorite_routes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(route: FavoriteRouteEntity)

    @Query("DELETE FROM favorite_routes WHERE originStationId = :oId AND destinationStationId = :dId")
    suspend fun delete(oId: String, dId: String)

    @Query("DELETE FROM favorite_routes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_routes WHERE originStationId = :oId AND destinationStationId = :dId)")
    fun isFavorite(oId: String, dId: String): Flow<Boolean>
}

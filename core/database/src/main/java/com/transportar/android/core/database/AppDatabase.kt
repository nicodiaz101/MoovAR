package com.transportar.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.transportar.android.core.database.dao.AlertDao
import com.transportar.android.core.database.dao.BranchDao
import com.transportar.android.core.database.dao.FavoriteRouteDao
import com.transportar.android.core.database.dao.LineDao
import com.transportar.android.core.database.dao.RecentStationDao
import com.transportar.android.core.database.dao.StationDao
import com.transportar.android.core.database.entity.AlertEntity
import com.transportar.android.core.database.entity.BranchEntity
import com.transportar.android.core.database.entity.FavoriteRouteEntity
import com.transportar.android.core.database.entity.LineEntity
import com.transportar.android.core.database.entity.RecentStationEntity
import com.transportar.android.core.database.entity.StationEntity

@Database(
    entities = [
        LineEntity::class,
        BranchEntity::class,
        StationEntity::class,
        AlertEntity::class,
        RecentStationEntity::class,
        FavoriteRouteEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lineDao(): LineDao
    abstract fun branchDao(): BranchDao
    abstract fun stationDao(): StationDao
    abstract fun alertDao(): AlertDao
    abstract fun recentStationDao(): RecentStationDao
    abstract fun favoriteRouteDao(): FavoriteRouteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Stub for future migration
            }
        }
    }
}

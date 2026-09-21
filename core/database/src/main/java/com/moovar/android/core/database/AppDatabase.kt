package com.moovar.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moovar.android.core.database.dao.AlertDao
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.FavoriteRouteDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.dao.RecentStationDao
import com.moovar.android.core.database.dao.StationDao
import com.moovar.android.core.database.entity.AlertEntity
import com.moovar.android.core.database.entity.BranchEntity
import com.moovar.android.core.database.entity.FavoriteRouteEntity
import com.moovar.android.core.database.entity.LineEntity
import com.moovar.android.core.database.entity.RecentStationEntity
import com.moovar.android.core.database.entity.StationEntity

@Database(
    entities = [
        LineEntity::class,
        BranchEntity::class,
        StationEntity::class,
        AlertEntity::class,
        RecentStationEntity::class,
        FavoriteRouteEntity::class
    ],
    version = 2,
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
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stations_lineId` ON `stations` (`lineId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_stations_accessedAt` ON `recent_stations` (`accessedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_routes_createdAt` ON `favorite_routes` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_alerts_cachedAt` ON `alerts` (`cachedAt`)")
            }
        }
    }
}

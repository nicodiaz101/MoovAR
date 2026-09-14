package com.transportar.android.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "transportar_database"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seeder is called here ideally via WorkManager or Coroutine
                // For simplicity in this DI setup, we might seed on first load somewhere else,
                // or use a Provider<AppDatabase> to avoid circular dependency.
            }
        })
        .build()
    }

    @Provides
    fun provideLineDao(db: AppDatabase) = db.lineDao()

    @Provides
    fun provideBranchDao(db: AppDatabase) = db.branchDao()

    @Provides
    fun provideStationDao(db: AppDatabase) = db.stationDao()

    @Provides
    fun provideAlertDao(db: AppDatabase) = db.alertDao()

    @Provides
    fun provideRecentStationDao(db: AppDatabase) = db.recentStationDao()

    @Provides
    fun provideFavoriteRouteDao(db: AppDatabase) = db.favoriteRouteDao()
    
    @Provides
    @Singleton
    fun provideDatabaseSeeder(@ApplicationContext context: Context, db: AppDatabase): DatabaseSeeder {
        return DatabaseSeeder(context, db)
    }
}

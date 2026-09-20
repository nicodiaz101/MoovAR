package com.moovar.android.di

import com.moovar.android.core.domain.repository.*
import com.moovar.android.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLineRepository(
        impl: LineRepositoryImpl
    ): LineRepository

    @Binds
    @Singleton
    abstract fun bindStationRepository(
        impl: StationRepositoryImpl
    ): StationRepository

    @Binds
    @Singleton
    abstract fun bindDepartureRepository(
        impl: DepartureRepositoryImpl
    ): DepartureRepository

    @Binds
    @Singleton
    abstract fun bindJourneyRepository(
        impl: JourneyRepositoryImpl
    ): JourneyRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        impl: AlertRepositoryImpl
    ): AlertRepository

    @Binds
    @Singleton
    abstract fun bindRecentStationRepository(
        impl: RecentStationRepositoryImpl
    ): RecentStationRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRouteRepository(
        impl: FavoriteRouteRepositoryImpl
    ): FavoriteRouteRepository
}

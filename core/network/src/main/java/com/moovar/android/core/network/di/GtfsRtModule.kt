package com.moovar.android.core.network.di

import com.moovar.android.core.network.gtfsrt.GtfsRtDataSource
import com.moovar.android.core.network.gtfsrt.GtfsRtEndpointUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GtfsRtModule {

    @Provides
    @Singleton
    @GtfsRtEndpointUrl
    fun provideGtfsRtEndpointUrl(): String = "https://cdn.buenosaires.gob.ar/datosabiertos/datasets/sbase/subtes-estado/estado.pb"

    @Provides
    @Singleton
    fun provideGtfsRtDataSource(
        okHttpClient: OkHttpClient,
        @GtfsRtEndpointUrl endpointUrl: String
    ): GtfsRtDataSource {
        return GtfsRtDataSource(okHttpClient, endpointUrl)
    }
}

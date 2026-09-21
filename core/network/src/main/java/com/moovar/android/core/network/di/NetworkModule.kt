package com.moovar.android.core.network.di

import com.moovar.android.core.network.sofse.TokenStorage
import com.moovar.android.core.network.sofse.api.SofseApiService
import com.moovar.android.core.network.sofse.api.SofseAuthApi
import com.moovar.android.core.network.sofse.interceptor.StaticHeadersInterceptor
import com.moovar.android.core.network.sofse.interceptor.TokenManagerInterceptor
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api-servicios.sofse.gob.ar/v1/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideStaticHeadersInterceptor(): StaticHeadersInterceptor = StaticHeadersInterceptor()

    @Provides
    @Singleton
    @Named("AuthOkHttpClient")
    fun provideAuthOkHttpClient(
        staticHeadersInterceptor: StaticHeadersInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(staticHeadersInterceptor)

        // No logging interceptor here, or add it only in debug if needed
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideSofseAuthApi(
        @Named("AuthOkHttpClient") okHttpClient: OkHttpClient,
        json: Json
    ): SofseAuthApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SofseAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTokenManagerInterceptor(
        tokenStorage: TokenStorage,
        authApi: SofseAuthApi
    ): TokenManagerInterceptor {
        return TokenManagerInterceptor(tokenStorage, authApi)
    }

    @Provides
    @Singleton
    @Named("MainOkHttpClient")
    fun provideMainOkHttpClient(
        staticHeadersInterceptor: StaticHeadersInterceptor,
        tokenManagerInterceptor: TokenManagerInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(staticHeadersInterceptor)
            .addInterceptor(tokenManagerInterceptor)

        if (com.moovar.android.core.network.BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization")
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideSofseApiService(
        @Named("MainOkHttpClient") okHttpClient: OkHttpClient,
        json: Json
    ): SofseApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SofseApiService::class.java)
    }
}

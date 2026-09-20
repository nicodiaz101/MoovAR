package com.moovar.android.di

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.*
import com.moovar.android.core.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FakeRepositoryModule {

    private val sampleLines = listOf(
        Line("roca", "Línea Roca", NetworkType.TREN, "#0055A5", LineStatus.NORMAL, "Servicio normal"),
        Line("sarmiento", "Línea Sarmiento", NetworkType.TREN, "#00A8E3", LineStatus.NORMAL, "Servicio normal"),
        Line("mitre", "Línea Mitre", NetworkType.TREN, "#008A27", LineStatus.DEMORADO, "Demoras por problemas técnicos"),
        Line("san_martin", "Línea San Martín", NetworkType.TREN, "#E3001B", LineStatus.NORMAL, "Servicio normal"),
        Line("belgrano_sur", "Línea Belgrano Sur", NetworkType.TREN, "#00A9E0", LineStatus.NORMAL, "Servicio normal"),
        Line("belgrano_norte", "Línea Belgrano Norte", NetworkType.TREN, "#E3001B", LineStatus.NORMAL, "Servicio normal"),
        Line("linea_a", "Línea A", NetworkType.SUBTE, "#18B4E9", LineStatus.NORMAL, "Completo entre cabeceras"),
        Line("linea_b", "Línea B", NetworkType.SUBTE, "#E30613", LineStatus.NORMAL, "Completo entre cabeceras"),
        Line("linea_c", "Línea C", NetworkType.SUBTE, "#006C9A", LineStatus.NORMAL, "Completo entre cabeceras"),
        Line("linea_d", "Línea D", NetworkType.SUBTE, "#00845B", LineStatus.NORMAL, "Completo entre cabeceras"),
        Line("linea_e", "Línea E", NetworkType.SUBTE, "#702A8C", LineStatus.DEMORADO, "Frecuencia reducida"),
        Line("linea_h", "Línea H", NetworkType.SUBTE, "#FFCC00", LineStatus.NORMAL, "Completo entre cabeceras")
    )

    @Provides
    @Singleton
    fun provideLineRepository(): LineRepository = object : LineRepository {
        override fun observeLines(): Flow<Result<List<Line>>> = flowOf(Result.Success(sampleLines))
        override suspend fun refreshLines() {}
    }

    @Provides
    @Singleton
    fun provideStationRepository(): StationRepository = object : StationRepository {
        override suspend fun searchStations(query: String, branchId: String?): List<Station> = emptyList()
        override suspend fun getBranchesByLine(lineId: String): List<Branch> = emptyList()
    }

    @Provides
    @Singleton
    fun provideAlertRepository(): AlertRepository = object : AlertRepository {
        override fun observeAlerts(lineId: String?): Flow<Result<List<ServiceAlert>>> = emptyFlow()
        override suspend fun refreshAlerts() {}
    }

    @Provides
    @Singleton
    fun provideDepartureRepository(): DepartureRepository = object : DepartureRepository {
        override suspend fun getDepartures(
            originId: String,
            destinationId: String,
            departureTime: LocalDateTime
        ): Result<List<Departure>> = Result.Success(emptyList())
    }

    @Provides
    @Singleton
    fun provideRecentStationRepository(): RecentStationRepository = object : RecentStationRepository {
        override suspend fun getRecentStations(): List<Station> = emptyList()
        override suspend fun saveRecentStation(station: Station) {}
    }

    @Provides
    @Singleton
    fun provideFavoriteRouteRepository(): FavoriteRouteRepository = object : FavoriteRouteRepository {
        override fun observeFavorites(): Flow<List<FavoriteRoute>> = emptyFlow()
        override fun isFavorite(originId: String, destinationId: String): Flow<Boolean> = flowOf(false)
        override suspend fun toggleFavorite(origin: Station, destination: Station) {}
    }

    @Provides
    @Singleton
    fun provideJourneyRepository(): JourneyRepository = object : JourneyRepository {
        override suspend fun getJourney(serviceId: String): Result<List<JourneyStop>> = Result.Success(emptyList())
    }
}

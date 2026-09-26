package com.moovar.android.core.domain.usecase

import com.moovar.android.core.domain.model.FavoriteRoute
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.FavoriteRouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private lateinit var fakeFavoriteRepo: FakeFavoriteRouteRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    private val origin = Station(
        id = "stn_origin",
        name = "Plaza Constitución",
        branchId = "roca_ezeiza",
        lineId = "roca",
        networkType = NetworkType.TREN,
        gtfsStopId = null,
        sequenceInBranch = 1,
        isTerminus = true,
        coordinates = null
    )

    private val destination = Station(
        id = "stn_dest",
        name = "Ezeiza",
        branchId = "roca_ezeiza",
        lineId = "roca",
        networkType = NetworkType.TREN,
        gtfsStopId = null,
        sequenceInBranch = 15,
        isTerminus = true,
        coordinates = null
    )

    @Before
    fun setup() {
        fakeFavoriteRepo = FakeFavoriteRouteRepository()
        useCase = ToggleFavoriteUseCase(fakeFavoriteRepo)
    }

    @Test
    @Test
    fun `toggleFavorite toggles repository state with origin and destination`() = runBlocking {
        // Initially empty
        assertEquals(0, fakeFavoriteRepo.toggledCount)

        // First toggle
        useCase(origin, destination, "Línea Roca")
        assertEquals(1, fakeFavoriteRepo.toggledCount)

        // Second toggle
        useCase(origin, destination, "Línea Roca")
        assertEquals(2, fakeFavoriteRepo.toggledCount)
    }

    @Test
    fun `toggleFavorite toggles repository state with origin only`() = runBlocking {
        assertEquals(0, fakeFavoriteRepo.toggledCount)

        // Toggle single station
        useCase(origin, null, "Línea Roca")
        assertEquals(1, fakeFavoriteRepo.toggledCount)
    }

    private class FakeFavoriteRouteRepository : FavoriteRouteRepository {
        var toggledCount = 0
        override fun observeFavorites(): Flow<List<FavoriteRoute>> = flowOf(emptyList())
        override fun isFavorite(originId: String, destinationId: String?): Flow<Boolean> = flowOf(false)
        override suspend fun toggleFavorite(origin: Station, destination: Station?, lineName: String) {
            toggledCount++
        }
        override suspend fun deleteFavorite(originId: String, destinationId: String?) {
            toggledCount--
        }
        override suspend fun deleteFavoriteById(id: Long) {
            toggledCount--
        }
    }
}

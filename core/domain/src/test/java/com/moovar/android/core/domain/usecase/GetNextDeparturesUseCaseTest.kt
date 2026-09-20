package com.moovar.android.core.domain.usecase

import com.moovar.android.core.common.Result
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.repository.DepartureRepository
import com.moovar.android.core.domain.repository.RecentStationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class GetNextDeparturesUseCaseTest {

    private lateinit var fakeDepartureRepo: FakeDepartureRepository
    private lateinit var fakeRecentRepo: FakeRecentStationRepository
    private lateinit var useCase: GetNextDeparturesUseCase

    private val originStation = Station(
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

    private val destStation = Station(
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
        fakeDepartureRepo = FakeDepartureRepository()
        fakeRecentRepo = FakeRecentStationRepository()
        useCase = GetNextDeparturesUseCase(fakeDepartureRepo, fakeRecentRepo)
    }

    @Test
    fun `invoking use case saves stations to history before fetching departures`() = runBlocking {
        val result = useCase(originStation, destStation)

        assertTrue(result is Result.Success)
        assertEquals(2, fakeRecentRepo.savedStations.size)
        assertEquals(originStation, fakeRecentRepo.savedStations[0])
        assertEquals(destStation, fakeRecentRepo.savedStations[1])
    }

    private class FakeDepartureRepository : DepartureRepository {
        override suspend fun getDepartures(
            originId: String,
            destinationId: String?,
            departureTime: LocalDateTime
        ): Result<List<Departure>> = Result.Success(emptyList())
    }

    private class FakeRecentStationRepository : RecentStationRepository {
        val savedStations = mutableListOf<Station>()
        override suspend fun getRecentStations(): List<Station> = savedStations
        override suspend fun saveRecentStation(station: Station) {
            savedStations.add(station)
        }
    }
}

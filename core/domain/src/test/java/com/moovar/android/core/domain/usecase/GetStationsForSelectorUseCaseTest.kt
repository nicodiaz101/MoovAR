package com.moovar.android.core.domain.usecase

import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.model.Station
import com.moovar.android.core.domain.model.StationSelectorData
import com.moovar.android.core.domain.repository.RecentStationRepository
import com.moovar.android.core.domain.repository.StationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetStationsForSelectorUseCaseTest {

    private lateinit var fakeStationRepo: FakeStationRepository
    private lateinit var fakeRecentRepo: FakeRecentStationRepository
    private lateinit var useCase: GetStationsForSelectorUseCase

    private val sampleStation = Station(
        id = "stn_1",
        name = "Constitución",
        branchId = "roca_ezeiza",
        lineId = "roca",
        networkType = NetworkType.TREN,
        gtfsStopId = null,
        sequenceInBranch = 1,
        isTerminus = true,
        coordinates = null
    )

    @Before
    fun setup() {
        fakeStationRepo = FakeStationRepository()
        fakeRecentRepo = FakeRecentStationRepository()
        useCase = GetStationsForSelectorUseCase(fakeStationRepo, fakeRecentRepo)
    }

    @Test
    fun `when query is empty and branch is null returns Recent stations`() = runBlocking {
        fakeRecentRepo.recentList = listOf(sampleStation)

        val result = useCase("", null)

        assertTrue(result is StationSelectorData.Recent)
        assertEquals(listOf(sampleStation), (result as StationSelectorData.Recent).stations)
    }

    @Test
    fun `when query is empty and branch is specified returns Results with branch stations`() = runBlocking {
        fakeStationRepo.stations = listOf(sampleStation)

        val result = useCase("", "roca_ezeiza")

        assertTrue(result is StationSelectorData.Results)
        assertEquals(listOf(sampleStation), (result as StationSelectorData.Results).stations)
    }

    @Test
    fun `when query is non-empty returns Results filtered by query`() = runBlocking {
        fakeStationRepo.stations = listOf(sampleStation)

        val result = useCase("Const")

        assertTrue(result is StationSelectorData.Results)
        assertEquals(listOf(sampleStation), (result as StationSelectorData.Results).stations)
    }

    private class FakeStationRepository : StationRepository {
        var stations = emptyList<Station>()
        override suspend fun searchStations(query: String, branchId: String?): List<Station> = stations
        override suspend fun getBranchesByLine(lineId: String): List<com.moovar.android.core.domain.model.Branch> = emptyList()
    }

    private class FakeRecentStationRepository : RecentStationRepository {
        var recentList = emptyList<Station>()
        override suspend fun getRecentStations(): List<Station> = recentList
        override suspend fun saveRecentStation(station: Station) {}
    }
}

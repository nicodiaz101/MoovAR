package com.moovar.android.data.repository

import com.moovar.android.core.common.Result
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.dao.StationDao
import com.moovar.android.core.database.entity.BranchEntity
import com.moovar.android.core.database.entity.LineEntity
import com.moovar.android.core.database.entity.LineStatus
import com.moovar.android.core.database.entity.NetworkType
import com.moovar.android.core.database.entity.StationEntity
import com.moovar.android.core.domain.model.JourneyDetails
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.StopState
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JourneyRepositoryTest {

    private lateinit var journeyDetailsCache: JourneyDetailsCache
    private lateinit var fakeStationDao: FakeStationDao
    private lateinit var fakeBranchDao: FakeBranchDao
    private lateinit var fakeLineDao: FakeLineDao
    private lateinit var fakeSofseApiService: FakeSofseApiService
    private lateinit var repository: JourneyRepositoryImpl

    @Before
    fun setup() {
        journeyDetailsCache = JourneyDetailsCache()
        fakeStationDao = FakeStationDao()
        fakeBranchDao = FakeBranchDao()
        fakeLineDao = FakeLineDao()
        fakeSofseApiService = FakeSofseApiService()
        repository = JourneyRepositoryImpl(
            sofseApiService = fakeSofseApiService,
            stationDao = fakeStationDao,
            branchDao = fakeBranchDao,
            lineDao = fakeLineDao,
            journeyDetailsCache = journeyDetailsCache
        )
    }

    @Test
    fun getJourneyDetails_returnsCachedDetailsDirectly() = runTest {
        val cachedDetails = JourneyDetails(
            branchName = "Retiro - Tigre",
            serviceType = "Regular",
            destination = "Retiro (LGM)",
            platform = "Andén 2",
            departureTime = "18:45",
            currentStatus = "A horario",
            stops = listOf(
                JourneyStop("Tigre", "18:30", StopState.PAST, isTerminus = true),
                JourneyStop("Carupá", "18:35", StopState.CURRENT, isTerminus = false),
                JourneyStop("San Fernando C", "18:45", StopState.FUTURE, isTerminus = false),
                JourneyStop("Retiro (LGM)", "19:30", StopState.FUTURE, isTerminus = true)
            )
        )
        journeyDetailsCache.put("service_123", cachedDetails)

        val result = repository.getJourneyDetails("service_123")
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("Retiro (LGM)", data.destination)
        assertEquals(4, data.stops.size)
        assertEquals("Carupá", data.stops[1].stationName)
        assertEquals(StopState.CURRENT, data.stops[1].stopState)
    }

    @Test
    fun getJourneyDetails_fallbackReversesDirectionTowardsRetiroAndPutsTrainBeforeOrigin() = runTest {
        // Setup line and branch
        val line = LineEntity(
            id = "mitre",
            name = "Línea Mitre",
            shortName = "Mitre",
            networkType = NetworkType.TREN,
            iconResName = "train",
            colorHex = "#0080FF",
            status = LineStatus.NORMAL,
            statusMessage = "Servicio normal",
            lastUpdatedAt = System.currentTimeMillis(),
            sortOrder = 1
        )
        val branch = BranchEntity(
            id = "mitre_tigre",
            lineId = "mitre",
            name = "Retiro - Tigre",
            originTerminus = "Retiro",
            destinationTerminus = "Tigre",
            subteLineColor = null
        )
        // Stations in branch order: Retiro -> Tigre
        val stations = listOf(
            StationEntity(id = "st_retiro", branchId = "mitre_tigre", lineId = "mitre", name = "Retiro", networkType = NetworkType.TREN, gtfsStopId = null, sequenceInBranch = 1, isTerminus = true, latitude = null, longitude = null),
            StationEntity(id = "st_virreyes", branchId = "mitre_tigre", lineId = "mitre", name = "Virreyes", networkType = NetworkType.TREN, gtfsStopId = null, sequenceInBranch = 2, isTerminus = false, latitude = null, longitude = null),
            StationEntity(id = "st_san_fernando", branchId = "mitre_tigre", lineId = "mitre", name = "San Fernando C", networkType = NetworkType.TREN, gtfsStopId = null, sequenceInBranch = 3, isTerminus = false, latitude = null, longitude = null),
            StationEntity(id = "st_carupa", branchId = "mitre_tigre", lineId = "mitre", name = "Carupá", networkType = NetworkType.TREN, gtfsStopId = null, sequenceInBranch = 4, isTerminus = false, latitude = null, longitude = null),
            StationEntity(id = "st_tigre", branchId = "mitre_tigre", lineId = "mitre", name = "Tigre", networkType = NetworkType.TREN, gtfsStopId = null, sequenceInBranch = 5, isTerminus = true, latitude = null, longitude = null)
        )

        fakeLineDao.lines[line.id] = line
        fakeBranchDao.branches[branch.id] = branch
        fakeStationDao.stationsByBranch[branch.id] = stations

        // User is at San Fernando C waiting for a train heading to Retiro (LGM), arriving in 4 minutes
        val serviceId = "srv___mitre_tigre___Retiro (LGM)___st_san_fernando___4___18:30___Andén 2___Regular___A horario___false"
        val result = repository.getJourneyDetails(serviceId)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("Retiro (LGM)", data.destination)

        // The station order must be Tigre -> Carupá -> San Fernando C -> Virreyes -> Retiro
        assertEquals("Tigre", data.stops.first().stationName)
        assertEquals("Retiro", data.stops.last().stationName)

        // Since the train is 4 minutes away from San Fernando C, it must NOT be at Virreyes!
        // It must be at Carupá (before San Fernando C).
        val currentStop = data.stops.find { it.stopState == StopState.CURRENT }
        assertNotNull(currentStop)
        assertEquals("Carupá", currentStop?.stationName)

        // San Fernando C and Virreyes must be in the FUTURE
        val sanFernandoStop = data.stops.find { it.stationName == "San Fernando C" }
        assertEquals(StopState.FUTURE, sanFernandoStop?.stopState)

        val virreyesStop = data.stops.find { it.stationName == "Virreyes" }
        assertEquals(StopState.FUTURE, virreyesStop?.stopState)
    }

    // ==========================================
    // Fakes
    // ==========================================
    private class FakeStationDao : StationDao {
        val stationsByBranch = mutableMapOf<String, List<StationEntity>>()

        override suspend fun search(query: String, branchId: String?): List<StationEntity> =
            stationsByBranch[branchId] ?: emptyList()

        override suspend fun getByBranch(branchId: String): List<StationEntity> =
            stationsByBranch[branchId] ?: emptyList()

        override suspend fun getByBranchOptional(branchId: String?): List<StationEntity> =
            stationsByBranch[branchId] ?: emptyList()

        override suspend fun getByLine(lineId: String): List<StationEntity> = emptyList()
        override suspend fun getById(id: String): StationEntity? = null
        override suspend fun getByIds(ids: List<String>): List<StationEntity> = emptyList()
        override suspend fun upsertAll(stations: List<StationEntity>) {}
    }

    private class FakeBranchDao : BranchDao {
        val branches = mutableMapOf<String, BranchEntity>()
        override suspend fun getByLine(lineId: String): List<BranchEntity> = branches.values.filter { it.lineId == lineId }
        override suspend fun getById(id: String): BranchEntity? = branches[id]
        override suspend fun upsertAll(branches: List<BranchEntity>) {}
    }

    private class FakeLineDao : LineDao {
        val lines = mutableMapOf<String, LineEntity>()
        override fun observeAll(): Flow<List<LineEntity>> = emptyFlow()
        override suspend fun getAll(): List<LineEntity> = lines.values.toList()
        override suspend fun getById(id: String): LineEntity? = lines[id]
        override suspend fun count(): Int = lines.size
        override suspend fun upsertAll(lines: List<LineEntity>) {}
    }

    private class FakeSofseApiService : SofseApiService {
        override suspend fun getGerencias() = emptyList<com.moovar.android.core.network.sofse.dto.GerenciaDto>()
        override suspend fun getRamales(idGerencia: Int?) = emptyList<com.moovar.android.core.network.sofse.dto.RamalDto>()
        override suspend fun getEstaciones(nombre: String?, idRamal: Int?) = emptyList<com.moovar.android.core.network.sofse.dto.EstacionDto>()
        override suspend fun getArribos(stationId: String) = com.moovar.android.core.network.sofse.dto.ArribosResponseDto()
        override suspend fun getLineas() = emptyList<com.moovar.android.core.network.sofse.dto.LineStatusDto>()
        override suspend fun getProximos(originId: String, destinationId: String) = emptyList<com.moovar.android.core.network.sofse.dto.DepartureDto>()
        override suspend fun getRecorrido(serviceId: String) = com.moovar.android.core.network.sofse.dto.JourneyDto("", "", emptyList())
        override suspend fun getRamalesLegacy(lineaId: String) = emptyList<com.moovar.android.core.network.sofse.dto.BranchDto>()
        override suspend fun getEstacionesLegacy(branchId: String) = emptyList<com.moovar.android.core.network.sofse.dto.StationDto>()
        override suspend fun getAlertas(lineId: String?) = emptyList<com.moovar.android.core.network.sofse.dto.AlertDto>()
    }
}

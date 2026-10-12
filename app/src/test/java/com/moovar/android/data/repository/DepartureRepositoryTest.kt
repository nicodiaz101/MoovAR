package com.moovar.android.data.repository

import com.moovar.android.core.common.Result
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.dao.StationDao
import com.moovar.android.core.database.entity.BranchEntity
import com.moovar.android.core.database.entity.LineEntity
import com.moovar.android.core.database.entity.NetworkType
import com.moovar.android.core.database.entity.StationEntity
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.network.sofse.api.SofseApiService
import com.moovar.android.core.network.sofse.dto.AndenDto
import com.moovar.android.core.network.sofse.dto.ArriboDetalleDto
import com.moovar.android.core.network.sofse.dto.ArriboResultDto
import com.moovar.android.core.network.sofse.dto.ArribosResponseDto
import com.moovar.android.core.network.sofse.dto.EstacionParadaDto
import com.moovar.android.core.network.sofse.dto.HorarioDetalleDto
import com.moovar.android.core.network.sofse.dto.PuntoEstacionDto
import com.moovar.android.core.network.sofse.dto.RamalInfo
import com.moovar.android.core.network.sofse.dto.ServicioDetalleDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class DepartureRepositoryTest {

    private lateinit var fakeStationDao: FakeStationDao
    private lateinit var fakeBranchDao: FakeBranchDao
    private lateinit var fakeLineDao: FakeLineDao
    private lateinit var fakeSofseApiService: FakeSofseApiService
    private lateinit var journeyDetailsCache: JourneyDetailsCache
    private lateinit var repository: DepartureRepositoryImpl

    private val retiroMitreCoords = Pair(-34.5919, -58.3749)
    private val lisandroCoords = Pair(-34.5625772, -58.4363821)
    private val belgranoCCoords = Pair(-34.5578, -58.4503)
    private val nunezCoords = Pair(-34.5484856, -58.4627415)

    private val sanFernandoRCoords = Pair(-34.4375312, -58.5539384)
    private val canalSanFernandoCoords = Pair(-34.4299239, -58.5629021)

    @Before
    fun setup() {
        fakeStationDao = FakeStationDao()
        fakeBranchDao = FakeBranchDao()
        fakeLineDao = FakeLineDao()
        fakeSofseApiService = FakeSofseApiService()
        journeyDetailsCache = JourneyDetailsCache()

        // Populate database with Mitre Tigre and Tren de la Costa stations
        val mitreStations = listOf(
            StationEntity("stn_retiro", "mitre_tigre", "mitre", "Retiro (Mitre)", NetworkType.TREN, "332", 1, true, retiroMitreCoords.first, retiroMitreCoords.second),
            StationEntity("stn_lisandro", "mitre_tigre", "mitre", "Lisandro de la Torre", NetworkType.TREN, "229", 2, false, lisandroCoords.first, lisandroCoords.second),
            StationEntity("stn_belgrano_c", "mitre_tigre", "mitre", "Belgrano C", NetworkType.TREN, "34", 3, false, belgranoCCoords.first, belgranoCCoords.second),
            StationEntity("stn_nunez", "mitre_tigre", "mitre", "Núñez", NetworkType.TREN, "287", 4, false, nunezCoords.first, nunezCoords.second),
            StationEntity("stn_san_fernando", "mitre_tigre", "mitre", "San Fernando", NetworkType.TREN, "355", 15, false, -34.4442261, -58.5587749),
            StationEntity("stn_tigre", "mitre_tigre", "mitre", "Tigre", NetworkType.TREN, "389", 17, true, -34.4235341, -58.5817628)
        )
        val costaStations = listOf(
            StationEntity("stn_tdc_maipu", "tren_costa_main", "tren_costa", "Avenida Maipú", NetworkType.TREN, "248", 1, true, -34.5129556, -58.4874923),
            StationEntity("stn_tdc_san_fernando_r", "tren_costa_main", "tren_costa", "San Fernando R", NetworkType.TREN, "356", 9, false, sanFernandoRCoords.first, sanFernandoRCoords.second),
            StationEntity("stn_tdc_canal", "tren_costa_main", "tren_costa", "Canal San Fernando", NetworkType.TREN, "58", 10, false, canalSanFernandoCoords.first, canalSanFernandoCoords.second),
            StationEntity("stn_tdc_delta", "tren_costa_main", "tren_costa", "Delta", NetworkType.TREN, "104", 11, true, -34.4185467, -58.5768043)
        )

        fakeStationDao.stations.addAll(mitreStations)
        fakeStationDao.stations.addAll(costaStations)

        fakeBranchDao.branches["mitre_tigre"] = BranchEntity("mitre_tigre", "mitre", "Retiro - Tigre", "Retiro", "Tigre", null)
        fakeBranchDao.branches["tren_costa_main"] = BranchEntity("tren_costa_main", "tren_costa", "Maipú - Delta", "Maipú", "Delta", null)

        repository = DepartureRepositoryImpl(
            sofseApiService = fakeSofseApiService,
            stationDao = fakeStationDao,
            branchDao = fakeBranchDao,
            lineDao = fakeLineDao,
            journeyDetailsCache = journeyDetailsCache
        )
    }

    @Test
    fun getDepartures_retiroTigre_trainMovingPastRetiro_interpolatesAlongSegment() = runTest {
        // Mock train departed Retiro, heading towards Lisandro de la Torre (L. de la Torre, id: 229)
        fakeSofseApiService.arribosByStation["34"] = ArribosResponseDto(
            results = listOf(
                ArriboResultDto(
                    arribo = ArriboDetalleDto(
                        nombre = "Belgrano C",
                        segundos = 450,
                        salida = HorarioDetalleDto(programada = "2026-10-12T20:24:00.000Z"),
                        anden = AndenDto(1, "1")
                    ),
                    servicio = ServicioDetalleDto(
                        id = "serv_tigre_1",
                        ramal = RamalInfo(5, "Retiro-Tigre"),
                        estaciones = listOf(
                            EstacionParadaDto(nombre = "Retiro (LGM)", idElemento = 332, segundos = 0, salida = HorarioDetalleDto(programada = "2026-10-12T20:24:00.000Z")),
                            EstacionParadaDto(nombre = "L. de la Torre", idElemento = 229, segundos = 270, llegada = HorarioDetalleDto(programada = "2026-10-12T20:33:00.000Z")),
                            EstacionParadaDto(nombre = "Belgrano C", idElemento = 34, segundos = 450, llegada = HorarioDetalleDto(programada = "2026-10-12T20:36:00.000Z")),
                            EstacionParadaDto(nombre = "Núñez", idElemento = 287, segundos = 690, llegada = HorarioDetalleDto(programada = "2026-10-12T20:40:00.000Z"))
                        )
                    )
                )
            )
        )

        // Query departures at an intermediate station
        val result = repository.getDepartures("stn_belgrano_c", departureTime = LocalDateTime.now())
        assertTrue(result is Result.Success)
        val departures = (result as Result.Success<List<Departure>>).data
        val departure = departures.first()

        assertNotNull(departure.vehicleCoordinates)
        val coords = departure.vehicleCoordinates!!

        // Coordinates should NOT be stuck at Retiro terminal
        assertNotEquals(retiroMitreCoords.first, coords.latitude, 0.0001)
        assertNotEquals(retiroMitreCoords.second, coords.longitude, 0.0001)

        // Coordinates should be between Retiro and Lisandro de la Torre
        assertTrue(coords.latitude > retiroMitreCoords.first && coords.latitude < lisandroCoords.first)
        assertTrue(coords.longitude < retiroMitreCoords.second && coords.longitude > lisandroCoords.second)
    }

    @Test
    fun getDepartures_trenDeLaCosta_sanFernandoR_distinctFromCanalSanFernando() = runTest {
        // Mock train arriving at Canal San Fernando, stations include San Fernando R (356) and C. San Fernando (58)
        fakeSofseApiService.arribosByStation["58"] = ArribosResponseDto(
            results = listOf(
                ArriboResultDto(
                    arribo = ArriboDetalleDto(
                        nombre = "Canal San Fernando",
                        segundos = 200,
                        anden = AndenDto(1, "1")
                    ),
                    servicio = ServicioDetalleDto(
                        id = "serv_tdc_1",
                        ramal = RamalInfo(41, "Maipú-Delta"),
                        estaciones = listOf(
                            EstacionParadaDto(nombre = "Maipú", idElemento = 248, segundos = 0),
                            EstacionParadaDto(nombre = "San Fernando R", idElemento = 356, segundos = 0),
                            EstacionParadaDto(nombre = "C. San Fernando", idElemento = 58, segundos = 200),
                            EstacionParadaDto(nombre = "Delta", idElemento = 104, segundos = 600)
                        )
                    )
                )
            )
        )

        val result = repository.getDepartures("stn_tdc_canal", departureTime = LocalDateTime.now())
        assertTrue(result is Result.Success)
        val departures = (result as Result.Success<List<Departure>>).data
        val departure = departures.first()

        assertNotNull(departure.vehicleCoordinates)

        // Verify the cached journey details stops: San Fernando R vs C. San Fernando
        val details = journeyDetailsCache.get(departure.serviceId)
        assertNotNull(details)

        val stopSanFernandoR = details!!.stops.find { it.stationName.contains("San Fernando R") }
        val stopCanal = details.stops.find { it.stationName.contains("San Fernando") && !it.stationName.contains("San Fernando R") }

        assertNotNull(stopSanFernandoR?.coordinates)
        assertNotNull(stopCanal?.coordinates)

        // Both stops MUST have completely distinct coordinates matching their respective physical locations
        assertEquals(sanFernandoRCoords.first, stopSanFernandoR!!.coordinates!!.latitude, 0.001)
        assertEquals(sanFernandoRCoords.second, stopSanFernandoR.coordinates!!.longitude, 0.001)

        assertEquals(canalSanFernandoCoords.first, stopCanal!!.coordinates!!.latitude, 0.001)
        assertEquals(canalSanFernandoCoords.second, stopCanal.coordinates!!.longitude, 0.001)

        assertNotEquals(stopSanFernandoR.coordinates!!.latitude, stopCanal.coordinates!!.latitude, 0.001)
    }

    // ==========================================
    // Fakes
    // ==========================================
    private class FakeStationDao : StationDao {
        val stations = mutableListOf<StationEntity>()

        override suspend fun getAll(): List<StationEntity> = stations.toList()
        override suspend fun getById(id: String): StationEntity? = stations.find { it.id == id }
        override suspend fun search(query: String, branchId: String?): List<StationEntity> =
            stations.filter { (branchId == null || it.branchId == branchId) && it.name.contains(query, ignoreCase = true) }
        override suspend fun getByBranch(branchId: String): List<StationEntity> =
            stations.filter { it.branchId == branchId }
        override suspend fun getByBranchOptional(branchId: String?): List<StationEntity> =
            if (branchId == null) stations else stations.filter { it.branchId == branchId }
        override suspend fun getByLine(lineId: String): List<StationEntity> =
            stations.filter { it.lineId == lineId }
        override suspend fun getByIds(ids: List<String>): List<StationEntity> =
            stations.filter { it.id in ids }
        override suspend fun upsertAll(stations: List<StationEntity>) {
            this.stations.addAll(stations)
        }
    }

    private class FakeBranchDao : BranchDao {
        val branches = mutableMapOf<String, BranchEntity>()
        override suspend fun getAll(): List<BranchEntity> = branches.values.toList()
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
        val arribosByStation = mutableMapOf<String, ArribosResponseDto>()

        override suspend fun getGerencias() = emptyList<com.moovar.android.core.network.sofse.dto.GerenciaDto>()
        override suspend fun getRamales(idGerencia: Int?) = emptyList<com.moovar.android.core.network.sofse.dto.RamalDto>()
        override suspend fun getEstaciones(nombre: String?, idRamal: Int?) = emptyList<com.moovar.android.core.network.sofse.dto.EstacionDto>()
        override suspend fun getArribos(stationId: String): ArribosResponseDto =
            arribosByStation[stationId] ?: ArribosResponseDto()
        override suspend fun getLineas() = emptyList<com.moovar.android.core.network.sofse.dto.LineStatusDto>()
        override suspend fun getProximos(originId: String, destinationId: String) = emptyList<com.moovar.android.core.network.sofse.dto.DepartureDto>()
        override suspend fun getRecorrido(serviceId: String) = com.moovar.android.core.network.sofse.dto.JourneyDto("", "", emptyList())
        override suspend fun getRamalesLegacy(lineaId: String) = emptyList<com.moovar.android.core.network.sofse.dto.BranchDto>()
        override suspend fun getEstacionesLegacy(branchId: String) = emptyList<com.moovar.android.core.network.sofse.dto.StationDto>()
        override suspend fun getAlertas(lineId: String?) = emptyList<com.moovar.android.core.network.sofse.dto.AlertDto>()
    }
}

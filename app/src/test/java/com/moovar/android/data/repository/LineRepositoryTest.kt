package com.moovar.android.data.repository

import com.moovar.android.core.database.DatabaseSeeder
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.entity.BranchEntity
import com.moovar.android.core.database.entity.LineEntity
import com.moovar.android.core.database.entity.LineStatus
import com.moovar.android.core.database.entity.NetworkType
import com.moovar.android.core.network.sofse.api.SofseApiService
import com.moovar.android.core.network.sofse.dto.AlertDto
import com.moovar.android.core.network.sofse.dto.ArribosResponseDto
import com.moovar.android.core.network.sofse.dto.BranchDto
import com.moovar.android.core.network.sofse.dto.DepartureDto
import com.moovar.android.core.network.sofse.dto.EstacionDto
import com.moovar.android.core.network.sofse.dto.GerenciaDto
import com.moovar.android.core.network.sofse.dto.GerenciaEstadoDto
import com.moovar.android.core.network.sofse.dto.JourneyDto
import com.moovar.android.core.network.sofse.dto.LineStatusDto
import com.moovar.android.core.network.sofse.dto.RamalDto
import com.moovar.android.core.network.sofse.dto.SofseAlertaDto
import com.moovar.android.core.network.sofse.dto.StationDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class LineRepositoryTest {

    private lateinit var fakeLineDao: FakeLineTestDao
    private lateinit var fakeBranchDao: FakeBranchTestDao
    private lateinit var fakeSofseApiService: FakeSofseApiTestService
    private lateinit var repository: LineRepositoryImpl

    @Before
    fun setup() {
        fakeLineDao = FakeLineTestDao()
        fakeBranchDao = FakeBranchTestDao()
        fakeSofseApiService = FakeSofseApiTestService()

        fakeLineDao.lines = mutableListOf(
            LineEntity("roca", "Línea Roca", "Roca", NetworkType.TREN, "ic_line_roca", "#0055A5", LineStatus.NORMAL, "Servicio normal", 0L, 1),
            LineEntity("sarmiento", "Línea Sarmiento", "Sarmiento", NetworkType.TREN, "ic_line_sarmiento", "#00A8E3", LineStatus.NORMAL, "Servicio normal", 0L, 2),
            LineEntity("mitre", "Línea Mitre", "Mitre", NetworkType.TREN, "ic_line_mitre", "#008A27", LineStatus.NORMAL, "Servicio normal", 0L, 3),
            LineEntity("san_martin", "Línea San Martín", "San Martín", NetworkType.TREN, "ic_line_san_martin", "#E3001B", LineStatus.NORMAL, "Servicio normal", 0L, 4),
            LineEntity("belgrano_sur", "Línea Belgrano Sur", "Belgrano Sur", NetworkType.TREN, "ic_line_belgrano_sur", "#00A9E0", LineStatus.NORMAL, "Servicio normal", 0L, 5),
            LineEntity("tren_costa", "Tren de la Costa", "Tren de la Costa", NetworkType.TREN, "ic_line_tren_costa", "#008A27", LineStatus.NORMAL, "Servicio normal", 0L, 8)
        )

        repository = LineRepositoryImpl(
            lineDao = fakeLineDao,
            branchDao = fakeBranchDao,
            sofseApiService = fakeSofseApiService,
            databaseSeeder = FakeDatabaseSeeder()
        ).apply {
            clock = Clock.fixed(Instant.parse("2026-09-21T14:40:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
        }
    }

    @Test
    fun refreshLines_futureAlerts_classifiedAsAviso() = runTest {
        // Gerencia 5 is Mitre
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(5, 1, "Mitre", GerenciaEstadoDto(13, "Alertas por ramal", "#435a6c"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[5] = listOf(
            RamalDto(1, 5, "Retiro-J.L. Suárez", 15, 1, 1, null, null, listOf(
                SofseAlertaDto(1, 5, 1, null, null, "El 26/9 y 27/9 inclusive los trenes del ramal Suárez no saldrán ni llegarán a Retiro...", 1, null, 4, null, null)
            )),
            RamalDto(2, 5, "Retiro-Mitre", 11, 1, 1, null, null, listOf(
                SofseAlertaDto(2, 5, 2, null, null, "El 26/9 y 27/9 inclusive los trenes del ramal Bmé. Mitre no saldrán ni llegarán a Retiro...", 1, null, 4, null, null)
            )),
            RamalDto(3, 5, "Retiro-Tigre", 17, 1, 1, null, null, listOf(
                SofseAlertaDto(3, 5, 3, null, null, "El 26/9 y 27/9 inclusive, los trenes del ramal Tigre estarán interrumpidos por obras...", 1, null, 4, null, null)
            ))
        )

        repository.refreshLines()

        val mitre = fakeLineDao.lines.first { it.id == "mitre" }
        assertEquals(LineStatus.AVISO, mitre.status)
        assertEquals("Obras programadas en ramales", mitre.statusMessage)
    }

    @Test
    fun refreshLines_singleBranchInterrupted_classifiedAsDemorado() = runTest {
        // Gerencia 11 is Roca
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(11, 1, "Roca", GerenciaEstadoDto(13, "Alertas por ramal", "#435a6c"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[11] = listOf(
            RamalDto(10, 11, "Constitución-La Plata", 20, 1, 1, null, null, emptyList()),
            RamalDto(11, 11, "Constitución-Ezeiza", 15, 1, 1, null, null, emptyList()),
            RamalDto(12, 11, "Constitución-Lobos", 10, 1, 0, null, null, listOf(
                SofseAlertaDto(10, 11, 12, null, null, "El servicio Cañuelas - Lobos se encuentra interrumpido por obras de renovación de vías.", 1, null, 4, null, null)
            ))
        )

        repository.refreshLines()

        val roca = fakeLineDao.lines.first { it.id == "roca" }
        assertEquals(LineStatus.DEMORADO, roca.status)
        assertEquals("Ramal interrumpido", roca.statusMessage)
    }

    @Test
    fun refreshLines_multipleBranchesInterrupted_classifiedAsDemorado() = runTest {
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(11, 1, "Roca", GerenciaEstadoDto(13, "Alertas por ramal", "#435a6c"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[11] = listOf(
            RamalDto(10, 11, "Constitución-La Plata", 20, 1, 1, null, null, emptyList()),
            RamalDto(11, 11, "Constitución-Ezeiza", 15, 1, 0, null, null, listOf(
                SofseAlertaDto(11, 11, 11, null, null, "Servicio interrumpido", 1, null, 4, null, null)
            )),
            RamalDto(12, 11, "Constitución-Lobos", 10, 1, 0, null, null, listOf(
                SofseAlertaDto(10, 11, 12, null, null, "Servicio interrumpido", 1, null, 4, null, null)
            ))
        )

        repository.refreshLines()

        val roca = fakeLineDao.lines.first { it.id == "roca" }
        assertEquals(LineStatus.DEMORADO, roca.status)
        assertEquals("Ramales interrumpidos", roca.statusMessage)
    }

    @Test
    fun refreshLines_allBranchesInterrupted_classifiedAsSinServicio() = runTest {
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(41, 1, "Tren de la Costa", GerenciaEstadoDto(13, "Interrumpido", "#d32f2f"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[41] = listOf(
            RamalDto(40, 41, "Maipú-Delta", 11, 1, 0, null, null, listOf(
                SofseAlertaDto(40, 41, 40, null, null, "Servicio interrumpido por obras", 1, null, 4, null, null)
            ))
        )

        repository.refreshLines()

        val tdc = fakeLineDao.lines.first { it.id == "tren_costa" }
        assertEquals(LineStatus.SIN_SERVICIO, tdc.status)
        assertEquals("Servicio interrumpido", tdc.statusMessage)
    }

    @Test
    fun refreshLines_singleBranchDelayed_classifiedAsDemorado() = runTest {
        repository.clock = Clock.fixed(Instant.parse("2026-09-21T14:40:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(1, 1, "Sarmiento", GerenciaEstadoDto(13, "Alertas por ramal", "#435a6c"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[1] = listOf(
            RamalDto(20, 1, "Once-Moreno", 16, 1, 1, null, null, emptyList()),
            RamalDto(21, 1, "Moreno-Mercedes", 12, 1, 1, null, null, listOf(
                SofseAlertaDto(20, 1, 21, null, null, "El tren de las 11:35 hs. desde Moreno hacia Luján circula con demoras de 14 minutos aproximadamente por problemas técnicos.", 1, null, 4, null, null)
            ))
        )

        repository.refreshLines()

        val sarmiento = fakeLineDao.lines.first { it.id == "sarmiento" }
        assertEquals(LineStatus.DEMORADO, sarmiento.status)
        assertEquals("Ramal con demoras", sarmiento.statusMessage)
    }

    @Test
    fun refreshLines_singleBranchLimitedRoute_classifiedAsDemorado() = runTest {
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(21, 1, "Belgrano Sur", GerenciaEstadoDto(13, "Alertas por ramal", "#435a6c"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[21] = listOf(
            RamalDto(30, 21, "Buenos Aires-Gonzalez Catán", 18, 1, 1, null, null, emptyList()),
            RamalDto(31, 21, "Buenos Aires-M.C.G. Belgrano", 15, 1, 1, null, null, listOf(
                SofseAlertaDto(30, 21, 31, null, null, "Los trenes del ramal Dr. Sáenz - M.C.G. Belgrano circulan con recorrido limitado entre Tapiales y M.C.G. Belgrano, por obras.", 1, null, 4, null, null)
            ))
        )

        repository.refreshLines()

        val belgranoSur = fakeLineDao.lines.first { it.id == "belgrano_sur" }
        assertEquals(LineStatus.DEMORADO, belgranoSur.status)
        assertEquals("Ramal con recorrido limitado", belgranoSur.statusMessage)
    }
}

private class FakeLineTestDao : LineDao {
    var lines = mutableListOf<LineEntity>()

    override fun observeAll(): Flow<List<LineEntity>> = flowOf(lines)
    override suspend fun getAll(): List<LineEntity> = lines.toList()
    override suspend fun getById(id: String): LineEntity? = lines.find { it.id == id }
    override suspend fun count(): Int = lines.size
    override suspend fun upsertAll(lines: List<LineEntity>) {
        lines.forEach { line ->
            val idx = this.lines.indexOfFirst { it.id == line.id }
            if (idx >= 0) this.lines[idx] = line else this.lines.add(line)
        }
    }
}

private class FakeBranchTestDao : BranchDao {
    override suspend fun getAll(): List<BranchEntity> = emptyList()
    override suspend fun getByLine(lineId: String): List<BranchEntity> = emptyList()
    override suspend fun getById(id: String): BranchEntity? = null
    override suspend fun upsertAll(branches: List<BranchEntity>) {}
}

private class FakeDatabaseSeeder : DatabaseSeeder(null, null) {
    override suspend fun seedInitialData() {}
}

private class FakeSofseApiTestService : SofseApiService {
    var gerencias: List<GerenciaDto> = emptyList()
    val ramalesByGerencia = mutableMapOf<Int, List<RamalDto>>()

    override suspend fun getGerencias(): List<GerenciaDto> = gerencias
    override suspend fun getRamales(idGerencia: Int?): List<RamalDto> =
        if (idGerencia != null) ramalesByGerencia[idGerencia] ?: emptyList() else ramalesByGerencia.values.flatten()
    override suspend fun getEstaciones(nombre: String?, idRamal: Int?): List<EstacionDto> = emptyList()
    override suspend fun getArribos(stationId: String): ArribosResponseDto = ArribosResponseDto()
    override suspend fun getLineas(): List<LineStatusDto> = emptyList()
    override suspend fun getProximos(originId: String, destinationId: String): List<DepartureDto> = emptyList()
    override suspend fun getRecorrido(serviceId: String): JourneyDto = JourneyDto("", "", emptyList())
    override suspend fun getRamalesLegacy(lineaId: String): List<BranchDto> = emptyList()
    override suspend fun getEstacionesLegacy(branchId: String): List<StationDto> = emptyList()
    override suspend fun getAlertas(lineId: String?): List<AlertDto> = emptyList()
}

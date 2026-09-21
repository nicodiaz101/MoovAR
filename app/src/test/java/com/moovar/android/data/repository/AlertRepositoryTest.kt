package com.moovar.android.data.repository

import com.moovar.android.core.common.Result
import com.moovar.android.core.database.dao.AlertDao
import com.moovar.android.core.database.entity.AlertEntity
import com.moovar.android.core.domain.model.AlertSeverity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlertRepositoryTest {

    private lateinit var fakeAlertDao: FakeAlertTestDao
    private lateinit var fakeSofseApiService: FakeSofseAlertTestService
    private lateinit var repository: AlertRepositoryImpl

    @Before
    fun setup() {
        fakeAlertDao = FakeAlertTestDao()
        fakeSofseApiService = FakeSofseAlertTestService()
        repository = AlertRepositoryImpl(
            alertDao = fakeAlertDao,
            sofseApiService = fakeSofseApiService
        )
    }

    @Test
    fun refreshAlerts_populatesAlertsFromSofse() = runTest {
        // Line 11 is Roca
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(11, 1, "Roca", GerenciaEstadoDto(13, "Alertas por ramal", "#435a6c"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[11] = listOf(
            RamalDto(12, 11, "Constitución-Lobos", 10, 1, 0, null, null, listOf(
                SofseAlertaDto(10, 11, 12, null, null, "El servicio Cañuelas - Lobos se encuentra interrumpido por obras.", 1, null, 1, "#c9302c", null)
            ))
        )

        repository.refreshAlerts()

        assertEquals(1, fakeAlertDao.alerts.size)
        val alert = fakeAlertDao.alerts.first()
        assertEquals("roca", alert.lineId)
        assertEquals("roca_12", alert.branchId)
        assertEquals("Línea Roca • Constitución-Lobos", alert.title)
        assertEquals(com.moovar.android.core.database.entity.AlertSeverity.CRITICAL, alert.severity)
    }

    @Test
    fun refreshAlerts_inoperativeBranchWithoutTextAlert_createsAlert() = runTest {
        // Line 21 is Belgrano Sur
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(21, 1, "Belgrano Sur", GerenciaEstadoDto(13, "Alertas", "#435a6c"), emptyList())
        )
        fakeSofseApiService.ramalesByGerencia[21] = listOf(
            RamalDto(31, 21, "González Catán - Navarro", 10, 0, 0, null, null, emptyList())
        )

        repository.refreshAlerts()

        assertEquals(1, fakeAlertDao.alerts.size)
        val alert = fakeAlertDao.alerts.first()
        assertEquals("belgrano_sur", alert.lineId)
        assertEquals("belgrano_sur_31", alert.branchId)
        assertEquals(com.moovar.android.core.database.entity.AlertSeverity.CRITICAL, alert.severity)
    }

    @Test
    fun refreshAlerts_filtersOutCudAlerts() = runTest {
        fakeSofseApiService.gerencias = listOf(
            GerenciaDto(1, 1, "Sarmiento", GerenciaEstadoDto(1, "Normal", "#00A8E3"), listOf(
                SofseAlertaDto(99, 1, null, null, null, "Para acceder al CUD de discapacidad presentar constancia.", 1, null, 4, null, null)
            ))
        )
        fakeSofseApiService.ramalesByGerencia[1] = listOf(
            RamalDto(2, 1, "Once-Moreno", 16, 1, 1, null, null, listOf(
                SofseAlertaDto(101, 1, 2, null, null, "Pase libre por discapacidad CUD en boletería.", 1, null, 4, null, null)
            ))
        )

        repository.refreshAlerts()

        assertTrue(fakeAlertDao.alerts.isEmpty())
    }

    @Test
    fun observeAlerts_mapsToDomainModels() = runTest {
        fakeAlertDao.alerts.add(
            AlertEntity(
                id = "alert_1",
                lineId = "mitre",
                branchId = "mitre_capilla",
                title = "Mitre: Victoria - Capilla",
                description = "Servicio interrumpido",
                severity = com.moovar.android.core.database.entity.AlertSeverity.CRITICAL,
                publishedAt = 1000L,
                expiresAt = null,
                cachedAt = 1000L
            )
        )

        val flow = repository.observeAlerts("mitre")
        val result = flow.first()

        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(AlertSeverity.CRITICAL, list[0].severity)
        assertEquals("mitre", list[0].lineId)
    }
}

private class FakeAlertTestDao : AlertDao {
    val alerts = mutableListOf<AlertEntity>()

    override fun observeAlerts(lineId: String?): Flow<List<AlertEntity>> {
        val filtered = if (lineId.isNullOrBlank()) {
            alerts
        } else {
            alerts.filter { it.lineId == lineId }
        }
        return flowOf(filtered)
    }

    override suspend fun deleteExpired(expiryTime: Long) {
        alerts.removeAll { it.cachedAt < expiryTime }
    }

    override suspend fun deleteAll() {
        alerts.clear()
    }

    override suspend fun upsertAll(alerts: List<AlertEntity>) {
        alerts.forEach { alert ->
            val idx = this.alerts.indexOfFirst { it.id == alert.id }
            if (idx >= 0) this.alerts[idx] = alert else this.alerts.add(alert)
        }
    }
}

private class FakeSofseAlertTestService : SofseApiService {
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

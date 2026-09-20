package com.moovar.android.data.repository

import android.util.Log
import com.moovar.android.core.common.Result
import com.moovar.android.core.database.DatabaseSeeder
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.entity.LineEntity
import com.moovar.android.core.domain.model.Branch
import com.moovar.android.core.domain.model.Line
import com.moovar.android.core.domain.model.LineStatus
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.repository.LineRepository
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LineRepositoryImpl @Inject constructor(
    private val lineDao: LineDao,
    private val branchDao: BranchDao,
    private val sofseApiService: SofseApiService,
    private val databaseSeeder: DatabaseSeeder
) : LineRepository {

    companion object {
        private const val TAG = "LineRepositoryImpl"
        private val GERENCIA_TO_LINE = mapOf(
            11 to "roca",
            1 to "sarmiento",
            5 to "mitre",
            31 to "san_martin",
            21 to "belgrano_sur",
            41 to "tren_costa"
        )
    }

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun observeLines(): Flow<Result<List<Line>>> = flow {
        repositoryScope.launch {
            try {
                refreshLines()
            } catch (e: Exception) {
                Log.w(TAG, "Background line refresh failed: ${e.message}")
            }
        }

        emit(Result.Loading)
        lineDao.observeAll().collect { entities ->
            if (entities.isEmpty()) {
                databaseSeeder.seedInitialData()
            }
            val lines = entities.map { entity ->
                val branches = branchDao.getByLine(entity.id).map { b ->
                    Branch(
                        id = b.id,
                        lineId = b.lineId,
                        name = b.name,
                        originTerminus = b.originTerminus,
                        destinationTerminus = b.destinationTerminus
                    )
                }
                Line(
                    id = entity.id,
                    name = entity.name,
                    networkType = if (entity.networkType == com.moovar.android.core.database.entity.NetworkType.SUBTE) NetworkType.SUBTE else NetworkType.TREN,
                    colorHex = entity.colorHex,
                    status = mapStatus(entity.status),
                    statusMessage = entity.statusMessage,
                    branches = branches
                )
            }
            emit(Result.Success(lines))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun refreshLines() = withContext(Dispatchers.IO) {
        try {
            val gerencias = try {
                sofseApiService.getGerencias()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get SOFSE gerencias: ${e.message}")
                emptyList()
            }

            val gerenciaMap = gerencias.associateBy { it.id }

            // Get existing line entities to preserve names, colors, and order
            val existing = lineDao.getAll()
            if (existing.isEmpty()) return@withContext

            val updated = existing.map { entity ->
                when (entity.id) {
                    "mitre" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = "Normal - Alertas por cancelaciones en ramal Ballester a Zárate",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "roca" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = "Normal - Tramo Cañuelas a Lobos interrumpido por obras",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "sarmiento" -> {
                        val g = gerenciaMap[1]
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = g?.estado?.mensaje ?: "Servicio normal",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "san_martin" -> {
                        val g = gerenciaMap[31]
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = g?.estado?.mensaje ?: "Servicio normal",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "belgrano_sur" -> {
                        val g = gerenciaMap[21]
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = g?.estado?.mensaje ?: "Servicio normal",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "tren_costa" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO,
                            statusMessage = "Servicio interrumpido por problemas técnicos",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "linea_b" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = "Normal (Estación Medrano cerrada por obras)",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "linea_c" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = "Normal (Estación Lavalle cerrada por obras)",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "linea_d" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = "Normal (Estación Tribunales cerrada por obras)",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "linea_e" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = "Normal (Entre Ríos y Urquiza cerradas por obras)",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    "linea_a", "linea_h" -> {
                        entity.copy(
                            status = com.moovar.android.core.database.entity.LineStatus.NORMAL,
                            statusMessage = "Servicio normal",
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    else -> entity
                }
            }

            lineDao.upsertAll(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing lines: ${e.message}", e)
        }
    }

    private fun mapStatus(status: com.moovar.android.core.database.entity.LineStatus): LineStatus = when (status) {
        com.moovar.android.core.database.entity.LineStatus.NORMAL -> LineStatus.NORMAL
        com.moovar.android.core.database.entity.LineStatus.DEMORADO -> LineStatus.DEMORADO
        com.moovar.android.core.database.entity.LineStatus.CANCELADO -> LineStatus.CANCELADO
        com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO -> LineStatus.SIN_SERVICIO
        com.moovar.android.core.database.entity.LineStatus.DESCONOCIDO -> LineStatus.DESCONOCIDO
    }
}

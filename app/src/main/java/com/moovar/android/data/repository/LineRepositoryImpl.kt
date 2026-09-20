package com.moovar.android.data.repository

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

    override fun observeLines(): Flow<Result<List<Line>>> = flow {
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
            val remoteStatus = sofseApiService.getLineas()
            val existing = lineDao.observeAll()
            // Update statuses in database if needed
            val updated = remoteStatus.map { dto ->
                LineEntity(
                    id = dto.id,
                    name = dto.name,
                    shortName = dto.name,
                    networkType = com.moovar.android.core.database.entity.NetworkType.TREN,
                    iconResName = "ic_line_${dto.id}",
                    colorHex = dto.colorHex ?: "#0055A5",
                    status = parseStatus(dto.status),
                    statusMessage = dto.message,
                    lastUpdatedAt = System.currentTimeMillis(),
                    sortOrder = 1
                )
            }
            if (updated.isNotEmpty()) {
                lineDao.upsertAll(updated)
            }
        } catch (_: Exception) {
            // Keep offline cached data on network error
        }
    }

    private fun mapStatus(status: com.moovar.android.core.database.entity.LineStatus): LineStatus = when (status) {
        com.moovar.android.core.database.entity.LineStatus.NORMAL -> LineStatus.NORMAL
        com.moovar.android.core.database.entity.LineStatus.DEMORADO -> LineStatus.DEMORADO
        com.moovar.android.core.database.entity.LineStatus.CANCELADO -> LineStatus.CANCELADO
        com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO -> LineStatus.SIN_SERVICIO
        com.moovar.android.core.database.entity.LineStatus.DESCONOCIDO -> LineStatus.DESCONOCIDO
    }

    private fun parseStatus(status: String): com.moovar.android.core.database.entity.LineStatus = when (status.uppercase()) {
        "NORMAL" -> com.moovar.android.core.database.entity.LineStatus.NORMAL
        "DEMORADO" -> com.moovar.android.core.database.entity.LineStatus.DEMORADO
        "CANCELADO" -> com.moovar.android.core.database.entity.LineStatus.CANCELADO
        "SIN_SERVICIO" -> com.moovar.android.core.database.entity.LineStatus.SIN_SERVICIO
        else -> com.moovar.android.core.database.entity.LineStatus.NORMAL
    }
}

package com.moovar.android.core.database

import android.content.Context
import android.util.Log
import com.moovar.android.core.database.entity.AlertEntity
import com.moovar.android.core.database.entity.AlertSeverity
import com.moovar.android.core.database.entity.BranchEntity
import com.moovar.android.core.database.entity.LineEntity
import com.moovar.android.core.database.entity.LineStatus
import com.moovar.android.core.database.entity.NetworkType
import com.moovar.android.core.database.entity.StationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

open class DatabaseSeeder(
    private val context: Context? = null,
    private val db: AppDatabase? = null
) {
    open suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        if (context == null || db == null) return@withContext
        val lineDao = db.lineDao()
        val branchDao = db.branchDao()
        val stationDao = db.stationDao()
        val alertDao = db.alertDao()

        try {
            val inputStream = context.assets.open("seed_data.json")
            val jsonString = InputStreamReader(inputStream).readText()
            val format = Json { ignoreUnknownKeys = true }
            val seedData = format.decodeFromString<SeedData>(jsonString)

            val lines = seedData.lines.map { it.toEntity() }
            val branches = seedData.branches.map { it.toEntity() }
            val stations = seedData.stations.map { it.toEntity() }
            val alerts = seedData.alerts.map { it.toEntity() }

            lineDao.upsertAll(lines)
            branchDao.upsertAll(branches)
            stationDao.upsertAll(stations)
            alertDao.deleteAll()
            if (alerts.isNotEmpty()) {
                alertDao.upsertAll(alerts)
            }
            Log.d("DatabaseSeeder", "Seeded ${lines.size} lines, ${branches.size} branches, ${stations.size} stations, and ${alerts.size} alerts")
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error seeding data", e)
        }
    }
}

@Serializable
data class SeedData(
    val lines: List<SeedLine>,
    val branches: List<SeedBranch>,
    val stations: List<SeedStation>,
    val alerts: List<SeedAlert> = emptyList()
)

@Serializable
data class SeedLine(
    val id: String,
    val name: String,
    val shortName: String,
    val networkType: String,
    val iconResName: String,
    val colorHex: String,
    val sortOrder: Int,
    val status: String = "NORMAL",
    val statusMessage: String? = null
) {
    fun toEntity() = LineEntity(
        id = id,
        name = name,
        shortName = shortName,
        networkType = NetworkType.valueOf(networkType),
        iconResName = iconResName,
        colorHex = colorHex,
        status = runCatching { LineStatus.valueOf(status) }.getOrDefault(LineStatus.NORMAL),
        statusMessage = statusMessage,
        lastUpdatedAt = System.currentTimeMillis(),
        sortOrder = sortOrder
    )
}

@Serializable
data class SeedAlert(
    val id: String,
    val lineId: String,
    val branchId: String? = null,
    val title: String,
    val description: String,
    val severity: String = "WARNING"
) {
    fun toEntity() = AlertEntity(
        id = id,
        lineId = lineId,
        branchId = branchId,
        title = title,
        description = description,
        severity = runCatching { AlertSeverity.valueOf(severity) }.getOrDefault(AlertSeverity.WARNING),
        publishedAt = System.currentTimeMillis(),
        expiresAt = null,
        cachedAt = System.currentTimeMillis()
    )
}

@Serializable
data class SeedBranch(
    val id: String,
    val lineId: String,
    val name: String,
    val originTerminus: String,
    val destinationTerminus: String,
    val subteLineColor: String? = null
) {
    fun toEntity() = BranchEntity(
        id = id,
        lineId = lineId,
        name = name,
        originTerminus = originTerminus,
        destinationTerminus = destinationTerminus,
        subteLineColor = subteLineColor
    )
}

@Serializable
data class SeedStation(
    val id: String,
    val branchId: String,
    val lineId: String,
    val name: String,
    val networkType: String,
    val gtfsStopId: String? = null,
    val sequenceInBranch: Int,
    val isTerminus: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun toEntity() = StationEntity(
        id = id,
        branchId = branchId,
        lineId = lineId,
        name = name,
        networkType = NetworkType.valueOf(networkType),
        gtfsStopId = gtfsStopId,
        sequenceInBranch = sequenceInBranch,
        isTerminus = isTerminus,
        latitude = latitude,
        longitude = longitude
    )
}

package com.moovar.android.core.database

import android.content.Context
import android.util.Log
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

class DatabaseSeeder(
    private val context: Context,
    private val db: AppDatabase
) {
    suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        val lineDao = db.lineDao()
        val branchDao = db.branchDao()
        val stationDao = db.stationDao()

        // Check if there are already lines seeded
        if (lineDao.count() > 0) {
            Log.d("DatabaseSeeder", "Database already seeded, skipping")
            return@withContext
        }

        try {
            val inputStream = context.assets.open("seed_data.json")
            val jsonString = InputStreamReader(inputStream).readText()
            val format = Json { ignoreUnknownKeys = true }
            val seedData = format.decodeFromString<SeedData>(jsonString)

            val lines = seedData.lines.map { it.toEntity() }
            val branches = seedData.branches.map { it.toEntity() }
            val stations = seedData.stations.map { it.toEntity() }

            lineDao.upsertAll(lines)
            branchDao.upsertAll(branches)
            stationDao.upsertAll(stations)
            Log.d("DatabaseSeeder", "Seeded ${lines.size} lines, ${branches.size} branches, and ${stations.size} stations")
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error seeding data", e)
        }
    }
}

@Serializable
data class SeedData(
    val lines: List<SeedLine>,
    val branches: List<SeedBranch>,
    val stations: List<SeedStation>
)

@Serializable
data class SeedLine(
    val id: String,
    val name: String,
    val shortName: String,
    val networkType: String,
    val iconResName: String,
    val colorHex: String,
    val sortOrder: Int
) {
    fun toEntity() = LineEntity(
        id = id,
        name = name,
        shortName = shortName,
        networkType = NetworkType.valueOf(networkType),
        iconResName = iconResName,
        colorHex = colorHex,
        status = LineStatus.NORMAL,
        statusMessage = null,
        lastUpdatedAt = System.currentTimeMillis(),
        sortOrder = sortOrder
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

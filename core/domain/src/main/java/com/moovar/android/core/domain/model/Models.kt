package com.moovar.android.core.domain.model

enum class NetworkType { TREN, SUBTE }
enum class LineStatus { NORMAL, AVISO, DEMORADO, CANCELADO, SIN_SERVICIO, DESCONOCIDO }
enum class AlertSeverity { INFO, WARNING, CRITICAL }
enum class StopState { PAST, CURRENT, FUTURE }

data class Coordinates(val latitude: Double, val longitude: Double)

data class Line(
    val id: String,
    val name: String,
    val networkType: NetworkType,
    val colorHex: String,
    val status: LineStatus,
    val statusMessage: String?,
    val branches: List<Branch> = emptyList()
)

data class Branch(
    val id: String,
    val lineId: String,
    val name: String,
    val originTerminus: String,
    val destinationTerminus: String
)

data class Station(
    val id: String,
    val name: String,
    val branchId: String,
    val lineId: String,
    val networkType: NetworkType,
    val gtfsStopId: String?,
    val sequenceInBranch: Int,
    val isTerminus: Boolean,
    val coordinates: Coordinates?
)

data class Departure(
    val serviceId: String,
    val branchName: String,
    val destination: String,
    val minutesAway: Int,
    val scheduledTime: String,
    val platform: String?,
    val serviceType: String?,
    val status: String,
    val vehicleCoordinates: Coordinates?,
    val networkType: NetworkType,
    val isTerminus: Boolean = false,
    val direction: String = "",
    val isCancelled: Boolean = false
)

data class JourneyDetails(
    val branchName: String,
    val serviceType: String,
    val destination: String,
    val platform: String?,
    val departureTime: String,
    val currentStatus: String,
    val stops: List<JourneyStop>
)

data class JourneyStop(
    val stationName: String,
    val scheduledTime: String?,
    val stopState: StopState,
    val isTerminus: Boolean
)

data class ServiceAlert(
    val id: String,
    val lineId: String,
    val branchId: String?,
    val title: String,
    val description: String,
    val severity: AlertSeverity
)

data class FavoriteRoute(
    val id: Long = 0,
    val originStationId: String,
    val originStationName: String,
    val destinationStationId: String,
    val destinationStationName: String,
    val lineId: String,
    val lineName: String,
    val createdAt: Long
)

enum class DepartureMode { NOW, SCHEDULED }

sealed class StationSelectorData {
    data class Recent(val stations: List<Station>) : StationSelectorData()
    data class Results(val stations: List<Station>) : StationSelectorData()
}

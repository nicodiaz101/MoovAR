# 🏗️ MoovAR — ESPECIFICACIÓN TÉCNICA (SPECS)

> Versión 1.0 | Arquitectura: Clean Architecture + MVVM  
> Fecha: 2026-08-28

---

## TABLA DE CONTENIDOS

1. [Mapa de Capas](#1-mapa-de-capas-clean-architecture)
2. [Capa de Datos — Room (Database)](#2-capa-de-datos--room-database)
3. [Capa de Datos — Red (SOFSE)](#3-capa-de-datos--red-sofse)
4. [Capa de Datos — Red (GTFS-RT Subte)](#4-capa-de-datos--red-gtfs-rt-subte)
5. [Capa de Dominio](#5-capa-de-dominio)
6. [Capa de Presentación — ViewModels y UiState](#6-capa-de-presentaci%C3%B3n--viewmodels-y-uistate)
7. [Capa de UI — Compose Components](#7-capa-de-ui--compose-components)
8. [Reglas de Negocio Críticas](#8-reglas-de-negocio-cr%C3%ADticas)

---

## 1. MAPA DE CAPAS (Clean Architecture)

```
┌──────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                      │
│   Compose UI ← ViewModel ← UiState (StateFlow)           │
│   :feature:home / departures / journey / alerts / map    │
└───────────────────────┬──────────────────────────────────┘
                        │ (observa / llama a)
┌───────────────────────▼──────────────────────────────────┐
│                    DOMAIN LAYER                           │
│   Use Cases · Repository Interfaces · Domain Entities    │
│   :core:domain  (Kotlin puro, cero deps Android)        │
└──────────┬────────────────────────────┬──────────────────┘
           │ implementado por            │ implementado por
┌──────────▼──────────┐    ┌────────────▼─────────────────┐
│   DATABASE LAYER    │    │      NETWORK LAYER            │
│   Room / SQLite     │    │ Retrofit (SOFSE)              │
│   :core:database    │    │ OkHttp + Protobuf (GTFS-RT)  │
│                     │    │ :core:network                 │
└─────────────────────┘    └──────────────────────────────┘
```

**Regla de Dependencia:** Las flechas solo apuntan hacia adentro (hacia Domain).
`core:domain` no importa ni Room ni Retrofit. Nunca.

---

## 2. CAPA DE DATOS — ROOM (DATABASE)

### 2.1 — Diagrama Entidad-Relación

```
NetworkType (enum: TREN | SUBTE)
         │
         ▼
    LineEntity ──────────────────── AlertEntity
         │ 1:N                           │ N:1
         ▼                               │
   BranchEntity ──────────────────────  │
         │ 1:N                           │
         ▼                               ▼
   StationEntity ───── RecentStationEntity
         │ (M:N via FavoriteRouteEntity)
         ▼
   FavoriteRouteEntity (originId, destinationId)
```

### 2.2 — Definición de Entidades Room

#### `LineEntity` (`:core:database`)

```kotlin
@Entity(tableName = "lines")
data class LineEntity(
    @PrimaryKey val id: String,           // "roca", "sarmiento", "lineaA", etc.
    val name: String,                      // "Roca", "Línea A"
    val shortName: String,                 // "Roca", "A"
    val networkType: NetworkType,          // TREN | SUBTE
    val iconResName: String,               // Nombre del drawable vectorial
    val colorHex: String,                  // "#1A6EA8" para theming de línea
    val status: LineStatus,                // NORMAL | DEMORADO | CANCELADO | SIN_SERVICIO
    val statusMessage: String?,            // Mensaje libre de estado
    val lastUpdatedAt: Long,               // Timestamp epoch millis
    val sortOrder: Int                     // Para mantener el orden en la lista
)

enum class NetworkType { TREN, SUBTE }

enum class LineStatus { NORMAL, DEMORADO, CANCELADO, SIN_SERVICIO, DESCONOCIDO }
```

#### `BranchEntity` (`:core:database`)

```kotlin
@Entity(
    tableName = "branches",
    foreignKeys = [ForeignKey(
        entity = LineEntity::class,
        parentColumns = ["id"],
        childColumns = ["lineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lineId")]
)
data class BranchEntity(
    @PrimaryKey val id: String,           // "roca_constitucion_ezeiza"
    val lineId: String,                    // FK → LineEntity.id
    val name: String,                      // "Plaza C. - Ezeiza"
    val originTerminus: String,            // "Plaza Constitución"
    val destinationTerminus: String,       // "Ezeiza"
    // Solo para SUBTE: null para TREN
    val subteLineColor: String?,           // "#18B4E9" (azul Línea A)
)
```

#### `StationEntity` (`:core:database`)

```kotlin
@Entity(
    tableName = "stations",
    foreignKeys = [ForeignKey(
        entity = BranchEntity::class,
        parentColumns = ["id"],
        childColumns = ["branchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("branchId"), Index("name")]
)
data class StationEntity(
    @PrimaryKey val id: String,           // "stn_constitucion", "gtfs_stop_1234"
    val branchId: String,                  // FK → BranchEntity.id
    val lineId: String,                    // Desnormalizado para queries rápidas
    val name: String,                      // "Constitución"
    val networkType: NetworkType,
    val gtfsStopId: String?,               // ID del stop en GTFS-RT (para Subte)
    val sequenceInBranch: Int,             // Orden en el ramal
    val isTerminus: Boolean,               // Para la lógica del FAB de mapa
    // Coordenadas: presentes para Tren, NULL para Subte (GTFS-RT BA no las provee)
    val latitude: Double?,
    val longitude: Double?
)
```

#### `AlertEntity` (`:core:database`)

```kotlin
@Entity(
    tableName = "alerts",
    foreignKeys = [ForeignKey(
        entity = LineEntity::class,
        parentColumns = ["id"],
        childColumns = ["lineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lineId")]
)
data class AlertEntity(
    @PrimaryKey val id: String,
    val lineId: String,                    // FK → LineEntity.id
    val branchId: String?,                 // Opcional: alerta específica de ramal
    val title: String,                     // "Plaza Constitución - Ezeiza"
    val description: String,
    val severity: AlertSeverity,           // INFO | WARNING | CRITICAL
    val publishedAt: Long,
    val expiresAt: Long?,
    val cachedAt: Long                     // Para TTL invalidation (15 min)
)

enum class AlertSeverity { INFO, WARNING, CRITICAL }
```

#### `RecentStationEntity` (`:core:database`)

```kotlin
@Entity(tableName = "recent_stations")
data class RecentStationEntity(
    @PrimaryKey val stationId: String,    // FK lógica → StationEntity (no FK real por simplicidad)
    val stationName: String,              // Desnormalizado para display rápido
    val lineId: String,
    val lineName: String,
    val accessedAt: Long                  // Para ordenar por más reciente
)
```

#### `FavoriteRouteEntity` (`:core:database`)

```kotlin
@Entity(
    tableName = "favorite_routes",
    indices = [Index(value = ["originStationId", "destinationStationId"], unique = true)]
)
data class FavoriteRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originStationId: String,
    val originStationName: String,
    val destinationStationId: String,
    val destinationStationName: String,
    val lineId: String,
    val lineName: String,
    val createdAt: Long
)
```

### 2.3 — TypeConverters

```kotlin
class RoomTypeConverters {
    @TypeConverter fun fromNetworkType(v: NetworkType): String = v.name
    @TypeConverter fun toNetworkType(v: String): NetworkType = NetworkType.valueOf(v)

    @TypeConverter fun fromLineStatus(v: LineStatus): String = v.name
    @TypeConverter fun toLineStatus(v: String): LineStatus = LineStatus.valueOf(v)

    @TypeConverter fun fromAlertSeverity(v: AlertSeverity): String = v.name
    @TypeConverter fun toAlertSeverity(v: String): AlertSeverity = AlertSeverity.valueOf(v)
}
```

### 2.4 — DAOs

```kotlin
@Dao
interface LineDao {
    @Query("SELECT * FROM lines ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<LineEntity>>

    @Upsert
    suspend fun upsertAll(lines: List<LineEntity>)
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE lineId = :lineId")
    suspend fun getByLine(lineId: String): List<BranchEntity>

    @Upsert
    suspend fun upsertAll(branches: List<BranchEntity>)
}

@Dao
interface StationDao {
    // Búsqueda full-text con LIKE para el selector
    @Query("""
        SELECT * FROM stations 
        WHERE (:branchId IS NULL OR branchId = :branchId)
        AND name LIKE '%' || :query || '%'
        ORDER BY sequenceInBranch ASC
    """)
    suspend fun search(query: String, branchId: String?): List<StationEntity>

    @Query("SELECT * FROM stations WHERE branchId = :branchId ORDER BY sequenceInBranch ASC")
    suspend fun getByBranch(branchId: String): List<StationEntity>

    @Upsert
    suspend fun upsertAll(stations: List<StationEntity>)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE (:lineId IS NULL OR lineId = :lineId) ORDER BY publishedAt DESC")
    fun observeAlerts(lineId: String?): Flow<List<AlertEntity>>

    @Query("DELETE FROM alerts WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)

    @Upsert
    suspend fun upsertAll(alerts: List<AlertEntity>)
}

@Dao
interface RecentStationDao {
    @Query("SELECT * FROM recent_stations ORDER BY accessedAt DESC LIMIT 5")
    suspend fun getRecent(): List<RecentStationEntity>

    @Upsert
    suspend fun upsert(station: RecentStationEntity)

    @Query("DELETE FROM recent_stations WHERE stationId NOT IN (SELECT stationId FROM recent_stations ORDER BY accessedAt DESC LIMIT 10)")
    suspend fun pruneOld()
}

@Dao
interface FavoriteRouteDao {
    @Query("SELECT * FROM favorite_routes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(route: FavoriteRouteEntity)

    @Query("DELETE FROM favorite_routes WHERE originStationId = :oId AND destinationStationId = :dId")
    suspend fun delete(oId: String, dId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_routes WHERE originStationId = :oId AND destinationStationId = :dId)")
    fun isFavorite(oId: String, dId: String): Flow<Boolean>
}
```

---

## 3. CAPA DE DATOS — RED (SOFSE)

### 3.1 — TokenManagerInterceptor (Detalle de Implementación)

```kotlin
class TokenManagerInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,          // EncryptedSharedPreferences wrapper
    private val authApi: SofseAuthApi,               // Retrofit instance SIN este interceptor
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val authenticatedRequest = originalRequest.withToken(tokenStorage.getToken())
        val response = chain.proceed(authenticatedRequest)

        return if (response.code == 401) {
            response.close()
            val newToken = refreshToken()       // Sincrónico (estamos en IO thread)
            tokenStorage.saveToken(newToken)
            chain.proceed(originalRequest.withToken(newToken))
        } else {
            response
        }
    }

    private fun refreshToken(): String {
        // 1. Generar hash MD5 de la constante de versión
        val versionString = "V3rS10n\$SOFSE"
        val md5Hash = MessageDigest.getInstance("MD5")
            .digest(versionString.toByteArray())
            .joinToString("") { "%02x".format(it) }

        // 2. Llamar al endpoint de autenticación (synchronously via execute())
        val tokenResponse = authApi.getToken(md5Hash).execute()
        return tokenResponse.body()?.token
            ?: throw TokenRefreshException("No se pudo obtener el token de SOFSE")
    }

    private fun Request.withToken(token: String?): Request =
        if (token != null) newBuilder().header("Authorization", "Bearer $token").build()
        else this
}
```

### 3.2 — Interceptores de Headers Estáticos

```kotlin
class StaticHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("x-api-key", "dXN1YXJpb2FwcDphcHAyeG1s")
                .header("Referer", "https://api-servicios.sofse.gob.ar")
                .header("Accept", "application/json")
                .build()
        )
}
```

### 3.3 — DTOs de la API SOFSE

```kotlin
// GET /v3/lineas
@Serializable
data class LineStatusDto(
    @SerialName("id")          val id: String,
    @SerialName("nombre")      val name: String,
    @SerialName("estado")      val status: String,  // "Normal", "Demorado", etc.
    @SerialName("mensaje")     val message: String?,
    @SerialName("icono")       val iconUrl: String?,
    @SerialName("color")       val colorHex: String?
)

// GET /v3/proximos/{origen}/{destino}
@Serializable
data class DepartureDto(
    @SerialName("id")              val serviceId: String,
    @SerialName("ramal")           val branchName: String,
    @SerialName("destino")         val destination: String,
    @SerialName("horario")         val scheduledTime: String,    // "HH:mm"
    @SerialName("tiempo")          val minutesAway: Int,
    @SerialName("estado")          val status: String,           // "NORMAL", "DEMORADO"
    @SerialName("anden")           val platform: String?,
    @SerialName("tipo_servicio")   val serviceType: String?,     // "DIRECTO", "SEMIDIRECTO"
    // Coordenadas de la formación (solo Tren, null si no disponible)
    @SerialName("lat")             val latitude: Double?,
    @SerialName("lon")             val longitude: Double?
)

// GET /v3/recorrido/{servicioId}
@Serializable
data class JourneyDto(
    @SerialName("ramal")    val branchName: String,
    @SerialName("servicio") val serviceType: String,
    @SerialName("paradas")  val stops: List<StopDto>
)

@Serializable
data class StopDto(
    @SerialName("nombre")        val name: String,
    @SerialName("horario")       val scheduledTime: String?,   // null si ya pasó
    @SerialName("estado")        val state: String,             // "FUTURO", "ACTUAL", "PASADO"
    @SerialName("es_cabecera")   val isTerminus: Boolean
)
```

---

## 4. CAPA DE DATOS — RED (GTFS-RT SUBTE)

### 4.1 — Estructura del Módulo Protobuf

```
:core:network/
└── src/
    └── main/
        ├── proto/
        │   └── gtfs-realtime.proto          ← Spec oficial de Google Transit
        └── kotlin/
            └── network/gtfsrt/
                ├── GtfsRtDataSource.kt      ← OkHttp client
                └── GtfsRtMapper.kt          ← FeedMessage → Domain models
```

### 4.2 — GtfsRtDataSource

```kotlin
class GtfsRtDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @GtfsRtEndpointUrl private val endpointUrl: String
) {
    suspend fun fetchFeed(): FeedMessage = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(endpointUrl)
            .header("Accept", "application/x-protobuf")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw NetworkException(response.code)
            val bytes = response.body?.bytes()
                ?: throw EmptyBodyException("GTFS-RT feed vacío")
            FeedMessage.parseFrom(bytes)
        }
    }
}
```

### 4.3 — GtfsRtMapper

```kotlin
object GtfsRtMapper {

    /**
     * Mapea TripUpdates a una lista de StopTimeUpdate por stop_id.
     * No extrae coordenadas (GTFS-RT BA no las reporta para Subte).
     */
    fun mapTripUpdates(feed: FeedMessage): Map<String, List<SubteArrival>> =
        feed.entityList
            .filter { it.hasTripUpdate() }
            .flatMap { entity ->
                val tripId = entity.tripUpdate.trip.tripId
                entity.tripUpdate.stopTimeUpdateList.map { update ->
                    SubteArrival(
                        stopId = update.stopId,
                        tripId = tripId,
                        arrivalDelay = update.arrival.delay,       // En segundos
                        arrivalTime = update.arrival.time,         // Epoch seconds
                        departureDelay = update.departure.delay,
                        scheduleRelationship = update.scheduleRelationship.name
                    )
                }
            }
            .groupBy { it.stopId }

    /**
     * VehiclePosition: para Subte no se extrae lat/long.
     * El 'hasCoordinates' siempre es false para NetworkType.SUBTE.
     */
    fun mapVehiclePositions(feed: FeedMessage): List<VehicleLocation> =
        feed.entityList
            .filter { it.hasVehicle() }
            .map { entity ->
                val v = entity.vehicle
                VehicleLocation(
                    vehicleId = v.vehicle.id,
                    tripId = v.trip.tripId,
                    stopId = v.stopId,
                    currentStatus = v.currentStatus.name,
                    hasCoordinates = false,   // SIEMPRE false para Subte (restricción de negocio)
                    latitude = null,
                    longitude = null
                )
            }
}
```

---

## 5. CAPA DE DOMINIO

### 5.1 — Entidades de Dominio (`:core:domain`)

```kotlin
// Entidades puras, sin anotaciones de Room ni Retrofit

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
    val coordinates: Coordinates?       // null para Subte y Trenes sin telemetría
)

data class Coordinates(val latitude: Double, val longitude: Double)

data class Departure(
    val serviceId: String,
    val branchName: String,
    val destination: String,
    val minutesAway: Int,
    val scheduledTime: String,
    val platform: String?,
    val serviceType: String?,
    val status: String,
    val vehicleCoordinates: Coordinates?,   // null si no hay o es Subte
    val networkType: NetworkType
)

data class JourneyStop(
    val stationName: String,
    val scheduledTime: String?,
    val stopState: StopState,
    val isTerminus: Boolean
)

enum class StopState { PAST, CURRENT, FUTURE }

data class ServiceAlert(
    val id: String,
    val lineId: String,
    val branchId: String?,
    val title: String,
    val description: String,
    val severity: AlertSeverity
)
```

### 5.2 — Interfaces de Repositorio

```kotlin
// :core:domain/repository/

interface LineRepository {
    fun observeLines(): Flow<Result<List<Line>>>
    suspend fun refreshLines()
}

interface DepartureRepository {
    suspend fun getDepartures(
        originId: String,
        destinationId: String,
        departureTime: LocalDateTime = LocalDateTime.now()
    ): Result<List<Departure>>
}

interface JourneyRepository {
    suspend fun getJourney(serviceId: String): Result<List<JourneyStop>>
}

interface AlertRepository {
    fun observeAlerts(lineId: String? = null): Flow<Result<List<ServiceAlert>>>
    suspend fun refreshAlerts()
}

interface StationRepository {
    suspend fun searchStations(query: String, branchId: String? = null): List<Station>
    suspend fun getBranchesByLine(lineId: String): List<Branch>
}

interface RecentStationRepository {
    suspend fun getRecentStations(): List<Station>
    suspend fun saveRecentStation(station: Station)
}

interface FavoriteRouteRepository {
    fun observeFavorites(): Flow<List<FavoriteRoute>>
    fun isFavorite(originId: String, destinationId: String): Flow<Boolean>
    suspend fun toggleFavorite(origin: Station, destination: Station)
}
```

### 5.3 — Use Cases Detallados

```kotlin
class GetNextDeparturesUseCase @Inject constructor(
    private val departureRepository: DepartureRepository,
    private val recentStationRepository: RecentStationRepository
) {
    suspend operator fun invoke(
        origin: Station,
        destination: Station,
        departureTime: LocalDateTime = LocalDateTime.now()
    ): Result<List<Departure>> {
        // Guardar en historial ANTES del fetch (UX: respuesta inmediata al historial)
        recentStationRepository.saveRecentStation(origin)
        recentStationRepository.saveRecentStation(destination)

        return departureRepository.getDepartures(origin.id, destination.id, departureTime)
    }
}

class GetStationsForSelectorUseCase @Inject constructor(
    private val stationRepository: StationRepository,
    private val recentStationRepository: RecentStationRepository
) {
    /**
     * @param query: texto de búsqueda (puede ser vacío)
     * @param branchId: chip seleccionado (null = mostrar historial si query vacío)
     */
    suspend operator fun invoke(query: String, branchId: String?): StationSelectorData {
        return when {
            query.isEmpty() && branchId == null -> {
                val recent = recentStationRepository.getRecentStations()
                StationSelectorData.Recent(recent)
            }
            else -> {
                val stations = stationRepository.searchStations(query.trim(), branchId)
                StationSelectorData.Results(stations)
            }
        }
    }
}

sealed class StationSelectorData {
    data class Recent(val stations: List<Station>) : StationSelectorData()
    data class Results(val stations: List<Station>) : StationSelectorData()
}
```

---

## 6. CAPA DE PRESENTACIÓN — ViewModels y UiState

### 6.1 — HomeViewModel

```kotlin
data class HomeUiState(
    val lines: List<Line> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLinesStatusUseCase: GetLinesStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getLinesStatusUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { result ->
                    _uiState.update {
                        when (result) {
                            is Result.Success -> it.copy(lines = result.data, isLoading = false, error = null)
                            is Result.Error   -> it.copy(error = result.message, isLoading = false)
                            is Result.Loading -> it.copy(isLoading = true)
                        }
                    }
                }
        }
    }
}
```

### 6.2 — DeparturesViewModel

```kotlin
data class DeparturesUiState(
    val line: Line? = null,
    val branches: List<Branch> = emptyList(),
    val selectedBranch: Branch? = null,
    val originStation: Station? = null,
    val destinationStation: Station? = null,
    val departures: List<Departure> = emptyList(),
    val hasActiveAlerts: Boolean = false,
    val isLoadingDepartures: Boolean = false,
    val departureMode: DepartureMode = DepartureMode.NOW,
    val scheduledTime: LocalDateTime? = null,
    val error: String? = null
)

enum class DepartureMode { NOW, SCHEDULED }

// Eventos one-shot (no persisten en estado)
sealed class DeparturesUiEvent {
    data class NavigateToJourney(val serviceId: String, val line: Line) : DeparturesUiEvent()
    data class NavigateToMap(val coordinates: Coordinates) : DeparturesUiEvent()
    data class NavigateToAlerts(val lineId: String) : DeparturesUiEvent()
}

@HiltViewModel
class DeparturesViewModel @Inject constructor(
    private val getNextDeparturesUseCase: GetNextDeparturesUseCase,
    private val getStationsForSelectorUseCase: GetStationsForSelectorUseCase,
    private val stationRepository: StationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lineId: String = checkNotNull(savedStateHandle["lineId"])

    private val _uiState = MutableStateFlow(DeparturesUiState())
    val uiState: StateFlow<DeparturesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DeparturesUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DeparturesUiEvent> = _events.asSharedFlow()

    fun onOriginSelected(station: Station) { /* actualiza estado + trigger búsqueda */ }
    fun onDestinationSelected(station: Station) { /* idem */ }
    fun onBranchChipSelected(branch: Branch?) { /* filtra selector */ }
    fun onSwapStations() { /* intercambia origen/destino */ }
    fun onTicketClicked(departure: Departure) {
        viewModelScope.launch {
            _events.emit(DeparturesUiEvent.NavigateToJourney(departure.serviceId, _uiState.value.line!!))
        }
    }
    fun onMapButtonClicked(departure: Departure) {
        viewModelScope.launch {
            departure.vehicleCoordinates?.let {
                _events.emit(DeparturesUiEvent.NavigateToMap(it))
            }
        }
    }
}
```

### 6.3 — StationSelectorViewModel

```kotlin
data class StationSelectorUiState(
    val searchQuery: String = "",
    val selectedBranchId: String? = null,
    val stationData: StationSelectorData = StationSelectorData.Recent(emptyList()),
    val isLoading: Boolean = false
)

@HiltViewModel
class StationSelectorViewModel @Inject constructor(
    private val getStationsForSelectorUseCase: GetStationsForSelectorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationSelectorUiState())
    val uiState: StateFlow<StationSelectorUiState> = _uiState.asStateFlow()

    // Debounce de búsqueda para no saturar Room con cada keystroke
    private val searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300L)                 // Espera 300ms de inactividad
                .distinctUntilChanged()
                .collect { query ->
                    val branchId = _uiState.value.selectedBranchId
                    val data = getStationsForSelectorUseCase(query, branchId)
                    _uiState.update { it.copy(stationData = data, isLoading = false) }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isLoading = true) }
        searchQueryFlow.value = query
    }

    fun onBranchChipSelected(branchId: String?) {
        _uiState.update { it.copy(selectedBranchId = branchId, isLoading = true) }
        searchQueryFlow.value = _uiState.value.searchQuery   // Retrigger search
    }
}
```

### 6.4 — JourneyViewModel

```kotlin
data class JourneyUiState(
    val lineInfo: String = "",
    val serviceHeader: JourneyHeader? = null,
    val stops: List<JourneyStop> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class JourneyHeader(
    val branchName: String,
    val serviceType: String,
    val platform: String?,
    val departureTime: String,
    val currentStatus: String
)
```

---

## 7. CAPA DE UI — COMPOSE COMPONENTS

### 7.1 — Árbol de Componentes

```
TransportArApp
├── TransportArNavGraph
│   ├── HomeScreen
│   │   ├── LineStatusItem (x8)           ← Tap nombre → DeparturesScreen
│   │   │   ├── LineIcon (Vector)         ← Tap estado → AlertsScreen (filtrado)
│   │   │   ├── LineNameText
│   │   │   └── StatusBadge
│   │   └── BottomNavigationBar (3 tabs)
│   │
│   ├── DeparturesScreen
│   │   ├── LineHeaderBar (ramal actual)
│   │   ├── StationInputField (origen)
│   │   ├── SwapStationsButton
│   │   ├── StationInputField (destino)
│   │   ├── DepartureModeRadioGroup (Ahora / Programar)
│   │   ├── AlertsBanner (si hasActiveAlerts)
│   │   ├── FilterChipsRow (LazyRow)
│   │   └── DepartureTicketCard (ElevatedCard)
│   │       ├── MinutesCountdown (left panel)
│   │       └── DepartureDetails (right panel)
│   │           └── MapFAB (condicional — ver reglas §8)
│   │
│   ├── StationSelectorScreen (bottom sheet o fullscreen)
│   │   ├── SearchTextField
│   │   ├── BranchFilterRow (LazyRow FilterChips)
│   │   └── StationList (LazyColumn)
│   │       ├── RecentHeader (si modo historial)
│   │       └── StationItem
│   │
│   ├── JourneyScreen
│   │   ├── JourneyHeaderCard (ElevatedCard con detalles del servicio)
│   │   └── StopTimelineList (LazyColumn)
│   │       ├── PastStopItem (atenuado, sin horario)
│   │       ├── CurrentStopItem (destacado, ícono especial)
│   │       └── FutureStopItem (horario visible)
│   │
│   ├── AlertsScreen
│   │   ├── SearchTopBar
│   │   └── AlertsByLineList (LazyColumn)
│   │       ├── LineGroupHeader (sticky)
│   │       └── AlertItem
│   │
│   ├── MapScreen
│   │   └── AndroidView { OsmdroidMapView }
│   │       ├── MarkerOverlay (posición formación)
│   │       └── PolylineOverlay (recorrido ramal)
│   │
│   └── FavoritesScreen
│       └── FavoriteRouteList (LazyColumn)
```

### 7.2 — Especificación de DepartureTicketCard

```kotlin
@Composable
fun DepartureTicketCard(
    departure: Departure,
    onCardClick: () -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // La condición de visibilidad del MapButton es calculada aquí,
    // en la UI, basándose en el modelo de dominio
    val showMapButton = remember(departure) {
        !departure.isTerminus &&
        departure.vehicleCoordinates != null &&
        departure.networkType != NetworkType.SUBTE
    }

    ElevatedCard(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Panel izquierdo — Minutos
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = departure.minutesAway.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "MINUTOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Separador punteado (efecto "ticket")
            DashedDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

            // Panel derecho — Detalles
            Box(modifier = Modifier.weight(0.65f).padding(16.dp)) {
                Column {
                    LabeledDetail(label = "Ramal:", value = departure.branchName, isHighlighted = true)
                    LabeledDetail(label = "Destino:", value = departure.destination, isHighlighted = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusChip(status = departure.status)
                }

                // FAB/IconButton de mapa — SOLO si se cumplen las 3 condiciones
                if (showMapButton) {
                    IconButton(
                        onClick = onMapClick,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(imageVector = Icons.Outlined.Map, contentDescription = "Ver en mapa")
                    }
                }
            }
        }
    }
}
```

### 7.3 — Especificación de StopTimeline (Stepper Vertical)

```kotlin
@Composable
fun StopTimelineList(stops: List<JourneyStop>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(stops, key = { _, stop -> stop.stationName }) { index, stop ->
            StopTimelineItem(
                stop = stop,
                isFirst = index == 0,
                isLast = index == stops.lastIndex
            )
        }
    }
}

@Composable
private fun StopTimelineItem(stop: JourneyStop, isFirst: Boolean, isLast: Boolean) {
    val alpha = if (stop.stopState == StopState.PAST) 0.4f else 1f
    val nodeColor = when (stop.stopState) {
        StopState.CURRENT -> MaterialTheme.colorScheme.primary
        StopState.FUTURE  -> MaterialTheme.colorScheme.outline
        StopState.PAST    -> MaterialTheme.colorScheme.outlineVariant
    }
    // Render: [VerticalLine | Node | StationName | Time]
    // La línea vertical se dibuja con Canvas para mayor control
    Row(modifier = Modifier.fillMaxWidth().alpha(alpha)) {
        TimelineNode(
            isFirst = isFirst,
            isLast = isLast,
            color = nodeColor,
            isCurrent = stop.stopState == StopState.CURRENT
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = stop.stationName, modifier = Modifier.weight(1f))
        stop.scheduledTime?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

---

## 8. REGLAS DE NEGOCIO CRÍTICAS

### 8.1 — Regla del Botón de Mapa (MapButton Visibility)

> Esta es la regla más compleja de la UI. Se evalúa en tiempo de composición.

```
showMapButton = (
    departure.isTerminus == false          // R1: No es cabecera/terminal final
    AND departure.vehicleCoordinates != null  // R2: La API devolvió lat/long
    AND departure.networkType != SUBTE        // R3: NO es Subte (GTFS-RT BA no da coords)
)
```

**Implementación:** La evaluación ocurre en `DepartureTicketCard` via `remember(departure)`.
El `DeparturesViewModel` NO pre-calcula este booleano; es responsabilidad de la capa UI.
Esto respeta la separación: el ViewModel provee datos, la UI decide cómo renderizar.

### 8.2 — Regla del Historial vs. Búsqueda en StationSelector

```
IF query.isEmpty() AND selectedBranchId == null:
    → Mostrar "Estaciones recientes" (Room: RecentStationDao.getRecent())
ELSE IF query.isEmpty() AND selectedBranchId != null:
    → Mostrar todas las estaciones del ramal (StationDao.getByBranch())
ELSE IF query.isNotEmpty():
    → Buscar en Room con LIKE (StationDao.search(query, selectedBranchId?))
```

### 8.3 — Regla de Coordenadas GPS Nulas para Subte

En el mapper GTFS-RT:
```kotlin
// SIEMPRE null para Subte, independientemente del contenido del Protobuf
hasCoordinates = networkType == NetworkType.SUBTE -> false
latitude = if (networkType == NetworkType.SUBTE) null else position.latitude
longitude = if (networkType == NetworkType.SUBTE) null else position.longitude
```

### 8.4 — Regla de Permisos

El `AndroidManifest.xml` solo puede contener estos permisos:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<!-- FIN. No ACCESS_FINE_LOCATION, no ACCESS_COARSE_LOCATION, no READ_PHONE_STATE -->
```

Osmdroid requiere configuración explícita para evitar que solicite permisos:
```kotlin
Configuration.getInstance().apply {
    userAgentValue = "TransportAR/1.0"
    // NO llamar a enableRotationGesture() ni activar MyLocationNewOverlay
}
```

### 8.5 — Regla de Privacidad en Build

```kotlin
// NetworkModule.kt
@Provides @Singleton
fun provideOkHttpClient(/* ... */): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(StaticHeadersInterceptor())
        .addInterceptor(TokenManagerInterceptor(/* ... */))
        // Solo en debug:
        .apply { if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor()) }
        .build()
```

---

## 9. RESULTADO WRAPPER GENÉRICO

```kotlin
// :core:common
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()

    val isSuccess get() = this is Success
    val isLoading get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    inline fun onError(action: (String) -> Unit): Result<T> {
        if (this is Error) action(message)
        return this
    }
}
```

---

*Especificación técnica completa. Versión 1.0 — 2026-08-28*

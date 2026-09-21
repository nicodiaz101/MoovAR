package com.moovar.android.data.repository

import android.util.Log
import com.moovar.android.core.common.Result
import com.moovar.android.core.database.dao.BranchDao
import com.moovar.android.core.database.dao.LineDao
import com.moovar.android.core.database.dao.StationDao
import com.moovar.android.core.domain.model.Coordinates
import com.moovar.android.core.domain.model.Departure
import com.moovar.android.core.domain.model.JourneyDetails
import com.moovar.android.core.domain.model.JourneyStop
import com.moovar.android.core.domain.model.NetworkType
import com.moovar.android.core.domain.model.StopState
import com.moovar.android.core.domain.repository.DepartureRepository
import com.moovar.android.core.network.sofse.api.SofseApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DepartureRepositoryImpl @Inject constructor(
    private val sofseApiService: SofseApiService,
    private val stationDao: StationDao,
    private val branchDao: BranchDao,
    private val lineDao: LineDao,
    private val journeyDetailsCache: JourneyDetailsCache
) : DepartureRepository {

    companion object {
        private const val TAG = "DepartureRepoImpl"
        private val ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        // Pre-seeded popular station mappings for instantaneous zero-latency lookups
        private val PRESEEDED_SOFSE_IDS = mapOf(
            "retiro" to "332",
            "retiro (mitre)" to "332",
            "retiro (lgm)" to "332",
            "retiro (san martín)" to "463",
            "retiro (san martin)" to "463",
            "retiro - lsm" to "463",
            "once" to "293",
            "plaza constitución" to "93",
            "plaza c." to "93",
            "constitución" to "93",
            "tigre" to "389",
            "villa ballester" to "412",
            "v. ballester" to "412",
            "zárate" to "434",
            "capilla del señor" to "61",
            "victoria" to "409",
            "san fernando" to "355",
            "san fernando c" to "355",
            "san fernando r" to "356",
            "san isidro" to "357",
            "san isidro c" to "357",
            "san isidro r" to "358",
            "vicente lópez" to "408",
            "belgrano c" to "34",
            "núñez" to "287",
            "rivadavia" to "337",
            "olivos" to "291",
            "la lucila" to "212",
            "martínez" to "262",
            "acassuso" to "5",
            "béccar" to "32",
            "beccar" to "32",
            "virreyes" to "428",
            "carupá" to "65",
            "lisandro de la torre" to "229",
            "l. de la torre" to "229",
            "dr. cetrángolo" to "119",
            "cetrángolo" to "119",
            "bartolomé mitre" to "273",
            "mitre" to "273",
            "juan b. justo" to "200",
            "florida" to "141",
            "florida (mitre)" to "141",
            "coghlan" to "90",
            "l.m. saavedra" to "346",
            "saavedra" to "346",
            "chilavert" to "79",
            "malaver" to "249",
            "san andrés" to "353",
            "san martín" to "360",
            "san martín (mitre)" to "360",
            "josé león suárez" to "190",
            "jose leon suarez" to "190",
            "j. l. suarez" to "190",
            "miguelete" to "271",
            "pueyrredón" to "318",
            "pueyrredón (mitre)" to "318",
            "gral. urquiza" to "150",
            "general urquiza" to "150",
            "l. m. drago" to "236",
            "drago" to "236",
            "belgrano r" to "35",
            "colegiales" to "91",
            "ministro carranza" to "272",
            "3 de febrero" to "1",
            "morón" to "279",
            "castelar" to "70",
            "ituzaingó" to "186",
            "san antonio de padua" to "347",
            "s.a. de padua" to "347",
            "merlo" to "269",
            "paso del rey" to "300",
            "moreno" to "278",
            "haedo" to "169",
            "ramos mejía" to "327",
            "ciudadela" to "84",
            "liniers" to "234",
            "villa luro" to "423",
            "floresta" to "140",
            "flores" to "139",
            "caballito" to "50",
            "palermo" to "297",
            "villa crespo" to "414",
            "la paternal" to "216",
            "villa del parque" to "415",
            "devoto" to "112",
            "sáenz peña" to "348",
            "santos lugares" to "369",
            "caseros" to "67",
            "el palomar" to "127",
            "hurlingham" to "177",
            "william morris" to "431",
            "bella vista" to "36",
            "muñiz" to "283",
            "san miguel" to "361",
            "josé c. paz" to "194",
            "sol y verde" to "378",
            "presidente derqui" to "106",
            "derqui" to "106",
            "villa astolfi" to "411",
            "pilar" to "306",
            "manzanares" to "252",
            "dr. domingo cabred" to "51",
            "cabred" to "51",
            "avellaneda" to "368",
            "d. santillán y m. kosteki (avellaneda)" to "368",
            "s y kosteki" to "368",
            "lanús" to "215",
            "banfield" to "30",
            "lomas de zamora" to "238",
            "l. zamora" to "238",
            "temperley" to "386",
            "adrogué" to "8",
            "burzaco" to "49",
            "longchamps" to "239",
            "glew" to "152",
            "guernica" to "165",
            "alejandro korn" to "13",
            "a. korn" to "13",
            "ezeiza" to "132",
            "monte grande" to "277",
            "m. grande" to "277",
            "el jagüel" to "125",
            "quilmes" to "322",
            "bernal" to "39",
            "don bosco" to "115",
            "wilde" to "430",
            "villa domínico" to "418",
            "sarandí" to "370",
            "ezpeleta" to "133",
            "berazategui" to "38",
            "plátanos" to "309",
            "hudson" to "176",
            "pereyra" to "303",
            "villa elisa" to "419",
            "city bell" to "83",
            "gonnet" to "153",
            "ringuelet" to "334",
            "tolosa" to "392",
            "la plata" to "217",
            "dr. antonio sáenz" to "525",
            "dr. antonio saenz" to "525",
            "dr. a. sáenz" to "525",
            "dr. a. saenz" to "525",
            "dr. a. sáenz viad." to "525",
            "dr. a. saenz viad." to "525",
            "dr. a. sáenz viaducto" to "525",
            "dr. a. saenz viaducto" to "525",
            "sáenz" to "525",
            "saenz" to "525",
            "gonzález catán" to "154",
            "gonzalez catan" to "154",
            "marinos del crucero gral. belgrano" to "259",
            "marinos del crucero general belgrano" to "259",
            "marinos c. g belgrano" to "259",
            // Tren de la Costa
            "avenida maipú" to "248",
            "avenida maipu" to "248",
            "av. maipú" to "248",
            "av. maipu" to "248",
            "av maipú" to "248",
            "av maipu" to "248",
            "maipú" to "248",
            "maipu" to "248",
            "delta" to "104",
            "borges" to "42",
            "libertador" to "233",
            "juan anchorena" to "198",
            "anchorena" to "198",
            "las barrancas" to "222",
            "barrancas" to "222",
            "san isidro r" to "358",
            "punta chica" to "319",
            "marina nueva" to "258",
            "san fernando r" to "356",
            "canal san fernando" to "58",
            "c. san fernando" to "58"
        )
    }

    private val stationIdCache = ConcurrentHashMap<String, String>(PRESEEDED_SOFSE_IDS)
    private val stationCoordsCache = ConcurrentHashMap<String, Coordinates>()

    private suspend fun ensureStationCoordsCache() {
        if (stationCoordsCache.isEmpty()) {
            try {
                val allStations = stationDao.getAll()
                for (stn in allStations) {
                    val lat = stn.latitude
                    val lon = stn.longitude
                    if (lat != null && lon != null) {
                        val coords = Coordinates(lat, lon)
                        stationCoordsCache[normalizeStationKey(stn.name)] = coords
                        stationCoordsCache[stn.id] = coords
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error caching station coordinates: ${e.message}")
            }
        }
    }

    private fun findStationCoordinatesSync(stationName: String): Coordinates? {
        val clean = normalizeStationKey(stationName)
        return stationCoordsCache[clean] ?: stationCoordsCache.entries.firstOrNull {
            clean.contains(it.key) || it.key.contains(clean)
        }?.value
    }

    private fun normalizeStationKey(name: String): String =
        name.lowercase().trim()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n")

    override suspend fun getDepartures(
        originId: String,
        destinationId: String?,
        departureTime: LocalDateTime
    ): Result<List<Departure>> = withContext(Dispatchers.IO) {
        ensureStationCoordsCache()
        val originStation = stationDao.getById(originId)
            ?: return@withContext Result.Error("Estación no encontrada")

        val branch = branchDao.getById(originStation.branchId)
        val line = lineDao.getById(originStation.lineId)
        val destStation = if (!destinationId.isNullOrEmpty()) stationDao.getById(destinationId) else null
        val branchDisplayName = branch?.name ?: line?.name ?: "Principal"

        val isConcession = originStation.lineId in listOf("belgrano_norte", "urquiza")

        // 1. PRIVATIZED CONCESSIONS (Belgrano Norte / Urquiza - No official SOFSE GPS)
        if (isConcession) {
            return@withContext Result.Error("Línea concesionada sin datos en tiempo real de SOFSE")
        }

        // 2. OFFICIAL SOFSE TRAINS (Mitre, Roca, Sarmiento, San Martín, Belgrano Sur, Tren de la Costa)
        try {
            val sofseStationId = resolveSofseStationId(originStation)
                ?: return@withContext Result.Error("Estación no encontrada en los servidores de SOFSE")

            val arribosResponse = sofseApiService.getArribos(sofseStationId)
            if (arribosResponse.results.isEmpty()) {
                return@withContext Result.Error("Sin servicios próximos reportados por SOFSE")
            }

            val departures = arribosResponse.results.mapNotNull { result ->
                mapSofseResultToDeparture(result, originStation, branch, branchDisplayName)
            }

            // If user filtered by destination, apply filter
            val filteredDepartures = if (destStation != null) {
                val destNameNorm = normalizeTerminus(destStation.name)
                val matches = departures.filter { dep ->
                    val depDestNorm = normalizeTerminus(dep.destination)
                    if (depDestNorm.contains(destNameNorm) || destNameNorm.contains(depDestNorm)) return@filter true

                    val cachedJourney = journeyDetailsCache.get(dep.serviceId)
                    if (cachedJourney != null) {
                        return@filter cachedJourney.stops.any { stop ->
                            val stopNorm = normalizeTerminus(stop.stationName)
                            stopNorm.contains(destNameNorm) || destNameNorm.contains(stopNorm)
                        }
                    }
                    false
                }
                if (matches.isNotEmpty()) matches else departures
            } else {
                departures
            }

            if (filteredDepartures.isNotEmpty()) {
                Result.Success(filteredDepartures)
            } else {
                Result.Error("No se encontraron servicios próximos hacia ese destino")
            }
        } catch (e: Exception) {
            Log.w(TAG, "SOFSE API call failed for ${originStation.name}: ${e.message}", e)
            Result.Error("Error de conexión con SOFSE")
        }
    }

    private suspend fun resolveSofseStationId(originStation: com.moovar.android.core.database.entity.StationEntity): String? {
        val cleanName = originStation.name.lowercase().trim()
        val cached = stationIdCache[cleanName]
        if (cached != null) return cached

        // Fast line-specific terminus overrides
        if (originStation.lineId == "mitre" && cleanName.contains("retiro")) return "332"
        if (originStation.lineId == "san_martin" && cleanName.contains("retiro")) return "463"
        if (originStation.lineId == "belgrano_norte" && cleanName.contains("retiro")) return null // Concesión
        if (originStation.lineId == "roca" && (cleanName.contains("constituci") || cleanName.contains("plaza c"))) return "93"
        if (originStation.lineId == "sarmiento" && cleanName.contains("once")) return "293"
        if (originStation.lineId == "belgrano_sur" && (cleanName.contains("sáenz") || cleanName.contains("saenz"))) return "525"
        if (originStation.lineId == "tren_costa" && (cleanName.contains("maip") || cleanName.contains("avenida maip"))) return "248"
        if (originStation.lineId == "tren_costa" && cleanName.contains("delta")) return "104"

        // Strip parentheses: "retiro (mitre)" -> "retiro"
        val strippedName = cleanName.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
        val strippedCached = stationIdCache[strippedName]
        if (strippedCached != null) {
            stationIdCache[cleanName] = strippedCached
            return strippedCached
        }

        val stopId = originStation.gtfsStopId
        if (!stopId.isNullOrBlank() && stopId.all { it.isDigit() }) {
            stationIdCache[cleanName] = stopId
            return stopId
        }

        // Query SOFSE estaciones search endpoint
        return try {
            val queryClean = strippedName
                .replace(Regex("""^(dr\.|doctor|gral\.|general|avenida|av\.)\s+"""), "")
                .trim()
            val queryName = queryClean.ifBlank { strippedName.ifBlank { originStation.name } }
            val candidates = sofseApiService.getEstaciones(nombre = queryName)
            if (candidates.isNotEmpty()) {
                val matched = candidates.firstOrNull {
                    it.nombre.equals(queryName, ignoreCase = true) ||
                    it.nombre.equals(originStation.name, ignoreCase = true)
                } ?: candidates.first()
                stationIdCache[cleanName] = matched.idEstacion
                matched.idEstacion
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving SOFSE station ID for ${originStation.name}: ${e.message}")
            null
        }
    }

    private fun mapSofseResultToDeparture(
        result: com.moovar.android.core.network.sofse.dto.ArriboResultDto,
        originStation: com.moovar.android.core.database.entity.StationEntity,
        branch: com.moovar.android.core.database.entity.BranchEntity?,
        branchDisplayName: String
    ): Departure? {
        val arribo = result.arribo ?: return null
        val servicio = result.servicio ?: return null

        val seconds = arribo.segundos ?: 0
        val minutesAway = maxOf(0, Math.round(seconds / 60.0).toInt())

        val rawIso = arribo.salida?.programada ?: arribo.llegada?.programada ?: arribo.salida?.estimada
        val scheduledTime = formatIsoTimeToLocal(rawIso) ?: "En viaje"
        val estimatedTime = formatIsoTimeToLocal(arribo.salida?.estimada ?: arribo.llegada?.estimada)

        val targetDest = servicio.hasta?.estacion?.nombre ?: branch?.destinationTerminus ?: "Terminal"
        val normDest = normalizeTerminus(targetDest)
        val normOrigin = normalizeTerminus(originStation.name)
        if (normDest == normOrigin || (normOrigin.isNotEmpty() && normDest.isNotEmpty() && normOrigin.contains(normDest))) {
            return null
        }

        val directionLabel = "Sentido $targetDest"

        val platform = arribo.anden?.nombre?.let { "Andén $it" } ?: "-"
        val isCancelled = servicio.cancelacion != null
        val serviceType = if (servicio.equipo?.esElectrico == 0) "Diésel" else "Regular"

        val status = when {
            isCancelled -> "CANCELADO"
            originStation.isTerminus -> "En andén - Sale $scheduledTime"
            minutesAway <= 2 -> "Próximo a arribar"
            estimatedTime != null && estimatedTime != scheduledTime -> "Estimado: $estimatedTime"
            else -> "A horario"
        }

        // Clean safeServiceId without slashes
        val safeDest = targetDest.replace("/", "-")
        val safePlatform = platform.replace("/", "-")
        val safeServiceType = serviceType.replace("/", "-")
        val safeStatus = status.replace("/", "-")
        val safeServiceId = "srv___${branch?.id ?: ""}___${safeDest}___${originStation.id}___${minutesAway}___${scheduledTime}___${safePlatform}___${safeServiceType}___${safeStatus}___$isCancelled"

        // Map and cache real official SOFSE station stops sequence
        val estaciones = servicio.estaciones
        var trainCoords: Coordinates? = null

        if (estaciones.isNotEmpty()) {
            val currentIdx = when {
                isCancelled -> {
                    val orgIdx = estaciones.indexOfFirst { it.nombre.equals(originStation.name, ignoreCase = true) }
                    if (orgIdx >= 0) orgIdx else 0
                }
                else -> {
                    val firstFuture = estaciones.indexOfFirst { (it.segundos ?: 0) > 0 }
                    if (firstFuture >= 0) firstFuture else estaciones.lastIndex
                }
            }.coerceIn(0, estaciones.lastIndex)

            val stops = estaciones.mapIndexed { idx, est ->
                val rawStopIso = est.salida?.estimada ?: est.salida?.programada ?: est.llegada?.estimada ?: est.llegada?.programada
                val stopTime = formatIsoTimeToLocal(rawStopIso)
                val state = when {
                    idx < currentIdx -> StopState.PAST
                    idx == currentIdx -> StopState.CURRENT
                    else -> StopState.FUTURE
                }
                val stopCoords = est.nombre?.let { findStationCoordinatesSync(it) }
                JourneyStop(
                    stationName = est.nombre ?: "Estación",
                    scheduledTime = stopTime,
                    stopState = state,
                    isTerminus = (idx == 0 || idx == estaciones.lastIndex),
                    coordinates = stopCoords
                )
            }

            val originLat = originStation.latitude
            val originLon = originStation.longitude
            val originCoords = if (originLat != null && originLon != null) Coordinates(originLat, originLon) else null

            if (!originStation.isTerminus && !isCancelled) {
                val currentStnName = estaciones[currentIdx].nombre ?: originStation.name
                trainCoords = findStationCoordinatesSync(currentStnName) ?: originCoords
            }

            val journeyDetails = JourneyDetails(
                branchName = servicio.ramal?.nombre ?: branchDisplayName,
                serviceType = serviceType,
                destination = targetDest,
                platform = platform,
                departureTime = scheduledTime,
                currentStatus = status,
                stops = stops,
                trainCoordinates = trainCoords
            )

            journeyDetailsCache.put(safeServiceId, journeyDetails)
            val srvId = servicio.id
            if (!srvId.isNullOrBlank()) {
                journeyDetailsCache.put(srvId, journeyDetails)
            }
        } else if (!originStation.isTerminus && !isCancelled) {
            val originLat = originStation.latitude
            val originLon = originStation.longitude
            if (originLat != null && originLon != null) {
                trainCoords = Coordinates(originLat, originLon)
            }
        }

        return Departure(
            serviceId = safeServiceId,
            branchName = servicio.ramal?.nombre ?: branchDisplayName,
            destination = targetDest,
            minutesAway = if (isCancelled) 0 else minutesAway,
            scheduledTime = scheduledTime,
            platform = if (isCancelled) "-" else platform,
            serviceType = serviceType,
            status = status,
            vehicleCoordinates = trainCoords,
            networkType = NetworkType.TREN,
            isTerminus = originStation.isTerminus,
            direction = directionLabel,
            isCancelled = isCancelled
        )
    }

    private fun normalizeTerminus(name: String): String {
        var s = name.lowercase()
            .replace(Regex("\\(.*?\\)"), "")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace(".", " ")
            .replace(",", " ")
        for (w in listOf("plaza", "gral", "general", "dr", "antonio", "viad", "prov", "viaducto", "avenida", "av")) {
            s = s.replace(Regex("\\b$w\\b"), " ")
        }
        s = s.replace(Regex("\\b[a-z]\\b"), " ")
        return s.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun formatIsoTimeToLocal(isoStr: String?): String? {
        if (isoStr.isNullOrBlank()) return null
        return try {
            val instant = Instant.parse(isoStr)
            val zdt = instant.atZone(ARGENTINA_ZONE)
            zdt.format(TIME_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }
}

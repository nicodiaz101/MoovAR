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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
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

    var clock: Clock = Clock.system(ARGENTINA_ZONE)

    companion object {
        private const val TAG = "DepartureRepoImpl"
        private val ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val PARENTHESES_REGEX = Regex("\\s*\\([^)]*\\)")
        private val QUERY_PREFIX_REGEX = Regex("""^(dr\.|doctor|gral\.|general|avenida|av\.)\s+""")
        private val TERMINUS_PARENTHESES_REGEX = Regex("\\(.*?\\)")
        private val SINGLE_LETTER_REGEX = Regex("\\b[a-z]\\b")
        private val TERMINUS_KEYWORDS_REGEX = Regex("\\b(plaza|gral|general|dr|antonio|viad|prov|viaducto|avenida|av)\\b")

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
            "c. san fernando" to "58",
            "c san fernando" to "58"
        )

        private val KNOWN_STATION_ALIASES = mapOf(
            "l de la torre" to "lisandro de la torre",
            "c san fernando" to "canal san fernando",
            "retiro (lgm)" to "retiro (mitre)",
            "retiro lgm" to "retiro (mitre)",
            "san fernando c" to "san fernando",
            "san isidro c" to "san isidro",
            "las barrancas" to "barrancas",
            "maipu" to "avenida maipu",
            "bme mitre" to "bartolome mitre",
            "gral urquiza" to "general urquiza",
            "dr cetrangolo" to "cetrangolo"
        )

        // Intermediate rail track waypoints along the curve between Retiro Mitre and Lisandro de la Torre
        private val RETIRO_LISANDRO_TRACK_WAYPOINTS = listOf(
            Coordinates(-34.5908, -58.3752), // Cabecera / Salida de Retiro
            Coordinates(-34.5885, -58.3792), // Curva bajo Autopista Illia
            Coordinates(-34.5860, -58.3826), // Empalme San Martín
            Coordinates(-34.5804, -58.3922), // Playa Saldías Norte
            Coordinates(-34.5736, -58.4021), // Estación Saldías
            Coordinates(-34.5712, -58.4046), // Frente Costanera / Aeroparque
            Coordinates(-34.5678, -58.4113), // Recta Costanera
            Coordinates(-34.5675, -58.4150), // Empalme Maldonado
            Coordinates(-34.5662, -58.4200), // Curva Palermo (Rosedal / Dorrego)
            Coordinates(-34.5647, -58.4232), // Inicio Viaducto Mitre
            Coordinates(-34.5636, -58.4285)  // Viaducto Olleros
        )
    }

    private val coordsCacheMutex = Mutex()
    @Volatile private var isCoordsCacheInitialized = false

    private val stationIdCache = ConcurrentHashMap<String, String>(PRESEEDED_SOFSE_IDS)
    private val lineStationCoordsCache = ConcurrentHashMap<String, MutableMap<String, Coordinates>>()
    private val globalStationCoordsCache = ConcurrentHashMap<String, Coordinates>()
    private val lineSofseCoordsCache = ConcurrentHashMap<String, MutableMap<String, Coordinates>>()
    private val globalSofseCoordsCache = ConcurrentHashMap<String, Coordinates>()

    private suspend fun ensureStationCoordsCache() {
        if (isCoordsCacheInitialized) return
        coordsCacheMutex.withLock {
            if (isCoordsCacheInitialized) return
            try {
                val allStations = stationDao.getAll()
                if (allStations.isEmpty()) return@withLock
                for (stn in allStations) {
                    val lat = stn.latitude
                    val lon = stn.longitude
                    if (lat != null && lon != null) {
                        val coords = Coordinates(lat, lon)
                        val lineMap = lineStationCoordsCache.getOrPut(stn.lineId) { ConcurrentHashMap() }
                        val lineSofseMap = lineSofseCoordsCache.getOrPut(stn.lineId) { ConcurrentHashMap() }
                        val cleanName = normalizeStationKey(stn.name)

                        lineMap[cleanName] = coords
                        globalStationCoordsCache[cleanName] = coords

                        // Index by GTFS stop ID if available and numeric
                        stn.gtfsStopId?.let { gid ->
                            if (gid.isNotBlank() && gid.all { it.isDigit() }) {
                                lineSofseMap[gid] = coords
                                globalSofseCoordsCache[gid] = coords
                            }
                        }

                        // Index by known preseeded SOFSE ID
                        PRESEEDED_SOFSE_IDS[cleanName]?.let { sid ->
                            lineSofseMap[sid] = coords
                            globalSofseCoordsCache[sid] = coords
                        }
                        KNOWN_STATION_ALIASES[cleanName]?.let { alias ->
                            PRESEEDED_SOFSE_IDS[alias]?.let { sid ->
                                lineSofseMap[sid] = coords
                                globalSofseCoordsCache[sid] = coords
                            }
                        }

                        val stripped = stripStationSuffixes(cleanName)
                        if (stripped != cleanName) {
                            lineMap[stripped] = coords
                            globalStationCoordsCache[stripped] = coords
                            PRESEEDED_SOFSE_IDS[stripped]?.let { sid ->
                                lineSofseMap[sid] = coords
                                globalSofseCoordsCache[sid] = coords
                            }
                        }
                    }
                }
                isCoordsCacheInitialized = true
            } catch (e: Exception) {
                Log.w(TAG, "Error caching station coordinates: ${e.message}")
            }
        }
    }

    private fun findStationCoordinates(
        stationName: String,
        lineId: String?,
        sofseElementId: Int? = null
    ): Coordinates? {
        // 0. High precision: SOFSE Element ID lookup (scoped to line if possible)
        if (sofseElementId != null) {
            val sidStr = sofseElementId.toString()
            if (lineId != null) {
                lineSofseCoordsCache[lineId]?.get(sidStr)?.let { return it }
            }
            globalSofseCoordsCache[sidStr]?.let { return it }
        }

        val clean = normalizeStationKey(stationName)
        val alias = KNOWN_STATION_ALIASES[clean]
        val targetKey = alias ?: clean

        // Check if preseeded SOFSE ID maps to known coordinates
        val mappedSid = PRESEEDED_SOFSE_IDS[targetKey] ?: PRESEEDED_SOFSE_IDS[clean]
        if (mappedSid != null) {
            if (lineId != null) {
                lineSofseCoordsCache[lineId]?.get(mappedSid)?.let { return it }
            }
            globalSofseCoordsCache[mappedSid]?.let { return it }
        }

        // 1. Same line lookup
        if (lineId != null) {
            lineStationCoordsCache[lineId]?.let { lineMap ->
                lineMap[targetKey]?.let { return it }
                lineMap[clean]?.let { return it }
                val stripped = stripStationSuffixes(targetKey)
                lineMap[stripped]?.let { return it }
                lineMap.entries.firstOrNull { (k, _) ->
                    isFuzzyStationMatch(targetKey, k) || isFuzzyStationMatch(clean, k)
                }?.value?.let { return it }
            }
        }

        // 2. Global exact / alias lookup
        globalStationCoordsCache[targetKey]?.let { return it }
        globalStationCoordsCache[clean]?.let { return it }
        val stripped = stripStationSuffixes(targetKey)
        globalStationCoordsCache[stripped]?.let { return it }

        // 3. Global fuzzy lookup
        return globalStationCoordsCache.entries.firstOrNull { (k, _) ->
            isFuzzyStationMatch(targetKey, k) || isFuzzyStationMatch(clean, k)
        }?.value
    }

    private fun isFuzzyStationMatch(input: String, candidate: String): Boolean {
        if (input == candidate) return true
        if (input.length < 5 || candidate.length < 5) return false

        // Prevent collision between "canal san fernando" and "san fernando" / "san fernando r"
        val inputIsCanal = input.startsWith("c ") || input.startsWith("canal ")
        val candIsCanal = candidate.startsWith("c ") || candidate.startsWith("canal ")
        if (inputIsCanal != candIsCanal) return false

        // Prevent collision between terminal branch variants (e.g., "belgrano c" vs "belgrano r", "san isidro c" vs "san isidro r")
        val inputEndsWithC = input.endsWith(" c")
        val candEndsWithC = candidate.endsWith(" c")
        val inputEndsWithR = input.endsWith(" r")
        val candEndsWithR = candidate.endsWith(" r")
        if ((inputEndsWithC && candEndsWithR) || (inputEndsWithR && candEndsWithC)) {
            return false
        }
        if (inputEndsWithR != candEndsWithR) {
            return false
        }

        return candidate.contains(input) || input.contains(candidate)
    }

    private fun stripStationSuffixes(s: String): String =
        s.replace("(mitre)", "")
            .replace("(san martin)", "")
            .replace("(san martín)", "")
            .replace("(belgrano norte)", "")
            .replace("(belgrano sur)", "")
            .replace("(roca)", "")
            .replace("(sarmiento)", "")
            .replace("(urquiza)", "")
            .replace("(lgm)", "")
            .replace("estacion ", "")
            .replace("estación ", "")
            .replace(".", "")
            .trim()

    private fun normalizeStationKey(name: String): String =
        name.lowercase().trim()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n")
            .replace(".", "")
            .replace(WHITESPACE_REGEX, " ")

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
        val strippedName = cleanName.replace(PARENTHESES_REGEX, "").trim()
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
                .replace(QUERY_PREFIX_REGEX, "")
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
            val firstFuture = if (isCancelled) -1 else estaciones.indexOfFirst { (it.segundos ?: 0) > 0 }
            val now = try { clock.instant() } catch (_: Exception) { Instant.now() }
            var atStationIdx: Int? = null

            if (isCancelled) {
                val orgIdx = estaciones.indexOfFirst { it.nombre.equals(originStation.name, ignoreCase = true) }
                atStationIdx = if (orgIdx >= 0) orgIdx else 0
            } else if (firstFuture == 0) {
                atStationIdx = 0
            } else if (firstFuture == -1) {
                atStationIdx = estaciones.lastIndex
            } else if (firstFuture > 0) {
                val nextEst = estaciones[firstFuture]
                val prevEst = estaciones[firstFuture - 1]
                val secsToNext = nextEst.segundos ?: 60

                // 1. Train is braking into or arrived at nextEst (<= 30 seconds away)
                if (secsToNext <= 30) {
                    atStationIdx = firstFuture
                } else {
                    // 2. Train is still stopped at prevEst (doors open / before departure)
                    val prevSalidaIso = prevEst.salida?.estimada ?: prevEst.salida?.programada
                    val isStillAtPrevStation = if (!prevSalidaIso.isNullOrBlank()) {
                        try {
                            val salidaInstant = Instant.parse(prevSalidaIso)
                            now.isBefore(salidaInstant.plusSeconds(30))
                        } catch (_: Exception) {
                            val totalSecs = calculateSegmentDurationSecs(prevEst, nextEst, secsToNext)
                            (totalSecs - secsToNext) <= 25
                        }
                    } else {
                        val totalSecs = calculateSegmentDurationSecs(prevEst, nextEst, secsToNext)
                        (totalSecs - secsToNext) <= 25
                    }

                    if (isStillAtPrevStation) {
                        atStationIdx = firstFuture - 1
                    }
                }
            }

            val isInTransit = !isCancelled && atStationIdx == null && firstFuture > 0

            val stops = estaciones.mapIndexed { idx, est ->
                val rawStopIso = est.salida?.estimada ?: est.salida?.programada ?: est.llegada?.estimada ?: est.llegada?.programada
                val stopTime = formatIsoTimeToLocal(rawStopIso)
                val state = when {
                    atStationIdx != null -> {
                        when {
                            idx < atStationIdx -> StopState.PAST
                            idx == atStationIdx -> StopState.CURRENT
                            else -> StopState.FUTURE
                        }
                    }
                    isInTransit -> {
                        when {
                            idx < firstFuture -> StopState.PAST
                            else -> StopState.FUTURE
                        }
                    }
                    else -> StopState.FUTURE
                }
                val stopCoords = est.nombre?.let { findStationCoordinates(it, originStation.lineId, est.idElemento) }
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
                trainCoords = when {
                    atStationIdx != null -> {
                        val atEst = estaciones.getOrNull(atStationIdx)
                        atEst?.nombre?.let {
                            findStationCoordinates(it, originStation.lineId, atEst.idElemento)
                        } ?: originCoords
                    }
                    firstFuture > 0 -> {
                        val prevEst = estaciones[firstFuture - 1]
                        val nextEst = estaciones[firstFuture]
                        val prevCoords = prevEst.nombre?.let { findStationCoordinates(it, originStation.lineId, prevEst.idElemento) }
                        val nextCoords = nextEst.nombre?.let { findStationCoordinates(it, originStation.lineId, nextEst.idElemento) }
                        val secs = (nextEst.segundos ?: 60).coerceAtLeast(0)

                        if (prevCoords != null && nextCoords != null) {
                            // Interpolate between prev and next station along the rail line based on segment duration
                            val totalSegmentSecs = calculateSegmentDurationSecs(prevEst, nextEst, secs)
                            val progress = (1.0 - (secs.toDouble() / totalSegmentSecs.toDouble())).coerceIn(0.08, 0.92)
                            interpolateCoordinatesAlongSegment(prevEst, nextEst, prevCoords, nextCoords, progress)
                        } else {
                            nextCoords ?: prevCoords ?: originCoords
                        }
                    }
                    else -> originCoords
                }
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

    private fun calculateSegmentDurationSecs(
        prevEst: com.moovar.android.core.network.sofse.dto.EstacionParadaDto,
        nextEst: com.moovar.android.core.network.sofse.dto.EstacionParadaDto,
        remainingSecs: Int
    ): Long {
        val prevIso = prevEst.salida?.estimada ?: prevEst.salida?.programada ?: prevEst.llegada?.programada
        val nextIso = nextEst.llegada?.estimada ?: nextEst.llegada?.programada ?: nextEst.salida?.programada
        if (!prevIso.isNullOrBlank() && !nextIso.isNullOrBlank()) {
            try {
                val prevInstant = Instant.parse(prevIso)
                val nextInstant = Instant.parse(nextIso)
                val diff = java.time.Duration.between(prevInstant, nextInstant).seconds
                if (diff in 45..1800) {
                    return maxOf(diff, remainingSecs.toLong())
                }
            } catch (_: Exception) {
                // Ignore parsing errors and fallback
            }
        }
        return maxOf(remainingSecs.toLong(), 180L)
    }

    private fun interpolateCoordinatesAlongSegment(
        prevEst: com.moovar.android.core.network.sofse.dto.EstacionParadaDto,
        nextEst: com.moovar.android.core.network.sofse.dto.EstacionParadaDto,
        prevCoords: Coordinates,
        nextCoords: Coordinates,
        progress: Double
    ): Coordinates {
        val polyline = getSegmentPolyline(prevEst, nextEst, prevCoords, nextCoords)
        if (polyline.size <= 2) {
            val lat = prevCoords.latitude + (nextCoords.latitude - prevCoords.latitude) * progress
            val lon = prevCoords.longitude + (nextCoords.longitude - prevCoords.longitude) * progress
            return Coordinates(lat, lon)
        }
        return interpolatePolyline(polyline, progress)
    }

    private fun getSegmentPolyline(
        prevEst: com.moovar.android.core.network.sofse.dto.EstacionParadaDto,
        nextEst: com.moovar.android.core.network.sofse.dto.EstacionParadaDto,
        prevCoords: Coordinates,
        nextCoords: Coordinates
    ): List<Coordinates> {
        val prevId = prevEst.idElemento
        val nextId = nextEst.idElemento
        val prevKey = normalizeStationKey(prevEst.nombre.orEmpty())
        val nextKey = normalizeStationKey(nextEst.nombre.orEmpty())

        val isRetiroToLisandro = (prevId == 332 && nextId == 229) ||
                (prevKey.contains("retiro") && nextKey.contains("lisandro"))
        if (isRetiroToLisandro) {
            return listOf(prevCoords) + RETIRO_LISANDRO_TRACK_WAYPOINTS + listOf(nextCoords)
        }

        val isLisandroToRetiro = (prevId == 229 && nextId == 332) ||
                (prevKey.contains("lisandro") && nextKey.contains("retiro"))
        if (isLisandroToRetiro) {
            return listOf(prevCoords) + RETIRO_LISANDRO_TRACK_WAYPOINTS.reversed() + listOf(nextCoords)
        }

        return listOf(prevCoords, nextCoords)
    }

    private fun interpolatePolyline(points: List<Coordinates>, progress: Double): Coordinates {
        val pClamped = progress.coerceIn(0.0, 1.0)
        if (points.size <= 1) return points.firstOrNull() ?: Coordinates(0.0, 0.0)

        var totalDist = 0.0
        val segDists = DoubleArray(points.size - 1)
        for (i in 0 until points.size - 1) {
            val dLat = points[i + 1].latitude - points[i].latitude
            val avgLatRad = Math.toRadians((points[i + 1].latitude + points[i].latitude) / 2.0)
            val dLon = (points[i + 1].longitude - points[i].longitude) * Math.cos(avgLatRad)
            val d = Math.hypot(dLat, dLon)
            segDists[i] = d
            totalDist += d
        }

        if (totalDist == 0.0) return points.first()

        val targetDist = pClamped * totalDist
        var accum = 0.0
        for (i in segDists.indices) {
            val d = segDists[i]
            if (accum + d >= targetDist || i == segDists.lastIndex) {
                val segProgress = if (d > 0.0) ((targetDist - accum) / d).coerceIn(0.0, 1.0) else 0.0
                val lat = points[i].latitude + (points[i + 1].latitude - points[i].latitude) * segProgress
                val lon = points[i].longitude + (points[i + 1].longitude - points[i].longitude) * segProgress
                return Coordinates(lat, lon)
            }
            accum += d
        }
        return points.last()
    }

    private fun normalizeTerminus(name: String): String {
        var s = name.lowercase()
            .replace(TERMINUS_PARENTHESES_REGEX, "")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace(".", " ")
            .replace(",", " ")
            .replace(TERMINUS_KEYWORDS_REGEX, " ")
            .replace(SINGLE_LETTER_REGEX, " ")
        return s.split(WHITESPACE_REGEX).filter { it.isNotBlank() }.joinToString(" ")
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

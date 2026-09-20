# 🤖 MoovAR — MANUAL DE ORQUESTACIÓN DE AGENTES (AGENTS)

> **Propósito:** Este documento define cómo se delegará el trabajo de código a agentes de IA especializados. Cada sección especifica: qué agente se usa, qué prompt recibe, qué artefactos produce, y cuáles son los criterios de aceptación.

---

## PRINCIPIOS DE ORQUESTACIÓN

### R1 — Verificación Post-Fase Obligatoria
Al finalizar cualquier fase de desarrollo (o iteración importante), el agente DEBE ejecutar el script `verify_phase.sh` (o el equivalente comando de Gradle `./gradlew assembleDebug`) para asegurar que no existan errores de compilación ni problemas en Gradle. **Importante:** La ejecución de Gradle debe hacerse usando **JDK 21** para mantener compatibilidad con las dependencias del proyecto (ej. Kotlin 2.3.0). El entorno local debe ser configurado con:
`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk && export ANDROID_HOME=/home/nicolas/Android/Sdk && export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"`
Nunca asumas que un scaffold o refactor está correcto sin haber pasado el `assembleDebug`.


### P1 — Máximo Contexto Relevante, Mínimo Ruido
Cada agente recibe **sólo el fragmento de SPECS.MD relevante a su tarea**, más las interfaces del módulo vecino que necesita implementar. No se envía el SPECS completo a cada agente; se segmenta para no saturar el contexto.

### P2 — Contratos Primero (Interfaces Before Implementation)
Antes de delegar la implementación de cualquier clase, su interfaz ya debe estar definida y aprobada en SPECS.MD. Los agentes implementan contratos; no los diseñan.

### P3 — El Agente que Genera, No Revisa
Un agente que generó código no es el revisor del mismo. Siempre hay una segunda pasada de validación, ya sea por un segundo agente o por un set de tests automatizados.

### P4 — Handoff a través de Archivos, No de Memoria
Los artefactos de un agente (archivos `.kt`) son la entrada del siguiente. No se confía en que el agente recuerde contexto de una sesión anterior.

---

## MATRIZ DE ROLES DE AGENTES

| Agente | Modelo Recomendado | Rol | Fortaleza |
|--------|--------------------|-----|-----------|
| **Arquitecto Base** | Gemini 2.5 Pro | Scaffold del proyecto, estructura de módulos, configuración Gradle | Razonamiento multi-archivo, coherencia global |
| **Engineer: Database** | Gemini 2.5 Flash | Entidades Room, DAOs, TypeConverters, Migrations, Seeder | Código repetitivo y preciso |
| **Engineer: Network SOFSE** | Gemini 2.5 Flash | Retrofit, DTOs, Interceptores, Mappers | Código boilerplate de red |
| **Engineer: Network GTFS** | Gemini 2.5 Pro | Configuración Protobuf, GtfsRtDataSource, GtfsRtMapper | Complejidad de build system + Protobuf |
| **Engineer: Domain** | Gemini 2.5 Flash | Use Cases, Repositorios, Result wrapper | Lógica pura, sin dependencias complejas |
| **Engineer: UI Compose** | Gemini 2.5 Flash | Screens, Composables, Temas, Animaciones | Volumen de código Compose |
| **Engineer: ViewModel** | Gemini 2.5 Flash | ViewModels, UiState, SharedFlow events | Patrón MVVM repetitivo |
| **QA Engineer** | Gemini 2.5 Pro | Unit tests, Integration tests, Revisión de código | Detección de edge cases |

---

## FASE 0 — AGENTE ARQUITECTO BASE

### Tarea
Generar el scaffold completo del proyecto multi-módulo.

### Modelo
`Gemini 2.5 Pro` — Necesita razonar sobre la coherencia de múltiples archivos `build.gradle.kts` simultáneamente.

### Prompt Base

```
Eres un experto en Android con Kotlin y Gradle. Necesito que generes el scaffold
completo de un proyecto Android multi-módulo con la siguiente estructura:

MÓDULOS REQUERIDOS:
:app, :core:database, :core:network, :core:domain, :core:common,
:feature:home, :feature:departures, :feature:journey,
:feature:alerts, :feature:map, :feature:favorites

CONFIGURACIÓN OBLIGATORIA:
- Kotlin DSL (*.kts) para todos los build files
- Version Catalog en libs.versions.toml (ver lista adjunta)
- KSP como procesador de anotaciones (no KAPT)
- compileSdk = 36, minSdk = 30, targetSdk = 36
- Hilt habilitado en :app y todos los :feature:*
- Compose BOM alineado en todos los módulos que usen UI
- buildFeatures { compose = true } solo en módulos de feature y :app
- :core:domain debe ser Kotlin-only (sin dependencias de Android framework)
- Proguard/R8 habilitado en release con reglas para Retrofit, Room y Protobuf

RESTRICCIONES DE DEPENDENCIAS ENTRE MÓDULOS:
- :feature:* puede depender de :core:* pero NO de otros :feature:*
- :core:database y :core:network NO pueden depender entre sí
- :core:domain no puede importar ni Room ni Retrofit
- :app depende de todos los módulos para el DI wiring

ENTREGABLES:
1. settings.gradle.kts (con todos los módulos incluídos)
2. build.gradle.kts raíz
3. libs.versions.toml completo (ver versiones en ROADMAP.MD §0.2)
4. build.gradle.kts de cada módulo (content mínimo: plugins + dependencias correctas)
5. AndroidManifest.xml de :app (con INTERNET permission y SIN permisos de ubicación)
6. MainActivity.kt con setContent { TransportArApp() }
7. TransportArApp.kt con NavHost básico y BottomNavigationBar placeholder

Genera cada archivo con su ruta relativa al proyecto completa.
No generes implementaciones de clases de negocio, solo el scaffold.
```

### Criterios de Aceptación
- [ ] `./gradlew :app:assembleDebug` compila sin errores.
- [ ] Ningún módulo `:core:*` declara dependencia de otro `:core:*` (excepto `:core:common`).
- [ ] `AndroidManifest.xml` no contiene permisos de ubicación.
- [ ] KSP está configurado (no KAPT).

### Artefactos Producidos
Todos los archivos `build.gradle.kts`, `libs.versions.toml`, `settings.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`.

---

## FASE 1 — AGENTE ENGINEER: DATABASE

### Tarea
Implementar toda la capa de base de datos Room.

### Modelo
`Gemini 2.5 Flash` — El código es preciso pero repetitivo. Flash es suficiente y más rápido.

### Prompt Base

```
Eres un experto en Jetpack Room (SQLite). Tienes el siguiente contrato de entidades
y DAOs definidos en SPECS.MD. Tu tarea es implementar TODOS los archivos del módulo
:core:database.

CONTRATO DE ENTIDADES (del SPECS.MD §2.2):
[Pegar aquí los data class de LineEntity, BranchEntity, StationEntity, 
AlertEntity, RecentStationEntity, FavoriteRouteEntity]

CONTRATO DE DAOs (del SPECS.MD §2.4):
[Pegar aquí las interfaces de LineDao, BranchDao, StationDao, AlertDao,
RecentStationDao, FavoriteRouteDao]

ENTREGABLES REQUERIDOS:

1. Todas las @Entity data classes con:
   - Foreign keys e índices según el diagrama ER
   - TypeConverters para enums (NetworkType, LineStatus, AlertSeverity)
   - Anotaciones KSP (@Entity, @PrimaryKey, @ForeignKey, @Index)

2. Todos los @Dao interfaces con:
   - Queries suspend para operaciones puntuales
   - Queries Flow<T> para observación reactiva
   - @Upsert para inserciones idempotentes (Room 2.5+)

3. AppDatabase.kt:
   - @Database con version = 1, lista de todas las entidades
   - RoomDatabase.Callback con override de onCreate para llamar al DatabaseSeeder
   - Migration_1_2 como stub vacío (preparación para futuras migraciones)
   - exportSchema = true

4. DatabaseSeeder.kt:
   - Función suspend seedInitialData(db: AppDatabase)
   - Lee el archivo assets/seed_data.json
   - Parsea con kotlinx.serialization
   - Inserta todas las líneas, ramales y estaciones en las tablas correspondientes
   - Debe ser idempotente (verificar si ya hay datos antes de insertar)

5. seed_data.json en assets/:
   - 7 líneas de tren: Roca, Sarmiento, Mitre, San Martín, Belgrano Sur, 
     Belgrano Norte, Tren de la Costa
   - Ramales completos de cada línea con sus estaciones (con sequenceInBranch correcto)
   - 6 líneas de Subte (A, B, C, D, E, H) con sus estaciones completas
   - Para Subte: latitude y longitude = null
   - Para Tren: incluir coordenadas aproximadas de las estaciones terminales

6. DatabaseModule.kt (Hilt):
   - Provee AppDatabase como @Singleton
   - Provee cada @Dao desde la instancia de AppDatabase
   - Provee Context de la aplicación para el seeder

REGLAS IMPORTANTES:
- NO usar KAPT. Usar KSP (@KspDependent)
- La clase AppDatabase va en :core:database
- El seeder debe loggear con Log.d() las líneas insertadas
- Usar @Upsert (no @Insert con REPLACE) para idempotencia limpia

Genera cada archivo con su ruta completa dentro de :core:database.
```

### Criterios de Aceptación
- [ ] `./gradlew :core:database:test` pasa (tests de DAO con in-memory DB).
- [ ] `DatabaseSeeder` inserta correctamente al menos 7 líneas y 100 estaciones.
- [ ] Ninguna entidad tiene `@ForeignKey` apuntando a una tabla que no existe.
- [ ] Los `Flow<T>` en los DAOs observacionales no tienen `suspend`.

### Artefactos Producidos
`LineEntity.kt`, `BranchEntity.kt`, `StationEntity.kt`, `AlertEntity.kt`, `RecentStationEntity.kt`, `FavoriteRouteEntity.kt`, `LineDao.kt`, `BranchDao.kt`, `StationDao.kt`, `AlertDao.kt`, `RecentStationDao.kt`, `FavoriteRouteDao.kt`, `AppDatabase.kt`, `DatabaseSeeder.kt`, `RoomTypeConverters.kt`, `DatabaseModule.kt`, `assets/seed_data.json`.

---

## FASE 2 — AGENTE ENGINEER: NETWORK SOFSE

### Tarea
Implementar el cliente Retrofit para la API de SOFSE con los interceptores de autenticación.

### Modelo
`Gemini 2.5 Flash`

### Prompt Base

```
Eres un experto en Retrofit, OkHttp y seguridad en Android. Implementa la capa
de red para la API de SOFSE en el módulo :core:network.

CONTEXTO DE LA API:
- Base URL: https://api-servicios.sofse.gob.ar/v3
- Autenticación: Token Bearer temporal
- Mecanismo de refresh: MD5 hash de "V3rS10n$SOFSE" → POST /v3/auth/token → Bearer token

CONTRATO DE INTERCEPTORES (del SPECS.MD §3.1 y §3.2):
[Pegar código de StaticHeadersInterceptor y TokenManagerInterceptor]

CONTRATO DE DTOs (del SPECS.MD §3.3):
[Pegar código de LineStatusDto, DepartureDto, JourneyDto, StopDto]

ENTREGABLES REQUERIDOS:

1. StaticHeadersInterceptor.kt:
   - Agrega x-api-key: dXN1YXJpb2FwcDphcHAyeG1s
   - Agrega Referer: https://api-servicios.sofse.gob.ar
   - Agrega Accept: application/json

2. TokenManagerInterceptor.kt:
   - Inyecta Bearer token en cada request
   - En 401: genera MD5 hash de "V3rS10n$SOFSE" (java.security.MessageDigest)
   - Llama a SofseAuthApi.getToken(hash) con .execute() (sincrónico)
   - Guarda el token en TokenStorage (EncryptedSharedPreferences)
   - Reintenta la request original (máximo 1 reintento para evitar loop infinito)
   - Thread-safe: usar @Synchronized en el bloque de refresh

3. TokenStorage.kt:
   - Wrapper sobre EncryptedSharedPreferences (Jetpack Security)
   - Funciones: saveToken(token: String), getToken(): String?, clearToken()

4. SofseApiService.kt (Retrofit):
   - @GET /v3/auth/token — para obtener token inicial
   - @GET /v3/lineas — lista de líneas con estado
   - @GET /v3/ramales/{lineaId} — ramales de una línea
   - @GET /v3/estaciones/{ramalId} — estaciones de un ramal
   - @GET /v3/proximos/{origen}/{destino} — próximos trenes
   - @GET /v3/recorrido/{servicioId} — recorrido del servicio
   - @GET /v3/alertas — alertas (con @Query opcional de lineaId)

5. Todos los DTOs @Serializable (LineStatusDto, DepartureDto, JourneyDto, StopDto)

6. SofseRemoteDataSource.kt:
   - Llama a cada endpoint y mapea DTO → Domain entity
   - Maneja errores HTTP con try/catch devolviendo Result<T>
   - NO tiene estado propio (stateless)

7. NetworkModule.kt (Hilt):
   - Provee SofseAuthApi (Retrofit SIN TokenManagerInterceptor para evitar recursión)
   - Provee OkHttpClient con los 3 interceptores (Static, Token, y Logging solo en DEBUG)
   - Provee Retrofit principal con el OkHttpClient completo
   - Provee SofseApiService
   - Provee SofseRemoteDataSource

REGLAS IMPORTANTES:
- SofseAuthApi y SofseApiService son interfaces Retrofit DISTINTAS
- SofseAuthApi usa un OkHttpClient SIN TokenManagerInterceptor (para evitar recursión infinita)
- El HttpLoggingInterceptor solo se agrega en BuildConfig.DEBUG
- Usar kotlinx.serialization (no Gson) para los DTOs
- No usar RxJava; solo coroutines y Flow

Genera cada archivo con su ruta completa dentro de :core:network.
```

### Criterios de Aceptación
- [ ] Test con `MockWebServer`: simular 401 → verificar que el interceptor hace refresh y reintenta.
- [ ] Test con `MockWebServer`: simular doble 401 → verificar que NO hay más de 1 reintento.
- [ ] `TokenStorage` usa `EncryptedSharedPreferences` (verificar mediante inspección de código).
- [ ] `HttpLoggingInterceptor` NO está en el grafo Hilt de la variante `release`.

### Artefactos Producidos
`StaticHeadersInterceptor.kt`, `TokenManagerInterceptor.kt`, `TokenStorage.kt`, `SofseAuthApi.kt`, `SofseApiService.kt`, DTOs varios, `SofseRemoteDataSource.kt`, `NetworkModule.kt`.

---

## FASE 3 — AGENTE ENGINEER: NETWORK GTFS-RT

### Tarea
Configurar el plugin de Protobuf y el cliente GTFS-RT para el Subte.

### Modelo
`Gemini 2.5 Pro` — La configuración del plugin Protobuf en Gradle es compleja y requiere razonamiento sobre el build system.

### Prompt Base

```
Eres un experto en Protocol Buffers, GTFS-RT y Android Gradle. Tu tarea es
configurar el cliente de datos en tiempo real para el Subte de Buenos Aires
usando el estándar GTFS-RT (Google Transit Feed Specification - Realtime).

CONTEXTO:
- El feed es binario Protobuf (no JSON)
- URL del feed: [URL del GCBA a confirmar]
- La especificación oficial está en gtfs-realtime.proto
- Las líneas de Subte NO reportan coordenadas GPS

ENTREGABLES REQUERIDOS:

1. Modificar build.gradle.kts de :core:network para agregar:
   - Plugin: com.google.protobuf (version 0.9.4)
   - Dependencia: com.google.protobuf:protobuf-kotlin-lite
   - Dependencia: com.google.transit:gtfs-realtime-bindings
   - Bloque protobuf { generateProtoTasks { all().forEach { task ->
       task.builtins { id("kotlin") { option("lite") } }
     }}}
   - Directorio src/main/proto/ con el archivo gtfs-realtime.proto

2. gtfs-realtime.proto:
   - Descargar y copiar la spec oficial de https://github.com/google/transit
   - NO modificar el archivo .proto

3. GtfsRtDataSource.kt:
   - Usa OkHttpClient puro (no Retrofit) para llamar al feed
   - Accept: application/x-protobuf
   - Lee response.body.bytes() y parsea con FeedMessage.parseFrom(bytes)
   - Devuelve FeedMessage

4. GtfsRtMapper.kt:
   - mapTripUpdates(feed: FeedMessage): Map<String, List<SubteArrival>>
     - Agrupa StopTimeUpdates por stop_id
     - Extrae arrival.delay y arrival.time
   - mapVehiclePositions(feed: FeedMessage): List<VehicleLocation>
     - hasCoordinates = false SIEMPRE (regla de negocio para Subte)
     - latitude = null, longitude = null SIEMPRE

5. SubteArrival.kt (data class dominio):
   - stopId, tripId, arrivalDelay (segundos), arrivalTime (epoch), scheduleRelationship

6. GtfsRtModule.kt (Hilt):
   - Provee GtfsRtDataSource con el OkHttpClient
   - Provee la URL del feed via @Named qualifier

7. SubteRtRepository.kt:
   - Polling con Flow cada 30 segundos usando flow { while(true) { emit(fetch()); delay(30_000) } }
   - Manejo de errores: si falla el fetch, emitir Result.Error pero NO detener el polling
   - Cancela correctamente al cancelar el scope del ViewModel

REGLAS CRÍTICAS:
- NO usar MyLocationNewOverlay ni ninguna API de GPS
- NO extraer latitud ni longitud del VehiclePosition aunque estén en el proto
- La función mapVehiclePositions hardcodea hasCoordinates = false para Subte

Genera cada archivo con su ruta completa.
```

### Criterios de Aceptación
- [ ] `./gradlew :core:network:generateProto` genera las clases Kotlin de GTFS-RT sin errores.
- [ ] Test unitario de `GtfsRtMapper` con un binario `.pb` de muestra: verifica que `hasCoordinates == false`.
- [ ] Test de `SubteRtRepository`: simula 3 ciclos de polling y verifica que los errores en el ciclo 2 no detienen el flujo.

### Artefactos Producidos
`build.gradle.kts` modificado de `:core:network`, `proto/gtfs-realtime.proto`, `GtfsRtDataSource.kt`, `GtfsRtMapper.kt`, `SubteArrival.kt`, `VehicleLocation.kt`, `GtfsRtModule.kt`, `SubteRtRepository.kt`.

---

## FASE 4 — AGENTE ENGINEER: DOMAIN

### Tarea
Implementar todas las entidades de dominio, repositorios y use cases.

### Modelo
`Gemini 2.5 Flash`

### Prompt Base

```
Eres un experto en Clean Architecture para Android. Implementa la capa de dominio
completa del proyecto TransportAR en los módulos :core:domain y :core:common.

RESTRICCIÓN FUNDAMENTAL:
:core:domain es un módulo Kotlin puro. NO puede importar:
- android.* (ningún paquete de Android framework)
- androidx.* (ningún paquete de AndroidX)
- Room, Retrofit, OkHttp, ni ninguna librería de terceros de Android
Solo puede importar: Kotlin stdlib, kotlinx.coroutines

CONTRATO DE ENTIDADES (del SPECS.MD §5.1):
[Pegar todas las data classes de dominio]

CONTRATO DE REPOSITORIOS (del SPECS.MD §5.2):
[Pegar todas las interfaces de repositorio]

ENTREGABLES REQUERIDOS:

1. Todas las entidades de dominio como data classes Kotlin puras:
   Line, Branch, Station, Coordinates, Departure, JourneyStop,
   ServiceAlert, FavoriteRoute, NetworkType (enum), LineStatus (enum),
   AlertSeverity (enum), StopState (enum), DepartureMode (enum)

2. Todas las interfaces de repositorio:
   LineRepository, DepartureRepository, JourneyRepository, AlertRepository,
   StationRepository, RecentStationRepository, FavoriteRouteRepository

3. Todos los Use Cases (del SPECS.MD §4.2 y §5.3):
   - GetLinesStatusUseCase
   - GetNextDeparturesUseCase (con guardado de historial)
   - GetJourneyStopsUseCase
   - GetAlertsUseCase
   - GetStationsForSelectorUseCase (con lógica historial vs búsqueda)
   - GetBranchesByLineUseCase
   - GetRecentStationsUseCase
   - SaveStationToHistoryUseCase
   - ToggleFavoriteUseCase
   - IsFavoriteUseCase

4. Result<T> sealed class en :core:common:
   - Success<T>, Error, Loading
   - Funciones de extensión: onSuccess, onError, getOrNull, mapSuccess

5. StationSelectorData sealed class (del SPECS.MD §5.3):
   - Recent(stations: List<Station>)
   - Results(stations: List<Station>)

REGLAS:
- Cada Use Case es una clase con operator fun invoke() (patrón functional)
- Los Use Cases son @Inject constructor con sus repositorios
- NO hay lógica de UI en el dominio
- Los Use Cases devuelven Result<T> o Flow<Result<T>>, nunca lanzan excepciones
  al caller (las capturan internamente)

Genera cada archivo con su ruta completa. Los Use Cases van en :core:domain/usecase/.
Las interfaces de repositorio van en :core:domain/repository/.
Las entidades van en :core:domain/model/.
```

### Criterios de Aceptación
- [ ] `./gradlew :core:domain:test` corre en JVM puro (sin Robolectric ni emulador).
- [ ] Ningún archivo en `:core:domain` importa `android.*` o `androidx.*`.
- [ ] `GetStationsForSelectorUseCase` tiene tests para los 3 casos: historial, ramal seleccionado, búsqueda.

### Artefactos Producidos
Todas las entidades de dominio, interfaces de repositorios, use cases, `Result.kt`, `StationSelectorData.kt`.

---

## FASE 5 — AGENTE ENGINEER: VIEWMODELS

### Tarea
Implementar todos los ViewModels con sus UiState y UiEvents.

### Modelo
`Gemini 2.5 Flash`

### Prompt Base

```
Eres un experto en MVVM con Jetpack ViewModel, StateFlow y Hilt para Android.
Implementa todos los ViewModels del proyecto TransportAR.

CONTRATO DE VIEWMODELS Y UISTATE (del SPECS.MD §6):
[Pegar definiciones de HomeUiState, DeparturesUiState, DeparturesUiEvent,
StationSelectorUiState, JourneyUiState y todas las firmas de ViewModel]

ENTREGABLES REQUERIDOS:

1. HomeViewModel:
   - Observa GetLinesStatusUseCase como Flow
   - Emite HomeUiState con isLoading, lines, error
   - No tiene eventos one-shot

2. DeparturesViewModel:
   - Recibe lineId via SavedStateHandle
   - Carga los ramales del line en init
   - onOriginSelected: actualiza estado, dispara búsqueda si ambas estaciones están seleccionadas
   - onDestinationSelected: idem
   - onBranchChipSelected: actualiza filtro activo
   - onSwapStations: intercambia origen y destino y retrigerea la búsqueda
   - onTicketClicked: emite NavigateToJourney event
   - onMapButtonClicked: emite NavigateToMap event (solo si coordinates != null)
   - onAlertsBannerClicked: emite NavigateToAlerts event
   - onToggleFavorite: llama ToggleFavoriteUseCase

3. StationSelectorViewModel:
   - Debounce de 300ms en la búsqueda
   - onQueryChanged, onBranchChipSelected
   - Emite StationSelectorUiState con stationData (historial o resultados)

4. JourneyViewModel:
   - Recibe serviceId via SavedStateHandle
   - Llama GetJourneyStopsUseCase en init
   - Emite JourneyUiState con stops categorizados (PAST, CURRENT, FUTURE)

5. AlertsViewModel:
   - Recibe lineId opcional via SavedStateHandle (para filtrado desde HomeScreen)
   - Observa GetAlertsUseCase como Flow
   - Función de búsqueda local con filterQuery

6. MapViewModel:
   - Recibe Coordinates via SavedStateHandle (lat, lon)
   - Estado simple: coordinates: Coordinates, isLoading: Boolean

REGLAS:
- Todos los ViewModels usan @HiltViewModel
- Todos los errores se capturan en viewModelScope con try/catch o .catch {}
- Los eventos one-shot se implementan con MutableSharedFlow (no Channel)
- No hay lógica de UI (colores, strings de display) en los ViewModels
- viewModelScope.launch usa Dispatchers.Default por defecto

Genera cada archivo con su ruta completa en sus módulos :feature:*.
```

### Criterios de Aceptación
- [ ] Test de `DeparturesViewModel`: onSwapStations() intercambia correctamente origen/destino.
- [ ] Test de `StationSelectorViewModel`: debounce no emite si el query cambia en menos de 300ms.
- [ ] `DeparturesViewModel.onMapButtonClicked()` NO emite evento si `coordinates == null`.

### Artefactos Producidos
`HomeViewModel.kt`, `DeparturesViewModel.kt`, `StationSelectorViewModel.kt`, `JourneyViewModel.kt`, `AlertsViewModel.kt`, `MapViewModel.kt`.

---

## FASE 6 — AGENTE ENGINEER: UI COMPOSE (Parte 1 — Pantallas Base)

### Tarea
Implementar el sistema de diseño Material 3 y las pantallas Home y Alerts.

### Modelo
`Gemini 2.5 Flash`

### Prompt Base (Parte 1)

```
Eres un experto en Jetpack Compose y Material 3 (Material You / Material Expressive).
Implementa el sistema de diseño y las primeras dos pantallas de TransportAR.

REFERENCIA VISUAL: [Adjuntar imágenes de Home y Alerts]

SISTEMA DE DISEÑO:

1. TransportArTheme.kt:
   - colorScheme dinámico con dynamicColorScheme() en Android 12+
   - Fallback con colorScheme de SOFSE: primary = Color(0xFF1A6EA8)
   - Typography con escala completa de Material 3
   - Shapes con cornerRadius = 12.dp para Cards

2. HomeScreen.kt:
   - Saludo dinámico según hora: "¡Buenos días!", "¡Buenas tardes!", "¡Buenas noches!"
   - LazyColumn con LineStatusItem para cada línea
   - El ítem "Subte" va al FINAL de la lista (último elemento)
   - LineStatusItem layout:
     - Inicio: ícono circular de la línea (ImageVector o Painter)
     - Centro: nombre de la línea (clickable → navigateToDepartures)
     - Fin: StatusBadge (clickable → navigateToAlerts con lineId)
   - Divider entre items
   - BottomNavigationBar con tabs: Inicio (Home), Favoritos (Heart), Alertas (Bell)

3. StatusBadge.kt:
   - Normal: flecha verde + texto "Normal"
   - Demorado: ícono amarillo + texto "Demorado"
   - Cancelado: ícono rojo + texto "Cancelado"
   - Sin servicio: ícono gris + texto "Sin servicio"

4. AlertsScreen.kt:
   - TopAppBar con título "Alertas" y SearchIcon
   - LazyColumn con StickyHeader por línea (grouped by lineId)
   - Cada grupo: LineGroupHeader (flechas ">>" + nombre línea, estilo original)
   - Cada alerta: AlertItem con título del ramal + descripción
   - Empty state si no hay alertas
   - FilteredView cuando llega lineId desde deeplink de Home

RESTRICCIONES DE DISEÑO:
- Usar SOLO APIs de Material 3 (androidx.compose.material3)
- Transiciones entre pantallas: fadeIn/fadeOut con duration 300ms
- Soportar tanto tema claro como oscuro (dynamicColorScheme gestiona esto)
- No usar imágenes de red para los íconos de líneas; usar vectores locales

Genera cada archivo con su ruta completa en :feature:home y :feature:alerts.
```

### Artefactos Producidos (Fase 6 Parte 1)
`TransportArTheme.kt`, `Color.kt`, `Typography.kt`, `Shape.kt`, `HomeScreen.kt`, `LineStatusItem.kt`, `StatusBadge.kt`, `AlertsScreen.kt`, `AlertItem.kt`, `LineGroupHeader.kt`.

---

## FASE 6 — AGENTE ENGINEER: UI COMPOSE (Parte 2 — Departures y StationSelector)

### Modelo
`Gemini 2.5 Flash`

### Prompt Base (Parte 2)

```
Eres un experto en Jetpack Compose y Material 3. Implementa las pantallas
de búsqueda de horarios de TransportAR.

REFERENCIA VISUAL: [Adjuntar imágenes de DeparturesScreen vacía y con resultados]

ENTREGABLES:

1. DeparturesScreen.kt:
   - TopAppBar con flecha atrás + nombre de línea centrado + íconos mapa/favorito
   - Indicador horizontal del ramal seleccionado (línea azul como en el original)
   - Dos StationInputField (origen con punto sólido, destino con círculo hueco)
   - SwapStationsButton (icono flecha bidireccional, esquina derecha)
   - DepartureModeRadioGroup (RadioButton "Salir ahora" / "Programar")
   - DateTimePicker (visible solo cuando mode = SCHEDULED)
   - AlertsBanner (si hasActiveAlerts):
     - Fondo rojo suave (error container)
     - Texto "Hay alertas que pueden afectar tu viaje."
     - Botón "+" que navega a AlertsScreen
   - FilterChipsRow (LazyRow horizontal):
     - Para Tren: chips de ramales ("V. Ballester - Zárate", etc.)
     - Para Subte: chips de líneas ("Línea A", "Línea B", etc.)
     - Un chip puede ser deseleccionado para "todos"
   - LazyColumn de DepartureTicketCard (una por servicio próximo)
   - Empty state con ilustración SVG cuando no hay estación seleccionada

2. DepartureTicketCard.kt (del SPECS.MD §7.2):
   - ElevatedCard dividida visualmente
   - Panel izquierdo: minutesAway en displayLarge, "MINUTOS" en labelSmall
   - Separador punteado vertical (Canvas.drawPath con PathEffect.dashPathEffect)
   - Panel derecho: branchName (highlighted), destination (highlighted), StatusChip
   - MapIconButton en esquina inferior derecha (SOLO si showMapButton == true)
   - showMapButton = !isTerminus && coordinates != null && networkType != SUBTE

3. StationInputField.kt:
   - OutlinedTextField de Material 3
   - Leading icon: punto sólido (origen) o círculo hueco (destino)
   - Trailing icon: X para limpiar (si hay texto)
   - Trailing icon: SwapIcon (solo en destino)
   - Al hacer tap: navega a StationSelectorScreen (NO abre teclado directamente)

4. StationSelectorScreen.kt:
   - FullScreen (no bottom sheet) para máxima área de selección
   - SearchTextField con autoFocus y teclado abierto
   - BranchFilterRow (LazyRow de FilterChip):
     - Sin chip = mostrar historial
     - Con chip = filtrar estaciones del ramal/línea
   - StationList (LazyColumn):
     - Si modo historial: header "Recientes" + estaciones con icono reloj
     - Si modo búsqueda/filtro: lista de estaciones con nombre de ramal/línea
   - StationItem clickable → devuelve estación al DeparturesScreen

ANIMACIONES:
- La StationSelectorScreen entra con slideInVertically (desde abajo)
- Los FilterChips animan su selección con animateColorAsState
- Los DepartureTicketCard tienen animatedVisibility al aparecer
```

### Artefactos Producidos (Fase 6 Parte 2)
`DeparturesScreen.kt`, `DepartureTicketCard.kt`, `StationInputField.kt`, `FilterChipsRow.kt`, `AlertsBanner.kt`, `StationSelectorScreen.kt`, `StationItem.kt`.

---

## FASE 6 — AGENTE ENGINEER: UI COMPOSE (Parte 3 — Journey y Map)

### Modelo
`Gemini 2.5 Flash` para Journey; `Gemini 2.5 Pro` para Map (complejidad de AndroidView).

### Prompt Base (Parte 3)

```
Eres un experto en Jetpack Compose y Osmdroid. Implementa las pantallas finales.

REFERENCIA VISUAL: [Adjuntar imagen de JourneyScreen]

1. JourneyScreen.kt:
   - TopAppBar con doble flecha atrás "«" + título "Recorrido del tren/subte"
   - Indicador de ramal en la esquina derecha
   - JourneyHeaderCard (ElevatedCard estilo ticket):
     - Panel izquierdo: hora de salida en displayMedium
     - Panel derecho: Ramal, Servicio (DIRECTO/SEMIDIRECTO), Andén, Estado (EN ANDÉN)
     - Compartir icon en esquina inferior derecha
   - StopTimelineList (LazyColumn):
     - StopTimelineItem con Canvas para la línea vertical conectora
     - PastStopItem: alpha 0.4, sin horario a la derecha, círculo X en el nodo
     - CurrentStopItem: alpha 1.0, ícono especial (tren/subte), horario visible
     - FutureStopItem: alpha 1.0, círculo vacío, horario a la derecha
     - Decoraciones laterales: ImageVectors de árboles/edificios (como en el original)
       usar íconos de Material Icons Extended para simularlos

2. MapScreen.kt:
   - AndroidView { OsmdroidMapView }
   - Configuración:
     - userAgentValue = "TransportAR/1.0"
     - setMultiTouchControls(true)
     - setTileSource(TileSourceFactory.MAPNIK)  ← OpenStreetMap tiles
     - Centrar mapa en las coordenadas del tren
     - Zoom level inicial = 14
   - MarkerOverlay: marcador en la posición de la formación
   - SIN MyLocationNewOverlay (prohibido por restricción de privacidad)
   - SIN solicitud de permisos de ningún tipo
   - BackButton en TopAppBar

RESTRICCIONES CRÍTICAS para MapScreen:
- NO llamar a: Configuration.getInstance().load() con context que solicite GPS
- NO instanciar: MyLocationNewOverlay
- NO agregar: LocationManager al MapView
- El mapa es PURAMENTE de visualización pasiva (solo centra en coords del tren)
```

### Artefactos Producidos (Fase 6 Parte 3)
`JourneyScreen.kt`, `StopTimelineList.kt`, `StopTimelineItem.kt`, `JourneyHeaderCard.kt`, `MapScreen.kt`.

---

## FASE 7 — AGENTE QA ENGINEER

### Tarea
Revisión de código y escritura de tests automatizados.

### Modelo
`Gemini 2.5 Pro` — Necesita razonar sobre edge cases complejos.

### Prompt Base

```
Eres un QA Engineer experto en Android Testing (JUnit 5, MockK, Turbine, MockWebServer).
Tu tarea es revisar el código generado en las fases anteriores y escribir tests exhaustivos.

ARCHIVOS A REVISAR: [Listar todos los archivos generados]

ENTREGABLES:

1. REVISIÓN DE CÓDIGO (reporte en comentarios):
   - ¿Hay loops infinitos posibles en TokenManagerInterceptor?
   - ¿Los Flow en DAOs pueden causar memory leaks?
   - ¿El showMapButton en DepartureTicketCard evalúa correctamente las 3 condiciones?
   - ¿El debounce en StationSelectorViewModel puede causar race conditions?

2. TESTS UNITARIOS (:core:domain/test):
   - GetStationsForSelectorUseCaseTest: casos historial, ramal, búsqueda
   - GetNextDeparturesUseCaseTest: verificar que guarda en historial antes del fetch
   - ToggleFavoriteUseCaseTest: toggle agrega y luego quita

3. TESTS DE INTERCEPTOR (:core:network/test):
   - TokenManagerInterceptorTest con MockWebServer:
     a. Request exitosa: token agregado en header
     b. 401 → refresh → retry exitoso
     c. 401 → refresh → segundo 401: NO reintenta más (evitar loop)

4. TESTS DE DAO (:core:database/androidTest):
   - LineDao: upsert idempotente (insertar dos veces la misma línea → solo 1 registro)
   - StationDao.search: búsqueda con query vacío y branchId → devuelve todas las del ramal
   - RecentStationDao: pruneOld() mantiene solo las 10 más recientes

5. TESTS DE MAPPER:
   - GtfsRtMapperTest: con FeedMessage de muestra (construido programáticamente):
     a. hasCoordinates siempre false
     b. mapTripUpdates agrupa correctamente por stopId
     c. Entities vacías no crashean el mapper

6. TESTS DE VIEWMODEL (:feature:*/test):
   - DeparturesViewModelTest con Turbine:
     a. onSwapStations intercambia origen y destino
     b. onMapButtonClicked NO emite si coordinates == null
   - StationSelectorViewModelTest:
     a. debounce: query rápido no dispara búsqueda hasta 300ms

Escribe tests completos y compilables, no pseudocódigo.
```

### Criterios de Aceptación Final
- [ ] `./gradlew test` en todos los módulos: 0 failures.
- [ ] `./gradlew connectedAndroidTest` en `:core:database`: 0 failures.
- [ ] Reporte de cobertura: >70% en `:core:domain`.
- [ ] Ningún test importa clases de producción que no correspondan a su módulo.

---

## PROTOCOLO DE HANDOFF ENTRE AGENTES

### Checklist de Handoff (Antes de Pasar a la Siguiente Fase)

```
[ ] El código compila sin warnings de tipo ERROR
[ ] Los tests de aceptación de la fase pasan
[ ] Las interfaces del módulo producido están documentadas con KDoc
[ ] No hay TODO() sin resolver en código crítico
[ ] Se actualizó el task.md con los ítems completados
```

### Regla de Rollback

Si el agente de la Fase N produce código que no pasa los criterios de aceptación:
1. NO pasar a la Fase N+1.
2. Re-enviar el mismo prompt con el código fallido y el mensaje de error.
3. Si falla 2 veces: escalar al modelo de mayor capacidad (Flash → Pro).
4. Máximo 3 intentos por fase; si falla el 3ro, revisar el SPECS.MD.

---

## CONVENCIONES DE CÓDIGO PARA TODOS LOS AGENTES

```kotlin
// Nomenclatura de archivos
// - Screens: NombreScreen.kt (ej: HomeScreen.kt)
// - ViewModels: NombreViewModel.kt
// - Entities (Room): NombreEntity.kt
// - DTOs: NombreDto.kt
// - Domain models: Nombre.kt (sin sufijo)
// - DAOs: NombreDao.kt
// - Use Cases: VerbNombreUseCase.kt

// Estructura de paquetes
com.transportar.android/
├── core/
│   ├── database/       → entities/, dao/, AppDatabase, DatabaseSeeder, DatabaseModule
│   ├── network/        → sofse/ (api, dto, datasource, interceptor), gtfsrt/, NetworkModule
│   ├── domain/         → model/, repository/, usecase/
│   └── common/         → Result, extensions/, constants/
└── feature/
    ├── home/           → HomeScreen, HomeViewModel, components/
    ├── departures/     → DeparturesScreen, StationSelectorScreen, viewmodel/, components/
    ├── journey/        → JourneyScreen, JourneyViewModel, components/
    ├── alerts/         → AlertsScreen, AlertsViewModel
    ├── map/            → MapScreen, MapViewModel
    └── favorites/      → FavoritesScreen, FavoritesViewModel
```

---

*Manual de Orquestación v1.0 — TransportAR — 2026-08-28*

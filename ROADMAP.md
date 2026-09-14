# 🚆 MoovAR — ROADMAP DE DESARROLLO

> **Stack:** Kotlin · Jetpack Compose · Room · Retrofit/OkHttp · Protobuf · Osmdroid  
> **Arquitectura:** Clean Architecture + MVVM + StateFlow  
> **Principios:** Zero Telemetría · Zero Permisos GPS · Offline-First  
> **Target:** Android 8.0+ (API 26) → compileSdk 36

---

## 📐 Principio Rector de Ordenamiento de Fases

El orden de construcción de capas sigue un grafo de dependencias estricto:

```
Base del Proyecto → Room (Datos Locales) → Network (APIs) → Domain (Lógica) → ViewModel → UI (Compose)
```

**¿Por qué este orden?** Nunca construir la UI sobre contratos de datos inciertos.
Room define las entidades primero porque son el contrato canónico que alimenta
tanto los Repositorios como los ViewModels. Los Interceptores de red se escriben
después de tener los modelos de dominio definidos, para que el mapeo
`NetworkModel → DomainModel → RoomEntity` sea coherente y sin retrabajo.

---

## 🗂️ FASE 0 — Andamiaje del Proyecto (Semana 1)

> **Objetivo:** Dejar el proyecto en un estado buildeable con todas las dependencias declaradas y la estructura de módulos establecida.

### 0.1 — Configuración Gradle y Versionado

- [ ] Crear proyecto Android con **Android Studio Hedgehog+** (AGP 8.x).
- [ ] Migrar a **Kotlin DSL** (`build.gradle.kts`) y **Version Catalogs** (`libs.versions.toml`).
- [ ] Configurar `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`.
- [ ] Habilitar **buildFeatures { compose = true }** y fijar `kotlinCompilerExtensionVersion`.
- [ ] Configurar **KSP** (Kotlin Symbol Processing) como procesador de anotaciones (reemplaza KAPT).

### 0.2 — Declaración de Dependencias en `libs.versions.toml`

```toml
[versions]
kotlin                 = "2.1.0"
ksp                    = "2.1.0-1.0.29"
compose-bom            = "2025.06.00"       # BOM de Compose para alinear todas las libs
lifecycle              = "2.9.0"
room                   = "2.7.1"
hilt                   = "2.53"
retrofit               = "2.11.0"
okhttp                 = "4.12.0"
protobuf               = "4.29.3"
gtfs-realtime          = "0.0.8"            # com.google.transit:gtfs-realtime-bindings
osmdroid               = "6.1.20"
kotlinx-coroutines     = "1.9.0"
kotlinx-serialization  = "1.7.3"
navigation-compose     = "2.9.0"
coil                   = "3.1.0"

[libraries]
# Compose
compose-bom                = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui                 = { group = "androidx.compose.ui", name = "ui" }
compose-material3          = { group = "androidx.compose.material3", name = "material3" }
compose-material3-adaptive  = { group = "androidx.compose.material3.adaptive", name = "adaptive" }
compose-animation          = { group = "androidx.compose.animation", name = "animation" }
compose-ui-tooling         = { group = "androidx.compose.ui", name = "ui-tooling" }

# Lifecycle / ViewModel
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose   = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Room
room-runtime  = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx      = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-ksp      = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Hilt
hilt-android       = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler      = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation    = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Retrofit / OkHttp
retrofit               = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-core            = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging         = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Protobuf / GTFS
protobuf-kotlin  = { group = "com.google.protobuf", name = "protobuf-kotlin-lite", version.ref = "protobuf" }
gtfs-realtime    = { group = "com.google.transit", name = "gtfs-realtime-bindings", version.ref = "gtfs-realtime" }

# Osmdroid (sin Google Maps)
osmdroid = { group = "org.osmdroid", name = "osmdroid-android", version.ref = "osmdroid" }

# Coil (carga de imágenes ligera, sin Firebase)
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }

# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
```

### 0.3 — Estructura de Módulos (Multi-Module)

```
:app                    ← Módulo principal (Activities, DI wiring, Navigation)
:core:database          ← Room: entidades, DAOs, migrations, TypeConverters
:core:network           ← Retrofit, OkHttp, Interceptores, DTOs
:core:domain            ← Entidades de dominio puras (Kotlin only, sin Android)
:core:common            ← Utils, Extensions, Constants, Result wrapper
:feature:home           ← HomeScreen, HomeViewModel
:feature:departures     ← DeparturesScreen, StationSelectorScreen, TicketCard
:feature:journey        ← JourneyScreen (Stepper vertical)
:feature:alerts         ← AlertsScreen
:feature:map            ← MapScreen (Osmdroid wrapper)
:feature:favorites      ← FavoritesScreen
```

> **Razonamiento:** La modularización mejora los tiempos de build incrementales
> (Gradle sólo recompila módulos afectados) y enforce el acoplamiento correcto.
> `:core:domain` no tiene dependencia de Android framework: sus modelos son
> POKOs Kotlin puras. Esto garantiza que los Unit Tests del dominio corran en JVM,
> sin necesidad de un emulador o Robolectric.

---

## 🗂️ FASE 1 — Capa de Dominio y Base de Datos Room (Semana 2-3)

> **Objetivo:** Materializar todas las entidades de negocio como clases Kotlin y sus correspondientes tablas Room. Al finalizar esta fase, la app puede funcionar 100% offline con datos seed.

### 1.1 — Definición de Entidades de Dominio (`:core:domain`)

- [ ] Modelar jerarquía `NetworkType` (TREN / SUBTE).
- [ ] Definir `Line`, `Branch`, `Station`, `ServiceAlert`, `Departure`, `StopTime`.
- [ ] Crear el `Result<T>` wrapper genérico (`Success`, `Error`, `Loading`).
- [ ] Definir las firmas de los repositorios como interfaces puras.

### 1.2 — Entidades e Índices Room (`:core:database`)

- [ ] Implementar todas las `@Entity` con foreign keys e índices compuestos.
- [ ] Crear `TypeConverters` para enums y listas.
- [ ] Escribir todos los `@Dao` con queries suspend/Flow.
- [ ] Configurar `AppDatabase` con `RoomDatabase.Callback` para inyectar datos seed.
- [ ] Crear **Migration 1→2** como esqueleto (aunque se parte de v1, es best-practice).

### 1.3 — Datos Seed Iniciales

- [ ] Generar JSON seed con todas las líneas de tren y sus ramales conocidos.
- [ ] Generar JSON seed con las 6 líneas de Subte y sus estaciones completas.
- [ ] Implementar `DatabaseSeeder` que se ejecuta en `onCreate` para poblar tablas.

> **Razonamiento:** Al seedear datos estáticos (líneas, ramales, estaciones) en Room,
> la app es funcional desde el primer launch sin conexión a internet. Las pantallas de
> selección de estación responden instantáneamente desde SQLite. La red sólo entra en
> juego para datos dinámicos (horarios RT, alertas). Esta es la esencia de Offline-First.

---

## 🗂️ FASE 2 — Capa de Red: API SOFSE (Semana 4)

> **Objetivo:** Implementar el cliente Retrofit con los Interceptores de autenticación y los DTOs de la API de SOFSE.

### 2.1 — Configuración OkHttp y Interceptores

- [ ] Implementar `StaticHeadersInterceptor` (agrega `x-api-key` y `Referer`).
- [ ] Implementar `TokenManagerInterceptor`:
  - Agrega `Authorization: Bearer {token}` a cada request.
  - Si el server responde `401`, genera hash MD5 de `"V3rS10n$SOFSE"`.
  - Llama a `/v3/auth/token` con el hash en el cuerpo.
  - Guarda el token en `EncryptedSharedPreferences` (Jetpack Security).
  - Reintenta la petición original (máximo 1 reintento para evitar loops).
- [ ] Implementar `NetworkMonitorInterceptor` para lanzar `NoConnectivityException`.
- [ ] En DEBUG: agregar `HttpLoggingInterceptor`; en RELEASE: OMITIR (privacidad).

### 2.2 — Definición de la API SOFSE (Retrofit)

- [ ] Declarar `SofseApiService` con endpoints:
  - `GET /v3/auth/token`
  - `GET /v3/lineas` — Listado de líneas y estado
  - `GET /v3/ramales/{lineaId}` — Ramales de una línea
  - `GET /v3/estaciones/{ramalId}` — Estaciones de un ramal
  - `GET /v3/proximos/{estacionOrigenId}/{estacionDestinoId}` — Próximos trenes
  - `GET /v3/recorrido/{servicioId}` — Recorrido completo del servicio
  - `GET /v3/alertas` — Alertas de servicio
- [ ] Definir todos los DTOs `@Serializable` correspondientes.
- [ ] Implementar `SofseRemoteDataSource` que llama a la API y mapea a modelos de dominio.

### 2.3 — Estrategia de Caché (Room como Single Source of Truth)

- [ ] Implementar `NetworkBoundResource<Local, Remote>` coroutine flow:
  ```
  1. Emitir datos de Room inmediatamente (Loading state con datos cached).
  2. Fetch de red en background.
  3. Si respuesta exitosa: actualizar Room → Room emite actualización automática.
  4. Si falla la red: emitir Error pero mantener datos cacheados.
  ```

> **Razonamiento:** El `TokenManagerInterceptor` opera como un Proxy transparente.
> Ningún ViewModel ni Repositorio conoce la lógica de tokens; solo ven respuestas
> correctas o errores de red. Esto cumple el principio de Separation of Concerns y
> simplifica radicalmente el testing (se puede mockear el Interceptor aisladamente).
> `EncryptedSharedPreferences` en lugar de plain `SharedPreferences` protege el token
> en el storage del dispositivo, aunque sea temporal.

---

## 🗂️ FASE 3 — Capa de Red: GTFS-RT Subte (Semana 5)

> **Objetivo:** Integrar el feed de SBASE/GCBA usando Protobuf, sin una sola línea de parsing JSON manual.

### 3.1 — Configuración del Plugin Protobuf

- [ ] Agregar plugin `com.google.protobuf` al módulo `:core:network`.
- [ ] Descargar el `.proto` de GTFS-RT (`gtfs-realtime.proto`) y colocarlo en `src/main/proto/`.
- [ ] Configurar `protobuf { generateProtoTasks { all().forEach { it.builtins { id("kotlin") {} } } } }`.
- [ ] Verificar que se generan las clases Kotlin `FeedMessage`, `TripUpdate`, `VehiclePosition`.

### 3.2 — Cliente GTFS-RT

- [ ] Implementar `GtfsRtDataSource` con OkHttp puro (no Retrofit, ya que no es JSON):
  - Llama al endpoint GCBA con `Accept: application/x-protobuf`.
  - Lee el `ResponseBody.bytes()` y parsea con `FeedMessage.parseFrom(bytes)`.
- [ ] Implementar mapper `GtfsRtMapper`:
  - `TripUpdate.StopTimeUpdate` → `StopTime` (dominio).
  - `VehiclePosition` → `VehicleLocation` (dominio, **sin lat/long para Subte**).
  - Lógica: si `NetworkType == SUBTE`, `VehicleLocation.hasCoordinates = false`.

### 3.3 — Tiempo Real Subte

- [ ] Implementar polling automático cada **30 segundos** usando `Flow` + `repeatOnLifecycle`.
- [ ] En Subte: el tiempo de arribo se calcula desde `StopTimeUpdate.arrival.delay` (offset en segundos respecto al horario programado).
- [ ] Los nombres de estaciones del GTFS se reconcilian con las entidades Room usando `stop_id` como FK.

> **Razonamiento:** Usar `OkHttp` directamente (sin Retrofit) para el cliente Protobuf
> es intencional. Retrofit espera convertidores y un protocolo request/response con
> Content-Type negociado. Para streams binarios Protobuf, es más limpio y eficiente
> hacer el call raw y parsear manualmente con `FeedMessage.parseFrom()`, que es la
> API estándar de la librería `gtfs-realtime-bindings`. Agregar un
> `Converter.Factory` de Retrofit para Protobuf introduciría complejidad innecesaria.

---

## 🗂️ FASE 4 — Capa de Dominio: Use Cases y Repositorios (Semana 6)

> **Objetivo:** Implementar la lógica de negocio pura, desacoplada de la UI y de las fuentes de datos.

### 4.1 — Repositorios

- [ ] Implementar `LineRepositoryImpl` (offline-first: Room → red si stale).
- [ ] Implementar `DepartureRepositoryImpl` (solo red, no cachear horarios RT).
- [ ] Implementar `JourneyRepositoryImpl` (solo red).
- [ ] Implementar `AlertRepositoryImpl` (cachear en Room con TTL de 15 minutos).
- [ ] Implementar `StationHistoryRepositoryImpl` (solo Room, escribir al seleccionar).
- [ ] Implementar `FavoritesRepositoryImpl` (solo Room).

### 4.2 — Use Cases

- [ ] `GetLinesStatusUseCase` → flujo de líneas con estado en tiempo real.
- [ ] `GetNextDeparturesUseCase(origin, destination, departureTime)`.
- [ ] `GetJourneyStopsUseCase(serviceId)`.
- [ ] `GetAlertsUseCase(lineId?)` → si lineId null, devuelve todas.
- [ ] `SearchStationsUseCase(query, branchId?)` → query Room con LIKE.
- [ ] `GetRecentStationsUseCase()` → últimas 5 estaciones usadas.
- [ ] `SaveStationToHistoryUseCase(station)`.
- [ ] `ToggleFavoriteUseCase(origin, destination)`.

> **Razonamiento:** Cada Use Case tiene una única responsabilidad y una única razón
> para cambiar (SRP). Esto permite que los ViewModels sean muy delgados: solo
> invocan Use Cases y mapean sus `Result<T>` a `UiState`. Los Use Cases
> son 100% testeables con JUnit sin mocks de Android.

---

## 🗂️ FASE 5 — Inyección de Dependencias con Hilt (Semana 6)

### 5.1 — Módulos Hilt

- [ ] `DatabaseModule` → provee `AppDatabase` y cada `@Dao` como `@Singleton`.
- [ ] `NetworkModule` → provee `OkHttpClient`, `Retrofit`, `SofseApiService`, `GtfsRtDataSource`.
- [ ] `RepositoryModule` → bindea interfaces a implementaciones.
- [ ] `DispatcherModule` → provee `@IoDispatcher`, `@MainDispatcher`, `@DefaultDispatcher`.

---

## 🗂️ FASE 6 — ViewModels y UI State (Semana 7)

### 6.1 — ViewModels

- [ ] `HomeViewModel` con `StateFlow<HomeUiState>`.
- [ ] `DeparturesViewModel` con `StateFlow<DeparturesUiState>` y `SharedFlow<UiEvent>`.
- [ ] `JourneyViewModel` con `StateFlow<JourneyUiState>`.
- [ ] `AlertsViewModel` con `StateFlow<AlertsUiState>` y filtro por línea.
- [ ] `StationSelectorViewModel` con debounce de búsqueda (300ms).

---

## 🗂️ FASE 7 — UI en Jetpack Compose + Material 3 (Semanas 8-10)

> **Objetivo:** Construir todas las pantallas guiándonos fielmente por las capturas de la app original, modernizadas con Material You / Material Expressive.

### 7.1 — Sistema de Diseño (Design System)

- [ ] Definir `TransportArTheme` con `colorScheme` dinámico (Material You: `dynamicColorScheme()`).
- [ ] Definir paleta fallback con los colores institucionales de SOFSE (azul `#1A6EA8`).
- [ ] Crear `Typography` con las escalas de texto de Material 3.
- [ ] Definir `Shape` con esquinas redondeadas medias para cards y chips.

### 7.2 — Navegación

- [ ] Implementar `TransportArNavGraph` con `NavHostController`.
- [ ] Bottom Navigation Bar con 3 tabs: Inicio / Favoritos / Alertas.
- [ ] Implementar transiciones animadas entre pantallas (`EnterTransition` / `ExitTransition`) con `spring()` y `tween()` para 120Hz.

### 7.3 — Pantallas por Orden de Construcción

1. **HomeScreen** — Lista de líneas con estado, BottomNavBar.
2. **AlertsScreen** — Lista agrupada por línea, búsqueda.
3. **DeparturesScreen** — Campos de estación + filtros chip + TicketCard.
4. **StationSelectorScreen** — LazyRow chips + LazyColumn estaciones + historial.
5. **JourneyScreen** — Stepper vertical con timeline personalizada.
6. **MapScreen** — AndroidView envolviendo Osmdroid.
7. **FavoritesScreen** — Rutas favoritas guardadas.

---

## 🗂️ FASE 8 — Testing (Semana 11)

### 8.1 — Unit Tests

- [ ] Testear todos los Use Cases con JUnit 5 + MockK.
- [ ] Testear `TokenManagerInterceptor` con `MockWebServer` (OkHttp).
- [ ] Testear `GtfsRtMapper` con archivos `.pb` de muestra.
- [ ] Testear `NetworkBoundResource` con coroutines de test (`runTest`).

### 8.2 — Integration Tests

- [ ] Testear DAOs de Room con `in-memory database`.
- [ ] Testear `DatabaseSeeder` verifica que las líneas seed se cargaron.

### 8.3 — UI Tests

- [ ] Testear `HomeScreen` con datos mockeados: renders `LineItem` correctamente.
- [ ] Testear `StationSelectorScreen`: tap en chip filtra correctamente la lista.
- [ ] Testear `TicketCard`: condición de visibilidad del botón mapa.

---

## 🗂️ FASE 9 — Optimización, Hardening y Publicación (Semana 12)

### 9.1 — Performance

- [ ] Habilitar `baseline profiles` con Macrobenchmark para acelerar startup.
- [ ] Verificar que todos los `LazyList` usan `key { }` para evitar recomposiciones.
- [ ] Perfilar con Android Studio Profiler: CPU, memoria, frames a 120Hz.

### 9.2 — Seguridad y Privacidad

- [ ] Auditar `AndroidManifest.xml`: verificar ausencia de `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET` solo (sin `READ_PHONE_STATE`, etc.).
- [ ] Verificar que `release` build type tiene `minifyEnabled = true` + `R8`.
- [ ] Verificar que `HttpLoggingInterceptor` no existe en el grafo de Hilt en release.
- [ ] Confirmar ausencia total de SDKs de Firebase, Crashlytics, Analytics.

### 9.3 — Publicación

- [ ] Configurar signing config con keystore.
- [ ] Generar `.aab` (Android App Bundle) para Play Store.
- [ ] Completar ficha de Play Store: descripción bilingüe (es/en), capturas de pantalla, política de privacidad.
- [ ] Política de privacidad: declarar explícitamente que la app no recolecta datos personales, no usa GPS, no tiene analytics.

---

## 📊 Cronograma Visual

| Semana | Fase | Hito Verificable |
|--------|------|-----------------|
| 1 | FASE 0 | Proyecto buildea sin errores; estructura de módulos presente |
| 2-3 | FASE 1 | Room Unit Tests pasan; datos seed consultables desde un test |
| 4 | FASE 2 | `MockWebServer` reproduce auth flow SOFSE correctamente |
| 5 | FASE 3 | Parser Protobuf decodifica feed GTFS-RT de muestra |
| 6 | FASE 4-5 | Use Cases devuelven `Result.Success` con datos mockeados |
| 7 | FASE 6 | ViewModels emiten `UiState` correcto en tests de coroutines |
| 8-10 | FASE 7 | App corre en emulador Pixel 8 Pro a 120Hz, todas las pantallas navegan |
| 11 | FASE 8 | Cobertura de tests >70% en `core:domain` y `core:database` |
| 12 | FASE 9 | `.aab` firmado sube a Play Store Internal Testing |

---

*Última actualización: 2026-08-28*

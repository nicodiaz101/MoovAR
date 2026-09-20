package com.moovar.android.core.network.sofse.api

import com.moovar.android.core.network.sofse.dto.AlertDto
import com.moovar.android.core.network.sofse.dto.ArribosResponseDto
import com.moovar.android.core.network.sofse.dto.BranchDto
import com.moovar.android.core.network.sofse.dto.DepartureDto
import com.moovar.android.core.network.sofse.dto.EstacionDto
import com.moovar.android.core.network.sofse.dto.GerenciaDto
import com.moovar.android.core.network.sofse.dto.JourneyDto
import com.moovar.android.core.network.sofse.dto.LineStatusDto
import com.moovar.android.core.network.sofse.dto.RamalDto
import com.moovar.android.core.network.sofse.dto.StationDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SofseApiService {

    // ==========================================
    // Real Official SOFSE v1 API Endpoints
    // ==========================================

    @GET("infraestructura/gerencias")
    suspend fun getGerencias(): List<GerenciaDto>

    @GET("infraestructura/ramales")
    suspend fun getRamales(
        @Query("idGerencia") idGerencia: Int? = null
    ): List<RamalDto>

    @GET("infraestructura/estaciones")
    suspend fun getEstaciones(
        @Query("nombre") nombre: String? = null,
        @Query("idRamal") idRamal: Int? = null
    ): List<EstacionDto>

    @GET("arribos/estacion/{id}")
    suspend fun getArribos(
        @Path("id") stationId: String
    ): ArribosResponseDto

    // ==========================================
    // Legacy Endpoints (fallback compatibility)
    // ==========================================

    @GET("lineas")
    suspend fun getLineas(): List<LineStatusDto>

    @GET("proximos/{origen}/{destino}")
    suspend fun getProximos(
        @Path("origen") originId: String,
        @Path("destino") destinationId: String
    ): List<DepartureDto>

    @GET("recorrido/{servicioId}")
    suspend fun getRecorrido(
        @Path("servicioId") serviceId: String
    ): JourneyDto

    @GET("ramales/{lineaId}")
    suspend fun getRamalesLegacy(
        @Path("lineaId") lineId: String
    ): List<BranchDto>

    @GET("estaciones/{ramalId}")
    suspend fun getEstacionesLegacy(
        @Path("ramalId") branchId: String
    ): List<StationDto>

    @GET("alertas")
    suspend fun getAlertas(
        @Query("lineaId") lineId: String? = null
    ): List<AlertDto>
}

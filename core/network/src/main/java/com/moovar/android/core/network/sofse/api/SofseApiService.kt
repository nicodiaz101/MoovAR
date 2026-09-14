package com.moovar.android.core.network.sofse.api

import com.moovar.android.core.network.sofse.dto.AlertDto
import com.moovar.android.core.network.sofse.dto.BranchDto
import com.moovar.android.core.network.sofse.dto.DepartureDto
import com.moovar.android.core.network.sofse.dto.JourneyDto
import com.moovar.android.core.network.sofse.dto.LineStatusDto
import com.moovar.android.core.network.sofse.dto.StationDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SofseApiService {
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
    suspend fun getRamales(
        @Path("lineaId") lineId: String
    ): List<BranchDto>

    @GET("estaciones/{ramalId}")
    suspend fun getEstaciones(
        @Path("ramalId") branchId: String
    ): List<StationDto>

    @GET("alertas")
    suspend fun getAlertas(
        @Query("lineaId") lineId: String? = null
    ): List<AlertDto>
}

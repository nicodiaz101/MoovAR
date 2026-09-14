package com.moovar.android.core.network.sofse.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LineStatusDto(
    @SerialName("id")          val id: String,
    @SerialName("nombre")      val name: String,
    @SerialName("estado")      val status: String,
    @SerialName("mensaje")     val message: String?,
    @SerialName("icono")       val iconUrl: String?,
    @SerialName("color")       val colorHex: String?
)

@Serializable
data class DepartureDto(
    @SerialName("id")              val serviceId: String,
    @SerialName("ramal")           val branchName: String,
    @SerialName("destino")         val destination: String,
    @SerialName("horario")         val scheduledTime: String,
    @SerialName("tiempo")          val minutesAway: Int,
    @SerialName("estado")          val status: String,
    @SerialName("anden")           val platform: String?,
    @SerialName("tipo_servicio")   val serviceType: String?,
    @SerialName("lat")             val latitude: Double? = null,
    @SerialName("lon")             val longitude: Double? = null
)

@Serializable
data class JourneyDto(
    @SerialName("ramal")    val branchName: String,
    @SerialName("servicio") val serviceType: String,
    @SerialName("paradas")  val stops: List<StopDto>
)

@Serializable
data class StopDto(
    @SerialName("nombre")        val name: String,
    @SerialName("horario")       val scheduledTime: String?,
    @SerialName("estado")        val state: String,
    @SerialName("es_cabecera")   val isTerminus: Boolean
)

@Serializable
data class BranchDto(
    @SerialName("id")            val id: String,
    @SerialName("nombre")        val name: String
)

@Serializable
data class StationDto(
    @SerialName("id")            val id: String,
    @SerialName("nombre")        val name: String,
    @SerialName("lat")           val latitude: Double? = null,
    @SerialName("lon")           val longitude: Double? = null
)

@Serializable
data class AlertDto(
    @SerialName("id")            val id: String,
    @SerialName("titulo")        val title: String,
    @SerialName("descripcion")   val description: String,
    @SerialName("severidad")     val severity: String,
    @SerialName("fecha")         val publishedAt: Long,
    @SerialName("vence")         val expiresAt: Long? = null
)

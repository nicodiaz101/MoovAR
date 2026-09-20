package com.moovar.android.core.network.sofse.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// REAL SOFSE v1 DTOs
// ==========================================

@Serializable
data class ArribosResponseDto(
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("results") val results: List<ArriboResultDto> = emptyList(),
    @SerialName("total") val total: Int? = null
)

@Serializable
data class ArriboResultDto(
    @SerialName("arribo") val arribo: ArriboDetalleDto? = null,
    @SerialName("servicio") val servicio: ServicioDetalleDto? = null
)

@Serializable
data class ArriboDetalleDto(
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("segundos") val segundos: Int? = null,
    @SerialName("orden") val orden: Int? = null,
    @SerialName("idElemento") val idElemento: Int? = null,
    @SerialName("anden") val anden: AndenDto? = null,
    @SerialName("equipo") val equipo: EquipoDto? = null,
    @SerialName("llegada") val llegada: HorarioDetalleDto? = null,
    @SerialName("salida") val salida: HorarioDetalleDto? = null
)

@Serializable
data class HorarioDetalleDto(
    @SerialName("programada") val programada: String? = null,
    @SerialName("estimada") val estimada: String? = null
)

@Serializable
data class AndenDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("nombre") val nombre: String? = null
)

@Serializable
data class EquipoDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("esElectrico") val esElectrico: Int? = null
)

@Serializable
data class CancelacionDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("motivo") val motivo: String? = null
)

@Serializable
data class ServicioDetalleDto(
    @SerialName("id") val id: String? = null,
    @SerialName("numero") val numero: Int? = null,
    @SerialName("sentido") val sentido: Int? = null,
    @SerialName("ramal") val ramal: RamalInfo? = null,
    @SerialName("gerencia") val gerencia: GerenciaInfo? = null,
    @SerialName("cancelacion") val cancelacion: CancelacionDto? = null,
    @SerialName("equipo") val equipo: EquipoDto? = null,
    @SerialName("desde") val desde: PuntoEstacionDto? = null,
    @SerialName("hasta") val hasta: PuntoEstacionDto? = null,
    @SerialName("estaciones") val estaciones: List<EstacionParadaDto> = emptyList()
)

@Serializable
data class RamalInfo(
    @SerialName("id") val id: Int? = null,
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("cabeceraFinal") val cabeceraFinal: CabeceraDto? = null,
    @SerialName("cabeceraInicial") val cabeceraInicial: CabeceraDto? = null
)

@Serializable
data class CabeceraDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("nombreCorto") val nombreCorto: String? = null
)

@Serializable
data class GerenciaInfo(
    @SerialName("id") val id: Int? = null,
    @SerialName("nombre") val nombre: String? = null
)

@Serializable
data class PuntoEstacionDto(
    @SerialName("estacion") val estacion: EstacionMinDto? = null,
    @SerialName("estado") val estado: EstadoMinDto? = null
)

@Serializable
data class EstacionMinDto(
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("idElemento") val idElemento: Int? = null
)

@Serializable
data class EstadoMinDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("nombre") val nombre: String? = null
)

@Serializable
data class EstacionParadaDto(
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("idElemento") val idElemento: Int? = null,
    @SerialName("orden") val orden: Int? = null,
    @SerialName("segundos") val segundos: Int? = null,
    @SerialName("llegada") val llegada: HorarioDetalleDto? = null,
    @SerialName("salida") val salida: HorarioDetalleDto? = null
)

@Serializable
data class GerenciaDto(
    @SerialName("id") val id: Int,
    @SerialName("id_empresa") val idEmpresa: Int? = null,
    @SerialName("nombre") val nombre: String,
    @SerialName("estado") val estado: GerenciaEstadoDto? = null,
    @SerialName("alerta") val alerta: List<SofseAlertaDto> = emptyList()
)

@Serializable
data class GerenciaEstadoDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("mensaje") val mensaje: String? = null,
    @SerialName("color") val color: String? = null
)

@Serializable
data class SofseAlertaDto(
    @SerialName("id") val id: Long,
    @SerialName("linea_id") val lineaId: Int? = null,
    @SerialName("ramal_id") val ramalId: Int? = null,
    @SerialName("estacion_id") val estacionId: Int? = null,
    @SerialName("sentido") val sentido: String? = null,
    @SerialName("contenido") val contenido: String,
    @SerialName("habilitado") val habilitado: Int? = null,
    @SerialName("vigencia_desde") val vigenciaDesde: String? = null,
    @SerialName("criticidad_orden") val criticidadOrden: Int? = null,
    @SerialName("criticidad_color_fondo") val criticidadColorFondo: String? = null,
    @SerialName("criticidad_color_texto") val criticidadColorTexto: String? = null
)

@Serializable
data class RamalDto(
    @SerialName("id") val id: Int,
    @SerialName("id_gerencia") val idGerencia: Int? = null,
    @SerialName("nombre") val nombre: String,
    @SerialName("estaciones") val estaciones: Int? = null,
    @SerialName("operativo") val operativo: Int? = null,
    @SerialName("es_electrico") val esElectrico: Int? = null,
    @SerialName("cabecera_inicial") val cabeceraInicial: CabeceraMinDto? = null,
    @SerialName("cabecera_final") val cabeceraFinal: CabeceraMinDto? = null,
    @SerialName("alerta") val alerta: List<SofseAlertaDto>? = null
)

@Serializable
data class CabeceraMinDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("nombre_corto") val nombreCorto: String? = null
)

@Serializable
data class EstacionDto(
    @SerialName("id_estacion") val idEstacion: String,
    @SerialName("nombre") val nombre: String,
    @SerialName("latitud") val latitud: String? = null,
    @SerialName("longitud") val longitud: String? = null
)

// ==========================================
// LEGACY COMPATIBILITY DTOs
// ==========================================

@Serializable
data class LineStatusDto(
    @SerialName("id")          val id: String,
    @SerialName("nombre")      val name: String,
    @SerialName("estado")      val status: String,
    @SerialName("mensaje")     val message: String? = null,
    @SerialName("icono")       val iconUrl: String? = null,
    @SerialName("color")       val colorHex: String? = null
)

@Serializable
data class DepartureDto(
    @SerialName("id")              val serviceId: String,
    @SerialName("ramal")           val branchName: String,
    @SerialName("destino")         val destination: String,
    @SerialName("horario")         val scheduledTime: String,
    @SerialName("tiempo")          val minutesAway: Int,
    @SerialName("estado")          val status: String,
    @SerialName("anden")           val platform: String? = null,
    @SerialName("tipo_servicio")   val serviceType: String? = null,
    @SerialName("lat")             val latitude: Double? = null,
    @SerialName("lon")             val longitude: Double? = null
)

@Serializable
data class JourneyDto(
    @SerialName("ramal")    val branchName: String,
    @SerialName("servicio") val serviceType: String,
    @SerialName("paradas")  val stops: List<StopDto> = emptyList()
)

@Serializable
data class StopDto(
    @SerialName("nombre")        val name: String,
    @SerialName("horario")       val scheduledTime: String? = null,
    @SerialName("estado")        val state: String,
    @SerialName("es_cabecera")   val isTerminus: Boolean = false
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

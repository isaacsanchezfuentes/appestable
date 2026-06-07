package com.example.appestable.network

import com.example.appestable.network.models.MeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @GET("me")
    suspend fun getMe(@Header("Authorization") token: String): Response<MeResponse>

    @GET("viajes")
    suspend fun getViajes(@Header("Authorization") token: String): Response<List<ViajeResponse>>

    @POST("viajes")
    suspend fun crearViaje(
        @Header("Authorization") token: String,
        @Body viaje: ViajeRequest
    ): Response<SimpleResponse>

    @GET("viajes/{viajeId}/personas")
    suspend fun getPersonas(
        @Header("Authorization") token: String,
        @Path("viajeId") viajeId: Int
    ): Response<List<PersonaResponse>>

    @POST("viajes/{viajeId}/personas")
    suspend fun crearPersona(
        @Header("Authorization") token: String,
        @Path("viajeId") viajeId: Int,
        @Body persona: PersonaRequest
    ): Response<SimpleResponse>

    @DELETE("viajes/{viajeId}/personas/{personaId}")
    suspend fun eliminarPersona(
        @Header("Authorization") token: String,
        @Path("viajeId") viajeId: Int,
        @Path("personaId") personaId: Int
    ): Response<SimpleResponse>

    @GET("viajes/{viajeId}/actividades")
    suspend fun getActividades(
        @Header("Authorization") token: String,
        @Path("viajeId") viajeId: Int
    ): Response<List<ActividadResponse>>

    @POST("viajes/{viajeId}/actividades")
    suspend fun crearActividad(
        @Header("Authorization") token: String,
        @Path("viajeId") viajeId: Int,
        @Body actividad: ActividadRequest
    ): Response<SimpleResponse>

    @GET("viajes/{viajeId}/participaciones")
    suspend fun getParticipaciones(
        @Header("Authorization") token: String,
        @Path("viajeId") viajeId: Int
    ): Response<List<ParticipacionResponse>>

    @PUT("viajes/{viajeId}/participaciones/{participacionId}")
    suspend fun actualizarParticipacion(
        @Header("Authorization") token: String,
        @Path("viajeId") viajeId: Int,
        @Path("participacionId") participacionId: Int,
        @Body update: ParticipacionUpdateRequest
    ): Response<SimpleResponse>
}

data class ViajeResponse(
    val id: Int,
    val nombre: String,
    val descripcion: String = "",
    val fecha_inicio: String = "",
    val fecha_fin: String = "",
    val estado: String = "ACTIVO",
    val organizador_usuario_id: Int? = null
)

data class ViajeRequest(
    val nombre: String,
    val descripcion: String = "",
    val fecha_inicio: String = "",
    val fecha_fin: String = ""
)

data class PersonaResponse(
    val id: Int? = null,
    val nombre: String,
    val email: String? = null,
    val celular: String? = null,
    val es_jefe: Boolean = false,
    val familia_id: Int? = null,
    val familia_nombre: String? = null,
    val viaje_id: Int? = null,
    val rol: String? = null
)

data class PersonaRequest(
    val nombre: String,
    val familia_nombre: String,
    val email: String? = null,
    val celular: String? = null,
    val es_jefe: Boolean = false
)

data class ActividadResponse(
    val id: Int,
    val nombre: String,
    val fecha: String,
    val costo_total: Double,
    val viaje_id: Int? = null
)

data class ActividadRequest(
    val nombre: String,
    val costo_total: Double,
    val fecha: String,
    val participantes_ids: List<Int>
)

data class ParticipacionResponse(
    val id: Int,
    val persona_id: Int,
    val actividad_id: Int,
    val costo_individual: Double,
    val pagado: Boolean = false
)

data class ParticipacionUpdateRequest(
    val costo_individual: Double? = null,
    val pagado: Boolean? = null
)

data class SimpleResponse(
    val status: String,
    val id: Int? = null,
    val message: String? = null
)
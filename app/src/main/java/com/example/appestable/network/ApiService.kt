package com.example.appestable.network

import com.example.appestable.network.models.MeResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<MeResponse>

    @POST("persona")
    suspend fun registrarPersona(
        @Header("Authorization") token: String,
        @Body persona: PersonaRequest
    ): Response<SimpleResponse>

    @POST("actividad")
    suspend fun registrarActividad(
        @Header("Authorization") token: String,
        @Body actividad: ActividadRequest
    ): Response<SimpleResponse>
    
    @POST("familia")
    suspend fun crearFamilia(
        @Header("Authorization") token: String,
        @Body familia: FamiliaRequest
    ): Response<SimpleResponse>
}

data class PersonaRequest(
    val nombre: String,
    val familia_nombre: String,
    val email: String? = null,
    val celular: String? = null,
    val es_jefe: Boolean = false
)

data class FamiliaRequest(
    val nombre_familia: String
)

data class ActividadRequest(
    val nombre: String,
    val costo_total: Double,
    val fecha: String,
    val participantes_ids: List<Int> // IDs de las personas en la BD del backend
)

data class SimpleResponse(
    val status: String,
    val id: Int? = null,
    val message: String? = null
)

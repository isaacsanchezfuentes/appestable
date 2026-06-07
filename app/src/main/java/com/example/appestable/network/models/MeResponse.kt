package com.example.appestable.network.models

data class MeResponse(
    val usuario: UsuarioResponse,
    val membresias: List<MembresiaResponse> = emptyList()
)

data class UsuarioResponse(
    val id: Int,
    val email: String,
    val nombre: String,
    val auth0_id: String
)

data class MembresiaResponse(
    val viaje_id: Int,
    val viaje_nombre: String,
    val rol: String,
    val familia_id: Int? = null,
    val familia_nombre: String? = null
)
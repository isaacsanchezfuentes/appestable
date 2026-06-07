package com.example.appestable.domain

import com.example.appestable.data.RolViaje

data class SessionContext(
    val viajeId: Int,
    val viajeNombre: String,
    val rol: RolViaje,
    val familiaId: Int? = null,
    val usuarioId: Int? = null,
    val isLoggedIn: Boolean = false
) {
    companion object {
        val GUEST_ORGANIZER = SessionContext(
            viajeId = 1,
            viajeNombre = "Viaje Principal",
            rol = RolViaje.ORGANIZADOR,
            isLoggedIn = false
        )
    }
}
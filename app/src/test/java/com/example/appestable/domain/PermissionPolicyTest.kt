package com.example.appestable.domain

import com.example.appestable.data.Persona
import com.example.appestable.data.RolViaje
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPolicyTest {

    private fun session(
        rol: RolViaje,
        familiaId: Int? = null,
    ) = SessionContext(
        viajeId = 1,
        viajeNombre = "Test",
        rol = rol,
        familiaId = familiaId,
        isLoggedIn = true,
    )

    private fun persona(familiaId: Int, rol: RolViaje = RolViaje.MIEMBRO) = Persona(
        id = 1,
        nombre = "Ana",
        celular = "",
        email = "ana@test.com",
        familiaId = familiaId,
        esJefe = rol == RolViaje.JEFE_FAMILIA,
        rol = rol,
    )

    @Test
    fun organizador_ve_todas_las_familias() {
        val s = session(RolViaje.ORGANIZADOR)
        assertTrue(PermissionPolicy.canViewFamilia(s, 1))
        assertTrue(PermissionPolicy.canViewFamilia(s, 99))
    }

    @Test
    fun jefe_solo_ve_su_familia() {
        val s = session(RolViaje.JEFE_FAMILIA, familiaId = 2)
        assertTrue(PermissionPolicy.canViewFamilia(s, 2))
        assertFalse(PermissionPolicy.canViewFamilia(s, 3))
    }

    @Test
    fun miembro_no_agrega_personas() {
        val s = session(RolViaje.MIEMBRO, familiaId = 1)
        assertFalse(PermissionPolicy.canAddPersona(s, 1))
    }

    @Test
    fun jefe_elimina_solo_miembros_de_su_familia() {
        val s = session(RolViaje.JEFE_FAMILIA, familiaId = 2)
        assertTrue(PermissionPolicy.canDeletePersona(s, persona(2)))
        assertFalse(PermissionPolicy.canDeletePersona(s, persona(3)))
        assertFalse(PermissionPolicy.canDeletePersona(s, persona(2, RolViaje.JEFE_FAMILIA)))
    }

    @Test
    fun miembro_no_crea_actividades() {
        val s = session(RolViaje.MIEMBRO, familiaId = 1)
        assertFalse(PermissionPolicy.canCreateActividad(s))
    }

    @Test
    fun organizador_ve_resumen_global() {
        assertTrue(PermissionPolicy.canViewResumenGlobal(session(RolViaje.ORGANIZADOR)))
        assertFalse(PermissionPolicy.canViewResumenGlobal(session(RolViaje.MIEMBRO, 1)))
    }
}
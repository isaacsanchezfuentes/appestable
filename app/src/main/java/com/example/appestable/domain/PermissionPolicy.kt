package com.example.appestable.domain

import com.example.appestable.data.Persona
import com.example.appestable.data.RolViaje

object PermissionPolicy {

    fun canViewFamilia(session: SessionContext, familiaId: Int): Boolean = when (session.rol) {
        RolViaje.ORGANIZADOR -> true
        RolViaje.JEFE_FAMILIA, RolViaje.MIEMBRO -> session.familiaId == familiaId
    }

    fun canCreateViaje(session: SessionContext): Boolean =
        session.rol == RolViaje.ORGANIZADOR

    fun canAddPersona(session: SessionContext, familiaId: Int): Boolean = when (session.rol) {
        RolViaje.ORGANIZADOR -> true
        RolViaje.JEFE_FAMILIA -> session.familiaId == familiaId
        RolViaje.MIEMBRO -> false
    }

    fun canDeletePersona(session: SessionContext, persona: Persona): Boolean = when (session.rol) {
        RolViaje.ORGANIZADOR -> persona.rol != RolViaje.ORGANIZADOR
        RolViaje.JEFE_FAMILIA -> session.familiaId == persona.familiaId && persona.rol == RolViaje.MIEMBRO
        RolViaje.MIEMBRO -> false
    }

    fun canCreateActividad(session: SessionContext): Boolean = when (session.rol) {
        RolViaje.ORGANIZADOR, RolViaje.JEFE_FAMILIA -> true
        RolViaje.MIEMBRO -> false
    }

    fun canSelectParticipante(session: SessionContext, persona: Persona): Boolean = when (session.rol) {
        RolViaje.ORGANIZADOR -> true
        RolViaje.JEFE_FAMILIA -> session.familiaId == persona.familiaId
        RolViaje.MIEMBRO -> false
    }

    fun canEditParticipacion(session: SessionContext, familiaId: Int): Boolean = when (session.rol) {
        RolViaje.ORGANIZADOR -> true
        RolViaje.JEFE_FAMILIA -> session.familiaId == familiaId
        RolViaje.MIEMBRO -> false
    }

    fun canViewResumenGlobal(session: SessionContext): Boolean =
        session.rol == RolViaje.ORGANIZADOR
}
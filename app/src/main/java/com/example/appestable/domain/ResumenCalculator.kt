package com.example.appestable.domain

import com.example.appestable.data.Actividad
import com.example.appestable.data.Familia
import com.example.appestable.data.Participacion
import com.example.appestable.data.Persona

object ResumenCalculator {

    fun montoEfectivo(participacion: Participacion, actividad: Actividad, participaciones: List<Participacion>): Double {
        if (participacion.montoAsignado > 0.0) return participacion.montoAsignado
        val totalParticipantes = participaciones
            .count { it.actividadId == actividad.id }
            .coerceAtLeast(1)
        return actividad.costoTotal / totalParticipantes
    }

    fun calcularFamilia(
        familia: Familia,
        personas: List<Persona>,
        actividades: List<Actividad>,
        participaciones: List<Participacion>
    ): ResumenFamiliaDetalle {
        val miembros = personas.filter { it.familiaId == familia.id }
        val participacionesFamilia = participaciones.filter { part ->
            miembros.any { it.id == part.personaId }
        }

        val lineas = participacionesFamilia.mapNotNull { part ->
            val persona = miembros.find { it.id == part.personaId } ?: return@mapNotNull null
            val actividad = actividades.find { it.id == part.actividadId } ?: return@mapNotNull null
            val monto = montoEfectivo(part, actividad, participaciones)
            LineaParticipacionResumen(
                personaId = persona.id,
                personaNombre = persona.nombre,
                esJefe = persona.esJefe,
                actividadId = actividad.id,
                actividadNombre = actividad.nombre,
                actividadFecha = actividad.fecha,
                monto = monto,
                pagado = part.pagado
            )
        }.sortedWith(
            compareBy<LineaParticipacionResumen> { it.actividadFecha }
                .thenBy { it.actividadNombre.lowercase() }
                .thenBy { it.personaNombre.lowercase() }
        )

        val totalAsignado = lineas.sumOf { it.monto }
        val totalPagado = lineas.filter { it.pagado }.sumOf { it.monto }
        val actividadesIds = lineas.map { it.actividadId }.toSet()

        return ResumenFamiliaDetalle(
            familiaId = familia.id,
            nombreFamilia = familia.nombreFamilia,
            integrantes = miembros.size,
            totalAsignado = totalAsignado,
            totalPagado = totalPagado,
            pendiente = totalAsignado - totalPagado,
            actividadesCount = actividadesIds.size,
            lineas = lineas
        )
    }

    fun calcularGlobal(
        familias: List<Familia>,
        personas: List<Persona>,
        actividades: List<Actividad>,
        participaciones: List<Participacion>
    ): ResumenViajeGlobal {
        val resumenesFamilia = familias.map { familia ->
            calcularFamilia(familia, personas, actividades, participaciones)
        }

        val costoTotalViaje = actividades.sumOf { it.costoTotal }
        val totalPagado = resumenesFamilia.sumOf { it.totalPagado }
        val totalPendiente = resumenesFamilia.sumOf { it.pendiente }

        val ranking = resumenesFamilia
            .sortedByDescending { it.totalAsignado }
            .map { it.nombreFamilia to it.totalAsignado }

        val actividadesConFaltante = actividades.mapNotNull { actividad ->
            val asignado = participaciones
                .filter { it.actividadId == actividad.id }
                .sumOf { montoEfectivo(it, actividad, participaciones) }
            val faltante = actividad.costoTotal - asignado
            if (kotlin.math.abs(faltante) > 0.01) {
                ActividadFaltanteResumen(
                    actividadId = actividad.id,
                    nombre = actividad.nombre,
                    costoTotal = actividad.costoTotal,
                    asignado = asignado,
                    faltante = faltante
                )
            } else null
        }

        return ResumenViajeGlobal(
            costoTotalViaje = costoTotalViaje,
            totalPagado = totalPagado,
            totalPendiente = totalPendiente,
            familiasRanking = ranking,
            actividadesConFaltante = actividadesConFaltante
        )
    }
}
package com.example.appestable.domain

data class LineaParticipacionResumen(
    val personaId: Int,
    val personaNombre: String,
    val esJefe: Boolean,
    val actividadId: Int,
    val actividadNombre: String,
    val actividadFecha: String,
    val monto: Double,
    val pagado: Boolean
)

data class ResumenFamiliaDetalle(
    val familiaId: Int,
    val nombreFamilia: String,
    val integrantes: Int,
    val totalAsignado: Double,
    val totalPagado: Double,
    val pendiente: Double,
    val actividadesCount: Int,
    val lineas: List<LineaParticipacionResumen>
)

data class ActividadFaltanteResumen(
    val actividadId: Int,
    val nombre: String,
    val costoTotal: Double,
    val asignado: Double,
    val faltante: Double
)

data class ResumenViajeGlobal(
    val costoTotalViaje: Double,
    val totalPagado: Double,
    val totalPendiente: Double,
    val familiasRanking: List<Pair<String, Double>>,
    val actividadesConFaltante: List<ActividadFaltanteResumen>
)
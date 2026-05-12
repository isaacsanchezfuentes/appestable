package com.example.appestable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appestable.data.*
import com.example.appestable.ui.theme.GastoFamiliaResumen
import com.example.appestable.ui.theme.GastoPersonaResumen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val personaDao = db.personaDao()
    private val familiaDao = db.familiaDao()
    private val gastoDao = db.gastoDao()
    private val actividadDao = db.actividadDao()
    private val participacionDao = db.participacionDao()

    // Personas y familias
    private val _persona = MutableStateFlow<List<Persona>>(emptyList())
    val persona = _persona.asStateFlow()

    private val _familiaList = MutableStateFlow<List<Familia>>(emptyList())
    val familiaList = _familiaList.asStateFlow()

    // Actividades
    private val _actividades = MutableStateFlow<List<Actividad>>(emptyList())
    val actividades = _actividades.asStateFlow()

    init {
        cargarDatos()
        cargarActividades()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _persona.value = personaDao.obtenerTodos()
            _familiaList.value = familiaDao.obtenerTodas()
        }
    }

    fun cargarActividades() {
        viewModelScope.launch {
            _actividades.value = actividadDao.obtenerTodas()
        }
    }

    fun agregarPersona(
        nombre: String,
        email: String,
        celular: String,
        familiaNombre: String,
        esJefe: Boolean
    ) {
        viewModelScope.launch {
            var familiaId = familiaList.value.find { it.nombreFamilia == familiaNombre }?.id
            if (familiaId == null) {
                familiaId = familiaDao.insertar(Familia(nombreFamilia = familiaNombre)).toInt()
                cargarDatos()
            }
            personaDao.insertar(
                Persona(
                    nombre = nombre,
                    email = email,
                    celular = celular,
                    familiaId = familiaId,
                    esJefe = esJefe
                )
            )
            cargarDatos()
        }
    }
    fun eliminarPersona(persona: Persona) {
        viewModelScope.launch {
            personaDao.eliminar(persona)
            cargarDatos()
        }
    }

    // Agregar actividad Y registrar participaciones
    fun agregarActividad(
        nombre: String,
        fecha: String,
        costoTotal: Double,
        participantes: List<Persona>
    ) {
        viewModelScope.launch {
            // Primero, inserta la actividad y obtén su ID
            val actividadId = actividadDao.insertar(
                Actividad(
                    nombre = nombre,
                    fecha = fecha,
                    costoTotal = costoTotal
                )
            ).toInt()
            // Inserta las participaciones
            participantes.forEach { persona ->
                participacionDao.insertar(
                    Participacion(
                        personaId = persona.id,
                        actividadId = actividadId
                    )
                )
            }
            cargarActividades()
        }
    }

    // NUEVO: Resumen de gastos por persona usando Participaciones + Actividades (sin división)
    suspend fun calcularResumenGastosPorPersona(): List<GastoPersonaResumen> {
        val personas = personaDao.obtenerTodos()
        val familias = familiaDao.obtenerTodas()
        val actividades = actividadDao.obtenerTodas()
        val participaciones = participacionDao.obtenerTodas()

        return personas.map { persona ->
            // Encuentra todas las actividades donde participó esta persona
            val actividadesPersona = participaciones
                .filter { it.personaId == persona.id }
                .mapNotNull { participacion ->
                    actividades.find { it.id == participacion.actividadId }
                }
            // Suma el costo total de todas esas actividades (sin dividir)
            val total = actividadesPersona.sumOf { it.costoTotal }

            val nombreFamilia = familias.find { it.id == persona.familiaId }?.nombreFamilia ?: ""
            GastoPersonaResumen(
                nombre = persona.nombre,
                familia = nombreFamilia,
                esJefe = persona.esJefe,
                total = total
            )
        }
    }

    // (Opcional) Resumen de gastos por familia — puedes actualizar lógica similar
    suspend fun calcularResumenGastosPorFamilia(): List<GastoFamiliaResumen> {
        val familias = familiaDao.obtenerTodas()
        val personas = personaDao.obtenerTodos()
        val actividades = actividadDao.obtenerTodas()
        val participaciones = participacionDao.obtenerTodas()
        return familias.map { familia ->
            val miembros = personas.filter { it.familiaId == familia.id }
            // Junta todas las actividades de todos los miembros
            val actividadesFamilia = participaciones
                .filter { part -> miembros.any { it.id == part.personaId } }
                .mapNotNull { part -> actividades.find { it.id == part.actividadId } }
            val total = actividadesFamilia.sumOf { it.costoTotal }
            GastoFamiliaResumen(
                familiaId = familia.id,
                nombreFamilia = familia.nombreFamilia,
                total = total
            )
        }
    }
}

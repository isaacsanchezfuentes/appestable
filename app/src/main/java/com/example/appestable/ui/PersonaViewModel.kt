package com.example.appestable.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appestable.auth.AuthManager
import com.example.appestable.data.*
import com.example.appestable.network.ActividadRequest
import com.example.appestable.network.PersonaRequest
import com.example.appestable.network.RetrofitClient
import com.example.appestable.ui.theme.GastoFamiliaResumen
import com.example.appestable.ui.theme.GastoPersonaResumen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PersonaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val authManager = AuthManager(application)

    private val personaDao = db.personaDao()
    private val familiaDao = db.familiaDao()
    private val gastoDao = db.gastoDao()
    private val actividadDao = db.actividadDao()
    private val participacionDao = db.participacionDao()

    // Personas y familias

    private val _persona =
        MutableStateFlow<List<Persona>>(emptyList())

    val persona =
        _persona.asStateFlow()

    private val _familiaList =
        MutableStateFlow<List<Familia>>(emptyList())

    val familiaList =
        _familiaList.asStateFlow()

    // Actividades

    private val _actividades =
        MutableStateFlow<List<Actividad>>(emptyList())

    val actividades =
        _actividades.asStateFlow()

    private val _participaciones =
        MutableStateFlow<List<Participacion>>(emptyList())

    val participaciones =
        _participaciones.asStateFlow()

    // Mensaje de error UI

    private val _mensajeError =
        MutableStateFlow<String?>(null)

    val mensajeError =
        _mensajeError.asStateFlow()

    init {

        cargarDatos()
        cargarActividades()
        cargarParticipaciones()
        sincronizarPersonasDesdeBackend()
    }

    fun cargarDatos() {

        viewModelScope.launch {

            _persona.value =
                personaDao.obtenerTodos()

            _familiaList.value =
                familiaDao.obtenerTodas()
        }
    }

    fun cargarActividades() {

        viewModelScope.launch {

            _actividades.value =
                actividadDao.obtenerTodas()
        }
    }

    fun cargarParticipaciones() {

        viewModelScope.launch {

            _participaciones.value =
                participacionDao.obtenerTodas()
        }
    }

    fun limpiarError() {

        _mensajeError.value = null
    }

    fun sincronizarPersonasDesdeBackend() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getPersonas()
                if (!response.isSuccessful) {
                    Log.e("API", "No se pudieron cargar personas: ${response.code()}")
                    return@launch
                }

                response.body().orEmpty().forEach { personaBackend ->
                    val nombreFamilia = personaBackend.familia_nombre?.takeIf { it.isNotBlank() } ?: "Sin Familia"
                    val familia = familiaDao.obtenerPorNombre(nombreFamilia)
                    val familiaId = familia?.id ?: familiaDao.insertar(
                        Familia(nombreFamilia = nombreFamilia)
                    ).toInt()

                    val emailBackend = personaBackend.email.orEmpty()
                    val existente = if (emailBackend.isNotBlank()) {
                        personaDao.obtenerPorEmail(emailBackend)
                    } else {
                        personaDao.obtenerPorNombreYFamilia(personaBackend.nombre, familiaId)
                    }

                    if (existente == null) {
                        personaDao.insertar(
                            Persona(
                                nombre = personaBackend.nombre,
                                email = emailBackend,
                                celular = personaBackend.celular.orEmpty(),
                                familiaId = familiaId,
                                esJefe = personaBackend.es_jefe,
                                backendId = personaBackend.id
                            )
                        )
                    } else if (existente.backendId == null && personaBackend.id != null) {
                        personaDao.actualizarBackendId(existente.id, personaBackend.id)
                    }
                }

                cargarDatos()
            } catch (e: Exception) {
                Log.e("API", "Fallo cargando personas del backend: ${e.message}")
            }
        }
    }

    private suspend fun getSessionInfo(): Pair<String?, String?> {
        return suspendCoroutine { continuation ->
            if (!authManager.isLoggedIn()) {
                continuation.resume(null to null)
                return@suspendCoroutine
            }
            authManager.restoreSession { email, token ->
                continuation.resume(email to token)
            }
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

            var familiaId =
                familiaList.value
                    .find {
                        it.nombreFamilia == familiaNombre
                    }
                    ?.id

            // Si no existe la familia, créala

            if (familiaId == null) {

                familiaId = familiaDao.insertar(

                    Familia(
                        nombreFamilia = familiaNombre
                    )

                ).toInt()

                cargarDatos()
            }

            // VALIDACIÓN:
            // Solo puede existir un jefe por familia

            if (esJefe) {

                val jefeExistente =
                    personaDao.obtenerJefePorFamilia(
                        familiaId
                    )

                if (jefeExistente != null) {

                    _mensajeError.value =
                        "Solo puede existir un jefe por familia"

                    return@launch
                }
            }

            // Insertar persona

            val localPersonaId = personaDao.insertar(

                Persona(
                    nombre = nombre,
                    email = email,
                    celular = celular,
                    familiaId = familiaId,
                    esJefe = esJefe
                )
            ).toInt()

            // 🔥 SINCRONIZACIÓN BACKEND
            val (_, token) = getSessionInfo()
            if (token != null) {
                try {
                    // Si el formulario no trae email, se guarda como integrante sin usuario Auth0.
                    val emailFinal = email.takeIf { it.isNotBlank() }

                    val response = RetrofitClient.api.registrarPersona(
                        "Bearer $token",
                        PersonaRequest(
                            nombre = nombre,
                            familia_nombre = familiaNombre,
                            email = emailFinal,
                            celular = celular,
                            es_jefe = esJefe
                        )
                    )
                    if (response.isSuccessful) {
                        response.body()?.id?.let { backendId ->
                            personaDao.actualizarBackendId(localPersonaId, backendId)
                        }
                        Log.d("API", "Persona sincronizada con el backend")
                    } else {
                        Log.e("API", "Error backend: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("API", "Fallo de conexión al sincronizar persona: ${e.message}")
                }
            }

            cargarDatos()
        }
    }

    fun eliminarPersona(persona: Persona) {

        viewModelScope.launch {

            personaDao.eliminar(persona)

            cargarDatos()
        }
    }

    // Agregar actividad y participaciones

    fun agregarActividad(
        nombre: String,
        fecha: String,
        costoTotal: Double,
        participantes: List<Persona>
    ) {

        viewModelScope.launch {

            val actividadId = actividadDao.insertar(

                Actividad(
                    nombre = nombre,
                    fecha = fecha,
                    costoTotal = costoTotal
                )

            ).toInt()

            val montoPorParticipante = costoTotal / participantes.size.coerceAtLeast(1)

            participantes.forEach { persona ->

                participacionDao.insertar(

                    Participacion(
                        personaId = persona.id,
                        actividadId = actividadId,
                        montoAsignado = montoPorParticipante
                    )
                )
            }

            // 🔥 SINCRONIZACIÓN BACKEND
            val (_, token) = getSessionInfo()
            if (token != null) {
                try {
                    val response = RetrofitClient.api.registrarActividad(
                        "Bearer $token",
                        ActividadRequest(
                            nombre = nombre,
                            costo_total = costoTotal,
                            fecha = fecha,
                            participantes_ids = participantes.mapNotNull { it.backendId }
                        )
                    )
                    if (response.isSuccessful) {
                        Log.d("API", "Actividad sincronizada con el backend")
                    }
                } catch (e: Exception) {
                    Log.e("API", "Error sincronizando actividad: ${e.message}")
                }
            }

            cargarActividades()
            cargarParticipaciones()
        }
    }

    fun actualizarMontoParticipacion(
        personaId: Int,
        actividadId: Int,
        monto: Double
    ) {
        viewModelScope.launch {
            participacionDao.actualizarMonto(personaId, actividadId, monto)
            cargarParticipaciones()
        }
    }

    fun montoAsignado(participacion: Participacion, actividad: Actividad): Double {
        if (participacion.montoAsignado > 0.0) return participacion.montoAsignado

        val totalParticipantes = _participaciones.value
            .count { it.actividadId == actividad.id }
            .coerceAtLeast(1)

        return actividad.costoTotal / totalParticipantes
    }

    // Resumen gastos por persona

    suspend fun calcularResumenGastosPorPersona():
            List<GastoPersonaResumen> {

        val personas =
            personaDao.obtenerTodos()

        val familias =
            familiaDao.obtenerTodas()

        val actividades =
            actividadDao.obtenerTodas()

        val participaciones =
            participacionDao.obtenerTodas()

        return personas.map { persona ->

            val total = participaciones
                .filter { it.personaId == persona.id }
                .sumOf { participacion ->
                    val actividad = actividades.find { it.id == participacion.actividadId }
                    if (actividad != null) montoAsignado(participacion, actividad) else 0.0
                }

            val nombreFamilia = familias.find {
                it.id == persona.familiaId
            }?.nombreFamilia ?: ""

            GastoPersonaResumen(
                nombre = persona.nombre,
                familia = nombreFamilia,
                esJefe = persona.esJefe,
                total = total
            )
        }
    }

    // Resumen gastos por familia

    suspend fun calcularResumenGastosPorFamilia():
            List<GastoFamiliaResumen> {

        val familias =
            familiaDao.obtenerTodas()

        val personas =
            personaDao.obtenerTodos()

        val actividades =
            actividadDao.obtenerTodas()

        val participaciones =
            participacionDao.obtenerTodas()

        return familias.map { familia ->

            val miembros = personas.filter {
                it.familiaId == familia.id
            }

            val total = participaciones
                .filter { part ->
                    miembros.any { it.id == part.personaId }
                }
                .sumOf { part ->
                    val actividad = actividades.find { it.id == part.actividadId }
                    if (actividad != null) montoAsignado(part, actividad) else 0.0
                }

            GastoFamiliaResumen(
                familiaId = familia.id,
                nombreFamilia = familia.nombreFamilia,
                total = total
            )
        }
    }
}
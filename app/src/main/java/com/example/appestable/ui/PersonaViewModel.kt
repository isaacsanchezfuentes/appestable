package com.example.appestable.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appestable.auth.AuthManager
import com.example.appestable.data.*
import com.example.appestable.domain.PermissionPolicy
import com.example.appestable.domain.ResumenCalculator
import com.example.appestable.domain.ResumenFamiliaDetalle
import com.example.appestable.domain.ResumenViajeGlobal
import com.example.appestable.domain.SessionContext
import com.example.appestable.network.ActividadRequest
import com.example.appestable.network.ParticipacionUpdateRequest
import com.example.appestable.network.PersonaRequest
import com.example.appestable.sync.ViajeSyncService
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
    private val syncService = ViajeSyncService(db)

    private val viajeDao = db.viajeDao()
    private val usuarioDao = db.usuarioDao()
    private val membresiaDao = db.membresiaViajeDao()
    private val personaDao = db.personaDao()
    private val familiaDao = db.familiaDao()
    private val actividadDao = db.actividadDao()
    private val participacionDao = db.participacionDao()

    private val _viajes = MutableStateFlow<List<Viaje>>(emptyList())
    val viajes = _viajes.asStateFlow()

    private val _viajeActivo = MutableStateFlow<Viaje?>(null)
    val viajeActivo = _viajeActivo.asStateFlow()

    private val _session = MutableStateFlow(SessionContext.GUEST_ORGANIZER)
    val session = _session.asStateFlow()

    private val _persona = MutableStateFlow<List<Persona>>(emptyList())
    val persona = _persona.asStateFlow()

    private val _familiaList = MutableStateFlow<List<Familia>>(emptyList())
    val familiaList = _familiaList.asStateFlow()

    private val _actividades = MutableStateFlow<List<Actividad>>(emptyList())
    val actividades = _actividades.asStateFlow()

    private val _participaciones = MutableStateFlow<List<Participacion>>(emptyList())
    val participaciones = _participaciones.asStateFlow()

    private val _resumenesFamilia = MutableStateFlow<List<ResumenFamiliaDetalle>>(emptyList())
    val resumenesFamilia = _resumenesFamilia.asStateFlow()

    private val _resumenGlobal = MutableStateFlow<ResumenViajeGlobal?>(null)
    val resumenGlobal = _resumenGlobal.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError = _mensajeError.asStateFlow()

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando = _sincronizando.asStateFlow()

    init {
        viewModelScope.launch {
            ensureSeedData()
            cargarViajes()
            val activo = viajeDao.obtenerPorId(1) ?: viajeDao.obtenerTodos().firstOrNull()
            if (activo != null) seleccionarViaje(activo.id, sincronizar = false)
        }
    }

    private suspend fun ensureSeedData() {
        if (viajeDao.obtenerTodos().isEmpty()) {
            viajeDao.insertar(
                Viaje(
                    id = 1,
                    nombre = "Viaje Principal",
                    descripcion = "Viaje por defecto",
                    backendId = 1
                )
            )
        }
    }

    fun canViewFamilia(familiaId: Int): Boolean =
        PermissionPolicy.canViewFamilia(_session.value, familiaId)

    fun canAddPersona(familiaId: Int): Boolean =
        PermissionPolicy.canAddPersona(_session.value, familiaId)

    fun canDeletePersona(persona: Persona): Boolean =
        PermissionPolicy.canDeletePersona(_session.value, persona)

    fun canCreateActividad(): Boolean =
        PermissionPolicy.canCreateActividad(_session.value)

    fun canSelectParticipante(persona: Persona): Boolean =
        PermissionPolicy.canSelectParticipante(_session.value, persona)

    fun canEditParticipacion(familiaId: Int): Boolean =
        PermissionPolicy.canEditParticipacion(_session.value, familiaId)

    fun canViewResumenGlobal(): Boolean =
        PermissionPolicy.canViewResumenGlobal(_session.value)

    fun canCreateViaje(): Boolean =
        authManager.isLoggedIn() || PermissionPolicy.canCreateViaje(_session.value)

    fun familiasVisibles(): List<Familia> {
        val current = _session.value
        return _familiaList.value.filter { PermissionPolicy.canViewFamilia(current, it.id) }
    }

    fun seleccionarViaje(viajeId: Int, sincronizar: Boolean = true) {
        viewModelScope.launch {
            val viaje = viajeDao.obtenerPorId(viajeId) ?: return@launch
            _viajeActivo.value = viaje
            actualizarSesionParaViaje(viaje)
            recargarDatosViaje(viajeId)
            if (sincronizar) sincronizarViajeActivo()
        }
    }

    fun sincronizarDesdeBackend() {
        viewModelScope.launch { sincronizarViajeActivo(incluirCuenta = true) }
    }

    private suspend fun sincronizarViajeActivo(incluirCuenta: Boolean = false) {
        val token = getAccessToken() ?: return
        val viajeId = _viajeActivo.value?.id ?: return
        val usuarioId = _session.value.usuarioId ?: resolveUsuarioId() ?: return

        _sincronizando.value = true
        try {
            if (incluirCuenta) {
                syncService.sincronizarCuenta(token, usuarioId)
                cargarViajes()
            }
            syncService.sincronizarViajeCompleto(viajeId, token)
            recargarDatosViaje(viajeId)
            _viajeActivo.value?.let { actualizarSesionParaViaje(it) }
        } finally {
            _sincronizando.value = false
        }
    }

    fun crearViaje(nombre: String) {
        viewModelScope.launch {
            val usuarioId = _session.value.usuarioId
            val token = getAccessToken()

            var backendId: Int? = null
            if (token != null) {
                backendId = syncService.crearViajeEnBackend(nombre.trim(), token)
            }

            val viajeId = viajeDao.insertar(
                Viaje(
                    nombre = nombre.trim(),
                    organizadorUsuarioId = usuarioId,
                    backendId = backendId
                )
            ).toInt()

            if (usuarioId != null) {
                membresiaDao.insertar(
                    MembresiaViaje(
                        viajeId = viajeId,
                        usuarioId = usuarioId,
                        familiaId = null,
                        rol = RolViaje.ORGANIZADOR
                    )
                )
            }

            cargarViajes()
            seleccionarViaje(viajeId, sincronizar = token != null)
        }
    }

    fun onAuthSession(authSession: AuthManager.AuthSession) {
        viewModelScope.launch {
            val auth0Id = authSession.auth0Id ?: return@launch
            val nombre = authSession.email.substringBefore("@")
            val usuarioId = usuarioDao.insertar(
                Usuario(
                    auth0Id = auth0Id,
                    email = authSession.email,
                    nombre = nombre
                )
            ).toInt()

            val viajeId = _viajeActivo.value?.id ?: 1
            val membresiaExistente = membresiaDao.obtenerPorViajeYUsuario(viajeId, usuarioId)
            if (membresiaExistente == null) {
                membresiaDao.insertar(
                    MembresiaViaje(
                        viajeId = viajeId,
                        usuarioId = usuarioId,
                        familiaId = null,
                        rol = RolViaje.ORGANIZADOR
                    )
                )
            }

            syncService.sincronizarCuenta(authSession.accessToken, usuarioId)
            cargarViajes()
            _viajeActivo.value?.let { actualizarSesionParaViaje(it) }
            sincronizarViajeActivo()
        }
    }

    fun onLogout() {
        _session.value = _viajeActivo.value?.let { viaje ->
            SessionContext(
                viajeId = viaje.id,
                viajeNombre = viaje.nombre,
                rol = RolViaje.ORGANIZADOR,
                isLoggedIn = false
            )
        } ?: SessionContext.GUEST_ORGANIZER
        recalcularResumenes()
    }

    private suspend fun actualizarSesionParaViaje(viaje: Viaje) {
        if (!authManager.isLoggedIn()) {
            _session.value = SessionContext(
                viajeId = viaje.id,
                viajeNombre = viaje.nombre,
                rol = RolViaje.ORGANIZADOR,
                isLoggedIn = false
            )
            return
        }

        val authSession = getAuthSession() ?: run {
            _session.value = SessionContext(
                viajeId = viaje.id,
                viajeNombre = viaje.nombre,
                rol = RolViaje.ORGANIZADOR,
                isLoggedIn = false
            )
            return
        }

        val auth0Id = authSession.auth0Id
        if (auth0Id == null) {
            _session.value = SessionContext(
                viajeId = viaje.id,
                viajeNombre = viaje.nombre,
                rol = RolViaje.ORGANIZADOR,
                isLoggedIn = true
            )
            return
        }

        val usuario = usuarioDao.obtenerPorAuth0Id(auth0Id)
        if (usuario == null) {
            onAuthSession(authSession)
            return
        }

        val membresia = membresiaDao.obtenerPorViajeYUsuario(viaje.id, usuario.id)
        _session.value = SessionContext(
            viajeId = viaje.id,
            viajeNombre = viaje.nombre,
            rol = membresia?.rol ?: RolViaje.MIEMBRO,
            familiaId = membresia?.familiaId,
            usuarioId = usuario.id,
            isLoggedIn = true
        )
    }

    private suspend fun resolveUsuarioId(): Int? {
        val authSession = getAuthSession() ?: return null
        val auth0Id = authSession.auth0Id ?: return null
        return usuarioDao.obtenerPorAuth0Id(auth0Id)?.id
    }

    private suspend fun cargarViajes() {
        _viajes.value = viajeDao.obtenerTodos()
    }

    private suspend fun recargarDatosViaje(viajeId: Int) {
        _persona.value = personaDao.obtenerPorViaje(viajeId)
        _familiaList.value = familiaDao.obtenerPorViaje(viajeId)
        _actividades.value = actividadDao.obtenerPorViaje(viajeId)
        _participaciones.value = participacionDao.obtenerPorViaje(viajeId)
        recalcularResumenes()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _viajeActivo.value?.let { recargarDatosViaje(it.id) }
        }
    }

    fun cargarActividades() = cargarDatos()
    fun cargarParticipaciones() = cargarDatos()

    fun limpiarError() {
        _mensajeError.value = null
    }

    private fun recalcularResumenes() {
        val familias = familiasVisibles()
        val personas = _persona.value
        val actividades = _actividades.value
        val participaciones = _participaciones.value

        _resumenesFamilia.value = familias.map { familia ->
            ResumenCalculator.calcularFamilia(familia, personas, actividades, participaciones)
        }

        _resumenGlobal.value = if (canViewResumenGlobal()) {
            ResumenCalculator.calcularGlobal(
                _familiaList.value,
                personas,
                actividades,
                participaciones
            )
        } else null
    }

    private suspend fun getAuthSession(): AuthManager.AuthSession? {
        return suspendCoroutine { continuation ->
            if (!authManager.isLoggedIn()) {
                continuation.resume(null)
                return@suspendCoroutine
            }
            authManager.restoreSession { session ->
                continuation.resume(session)
            }
        }
    }

    private suspend fun getAccessToken(): String? = getAuthSession()?.accessToken

    fun agregarPersona(
        nombre: String,
        email: String,
        celular: String,
        familiaNombre: String,
        esJefe: Boolean
    ) {
        viewModelScope.launch {
            val viajeId = _viajeActivo.value?.id ?: return@launch

            var familiaId = _familiaList.value.find { it.nombreFamilia == familiaNombre }?.id
            if (familiaId == null) {
                familiaId = familiaDao.insertar(
                    Familia(nombreFamilia = familiaNombre, viajeId = viajeId)
                ).toInt()
            }

            if (!canAddPersona(familiaId)) {
                _mensajeError.value = "No tienes permiso para agregar personas en esta familia"
                return@launch
            }

            if (esJefe) {
                val jefeExistente = personaDao.obtenerJefePorFamilia(familiaId)
                if (jefeExistente != null) {
                    _mensajeError.value = "Solo puede existir un jefe por familia"
                    return@launch
                }
            }

            val rol = if (esJefe) RolViaje.JEFE_FAMILIA else RolViaje.MIEMBRO
            val localPersonaId = personaDao.insertar(
                Persona(
                    nombre = nombre,
                    email = email,
                    celular = celular,
                    familiaId = familiaId,
                    esJefe = esJefe,
                    viajeId = viajeId,
                    rol = rol
                )
            ).toInt()

            val token = getAccessToken()
            if (token != null) {
                val backendId = syncService.pushPersona(
                    viajeId,
                    token,
                    PersonaRequest(
                        nombre = nombre,
                        familia_nombre = familiaNombre,
                        email = email.takeIf { it.isNotBlank() },
                        celular = celular,
                        es_jefe = esJefe
                    )
                )
                backendId?.let { personaDao.actualizarBackendId(localPersonaId, it) }
            }

            cargarDatos()
        }
    }

    fun eliminarPersona(persona: Persona) {
        viewModelScope.launch {
            if (!canDeletePersona(persona)) {
                _mensajeError.value = "No tienes permiso para eliminar esta persona"
                return@launch
            }

            val viajeId = _viajeActivo.value?.id ?: return@launch
            val token = getAccessToken()
            if (token != null && persona.backendId != null) {
                syncService.pushDeletePersona(viajeId, persona.backendId, token)
            }

            personaDao.eliminar(persona)
            cargarDatos()
        }
    }

    fun agregarActividad(
        nombre: String,
        fecha: String,
        costoTotal: Double,
        participantes: List<Persona>
    ) {
        viewModelScope.launch {
            if (!canCreateActividad()) {
                _mensajeError.value = "No tienes permiso para crear actividades"
                return@launch
            }

            val viajeId = _viajeActivo.value?.id ?: return@launch
            val usuarioId = _session.value.usuarioId
            val token = getAccessToken()

            val participantesConBackend = participantes.filter { it.backendId != null }
            if (token != null && participantesConBackend.size != participantes.size) {
                sincronizarViajeActivo()
            }

            val actividadId = actividadDao.insertar(
                Actividad(
                    nombre = nombre,
                    fecha = fecha,
                    costoTotal = costoTotal,
                    viajeId = viajeId,
                    creadoPorUsuarioId = usuarioId
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

            if (token != null) {
                val refreshed = personaDao.obtenerPorViaje(viajeId)
                val idsBackend = participantes.mapNotNull { p ->
                    refreshed.find { it.id == p.id }?.backendId
                }
                if (idsBackend.isNotEmpty()) {
                    val backendActividadId = syncService.pushActividad(
                        viajeId,
                        token,
                        ActividadRequest(
                            nombre = nombre,
                            costo_total = costoTotal,
                            fecha = fecha,
                            participantes_ids = idsBackend
                        )
                    )
                    backendActividadId?.let { actividadDao.actualizarBackendId(actividadId, it) }
                    syncService.sincronizarViajeCompleto(viajeId, token)
                }
            }

            cargarDatos()
        }
    }

    fun actualizarMontoParticipacion(personaId: Int, actividadId: Int, monto: Double) {
        viewModelScope.launch {
            val persona = _persona.value.find { it.id == personaId } ?: return@launch
            if (!canEditParticipacion(persona.familiaId)) {
                _mensajeError.value = "No tienes permiso para editar este monto"
                return@launch
            }

            participacionDao.actualizarMonto(personaId, actividadId, monto)
            pushParticipacionSiPosible(personaId, actividadId, monto = monto)
            cargarDatos()
        }
    }

    fun actualizarPagadoParticipacion(personaId: Int, actividadId: Int, pagado: Boolean) {
        viewModelScope.launch {
            val persona = _persona.value.find { it.id == personaId } ?: return@launch
            if (!canEditParticipacion(persona.familiaId)) {
                _mensajeError.value = "No tienes permiso para cambiar el estado de pago"
                return@launch
            }

            participacionDao.actualizarPagado(personaId, actividadId, pagado)
            pushParticipacionSiPosible(personaId, actividadId, pagado = pagado)
            cargarDatos()
        }
    }

    private suspend fun pushParticipacionSiPosible(
        personaId: Int,
        actividadId: Int,
        monto: Double? = null,
        pagado: Boolean? = null
    ) {
        val viajeId = _viajeActivo.value?.id ?: return
        val token = getAccessToken() ?: return

        var participacion = participacionDao.obtenerPorPersonaYActividad(personaId, actividadId)
        if (participacion?.backendId == null) {
            syncService.sincronizarViajeCompleto(viajeId, token)
            participacion = participacionDao.obtenerPorPersonaYActividad(personaId, actividadId)
        }

        val backendId = participacion?.backendId ?: return
        syncService.pushParticipacion(
            viajeId,
            backendId,
            token,
            ParticipacionUpdateRequest(
                costo_individual = monto,
                pagado = pagado
            )
        )
    }

    fun montoAsignado(participacion: Participacion, actividad: Actividad): Double =
        ResumenCalculator.montoEfectivo(participacion, actividad, _participaciones.value)

    suspend fun calcularResumenGastosPorPersona(): List<GastoPersonaResumen> {
        val personas = _persona.value
        val familias = _familiaList.value
        val actividades = _actividades.value
        val participaciones = _participaciones.value

        return personas.map { persona ->
            val total = participaciones
                .filter { it.personaId == persona.id }
                .sumOf { part ->
                    val actividad = actividades.find { it.id == part.actividadId }
                    if (actividad != null) montoAsignado(part, actividad) else 0.0
                }

            GastoPersonaResumen(
                nombre = persona.nombre,
                familia = familias.find { it.id == persona.familiaId }?.nombreFamilia ?: "",
                esJefe = persona.esJefe,
                total = total
            )
        }
    }

    suspend fun calcularResumenGastosPorFamilia(): List<GastoFamiliaResumen> =
        _resumenesFamilia.value.map {
            GastoFamiliaResumen(
                familiaId = it.familiaId,
                nombreFamilia = it.nombreFamilia,
                total = it.totalAsignado
            )
        }
}
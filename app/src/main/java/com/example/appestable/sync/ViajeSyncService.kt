package com.example.appestable.sync

import android.util.Log
import com.example.appestable.data.*
import com.example.appestable.network.*

class ViajeSyncService(
    private val db: AppDatabase,
    private val api: ApiService = RetrofitClient.api
) {
    private val viajeDao = db.viajeDao()
    private val usuarioDao = db.usuarioDao()
    private val membresiaDao = db.membresiaViajeDao()
    private val personaDao = db.personaDao()
    private val familiaDao = db.familiaDao()
    private val actividadDao = db.actividadDao()
    private val participacionDao = db.participacionDao()

    private fun bearer(token: String) = "Bearer $token"

    fun backendViajeId(viaje: Viaje): Int = viaje.backendId ?: viaje.id

    suspend fun sincronizarCuenta(token: String, localUsuarioId: Int): Boolean {
        return try {
            val meResponse = api.getMe(bearer(token))
            if (!meResponse.isSuccessful) {
                Log.e("SYNC", "GET /me falló: ${meResponse.code()}")
                return false
            }
            val me = meResponse.body() ?: return false
            aplicarMembresias(me.membresias, localUsuarioId)
            sincronizarViajes(token)
            true
        } catch (e: Exception) {
            Log.e("SYNC", "Error sincronizando cuenta: ${e.message}")
            false
        }
    }

    suspend fun sincronizarViajes(token: String) {
        val response = api.getViajes(bearer(token))
        if (!response.isSuccessful) return

        response.body().orEmpty().forEach { viajeBackend ->
            val existente = viajeDao.obtenerPorBackendId(viajeBackend.id)
            if (existente == null) {
                viajeDao.insertar(
                    Viaje(
                        nombre = viajeBackend.nombre,
                        descripcion = viajeBackend.descripcion,
                        fechaInicio = viajeBackend.fecha_inicio,
                        fechaFin = viajeBackend.fecha_fin,
                        estado = viajeBackend.estado,
                        backendId = viajeBackend.id
                    )
                )
            } else {
                viajeDao.actualizar(
                    existente.copy(
                        nombre = viajeBackend.nombre,
                        descripcion = viajeBackend.descripcion,
                        fechaInicio = viajeBackend.fecha_inicio,
                        fechaFin = viajeBackend.fecha_fin,
                        estado = viajeBackend.estado,
                        backendId = viajeBackend.id
                    )
                )
            }
        }
    }

    private suspend fun aplicarMembresias(membresias: List<com.example.appestable.network.models.MembresiaResponse>, localUsuarioId: Int) {
        membresias.forEach { membresia ->
            var viajeLocal = viajeDao.obtenerPorBackendId(membresia.viaje_id)
            if (viajeLocal == null) {
                val newId = viajeDao.insertar(
                    Viaje(
                        nombre = membresia.viaje_nombre,
                        backendId = membresia.viaje_id
                    )
                ).toInt()
                viajeLocal = viajeDao.obtenerPorId(newId) ?: return@forEach
            }

            val rol = parseRol(membresia.rol)
            val familiaIdLocal = membresia.familia_id?.let { backendFamiliaId ->
                familiaDao.obtenerPorBackendIdYViaje(backendFamiliaId, viajeLocal.id)?.id
            }

            val existente = membresiaDao.obtenerPorViajeYUsuario(viajeLocal.id, localUsuarioId)
            if (existente == null) {
                membresiaDao.insertar(
                    MembresiaViaje(
                        viajeId = viajeLocal.id,
                        usuarioId = localUsuarioId,
                        familiaId = familiaIdLocal,
                        rol = rol
                    )
                )
            } else {
                membresiaDao.insertar(
                    existente.copy(rol = rol, familiaId = familiaIdLocal ?: existente.familiaId)
                )
            }
        }
    }

    suspend fun sincronizarViajeCompleto(localViajeId: Int, token: String): Boolean {
        val viaje = viajeDao.obtenerPorId(localViajeId) ?: return false
        val backendId = backendViajeId(viaje)

        return try {
            sincronizarPersonas(backendId, localViajeId, token)
            sincronizarActividades(backendId, localViajeId, token)
            sincronizarParticipaciones(backendId, localViajeId, token)
            true
        } catch (e: Exception) {
            Log.e("SYNC", "Error sincronizando viaje $localViajeId: ${e.message}")
            false
        }
    }

    private suspend fun sincronizarPersonas(backendViajeId: Int, localViajeId: Int, token: String) {
        val response = api.getPersonas(bearer(token), backendViajeId)
        if (!response.isSuccessful) return

        response.body().orEmpty().forEach { personaBackend ->
            val backendPersonaId = personaBackend.id ?: return@forEach
            val nombreFamilia = personaBackend.familia_nombre?.takeIf { it.isNotBlank() } ?: "Sin Familia"

            val familiaId = upsertFamilia(localViajeId, nombreFamilia, personaBackend.familia_id)
            val rol = parseRol(personaBackend.rol, personaBackend.es_jefe)

            val existente = personaDao.obtenerPorBackendIdYViaje(backendPersonaId, localViajeId)
                ?: personaBackend.email?.takeIf { it.isNotBlank() }?.let { personaDao.obtenerPorEmail(it) }
                ?: personaDao.obtenerPorNombreYFamilia(personaBackend.nombre, familiaId)

            if (existente == null) {
                personaDao.insertar(
                    Persona(
                        nombre = personaBackend.nombre,
                        email = personaBackend.email.orEmpty(),
                        celular = personaBackend.celular.orEmpty(),
                        familiaId = familiaId,
                        esJefe = personaBackend.es_jefe,
                        backendId = backendPersonaId,
                        viajeId = localViajeId,
                        rol = rol
                    )
                )
            } else {
                personaDao.actualizar(
                    existente.copy(
                        nombre = personaBackend.nombre,
                        email = personaBackend.email.orEmpty(),
                        celular = personaBackend.celular.orEmpty(),
                        familiaId = familiaId,
                        esJefe = personaBackend.es_jefe,
                        backendId = backendPersonaId,
                        rol = rol
                    )
                )
            }
        }
    }

    private suspend fun sincronizarActividades(backendViajeId: Int, localViajeId: Int, token: String) {
        val response = api.getActividades(bearer(token), backendViajeId)
        if (!response.isSuccessful) return

        response.body().orEmpty().forEach { actividadBackend ->
            val existente = actividadDao.obtenerPorBackendIdYViaje(actividadBackend.id, localViajeId)
            if (existente == null) {
                actividadDao.insertar(
                    Actividad(
                        nombre = actividadBackend.nombre,
                        fecha = actividadBackend.fecha,
                        costoTotal = actividadBackend.costo_total,
                        viajeId = localViajeId,
                        backendId = actividadBackend.id
                    )
                )
            } else {
                actividadDao.actualizar(
                    existente.copy(
                        nombre = actividadBackend.nombre,
                        fecha = actividadBackend.fecha,
                        costoTotal = actividadBackend.costo_total,
                        backendId = actividadBackend.id
                    )
                )
            }
        }
    }

    private suspend fun sincronizarParticipaciones(backendViajeId: Int, localViajeId: Int, token: String) {
        val response = api.getParticipaciones(bearer(token), backendViajeId)
        if (!response.isSuccessful) return

        val personas = personaDao.obtenerPorViaje(localViajeId)
        val actividades = actividadDao.obtenerPorViaje(localViajeId)

        response.body().orEmpty().forEach { partBackend ->
            val personaLocal = personas.find { it.backendId == partBackend.persona_id } ?: return@forEach
            val actividadLocal = actividades.find { it.backendId == partBackend.actividad_id } ?: return@forEach

            val existente = participacionDao.obtenerPorPersonaYActividad(personaLocal.id, actividadLocal.id)
            if (existente == null) {
                participacionDao.insertar(
                    Participacion(
                        personaId = personaLocal.id,
                        actividadId = actividadLocal.id,
                        montoAsignado = partBackend.costo_individual,
                        pagado = partBackend.pagado,
                        backendId = partBackend.id
                    )
                )
            } else {
                participacionDao.actualizar(
                    existente.copy(
                        montoAsignado = partBackend.costo_individual,
                        pagado = partBackend.pagado,
                        backendId = partBackend.id
                    )
                )
            }
        }
    }

    suspend fun crearViajeEnBackend(nombre: String, token: String): Int? {
        return try {
            val response = api.crearViaje(
                bearer(token),
                ViajeRequest(nombre = nombre.trim())
            )
            if (response.isSuccessful) response.body()?.id else null
        } catch (e: Exception) {
            Log.e("SYNC", "Error creando viaje en backend: ${e.message}")
            null
        }
    }

    suspend fun pushPersona(
        localViajeId: Int,
        token: String,
        request: PersonaRequest
    ): Int? {
        val viaje = viajeDao.obtenerPorId(localViajeId) ?: return null
        return try {
            val response = api.crearPersona(bearer(token), backendViajeId(viaje), request)
            if (response.isSuccessful) response.body()?.id else null
        } catch (e: Exception) {
            Log.e("SYNC", "Error pusheando persona: ${e.message}")
            null
        }
    }

    suspend fun pushDeletePersona(localViajeId: Int, personaBackendId: Int, token: String): Boolean {
        val viaje = viajeDao.obtenerPorId(localViajeId) ?: return false
        return try {
            val response = api.eliminarPersona(bearer(token), backendViajeId(viaje), personaBackendId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SYNC", "Error eliminando persona en backend: ${e.message}")
            false
        }
    }

    suspend fun pushActividad(
        localViajeId: Int,
        token: String,
        request: ActividadRequest
    ): Int? {
        val viaje = viajeDao.obtenerPorId(localViajeId) ?: return null
        return try {
            val response = api.crearActividad(bearer(token), backendViajeId(viaje), request)
            if (response.isSuccessful) response.body()?.id else null
        } catch (e: Exception) {
            Log.e("SYNC", "Error pusheando actividad: ${e.message}")
            null
        }
    }

    suspend fun pushParticipacion(
        localViajeId: Int,
        participacionBackendId: Int,
        token: String,
        update: ParticipacionUpdateRequest
    ): Boolean {
        val viaje = viajeDao.obtenerPorId(localViajeId) ?: return false
        return try {
            val response = api.actualizarParticipacion(
                bearer(token),
                backendViajeId(viaje),
                participacionBackendId,
                update
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SYNC", "Error pusheando participacion: ${e.message}")
            false
        }
    }

    private suspend fun upsertFamilia(localViajeId: Int, nombreFamilia: String, backendFamiliaId: Int?): Int {
        val existente = familiaDao.obtenerPorNombreYViaje(localViajeId, nombreFamilia)
        if (existente != null) {
            if (backendFamiliaId != null && existente.backendId == null) {
                familiaDao.actualizar(existente.copy(backendId = backendFamiliaId))
            }
            return existente.id
        }
        return familiaDao.insertar(
            Familia(
                nombreFamilia = nombreFamilia,
                viajeId = localViajeId,
                backendId = backendFamiliaId
            )
        ).toInt()
    }

    private fun parseRol(rol: String?, esJefe: Boolean = false): RolViaje {
        return when (rol?.uppercase()) {
            "ORGANIZADOR" -> RolViaje.ORGANIZADOR
            "JEFE_FAMILIA" -> RolViaje.JEFE_FAMILIA
            "MIEMBRO" -> RolViaje.MIEMBRO
            else -> if (esJefe) RolViaje.JEFE_FAMILIA else RolViaje.MIEMBRO
        }
    }
}
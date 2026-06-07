package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ParticipacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(participacion: Participacion)

    @Update
    suspend fun actualizar(participacion: Participacion)

    @Query("""
        SELECT p.* FROM participaciones p
        INNER JOIN actividades a ON p.actividadId = a.id
        WHERE a.viajeId = :viajeId
    """)
    suspend fun obtenerPorViaje(viajeId: Int): List<Participacion>

    @Query("SELECT * FROM participaciones WHERE actividadId = :actividadId")
    suspend fun obtenerParticipantes(actividadId: Int): List<Participacion>

    @Query("SELECT * FROM participaciones")
    suspend fun obtenerTodas(): List<Participacion>

    @Query("SELECT * FROM participaciones WHERE personaId = :personaId")
    suspend fun obtenerPorPersona(personaId: Int): List<Participacion>

    @Query("""
        SELECT * FROM participaciones
        WHERE personaId = :personaId AND actividadId = :actividadId
        LIMIT 1
    """)
    suspend fun obtenerPorPersonaYActividad(personaId: Int, actividadId: Int): Participacion?

    @Query("""
        UPDATE participaciones
        SET montoAsignado = :monto
        WHERE personaId = :personaId
        AND actividadId = :actividadId
    """)
    suspend fun actualizarMonto(personaId: Int, actividadId: Int, monto: Double)

    @Query("""
        UPDATE participaciones
        SET pagado = :pagado
        WHERE personaId = :personaId
        AND actividadId = :actividadId
    """)
    suspend fun actualizarPagado(personaId: Int, actividadId: Int, pagado: Boolean)

    @Query("UPDATE participaciones SET backendId = :backendId WHERE id = :participacionId")
    suspend fun actualizarBackendId(participacionId: Int, backendId: Int)
}
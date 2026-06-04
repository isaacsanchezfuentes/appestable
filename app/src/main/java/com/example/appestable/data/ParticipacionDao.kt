package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ParticipacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(participacion: Participacion)

    @Query("SELECT * FROM participaciones WHERE actividadId = :actividadId")
    suspend fun obtenerParticipantes(actividadId: Int): List<Participacion>

    @Query("SELECT * FROM participaciones")
    suspend fun obtenerTodas(): List<Participacion>

    @Query("SELECT * FROM participaciones WHERE personaId = :personaId")
    suspend fun obtenerPorPersona(personaId: Int): List<Participacion>

    @Query("""
        UPDATE participaciones
        SET montoAsignado = :monto
        WHERE personaId = :personaId
        AND actividadId = :actividadId
    """)
    suspend fun actualizarMonto(personaId: Int, actividadId: Int, monto: Double)
}
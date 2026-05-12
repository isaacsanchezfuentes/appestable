package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ParticipacionDao {
    @Insert
    suspend fun insertar(participacion: Participacion)

    @Query("SELECT * FROM participaciones WHERE actividadId = :actividadId")
    suspend fun obtenerParticipantes(actividadId: Int): List<Participacion>

    @Query("SELECT * FROM participaciones")
    suspend fun obtenerTodas(): List<Participacion>

    @Query("SELECT * FROM participaciones WHERE personaId = :personaId")
    suspend fun obtenerPorPersona(personaId: Int): List<Participacion>
}

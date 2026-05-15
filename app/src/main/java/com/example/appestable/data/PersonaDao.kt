package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PersonaDao {

    @Insert
    suspend fun insertar(persona: Persona): Long

    @Query("SELECT * FROM personas")
    suspend fun obtenerTodos(): List<Persona>

    @Delete
    suspend fun eliminar(persona: Persona)

    @Query("""
        SELECT * FROM personas
        WHERE familiaId = :familiaId
        AND esJefe = 1
        LIMIT 1
    """)
    suspend fun obtenerJefePorFamilia(
        familiaId: Int
    ): Persona?
}
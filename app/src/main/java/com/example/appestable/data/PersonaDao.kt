package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface PersonaDao {
    @Insert
    suspend fun insertar(persona: Persona): Long

    @Query("SELECT * FROM personas")
    suspend fun obtenerTodos(): List<Persona>

    @Delete
    suspend fun eliminar(persona: Persona)
}

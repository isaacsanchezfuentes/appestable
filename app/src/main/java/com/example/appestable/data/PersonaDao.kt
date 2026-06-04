package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PersonaDao {

    @Insert
    suspend fun insertar(persona: Persona): Long

    @Query("SELECT * FROM personas ORDER BY familiaId ASC, esJefe DESC, nombre COLLATE NOCASE ASC")
    suspend fun obtenerTodos(): List<Persona>

    @Query("SELECT * FROM personas WHERE email = :email LIMIT 1")
    suspend fun obtenerPorEmail(email: String): Persona?

    @Query("SELECT * FROM personas WHERE nombre = :nombre AND familiaId = :familiaId LIMIT 1")
    suspend fun obtenerPorNombreYFamilia(nombre: String, familiaId: Int): Persona?

    @Query("UPDATE personas SET backendId = :backendId WHERE id = :personaId")
    suspend fun actualizarBackendId(personaId: Int, backendId: Int)

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
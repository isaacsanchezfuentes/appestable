package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ViajeDao {
    @Insert
    suspend fun insertar(viaje: Viaje): Long

    @Update
    suspend fun actualizar(viaje: Viaje)

    @Query("SELECT * FROM viajes ORDER BY nombre COLLATE NOCASE ASC")
    suspend fun obtenerTodos(): List<Viaje>

    @Query("SELECT * FROM viajes WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): Viaje?

    @Query("SELECT * FROM viajes WHERE backendId = :backendId LIMIT 1")
    suspend fun obtenerPorBackendId(backendId: Int): Viaje?

    @Query("UPDATE viajes SET backendId = :backendId WHERE id = :viajeId")
    suspend fun actualizarBackendId(viajeId: Int, backendId: Int)
}
package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ActividadDao {
    @Insert
    suspend fun insertar(actividad: Actividad): Long

    @Update
    suspend fun actualizar(actividad: Actividad)

    @Query("SELECT * FROM actividades WHERE viajeId = :viajeId ORDER BY fecha DESC, nombre COLLATE NOCASE ASC")
    suspend fun obtenerPorViaje(viajeId: Int): List<Actividad>

    @Query("SELECT * FROM actividades")
    suspend fun obtenerTodas(): List<Actividad>

    @Query("SELECT * FROM actividades WHERE backendId = :backendId AND viajeId = :viajeId LIMIT 1")
    suspend fun obtenerPorBackendIdYViaje(backendId: Int, viajeId: Int): Actividad?

    @Query("UPDATE actividades SET backendId = :backendId WHERE id = :actividadId")
    suspend fun actualizarBackendId(actividadId: Int, backendId: Int)
}
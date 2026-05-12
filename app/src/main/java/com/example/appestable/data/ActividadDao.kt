package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ActividadDao {
    @Insert
    suspend fun insertar(actividad: Actividad): Long

    @Query("SELECT * FROM actividades")
    suspend fun obtenerTodas(): List<Actividad>
}

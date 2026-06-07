package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FamiliaDao {
    @Insert
    suspend fun insertar(familia: Familia): Long

    @Query("SELECT * FROM familias WHERE viajeId = :viajeId ORDER BY nombreFamilia COLLATE NOCASE ASC")
    suspend fun obtenerPorViaje(viajeId: Int): List<Familia>

    @Query("SELECT * FROM familias ORDER BY nombreFamilia COLLATE NOCASE ASC")
    suspend fun obtenerTodas(): List<Familia>

    @Query("SELECT * FROM familias WHERE nombreFamilia = :nombre LIMIT 1")
    suspend fun obtenerPorNombre(nombre: String): Familia?

    @Query("SELECT * FROM familias WHERE viajeId = :viajeId AND nombreFamilia = :nombre LIMIT 1")
    suspend fun obtenerPorNombreYViaje(viajeId: Int, nombre: String): Familia?

    @Query("SELECT * FROM familias WHERE backendId = :backendId AND viajeId = :viajeId LIMIT 1")
    suspend fun obtenerPorBackendIdYViaje(backendId: Int, viajeId: Int): Familia?

    @Update
    suspend fun actualizar(familia: Familia)
}
package com.example.appestable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MembresiaViajeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(membresia: MembresiaViaje): Long

    @Query("SELECT * FROM membresias_viaje WHERE viajeId = :viajeId AND usuarioId = :usuarioId LIMIT 1")
    suspend fun obtenerPorViajeYUsuario(viajeId: Int, usuarioId: Int): MembresiaViaje?

    @Query("SELECT * FROM membresias_viaje WHERE usuarioId = :usuarioId")
    suspend fun obtenerPorUsuario(usuarioId: Int): List<MembresiaViaje>
}
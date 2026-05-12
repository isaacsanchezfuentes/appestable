package com.example.appestable.data

import androidx.room.*

@Dao
interface OrganizadorDao {
    @Insert suspend fun insertar(organizador: Organizador): Long
    @Query("SELECT * FROM organizadores") suspend fun obtenerTodos(): List<Organizador>
}

@Dao
interface FamiliaDao {
    @Insert suspend fun insertar(familia: Familia): Long
    @Query("SELECT * FROM familias") suspend fun obtenerTodas(): List<Familia>
}



@Dao
interface GastoDao {
    @Insert suspend fun insertar(gasto: Gasto)
    @Query("SELECT * FROM gastos WHERE personaId = :personaId")
    suspend fun obtenerPorPersona(personaId: Int): List<Gasto>
}


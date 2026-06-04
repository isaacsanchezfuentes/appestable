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
    @Query("SELECT * FROM familias ORDER BY nombreFamilia COLLATE NOCASE ASC") suspend fun obtenerTodas(): List<Familia>
    @Query("SELECT * FROM familias WHERE nombreFamilia = :nombre LIMIT 1") suspend fun obtenerPorNombre(nombre: String): Familia?
}



@Dao
interface GastoDao {
    @Insert suspend fun insertar(gasto: Gasto)
    @Query("SELECT * FROM gastos WHERE personaId = :personaId")
    suspend fun obtenerPorPersona(personaId: Int): List<Gasto>
}


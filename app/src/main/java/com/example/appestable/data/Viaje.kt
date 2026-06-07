package com.example.appestable.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "viajes")
data class Viaje(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val estado: String = "ACTIVO",
    val organizadorUsuarioId: Int? = null,
    val backendId: Int? = null
)
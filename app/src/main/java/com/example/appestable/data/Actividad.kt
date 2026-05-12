package com.example.appestable.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actividades")
data class Actividad(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val fecha: String,
    val costoTotal: Double
)

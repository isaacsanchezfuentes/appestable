package com.example.appestable.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "actividades",
    foreignKeys = [
        ForeignKey(
            entity = Viaje::class,
            parentColumns = ["id"],
            childColumns = ["viajeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("viajeId")]
)
data class Actividad(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val fecha: String,
    val costoTotal: Double,
    val viajeId: Int = 1,
    val creadoPorUsuarioId: Int? = null,
    val backendId: Int? = null
)
package com.example.appestable.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personas",
    foreignKeys = [
        ForeignKey(
            entity = Familia::class,
            parentColumns = ["id"],
            childColumns = ["familiaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Viaje::class,
            parentColumns = ["id"],
            childColumns = ["viajeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("familiaId"), Index("viajeId")]
)
data class Persona(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val celular: String,
    val email: String,
    val familiaId: Int,
    val esJefe: Boolean,
    val backendId: Int? = null,
    val viajeId: Int = 1,
    val usuarioId: Int? = null,
    val rol: RolViaje = RolViaje.MIEMBRO
)
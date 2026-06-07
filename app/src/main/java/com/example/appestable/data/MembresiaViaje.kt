package com.example.appestable.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "membresias_viaje",
    foreignKeys = [
        ForeignKey(
            entity = Viaje::class,
            parentColumns = ["id"],
            childColumns = ["viajeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Familia::class,
            parentColumns = ["id"],
            childColumns = ["familiaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("viajeId"),
        Index("usuarioId"),
        Index(value = ["viajeId", "usuarioId"], unique = true)
    ]
)
data class MembresiaViaje(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val viajeId: Int,
    val usuarioId: Int,
    val familiaId: Int? = null,
    val rol: RolViaje
)
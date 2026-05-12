package com.example.appestable.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "gastos",
    foreignKeys = [
        ForeignKey(entity = Persona::class, parentColumns = ["id"], childColumns = ["personaId"]),
        ForeignKey(
            entity = Actividad::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"]
        )
    ]
)
data class Gasto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personaId: Int,
    val actividadId: Int,
    val monto: Double
)
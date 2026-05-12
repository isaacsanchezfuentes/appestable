package com.example.appestable.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "participaciones",
    primaryKeys = ["personaId", "actividadId"],
    foreignKeys = [
        ForeignKey(entity = Persona::class, parentColumns = ["id"], childColumns = ["personaId"]),
        ForeignKey(entity = Actividad::class, parentColumns = ["id"], childColumns = ["actividadId"])
    ]
)
data class Participacion(
    val personaId: Int,
    val actividadId: Int
)



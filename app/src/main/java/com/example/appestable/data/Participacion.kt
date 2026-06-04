package com.example.appestable.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "participaciones",
    primaryKeys = ["personaId", "actividadId"],
    foreignKeys = [
        ForeignKey(
            entity = Persona::class,
            parentColumns = ["id"],
            childColumns = ["personaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Actividad::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("personaId"),
        Index("actividadId")
    ]
)
data class Participacion(
    val personaId: Int,
    val actividadId: Int,
    @ColumnInfo(defaultValue = "0.0")
    val montoAsignado: Double = 0.0
)
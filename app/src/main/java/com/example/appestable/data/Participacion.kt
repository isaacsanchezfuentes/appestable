package com.example.appestable.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "participaciones",
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
        Index("actividadId"),
        Index(value = ["personaId", "actividadId"], unique = true)
    ]
)
data class Participacion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personaId: Int,
    val actividadId: Int,
    @ColumnInfo(defaultValue = "0.0")
    val montoAsignado: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val pagado: Boolean = false,
    val backendId: Int? = null
)
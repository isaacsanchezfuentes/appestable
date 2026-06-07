package com.example.appestable.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "familias",
    foreignKeys = [
        ForeignKey(
            entity = Viaje::class,
            parentColumns = ["id"],
            childColumns = ["viajeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["viajeId", "nombreFamilia"], unique = true)]
)
data class Familia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreFamilia: String,
    val viajeId: Int = 1,
    val backendId: Int? = null
)
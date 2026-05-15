package com.example.appestable.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personas")
data class Persona(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,
    val celular: String,
    val email: String,
    val familiaId: Int,
    val esJefe: Boolean
)
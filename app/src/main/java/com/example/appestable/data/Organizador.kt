package com.example.appestable.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "organizadores")
data class Organizador(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val email: String,
    val preferencias: String
)

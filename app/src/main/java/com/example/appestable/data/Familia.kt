package com.example.appestable.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "familias")
data class Familia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreFamilia: String
)

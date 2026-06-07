package com.example.appestable.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRolViaje(rol: RolViaje): String = rol.name

    @TypeConverter
    fun toRolViaje(value: String): RolViaje = RolViaje.valueOf(value)
}
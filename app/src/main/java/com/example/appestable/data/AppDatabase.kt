package com.example.appestable.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Viaje::class,
        Usuario::class,
        MembresiaViaje::class,
        Familia::class,
        Persona::class,
        Actividad::class,
        Participacion::class
    ],
    version = 22
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun viajeDao(): ViajeDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun membresiaViajeDao(): MembresiaViajeDao
    abstract fun familiaDao(): FamiliaDao
    abstract fun personaDao(): PersonaDao
    abstract fun actividadDao(): ActividadDao
    abstract fun participacionDao(): ParticipacionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travelmanager_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
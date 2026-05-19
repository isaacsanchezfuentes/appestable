package com.example.appestable.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Organizador::class, Familia::class, Persona::class, Actividad::class, Participacion::class, Gasto::class],
    version = 17
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun organizadorDao(): OrganizadorDao
    abstract fun familiaDao(): FamiliaDao
    abstract fun personaDao(): PersonaDao
    abstract fun actividadDao(): ActividadDao
    abstract fun participacionDao(): ParticipacionDao
    abstract fun gastoDao(): GastoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travelmanager_db"
                )
                    .fallbackToDestructiveMigration() // ⚡ Esta línea evita crashes por migración en desarrollo
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

package com.example.appestable.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                auth0Id TEXT NOT NULL,
                email TEXT NOT NULL,
                nombre TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_auth0Id ON usuarios(auth0Id)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS viajes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                descripcion TEXT NOT NULL DEFAULT '',
                fechaInicio TEXT NOT NULL DEFAULT '',
                fechaFin TEXT NOT NULL DEFAULT '',
                estado TEXT NOT NULL DEFAULT 'ACTIVO',
                organizadorUsuarioId INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO viajes (id, nombre, descripcion, estado)
            VALUES (1, 'Viaje Principal', 'Migrado automáticamente', 'ACTIVO')
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS familias_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombreFamilia TEXT NOT NULL,
                viajeId INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO familias_new (id, nombreFamilia, viajeId)
            SELECT id, nombreFamilia, 1 FROM familias
        """.trimIndent())
        db.execSQL("DROP TABLE familias")
        db.execSQL("ALTER TABLE familias_new RENAME TO familias")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_familias_viajeId_nombreFamilia ON familias(viajeId, nombreFamilia)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS personas_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                celular TEXT NOT NULL,
                email TEXT NOT NULL,
                familiaId INTEGER NOT NULL,
                esJefe INTEGER NOT NULL,
                backendId INTEGER,
                viajeId INTEGER NOT NULL DEFAULT 1,
                usuarioId INTEGER,
                rol TEXT NOT NULL DEFAULT 'MIEMBRO'
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO personas_new (id, nombre, celular, email, familiaId, esJefe, backendId, viajeId, rol)
            SELECT id, nombre, celular, email, familiaId, esJefe, backendId, 1,
                   CASE WHEN esJefe = 1 THEN 'JEFE_FAMILIA' ELSE 'MIEMBRO' END
            FROM personas
        """.trimIndent())
        db.execSQL("DROP TABLE personas")
        db.execSQL("ALTER TABLE personas_new RENAME TO personas")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_personas_familiaId ON personas(familiaId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_personas_viajeId ON personas(viajeId)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS actividades_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                fecha TEXT NOT NULL,
                costoTotal REAL NOT NULL,
                viajeId INTEGER NOT NULL DEFAULT 1,
                creadoPorUsuarioId INTEGER,
                backendId INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO actividades_new (id, nombre, fecha, costoTotal, viajeId)
            SELECT id, nombre, fecha, costoTotal, 1 FROM actividades
        """.trimIndent())
        db.execSQL("DROP TABLE actividades")
        db.execSQL("ALTER TABLE actividades_new RENAME TO actividades")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_actividades_viajeId ON actividades(viajeId)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS participaciones_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                personaId INTEGER NOT NULL,
                actividadId INTEGER NOT NULL,
                montoAsignado REAL NOT NULL DEFAULT 0.0,
                pagado INTEGER NOT NULL DEFAULT 0,
                backendId INTEGER,
                FOREIGN KEY(personaId) REFERENCES personas(id) ON DELETE CASCADE,
                FOREIGN KEY(actividadId) REFERENCES actividades(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO participaciones_new (personaId, actividadId, montoAsignado, pagado)
            SELECT personaId, actividadId, montoAsignado, 0 FROM participaciones
        """.trimIndent())
        db.execSQL("DROP TABLE participaciones")
        db.execSQL("ALTER TABLE participaciones_new RENAME TO participaciones")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_participaciones_personaId ON participaciones(personaId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_participaciones_actividadId ON participaciones(actividadId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_participaciones_personaId_actividadId ON participaciones(personaId, actividadId)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS membresias_viaje (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                viajeId INTEGER NOT NULL,
                usuarioId INTEGER NOT NULL,
                familiaId INTEGER,
                rol TEXT NOT NULL,
                FOREIGN KEY(viajeId) REFERENCES viajes(id) ON DELETE CASCADE,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY(familiaId) REFERENCES familias(id) ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_membresias_viaje_viajeId ON membresias_viaje(viajeId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_membresias_viaje_usuarioId ON membresias_viaje(usuarioId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_membresias_viaje_viajeId_usuarioId ON membresias_viaje(viajeId, usuarioId)")

        db.execSQL("DROP TABLE IF EXISTS organizadores")
        db.execSQL("DROP TABLE IF EXISTS gastos")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Recrear la tabla familias para incluir la Foreign Key y corregir el default de viajeId
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS familias_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombreFamilia TEXT NOT NULL,
                viajeId INTEGER NOT NULL,
                backendId INTEGER,
                FOREIGN KEY(viajeId) REFERENCES viajes(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO familias_new (id, nombreFamilia, viajeId)
            SELECT id, nombreFamilia, viajeId FROM familias
        """)

        db.execSQL("DROP TABLE familias")
        db.execSQL("ALTER TABLE familias_new RENAME TO familias")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_familias_viajeId_nombreFamilia ON familias(viajeId, nombreFamilia)")

        // 2. Actualizar viajes
        db.execSQL("ALTER TABLE viajes ADD COLUMN backendId INTEGER")
        db.execSQL("UPDATE viajes SET backendId = id WHERE backendId IS NULL")
    }
}
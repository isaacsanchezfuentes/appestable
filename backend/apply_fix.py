import psycopg2
import traceback

def fix():
    try:
        conn = psycopg2.connect(dbname='appestable', user='postgres', password='postgres123', host='localhost', port=5432)
        cur = conn.cursor()
        
        print("Sincronizando estructura de base de datos...")

        # 0. Asegurar que exista al menos el viaje con ID 1
        cur.execute("INSERT INTO viajes (id, nombre, descripcion) VALUES (1, 'Viaje Principal', 'Auto-creado para migración') ON CONFLICT (id) DO NOTHING;")
        conn.commit()

        # Función auxiliar para agregar columna si no existe
        def add_col(table, col, definition):
            try:
                cur.execute(f"ALTER TABLE {table} ADD COLUMN {col} {definition};")
                print(f"OK: Columna {col} agregada a {table}")
                conn.commit()
            except Exception as e:
                conn.rollback()
                if "ya existe" in str(e):
                    print(f"INFO: {col} en {table} ya existe")
                else:
                    print(f"ERROR: {col} en {table}: {e}")

        add_col("familias", "viaje_id", "INTEGER REFERENCES viajes(id) DEFAULT 1")
        add_col("personas", "viaje_id", "INTEGER REFERENCES viajes(id) DEFAULT 1")
        add_col("personas", "usuario_id", "INTEGER REFERENCES usuarios(id)")
        add_col("personas", "rol", "VARCHAR DEFAULT 'MIEMBRO'")
        add_col("actividades", "viaje_id", "INTEGER REFERENCES viajes(id) DEFAULT 1")
        add_col("actividades", "creado_por_usuario_id", "INTEGER REFERENCES usuarios(id)")

        print("\n¡Estructura de DB corregida exitosamente!")
        cur.close()
        conn.close()
    except Exception:
        traceback.print_exc()

if __name__ == "__main__":
    fix()

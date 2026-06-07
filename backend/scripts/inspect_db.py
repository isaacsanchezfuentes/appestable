import psycopg2

conn = psycopg2.connect("postgresql://postgres:postgres123@localhost:5432/appestable")
cur = conn.cursor()

tables = [
    "familias",
    "personas",
    "actividades",
    "viajes",
    "usuarios",
    "membresias_viaje",
    "alembic_version",
]
for table in tables:
    cur.execute(
        "SELECT column_name FROM information_schema.columns "
        "WHERE table_name=%s ORDER BY ordinal_position",
        (table,),
    )
    cols = [r[0] for r in cur.fetchall()]
    print(f"{table}: {', '.join(cols) if cols else 'NO TABLE'}")

cur.execute("SELECT version_num FROM alembic_version")
print("alembic:", cur.fetchall())
conn.close()
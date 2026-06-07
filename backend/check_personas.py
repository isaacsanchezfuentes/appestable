import psycopg2
import traceback

def check():
    try:
        conn = psycopg2.connect(dbname='appestable', user='postgres', password='postgres123', host='localhost', port=5432)
        cur = conn.cursor()
        
        print("--- ÚLTIMAS 10 PERSONAS ---")
        cur.execute('SELECT id, nombre, email, familia_id, viaje_id, is_deleted FROM personas ORDER BY id DESC LIMIT 10;')
        rows = cur.fetchall()
        for row in rows:
            print(row)
            
        print("\n--- ÚLTIMAS 5 FAMILIAS ---")
        cur.execute('SELECT id, nombre_familia, viaje_id FROM familias ORDER BY id DESC LIMIT 5;')
        rows = cur.fetchall()
        for row in rows:
            print(row)
            
        cur.close()
        conn.close()
    except Exception:
        traceback.print_exc()

if __name__ == "__main__":
    check()

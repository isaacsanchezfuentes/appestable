import psycopg2
import traceback
try:
    conn = psycopg2.connect(dbname='appestable', user='postgres', password='postgres123', host='localhost', port=5432)
    cur = conn.cursor()
    cur.execute('SELECT version();')
    print(cur.fetchone())
    cur.close()
    conn.close()
except Exception:
    traceback.print_exc()

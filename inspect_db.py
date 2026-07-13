import sqlite3

conn = sqlite3.connect('C:/Users/ASUS/AndroidStudioProjects/SmartGov/smartgov_local.db')
cursor = conn.cursor()

print("--- TABLE DOCUMENTOS ---")
try:
    cursor.execute("SELECT id_documento, nro_documento_unico, sync_state FROM documentos_ingresados")
    for row in cursor.fetchall():
        print(row)
except Exception as e:
    print(e)

print("\n--- TABLE DERIVACIONES ---")
try:
    cursor.execute("SELECT id_derivacion, codigo_barras_seguimiento, id_documento, sync_state FROM hojas_ruta_derivaciones")
    for row in cursor.fetchall():
        print(row)
except Exception as e:
    print(e)

conn.close()

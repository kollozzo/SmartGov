package com.example.SmartGov.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smartgov_local.db";
    private static final int DATABASE_VERSION = 1;

    // Nombre de las tablas
    public static final String TABLE_OFICINAS = "oficinas";
    public static final String TABLE_TIPOS_DOCUMENTOS = "tipos_documentos";
    public static final String TABLE_ADMINISTRADOS = "administrados";
    public static final String TABLE_DIRECCIONES = "administrado_direcciones";
    public static final String TABLE_PERSONAL = "personal_especialistas";
    public static final String TABLE_EXPEDIENTES = "expedientes_generales";
    public static final String TABLE_DOCUMENTOS = "documentos_ingresados";
    public static final String TABLE_DERIVACIONES = "hojas_ruta_derivaciones";
    public static final String TABLE_ARCHIVO_FISICO = "archivo_fisico_central";
    public static final String TABLE_ACTAS = "actas_archivamiento";

    // Columna de estado de sincronización (0 = Sincronizado, 1 = Pendiente de Sincronizar, 2 = Conflicto)
    public static final String COLUMN_SYNC_STATE = "sync_state";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Oficinas
        db.execSQL("CREATE TABLE " + TABLE_OFICINAS + " (" +
                "id_oficina INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codigo_oficina TEXT NOT NULL, " +
                "siglas_oficiales TEXT NOT NULL, " +
                "nombre_unidad TEXT NOT NULL)");

        // 2. Tipos de Documentos
        db.execSQL("CREATE TABLE " + TABLE_TIPOS_DOCUMENTOS + " (" +
                "id_tipo_documento INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_tipo_documento TEXT NOT NULL)");

        // 3. Administrados
        db.execSQL("CREATE TABLE " + TABLE_ADMINISTRADOS + " (" +
                "id_administrado INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codigo_administrado TEXT NOT NULL, " +
                "dni_ruc TEXT NOT NULL, " +
                "nombre_razon_social TEXT NOT NULL, " +
                "telefono TEXT, " +
                "correo_notificaciones TEXT, " +
                COLUMN_SYNC_STATE + " INTEGER DEFAULT 0)");

        // 4. Administrado Direcciones
        db.execSQL("CREATE TABLE " + TABLE_DIRECCIONES + " (" +
                "id_direccion INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_administrado INTEGER NOT NULL, " +
                "tipo_inmueble TEXT, " +
                "calle TEXT, " +
                "numero TEXT, " +
                "comuna_distrito TEXT, " +
                "ciudad TEXT, " +
                COLUMN_SYNC_STATE + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(id_administrado) REFERENCES " + TABLE_ADMINISTRADOS + "(id_administrado) ON DELETE CASCADE)");

        // 5. Personal Especialistas
        db.execSQL("CREATE TABLE " + TABLE_PERSONAL + " (" +
                "id_empleado INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codigo_empleado TEXT NOT NULL, " +
                "nombre_completo TEXT NOT NULL, " +
                "cargo TEXT NOT NULL, " +
                "id_oficina INTEGER NOT NULL, " +
                "FOREIGN KEY(id_oficina) REFERENCES " + TABLE_OFICINAS + "(id_oficina))");

        // 6. Expedientes Generales
        db.execSQL("CREATE TABLE " + TABLE_EXPEDIENTES + " (" +
                "id_expediente INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nro_expediente_anual TEXT NOT NULL, " +
                "fecha_hora_apertura TEXT NOT NULL, " +
                "asunto_general TEXT, " +
                "estado_global TEXT NOT NULL, " +
                COLUMN_SYNC_STATE + " INTEGER DEFAULT 0)");

        // 7. Documentos Ingresados
        db.execSQL("CREATE TABLE " + TABLE_DOCUMENTOS + " (" +
                "id_documento INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nro_documento_unico TEXT NOT NULL, " +
                "id_expediente INTEGER NOT NULL, " +
                "id_tipo_documento INTEGER NOT NULL, " +
                "id_administrado INTEGER NOT NULL, " +
                "cantidad_folios INTEGER NOT NULL, " +
                "fecha_hora_recepcion TEXT NOT NULL, " +
                COLUMN_SYNC_STATE + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(id_expediente) REFERENCES " + TABLE_EXPEDIENTES + "(id_expediente), " +
                "FOREIGN KEY(id_tipo_documento) REFERENCES " + TABLE_TIPOS_DOCUMENTOS + "(id_tipo_documento), " +
                "FOREIGN KEY(id_administrado) REFERENCES " + TABLE_ADMINISTRADOS + "(id_administrado))");

        // 8. Hojas Ruta Derivaciones (Con Geolocalización)
        db.execSQL("CREATE TABLE " + TABLE_DERIVACIONES + " (" +
                "id_derivacion INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codigo_barras_seguimiento TEXT NOT NULL, " +
                "id_documento INTEGER NOT NULL, " +
                "id_empleado_assigned INTEGER NOT NULL, " +
                "id_oficina_procedencia INTEGER NOT NULL, " +
                "fecha_hora_despacho TEXT NOT NULL, " +
                "prioridad_envio TEXT, " +
                "fecha_hora_recepcion TEXT, " +
                "observaciones_receptor TEXT, " +
                "estado_derivacion TEXT, " +
                "latitud REAL, " + // Geolocalización
                "longitud REAL, " + // Geolocalización
                COLUMN_SYNC_STATE + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(id_documento) REFERENCES " + TABLE_DOCUMENTOS + "(id_documento), " +
                "FOREIGN KEY(id_empleado_assigned) REFERENCES " + TABLE_PERSONAL + "(id_empleado), " +
                "FOREIGN KEY(id_oficina_procedencia) REFERENCES " + TABLE_OFICINAS + "(id_oficina))");

        // 9. Archivo Fisico Central
        db.execSQL("CREATE TABLE " + TABLE_ARCHIVO_FISICO + " (" +
                "id_ubicacion INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codigo_almacen TEXT NOT NULL, " +
                "nro_pabellon INTEGER NOT NULL, " +
                "nro_estante INTEGER NOT NULL, " +
                "nro_caja_fisica INTEGER NOT NULL)");

        // 10. Actas Archivamiento (Con Foto Multimedia)
        db.execSQL("CREATE TABLE " + TABLE_ACTAS + " (" +
                "id_acta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nro_acta_unico TEXT NOT NULL, " +
                "id_derivacion INTEGER NOT NULL, " +
                "id_ubicacion_archivo INTEGER NOT NULL, " +
                "fecha_hora_guardado TEXT NOT NULL, " +
                "costo_digitalizacion REAL, " +
                "costo_arancel_custodia REAL, " +
                "costo_final_procesamiento REAL, " +
                "foto_ruta TEXT, " + // Evidencia Multimedia (Ruta de archivo de foto)
                COLUMN_SYNC_STATE + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(id_derivacion) REFERENCES " + TABLE_DERIVACIONES + "(id_derivacion), " +
                "FOREIGN KEY(id_ubicacion_archivo) REFERENCES " + TABLE_ARCHIVO_FISICO + "(id_ubicacion))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARCHIVO_FISICO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DERIVACIONES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOCUMENTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPEDIENTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERSONAL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DIRECCIONES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMINISTRADOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIPOS_DOCUMENTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFICINAS);
        onCreate(db);
    }
}

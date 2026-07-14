package com.example.smartgov.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.smartgov.database.DatabaseHelper;
import com.example.smartgov.network.ApiService;
import com.example.smartgov.network.RetrofitClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.smartgov.utils.SessionManager;
import com.example.smartgov.ui.LoginActivity;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final ApiService apiService;

    public SyncManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.apiService = RetrofitClient.getClient(context).create(ApiService.class);
    }

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // ----------------------------------------------------------------
    // OPERACIÓN PUSH: Subir datos creados localmente sin conexión
    // ----------------------------------------------------------------
    public void pushLocalChanges(SyncCallback callback) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        JsonObject pushBody = new JsonObject();

        // 1. Obtener Expedientes locales no sincronizados (sync_state = 1)
        JsonArray expedientesArray = new JsonArray();
        Cursor cursorExp = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_EXPEDIENTES + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorExp.moveToNext()) {
            JsonObject item = new JsonObject();
            // Enviamos un ID negativo/cero para indicar al backend que es nuevo
            item.addProperty("idExpediente", cursorExp.getInt(cursorExp.getColumnIndexOrThrow("id_expediente")));
            item.addProperty("nroExpedienteAnual", cursorExp.getString(cursorExp.getColumnIndexOrThrow("nro_expediente_anual")));
            item.addProperty("fechaHoraApertura", cursorExp.getString(cursorExp.getColumnIndexOrThrow("fecha_hora_apertura")));
            item.addProperty("asuntoGeneral", cursorExp.getString(cursorExp.getColumnIndexOrThrow("asunto_general")));
            item.addProperty("estadoGlobal", cursorExp.getString(cursorExp.getColumnIndexOrThrow("estado_global")));
            expedientesArray.add(item);
        }
        cursorExp.close();
        if (expedientesArray.size() > 0) {
            pushBody.add("expedientes", expedientesArray);
        }

        // 2. Obtener Documentos locales no sincronizados
        JsonArray documentosArray = new JsonArray();
        Cursor cursorDoc = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DOCUMENTOS + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorDoc.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idDocumento", cursorDoc.getInt(cursorDoc.getColumnIndexOrThrow("id_documento")));
            item.addProperty("nroDocumentoUnico", cursorDoc.getString(cursorDoc.getColumnIndexOrThrow("nro_documento_unico")));
            item.addProperty("idExpediente", cursorDoc.getInt(cursorDoc.getColumnIndexOrThrow("id_expediente")));
            item.addProperty("idTipoDocumento", cursorDoc.getInt(cursorDoc.getColumnIndexOrThrow("id_tipo_documento")));
            item.addProperty("idAdministrado", cursorDoc.getInt(cursorDoc.getColumnIndexOrThrow("id_administrado")));
            item.addProperty("cantidadFolios", cursorDoc.getInt(cursorDoc.getColumnIndexOrThrow("cantidad_folios")));
            item.addProperty("fechaHoraRecepcion", cursorDoc.getString(cursorDoc.getColumnIndexOrThrow("fecha_hora_recepcion")));
            documentosArray.add(item);
        }
        cursorDoc.close();
        if (documentosArray.size() > 0) {
            pushBody.add("documentos", documentosArray);
        }

        // 3. Obtener Derivaciones locales no sincronizadas
        JsonArray derivacionesArray = new JsonArray();
        Cursor cursorDer = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DERIVACIONES + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorDer.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idDerivacion", cursorDer.getInt(cursorDer.getColumnIndexOrThrow("id_derivacion")));
            item.addProperty("codigoBarrasSeguimiento", cursorDer.getString(cursorDer.getColumnIndexOrThrow("codigo_barras_seguimiento")));
            item.addProperty("idDocumento", cursorDer.getInt(cursorDer.getColumnIndexOrThrow("id_documento")));
            item.addProperty("idEmpleadoAsignado", cursorDer.getInt(cursorDer.getColumnIndexOrThrow("id_empleado_assigned")));
            item.addProperty("idOficinaProcedencia", cursorDer.getInt(cursorDer.getColumnIndexOrThrow("id_oficina_procedencia")));
            item.addProperty("fechaHoraDespacho", cursorDer.getString(cursorDer.getColumnIndexOrThrow("fecha_hora_despacho")));
            if (!cursorDer.isNull(cursorDer.getColumnIndexOrThrow("prioridad_envio"))) {
                item.addProperty("prioridadEnvio", cursorDer.getString(cursorDer.getColumnIndexOrThrow("prioridad_envio")));
            }
            if (!cursorDer.isNull(cursorDer.getColumnIndexOrThrow("fecha_hora_recepcion"))) {
                item.addProperty("fechaHoraRecepcion", cursorDer.getString(cursorDer.getColumnIndexOrThrow("fecha_hora_recepcion")));
            }
            if (!cursorDer.isNull(cursorDer.getColumnIndexOrThrow("observaciones_receptor"))) {
                item.addProperty("observacionesReceptor", cursorDer.getString(cursorDer.getColumnIndexOrThrow("observaciones_receptor")));
            }
            if (!cursorDer.isNull(cursorDer.getColumnIndexOrThrow("estado_derivacion"))) {
                item.addProperty("estadoDerivacion", cursorDer.getString(cursorDer.getColumnIndexOrThrow("estado_derivacion")));
            }
            derivacionesArray.add(item);
        }
        cursorDer.close();
        if (derivacionesArray.size() > 0) {
            pushBody.add("derivaciones", derivacionesArray);
        }

        // 4. Obtener Actas locales no sincronizadas
        JsonArray actasArray = new JsonArray();
        Cursor cursorAct = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_ACTAS + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorAct.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idActa", cursorAct.getInt(cursorAct.getColumnIndexOrThrow("id_acta")));
            item.addProperty("nroActaUnico", cursorAct.getString(cursorAct.getColumnIndexOrThrow("nro_acta_unico")));
            item.addProperty("idDerivacion", cursorAct.getInt(cursorAct.getColumnIndexOrThrow("id_derivacion")));
            item.addProperty("idUbicacionArchivo", cursorAct.getInt(cursorAct.getColumnIndexOrThrow("id_ubicacion_archivo")));
            item.addProperty("fechaHoraGuardado", cursorAct.getString(cursorAct.getColumnIndexOrThrow("fecha_hora_guardado")));
            item.addProperty("costoDigitalizacion", cursorAct.getDouble(cursorAct.getColumnIndexOrThrow("costo_digitalizacion")));
            item.addProperty("costoArancelCustodia", cursorAct.getDouble(cursorAct.getColumnIndexOrThrow("costo_arancel_custodia")));
            item.addProperty("costoFinalProcesamiento", cursorAct.getDouble(cursorAct.getColumnIndexOrThrow("costo_final_procesamiento")));
            actasArray.add(item);
        }
        cursorAct.close();
        if (actasArray.size() > 0) {
            pushBody.add("actas", actasArray);
        }

        // 5. Obtener Administrados locales no sincronizados
        JsonArray admArray = new JsonArray();
        Cursor cursorAdm = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_ADMINISTRADOS + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorAdm.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idAdministrado", cursorAdm.getInt(cursorAdm.getColumnIndexOrThrow("id_administrado")));
            item.addProperty("codigoAdministrado", cursorAdm.getString(cursorAdm.getColumnIndexOrThrow("codigo_administrado")));
            item.addProperty("dniRuc", cursorAdm.getString(cursorAdm.getColumnIndexOrThrow("dni_ruc")));
            item.addProperty("nombreRazonSocial", cursorAdm.getString(cursorAdm.getColumnIndexOrThrow("nombre_razon_social")));
            if (!cursorAdm.isNull(cursorAdm.getColumnIndexOrThrow("telefono"))) {
                item.addProperty("telefono", cursorAdm.getString(cursorAdm.getColumnIndexOrThrow("telefono")));
            }
            if (!cursorAdm.isNull(cursorAdm.getColumnIndexOrThrow("correo_notificaciones"))) {
                item.addProperty("correoNotificaciones", cursorAdm.getString(cursorAdm.getColumnIndexOrThrow("correo_notificaciones")));
            }
            admArray.add(item);
        }
        cursorAdm.close();
        if (admArray.size() > 0) {
            pushBody.add("administrados", admArray);
        }

        // 6. Oficinas
        JsonArray oficinasArray = new JsonArray();
        Cursor cursorOfi = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_OFICINAS + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorOfi.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idOficina", cursorOfi.getInt(cursorOfi.getColumnIndexOrThrow("id_oficina")));
            item.addProperty("codigoOficina", cursorOfi.getString(cursorOfi.getColumnIndexOrThrow("codigo_oficina")));
            item.addProperty("siglasOficiales", cursorOfi.getString(cursorOfi.getColumnIndexOrThrow("siglas_oficiales")));
            item.addProperty("nombreUnidad", cursorOfi.getString(cursorOfi.getColumnIndexOrThrow("nombre_unidad")));
            oficinasArray.add(item);
        }
        cursorOfi.close();
        if (oficinasArray.size() > 0) {
            pushBody.add("oficinas", oficinasArray);
        }

        // 7. Tipos Documentos
        JsonArray tiposDocArray = new JsonArray();
        Cursor cursorTd = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_TIPOS_DOCUMENTOS + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorTd.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idTipoDocumento", cursorTd.getInt(cursorTd.getColumnIndexOrThrow("id_tipo_documento")));
            item.addProperty("nombreTipoDocumento", cursorTd.getString(cursorTd.getColumnIndexOrThrow("nombre_tipo_documento")));
            tiposDocArray.add(item);
        }
        cursorTd.close();
        if (tiposDocArray.size() > 0) {
            pushBody.add("tiposDocumentos", tiposDocArray);
        }

        // 8. Personal (usa DTO PersonalEspecialistaRequest con idEmpleado)
        JsonArray personalArray = new JsonArray();
        Cursor cursorPer = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PERSONAL + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorPer.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idEmpleado", cursorPer.getInt(cursorPer.getColumnIndexOrThrow("id_empleado")));
            item.addProperty("codigoEmpleado", cursorPer.getString(cursorPer.getColumnIndexOrThrow("codigo_empleado")));
            item.addProperty("nombreCompleto", cursorPer.getString(cursorPer.getColumnIndexOrThrow("nombre_completo")));
            item.addProperty("cargo", cursorPer.getString(cursorPer.getColumnIndexOrThrow("cargo")));
            item.addProperty("idOficina", cursorPer.getInt(cursorPer.getColumnIndexOrThrow("id_oficina")));
            personalArray.add(item);
        }
        cursorPer.close();
        if (personalArray.size() > 0) {
            pushBody.add("personal", personalArray);
        }

        // 9. Direcciones
        JsonArray direccionesArray = new JsonArray();
        Cursor cursorDir = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DIRECCIONES + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorDir.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idDireccion", cursorDir.getInt(cursorDir.getColumnIndexOrThrow("id_direccion")));
            item.addProperty("idAdministrado", cursorDir.getInt(cursorDir.getColumnIndexOrThrow("id_administrado")));
            if (!cursorDir.isNull(cursorDir.getColumnIndexOrThrow("tipo_inmueble"))) {
                item.addProperty("tipoInmueble", cursorDir.getString(cursorDir.getColumnIndexOrThrow("tipo_inmueble")));
            }
            if (!cursorDir.isNull(cursorDir.getColumnIndexOrThrow("calle"))) {
                item.addProperty("calle", cursorDir.getString(cursorDir.getColumnIndexOrThrow("calle")));
            }
            if (!cursorDir.isNull(cursorDir.getColumnIndexOrThrow("numero"))) {
                item.addProperty("numero", cursorDir.getString(cursorDir.getColumnIndexOrThrow("numero")));
            }
            if (!cursorDir.isNull(cursorDir.getColumnIndexOrThrow("comuna_distrito"))) {
                item.addProperty("comunaDistrito", cursorDir.getString(cursorDir.getColumnIndexOrThrow("comuna_distrito")));
            }
            if (!cursorDir.isNull(cursorDir.getColumnIndexOrThrow("ciudad"))) {
                item.addProperty("ciudad", cursorDir.getString(cursorDir.getColumnIndexOrThrow("ciudad")));
            }
            direccionesArray.add(item);
        }
        cursorDir.close();
        if (direccionesArray.size() > 0) {
            pushBody.add("direcciones", direccionesArray);
        }

        // 10. Ubicaciones Físicas
        JsonArray ubicacionesArray = new JsonArray();
        Cursor cursorUbi = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_ARCHIVO_FISICO + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
        while (cursorUbi.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("idUbicacion", cursorUbi.getInt(cursorUbi.getColumnIndexOrThrow("id_ubicacion")));
            item.addProperty("codigoAlmacen", cursorUbi.getString(cursorUbi.getColumnIndexOrThrow("codigo_almacen")));
            item.addProperty("nroPabellon", cursorUbi.getInt(cursorUbi.getColumnIndexOrThrow("nro_pabellon")));
            item.addProperty("nroEstante", cursorUbi.getInt(cursorUbi.getColumnIndexOrThrow("nro_estante")));
            item.addProperty("nroCajaFisica", cursorUbi.getInt(cursorUbi.getColumnIndexOrThrow("nro_caja_fisica")));
            ubicacionesArray.add(item);
        }
        cursorUbi.close();
        if (ubicacionesArray.size() > 0) {
            pushBody.add("ubicacionesFisicas", ubicacionesArray);
        }

        // Si no hay datos pendientes, terminar
        if (pushBody.size() == 0) {
            callback.onSuccess("No hay cambios pendientes de sincronizar");
            return;
        }

        // Ejecutar llamada Retrofit
        apiService.pushData(pushBody).enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.code() == 401 || response.code() == 403) {
                    SessionManager sm = new SessionManager(context);
                    sm.clearSession();
                    android.content.Intent intent = new android.content.Intent(context, LoginActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                    callback.onError("Sesión expirada. Por favor inicie sesión nuevamente.");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SQLiteDatabase writeDb = dbHelper.getWritableDatabase();
                            // Marcar registros locales como Sincronizados (sync_state = 0)
                            writeDb.beginTransaction();
                            try {
                                ContentValues values = new ContentValues();
                                values.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);

                                writeDb.update(DatabaseHelper.TABLE_ADMINISTRADOS, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_EXPEDIENTES, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_DOCUMENTOS, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_DERIVACIONES, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_ACTAS, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_DIRECCIONES, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_OFICINAS, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_PERSONAL, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                writeDb.update(DatabaseHelper.TABLE_ARCHIVO_FISICO, values, DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);

                                writeDb.setTransactionSuccessful();
                                Log.d(TAG, "Sincronización PUSH completada con éxito.");
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess("Sincronización PUSH exitosa.");
                                    }
                                });
                            } finally {
                                writeDb.endTransaction();
                            }
                        }
                    }).start();
                } else {
                    callback.onError("Error en respuesta PUSH del servidor");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Fallo de red en PUSH: " + t.getMessage());
            }
        });
    }

    // ----------------------------------------------------------------
    // OPERACIÓN PULL: Descargar base de datos completa del servidor
    // ----------------------------------------------------------------
    public void pullServerChanges(SyncCallback callback) {
        apiService.pullData().enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.code() == 401 || response.code() == 403) {
                    SessionManager sm = new SessionManager(context);
                    sm.clearSession();
                    android.content.Intent intent = new android.content.Intent(context, LoginActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                    callback.onError("Sesión expirada. Por favor inicie sesión nuevamente.");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    final JsonObject serverData = response.body();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SQLiteDatabase writeDb = dbHelper.getWritableDatabase();
                            writeDb.beginTransaction();
                            try {
                                // ----------------------------------------------------------------
                                // Helper: preservar registros pendientes (sync_state = 1)
                                // ----------------------------------------------------------------
                                class PendingKeeper {
                                    List<ContentValues> save(String table) {
                                        List<ContentValues> list = new ArrayList<>();
                                        Cursor c = writeDb.rawQuery("SELECT * FROM " + table + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                        while (c.moveToNext()) {
                                            ContentValues pcv = new ContentValues();
                                            for (int i = 0; i < c.getColumnCount(); i++) {
                                                String col = c.getColumnName(i);
                                                if (!col.equals(DatabaseHelper.COLUMN_SYNC_STATE)) {
                                                    switch (c.getType(i)) {
                                                        case Cursor.FIELD_TYPE_INTEGER:
                                                            pcv.put(col, c.getInt(i)); break;
                                                        case Cursor.FIELD_TYPE_FLOAT:
                                                            pcv.put(col, c.getDouble(i)); break;
                                                        case Cursor.FIELD_TYPE_BLOB:
                                                            pcv.put(col, c.getBlob(i)); break;
                                                        default:
                                                            pcv.put(col, c.getString(i)); break;
                                                    }
                                                }
                                            }
                                            pcv.put(DatabaseHelper.COLUMN_SYNC_STATE, 1);
                                            list.add(pcv);
                                        }
                                        c.close();
                                        return list;
                                    }

                                    void cleanAndRestore(String table, List<ContentValues> pending) {
                                        writeDb.delete(table, DatabaseHelper.COLUMN_SYNC_STATE + " != 1", null);
                                        for (ContentValues p : pending) {
                                            writeDb.insertWithOnConflict(table, null, p, SQLiteDatabase.CONFLICT_REPLACE);
                                        }
                                    }
                                }
                                PendingKeeper keeper = new PendingKeeper();

                                // ----------------------------------------------------------------
                                // Helper: insertar fila plana desde JsonObject con mapeo camelCase→snake_case
                                // ----------------------------------------------------------------
                                // 1. OFICINAS
                                if (serverData.has("oficinas")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_OFICINAS);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_OFICINAS, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("oficinas")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idOficina").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_oficina") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_oficina", serverId);
                                            cv.put("codigo_oficina", obj.get("codigoOficina").getAsString());
                                            cv.put("siglas_oficiales", obj.get("siglasOficiales").getAsString());
                                            cv.put("nombre_unidad", obj.get("nombreUnidad").getAsString());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_OFICINAS, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_OFICINAS, null, p);
                                    }
                                }

                                // 2. TIPOS DOCUMENTOS
                                if (serverData.has("tiposDocumentos")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("tiposDocumentos")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idTipoDocumento").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_tipo_documento") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_tipo_documento", serverId);
                                            cv.put("nombre_tipo_documento", obj.get("nombreTipoDocumento").getAsString());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null, p);
                                    }
                                }

                                // 3. PERSONAL (nested oficina → id_oficina)
                                if (serverData.has("personal")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_PERSONAL);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_PERSONAL, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("personal")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idEmpleado").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_empleado") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_empleado", serverId);
                                            cv.put("codigo_empleado", obj.get("codigoEmpleado").getAsString());
                                            cv.put("nombre_completo", obj.get("nombreCompleto").getAsString());
                                            cv.put("cargo", obj.get("cargo").getAsString());
                                            int idOf = obj.has("oficina") && obj.get("oficina").isJsonObject()
                                                    ? obj.getAsJsonObject("oficina").get("idOficina").getAsInt()
                                                    : 0;
                                            cv.put("id_oficina", idOf);
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_PERSONAL, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_PERSONAL, null, p);
                                    }
                                }

                                // 4. ARCHIVO FÍSICO
                                if (serverData.has("ubicacionesFisicas")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_ARCHIVO_FISICO);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_ARCHIVO_FISICO, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("ubicacionesFisicas")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idUbicacion").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_ubicacion") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_ubicacion", serverId);
                                            cv.put("codigo_almacen", obj.get("codigoAlmacen").getAsString());
                                            cv.put("nro_pabellon", obj.get("nroPabellon").getAsInt());
                                            cv.put("nro_estante", obj.get("nroEstante").getAsInt());
                                            cv.put("nro_caja_fisica", obj.get("nroCajaFisica").getAsInt());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_ARCHIVO_FISICO, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_ARCHIVO_FISICO, null, p);
                                    }
                                }

                                // ----------------------------------------------------------------
                                // Tablas con sync_state: preservar pendientes locales
                                // ----------------------------------------------------------------

                                // 5. ADMINISTRADOS
                                if (serverData.has("administrados")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_ADMINISTRADOS);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_ADMINISTRADOS, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("administrados")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idAdministrado").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_administrado") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_administrado", serverId);
                                            cv.put("codigo_administrado", obj.get("codigoAdministrado").getAsString());
                                            cv.put("dni_ruc", obj.get("dniRuc").getAsString());
                                            cv.put("nombre_razon_social", obj.get("nombreRazonSocial").getAsString());
                                            if (obj.has("telefono") && !obj.get("telefono").isJsonNull())
                                                cv.put("telefono", obj.get("telefono").getAsString());
                                            if (obj.has("correoNotificaciones") && !obj.get("correoNotificaciones").isJsonNull())
                                                cv.put("correo_notificaciones", obj.get("correoNotificaciones").getAsString());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_ADMINISTRADOS, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_ADMINISTRADOS, null, p);
                                    }
                                }

                                // 6. DIRECCIONES
                                if (serverData.has("direcciones")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_DIRECCIONES);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_DIRECCIONES, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("direcciones")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idDireccion").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_direccion") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_direccion", serverId);
                                            if (obj.has("administrado") && obj.get("administrado").isJsonObject())
                                                cv.put("id_administrado", obj.getAsJsonObject("administrado").get("idAdministrado").getAsInt());
                                            if (obj.has("tipoInmueble") && !obj.get("tipoInmueble").isJsonNull())
                                                cv.put("tipo_inmueble", obj.get("tipoInmueble").getAsString());
                                            if (obj.has("calle") && !obj.get("calle").isJsonNull())
                                                cv.put("calle", obj.get("calle").getAsString());
                                            if (obj.has("numero") && !obj.get("numero").isJsonNull())
                                                cv.put("numero", obj.get("numero").getAsString());
                                            if (obj.has("comunaDistrito") && !obj.get("comunaDistrito").isJsonNull())
                                                cv.put("comuna_distrito", obj.get("comunaDistrito").getAsString());
                                            if (obj.has("ciudad") && !obj.get("ciudad").isJsonNull())
                                                cv.put("ciudad", obj.get("ciudad").getAsString());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_DIRECCIONES, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_DIRECCIONES, null, p);
                                    }
                                }

                                // 7. EXPEDIENTES
                                if (serverData.has("expedientes")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_EXPEDIENTES);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_EXPEDIENTES, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("expedientes")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idExpediente").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_expediente") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_expediente", serverId);
                                            cv.put("nro_expediente_anual", obj.get("nroExpedienteAnual").getAsString());
                                            cv.put("fecha_hora_apertura", obj.get("fechaHoraApertura").getAsString());
                                            if (obj.has("asuntoGeneral") && !obj.get("asuntoGeneral").isJsonNull())
                                                cv.put("asunto_general", obj.get("asuntoGeneral").getAsString());
                                            cv.put("estado_global", obj.get("estadoGlobal").getAsString());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_EXPEDIENTES, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_EXPEDIENTES, null, p);
                                    }
                                }

                                // 8. DOCUMENTOS (nested expediente/tipoDocumento/administrado)
                                if (serverData.has("documentos")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_DOCUMENTOS);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_DOCUMENTOS, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("documentos")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idDocumento").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_documento") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_documento", serverId);
                                            cv.put("nro_documento_unico", obj.get("nroDocumentoUnico").getAsString());
                                            cv.put("cantidad_folios", obj.get("cantidadFolios").getAsInt());
                                            cv.put("fecha_hora_recepcion", obj.get("fechaHoraRecepcion").getAsString());
                                            // Extraer IDs anidados
                                            if (obj.has("expediente") && obj.get("expediente").isJsonObject())
                                                cv.put("id_expediente", obj.getAsJsonObject("expediente").get("idExpediente").getAsInt());
                                            if (obj.has("tipoDocumento") && obj.get("tipoDocumento").isJsonObject())
                                                cv.put("id_tipo_documento", obj.getAsJsonObject("tipoDocumento").get("idTipoDocumento").getAsInt());
                                            if (obj.has("administrado") && obj.get("administrado").isJsonObject())
                                                cv.put("id_administrado", obj.getAsJsonObject("administrado").get("idAdministrado").getAsInt());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_DOCUMENTOS, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_DOCUMENTOS, null, p);
                                    }
                                }

                                // 9. DERIVACIONES (nested documento/empleadoAsignado/oficinaProcedencia)
                                if (serverData.has("derivaciones")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_DERIVACIONES);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_DERIVACIONES, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("derivaciones")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idDerivacion").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_derivacion") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_derivacion", serverId);
                                            cv.put("codigo_barras_seguimiento", obj.get("codigoBarrasSeguimiento").getAsString());
                                            if (obj.has("documento") && obj.get("documento").isJsonObject())
                                                cv.put("id_documento", obj.getAsJsonObject("documento").get("idDocumento").getAsInt());
                                            if (obj.has("empleadoAsignado") && obj.get("empleadoAsignado").isJsonObject())
                                                cv.put("id_empleado_assigned", obj.getAsJsonObject("empleadoAsignado").get("idEmpleado").getAsInt());
                                            if (obj.has("oficinaProcedencia") && obj.get("oficinaProcedencia").isJsonObject())
                                                cv.put("id_oficina_procedencia", obj.getAsJsonObject("oficinaProcedencia").get("idOficina").getAsInt());
                                            cv.put("fecha_hora_despacho", obj.get("fechaHoraDespacho").getAsString());
                                            if (obj.has("prioridadEnvio") && !obj.get("prioridadEnvio").isJsonNull())
                                                cv.put("prioridad_envio", obj.get("prioridadEnvio").getAsString());
                                            if (obj.has("fechaHoraRecepcion") && !obj.get("fechaHoraRecepcion").isJsonNull())
                                                cv.put("fecha_hora_recepcion", obj.get("fechaHoraRecepcion").getAsString());
                                            if (obj.has("observacionesReceptor") && !obj.get("observacionesReceptor").isJsonNull())
                                                cv.put("observaciones_receptor", obj.get("observacionesReceptor").getAsString());
                                            if (obj.has("estadoDerivacion") && !obj.get("estadoDerivacion").isJsonNull())
                                                cv.put("estado_derivacion", obj.get("estadoDerivacion").getAsString());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_DERIVACIONES, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_DERIVACIONES, null, p);
                                    }
                                }

                                // 10. ACTAS
                                if (serverData.has("actas")) {
                                    List<ContentValues> pending = keeper.save(DatabaseHelper.TABLE_ACTAS);
                                    keeper.cleanAndRestore(DatabaseHelper.TABLE_ACTAS, pending);
                                    for (JsonElement el : serverData.getAsJsonArray("actas")) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idActa").getAsInt();
                                        boolean conflicted = false;
                                        for (ContentValues p : pending) {
                                            if (p.getAsInteger("id_acta") == serverId) { conflicted = true; break; }
                                        }
                                        if (!conflicted) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("id_acta", serverId);
                                            cv.put("nro_acta_unico", obj.get("nroActaUnico").getAsString());
                                            if (obj.has("derivacion") && obj.get("derivacion").isJsonObject())
                                                cv.put("id_derivacion", obj.getAsJsonObject("derivacion").get("idDerivacion").getAsInt());
                                            if (obj.has("ubicacionArchivo") && obj.get("ubicacionArchivo").isJsonObject())
                                                cv.put("id_ubicacion_archivo", obj.getAsJsonObject("ubicacionArchivo").get("idUbicacion").getAsInt());
                                            cv.put("fecha_hora_guardado", obj.get("fechaHoraGuardado").getAsString());
                                            if (obj.has("costoDigitalizacion") && !obj.get("costoDigitalizacion").isJsonNull())
                                                cv.put("costo_digitalizacion", obj.get("costoDigitalizacion").getAsDouble());
                                            if (obj.has("costoArancelCustodia") && !obj.get("costoArancelCustodia").isJsonNull())
                                                cv.put("costo_arancel_custodia", obj.get("costoArancelCustodia").getAsDouble());
                                            if (obj.has("costoFinalProcesamiento") && !obj.get("costoFinalProcesamiento").isJsonNull())
                                                cv.put("costo_final_procesamiento", obj.get("costoFinalProcesamiento").getAsDouble());
                                            cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 0);
                                            writeDb.insert(DatabaseHelper.TABLE_ACTAS, null, cv);
                                        }
                                    }
                                    for (ContentValues p : pending) {
                                        writeDb.insert(DatabaseHelper.TABLE_ACTAS, null, p);
                                    }
                                }

                                writeDb.setTransactionSuccessful();
                                Log.d(TAG, "Sincronización PULL completada con éxito.");
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess("Sincronización PULL exitosa. Datos locales actualizados.");
                                    }
                                });
                            } catch (final Exception e) {
                                Log.e(TAG, "Error procesando PULL: " + e.getMessage());
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onError("Error procesando los datos recibidos del servidor");
                                    }
                                });
                            } finally {
                                writeDb.endTransaction();
                            }
                        }
                    }).start();
                } else {
                    callback.onError("Error en respuesta PULL del servidor");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Fallo de red en PULL: " + t.getMessage());
            }
        });
    }
}

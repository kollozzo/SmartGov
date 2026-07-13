package com.example.SmartGov.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.SmartGov.database.DatabaseHelper;
import com.example.SmartGov.network.ApiService;
import com.example.SmartGov.network.RetrofitClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.SmartGov.utils.SessionManager;
import com.example.SmartGov.ui.LoginActivity;

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
                                // 1. Sincronizar Catálogo de Oficinas
                                if (serverData.has("oficinas")) {
                                    writeDb.delete(DatabaseHelper.TABLE_OFICINAS, null, null);
                                    JsonArray oficinas = serverData.getAsJsonArray("oficinas");
                                    for (JsonElement el : oficinas) {
                                        JsonObject obj = el.getAsJsonObject();
                                        ContentValues cv = new ContentValues();
                                        cv.put("id_oficina", obj.get("idOficina").getAsInt());
                                        cv.put("codigo_oficina", obj.get("codigoOficina").getAsString());
                                        cv.put("siglas_oficiales", obj.get("siglasOficiales").getAsString());
                                        cv.put("nombre_unidad", obj.get("nombreUnidad").getAsString());
                                        writeDb.insert(DatabaseHelper.TABLE_OFICINAS, null, cv);
                                    }
                                }

                                // 2. Sincronizar Catálogo de Tipos de Documentos
                                if (serverData.has("tiposDocumentos")) {
                                    writeDb.delete(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null, null);
                                    JsonArray tipos = serverData.getAsJsonArray("tiposDocumentos");
                                    for (JsonElement el : tipos) {
                                        JsonObject obj = el.getAsJsonObject();
                                        ContentValues cv = new ContentValues();
                                        cv.put("id_tipo_documento", obj.get("idTipoDocumento").getAsInt());
                                        cv.put("nombre_tipo_documento", obj.get("nombreTipoDocumento").getAsString());
                                        writeDb.insert(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null, cv);
                                    }
                                }

                                // 3. Sincronizar Administrados (con resolución de conflictos)
                                if (serverData.has("administrados")) {
                                    // 3a. Preservar registros locales con cambios pendientes (sync_state = 1)
                                    List<ContentValues> pendingAdm = new ArrayList<>();
                                    Cursor pendingC = writeDb.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_ADMINISTRADOS + " WHERE " + DatabaseHelper.COLUMN_SYNC_STATE + " = 1", null);
                                    while (pendingC.moveToNext()) {
                                        ContentValues pcv = new ContentValues();
                                        pcv.put("id_administrado", pendingC.getInt(pendingC.getColumnIndexOrThrow("id_administrado")));
                                        pcv.put("codigo_administrado", pendingC.getString(pendingC.getColumnIndexOrThrow("codigo_administrado")));
                                        pcv.put("dni_ruc", pendingC.getString(pendingC.getColumnIndexOrThrow("dni_ruc")));
                                        pcv.put("nombre_razon_social", pendingC.getString(pendingC.getColumnIndexOrThrow("nombre_razon_social")));
                                        if (!pendingC.isNull(pendingC.getColumnIndexOrThrow("telefono"))) {
                                            pcv.put("telefono", pendingC.getString(pendingC.getColumnIndexOrThrow("telefono")));
                                        }
                                        if (!pendingC.isNull(pendingC.getColumnIndexOrThrow("correo_notificaciones"))) {
                                            pcv.put("correo_notificaciones", pendingC.getString(pendingC.getColumnIndexOrThrow("correo_notificaciones")));
                                        }
                                        pcv.put(DatabaseHelper.COLUMN_SYNC_STATE, 1);
                                        pendingAdm.add(pcv);
                                    }
                                    pendingC.close();

                                    // 3b. Eliminar solo registros limpios (sync_state = 0 o 2) — los pendientes se preservan
                                    writeDb.delete(DatabaseHelper.TABLE_ADMINISTRADOS, DatabaseHelper.COLUMN_SYNC_STATE + " != 1", null);

                                    // 3c. Insertar datos del servidor (source of truth para datos sincronizados)
                                    JsonArray administrados = serverData.getAsJsonArray("administrados");
                                    for (JsonElement el : administrados) {
                                        JsonObject obj = el.getAsJsonObject();
                                        int serverId = obj.get("idAdministrado").getAsInt();
                                        // Verificar si no hay un conflicto local pendiente con este ID
                                        boolean hasConflict = false;
                                        for (ContentValues p : pendingAdm) {
                                            if (p.getAsInteger("id_administrado") == serverId) {
                                                hasConflict = true;
                                                break;
                                            }
                                        }
                                        if (!hasConflict) {
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

                                    // 3d. Re-insertar registros pendientes preservados (local wins for pending)
                                    for (ContentValues p : pendingAdm) {
                                        writeDb.insert(DatabaseHelper.TABLE_ADMINISTRADOS, null, p);
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

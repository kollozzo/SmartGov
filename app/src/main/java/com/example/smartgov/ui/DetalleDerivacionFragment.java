package com.example.SmartGov.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.SmartGov.database.DatabaseHelper;
import com.example.SmartGov.databinding.FragmentDetalleDerivacionBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetalleDerivacionFragment extends Fragment {

    private FragmentDetalleDerivacionBinding binding;
    private DatabaseHelper dbHelper;
    private int idDerivacion = -1;

    private List<Integer> idUbicacionesList = new ArrayList<>();
    private String simulatedPhotoPath = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDetalleDerivacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        if (getArguments() != null) {
            idDerivacion = getArguments().getInt("id_derivacion", -1);
        }

        seedArchivosFisicosIfNeeded();
        loadUbicacionesSpinner();
        loadDerivacionDetails();

        binding.buttonRecibir.setOnClickListener(v -> updateDerivacionEstado("RECIBIDO", -12.046374, -77.042793));
        binding.buttonRechazar.setOnClickListener(v -> updateDerivacionEstado("RECHAZADO", -12.046374, -77.042793));
        binding.buttonTomarFoto.setOnClickListener(v -> captureEvidenciaPhoto());
        binding.buttonConfirmarArchivar.setOnClickListener(v -> saveActaArchivamiento(v));
    }

    private void seedArchivosFisicosIfNeeded() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ARCHIVO_FISICO, null);
        if (c.moveToFirst() && c.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("codigo_almacen", "ALM-PRINCIPAL");
            cv.put("nro_pabellon", 1);
            cv.put("nro_estante", 4);
            cv.put("nro_caja_fisica", 12);
            db.insert(DatabaseHelper.TABLE_ARCHIVO_FISICO, null, cv);

            cv.clear();
            cv.put("codigo_almacen", "ALM-SECUNDARIO");
            cv.put("nro_pabellon", 2);
            cv.put("nro_estante", 1);
            cv.put("nro_caja_fisica", 8);
            db.insert(DatabaseHelper.TABLE_ARCHIVO_FISICO, null, cv);
        }
        c.close();
    }

    private void loadUbicacionesSpinner() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> listNames = new ArrayList<>();
        idUbicacionesList.clear();

        Cursor c = db.rawQuery("SELECT id_ubicacion, codigo_almacen, nro_pabellon, nro_estante FROM " + DatabaseHelper.TABLE_ARCHIVO_FISICO, null);
        while (c.moveToNext()) {
            idUbicacionesList.add(c.getInt(0));
            listNames.add(c.getString(1) + " (Pab: " + c.getInt(2) + " Est: " + c.getInt(3) + ")");
        }
        c.close();

        binding.spinnerUbicacion.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, listNames));
    }

    private void loadDerivacionDetails() {
        if (idDerivacion == -1) return;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT d.*, doc.nro_documento_unico " +
                "FROM " + DatabaseHelper.TABLE_DERIVACIONES + " d " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DOCUMENTOS + " doc ON d.id_documento = doc.id_documento " +
                "WHERE d.id_derivacion = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idDerivacion)});
        if (cursor.moveToFirst()) {
            String tracking = cursor.getString(cursor.getColumnIndexOrThrow("codigo_barras_seguimiento"));
            String docNum = cursor.getString(cursor.getColumnIndexOrThrow("nro_documento_unico"));
            String prioridad = cursor.getString(cursor.getColumnIndexOrThrow("prioridad_envio"));
            String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora_despacho"));
            String estado = cursor.getString(cursor.getColumnIndexOrThrow("estado_derivacion"));

            binding.detailTracking.setText("Seguimiento: " + tracking);
            binding.detailDocumento.setText("Doc: " + docNum);
            binding.detailPrioridad.setText("Prioridad: " + prioridad);
            binding.detailFecha.setText("Fecha Despacho: " + fecha);
            binding.detailEstado.setText("Estado: " + estado);

            // Mostrar u ocultar sección de archivado según el estado de recepción
            if ("RECIBIDO".equalsIgnoreCase(estado)) {
                binding.cardArchivar.setVisibility(View.VISIBLE);
            } else {
                binding.cardArchivar.setVisibility(View.GONE);
            }
        }
        cursor.close();
    }

    private void updateDerivacionEstado(String nuevoEstado, double lat, double lon) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado_derivacion", nuevoEstado);
        cv.put("latitud", lat);
        cv.put("longitud", lon);
        cv.put("sync_state", 1); 

        int rows = db.update(DatabaseHelper.TABLE_DERIVACIONES, cv, "id_derivacion = ?", new String[]{String.valueOf(idDerivacion)});
        if (rows > 0) {
            Toast.makeText(requireContext(), "Estado derivación actualizado a: " + nuevoEstado + " (GPS guardado)", Toast.LENGTH_SHORT).show();
            
            if (com.example.SmartGov.utils.NetworkUtils.isOnline(requireContext())) {
                com.example.SmartGov.repository.SyncManager syncManager = new com.example.SmartGov.repository.SyncManager(requireContext());
                syncManager.pushLocalChanges(new com.example.SmartGov.repository.SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Sincronizado
                    }

                    @Override
                    public void onError(String error) {
                        // Error de sincronización asíncrona
                    }
                });
            }
            loadDerivacionDetails();
        } else {
            Toast.makeText(requireContext(), "Error al actualizar estado local", Toast.LENGTH_SHORT).show();
        }
    }

    private void captureEvidenciaPhoto() {
        // En emuladores virtuales, simulamos la captura escribiendo un path de foto falso
        simulatedPhotoPath = "/sdcard/Pictures/evidencia_" + System.currentTimeMillis() + ".jpg";
        Toast.makeText(requireContext(), "Foto de Evidencia Multimedia capturada con éxito", Toast.LENGTH_SHORT).show();
        
        // Simular que el image preview cambia de fondo para dar feedback visual
        binding.imagePreviewEvidencia.setBackgroundColor(0xFF4CAF50); // Fondo verde indicativo
    }

    private void saveActaArchivamiento(View view) {
        if (simulatedPhotoPath.isEmpty()) {
            Toast.makeText(requireContext(), "Debe capturar la evidencia fotográfica antes de archivar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (idUbicacionesList.isEmpty()) {
            Toast.makeText(requireContext(), "No hay ubicaciones físicas cargadas", Toast.LENGTH_SHORT).show();
            return;
        }

        int idUbicacion = idUbicacionesList.get(binding.spinnerUbicacion.getSelectedItemPosition());
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        db.beginTransaction();
        try {
            // 1. Crear Acta de Archivamiento en SQLite
            ContentValues cvActa = new ContentValues();
            cvActa.put("nro_acta_unico", "ACT-" + System.currentTimeMillis() / 1000);
            cvActa.put("id_derivacion", idDerivacion);
            cvActa.put("id_ubicacion_archivo", idUbicacion);
            cvActa.put("fecha_hora_guardado", fechaActual);
            cvActa.put("costo_digitalizacion", 5.50);
            cvActa.put("costo_arancel_custodia", 12.00);
            cvActa.put("costo_final_procesamiento", 17.50);
            cvActa.put("foto_ruta", simulatedPhotoPath); // Multimedia
            cvActa.put("sync_state", 1);
            db.insert(DatabaseHelper.TABLE_ACTAS, null, cvActa);

            // 2. Marcar Derivación como ARCHIVADO en SQLite
            ContentValues cvDer = new ContentValues();
            cvDer.put("estado_derivacion", "ARCHIVADO");
            cvDer.put("sync_state", 1);
            db.update(DatabaseHelper.TABLE_DERIVACIONES, cvDer, "id_derivacion = ?", new String[]{String.valueOf(idDerivacion)});

            db.setTransactionSuccessful();
            success = true;
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error al archivar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }

        if (success) {
            Toast.makeText(requireContext(), "Expediente Archivado exitosamente en Central", Toast.LENGTH_LONG).show();
            
            if (com.example.SmartGov.utils.NetworkUtils.isOnline(requireContext())) {
                com.example.SmartGov.repository.SyncManager syncManager = new com.example.SmartGov.repository.SyncManager(requireContext());
                syncManager.pushLocalChanges(new com.example.SmartGov.repository.SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Sincronizado
                    }

                    @Override
                    public void onError(String error) {
                        // Error de sincronización asíncrona
                    }
                });
            }
            Navigation.findNavController(view).popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

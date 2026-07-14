package com.example.smartgov.ui;

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
import com.example.smartgov.database.DatabaseHelper;
import com.example.smartgov.databinding.FragmentRegistrarDocumentoBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistrarDocumentoFragment extends Fragment {

    private FragmentRegistrarDocumentoBinding binding;
    private DatabaseHelper dbHelper;

    private List<Integer> idExpedientesList = new ArrayList<>();
    private List<Integer> idTiposList = new ArrayList<>();
    private List<Integer> idAdministradosList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistrarDocumentoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        // Cargar combos / spinners desde SQLite o con valores semilla si está vacío
        seedLocalDatabaseIfNeeded();
        loadSpinnersData();

        binding.buttonGuardarDocumento.setOnClickListener(v -> saveDocumento(v));
    }

    private void seedLocalDatabaseIfNeeded() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Seed Expedientes
        Cursor cExp = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_EXPEDIENTES, null);
        if (cExp.moveToFirst() && cExp.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("nro_expediente_anual", "EXP-2026-001");
            cv.put("fecha_hora_apertura", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            cv.put("asunto_general", "Trámite de Licencia de Funcionamiento");
            cv.put("estado_global", "PENDIENTE");
            cv.put("sync_state", 0);
            db.insert(DatabaseHelper.TABLE_EXPEDIENTES, null, cv);
        }
        cExp.close();

        // Seed Tipos
        Cursor cTip = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null);
        if (cTip.moveToFirst() && cTip.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("nombre_tipo_documento", "Carta Solicitud");
            db.insert(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null, cv);

            cv.clear();
            cv.put("nombre_tipo_documento", "Oficio Múltiple");
            db.insert(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null, cv);
        }
        cTip.close();

        // Seed Administrados
        Cursor cAdm = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ADMINISTRADOS, null);
        if (cAdm.moveToFirst() && cAdm.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("codigo_administrado", "ADM-001");
            cv.put("dni_ruc", "10458796541");
            cv.put("nombre_razon_social", "Juan Pérez Corp");
            cv.put("sync_state", 0);
            db.insert(DatabaseHelper.TABLE_ADMINISTRADOS, null, cv);
        }
        cAdm.close();
    }

    private void loadSpinnersData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Cargar Expedientes
        List<String> expedientesNames = new ArrayList<>();
        idExpedientesList.clear();
        Cursor cExp = db.rawQuery("SELECT id_expediente, nro_expediente_anual FROM " + DatabaseHelper.TABLE_EXPEDIENTES, null);
        while (cExp.moveToNext()) {
            idExpedientesList.add(cExp.getInt(0));
            expedientesNames.add(cExp.getString(1));
        }
        cExp.close();
        binding.spinnerExpediente.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, expedientesNames));

        // 2. Cargar Tipos
        List<String> tiposNames = new ArrayList<>();
        idTiposList.clear();
        Cursor cTip = db.rawQuery("SELECT id_tipo_documento, nombre_tipo_documento FROM " + DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, null);
        while (cTip.moveToNext()) {
            idTiposList.add(cTip.getInt(0));
            tiposNames.add(cTip.getString(1));
        }
        cTip.close();
        binding.spinnerTipoDocumento.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, tiposNames));

        // 3. Cargar Administrados
        List<String> administradosNames = new ArrayList<>();
        idAdministradosList.clear();
        Cursor cAdm = db.rawQuery("SELECT id_administrado, nombre_razon_social FROM " + DatabaseHelper.TABLE_ADMINISTRADOS, null);
        while (cAdm.moveToNext()) {
            idAdministradosList.add(cAdm.getInt(0));
            administradosNames.add(cAdm.getString(1));
        }
        cAdm.close();
        binding.spinnerAdministrado.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, administradosNames));
    }

    private void saveDocumento(View view) {
        String nroDoc = binding.nroDocEdit.getText().toString().trim();
        String foliosStr = binding.foliosEdit.getText().toString().trim();

        if (nroDoc.isEmpty()) {
            binding.nroDocLayout.setError("Requerido");
            return;
        } else {
            binding.nroDocLayout.setError(null);
        }

        if (foliosStr.isEmpty()) {
            binding.foliosLayout.setError("Requerido");
            return;
        } else {
            binding.foliosLayout.setError(null);
        }

        int folios = Integer.parseInt(foliosStr);

        if (idExpedientesList.isEmpty() || idTiposList.isEmpty() || idAdministradosList.isEmpty()) {
            Toast.makeText(requireContext(), "Faltan catálogos por cargar", Toast.LENGTH_SHORT).show();
            return;
        }

        int idExpediente = idExpedientesList.get(binding.spinnerExpediente.getSelectedItemPosition());
        int idTipoDoc = idTiposList.get(binding.spinnerTipoDocumento.getSelectedItemPosition());
        int idAdministrado = idAdministradosList.get(binding.spinnerAdministrado.getSelectedItemPosition());

        String fechaActual = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nro_documento_unico", nroDoc);
        cv.put("id_expediente", idExpediente);
        cv.put("id_tipo_documento", idTipoDoc);
        cv.put("id_administrado", idAdministrado);
        cv.put("cantidad_folios", folios);
        cv.put("fecha_hora_recepcion", fechaActual);
        cv.put("sync_state", 1); // 1 = Pendiente de Sincronizar

        boolean success = false;
        db.beginTransaction();
        try {
            long result = db.insert(DatabaseHelper.TABLE_DOCUMENTOS, null, cv);
            if (result != -1) {
                // Generar automáticamente la hoja de ruta/derivación para este documento
                ContentValues cvDer = new ContentValues();
                cvDer.put("codigo_barras_seguimiento", "TRK-" + System.currentTimeMillis() / 1000);
                cvDer.put("id_documento", result);
                cvDer.put("id_empleado_assigned", 1); // Especialista default
                cvDer.put("id_oficina_procedencia", 1); // Oficina default
                cvDer.put("fecha_hora_despacho", fechaActual);
                cvDer.put("prioridad_envio", "ALTA");
                cvDer.put("estado_derivacion", "PENDIENTE");
                cvDer.put("sync_state", 1); // Pendiente de sincronizar

                db.insert(DatabaseHelper.TABLE_DERIVACIONES, null, cvDer);
                db.setTransactionSuccessful();
                success = true;
            } else {
                Toast.makeText(requireContext(), "Error al guardar localmente", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error transaccional: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }

        if (success) {
            Toast.makeText(requireContext(), "Documento registrado y asignado a Bandeja", Toast.LENGTH_SHORT).show();
            
            if (com.example.smartgov.utils.NetworkUtils.isOnline(requireContext())) {
                com.example.smartgov.repository.SyncManager syncManager = new com.example.smartgov.repository.SyncManager(requireContext());
                syncManager.pushLocalChanges(new com.example.smartgov.repository.SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Sincronización exitosa
                    }

                    @Override
                    public void onError(String error) {
                        // Sincronización asíncrona fallida
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

package com.example.SmartGov.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.SmartGov.R;
import com.example.SmartGov.database.DatabaseHelper;
import com.example.SmartGov.databinding.FragmentBandejaBinding;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class BandejaDerivacionesFragment extends Fragment {

    private FragmentBandejaBinding binding;
    private DatabaseHelper dbHelper;
    private List<JsonObject> derivacionesList;
    private DerivacionesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBandejaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        derivacionesList = new ArrayList<>();

        binding.recyclerViewDerivaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new DerivacionesAdapter(derivacionesList, idDerivacion -> {
            // Navegar a Detalle pasando el ID de la derivacion en un Bundle
            Bundle args = new Bundle();
            args.putInt("id_derivacion", idDerivacion);
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_home_to_nav_detalle_derivacion, args);
        });
        
        binding.recyclerViewDerivaciones.setAdapter(adapter);

        seedDerivacionesIfNeeded();
        loadDerivacionesFromLocalDb();

        binding.swipeRefresh.setColorSchemeColors(
                getResources().getColor(R.color.md_primary),
                getResources().getColor(R.color.md_secondary));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshData();
        });
    }

    private void refreshData() {
        loadDerivacionesFromLocalDb();
        if (com.example.SmartGov.utils.NetworkUtils.isOnline(requireContext())) {
            com.example.SmartGov.repository.SyncManager syncManager = new com.example.SmartGov.repository.SyncManager(requireContext());
            syncManager.pullServerChanges(new com.example.SmartGov.repository.SyncManager.SyncCallback() {
                @Override
                public void onSuccess(String msg) {
                    if (isAdded()) {
                        loadDerivacionesFromLocalDb();
                        binding.swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), "Actualizado", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(String error) {
                    if (isAdded()) binding.swipeRefresh.setRefreshing(false);
                }
            });
        } else {
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDerivacionesFromLocalDb();
    }

    private void seedDerivacionesIfNeeded() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 1. Asegurar oficinas y personal para que no fallen llaves foráneas
        Cursor cOf = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_OFICINAS, null);
        if (cOf.moveToFirst() && cOf.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("codigo_oficina", "OF-001");
            cv.put("siglas_oficiales", "SG");
            cv.put("nombre_unidad", "Secretaría General");
            db.insert(DatabaseHelper.TABLE_OFICINAS, null, cv);
        }
        cOf.close();

        Cursor cPer = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_PERSONAL, null);
        if (cPer.moveToFirst() && cPer.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("codigo_empleado", "EMP-100");
            cv.put("nombre_completo", "Ing. Carlos Mendoza");
            cv.put("cargo", "Especialista Documentario");
            cv.put("id_oficina", 1);
            db.insert(DatabaseHelper.TABLE_PERSONAL, null, cv);
        }
        cPer.close();

        // 2. Asegurar expediente y documento
        Cursor cDoc = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DOCUMENTOS, null);
        if (cDoc.moveToFirst() && cDoc.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("nro_documento_unico", "SOL-2026-9081");
            cv.put("id_expediente", 1);
            cv.put("id_tipo_documento", 1);
            cv.put("id_administrado", 1);
            cv.put("cantidad_folios", 5);
            cv.put("fecha_hora_recepcion", "2026-07-08 10:00:00");
            cv.put("sync_state", 0);
            db.insert(DatabaseHelper.TABLE_DOCUMENTOS, null, cv);
        }
        cDoc.close();

        // 3. Sembrar Derivaciones
        Cursor cDer = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DERIVACIONES, null);
        if (cDer.moveToFirst() && cDer.getInt(0) == 0) {
            ContentValues cv = new ContentValues();
            cv.put("codigo_barras_seguimiento", "TRK-001928");
            cv.put("id_documento", 1);
            cv.put("id_empleado_assigned", 1);
            cv.put("id_oficina_procedencia", 1);
            cv.put("fecha_hora_despacho", "2026-07-08 11:30:00");
            cv.put("prioridad_envio", "ALTA");
            cv.put("estado_derivacion", "PENDIENTE");
            cv.put("sync_state", 0);
            db.insert(DatabaseHelper.TABLE_DERIVACIONES, null, cv);

            cv.clear();
            cv.put("codigo_barras_seguimiento", "TRK-001929");
            cv.put("id_documento", 1);
            cv.put("id_empleado_assigned", 1);
            cv.put("id_oficina_procedencia", 1);
            cv.put("fecha_hora_despacho", "2026-07-08 14:15:00");
            cv.put("prioridad_envio", "MEDIA");
            cv.put("estado_derivacion", "RECIBIDO");
            cv.put("sync_state", 0);
            db.insert(DatabaseHelper.TABLE_DERIVACIONES, null, cv);
        }
        cDer.close();
    }

    private void loadDerivacionesFromLocalDb() {
        derivacionesList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query = "SELECT d.*, doc.nro_documento_unico " +
                "FROM " + DatabaseHelper.TABLE_DERIVACIONES + " d " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DOCUMENTOS + " doc ON d.id_documento = doc.id_documento " +
                "ORDER BY d.id_derivacion DESC";
                
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("id_derivacion", cursor.getInt(cursor.getColumnIndexOrThrow("id_derivacion")));
            item.addProperty("codigo_barras_seguimiento", cursor.getString(cursor.getColumnIndexOrThrow("codigo_barras_seguimiento")));
            item.addProperty("nro_documento_unico", cursor.getString(cursor.getColumnIndexOrThrow("nro_documento_unico")));
            item.addProperty("fecha_hora_despacho", cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora_despacho")));
            item.addProperty("prioridad_envio", cursor.getString(cursor.getColumnIndexOrThrow("prioridad_envio")));
            item.addProperty("estado_derivacion", cursor.getString(cursor.getColumnIndexOrThrow("estado_derivacion")));
            item.addProperty("sync_state", cursor.getInt(cursor.getColumnIndexOrThrow("sync_state")));
            derivacionesList.add(item);
        }
        cursor.close();
        adapter.notifyDataSetChanged();

        if (derivacionesList.isEmpty()) {
            Toast.makeText(requireContext(), "Bandeja vacía. Sincronice para descargar derivaciones.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // interface para clics
    public interface OnItemClickListener {
        void onItemClick(int idDerivacion);
    }

    // =================================================================
    // ADAPTER INTERNO: Evita crear archivos independientes innecesarios
    // =================================================================
    private static class DerivacionesAdapter extends RecyclerView.Adapter<DerivacionesAdapter.ViewHolder> {

        private final List<JsonObject> list;
        private final OnItemClickListener listener;

        public DerivacionesAdapter(List<JsonObject> list, OnItemClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_derivacion, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JsonObject item = list.get(position);
            int idDerivacion = item.get("id_derivacion").getAsInt();

            holder.textTrackingCode.setText("Seguimiento: " + item.get("codigo_barras_seguimiento").getAsString());
            holder.textInfoDocumento.setText("Doc: " + (item.has("nro_documento_unico") ? item.get("nro_documento_unico").getAsString() : "S/N"));
            holder.textFechaDespacho.setText("Despacho: " + item.get("fecha_hora_despacho").getAsString());
            
            String estado = item.get("estado_derivacion").getAsString();
            holder.textEstadoDerivacion.setText("Estado: " + estado);

            String prioridad = item.get("prioridad_envio").getAsString();
            holder.textPrioridad.setText(prioridad);
            if ("ALTA".equalsIgnoreCase(prioridad)) {
                holder.textPrioridad.setBackgroundColor(Color.parseColor("#F44336")); // Rojo
            } else if ("MEDIA".equalsIgnoreCase(prioridad)) {
                holder.textPrioridad.setBackgroundColor(Color.parseColor("#FF9800")); // Naranja
            } else {
                holder.textPrioridad.setBackgroundColor(Color.parseColor("#9E9E9E")); // Gris
            }

            int syncState = item.get("sync_state").getAsInt();
            if (syncState == 0) {
                holder.textSyncStatus.setText("Sincronizado");
                holder.textSyncStatus.setTextColor(Color.parseColor("#4CAF50"));
                holder.imageSyncStatus.setImageResource(android.R.drawable.stat_sys_upload_done);
                holder.imageSyncStatus.setColorFilter(Color.parseColor("#4CAF50"));
            } else if (syncState == 2) {
                holder.textSyncStatus.setText("Conflicto");
                holder.textSyncStatus.setTextColor(Color.parseColor("#F44336"));
                holder.imageSyncStatus.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                holder.imageSyncStatus.setColorFilter(Color.parseColor("#F44336"));
            } else {
                holder.textSyncStatus.setText("Pendiente");
                holder.textSyncStatus.setTextColor(Color.parseColor("#FF9800"));
                holder.imageSyncStatus.setImageResource(android.R.drawable.ic_popup_sync);
                holder.imageSyncStatus.setColorFilter(Color.parseColor("#FF9800"));
            }

            holder.itemView.setOnClickListener(v -> listener.onItemClick(idDerivacion));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTrackingCode, textPrioridad, textInfoDocumento, textFechaDespacho, textEstadoDerivacion, textSyncStatus;
            ImageView imageSyncStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textTrackingCode = itemView.findViewById(R.id.textTrackingCode);
                textPrioridad = itemView.findViewById(R.id.textPrioridad);
                textInfoDocumento = itemView.findViewById(R.id.textInfoDocumento);
                textFechaDespacho = itemView.findViewById(R.id.textFechaDespacho);
                textEstadoDerivacion = itemView.findViewById(R.id.textEstadoDerivacion);
                textSyncStatus = itemView.findViewById(R.id.textSyncStatus);
                imageSyncStatus = itemView.findViewById(R.id.imageSyncStatus);
            }
        }
    }
}

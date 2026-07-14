package com.example.smartgov.ui;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgov.R;
import com.example.smartgov.database.DatabaseHelper;
import com.example.smartgov.databinding.FragmentListActasBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ListActasFragment extends Fragment {

    private FragmentListActasBinding binding;
    private DatabaseHelper dbHelper;
    private final List<JsonObject> actasList = new ArrayList<>();
    private ActasAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListActasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        binding.recyclerViewActas.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ActasAdapter(actasList, this::showDetailBottomSheet, this::confirmDelete);
        binding.recyclerViewActas.setAdapter(adapter);

        loadActas();

        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                adapter.getFilter().filter(s.toString());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadActas();
    }

    private void loadActas() {
        actasList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT a.*, d.codigo_barras_seguimiento, f.codigo_almacen " +
                "FROM " + DatabaseHelper.TABLE_ACTAS + " a " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DERIVACIONES + " d ON a.id_derivacion = d.id_derivacion " +
                "LEFT JOIN " + DatabaseHelper.TABLE_ARCHIVO_FISICO + " f ON a.id_ubicacion_archivo = f.id_ubicacion " +
                "ORDER BY a.id_acta DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("id_acta", cursor.getInt(cursor.getColumnIndexOrThrow("id_acta")));
            item.addProperty("nro_acta_unico", cursor.getString(cursor.getColumnIndexOrThrow("nro_acta_unico")));
            item.addProperty("id_derivacion", cursor.getInt(cursor.getColumnIndexOrThrow("id_derivacion")));
            item.addProperty("id_ubicacion_archivo", cursor.getInt(cursor.getColumnIndexOrThrow("id_ubicacion_archivo")));
            item.addProperty("fecha_hora_guardado", cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora_guardado")));
            item.addProperty("costo_digitalizacion", cursor.getDouble(cursor.getColumnIndexOrThrow("costo_digitalizacion")));
            item.addProperty("costo_arancel_custodia", cursor.getDouble(cursor.getColumnIndexOrThrow("costo_arancel_custodia")));
            item.addProperty("costo_final_procesamiento", cursor.getDouble(cursor.getColumnIndexOrThrow("costo_final_procesamiento")));
            item.addProperty("foto_ruta", cursor.getString(cursor.getColumnIndexOrThrow("foto_ruta")));
            item.addProperty("sync_state", cursor.getInt(cursor.getColumnIndexOrThrow("sync_state")));
            item.addProperty("codigo_barras_seguimiento", cursor.getString(cursor.getColumnIndexOrThrow("codigo_barras_seguimiento")));
            item.addProperty("codigo_almacen", cursor.getString(cursor.getColumnIndexOrThrow("codigo_almacen")));
            actasList.add(item);
        }
        cursor.close();
        adapter.updateData();
        adapter.getFilter().filter("");

        boolean empty = actasList.isEmpty();
        binding.textEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerViewActas.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showDetailBottomSheet(int idActa) {
        JsonObject item = null;
        for (JsonObject a : actasList) {
            if (a.get("id_acta").getAsInt() == idActa) { item = a; break; }
        }
        if (item == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_catalog, null);
        dialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.textSheetTitle);
        LinearLayout formContainer = sheetView.findViewById(R.id.formContainer);
        MaterialButton btnSave = sheetView.findViewById(R.id.btnSaveSheet);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btnCancelSheet);

        title.setText("Detalle de Acta");
        btnSave.setText("Cerrar");
        btnSave.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setVisibility(View.GONE);

        addDetailRow(formContainer, "Nro Acta", item.get("nro_acta_unico").getAsString());
        addDetailRow(formContainer, "Derivación", item.has("codigo_barras_seguimiento") ? item.get("codigo_barras_seguimiento").getAsString() : "-");
        addDetailRow(formContainer, "Ubicación", item.has("codigo_almacen") ? item.get("codigo_almacen").getAsString() : "-");
        addDetailRow(formContainer, "Fecha Guardado", item.get("fecha_hora_guardado").getAsString());
        addDetailRow(formContainer, "Costo Digitalización", "S/. " + String.format("%.2f", item.get("costo_digitalizacion").getAsDouble()));
        addDetailRow(formContainer, "Costo Arancel", "S/. " + String.format("%.2f", item.get("costo_arancel_custodia").getAsDouble()));
        addDetailRow(formContainer, "Costo Final", "S/. " + String.format("%.2f", item.get("costo_final_procesamiento").getAsDouble()));
        addDetailRow(formContainer, "Foto Ruta", item.has("foto_ruta") ? item.get("foto_ruta").getAsString() : "-");

        dialog.show();
    }

    private void addDetailRow(LinearLayout container, String label, String value) {
        TextView tv = new TextView(requireContext());
        tv.setText(label + ":  " + value);
        tv.setPadding(0, 8, 0, 8);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        container.addView(tv);
    }

    private void confirmDelete(int idActa) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Acta")
                .setMessage("¿Eliminar esta acta de archivamiento?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int rows = db.delete(DatabaseHelper.TABLE_ACTAS, "id_acta = ?",
                            new String[]{String.valueOf(idActa)});
                    if (rows > 0) {
                        Toast.makeText(requireContext(), "Acta eliminada", Toast.LENGTH_SHORT).show();
                        loadActas();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // =================================================================
    // ADAPTER
    // =================================================================
    public interface OnItemClickListener {
        void onItemClick(int idActa);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int idActa);
    }

    private static class ActasAdapter extends RecyclerView.Adapter<ActasAdapter.ViewHolder> {

        private final List<JsonObject> sourceList;
        private List<JsonObject> displayList;
        private final OnItemClickListener clickListener;
        private final OnItemLongClickListener longClickListener;

        ActasAdapter(List<JsonObject> sourceList, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
            this.sourceList = sourceList;
            this.displayList = new ArrayList<>(sourceList);
            this.clickListener = clickListener;
            this.longClickListener = longClickListener;
        }

        void updateData() {
            displayList = new ArrayList<>(sourceList);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_acta, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JsonObject item = displayList.get(position);
            int idActa = item.get("id_acta").getAsInt();

            holder.itemView.setOnClickListener(v -> clickListener.onItemClick(idActa));
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onItemLongClick(idActa);
                return true;
            });

            holder.textNroActa.setText(item.get("nro_acta_unico").getAsString());
            String tracking = item.has("codigo_barras_seguimiento") ? item.get("codigo_barras_seguimiento").getAsString() : "-";
            String almacen = item.has("codigo_almacen") ? item.get("codigo_almacen").getAsString() : "-";
            holder.textDerivacionUbicacion.setText("Der: " + tracking + " · " + almacen);
            String fecha = item.get("fecha_hora_guardado").getAsString();
            double costo = item.get("costo_final_procesamiento").getAsDouble();
            holder.textFechaCosto.setText(fecha + "  |  S/. " + String.format("%.2f", costo));

            int sync = item.get("sync_state").getAsInt();
            if (sync == 0) {
                holder.textSyncStatus.setText("Sincronizado");
                holder.textSyncStatus.setTextColor(Color.parseColor("#4CAF50"));
                holder.imageSyncStatus.setImageResource(android.R.drawable.stat_sys_upload_done);
                holder.imageSyncStatus.setColorFilter(Color.parseColor("#4CAF50"));
            } else if (sync == 2) {
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
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    String q = constraint != null ? constraint.toString().toLowerCase() : "";
                    List<JsonObject> result = new ArrayList<>();
                    for (JsonObject item : sourceList) {
                        String nro = item.get("nro_acta_unico").getAsString().toLowerCase();
                        String tracking = item.has("codigo_barras_seguimiento") ? item.get("codigo_barras_seguimiento").getAsString().toLowerCase() : "";
                        if (nro.contains(q) || tracking.contains(q)) {
                            result.add(item);
                        }
                    }
                    FilterResults fr = new FilterResults();
                    fr.values = result;
                    fr.count = result.size();
                    return fr;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    displayList = (List<JsonObject>) results.values;
                    if (displayList == null) displayList = new ArrayList<>();
                    notifyDataSetChanged();
                }
            };
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textNroActa, textDerivacionUbicacion, textFechaCosto, textSyncStatus;
            ImageView imageSyncStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textNroActa = itemView.findViewById(R.id.textNroActa);
                textDerivacionUbicacion = itemView.findViewById(R.id.textDerivacionUbicacion);
                textFechaCosto = itemView.findViewById(R.id.textFechaCosto);
                textSyncStatus = itemView.findViewById(R.id.textSyncStatus);
                imageSyncStatus = itemView.findViewById(R.id.imageSyncStatus);
            }
        }
    }
}

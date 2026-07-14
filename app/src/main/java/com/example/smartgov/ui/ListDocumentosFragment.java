package com.example.smartgov.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgov.R;
import com.example.smartgov.database.DatabaseHelper;
import com.example.smartgov.databinding.FragmentListDocumentosBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ListDocumentosFragment extends Fragment {

    private FragmentListDocumentosBinding binding;
    private DatabaseHelper dbHelper;
    private final List<JsonObject> documentosList = new ArrayList<>();
    private DocumentosAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListDocumentosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        binding.recyclerViewDocumentos.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DocumentosAdapter(documentosList, id -> showEditBottomSheet(id), id -> confirmDelete(id));
        binding.recyclerViewDocumentos.setAdapter(adapter);

        loadDocumentos();

        binding.fabAddDocumento.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.nav_registrar_documento));

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
        loadDocumentos();
    }

    private void loadDocumentos() {
        documentosList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT d.*, t.nombre_tipo_documento, a.nombre_razon_social, e.nro_expediente_anual " +
                "FROM " + DatabaseHelper.TABLE_DOCUMENTOS + " d " +
                "LEFT JOIN " + DatabaseHelper.TABLE_TIPOS_DOCUMENTOS + " t ON d.id_tipo_documento = t.id_tipo_documento " +
                "LEFT JOIN " + DatabaseHelper.TABLE_ADMINISTRADOS + " a ON d.id_administrado = a.id_administrado " +
                "LEFT JOIN " + DatabaseHelper.TABLE_EXPEDIENTES + " e ON d.id_expediente = e.id_expediente " +
                "ORDER BY d.id_documento DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            JsonObject item = new JsonObject();
            item.addProperty("id_documento", cursor.getInt(cursor.getColumnIndexOrThrow("id_documento")));
            item.addProperty("nro_documento_unico", cursor.getString(cursor.getColumnIndexOrThrow("nro_documento_unico")));
            item.addProperty("id_expediente", cursor.getInt(cursor.getColumnIndexOrThrow("id_expediente")));
            item.addProperty("id_tipo_documento", cursor.getInt(cursor.getColumnIndexOrThrow("id_tipo_documento")));
            item.addProperty("id_administrado", cursor.getInt(cursor.getColumnIndexOrThrow("id_administrado")));
            item.addProperty("cantidad_folios", cursor.getInt(cursor.getColumnIndexOrThrow("cantidad_folios")));
            item.addProperty("fecha_hora_recepcion", cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora_recepcion")));
            item.addProperty("sync_state", cursor.getInt(cursor.getColumnIndexOrThrow("sync_state")));
            item.addProperty("nombre_tipo_documento", cursor.getString(cursor.getColumnIndexOrThrow("nombre_tipo_documento")));
            item.addProperty("nombre_razon_social", cursor.getString(cursor.getColumnIndexOrThrow("nombre_razon_social")));
            item.addProperty("nro_expediente_anual", cursor.getString(cursor.getColumnIndexOrThrow("nro_expediente_anual")));
            documentosList.add(item);
        }
        cursor.close();
        adapter.updateData();
        adapter.getFilter().filter("");

        boolean empty = documentosList.isEmpty();
        binding.textEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerViewDocumentos.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showEditBottomSheet(int idDocumento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DOCUMENTOS + " WHERE id_documento = ?",
                new String[]{String.valueOf(idDocumento)});
        if (!c.moveToFirst()) { c.close(); return; }

        String nroDoc = c.getString(c.getColumnIndexOrThrow("nro_documento_unico"));
        int folios = c.getInt(c.getColumnIndexOrThrow("cantidad_folios"));
        int idExpediente = c.getInt(c.getColumnIndexOrThrow("id_expediente"));
        int idTipoDoc = c.getInt(c.getColumnIndexOrThrow("id_tipo_documento"));
        int idAdministrado = c.getInt(c.getColumnIndexOrThrow("id_administrado"));
        c.close();

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_catalog, null);
        dialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.textSheetTitle);
        LinearLayout formContainer = sheetView.findViewById(R.id.formContainer);
        MaterialButton btnSave = sheetView.findViewById(R.id.btnSaveSheet);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btnCancelSheet);

        title.setText("Editar Documento");

        // Número de documento
        TextInputLayout tilNro = new TextInputLayout(requireContext());
        tilNro.setHint("Número Único de Documento");
        tilNro.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tilNro.setPadding(0, 0, 0, 12);
        TextInputEditText etNro = new TextInputEditText(requireContext());
        etNro.setText(nroDoc);
        tilNro.addView(etNro);
        formContainer.addView(tilNro);

        // Folios
        TextInputLayout tilFol = new TextInputLayout(requireContext());
        tilFol.setHint("Cantidad de Folios");
        tilFol.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tilFol.setPadding(0, 0, 0, 12);
        TextInputEditText etFol = new TextInputEditText(requireContext());
        etFol.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etFol.setText(String.valueOf(folios));
        tilFol.addView(etFol);
        formContainer.addView(tilFol);

        // Spinners
        Spinner spExp = addSpinner(formContainer, "Expediente");
        Spinner spTipo = addSpinner(formContainer, "Tipo Documento");
        Spinner spAdm = addSpinner(formContainer, "Administrado");

        // Cargar datos de spinners
        List<Integer> idsExp = loadSpinner(spExp, DatabaseHelper.TABLE_EXPEDIENTES, "id_expediente", "nro_expediente_anual", idExpediente);
        List<Integer> idsTipo = loadSpinner(spTipo, DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, "id_tipo_documento", "nombre_tipo_documento", idTipoDoc);
        List<Integer> idsAdm = loadSpinner(spAdm, DatabaseHelper.TABLE_ADMINISTRADOS, "id_administrado", "nombre_razon_social", idAdministrado);

        btnSave.setOnClickListener(v -> {
            String newNro = etNro.getText().toString().trim();
            String folStr = etFol.getText().toString().trim();
            if (newNro.isEmpty()) { tilNro.setError("Requerido"); return; }
            tilNro.setError(null);
            if (folStr.isEmpty()) { tilFol.setError("Requerido"); return; }
            tilFol.setError(null);

            int newFolios = Integer.parseInt(folStr);
            int newExp = idsExp.get(spExp.getSelectedItemPosition());
            int newTipo = idsTipo.get(spTipo.getSelectedItemPosition());
            int newAdm = idsAdm.get(spAdm.getSelectedItemPosition());

            SQLiteDatabase wdb = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("nro_documento_unico", newNro);
            cv.put("cantidad_folios", newFolios);
            cv.put("id_expediente", newExp);
            cv.put("id_tipo_documento", newTipo);
            cv.put("id_administrado", newAdm);
            cv.put("sync_state", 1);

            int rows = wdb.update(DatabaseHelper.TABLE_DOCUMENTOS, cv, "id_documento = ?",
                    new String[]{String.valueOf(idDocumento)});
            if (rows > 0) {
                Toast.makeText(requireContext(), "Documento actualizado", Toast.LENGTH_SHORT).show();
                loadDocumentos();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private Spinner addSpinner(LinearLayout container, String label) {
        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        tv.setPadding(0, 12, 0, 4);
        container.addView(tv);

        Spinner spinner = new Spinner(requireContext());
        container.addView(spinner);
        return spinner;
    }

    private List<Integer> loadSpinner(Spinner spinner, String table, String idCol, String displayCol, int selectedId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> names = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        int selectedPos = 0;

        Cursor c = db.rawQuery("SELECT " + idCol + ", " + displayCol + " FROM " + table + " ORDER BY " + displayCol, null);
        int pos = 0;
        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            names.add(c.getString(1));
            if (c.getInt(0) == selectedId) selectedPos = pos;
            pos++;
        }
        c.close();

        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
        if (!ids.isEmpty()) spinner.setSelection(selectedPos);
        return ids;
    }

    private void confirmDelete(int idDocumento) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Documento")
                .setMessage("¿Eliminar este documento? Se eliminarán también las derivaciones asociadas.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();
                    try {
                        db.delete(DatabaseHelper.TABLE_DERIVACIONES, "id_documento = ?",
                                new String[]{String.valueOf(idDocumento)});
                        db.delete(DatabaseHelper.TABLE_DOCUMENTOS, "id_documento = ?",
                                new String[]{String.valueOf(idDocumento)});
                        db.setTransactionSuccessful();
                        Toast.makeText(requireContext(), "Documento eliminado", Toast.LENGTH_SHORT).show();
                        loadDocumentos();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        db.endTransaction();
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

    public interface OnItemClickListener {
        void onItemClick(int idDocumento);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int idDocumento);
    }

    // =================================================================
    // ADAPTER
    // =================================================================
    private static class DocumentosAdapter extends RecyclerView.Adapter<DocumentosAdapter.ViewHolder> {

        private final List<JsonObject> sourceList;
        private List<JsonObject> displayList;
        private final OnItemClickListener clickListener;
        private final OnItemLongClickListener longClickListener;

        DocumentosAdapter(List<JsonObject> sourceList, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
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
                    .inflate(R.layout.item_documento, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JsonObject item = displayList.get(position);
            holder.textNroDocumento.setText(item.get("nro_documento_unico").getAsString());
            String tipo = item.has("nombre_tipo_documento") ? item.get("nombre_tipo_documento").getAsString() : "";
            String adm = item.has("nombre_razon_social") ? item.get("nombre_razon_social").getAsString() : "";
            holder.textTipoAdministrado.setText(tipo + " · " + adm);
            String exp = item.has("nro_expediente_anual") ? item.get("nro_expediente_anual").getAsString() : "";
            holder.textExpediente.setText("Exp: " + exp);
            String fecha = item.has("fecha_hora_recepcion") ? item.get("fecha_hora_recepcion").getAsString() : "";
            int folios = item.get("cantidad_folios").getAsInt();
            holder.textFechaFolios.setText(fecha + "  |  Folios: " + folios);

            int idDoc = item.get("id_documento").getAsInt();
            holder.itemView.setOnClickListener(v -> clickListener.onItemClick(idDoc));
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onItemLongClick(idDoc);
                return true;
            });

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
                        String nro = item.get("nro_documento_unico").getAsString().toLowerCase();
                        String tipo = item.has("nombre_tipo_documento") ? item.get("nombre_tipo_documento").getAsString().toLowerCase() : "";
                        String adm = item.has("nombre_razon_social") ? item.get("nombre_razon_social").getAsString().toLowerCase() : "";
                        if (nro.contains(q) || tipo.contains(q) || adm.contains(q)) {
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
            TextView textNroDocumento, textTipoAdministrado, textExpediente, textFechaFolios, textSyncStatus;
            ImageView imageSyncStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textNroDocumento = itemView.findViewById(R.id.textNroDocumento);
                textTipoAdministrado = itemView.findViewById(R.id.textTipoAdministrado);
                textExpediente = itemView.findViewById(R.id.textExpediente);
                textFechaFolios = itemView.findViewById(R.id.textFechaFolios);
                textSyncStatus = itemView.findViewById(R.id.textSyncStatus);
                imageSyncStatus = itemView.findViewById(R.id.imageSyncStatus);
            }
        }
    }
}

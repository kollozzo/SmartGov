package com.example.SmartGov.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.SmartGov.R;
import com.example.SmartGov.database.DatabaseHelper;
import com.example.SmartGov.databinding.FragmentGenericCatalogBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericCatalogFragment extends Fragment {

    private static final int TYPE_TEXT = 0;
    private static final int TYPE_INTEGER = 1;

    static class ForeignKeyConfig {
        final String fkColumn;
        final String refTable;
        final String refIdColumn;
        final String refDisplayColumn;

        ForeignKeyConfig(String fkColumn, String refTable, String refIdColumn, String refDisplayColumn) {
            this.fkColumn = fkColumn;
            this.refTable = refTable;
            this.refIdColumn = refIdColumn;
            this.refDisplayColumn = refDisplayColumn;
        }
    }

    static class CatalogConfig {
        final String tableName;
        final String idColumn;
        final String displayColumn;
        final String[] editableColumns;
        final String[] columnLabels;
        final int[] columnTypes;
        final boolean hasSyncState;
        final ForeignKeyConfig fkConfig;
        final int iconResId;

        CatalogConfig(String tableName, String idColumn, String displayColumn,
                      String[] editableColumns, String[] columnLabels, int[] columnTypes,
                      boolean hasSyncState, ForeignKeyConfig fkConfig, int iconResId) {
            this.tableName = tableName;
            this.idColumn = idColumn;
            this.displayColumn = displayColumn;
            this.editableColumns = editableColumns;
            this.columnLabels = columnLabels;
            this.columnTypes = columnTypes;
            this.hasSyncState = hasSyncState;
            this.fkConfig = fkConfig;
            this.iconResId = iconResId;
        }
    }

    private static final Map<String, CatalogConfig> CONFIGS = new HashMap<>();

    static {
        CONFIGS.put(DatabaseHelper.TABLE_OFICINAS, new CatalogConfig(
                DatabaseHelper.TABLE_OFICINAS, "id_oficina", "nombre_unidad",
                new String[]{"codigo_oficina", "siglas_oficiales", "nombre_unidad"},
                new String[]{"Código Oficina", "Siglas", "Nombre de Unidad"},
                new int[]{TYPE_TEXT, TYPE_TEXT, TYPE_TEXT},
                false, null, android.R.drawable.ic_menu_directions));

        CONFIGS.put(DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, new CatalogConfig(
                DatabaseHelper.TABLE_TIPOS_DOCUMENTOS, "id_tipo_documento", "nombre_tipo_documento",
                new String[]{"nombre_tipo_documento"},
                new String[]{"Nombre Tipo Documento"},
                new int[]{TYPE_TEXT},
                false, null, android.R.drawable.ic_menu_sort_by_size));

        CONFIGS.put(DatabaseHelper.TABLE_ADMINISTRADOS, new CatalogConfig(
                DatabaseHelper.TABLE_ADMINISTRADOS, "id_administrado", "nombre_razon_social",
                new String[]{"codigo_administrado", "dni_ruc", "nombre_razon_social", "telefono", "correo_notificaciones"},
                new String[]{"Código Administrado", "DNI/RUC", "Nombre/Razón Social", "Teléfono", "Correo"},
                new int[]{TYPE_TEXT, TYPE_TEXT, TYPE_TEXT, TYPE_TEXT, TYPE_TEXT},
                true, null, android.R.drawable.ic_menu_my_calendar));

        CONFIGS.put(DatabaseHelper.TABLE_PERSONAL, new CatalogConfig(
                DatabaseHelper.TABLE_PERSONAL, "id_empleado", "nombre_completo",
                new String[]{"codigo_empleado", "nombre_completo", "cargo", "id_oficina"},
                new String[]{"Código Empleado", "Nombre Completo", "Cargo", "Oficina"},
                new int[]{TYPE_TEXT, TYPE_TEXT, TYPE_TEXT, TYPE_INTEGER},
                false,
                new ForeignKeyConfig("id_oficina", DatabaseHelper.TABLE_OFICINAS, "id_oficina", "nombre_unidad"),
                android.R.drawable.ic_menu_gallery));

        CONFIGS.put(DatabaseHelper.TABLE_EXPEDIENTES, new CatalogConfig(
                DatabaseHelper.TABLE_EXPEDIENTES, "id_expediente", "nro_expediente_anual",
                new String[]{"nro_expediente_anual", "fecha_hora_apertura", "asunto_general", "estado_global"},
                new String[]{"Nro Expediente Anual", "Fecha Apertura", "Asunto General", "Estado Global"},
                new int[]{TYPE_TEXT, TYPE_TEXT, TYPE_TEXT, TYPE_TEXT},
                true, null, android.R.drawable.ic_menu_compass));

        CONFIGS.put(DatabaseHelper.TABLE_ARCHIVO_FISICO, new CatalogConfig(
                DatabaseHelper.TABLE_ARCHIVO_FISICO, "id_ubicacion", "codigo_almacen",
                new String[]{"codigo_almacen", "nro_pabellon", "nro_estante", "nro_caja_fisica"},
                new String[]{"Código Almacén", "Nro Pabellón", "Nro Estante", "Nro Caja Física"},
                new int[]{TYPE_TEXT, TYPE_INTEGER, TYPE_INTEGER, TYPE_INTEGER},
                false, null, android.R.drawable.ic_menu_gallery));
    }

    private FragmentGenericCatalogBinding binding;
    private DatabaseHelper dbHelper;
    private CatalogConfig config;
    private final List<String> displayList = new ArrayList<>();
    private final List<Integer> idList = new ArrayList<>();
    private final List<Integer> syncStateList = new ArrayList<>();
    private CatalogAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGenericCatalogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        String tableName = getArguments() != null ? getArguments().getString("tableName") : null;
        if (tableName == null) return;
        config = CONFIGS.get(tableName);
        if (config == null) return;

        adapter = new CatalogAdapter();
        binding.listViewCatalog.setAdapter(adapter);

        loadData();

        binding.fabAddCatalog.setOnClickListener(v -> showBottomSheet(-1));
        binding.listViewCatalog.setOnItemClickListener((parent, v, position, id) ->
                showBottomSheet(idList.get(position)));
        binding.listViewCatalog.setOnItemLongClickListener((parent, v, position, id) -> {
            confirmDelete(idList.get(position));
            return true;
        });

        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                adapter.getFilter().filter(s.toString());
            }
        });
    }

    private void loadData() {
        displayList.clear();
        idList.clear();
        syncStateList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String columns = config.idColumn + ", " + config.displayColumn;
        if (config.hasSyncState) {
            columns += ", " + DatabaseHelper.COLUMN_SYNC_STATE;
        }
        Cursor c = db.rawQuery("SELECT " + columns + " FROM " + config.tableName + " ORDER BY " + config.idColumn + " DESC", null);
        while (c.moveToNext()) {
            idList.add(c.getInt(0));
            displayList.add(c.getString(1));
            if (config.hasSyncState) {
                syncStateList.add(c.getInt(2));
            } else {
                syncStateList.add(0);
            }
        }
        c.close();
        adapter.notifyDataSetChanged();
        adapter.getFilter().filter("");

        boolean empty = displayList.isEmpty();
        binding.textEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.listViewCatalog.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ────────────── Bottom Sheet (modern add/edit) ─────────────────

    private void showBottomSheet(int editId) {
        boolean isEditing = editId != -1;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_catalog, null);
        dialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.textSheetTitle);
        LinearLayout formContainer = sheetView.findViewById(R.id.formContainer);
        MaterialButton btnSave = sheetView.findViewById(R.id.btnSaveSheet);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btnCancelSheet);

        title.setText(isEditing ? "Editar" : "Agregar");

        List<TextInputLayout> inputLayouts = new ArrayList<>();
        List<TextInputEditText> editTexts = new ArrayList<>();
        List<Spinner> spinners = new ArrayList<>();
        int fkMatchIdx = (config.fkConfig != null) ? indexOf(config.editableColumns, config.fkConfig.fkColumn) : -1;

        for (int i = 0; i < config.editableColumns.length; i++) {
            if (i == fkMatchIdx) {
                TextView label = new TextView(requireContext());
                label.setText(config.columnLabels[i]);
                label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
                label.setPadding(0, 12, 0, 4);
                formContainer.addView(label);
                Spinner spinner = new Spinner(requireContext());
                loadSpinnerData(spinner, config.fkConfig);
                spinners.add(spinner);
                formContainer.addView(spinner);
            } else {
                TextInputLayout til = new TextInputLayout(requireContext());
                til.setHint(config.columnLabels[i]);
                til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
                til.setPadding(0, 0, 0, 12);

                TextInputEditText et = new TextInputEditText(requireContext());
                if (config.columnTypes[i] == TYPE_INTEGER) {
                    et.setInputType(InputType.TYPE_CLASS_NUMBER);
                }
                til.addView(et);
                editTexts.add(et);
                inputLayouts.add(til);
                formContainer.addView(til);
            }
        }

        if (isEditing) {
            loadEditValues(editId, editTexts, spinners, fkMatchIdx);
        }

        btnSave.setOnClickListener(v -> {
            ContentValues cv = new ContentValues();
            int ei = 0, si = 0;
            boolean valid = true;

            for (int i = 0; i < config.editableColumns.length; i++) {
                String col = config.editableColumns[i];
                if (i == fkMatchIdx) {
                    SpinnerItem item = (SpinnerItem) spinners.get(si++).getSelectedItem();
                    if (item != null) cv.put(col, item.id);
                } else {
                    String value = editTexts.get(ei).getText().toString().trim();
                    if (value.isEmpty()) {
                        inputLayouts.get(ei).setError("Requerido");
                        valid = false;
                        break;
                    }
                    inputLayouts.get(ei).setError(null);
                    if (config.columnTypes[i] == TYPE_INTEGER) {
                        try {
                            cv.put(col, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            inputLayouts.get(ei).setError("Debe ser número");
                            valid = false;
                            break;
                        }
                    } else {
                        cv.put(col, value);
                    }
                    ei++;
                }
            }

            if (!valid) return;

            if (config.hasSyncState) cv.put(DatabaseHelper.COLUMN_SYNC_STATE, 1);

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long result;
            if (isEditing) {
                result = db.update(config.tableName, cv, config.idColumn + " = ?", new String[]{String.valueOf(editId)});
            } else {
                result = db.insert(config.tableName, null, cv);
            }

            if (result != -1) {
                Toast.makeText(requireContext(), isEditing ? "Actualizado" : "Creado", Toast.LENGTH_SHORT).show();
                loadData();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadEditValues(int editId, List<TextInputEditText> editTexts, List<Spinner> spinners, int fkMatchIdx) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + config.tableName + " WHERE " + config.idColumn + " = ?",
                new String[]{String.valueOf(editId)});
        if (c.moveToFirst()) {
            int ei = 0, si = 0;
            for (int i = 0; i < config.editableColumns.length; i++) {
                if (i == fkMatchIdx) {
                    int fkValue = c.getInt(c.getColumnIndexOrThrow(config.editableColumns[i]));
                    Spinner sp = spinners.get(si++);
                    for (int p = 0; p < sp.getCount(); p++) {
                        SpinnerItem item = (SpinnerItem) sp.getItemAtPosition(p);
                        if (item.id == fkValue) { sp.setSelection(p); break; }
                    }
                } else {
                    editTexts.get(ei++).setText(c.getString(c.getColumnIndexOrThrow(config.editableColumns[i])));
                }
            }
        }
        c.close();
    }

    private void confirmDelete(int id) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar")
                .setMessage("¿Eliminar este registro?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int rows = db.delete(config.tableName, config.idColumn + " = ?", new String[]{String.valueOf(id)});
                    if (rows > 0) {
                        Toast.makeText(requireContext(), "Eliminado", Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ────────────── Spinner helper ────────────────────────────────

    static class SpinnerItem {
        final int id;
        final String display;
        SpinnerItem(int id, String display) { this.id = id; this.display = display; }
        @Override public String toString() { return display; }
    }

    private void loadSpinnerData(Spinner spinner, ForeignKeyConfig fk) {
        List<SpinnerItem> items = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + fk.refIdColumn + ", " + fk.refDisplayColumn
                + " FROM " + fk.refTable + " ORDER BY " + fk.refDisplayColumn, null);
        while (c.moveToNext()) items.add(new SpinnerItem(c.getInt(0), c.getString(1)));
        c.close();
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, items));
    }

    private static int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(value)) return i;
        return -1;
    }

    // ────────────── List Adapter with Filter ──────────────────────

    private class CatalogAdapter extends BaseAdapter implements Filterable {
        private final List<String> filteredDisplay = new ArrayList<>();
        private final List<Integer> filteredIds = new ArrayList<>();
        private final List<Integer> filteredSync = new ArrayList<>();
        private String filterQuery = "";

        @Override public int getCount() { return filteredDisplay.size(); }
        @Override public Object getItem(int pos) { return filteredDisplay.get(pos); }
        @Override public long getItemId(int pos) { return filteredIds.get(pos); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext()).inflate(R.layout.item_catalog, parent, false);
            }
            ImageView iconCatalog = convertView.findViewById(R.id.iconCatalog);
            TextView textCatalogName = convertView.findViewById(R.id.textCatalogName);
            ImageView syncIcon = convertView.findViewById(R.id.imageSyncStatus);
            TextView syncText = convertView.findViewById(R.id.textSyncStatus);

            iconCatalog.setImageResource(config.iconResId);
            textCatalogName.setText(filteredDisplay.get(position));

            if (config.hasSyncState) {
                int sync = filteredSync.get(position);
                syncIcon.setVisibility(View.VISIBLE);
                syncText.setVisibility(View.VISIBLE);
                if (sync == 0) {
                    syncText.setText("Ok");
                    syncText.setTextColor(Color.parseColor("#4CAF50"));
                    syncIcon.setImageResource(android.R.drawable.stat_sys_upload_done);
                    syncIcon.setColorFilter(Color.parseColor("#4CAF50"));
                } else if (sync == 2) {
                    syncText.setText("Conflicto");
                    syncText.setTextColor(Color.parseColor("#F44336"));
                    syncIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                    syncIcon.setColorFilter(Color.parseColor("#F44336"));
                } else {
                    syncText.setText("Pendiente");
                    syncText.setTextColor(Color.parseColor("#FF9800"));
                    syncIcon.setImageResource(android.R.drawable.ic_popup_sync);
                    syncIcon.setColorFilter(Color.parseColor("#FF9800"));
                }
            } else {
                syncIcon.setVisibility(View.GONE);
                syncText.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    filterQuery = constraint != null ? constraint.toString().toLowerCase() : "";
                    List<String> filtered = new ArrayList<>();
                    List<Integer> filteredId = new ArrayList<>();
                    List<Integer> filteredSt = new ArrayList<>();
                    for (int i = 0; i < displayList.size(); i++) {
                        if (displayList.get(i).toLowerCase().contains(filterQuery)) {
                            filtered.add(displayList.get(i));
                            filteredId.add(idList.get(i));
                            filteredSt.add(syncStateList.get(i));
                        }
                    }
                    FilterResults results = new FilterResults();
                    results.values = new Object[]{filtered, filteredId, filteredSt};
                    results.count = filtered.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredDisplay.clear();
                    filteredIds.clear();
                    filteredSync.clear();
                    if (results.values instanceof Object[]) {
                        Object[] data = (Object[]) results.values;
                        filteredDisplay.addAll((List<String>) data[0]);
                        filteredIds.addAll((List<Integer>) data[1]);
                        filteredSync.addAll((List<Integer>) data[2]);
                    }
                    notifyDataSetChanged();
                    binding.textEmptyState.setVisibility(filteredDisplay.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.listViewCatalog.setVisibility(filteredDisplay.isEmpty() ? View.GONE : View.VISIBLE);
                }
            };
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

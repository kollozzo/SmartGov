package com.example.SmartGov;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.example.SmartGov.repository.SyncManager;
import com.example.SmartGov.ui.LoginActivity;
import com.example.SmartGov.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.SmartGov.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private SyncManager syncManager;
    private boolean wasOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        syncManager = new SyncManager(this);
        wasOffline = !com.example.SmartGov.utils.NetworkUtils.isOnline(this);

        // Corregir formatos de fechas guardados localmente para cumplir con ISO 8601 del Backend
        try {
            com.example.SmartGov.database.DatabaseHelper dbHelper = new com.example.SmartGov.database.DatabaseHelper(this);
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("UPDATE documentos_ingresados SET fecha_hora_recepcion = replace(fecha_hora_recepcion, ' ', 'T') WHERE fecha_hora_recepcion LIKE '% %'");
            db.execSQL("UPDATE hojas_ruta_derivaciones SET fecha_hora_despacho = replace(fecha_hora_despacho, ' ', 'T') WHERE fecha_hora_despacho LIKE '% %'");
            db.execSQL("UPDATE actas_archivamiento SET fecha_hora_guardado = replace(fecha_hora_guardado, ' ', 'T') WHERE fecha_hora_guardado LIKE '% %'");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Si no está logueado, redirigir a LoginActivity
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_registrar_documento,
                R.id.nav_oficinas, R.id.nav_tipos_documentos,
                R.id.nav_administrados, R.id.nav_personal,
                R.id.nav_expedientes, R.id.nav_archivo_fisico)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Capturar selección de cerrar sesión y sincronización manual en el menú drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                sessionManager.clearSession();
                Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_sync) {
                drawer.closeDrawers();
                syncManager.pushLocalChanges(new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Push: " + msg, Toast.LENGTH_SHORT).show());
                        syncManager.pullServerChanges(new SyncManager.SyncCallback() {
                            @Override
                            public void onSuccess(String pullMsg) {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "Pull: " + pullMsg, Toast.LENGTH_SHORT).show();
                                    try {
                                        navController.navigate(R.id.nav_home);
                                    } catch (Exception ignored) {}
                                });
                            }
                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error Pull: " + error, Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error Push: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
                return true;
            }
            
            // Navegación estándar de Android Jetpack para los demás ítems
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawer.closeDrawers();
            }
            return handled;
        });

        // Registrar Callback de Red para Sincronización Automática al recuperar conexión
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(new android.net.ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(android.net.Network network) {
                    super.onAvailable(network);
                    // Validamos si venimos de offline y si realmente tenemos internet
                    if (wasOffline && com.example.SmartGov.utils.NetworkUtils.isOnline(MainActivity.this)) {
                        wasOffline = false;
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Conexión restaurada. Sincronizando datos automáticamente...", Toast.LENGTH_LONG).show();
                            syncManager.pushLocalChanges(new SyncManager.SyncCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    try {
                                        NavController nController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main);
                                        nController.navigate(R.id.nav_home);
                                    } catch (Exception e) {}
                                }

                                @Override
                                public void onError(String error) {}
                            });
                        });
                    }
                }

                @Override
                public void onLost(android.net.Network network) {
                    super.onLost(network);
                    wasOffline = true;
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
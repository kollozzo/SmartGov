package com.example.smartgov.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartgov.MainActivity;
import com.example.smartgov.databinding.ActivityLoginBinding;
import com.example.smartgov.model.AuthRequest;
import com.example.smartgov.model.AuthResponse;
import com.example.smartgov.network.ApiService;
import com.example.smartgov.network.RetrofitClient;
import com.example.smartgov.utils.NetworkUtils;
import com.example.smartgov.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Si ya hay sesión activa (token JWT u offline), saltar
        if (sessionManager.isLoggedIn()) {
            startMainActivity();
            return;
        }

        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        binding.loginButton.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = binding.usernameEdit.getText().toString().trim();
        String password = binding.passwordEdit.getText().toString().trim();

        if (username.isEmpty()) {
            binding.usernameLayout.setError("Ingrese su nombre de usuario");
            return;
        } else {
            binding.usernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setError("Ingrese su contraseña");
            return;
        } else {
            binding.passwordLayout.setError(null);
        }

        // ── Ruta Offline ──
        if (!NetworkUtils.isOnline(this)) {
            if (sessionManager.checkCredentialsLocally(username, password)) {
                sessionManager.saveAuthToken("OFFLINE_MODE");
                Toast.makeText(this, "Modo offline: bienvenido, " + username, Toast.LENGTH_SHORT).show();
                startMainActivity();
            } else {
                Toast.makeText(this, "Sin conexión. Credenciales no disponibles offline.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // ── Ruta Online ──
        setLoading(true);

        AuthRequest authRequest = new AuthRequest(username, password);
        Call<AuthResponse> call = apiService.login(authRequest);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    sessionManager.saveAuthToken(authResponse.getToken());
                    sessionManager.saveCredentials(username, password);

                    Toast.makeText(LoginActivity.this, "¡Bienvenido, " + username + "!", Toast.LENGTH_SHORT).show();
                    startMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!isLoading);
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

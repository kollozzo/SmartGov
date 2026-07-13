package com.example.SmartGov.network;

import com.example.SmartGov.model.AuthRequest;
import com.example.SmartGov.model.AuthResponse;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    // Inicio de sesión (Autenticación JWT)
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    // Sincronización - PULL: Descargar todo del servidor
    @GET("api/sync/pull")
    Call<JsonObject> pullData();

    // Sincronización - PUSH: Subir cambios locales offline en lote
    @POST("api/sync/push")
    Call<JsonObject> pushData(@Body JsonObject request);
}

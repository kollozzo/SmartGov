package com.example.SmartGov.network;

import android.content.Context;
import com.example.SmartGov.utils.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // 10.0.2.2 es la IP mapeada al localhost de la máquina host desde el emulador de Android
    // Cambiar IP segun la red local del PC para probar en celular real
    private static final String BASE_URL = "https://smartgov-backend-production.up.railway.app/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            SessionManager sessionManager = new SessionManager(context);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        String token = sessionManager.fetchAuthToken();
                        if (token != null) {
                            builder.addHeader("Authorization", "Bearer " + token);
                        }

                        return chain.proceed(builder.build());
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}

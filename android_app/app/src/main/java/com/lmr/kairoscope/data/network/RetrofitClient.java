package com.lmr.kairoscope.data.network;

import java.util.concurrent.TimeUnit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Cliente Singleton para configurar y gestionar las conexiones HTTP con Retrofit.
 * Incluye configuración de timeouts y logging para desarrollo.
 */
public class RetrofitClient {
    private static final String BASE_URL = "http://10.0.2.2:8000/"; // IP del emulador Android
    private static RetrofitClient instance;
    private final ApiService apiService;

    private RetrofitClient() {
        // Interceptor para mostrar logs de peticiones HTTP en desarrollo
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Cliente HTTP con timeouts extendidos para operaciones LLM
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)      // Timeout mayor para generación de barajas
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Configurar Retrofit con conversor JSON
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    /**
     * Patrón Singleton thread-safe para obtener la instancia única.
     */
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
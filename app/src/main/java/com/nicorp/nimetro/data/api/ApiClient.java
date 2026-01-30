package com.nicorp.nimetro.data.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static ApiClient instance;
    private ApiService apiService;
    private String baseUrl;

    private ApiClient(Context context) {
        // Получаем base URL из strings.xml, по умолчанию https://metro.nicorp.tech/api/v1
        try {
            int resId = context.getResources().getIdentifier("api_base_url", "string", context.getPackageName());
            if (resId != 0) {
                baseUrl = context.getString(resId);
            } else {
                baseUrl = "https://metro.nicorp.tech/api/v1";
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get api_base_url from strings.xml, using default", e);
            baseUrl = "https://metro.nicorp.tech/api/v1";
        }
        
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://metro.nicorp.tech/api/v1";
        }
        
        // Убеждаемся, что URL заканчивается на /api/v1
        if (!baseUrl.endsWith("/api/v1")) {
            if (baseUrl.endsWith("/")) {
                baseUrl += "api/v1";
            } else {
                baseUrl += "/api/v1";
            }
        }
        
        // Убеждаемся, что начинается с https://
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl;
        }

        initRetrofit();
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }

    private void initRetrofit() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

        Gson gson = new GsonBuilder()
            .setLenient()
            .create();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

        apiService = retrofit.create(ApiService.class);
        Log.d(TAG, "ApiClient initialized with base URL: " + baseUrl);
    }

    public ApiService getApiService() {
        return apiService;
    }

    public void updateBaseUrl(String newBaseUrl) {
        if (newBaseUrl != null && !newBaseUrl.isEmpty()) {
            this.baseUrl = newBaseUrl;
            initRetrofit();
        }
    }
}


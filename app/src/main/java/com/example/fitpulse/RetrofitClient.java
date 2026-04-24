package com.example.fitpulse;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://api.groq.com/openai/v1/";

    private static Retrofit retrofit;

    public static GroqApi getApi(String apiKey) {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    // Always use the latest apiKey provided
                    // Removed manual Content-Type as Retrofit's GsonConverter adds it correctly
                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + apiKey)
                            .build();

                    return chain.proceed(request);
                })
                .build();

        // Recreate retrofit if it doesn't exist or if we need a fresh client
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(GroqApi.class);
    }
}

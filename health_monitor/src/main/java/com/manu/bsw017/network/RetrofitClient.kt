package com.manu.bsw017.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * IMPORTANT — pick the right host for where the app is running:
     *   - Android EMULATOR:      "http://10.0.2.2:8000/"   (10.0.2.2 is the emulator's alias for your host machine)
     *   - Physical device + USB: run `adb reverse tcp:8000 tcp:8000` on your host, then use "http://127.0.0.1:8000/"
     *   - Physical device on Wi-Fi: use your host machine's LAN IP, e.g. "http://192.168.1.42:8000/"
     *
     * "localhost"/"127.0.0.1" from a real phone means the PHONE itself, not your dev machine —
     * that's the single most common reason a "sync" silently fails with a connection error.
     */
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: HealthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HealthApi::class.java)
    }
}

package com.example.appestable.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Usar la IP local de tu PC (ej. 192.168.0.7) para celular físico
    private const val BASE_URL =
        "http://YOUR_BACKEND_IP:8000/"

    val api: ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }
}
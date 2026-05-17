package com.example.appestable.network

import com.example.appestable.network.models.MeResponse

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {

    @GET("me")
    suspend fun getMe(

        @Header("Authorization")
        token: String

    ): Response<MeResponse>
}
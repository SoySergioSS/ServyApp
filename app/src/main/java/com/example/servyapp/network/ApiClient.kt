package com.example.servyapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // 👇 Pon aquí la URL de tu backend Flask o Cloudflare Tunnel
    private const val BASE_URL = "https://servyapp.refactel.me/"

    val chatApi: ChatApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApi::class.java)
    }
}
package com.example.scoretrack.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object RetrofitClient {
    private const val BASE_URL = "https://v3.football.api-sports.io/"

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("x-rapidapi-key", "ee50c7b11bdf635d23f5dbe4552f4ac8") // <--- KANKA KEY BURAYA!
            .addHeader("x-rapidapi-host", "v3.football.api-sports.io")
            .build()
        chain.proceed(request)
    }.build()

    val apiService: SportsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SportsApi::class.java)
    }
}
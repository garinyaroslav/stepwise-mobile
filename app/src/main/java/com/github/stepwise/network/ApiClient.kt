package com.github.stepwise.network

import retrofit2.Retrofit
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.0.16:8080/api/"

    private var retrofit: Retrofit? = null

    fun init(context: Context, baseUrl: String = BASE_URL) {
        val prefs = context.getSharedPreferences("stepwise_prefs", Context.MODE_PRIVATE)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(prefs, context.applicationContext))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("ApiClient is not initialized. Call ApiClient.init(context) first.")
}
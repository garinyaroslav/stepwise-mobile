package com.github.stepwise.network

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val prefs: SharedPreferences, private val ctx: Context) : Interceptor {

    companion object {
        const val PREFS_TOKEN_KEY = "token"
        const val ACTION_LOGOUT = "com.github.stepwise.ACTION_LOGOUT"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = prefs.getString(PREFS_TOKEN_KEY, null)

        val requestBuilder = original.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        if (response.code == 401) {
            prefs.edit().remove(PREFS_TOKEN_KEY).apply()

            val intent = Intent(ACTION_LOGOUT)
            ctx.sendBroadcast(intent)
        }

        return response
    }
}
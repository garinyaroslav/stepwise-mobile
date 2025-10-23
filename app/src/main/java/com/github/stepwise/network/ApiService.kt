package com.github.stepwise.network

import com.github.stepwise.network.models.LoginRequest
import com.github.stepwise.network.models.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/signin")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
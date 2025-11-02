package com.github.stepwise.network

import com.github.stepwise.network.models.CreateWorkReq
import com.github.stepwise.network.models.GroupResponseDto
import com.github.stepwise.network.models.LoginRequest
import com.github.stepwise.network.models.LoginResponse
import com.github.stepwise.network.models.ProfileReq
import com.github.stepwise.network.models.ProfileRes
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {
    @POST("auth/signin")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("user/profile/my")
    suspend fun getMyProfile(): Response<ProfileRes>

    @PUT("user/profile")
    suspend fun updateProfile(@Body profile: ProfileReq): Response<ProfileRes>

    @POST("work")
    suspend fun createWork(@Body work: CreateWorkReq): Response<Void>

    @GET("group")
    suspend fun getAllGroups(@Query("search") search: String?): Response<List<GroupResponseDto>>
}
package com.github.stepwise.network

import android.R
import com.github.stepwise.network.models.CreateWorkReq
import com.github.stepwise.network.models.GroupResponseDto
import com.github.stepwise.network.models.LoginRequest
import com.github.stepwise.network.models.LoginResponse
import com.github.stepwise.network.models.ProfileReq
import com.github.stepwise.network.models.ProfileRes
import com.github.stepwise.network.models.ProjectResponseDto
import com.github.stepwise.network.models.RejectItemDto
import com.github.stepwise.network.models.ResetPasswordDto
import com.github.stepwise.network.models.UpdateProjectDto
import com.github.stepwise.network.models.WorkResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
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

    @GET("work/teacher/{teacherId}")
    suspend fun getWorksByTeacherId(@Path("teacherId") teacherId: Long, @Query("groupId") groupId: Long?): Response<List<WorkResponseDto>>

    @GET("work/{workId}")
    suspend fun getWorkById(@Path("workId") workId: Long): Response<WorkResponseDto>

    @GET("project/work/{workId}")
    suspend fun getProjectsByWork(@Path("workId") workId: Long): Response<List<ProjectResponseDto>>

    @GET("project/work/{workId}/teacher")
    suspend fun getProjectsByWorkForTeacher(@Path("workId") workId: Long): Response<List<ProjectResponseDto>>

    @GET("project/{projectId}")
    suspend fun getProjectById(@Path("projectId") projectId: Long): Response<ProjectResponseDto>

    @GET("project/{projectId}/teacher")
    suspend fun getProjectByIdForTeacher(@Path("projectId") projectId: Long): Response<ProjectResponseDto>

    @PUT("project")
    suspend fun updateProject(@Body project: UpdateProjectDto): Response<UpdateProjectDto>

    @POST("explanatory-note-item/approve/{id}")
    suspend fun approveExplanatoryNoteItem(@Path("id") itemId: Long): Response<Void>

    @POST("explanatory-note-item/reject/{id}")
    suspend fun rejectExplanatoryNoteItem(@Path("id") itemId: Long, @Body rejectItemDto: RejectItemDto): Response<Void>

    @GET("explanatory-note-item/file")
    suspend fun downloadItemFile(
        @Query("userId") userId: Long?,
        @Query("projectId") projectId: Long,
        @Query("itemId") itemId: Long
    ): Response<ResponseBody>

    @POST("explanatory-note-item/submit/{id}")
    suspend fun submitExplanatoryNoteItem(@Path("id") id: Long): Response<Void>

    @POST("auth/password/reset-request")
    suspend fun requestPasswordReset(@Query("email") email: String): Response<Void>

    @POST("auth/password/reset")
    suspend fun resetPassword(@Body redDto: ResetPasswordDto): Response<Void>

    @GET("work/student")
    suspend fun getStudentWorks(): Response<List<WorkResponseDto>>

    @Multipart
    @POST("explanatory-note-item/draft")
    suspend fun createExplanatoryNoteItem(
        @Part("projectId") projectId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Void>

    @POST("project/{projectId}/approve")
    suspend fun approveProject(@Path("projectId") projectId: Long): Response<ProjectResponseDto>
}
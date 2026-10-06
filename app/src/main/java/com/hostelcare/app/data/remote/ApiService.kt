package com.hostelcare.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>
    @POST("api/complaints")
    suspend fun createComplaint(@Body request: ComplaintRequest): Response<ComplaintResponse>

    @GET("api/complaints")
    suspend fun getComplaints(): Response<ComplaintsListResponse>

    @GET("api/complaints/{id}")
    suspend fun getComplaint(@retrofit2.http.Path("id") id: String): Response<ComplaintResponse>
    @POST("api/ai/analyze-complaint")
    suspend fun analyzeComplaint(@Body request: AiAnalyzeRequest): retrofit2.Response<AiAnalyzeResponse>
}
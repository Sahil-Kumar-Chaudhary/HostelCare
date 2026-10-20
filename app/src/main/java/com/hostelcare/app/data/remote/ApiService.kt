package com.hostelcare.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PATCH
import okhttp3.MultipartBody
import okhttp3.RequestBody


interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<SimpleSuccessResponse>

    @GET("api/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>

    @Multipart
    @POST("api/profile/photo")
    suspend fun updateProfilePhoto(@Part photo: okhttp3.MultipartBody.Part): Response<ProfilePhotoResponse>

    @DELETE("api/profile/photo")
    suspend fun deleteProfilePhoto(): Response<ProfilePhotoResponse>

    @POST("api/complaints")
    suspend fun createComplaint(@Body request: ComplaintRequest): Response<ComplaintResponse>

    @Multipart
    @POST("api/complaints")
    suspend fun createComplaintWithPhoto(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("priority") priority: RequestBody?,
        @Part("hostelBlock") hostelBlock: RequestBody,
        @Part("roomNumber") roomNumber: RequestBody,
        @Part("aiSummary") aiSummary: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): Response<ComplaintResponse>

    @GET("api/complaints")
    suspend fun getComplaints(): Response<ComplaintsListResponse>

    @GET("api/complaints/{id}")
    suspend fun getComplaint(@Path("id") id: String): Response<ComplaintResponse>

    @POST("api/complaints/{id}/withdraw")
    suspend fun withdrawComplaint(
        @Path("id") id: String,
        @Body request: WithdrawComplaintRequest = WithdrawComplaintRequest()
    ): Response<ComplaintResponse>

    @POST("api/ai/analyze-complaint")
    suspend fun analyzeComplaint(@Body request: AiAnalyzeRequest): Response<AiAnalyzeResponse>

    // Warden Endpoints
    @GET("api/warden/dashboard")
    suspend fun getWardenDashboard(): Response<WardenDashboardResponse>

    @GET("api/warden/complaints")
    suspend fun getWardenComplaints(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("priority") priority: String? = null
    ): Response<WardenComplaintsResponse>

    @GET("api/warden/complaints/{id}")
    suspend fun getWardenComplaintDetail(@Path("id") id: String): Response<WardenComplaintDetailResponse>

    @PATCH("api/warden/complaints/{id}/status")
    suspend fun updateWardenComplaintStatus(
        @Path("id") id: String,
        @Body request: UpdateComplaintStatusRequest
    ): Response<SimpleSuccessResponse>

    @POST("api/warden/complaints/{id}/assign")
    suspend fun assignWardenStaff(
        @Path("id") id: String,
        @Body request: AssignStaffRequest
    ): Response<SimpleSuccessResponse>

    @GET("api/warden/staff")
    suspend fun getWardenStaff(
        @Query("filter") filter: String? = null,
        @Query("search") search: String? = null
    ): Response<WardenStaffListResponse>

    @POST("api/warden/staff")
    suspend fun createWardenStaff(@Body request: CreateStaffRequest): Response<SimpleSuccessResponse>

    @GET("api/warden/staff/{id}")
    suspend fun getWardenStaffDetail(@Path("id") id: String): Response<WardenStaffDetailResponse>

    @PUT("api/warden/staff/{id}")
    suspend fun updateWardenStaff(
        @Path("id") id: String,
        @Body request: UpdateStaffRequest
    ): Response<SimpleSuccessResponse>

    @PATCH("api/warden/staff/{id}/status")
    suspend fun toggleWardenStaffStatus(
        @Path("id") id: String,
        @Body request: ToggleStaffStatusRequest
    ): Response<SimpleSuccessResponse>

    @GET("api/warden/reports")
    suspend fun getWardenReports(@Query("timeRange") timeRange: String? = null): Response<WardenReportsResponse>

    @GET("api/warden/profile")
    suspend fun getWardenProfile(): Response<WardenProfileResponse>

    @PUT("api/warden/profile")
    suspend fun updateWardenProfile(@Body request: UpdateWardenProfileRequest): Response<SimpleSuccessResponse>

    // Staff Endpoints
    @GET("api/staff/tasks")
    suspend fun getStaffTasks(@Query("status") status: String? = null): Response<StaffTasksResponse>

    @GET("api/staff/tasks/{id}")
    suspend fun getStaffTaskDetail(@Path("id") id: String): Response<StaffTaskDetailResponse>

    @PATCH("api/staff/tasks/{id}/status")
    suspend fun updateStaffTaskStatus(
        @Path("id") id: String,
        @Body request: UpdateStaffTaskStatusRequest
    ): Response<SimpleSuccessResponse>

    @POST("api/staff/tasks/{id}/complete")
    suspend fun completeStaffTask(
        @Path("id") id: String,
        @Body request: CompleteStaffTaskRequest
    ): Response<SimpleSuccessResponse>

    // In-App Notifications
    @GET("api/notifications")
    suspend fun getInAppNotifications(): Response<InAppNotificationsResponse>

    @PATCH("api/notifications/{id}/read")
    suspend fun markInAppNotificationRead(@Path("id") id: String): Response<SimpleSuccessResponse>

    @PATCH("api/notifications/mark-all-read")
    suspend fun markAllInAppNotificationsRead(): Response<SimpleSuccessResponse>
}
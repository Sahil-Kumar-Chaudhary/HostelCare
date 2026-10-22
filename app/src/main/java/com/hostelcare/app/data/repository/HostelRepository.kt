package com.hostelcare.app.data.repository

import com.hostelcare.app.data.model.Complaint
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.data.model.Notification
import com.hostelcare.app.data.model.Staff
import com.hostelcare.app.data.model.User
import kotlinx.coroutines.flow.Flow

interface HostelRepository {
    // Auth & Users
    suspend fun login(email: String, password: String, portal: String? = null): Result<User>
    suspend fun registerStudent(user: User, password: String, confirmPassword: String = password, acceptedTerms: Boolean = true): Result<User>
    suspend fun getCurrentUser(): User?
    fun getCachedUser(): User? = null
    suspend fun logout()
    suspend fun updateUser(user: User): Result<User>
    suspend fun updateProfilePhoto(photoBytes: ByteArray, mimeType: String): Result<String>
    suspend fun deleteProfilePhoto(): Result<Unit>
    fun currentUserFlow(): Flow<User?>

    // Complaints
    fun getComplaintsForStudent(studentId: String): Flow<List<Complaint>>
    suspend fun refreshComplaints()
    suspend fun triggerAiAnalysis(complaintId: String, forceRetry: Boolean = false): Result<Unit>
    suspend fun fetchComplaint(id: String): Result<Complaint>
    suspend fun withdrawComplaint(complaintId: String, reason: String? = null): Result<Complaint>
    fun getAllComplaints(): Flow<List<Complaint>>
    fun getComplaint(id: String): Flow<Complaint?>
    suspend fun submitComplaint(complaint: Complaint, photoBytes: ByteArray? = null, mimeType: String? = null): Result<Complaint>
    suspend fun updateComplaintStatus(id: String, status: ComplaintStatus, resolutionNote: String? = null): Result<Unit>
    suspend fun assignStaffToComplaint(complaintId: String, staffId: String): Result<Unit>
    suspend fun submitFeedback(complaintId: String, rating: Int, note: String?): Result<Unit>

    // Staff
    fun getAvailableStaff(): Flow<List<Staff>>

    // Notifications
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>
    suspend fun markNotificationAsRead(id: String)
    suspend fun analyzeComplaint(title: String, description: String, category: String?): Result<com.hostelcare.app.data.model.AiAnalysisResult>

    // Warden
    suspend fun getWardenDashboard(): Result<com.hostelcare.app.data.remote.WardenDashboardResponse>
    suspend fun getWardenComplaints(search: String? = null, status: String? = null, category: String? = null, priority: String? = null): Result<List<com.hostelcare.app.data.remote.WardenRecentComplaint>>
    suspend fun getWardenComplaintDetail(id: String): Result<com.hostelcare.app.data.remote.WardenComplaintDetail>
    suspend fun updateWardenComplaintStatus(id: String, status: String, note: String? = null, resolutionNote: String? = null): Result<Unit>
    suspend fun assignWardenStaff(id: String, staffId: String, instructions: String? = null): Result<Unit>
    suspend fun getWardenStaff(filter: String? = null, search: String? = null): Result<List<com.hostelcare.app.data.remote.WardenStaffItem>>
    suspend fun createWardenStaff(request: com.hostelcare.app.data.remote.CreateStaffRequest): Result<Unit>
    suspend fun getWardenStaffDetail(id: String): Result<com.hostelcare.app.data.remote.WardenStaffDetail>
    suspend fun updateWardenStaff(id: String, request: com.hostelcare.app.data.remote.UpdateStaffRequest): Result<Unit>
    suspend fun toggleWardenStaffStatus(id: String, isActive: Boolean): Result<Unit>
    suspend fun getWardenReports(timeRange: String? = null): Result<com.hostelcare.app.data.remote.WardenReportsResponse>
    suspend fun getWardenProfile(): Result<com.hostelcare.app.data.remote.WardenProfileData>
    suspend fun updateWardenProfile(name: String?, phone: String?): Result<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    // Staff Portal
    suspend fun getStaffTasks(status: String? = null): Result<com.hostelcare.app.data.remote.StaffTasksResponse>
    suspend fun getStaffTaskDetail(id: String): Result<com.hostelcare.app.data.remote.StaffTaskDetail>
    suspend fun updateStaffTaskStatus(id: String, status: String?, workNotes: String?): Result<Unit>
    suspend fun completeStaffTask(id: String, completionNotes: String?, completionPhotoUrl: String?): Result<Unit>

    // In-App Notifications
    suspend fun getInAppNotifications(): Result<com.hostelcare.app.data.remote.InAppNotificationsResponse>
    suspend fun markInAppNotificationRead(id: String): Result<Unit>
    suspend fun markAllInAppNotificationsRead(): Result<Unit>
}
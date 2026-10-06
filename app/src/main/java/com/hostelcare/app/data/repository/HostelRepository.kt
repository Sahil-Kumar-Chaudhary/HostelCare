package com.hostelcare.app.data.repository

import com.hostelcare.app.data.model.Complaint
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.data.model.Notification
import com.hostelcare.app.data.model.Staff
import com.hostelcare.app.data.model.User
import kotlinx.coroutines.flow.Flow

interface HostelRepository {
    // Auth & Users
    suspend fun login(email: String, password: String): Result<User>
    suspend fun registerStudent(user: User, password: String): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    suspend fun updateUser(user: User): Result<User>
    fun currentUserFlow(): Flow<User?>

    // Complaints
    fun getComplaintsForStudent(studentId: String): Flow<List<Complaint>>
    suspend fun refreshComplaints()
    suspend fun fetchComplaint(id: String): Result<Complaint>
    fun getAllComplaints(): Flow<List<Complaint>>
    fun getComplaint(id: String): Flow<Complaint?>
    suspend fun submitComplaint(complaint: Complaint): Result<Complaint>
    suspend fun updateComplaintStatus(id: String, status: ComplaintStatus, resolutionNote: String? = null): Result<Unit>
    suspend fun assignStaffToComplaint(complaintId: String, staffId: String): Result<Unit>
    suspend fun submitFeedback(complaintId: String, rating: Int, note: String?): Result<Unit>

    // Staff
    fun getAvailableStaff(): Flow<List<Staff>>

    // Notifications
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>
    suspend fun markNotificationAsRead(id: String)
}

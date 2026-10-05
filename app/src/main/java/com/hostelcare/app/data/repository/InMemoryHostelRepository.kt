package com.hostelcare.app.data.repository

import com.hostelcare.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryHostelRepository : HostelRepository {

    private val users = mutableListOf<User>(
        User(id = "admin1", name = "Admin", email = "admin@campus.edu", role = Role.ADMIN),
        User(id = "student1", name = "Alex Johnson", email = "alex.j@campus.edu", studentId = "STU-8842", hostelBlock = "Block B", roomNumber = "B-204")
    )
    private val currentUser = MutableStateFlow<User?>(null)

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    
    private val staffList = listOf(
        Staff(id = "staff1", name = "Ramesh Kumar", roleTitle = "Duty Plumber"),
        Staff(id = "staff2", name = "Suresh Patil", roleTitle = "Senior Plumber", isAvailable = false),
        Staff(id = "staff3", name = "Manoj Singh", roleTitle = "General Maintenance Tech"),
        Staff(id = "staff4", name = "Vikramaditya Rao", roleTitle = "Duty Electrician")
    )

    init {
        // Pre-populate some complaints for demo
        val demoComplaint = Complaint(
            id = "HC-8941",
            title = "Water leakage in bathroom",
            description = "The sink pipe has a steady drip under the basin since morning.",
            category = ComplaintCategory.PLUMBING,
            priority = ComplaintPriority.HIGH,
            hostelBlock = "Block B",
            roomNumber = "204",
            studentId = "student1",
            status = ComplaintStatus.IN_PROGRESS,
            assignedStaffId = "staff1"
        )
        _complaints.value = listOf(demoComplaint)
        
        _notifications.value = listOf(
            Notification(
                userId = "student1",
                complaintId = "HC-8941",
                type = NotificationType.IN_PROGRESS,
                title = "In Progress",
                message = "Maintenance staff Ramesh Kumar is now in progress on your bathroom pipe issue."
            )
        )
    }

    override suspend fun login(email: String, password: String): Result<User> {
        // Simple mock login
        val user = users.find { it.email == email }
        if (user != null) {
            currentUser.value = user
            return Result.success(user)
        }
        return Result.failure(Exception("Invalid credentials"))
    }

    override suspend fun registerStudent(user: User, password: String): Result<User> {
        users.add(user)
        currentUser.value = user
        return Result.success(user)
    }

    override suspend fun getCurrentUser(): User? = currentUser.value

    override suspend fun logout() {
        currentUser.value = null
    }

    override suspend fun updateUser(user: User): Result<User> {
        val index = users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            users[index] = user
            currentUser.value = user
            return Result.success(user)
        }
        return Result.failure(Exception("User not found"))
    }

    override fun currentUserFlow(): Flow<User?> = currentUser

    override fun getComplaintsForStudent(studentId: String): Flow<List<Complaint>> {
        return _complaints.map { list -> list.filter { it.studentId == studentId }.sortedByDescending { it.createdAt } }
    }

    override fun getAllComplaints(): Flow<List<Complaint>> {
        return _complaints.map { it.sortedByDescending { c -> c.createdAt } }
    }

    override fun getComplaint(id: String): Flow<Complaint?> {
        return _complaints.map { list -> list.find { it.id == id } }
    }

    override suspend fun submitComplaint(complaint: Complaint): Result<Complaint> {
        _complaints.update { it + complaint }
        // Create notification
        val notif = Notification(
            userId = complaint.studentId,
            complaintId = complaint.id,
            type = NotificationType.SUBMITTED,
            title = "Complaint Submitted",
            message = "Your complaint '${complaint.title}' has been received."
        )
        _notifications.update { listOf(notif) + it }
        return Result.success(complaint)
    }

    override suspend fun updateComplaintStatus(
        id: String,
        status: ComplaintStatus,
        resolutionNote: String?
    ): Result<Unit> {
        var complaintStudentId = ""
        var complaintTitle = ""
        _complaints.update { list ->
            list.map {
                if (it.id == id) {
                    complaintStudentId = it.studentId
                    complaintTitle = it.title
                    it.copy(status = status, resolutionNote = resolutionNote ?: it.resolutionNote)
                } else it
            }
        }
        
        // Notification
        if (complaintStudentId.isNotEmpty()) {
            val type = when(status) {
                ComplaintStatus.RESOLVED -> NotificationType.RESOLVED
                ComplaintStatus.IN_PROGRESS -> NotificationType.IN_PROGRESS
                else -> NotificationType.ADVISORY
            }
            val msg = if (status == ComplaintStatus.RESOLVED) "Your complaint has been marked as Resolved." else "Complaint status changed to $status."
            val notif = Notification(
                userId = complaintStudentId,
                complaintId = id,
                type = type,
                title = "Status Update",
                message = msg
            )
            _notifications.update { listOf(notif) + it }
        }
        return Result.success(Unit)
    }

    override suspend fun assignStaffToComplaint(complaintId: String, staffId: String): Result<Unit> {
        var complaintStudentId = ""
        val staff = staffList.find { it.id == staffId }
        _complaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    complaintStudentId = it.studentId
                    it.copy(assignedStaffId = staffId, status = ComplaintStatus.ASSIGNED)
                } else it
            }
        }
        if (complaintStudentId.isNotEmpty() && staff != null) {
            val notif = Notification(
                userId = complaintStudentId,
                complaintId = complaintId,
                type = NotificationType.ASSIGNED,
                title = "Staff Assigned",
                message = "Your complaint has been assigned to ${staff.name} (${staff.roleTitle})."
            )
            _notifications.update { listOf(notif) + it }
        }
        return Result.success(Unit)
    }

    override suspend fun submitFeedback(
        complaintId: String,
        rating: Int,
        note: String?
    ): Result<Unit> {
        _complaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    it.copy(feedbackRating = rating, feedbackNote = note)
                } else it
            }
        }
        return Result.success(Unit)
    }

    override fun getAvailableStaff(): Flow<List<Staff>> = MutableStateFlow(staffList)

    override fun getNotificationsForUser(userId: String): Flow<List<Notification>> {
        return _notifications.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }
    }

    override suspend fun markNotificationAsRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }
}

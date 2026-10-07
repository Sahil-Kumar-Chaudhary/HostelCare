package com.hostelcare.app.data.model

import java.util.UUID

enum class Role { STUDENT, ADMIN }

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val studentId: String = "",
    val hostelBlock: String = "",
    val roomNumber: String = "",
    val phone: String = "",
    val profilePhotoUri: String? = null,
    val role: Role = Role.STUDENT
)

enum class ComplaintCategory {
    PLUMBING, ELECTRICAL, INTERNET, CLEANING, FURNITURE, CARPENTRY, INFRASTRUCTURE, OTHER
}

enum class ComplaintPriority {
    LOW, MEDIUM, HIGH, EMERGENCY
}

enum class ComplaintStatus {
    SUBMITTED, UNDER_REVIEW, ASSIGNED, IN_PROGRESS, RESOLVED
}

data class Complaint(
    val id: String = UUID.randomUUID().toString(),
    val ticketId: String = "",
    val title: String,
    val description: String,
    val category: ComplaintCategory,
    val priority: ComplaintPriority = ComplaintPriority.LOW,
    val hostelBlock: String,
    val roomNumber: String,
    val photoUri: String? = null,
    val status: ComplaintStatus = ComplaintStatus.SUBMITTED,
    val createdAt: Long = System.currentTimeMillis(),
    val studentId: String,
    val assignedStaffId: String? = null,
    val resolutionNote: String? = null,
    val feedbackRating: Int? = null,
    val feedbackNote: String? = null,
    val aiSummary: String? = null
)

data class Staff(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val roleTitle: String,
    val isAvailable: Boolean = true
)

enum class NotificationType {
    SUBMITTED, AI_ANALYSIS_COMPLETED, ASSIGNED, STATUS_UPDATED, IN_PROGRESS, RESOLVED, ADVISORY
}

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val complaintId: String?,
    val type: NotificationType,
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class AiAnalysisResult(
    val category: ComplaintCategory,
    val priority: ComplaintPriority,
    val summary: String
)
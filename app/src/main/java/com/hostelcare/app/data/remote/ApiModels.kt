package com.hostelcare.app.data.remote

import com.google.gson.annotations.SerializedName
import com.hostelcare.app.data.model.Role

data class RegisterRequest(
    val name: String,
    val studentId: String,
    val email: String,
    val phone: String,
    val collegeName: String,
    val course: String,
    val currentYear: String,
    val semester: String,
    val hostelName: String,
    val hostelBlock: String,
    val roomNumber: String,
    val password: String,
    val confirmPassword: String,
    val acceptedTerms: Boolean
)

data class LoginRequest(
    val email: String? = null,
    val studentId: String? = null,
    val password: String,
    val portal: String? = null
)

data class AuthResponse(
    val message: String,
    val token: String?,
    val user: NetworkUser
)

data class ProfileResponse(
    val user: NetworkUser
)

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val hostelBlock: String,
    val roomNumber: String,
    val phone: String,
    val collegeName: String = "",
    val course: String = "",
    val currentYear: String = "",
    val semester: String = "",
    val hostelName: String = ""
)

data class NetworkUser(
    val id: String,
    val name: String,
    val studentId: String? = null,
    val email: String,
    val phone: String = "",
    val collegeName: String? = null,
    val course: String? = null,
    val currentYear: String? = null,
    val semester: String? = null,
    val hostelName: String? = null,
    val hostelBlock: String? = null,
    val roomNumber: String? = null,
    @SerializedName("profilePhoto")
    val profilePhotoUri: String? = null,
    val role: String
) {
    fun toLocalUser() = com.hostelcare.app.data.model.User(
        id = id,
        name = name,
        email = email,
        studentId = studentId ?: "",
        phone = phone,
        collegeName = collegeName ?: "",
        course = course ?: "",
        currentYear = currentYear ?: "",
        semester = semester ?: "",
        hostelName = hostelName ?: "",
        hostelBlock = hostelBlock ?: "",
        roomNumber = roomNumber ?: "",
        profilePhotoUri = profilePhotoUri,
        role = when (role.trim().lowercase()) {
            "warden" -> Role.WARDEN
            "staff" -> Role.STAFF
            "admin" -> Role.STAFF
            else -> Role.STUDENT
        }
    )
}

data class ComplaintRequest(
    val title: String,
    val description: String,
    val category: String,
    val priority: String?,
    val hostelBlock: String,
    val roomNumber: String,
    val photoUrl: String?,
    val aiSummary: String?
)

data class ComplaintResponse(
    val message: String?,
    val complaint: NetworkComplaint
)

data class ComplaintsListResponse(
    val complaints: List<NetworkComplaint>
)

data class NetworkComplaint(
    @SerializedName("_id") val id: String,
    val ticketId: String?,
    val student: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    val hostelBlock: String,
    val roomNumber: String,
    val photoUrl: String?,
    val aiSummary: String?,
    val status: String,
    val assignedStaff: String?,
    val resolutionNote: String?,
    val createdAt: String,
    val updatedAt: String
) {
    fun toLocalComplaint() = com.hostelcare.app.data.model.Complaint(
        id = id,
        ticketId = ticketId ?: "",
        title = title,
        description = description,
        category = try { com.hostelcare.app.data.model.ComplaintCategory.valueOf(category.uppercase()) } catch (e: Exception) { com.hostelcare.app.data.model.ComplaintCategory.OTHER },
        priority = try { com.hostelcare.app.data.model.ComplaintPriority.valueOf(priority.uppercase()) } catch (e: Exception) { com.hostelcare.app.data.model.ComplaintPriority.MEDIUM },
        hostelBlock = hostelBlock,
        roomNumber = roomNumber,
        photoUri = photoUrl,
        status = try { com.hostelcare.app.data.model.ComplaintStatus.valueOf(status.uppercase()) } catch (e: Exception) { com.hostelcare.app.data.model.ComplaintStatus.SUBMITTED },
        createdAt = try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { System.currentTimeMillis() },
        studentId = student,
        assignedStaffId = assignedStaff,
        resolutionNote = resolutionNote,
        aiSummary = aiSummary
    )
}
data class AiAnalyzeRequest(val complaintId: String? = null,
    val title: String,
    val description: String,
    val category: String? = null,
    val image: String? = null
)

data class AiAnalysisData(
    val category: String,
    val priority: String,
    val summary: String
)

data class AiAnalyzeResponse(
    val message: String,
    val analysis: AiAnalysisData
)


data class ProfilePhotoResponse(val message: String, val photoUrl: String? = null)

data class WardenDashboardResponse(
    val success: Boolean,
    val warden: NetworkWardenScope?,
    val stats: WardenStatsData,
    val recentComplaints: List<WardenRecentComplaint>
)

data class NetworkWardenScope(
    val name: String,
    val email: String,
    val hostelName: String,
    val hostelBlock: String? = null
)

data class WardenStatsData(
    val total: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val resolved: Int = 0,
    val priorityIssues: Int = 0
)

data class WardenRecentComplaint(
    val id: String,
    val ticketId: String?,
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val status: String,
    val hostelBlock: String?,
    val roomNumber: String?,
    val photoUrl: String?,
    val aiSummary: String?,
    val createdAt: String?,
    val student: WardenComplaintStudent?,
    val assignedStaff: WardenComplaintStaff?
)

data class WardenComplaintStudent(
    val id: String?,
    val name: String?,
    val studentId: String?,
    val roomNumber: String?,
    val hostelBlock: String?
)

data class WardenComplaintStaff(
    val id: String?,
    val name: String?,
    val staffId: String? = null,
    val department: String? = null,
    val phone: String? = null
)

data class WardenComplaintsResponse(
    val success: Boolean,
    val count: Int,
    val complaints: List<WardenRecentComplaint>
)

data class WardenComplaintDetailResponse(
    val success: Boolean,
    val complaint: WardenComplaintDetail
)

data class WardenComplaintDetail(
    val id: String,
    val ticketId: String?,
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val status: String,
    val hostelName: String? = null,
    val hostelBlock: String?,
    val roomNumber: String?,
    val photoUrl: String?,
    val completionPhotoUrl: String?,
    val aiSummary: String?,
    val assignmentInstructions: String?,
    val workNotes: String?,
    val resolutionNote: String?,
    val createdAt: String?,
    val student: WardenComplaintStudentDetail?,
    val assignedStaff: WardenComplaintStaff?,
    val timeline: List<WardenTimelineItem> = emptyList()
)

data class WardenComplaintStudentDetail(
    val id: String?,
    val name: String?,
    val studentId: String?,
    val email: String?,
    val phone: String?,
    val hostelName: String? = null,
    val roomNumber: String?,
    val hostelBlock: String?
)

data class WardenTimelineItem(
    val status: String,
    val note: String?,
    val createdAt: String?,
    val updatedBy: WardenTimelineUser? = null
)

data class WardenTimelineUser(
    val name: String?,
    val role: String?
)

data class UpdateComplaintStatusRequest(
    val status: String,
    val note: String? = null,
    val resolutionNote: String? = null
)

data class AssignStaffRequest(
    val staffId: String,
    val instructions: String? = null
)

data class WardenStaffListResponse(
    val success: Boolean,
    val count: Int,
    val staff: List<WardenStaffItem>
)

data class WardenStaffItem(
    val id: String,
    val staffId: String,
    val name: String,
    val email: String,
    val phone: String?,
    val department: String,
    val hostelName: String?,
    val hostelBlock: String?,
    val isActive: Boolean = true,
    val totalAssigned: Int = 0,
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val pendingCount: Int = 0
)

data class CreateStaffRequest(
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val hostelBlock: String? = null,
    val password: String
)

data class WardenStaffDetailResponse(
    val success: Boolean,
    val staff: WardenStaffDetail
)

data class WardenStaffDetail(
    val id: String,
    val staffId: String,
    val name: String,
    val email: String,
    val phone: String?,
    val department: String,
    val hostelName: String?,
    val hostelBlock: String?,
    val isActive: Boolean = true,
    val stats: StaffStatsData = StaffStatsData(),
    val recentTasks: List<StaffRecentTask> = emptyList()
)

data class StaffStatsData(
    val totalAssigned: Int = 0,
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val pendingCount: Int = 0,
    val pendingReviewCount: Int = 0
)

data class StaffRecentTask(
    val id: String,
    val ticketId: String?,
    val title: String,
    val category: String,
    val status: String,
    val priority: String,
    val roomNumber: String?,
    val hostelBlock: String?,
    val createdAt: String?
)

data class UpdateStaffRequest(
    val name: String? = null,
    val phone: String? = null,
    val department: String? = null,
    val hostelBlock: String? = null
)

data class ToggleStaffStatusRequest(
    val isActive: Boolean
)

data class WardenReportsResponse(
    val success: Boolean,
    val timeRange: String,
    val stats: WardenReportStats,
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val trends: List<ReportTrendItem> = emptyList()
)

data class WardenReportStats(
    val total: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val resolved: Int = 0,
    val avgResolutionHours: Int = 0
)

data class CategoryBreakdownItem(
    val category: String,
    val count: Int
)

data class ReportTrendItem(
    val date: String,
    val count: Int
)

data class WardenProfileResponse(
    val success: Boolean,
    val warden: WardenProfileData
)

data class WardenProfileData(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val hostelName: String,
    val hostelBlock: String?,
    val profilePhoto: String?,
    val role: String
)

data class UpdateWardenProfileRequest(
    val name: String? = null,
    val phone: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class StaffTasksResponse(
    val success: Boolean,
    val stats: StaffStatsData = StaffStatsData(),
    val tasks: List<StaffTaskItem> = emptyList()
)

data class StaffTaskItem(
    val id: String,
    val ticketId: String?,
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val status: String,
    val hostelName: String? = null,
    val hostelBlock: String?,
    val roomNumber: String?,
    val photoUrl: String?,
    val aiSummary: String?,
    val assignmentInstructions: String?,
    val workNotes: String?,
    val createdAt: String?,
    val student: StaffTaskStudent?
)

data class StaffTaskStudent(
    val id: String?,
    val name: String?,
    val studentId: String? = null,
    val phone: String?,
    val email: String? = null,
    val roomNumber: String?,
    val hostelBlock: String?,
    val hostelName: String? = null
)

data class StaffTaskDetailResponse(
    val success: Boolean,
    val task: StaffTaskDetail
)

data class StaffTaskDetail(
    val id: String,
    val ticketId: String?,
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val status: String,
    val hostelName: String? = null,
    val hostelBlock: String?,
    val roomNumber: String?,
    val photoUrl: String?,
    val completionPhotoUrl: String?,
    val assignmentInstructions: String?,
    val workNotes: String?,
    val aiSummary: String?,
    val createdAt: String?,
    val student: StaffTaskStudent?,
    val timeline: List<WardenTimelineItem> = emptyList()
)

data class UpdateStaffTaskStatusRequest(
    val status: String? = null,
    val workNotes: String? = null
)

data class CompleteStaffTaskRequest(
    val completionNotes: String? = null,
    val completionPhotoUrl: String? = null
)

data class InAppNotificationsResponse(
    val success: Boolean,
    val unreadCount: Int = 0,
    val notifications: List<InAppNotificationItem> = emptyList()
)

data class InAppNotificationItem(
    @SerializedName(value = "id", alternate = ["_id"]) val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "system",
    @SerializedName(value = "complaintId", alternate = ["complaint"]) val complaintId: String? = null,
    val isRead: Boolean = false,
    val createdAt: String? = null
)

data class SimpleSuccessResponse(
    val success: Boolean,
    val message: String?
)
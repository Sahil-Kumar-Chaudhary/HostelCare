package com.hostelcare.app.data.remote

import com.google.gson.annotations.SerializedName
import com.hostelcare.app.data.model.Role

data class RegisterRequest(
    val name: String,
    val studentId: String,
    val email: String,
    val password: String,
    val hostelBlock: String,
    val roomNumber: String,
    val phone: String
)

data class LoginRequest(
    val email: String,
    val password: String
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
    val phone: String
)

data class NetworkUser(
    val id: String,
    val name: String,
    val studentId: String,
    val email: String,
    val hostelBlock: String,
    val roomNumber: String,
    val phone: String,
    @SerializedName("profilePhoto")
    val profilePhotoUri: String?,
    val role: String
) {
    fun toLocalUser() = com.hostelcare.app.data.model.User(
        id = id,
        name = name,
        email = email,
        studentId = studentId,
        hostelBlock = hostelBlock,
        roomNumber = roomNumber,
        phone = phone,
        profilePhotoUri = profilePhotoUri,
        role = if (role.equals("admin", ignoreCase = true) || role.equals("staff", ignoreCase = true)) Role.ADMIN else Role.STUDENT
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
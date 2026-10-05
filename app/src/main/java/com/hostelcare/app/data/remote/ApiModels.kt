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

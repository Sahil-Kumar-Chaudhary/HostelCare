package com.hostelcare.app.data.repository

import com.hostelcare.app.data.model.*
import com.hostelcare.app.data.remote.ApiService
import com.hostelcare.app.data.remote.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class InMemoryHostelRepository(private val apiService: ApiService, private val tokenManager: TokenManager) : HostelRepository {

    private val currentUser = MutableStateFlow<User?>(null)
    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    
    private val staffList = listOf(
        Staff(id = "staff1", name = "Ramesh Kumar", roleTitle = "Duty Plumber"),
        Staff(id = "staff2", name = "Suresh Patil", roleTitle = "Senior Plumber", isAvailable = false),
        Staff(id = "staff3", name = "Manoj Singh", roleTitle = "General Maintenance Tech"),
        Staff(id = "staff4", name = "Vikramaditya Rao", roleTitle = "Duty Electrician")
    )

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiService.login(com.hostelcare.app.data.remote.LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                body.token?.let { tokenManager.saveToken(it) }
                val user = body.user.toLocalUser()
                currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } 
        catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Something went wrong on the server. Please try again."))
        }

    }

    override suspend fun registerStudent(user: User, password: String): Result<User> {
        return try {
            val req = com.hostelcare.app.data.remote.RegisterRequest(
                name = user.name,
                studentId = user.studentId,
                email = user.email,
                password = password,
                hostelBlock = user.hostelBlock,
                roomNumber = user.roomNumber,
                phone = user.phone
            )
            val response = apiService.register(req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.user.toLocalUser())
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user.toLocalUser()
                currentUser.value = user
                user
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        currentUser.value = null
    }

    override suspend fun updateProfilePhoto(photoBytes: ByteArray, mimeType: String): Result<String> {
        return try {
            val reqFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse(mimeType), photoBytes)
            val photoPart = okhttp3.MultipartBody.Part.createFormData("photo", "profile.jpg", reqFile)
            val response = apiService.updateProfilePhoto(photoPart)
            if (response.isSuccessful && response.body() != null) {
                val url = response.body()!!.photoUrl ?: ""
                val current = currentUser.value
                if (current != null) {
                    currentUser.value = current.copy(profilePhotoUri = url)
                }
                Result.success(url)
            } else {
                Result.failure(Exception("Failed to upload photo: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProfilePhoto(): Result<Unit> {
        return try {
            val response = apiService.deleteProfilePhoto()
            if (response.isSuccessful) {
                val current = currentUser.value
                if (current != null) {
                    currentUser.value = current.copy(profilePhotoUri = null)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete photo: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            val req = com.hostelcare.app.data.remote.UpdateProfileRequest(
                name = user.name,
                email = user.email,
                hostelBlock = user.hostelBlock,
                roomNumber = user.roomNumber,
                phone = user.phone
            )
            val response = apiService.updateProfile(req)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!.user.toLocalUser()
                currentUser.value = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Update failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

        override suspend fun refreshComplaints() {
        try {
            val response = apiService.getComplaints()
            if (response.isSuccessful && response.body() != null) {
                val complaints = response.body()!!.complaints.map { it.toLocalComplaint() }
                _complaints.value = complaints
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

        override suspend fun triggerAiAnalysis(complaintId: String): Result<Unit> {
        return try {
            val req = com.hostelcare.app.data.remote.AiAnalyzeRequest(complaintId = complaintId, title = "", description = "", category = null)
            val response = apiService.analyzeComplaint(req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("AI analysis failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchComplaint(id: String): Result<Complaint> {
        return try {
            val response = apiService.getComplaint(id)
            if (response.isSuccessful && response.body() != null) {
                val complaint = response.body()!!.complaint.toLocalComplaint()
                // Update local list
                val current = _complaints.value.toMutableList()
                val index = current.indexOfFirst { it.id == id }
                if (index != -1) current[index] = complaint else current.add(complaint)
                _complaints.value = current
                Result.success(complaint)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitComplaint(complaint: Complaint, photoBytes: ByteArray?, mimeType: String?): Result<Complaint> {
        return try {
            if (photoBytes != null && mimeType != null) {
                val titleBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), complaint.title)
                val descBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), complaint.description)
                val catBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), complaint.category.name)
                val prioBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), complaint.priority.name)
                val blockBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), complaint.hostelBlock)
                val roomBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), complaint.roomNumber)
                val aiBody = complaint.aiSummary?.let { okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), it) }
                
                val requestFile = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull() /* okhttp3 uses parse, or we can use extension. Let us fix it securely: */ ?: "image/jpeg".toMediaTypeOrNull(), photoBytes)
                val ext = if (mimeType.contains("png")) "png" else if (mimeType.contains("webp")) "webp" else "jpg"
                val photoPart = okhttp3.MultipartBody.Part.createFormData("photo", "upload." + ext, requestFile)

                val response = apiService.createComplaintWithPhoto(
                    titleBody, descBody, catBody, prioBody, blockBody, roomBody, aiBody, photoPart
                )
                if (response.isSuccessful && response.body() != null) {
                    val created = response.body()!!.complaint.toLocalComplaint()
                    _complaints.update { listOf(created) + it }
                    Result.success(created)
                } else {
                    
                    val code = response.code()
                    val msg = when (code) {
                        400 -> "Please check the complaint details."
                        401 -> "Your session has expired. Please log in again."
                        500 -> "Something went wrong on the server. Please try again."
                        else -> "Failed to submit complaint: $code"
                    }
                    Result.failure(Exception(msg))

                }
            } else {
                val request = com.hostelcare.app.data.remote.ComplaintRequest(
                    title = complaint.title,
                    description = complaint.description,
                    category = complaint.category.name,
                    priority = complaint.priority.name,
                    hostelBlock = complaint.hostelBlock,
                    roomNumber = complaint.roomNumber,
                    photoUrl = complaint.photoUri,
                    aiSummary = complaint.aiSummary
                )
                val response = apiService.createComplaint(request)
                if (response.isSuccessful && response.body() != null) {
                    val created = response.body()!!.complaint.toLocalComplaint()
                    _complaints.update { listOf(created) + it }
                    Result.success(created)
                } else {
                    
                    val code = response.code()
                    val msg = when (code) {
                        400 -> "Please check the complaint details."
                        401 -> "Your session has expired. Please log in again."
                        500 -> "Something went wrong on the server. Please try again."
                        else -> "Failed to submit complaint: $code"
                    }
                    Result.failure(Exception(msg))

                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateComplaintStatus(
        id: String,
        status: ComplaintStatus,
        resolutionNote: String?
    ): Result<Unit> {
        _complaints.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(status = status, resolutionNote = resolutionNote ?: it.resolutionNote)
                } else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun assignStaffToComplaint(complaintId: String, staffId: String): Result<Unit> {
        _complaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    it.copy(assignedStaffId = staffId, status = ComplaintStatus.ASSIGNED)
                } else it
            }
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
    override suspend fun analyzeComplaint(
        title: String,
        description: String,
        category: String?
    ): Result<com.hostelcare.app.data.model.AiAnalysisResult> {
        return try {
            val req = com.hostelcare.app.data.remote.AiAnalyzeRequest(title = title, description = description, category = category)
            val response = apiService.analyzeComplaint(req)
            if (response.isSuccessful) {
                val data = response.body()?.analysis
                if (data != null) {
                    val catEnum = try { com.hostelcare.app.data.model.ComplaintCategory.valueOf(data.category.uppercase()) } catch(e: Exception) { com.hostelcare.app.data.model.ComplaintCategory.OTHER }
                    val prioEnum = try { com.hostelcare.app.data.model.ComplaintPriority.valueOf(data.priority.uppercase()) } catch(e: Exception) { com.hostelcare.app.data.model.ComplaintPriority.MEDIUM }
                    Result.success(com.hostelcare.app.data.model.AiAnalysisResult(catEnum, prioEnum, data.summary))
                } else {
                    Result.failure(Exception("Empty AI analysis result"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "AI analysis failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
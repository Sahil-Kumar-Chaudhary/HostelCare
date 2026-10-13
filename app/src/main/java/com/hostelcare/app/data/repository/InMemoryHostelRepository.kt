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

    override suspend fun registerStudent(user: User, password: String, confirmPassword: String, acceptedTerms: Boolean): Result<User> {
        return try {
            val req = com.hostelcare.app.data.remote.RegisterRequest(
                name = user.name,
                studentId = user.studentId,
                email = user.email,
                phone = user.phone,
                collegeName = user.collegeName,
                course = user.course,
                currentYear = user.currentYear,
                semester = user.semester,
                hostelName = user.hostelName,
                hostelBlock = user.hostelBlock,
                roomNumber = user.roomNumber,
                password = password,
                confirmPassword = confirmPassword,
                acceptedTerms = acceptedTerms
            )
            val response = apiService.register(req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.user.toLocalUser())
            } else {
                val errorMsg = try {
                    val errBody = response.errorBody()?.string()
                    if (!errBody.isNullOrBlank()) {
                        val json = org.json.JSONObject(errBody)
                        json.optString("message", "Registration failed: ${response.message()}")
                    } else "Registration failed: ${response.message()}"
                } catch (e: Exception) {
                    "Registration failed: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
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
            val reqFile = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), photoBytes)
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
                phone = user.phone,
                collegeName = user.collegeName,
                course = user.course,
                currentYear = user.currentYear,
                semester = user.semester,
                hostelName = user.hostelName
            )
            val response = apiService.updateProfile(req)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!.user.toLocalUser()
                currentUser.value = updated
                Result.success(updated)
            } else {
                val errorJsonMsg = try {
                    response.errorBody()?.string()?.let { org.json.JSONObject(it).optString("message", "") }
                } catch (e: Exception) { null }
                Result.failure(Exception(errorJsonMsg?.takeIf { it.isNotBlank() } ?: "Update failed: ${response.message()}"))
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
                    val errorJsonMsg = try {
                        response.errorBody()?.string()?.let { org.json.JSONObject(it).optString("message", "") }
                    } catch (e: Exception) { null }
                    val code = response.code()
                    val msg = errorJsonMsg?.takeIf { it.isNotBlank() } ?: when (code) {
                        400 -> "Please check the complaint details."
                        401 -> "Your session has expired. Please log in again."
                        403 -> "You are not authorized to submit this complaint."
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
                    val errorJsonMsg = try {
                        response.errorBody()?.string()?.let { org.json.JSONObject(it).optString("message", "") }
                    } catch (e: Exception) { null }
                    val code = response.code()
                    val msg = errorJsonMsg?.takeIf { it.isNotBlank() } ?: when (code) {
                        400 -> "Please check the complaint details."
                        401 -> "Your session has expired. Please log in again."
                        403 -> "You are not authorized to submit this complaint."
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

    override suspend fun getWardenDashboard(): Result<com.hostelcare.app.data.remote.WardenDashboardResponse> {
        return try {
            val response = apiService.getWardenDashboard()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load dashboard: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWardenComplaints(
        search: String?,
        status: String?,
        category: String?,
        priority: String?
    ): Result<List<com.hostelcare.app.data.remote.WardenRecentComplaint>> {
        return try {
            val response = apiService.getWardenComplaints(search, status, category, priority)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.complaints)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load complaints: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWardenComplaintDetail(id: String): Result<com.hostelcare.app.data.remote.WardenComplaintDetail> {
        return try {
            val response = apiService.getWardenComplaintDetail(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.complaint)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load complaint details: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWardenComplaintStatus(
        id: String,
        status: String,
        note: String?,
        resolutionNote: String?
    ): Result<Unit> {
        return try {
            val req = com.hostelcare.app.data.remote.UpdateComplaintStatusRequest(
                status = status,
                note = note,
                resolutionNote = resolutionNote
            )
            val response = apiService.updateWardenComplaintStatus(id, req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to update complaint status: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun assignWardenStaff(
        id: String,
        staffId: String,
        instructions: String?
    ): Result<Unit> {
        return try {
            val req = com.hostelcare.app.data.remote.AssignStaffRequest(
                staffId = staffId,
                instructions = instructions
            )
            val response = apiService.assignWardenStaff(id, req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to assign staff: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWardenStaff(
        filter: String?,
        search: String?
    ): Result<List<com.hostelcare.app.data.remote.WardenStaffItem>> {
        return try {
            val response = apiService.getWardenStaff(filter, search)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.staff)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load staff list: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createWardenStaff(
        request: com.hostelcare.app.data.remote.CreateStaffRequest
    ): Result<Unit> {
        return try {
            val response = apiService.createWardenStaff(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to create staff: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWardenStaffDetail(id: String): Result<com.hostelcare.app.data.remote.WardenStaffDetail> {
        return try {
            val response = apiService.getWardenStaffDetail(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.staff)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load staff details: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWardenStaff(
        id: String,
        request: com.hostelcare.app.data.remote.UpdateStaffRequest
    ): Result<Unit> {
        return try {
            val response = apiService.updateWardenStaff(id, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to update staff: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleWardenStaffStatus(id: String, isActive: Boolean): Result<Unit> {
        return try {
            val response = apiService.toggleWardenStaffStatus(id, com.hostelcare.app.data.remote.ToggleStaffStatusRequest(isActive))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to update staff status: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWardenReports(timeRange: String?): Result<com.hostelcare.app.data.remote.WardenReportsResponse> {
        return try {
            val response = apiService.getWardenReports(timeRange)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load reports: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWardenProfile(): Result<com.hostelcare.app.data.remote.WardenProfileData> {
        return try {
            val response = apiService.getWardenProfile()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.warden)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load warden profile: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWardenProfile(name: String?, phone: String?): Result<Unit> {
        return try {
            val req = com.hostelcare.app.data.remote.UpdateWardenProfileRequest(name = name, phone = phone)
            val response = apiService.updateWardenProfile(req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to update profile: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val req = com.hostelcare.app.data.remote.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = newPassword
            )
            val response = apiService.changePassword(req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to update password: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStaffTasks(status: String?): Result<com.hostelcare.app.data.remote.StaffTasksResponse> {
        return try {
            val response = apiService.getStaffTasks(status)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load tasks: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStaffTaskDetail(id: String): Result<com.hostelcare.app.data.remote.StaffTaskDetail> {
        return try {
            val response = apiService.getStaffTaskDetail(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.task)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load task details: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStaffTaskStatus(id: String, status: String?, workNotes: String?): Result<Unit> {
        return try {
            val req = com.hostelcare.app.data.remote.UpdateStaffTaskStatusRequest(status = status, workNotes = workNotes)
            val response = apiService.updateStaffTaskStatus(id, req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to update task: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeStaffTask(id: String, completionNotes: String?, completionPhotoUrl: String?): Result<Unit> {
        return try {
            val req = com.hostelcare.app.data.remote.CompleteStaffTaskRequest(
                completionNotes = completionNotes,
                completionPhotoUrl = completionPhotoUrl
            )
            val response = apiService.completeStaffTask(id, req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to submit completion: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInAppNotifications(): Result<com.hostelcare.app.data.remote.InAppNotificationsResponse> {
        return try {
            val response = apiService.getInAppNotifications()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Failed to load notifications: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Unable to connect to the server. Please check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markInAppNotificationRead(id: String): Result<Unit> {
        return try {
            val response = apiService.markInAppNotificationRead(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark notification as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllInAppNotificationsRead(): Result<Unit> {
        return try {
            val response = apiService.markAllInAppNotificationsRead()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark all notifications as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            val json = org.json.JSONObject(errorBody)
            json.optString("message", fallback)
        } catch (e: Exception) {
            fallback
        }
    }
}
package com.hostelcare.app.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelcare.app.ai.AiComplaintAnalyzer
import com.hostelcare.app.data.model.*
import com.hostelcare.app.data.remote.InAppNotificationItem
import com.hostelcare.app.data.repository.HostelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentViewModel(
    private val repository: HostelRepository,
    private val aiAnalyzer: AiComplaintAnalyzer
) : ViewModel() {

    val currentUser = repository.currentUserFlow()

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    val complaints: StateFlow<List<Complaint>> = _complaints.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _notificationsState = MutableStateFlow<StudentNotificationsUiState>(StudentNotificationsUiState.Loading)
    val notificationsState: StateFlow<StudentNotificationsUiState> = _notificationsState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _newComplaintState = MutableStateFlow<NewComplaintState>(NewComplaintState.Idle)
    val newComplaintState: StateFlow<NewComplaintState> = _newComplaintState.asStateFlow()
    
    private val _draftComplaint = MutableStateFlow<Complaint?>(null)
    val draftComplaint: StateFlow<Complaint?> = _draftComplaint.asStateFlow()

    val staffList = repository.getAvailableStaff()
    var selectedPhotoBytes: ByteArray? = null
    var selectedPhotoMimeType: String? = null

    init {
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                if (user != null) {
                    launch { repository.refreshComplaints() }
                    launch {
                        repository.getComplaintsForStudent(user.id).collect {
                            _complaints.value = it
                        }
                    }
                    launch {
                        repository.getNotificationsForUser(user.id).collect {
                            _notifications.value = it
                        }
                    }
                    launch {
                        loadNotifications()
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun startNewComplaint(complaint: Complaint) {
        _draftComplaint.value = complaint
        _newComplaintState.value = NewComplaintState.Idle
    }

    fun analyzeComplaint() {
        val draft = _draftComplaint.value ?: return
        viewModelScope.launch {
            _newComplaintState.value = NewComplaintState.Analyzing
            val result = repository.analyzeComplaint(draft.title, draft.description, draft.category.name)
            if (result.isSuccess) {
                val analysis = result.getOrNull()!!
                _draftComplaint.value = draft.copy(
                    category = analysis.category,
                    priority = analysis.priority,
                    aiSummary = analysis.summary
                )
                _newComplaintState.value = NewComplaintState.AnalysisComplete(analysis)
            } else {
                _newComplaintState.value = NewComplaintState.Error(result.exceptionOrNull()?.message ?: "AI analysis is currently unavailable. Please try again.")
            }
        }
    }

    fun submitComplaint() {
        val draft = _draftComplaint.value ?: return
        viewModelScope.launch {
            _newComplaintState.value = NewComplaintState.Submitting
            val result = repository.submitComplaint(draft, selectedPhotoBytes, selectedPhotoMimeType)
            if (result.isSuccess) { selectedPhotoBytes = null; selectedPhotoMimeType = null }
            if (result.isSuccess) {
                val createdComplaint = result.getOrNull()
                if (createdComplaint != null) {
                    _newComplaintState.value = NewComplaintState.Success(createdComplaint.id)
                    _draftComplaint.value = null
                    repository.refreshComplaints()
                    loadNotifications()
                    triggerAiAnalysis(createdComplaint.id)
                } else {
                    _newComplaintState.value = NewComplaintState.Error("Submission failed")
                }
            } else {
                _newComplaintState.value = NewComplaintState.Error(result.exceptionOrNull()?.message ?: "Submission failed")
            }
        }
    }

    private val _complaintDetailsState = MutableStateFlow<ComplaintDetailsState>(ComplaintDetailsState.Loading)
    val complaintDetailsState: StateFlow<ComplaintDetailsState> = _complaintDetailsState.asStateFlow()

    fun triggerAiAnalysis(complaintId: String, forceRetry: Boolean = false) {
        viewModelScope.launch {
            val result = repository.triggerAiAnalysis(complaintId, forceRetry)
            if (result.isSuccess) {
                // refresh details
                fetchComplaintDetails(complaintId)
                repository.refreshComplaints()
            }
        }
    }

    fun fetchComplaintDetails(id: String) {
        viewModelScope.launch {
            _complaintDetailsState.value = ComplaintDetailsState.Loading
            val result = repository.fetchComplaint(id)
            if (result.isSuccess) {
                _complaintDetailsState.value = ComplaintDetailsState.Success(result.getOrNull()!!)
            } else {
                _complaintDetailsState.value = ComplaintDetailsState.Error(result.exceptionOrNull()?.message ?: "Failed to load complaint")
            }
        }
    }

suspend fun refreshComplaintDetails(id: String): Boolean {
        val result = repository.fetchComplaint(id)
        if (result.isSuccess) {
            _complaintDetailsState.value = ComplaintDetailsState.Success(result.getOrNull()!!)
            repository.refreshComplaints()
            return true
        }
        return false
    }

    fun withdrawComplaint(complaintId: String, reason: String? = null, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.withdrawComplaint(complaintId, reason)
            if (result.isSuccess) {
                val updated = result.getOrNull()!!
                _complaintDetailsState.value = ComplaintDetailsState.Success(updated)
                repository.refreshComplaints()
                onResult(true, "Complaint withdrawn successfully")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to withdraw complaint"
                onResult(false, msg)
            }
        }
    }

    fun resetComplaintState() {
        _newComplaintState.value = NewComplaintState.Idle
        _draftComplaint.value = null
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
            repository.markInAppNotificationRead(id)
            loadNotifications()
        }
    }

    fun markInAppNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markInAppNotificationRead(id)
            loadNotifications()
        }
    }

    fun markAllInAppNotificationsRead() {
        viewModelScope.launch {
            repository.markAllInAppNotificationsRead()
            loadNotifications()
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = StudentNotificationsUiState.Loading
            val result = repository.getInAppNotifications()
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    _unreadCount.value = data.unreadCount
                    if (data.notifications.isEmpty()) {
                        _notificationsState.value = StudentNotificationsUiState.Empty
                    } else {
                        _notificationsState.value = StudentNotificationsUiState.Success(data.notifications, data.unreadCount)
                    }
                } else {
                    _unreadCount.value = 0
                    _notificationsState.value = StudentNotificationsUiState.Empty
                }
            } else {
                _notificationsState.value = StudentNotificationsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load notifications"
                )
            }
        }
    }

    fun submitFeedback(complaintId: String, rating: Int, note: String) {
        viewModelScope.launch {
            repository.submitFeedback(complaintId, rating, note)
        }
    }

    private val _updateProfileState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateProfileState: StateFlow<UpdateProfileState> = _updateProfileState.asStateFlow()

        var profilePhotoBytes: ByteArray? = null
    var profilePhotoMimeType: String? = null
    var removeProfilePhoto: Boolean = false

    fun updateProfile(
        name: String,
        email: String,
        hostelBlock: String,
        roomNumber: String,
        phone: String = "",
        collegeName: String? = null,
        course: String? = null,
        currentYear: String? = null,
        semester: String? = null,
        hostelName: String? = null
    ) {
        viewModelScope.launch {
            _updateProfileState.value = UpdateProfileState.Loading
            val current = repository.getCurrentUser()
            if (current == null) {
                _updateProfileState.value = UpdateProfileState.Error("Session expired")
                return@launch
            }
            val updated = current.copy(
                name = name,
                email = email,
                hostelBlock = hostelBlock,
                roomNumber = roomNumber,
                phone = phone,
                collegeName = collegeName ?: current.collegeName,
                course = course ?: current.course,
                currentYear = currentYear ?: current.currentYear,
                semester = semester ?: current.semester,
                hostelName = hostelName ?: current.hostelName
            )
            val result = repository.updateUser(updated)
            
            if (result.isSuccess) {
                var photoError: String? = null
                if (removeProfilePhoto) {
                    val r = repository.deleteProfilePhoto()
                    if (r.isFailure) photoError = r.exceptionOrNull()?.message
                } else if (profilePhotoBytes != null && profilePhotoMimeType != null) {
                    val r = repository.updateProfilePhoto(profilePhotoBytes!!, profilePhotoMimeType!!)
                    if (r.isFailure) photoError = r.exceptionOrNull()?.message
                }
                
                profilePhotoBytes = null
                profilePhotoMimeType = null
                removeProfilePhoto = false
                
                if (photoError == null) {
                    _updateProfileState.value = UpdateProfileState.Success
                } else {
                    _updateProfileState.value = UpdateProfileState.Error("Profile updated, but photo failed: $photoError")
                }
            } else {
                _updateProfileState.value = UpdateProfileState.Error(result.exceptionOrNull()?.message ?: "Update failed")
            }
        }
    }

    fun uploadProfilePhotoDirect(bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _updateProfileState.value = UpdateProfileState.Loading
            val r = repository.updateProfilePhoto(bytes, mimeType)
            if (r.isSuccess) {
                _updateProfileState.value = UpdateProfileState.Success
            } else {
                _updateProfileState.value = UpdateProfileState.Error(r.exceptionOrNull()?.message ?: "Failed to upload photo")
            }
        }
    }

    fun deleteProfilePhotoDirect() {
        viewModelScope.launch {
            _updateProfileState.value = UpdateProfileState.Loading
            val r = repository.deleteProfilePhoto()
            if (r.isSuccess) {
                _updateProfileState.value = UpdateProfileState.Success
            } else {
                _updateProfileState.value = UpdateProfileState.Error(r.exceptionOrNull()?.message ?: "Failed to remove photo")
            }
        }
    }

    fun resetUpdateState() {
        _updateProfileState.value = UpdateProfileState.Idle
    }
}

sealed class UpdateProfileState {
    object Idle : UpdateProfileState()
    object Loading : UpdateProfileState()
    object Success : UpdateProfileState()
    data class Error(val message: String) : UpdateProfileState()
}

sealed class NewComplaintState {
    object Idle : NewComplaintState()
    object Analyzing : NewComplaintState()
    data class AnalysisComplete(val result: com.hostelcare.app.data.model.AiAnalysisResult) : NewComplaintState()
    object Submitting : NewComplaintState()
    data class Success(val complaintId: String) : NewComplaintState()
    data class Error(val message: String) : NewComplaintState()
}
sealed class ComplaintDetailsState {
    object Loading : ComplaintDetailsState()
    data class Success(val complaint: com.hostelcare.app.data.model.Complaint) : ComplaintDetailsState()
    data class Error(val message: String) : ComplaintDetailsState()
}

sealed class StudentNotificationsUiState {
    object Loading : StudentNotificationsUiState()
    data class Success(val notifications: List<InAppNotificationItem>, val unreadCount: Int) : StudentNotificationsUiState()
    object Empty : StudentNotificationsUiState()
    data class Error(val message: String) : StudentNotificationsUiState()
}

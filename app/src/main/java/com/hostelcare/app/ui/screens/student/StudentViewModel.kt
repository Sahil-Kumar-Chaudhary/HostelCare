package com.hostelcare.app.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelcare.app.ai.AiComplaintAnalyzer
import com.hostelcare.app.data.model.*
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

    private val _newComplaintState = MutableStateFlow<NewComplaintState>(NewComplaintState.Idle)
    val newComplaintState: StateFlow<NewComplaintState> = _newComplaintState.asStateFlow()
    
    private val _draftComplaint = MutableStateFlow<Complaint?>(null)
    val draftComplaint: StateFlow<Complaint?> = _draftComplaint.asStateFlow()

    val staffList = repository.getAvailableStaff()

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
            val result = repository.submitComplaint(draft)
            if (result.isSuccess) {
                val createdComplaint = result.getOrNull()
                if (createdComplaint != null) {
                    _newComplaintState.value = NewComplaintState.Success(createdComplaint.id)
                    _draftComplaint.value = null
                    repository.refreshComplaints()
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

    fun resetComplaintState() {
        _newComplaintState.value = NewComplaintState.Idle
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun submitFeedback(complaintId: String, rating: Int, note: String) {
        viewModelScope.launch {
            repository.submitFeedback(complaintId, rating, note)
        }
    }

    private val _updateProfileState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateProfileState: StateFlow<UpdateProfileState> = _updateProfileState.asStateFlow()

    fun updateProfile(name: String, email: String, hostelBlock: String, roomNumber: String, phone: String = "", profilePhotoUri: String? = null) {
        viewModelScope.launch {
            _updateProfileState.value = UpdateProfileState.Loading
            val current = repository.getCurrentUser()
            if (current == null) {
                _updateProfileState.value = UpdateProfileState.Error("Session expired")
                return@launch
            }
            val updated = current.copy(name = name, email = email, hostelBlock = hostelBlock, roomNumber = roomNumber, phone = phone, profilePhotoUri = profilePhotoUri)
            val result = repository.updateUser(updated)
            if (result.isSuccess) {
                _updateProfileState.value = UpdateProfileState.Success
            } else {
                _updateProfileState.value = UpdateProfileState.Error(result.exceptionOrNull()?.message ?: "Update failed")
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

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
            val result = aiAnalyzer.analyzeComplaint(draft.title, draft.description, draft.category)
            if (result.isSuccess) {
                val analysis = result.getOrNull()!!
                _draftComplaint.value = draft.copy(
                    category = analysis.category,
                    priority = analysis.priority,
                    aiSummary = analysis.summary
                )
                _newComplaintState.value = NewComplaintState.AnalysisComplete(analysis)
            } else {
                _newComplaintState.value = NewComplaintState.Error("AI Analysis failed")
            }
        }
    }

    fun submitComplaint() {
        val draft = _draftComplaint.value ?: return
        viewModelScope.launch {
            _newComplaintState.value = NewComplaintState.Submitting
            val result = repository.submitComplaint(draft)
            if (result.isSuccess) {
                _newComplaintState.value = NewComplaintState.Success
                _draftComplaint.value = null
            } else {
                _newComplaintState.value = NewComplaintState.Error("Submission failed")
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

    fun updateProfile(name: String, email: String, hostelBlock: String, roomNumber: String, phone: String = "", profilePhotoUri: String? = null) {
        viewModelScope.launch {
            val current = repository.getCurrentUser() ?: return@launch
            val updated = current.copy(name = name, email = email, hostelBlock = hostelBlock, roomNumber = roomNumber, phone = phone, profilePhotoUri = profilePhotoUri)
            repository.updateUser(updated)
        }
    }
}

sealed class NewComplaintState {
    object Idle : NewComplaintState()
    object Analyzing : NewComplaintState()
    data class AnalysisComplete(val result: AiAnalysisResult) : NewComplaintState()
    object Submitting : NewComplaintState()
    object Success : NewComplaintState()
    data class Error(val message: String) : NewComplaintState()
}

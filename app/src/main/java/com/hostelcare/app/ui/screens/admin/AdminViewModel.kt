package com.hostelcare.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelcare.app.data.model.Complaint
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.data.model.Staff
import com.hostelcare.app.data.repository.HostelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: HostelRepository) : ViewModel() {

    val currentUser = repository.currentUserFlow()

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    val complaints: StateFlow<List<Complaint>> = _complaints.asStateFlow()

    private val _staffList = MutableStateFlow<List<Staff>>(emptyList())
    val staffList: StateFlow<List<Staff>> = _staffList.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllComplaints().collect {
                _complaints.value = it
            }
        }
        viewModelScope.launch {
            repository.getAvailableStaff().collect {
                _staffList.value = it
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun assignStaff(complaintId: String, staffId: String) {
        viewModelScope.launch {
            repository.assignStaffToComplaint(complaintId, staffId)
        }
    }

    fun updateComplaintStatus(complaintId: String, status: ComplaintStatus, resolutionNote: String? = null) {
        viewModelScope.launch {
            repository.updateComplaintStatus(complaintId, status, resolutionNote)
        }
    }
}

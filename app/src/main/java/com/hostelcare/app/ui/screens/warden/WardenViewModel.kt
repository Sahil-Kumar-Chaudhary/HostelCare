package com.hostelcare.app.ui.screens.warden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelcare.app.data.remote.*
import com.hostelcare.app.data.repository.HostelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WardenDashboardUiState {
    object Loading : WardenDashboardUiState()
    data class Success(val data: WardenDashboardResponse) : WardenDashboardUiState()
    data class Empty(val message: String = "No complaints found in your assigned hostel.") : WardenDashboardUiState()
    data class Error(val message: String) : WardenDashboardUiState()
}

sealed class WardenComplaintsUiState {
    object Loading : WardenComplaintsUiState()
    data class Success(val complaints: List<WardenRecentComplaint>) : WardenComplaintsUiState()
    data class Empty(val message: String = "No complaints found matching criteria.") : WardenComplaintsUiState()
    data class Error(val message: String) : WardenComplaintsUiState()
}

sealed class WardenComplaintDetailUiState {
    object Loading : WardenComplaintDetailUiState()
    data class Success(val complaint: WardenComplaintDetail) : WardenComplaintDetailUiState()
    data class Error(val message: String) : WardenComplaintDetailUiState()
}

sealed class WardenStaffListUiState {
    object Loading : WardenStaffListUiState()
    data class Success(val staff: List<WardenStaffItem>) : WardenStaffListUiState()
    data class Empty(val message: String = "No maintenance staff registered.") : WardenStaffListUiState()
    data class Error(val message: String) : WardenStaffListUiState()
}

sealed class WardenStaffDetailUiState {
    object Loading : WardenStaffDetailUiState()
    data class Success(val staff: WardenStaffDetail) : WardenStaffDetailUiState()
    data class Error(val message: String) : WardenStaffDetailUiState()
}

sealed class WardenReportsUiState {
    object Loading : WardenReportsUiState()
    data class Success(val reports: WardenReportsResponse) : WardenReportsUiState()
    data class Error(val message: String) : WardenReportsUiState()
}

sealed class WardenProfileUiState {
    object Loading : WardenProfileUiState()
    data class Success(val profile: WardenProfileData) : WardenProfileUiState()
    data class Error(val message: String) : WardenProfileUiState()
}

sealed class WardenNotificationsUiState {
    object Loading : WardenNotificationsUiState()
    data class Success(val notifications: List<InAppNotificationItem>, val unreadCount: Int) : WardenNotificationsUiState()
    data class Error(val message: String) : WardenNotificationsUiState()
}

class WardenViewModel(private val repository: HostelRepository) : ViewModel() {
    val currentUser = repository.currentUserFlow()

    // Dashboard State
    private val _uiState = MutableStateFlow<WardenDashboardUiState>(WardenDashboardUiState.Loading)
    val uiState: StateFlow<WardenDashboardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Complaints List State
    private val _complaintsState = MutableStateFlow<WardenComplaintsUiState>(WardenComplaintsUiState.Loading)
    val complaintsState: StateFlow<WardenComplaintsUiState> = _complaintsState.asStateFlow()

    // Complaint Details State
    private val _complaintDetailState = MutableStateFlow<WardenComplaintDetailUiState>(WardenComplaintDetailUiState.Loading)
    val complaintDetailState: StateFlow<WardenComplaintDetailUiState> = _complaintDetailState.asStateFlow()

    // Staff List State
    private val _staffListState = MutableStateFlow<WardenStaffListUiState>(WardenStaffListUiState.Loading)
    val staffListState: StateFlow<WardenStaffListUiState> = _staffListState.asStateFlow()

    // Staff Detail State
    private val _staffDetailState = MutableStateFlow<WardenStaffDetailUiState>(WardenStaffDetailUiState.Loading)
    val staffDetailState: StateFlow<WardenStaffDetailUiState> = _staffDetailState.asStateFlow()

    // Reports State
    private val _reportsState = MutableStateFlow<WardenReportsUiState>(WardenReportsUiState.Loading)
    val reportsState: StateFlow<WardenReportsUiState> = _reportsState.asStateFlow()

    // Profile State
    private val _profileState = MutableStateFlow<WardenProfileUiState>(WardenProfileUiState.Loading)
    val profileState: StateFlow<WardenProfileUiState> = _profileState.asStateFlow()

    // Notifications State
    private val _notificationsState = MutableStateFlow<WardenNotificationsUiState>(WardenNotificationsUiState.Loading)
    val notificationsState: StateFlow<WardenNotificationsUiState> = _notificationsState.asStateFlow()

    init {
        loadDashboard()
    }

    // 1. Dashboard
    fun loadDashboard(isExplicitRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isExplicitRefresh) {
                _isRefreshing.value = true
            } else if (_uiState.value !is WardenDashboardUiState.Success) {
                _uiState.value = WardenDashboardUiState.Loading
            }

            val result = repository.getWardenDashboard()
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data == null || (data.stats.total == 0 && data.recentComplaints.isEmpty())) {
                    _uiState.value = WardenDashboardUiState.Empty()
                } else {
                    _uiState.value = WardenDashboardUiState.Success(data)
                }
            } else {
                _uiState.value = WardenDashboardUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load dashboard data."
                )
            }

            if (isExplicitRefresh) {
                _isRefreshing.value = false
            }
        }
    }

    // 2. Complaints List
    fun loadComplaints(search: String? = null, status: String? = null, category: String? = null, priority: String? = null) {
        viewModelScope.launch {
            _complaintsState.value = WardenComplaintsUiState.Loading
            val result = repository.getWardenComplaints(search, status, category, priority)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                if (list.isEmpty()) {
                    _complaintsState.value = WardenComplaintsUiState.Empty()
                } else {
                    _complaintsState.value = WardenComplaintsUiState.Success(list)
                }
            } else {
                _complaintsState.value = WardenComplaintsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load complaints."
                )
            }
        }
    }

    // 3. Complaint Details
    fun loadComplaintDetail(id: String) {
        viewModelScope.launch {
            _complaintDetailState.value = WardenComplaintDetailUiState.Loading
            val result = repository.getWardenComplaintDetail(id)
            if (result.isSuccess) {
                val complaint = result.getOrNull()
                if (complaint != null) {
                    _complaintDetailState.value = WardenComplaintDetailUiState.Success(complaint)
                } else {
                    _complaintDetailState.value = WardenComplaintDetailUiState.Error("Complaint not found.")
                }
            } else {
                _complaintDetailState.value = WardenComplaintDetailUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load complaint details."
                )
            }
        }
    }

    fun updateComplaintStatus(
        id: String,
        status: String,
        note: String?,
        resolutionNote: String?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateWardenComplaintStatus(id, status, note, resolutionNote)
            if (result.isSuccess) {
                loadComplaintDetail(id)
                loadDashboard(true)
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to update status")
            }
        }
    }

    fun assignStaff(
        complaintId: String,
        staffId: String,
        instructions: String?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.assignWardenStaff(complaintId, staffId, instructions)
            if (result.isSuccess) {
                loadComplaintDetail(complaintId)
                loadDashboard(true)
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to assign staff")
            }
        }
    }

    // 4. Staff Management
    fun loadStaffList(filter: String? = null, search: String? = null) {
        viewModelScope.launch {
            _staffListState.value = WardenStaffListUiState.Loading
            val result = repository.getWardenStaff(filter, search)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                if (list.isEmpty()) {
                    _staffListState.value = WardenStaffListUiState.Empty()
                } else {
                    _staffListState.value = WardenStaffListUiState.Success(list)
                }
            } else {
                _staffListState.value = WardenStaffListUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load staff list."
                )
            }
        }
    }

    fun createStaff(request: CreateStaffRequest, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.createWardenStaff(request)
            if (result.isSuccess) {
                loadStaffList()
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to create staff member")
            }
        }
    }

    // 5. Staff Detail
    fun loadStaffDetail(id: String) {
        viewModelScope.launch {
            _staffDetailState.value = WardenStaffDetailUiState.Loading
            val result = repository.getWardenStaffDetail(id)
            if (result.isSuccess) {
                val staff = result.getOrNull()
                if (staff != null) {
                    _staffDetailState.value = WardenStaffDetailUiState.Success(staff)
                } else {
                    _staffDetailState.value = WardenStaffDetailUiState.Error("Staff member not found.")
                }
            } else {
                _staffDetailState.value = WardenStaffDetailUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load staff details."
                )
            }
        }
    }

    fun updateStaff(id: String, request: UpdateStaffRequest, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateWardenStaff(id, request)
            if (result.isSuccess) {
                loadStaffDetail(id)
                loadStaffList()
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to update staff member")
            }
        }
    }

    fun toggleStaffStatus(id: String, isActive: Boolean, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.toggleWardenStaffStatus(id, isActive)
            if (result.isSuccess) {
                loadStaffDetail(id)
                loadStaffList()
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to update status")
            }
        }
    }

    // 6. Reports & Analytics
    fun loadReports(timeRange: String? = "30days") {
        viewModelScope.launch {
            _reportsState.value = WardenReportsUiState.Loading
            val result = repository.getWardenReports(timeRange)
            if (result.isSuccess) {
                val reports = result.getOrNull()
                if (reports != null) {
                    _reportsState.value = WardenReportsUiState.Success(reports)
                } else {
                    _reportsState.value = WardenReportsUiState.Error("Failed to parse reports.")
                }
            } else {
                _reportsState.value = WardenReportsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load reports."
                )
            }
        }
    }

    // 7. Profile & Settings
    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = WardenProfileUiState.Loading
            val result = repository.getWardenProfile()
            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    _profileState.value = WardenProfileUiState.Success(profile)
                } else {
                    _profileState.value = WardenProfileUiState.Error("Failed to parse profile.")
                }
            } else {
                _profileState.value = WardenProfileUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load profile."
                )
            }
        }
    }

    fun updateProfile(name: String?, phone: String?, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateWardenProfile(name, phone)
            if (result.isSuccess) {
                loadProfile()
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to update profile")
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.changePassword(currentPassword, newPassword)
            if (result.isSuccess) {
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to update password")
            }
        }
    }

    // 8. Notifications
    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = WardenNotificationsUiState.Loading
            val result = repository.getInAppNotifications()
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    _notificationsState.value = WardenNotificationsUiState.Success(data.notifications, data.unreadCount)
                } else {
                    _notificationsState.value = WardenNotificationsUiState.Success(emptyList(), 0)
                }
            } else {
                _notificationsState.value = WardenNotificationsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load notifications"
                )
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markInAppNotificationRead(id)
            loadNotifications()
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllInAppNotificationsRead()
            loadNotifications()
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }
}

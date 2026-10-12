package com.hostelcare.app.ui.screens.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelcare.app.data.remote.StaffStatsData
import com.hostelcare.app.data.remote.StaffTaskDetail
import com.hostelcare.app.data.remote.StaffTaskItem
import com.hostelcare.app.data.repository.HostelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StaffTasksUiState {
    object Loading : StaffTasksUiState()
    data class Success(val tasks: List<StaffTaskItem>, val stats: StaffStatsData) : StaffTasksUiState()
    data class Empty(val message: String = "No tasks currently assigned.") : StaffTasksUiState()
    data class Error(val message: String) : StaffTasksUiState()
}

sealed class StaffTaskDetailUiState {
    object Loading : StaffTaskDetailUiState()
    data class Success(val task: StaffTaskDetail) : StaffTaskDetailUiState()
    data class Error(val message: String) : StaffTaskDetailUiState()
}

class StaffViewModel(private val repository: HostelRepository) : ViewModel() {
    val currentUser = repository.currentUserFlow()

    private val _tasksState = MutableStateFlow<StaffTasksUiState>(StaffTasksUiState.Loading)
    val tasksState: StateFlow<StaffTasksUiState> = _tasksState.asStateFlow()

    private val _taskDetailState = MutableStateFlow<StaffTaskDetailUiState>(StaffTaskDetailUiState.Loading)
    val taskDetailState: StateFlow<StaffTaskDetailUiState> = _taskDetailState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks(status: String? = null, isExplicitRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isExplicitRefresh) {
                _isRefreshing.value = true
            } else if (_tasksState.value !is StaffTasksUiState.Success) {
                _tasksState.value = StaffTasksUiState.Loading
            }

            val result = repository.getStaffTasks(status)
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data == null || data.tasks.isEmpty()) {
                    _tasksState.value = StaffTasksUiState.Empty()
                } else {
                    _tasksState.value = StaffTasksUiState.Success(data.tasks, data.stats)
                }
            } else {
                _tasksState.value = StaffTasksUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load assigned tasks."
                )
            }

            if (isExplicitRefresh) {
                _isRefreshing.value = false
            }
        }
    }

    fun loadTaskDetail(id: String) {
        viewModelScope.launch {
            _taskDetailState.value = StaffTaskDetailUiState.Loading
            val result = repository.getStaffTaskDetail(id)
            if (result.isSuccess) {
                val task = result.getOrNull()
                if (task != null) {
                    _taskDetailState.value = StaffTaskDetailUiState.Success(task)
                } else {
                    _taskDetailState.value = StaffTaskDetailUiState.Error("Task not found.")
                }
            } else {
                _taskDetailState.value = StaffTaskDetailUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load task details."
                )
            }
        }
    }

    fun updateTaskProgress(id: String, status: String?, workNotes: String?, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateStaffTaskStatus(id, status, workNotes)
            if (result.isSuccess) {
                loadTaskDetail(id)
                loadTasks(isExplicitRefresh = true)
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to update task progress")
            }
        }
    }

    fun submitCompletion(id: String, completionNotes: String?, completionPhotoUrl: String?, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.completeStaffTask(id, completionNotes, completionPhotoUrl)
            if (result.isSuccess) {
                loadTaskDetail(id)
                loadTasks(isExplicitRefresh = true)
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to submit completion")
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }
}

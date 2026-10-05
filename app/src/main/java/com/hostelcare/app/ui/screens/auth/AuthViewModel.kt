package com.hostelcare.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelcare.app.data.model.User
import com.hostelcare.app.data.repository.HostelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: HostelRepository) : ViewModel() {
    val currentUser = repository.currentUserFlow()
    val isSessionChecked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.getCurrentUser()
            isSessionChecked.value = true
        }
    }

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun login(email: String, password: String, asAdmin: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            val result = repository.login(email, password)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(user: User, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            val result = repository.registerStudent(user, password)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

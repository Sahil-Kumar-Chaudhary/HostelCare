package com.hostelcare.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hostelcare.app.ai.AiComplaintAnalyzer
import com.hostelcare.app.data.repository.HostelRepository
import com.hostelcare.app.ui.screens.admin.AdminViewModel
import com.hostelcare.app.ui.screens.auth.AuthViewModel
import com.hostelcare.app.ui.screens.student.StudentViewModel

class AppViewModelFactory(
    private val repository: HostelRepository,
    private val aiAnalyzer: AiComplaintAnalyzer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentViewModel(repository, aiAnalyzer) as T
        }
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

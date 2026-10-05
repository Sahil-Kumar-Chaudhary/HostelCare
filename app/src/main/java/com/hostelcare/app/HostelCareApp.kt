package com.hostelcare.app

import android.app.Application
import com.hostelcare.app.ai.AiComplaintAnalyzer
import com.hostelcare.app.ai.MockAiComplaintAnalyzer
import com.hostelcare.app.data.remote.RetrofitClient
import com.hostelcare.app.data.remote.TokenManager
import com.hostelcare.app.data.repository.HostelRepository
import com.hostelcare.app.data.repository.InMemoryHostelRepository

class HostelCareApp : Application() {
    lateinit var repository: HostelRepository
    lateinit var aiAnalyzer: AiComplaintAnalyzer
    lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        val apiService = RetrofitClient.create(tokenManager)
        
        repository = InMemoryHostelRepository(apiService, tokenManager)
        aiAnalyzer = MockAiComplaintAnalyzer()
    }
}
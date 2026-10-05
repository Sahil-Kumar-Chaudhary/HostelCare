package com.hostelcare.app

import android.app.Application
import com.hostelcare.app.ai.AiComplaintAnalyzer
import com.hostelcare.app.ai.MockAiComplaintAnalyzer
import com.hostelcare.app.data.repository.HostelRepository
import com.hostelcare.app.data.repository.InMemoryHostelRepository

class HostelCareApp : Application() {
    lateinit var repository: HostelRepository
    lateinit var aiAnalyzer: AiComplaintAnalyzer

    override fun onCreate() {
        super.onCreate()
        repository = InMemoryHostelRepository()
        aiAnalyzer = MockAiComplaintAnalyzer()
    }
}

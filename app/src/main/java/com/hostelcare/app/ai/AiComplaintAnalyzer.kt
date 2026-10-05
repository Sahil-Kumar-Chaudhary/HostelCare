package com.hostelcare.app.ai

import com.hostelcare.app.data.model.AiAnalysisResult
import com.hostelcare.app.data.model.ComplaintCategory
import com.hostelcare.app.data.model.ComplaintPriority
import kotlinx.coroutines.delay

interface AiComplaintAnalyzer {
    suspend fun analyzeComplaint(title: String, description: String, category: ComplaintCategory): Result<AiAnalysisResult>
}

class MockAiComplaintAnalyzer : AiComplaintAnalyzer {
    override suspend fun analyzeComplaint(
        title: String,
        description: String,
        category: ComplaintCategory
    ): Result<AiAnalysisResult> {
        // Simulate network delay
        delay(1500)
        
        val text = "$title $description".lowercase()
        var priority = ComplaintPriority.LOW
        
        if (text.contains("water") || text.contains("leak") || text.contains("flood")) {
            priority = ComplaintPriority.HIGH
        } else if (text.contains("spark") || text.contains("fire") || text.contains("emergency")) {
            priority = ComplaintPriority.EMERGENCY
        } else if (text.contains("broken") || text.contains("not working")) {
            priority = ComplaintPriority.MEDIUM
        }

        val summary = "AI analysis suggests this is a ${priority.name.lowercase()} priority issue related to ${category.name.lowercase()}."

        return Result.success(
            AiAnalysisResult(
                category = category,
                priority = priority,
                summary = summary
            )
        )
    }
}

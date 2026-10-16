package com.hostelcare.app

import com.google.gson.Gson
import com.hostelcare.app.data.model.Complaint
import com.hostelcare.app.data.model.ComplaintCategory
import com.hostelcare.app.data.model.ComplaintPriority
import com.hostelcare.app.data.model.ComplaintStatus
import com.hostelcare.app.data.remote.AiAnalyzeRequest
import com.hostelcare.app.data.remote.AiAnalyzeResponse
import com.hostelcare.app.data.remote.ComplaintResponse
import org.junit.Assert.*
import org.junit.Test

class AiPersistenceTest {

    private val gson = Gson()

    @Test
    fun testComplaintWithAiLifecycleFieldsParsing() {
        val json = """
        {
          "message": "Complaint retrieved",
          "complaint": {
            "_id": "670ef1234567890abcdef123",
            "ticketId": "HC-123456",
            "student": "60d5ecb8b392d725643e2a9b",
            "title": "Water heater broken",
            "description": "Geyser is not turning on and leaking water from bottom valve.",
            "category": "electrical",
            "priority": "high",
            "hostelBlock": "Tagore Block B",
            "roomNumber": "302",
            "photoUrl": "https://example.com/photo.jpg",
            "aiSummary": "Faulty water heater element causing short circuit and minor leak.",
            "aiStatus": "completed",
            "aiProvider": "gemini",
            "status": "submitted",
            "createdAt": "2026-10-16T18:00:00.000Z",
            "updatedAt": "2026-10-16T18:05:00.000Z"
          }
        }
        """.trimIndent()

        val response = gson.fromJson(json, ComplaintResponse::class.java)
        assertNotNull(response.complaint)
        assertEquals("completed", response.complaint.aiStatus)
        assertEquals("gemini", response.complaint.aiProvider)

        val local = response.complaint.toLocalComplaint()
        assertEquals("670ef1234567890abcdef123", local.id)
        assertEquals("HC-123456", local.ticketId)
        assertEquals("completed", local.aiStatus)
        assertEquals("gemini", local.aiProvider)
        assertEquals("Faulty water heater element causing short circuit and minor leak.", local.aiSummary)
        assertEquals(ComplaintCategory.ELECTRICAL, local.category)
        assertEquals(ComplaintPriority.HIGH, local.priority)
        assertEquals(ComplaintStatus.SUBMITTED, local.status)
    }

    @Test
    fun testComplaintWithoutAiLifecycleFieldsBackwardCompatibility() {
        // Document without aiStatus or aiProvider (legacy records)
        val json = """
        {
          "message": "Complaint retrieved",
          "complaint": {
            "_id": "670ef1234567890abcdef999",
            "ticketId": "HC-999888",
            "student": "60d5ecb8b392d725643e2a9b",
            "title": "Door lock stuck",
            "description": "Key gets jammed when turning.",
            "category": "furniture",
            "priority": "low",
            "hostelBlock": "B",
            "roomNumber": "105",
            "photoUrl": "",
            "aiSummary": "Stuck key cylinder.",
            "status": "in_progress",
            "createdAt": "2026-10-10T12:00:00.000Z",
            "updatedAt": "2026-10-10T12:10:00.000Z"
          }
        }
        """.trimIndent()

        val response = gson.fromJson(json, ComplaintResponse::class.java)
        assertNotNull(response.complaint)
        assertNull(response.complaint.aiStatus)
        assertNull(response.complaint.aiProvider)

        val local = response.complaint.toLocalComplaint()
        assertNull(local.aiStatus)
        assertNull(local.aiProvider)
        assertEquals("Stuck key cylinder.", local.aiSummary)
        assertEquals(ComplaintStatus.IN_PROGRESS, local.status)
    }

    @Test
    fun testAiAnalyzeResponseCachedAndFailoverParsing() {
        val cachedJson = """
        {
          "message": "Complaint analyzed successfully",
          "cached": true,
          "analysis": {
            "category": "plumbing",
            "priority": "medium",
            "summary": "Leaking sink tap."
          },
          "provider": "persisted",
          "failedOver": false
        }
        """.trimIndent()

        val cachedResponse = gson.fromJson(cachedJson, AiAnalyzeResponse::class.java)
        assertEquals("Complaint analyzed successfully", cachedResponse.message)
        assertTrue(cachedResponse.cached == true)
        assertEquals("persisted", cachedResponse.provider)
        assertEquals(false, cachedResponse.failedOver)
        assertEquals("plumbing", cachedResponse.analysis?.category)

        val failoverJson = """
        {
          "message": "Complaint analyzed successfully",
          "cached": false,
          "analysis": {
            "category": "internet",
            "priority": "high",
            "summary": "Router power failure."
          },
          "provider": "openrouter",
          "failedOver": true
        }
        """.trimIndent()

        val failoverResponse = gson.fromJson(failoverJson, AiAnalyzeResponse::class.java)
        assertEquals("openrouter", failoverResponse.provider)
        assertEquals(true, failoverResponse.failedOver)
    }

    @Test
    fun testAiAnalyzeRequestSerialization() {
        val req = AiAnalyzeRequest(
            complaintId = "670ef1234567890abcdef123",
            title = "",
            description = "",
            category = null,
            image = null,
            forceRetry = true
        )
        val json = gson.toJson(req)
        assertTrue(json.contains("\"complaintId\":\"670ef1234567890abcdef123\""))
        assertTrue(json.contains("\"forceRetry\":true"))
    }
}

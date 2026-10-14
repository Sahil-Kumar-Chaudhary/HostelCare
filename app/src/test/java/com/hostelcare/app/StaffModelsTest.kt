package com.hostelcare.app

import com.google.gson.Gson
import com.hostelcare.app.data.remote.*
import org.junit.Assert.*
import org.junit.Test

class StaffModelsTest {

    @Test
    fun testStaffStatsDataSerialization() {
        val stats = StaffStatsData(
            totalAssigned = 10,
            completedCount = 6,
            inProgressCount = 2,
            pendingCount = 4,
            pendingReviewCount = 1
        )
        val json = Gson().toJson(stats)
        assertTrue(json.contains("\"totalAssigned\":10"))
        assertTrue(json.contains("\"completedCount\":6"))
        assertTrue(json.contains("\"inProgressCount\":2"))
        assertTrue(json.contains("\"pendingCount\":4"))
        assertTrue(json.contains("\"pendingReviewCount\":1"))

        val deserialized = Gson().fromJson(json, StaffStatsData::class.java)
        assertEquals(10, deserialized.totalAssigned)
        assertEquals(6, deserialized.completedCount)
        assertEquals(2, deserialized.inProgressCount)
        assertEquals(4, deserialized.pendingCount)
        assertEquals(1, deserialized.pendingReviewCount)
    }

    @Test
    fun testStaffTaskItemWithStudentDetails() {
        val student = StaffTaskStudent(
            id = "6acbdcd8232d3175c5c10f0d",
            name = "Sahil Kumar Chaudhary",
            studentId = "12305507",
            phone = "9876543210",
            email = "sahilku707@gmail.com",
            roomNumber = "927",
            hostelBlock = "B",
            hostelName = "BH1"
        )
        val task = StaffTaskItem(
            id = "task-123",
            ticketId = "HC-076299",
            title = "Water leakage in bathroom",
            description = "Continuous water leakage",
            category = "plumbing",
            priority = "high",
            status = "assigned",
            hostelName = "BH1",
            hostelBlock = "B",
            roomNumber = "927",
            photoUrl = null,
            aiSummary = null,
            assignmentInstructions = "Check washer",
            workNotes = null,
            createdAt = "2026-10-12T08:17:00.000Z",
            student = student
        )

        val json = Gson().toJson(task)
        assertTrue(json.contains("HC-076299"))
        assertTrue(json.contains("Water leakage in bathroom"))
        assertTrue(json.contains("Sahil Kumar Chaudhary"))
        assertTrue(json.contains("12305507"))
        assertTrue(json.contains("BH1"))

        val deserialized = Gson().fromJson(json, StaffTaskItem::class.java)
        assertEquals("HC-076299", deserialized.ticketId)
        assertEquals("BH1", deserialized.hostelName)
        assertEquals("12305507", deserialized.student?.studentId)
        assertEquals("Sahil Kumar Chaudhary", deserialized.student?.name)
    }

    @Test
    fun testInAppNotificationSerialization() {
        val notif = InAppNotificationItem(
            id = "notif-001",
            title = "Task Assigned",
            message = "You have been assigned to HC-076299 (Water leakage).",
            type = "complaint_assigned",
            complaintId = "task-123",
            isRead = false,
            createdAt = "2026-10-14T07:00:00.000Z"
        )
        val json = Gson().toJson(notif)
        assertTrue(json.contains("Task Assigned"))
        assertTrue(json.contains("complaint_assigned"))
        assertFalse(json.contains("\"isRead\":true"))

        val deserialized = Gson().fromJson(json, InAppNotificationItem::class.java)
        assertEquals("Task Assigned", deserialized.title)
        assertEquals("task-123", deserialized.complaintId)
        assertFalse(deserialized.isRead)
    }
}

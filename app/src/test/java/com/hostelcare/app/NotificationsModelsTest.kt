package com.hostelcare.app

import com.google.gson.Gson
import com.hostelcare.app.data.remote.InAppNotificationItem
import com.hostelcare.app.data.remote.InAppNotificationsResponse
import com.hostelcare.app.ui.screens.staff.getNotificationVisuals
import com.hostelcare.app.ui.screens.student.formatStudentTimeAgo
import com.hostelcare.app.ui.screens.student.getStudentNotificationVisuals
import com.hostelcare.app.ui.screens.warden.formatWardenTimeAgo
import com.hostelcare.app.ui.screens.warden.getWardenNotificationVisuals
import org.junit.Assert.*
import org.junit.Test

class NotificationsModelsTest {

    @Test
    fun testInAppNotificationItemSerialization() {
        val item = InAppNotificationItem(
            id = "notif_100",
            title = "Complaint Registered",
            message = "Your complaint has been registered with ticket HC-076299",
            type = "complaint_created",
            complaintId = "complaint_abc",
            isRead = false,
            createdAt = "2026-10-14T10:00:00.000Z"
        )
        val json = Gson().toJson(item)
        assertTrue(json.contains("\"title\":\"Complaint Registered\""))
        assertTrue(json.contains("\"type\":\"complaint_created\""))
        assertTrue(json.contains("\"isRead\":false"))

        val deserialized = Gson().fromJson(json, InAppNotificationItem::class.java)
        assertEquals("notif_100", deserialized.id)
        assertEquals("Complaint Registered", deserialized.title)
        assertEquals("complaint_created", deserialized.type)
        assertEquals("complaint_abc", deserialized.complaintId)
        assertFalse(deserialized.isRead)
    }

    @Test
    fun testInAppNotificationsResponseParsing() {
        val json = """
            {
                "success": true,
                "unreadCount": 3,
                "notifications": [
                    {
                        "_id": "n1",
                        "title": "Task Assigned",
                        "message": "You have been assigned to HC-076299",
                        "type": "complaint_assigned",
                        "complaintId": "c1",
                        "isRead": false,
                        "createdAt": "2026-10-14T12:00:00.000Z"
                    },
                    {
                        "_id": "n2",
                        "title": "Complaint Resolved",
                        "message": "Your complaint has been resolved.",
                        "type": "complaint_resolved",
                        "complaintId": "c2",
                        "isRead": true,
                        "createdAt": "2026-10-14T11:00:00.000Z"
                    }
                ]
            }
        """.trimIndent()

        val resp = Gson().fromJson(json, InAppNotificationsResponse::class.java)
        assertEquals(3, resp.unreadCount)
        assertEquals(2, resp.notifications.size)
        assertEquals("n1", resp.notifications[0].id)
        assertFalse(resp.notifications[0].isRead)
        assertEquals("n2", resp.notifications[1].id)
        assertTrue(resp.notifications[1].isRead)
    }

    @Test
    fun testStudentNotificationVisuals() {
        val created = getStudentNotificationVisuals("complaint_created", "Registered")
        assertNotNull(created.first)

        val assigned = getStudentNotificationVisuals("complaint_assigned", "Assigned")
        assertNotNull(assigned.first)

        val workStarted = getStudentNotificationVisuals("work_started", "Work Started")
        assertNotNull(workStarted.first)

        val resolved = getStudentNotificationVisuals("complaint_resolved", "Resolved")
        assertNotNull(resolved.first)
    }

    @Test
    fun testWardenNotificationVisuals() {
        val completion = getWardenNotificationVisuals("completion_requested", "Completion Review")
        assertNotNull(completion.first)

        val created = getWardenNotificationVisuals("complaint_created", "New Complaint")
        assertNotNull(created.first)

        val resolved = getWardenNotificationVisuals("complaint_resolved", "Complaint Resolved")
        assertNotNull(resolved.first)
    }

    @Test
    fun testStaffNotificationVisuals() {
        val assigned = getNotificationVisuals("complaint_assigned", "Task Assigned")
        assertNotNull(assigned.first)

        val status = getNotificationVisuals("status_updated", "Status Updated")
        assertNotNull(status.first)
    }

    @Test
    fun testRelativeTimeFormatting() {
        assertEquals("", formatStudentTimeAgo(null))
        assertEquals("", formatStudentTimeAgo(""))
        assertEquals("", formatWardenTimeAgo(null))

        val nowIso = java.time.Instant.now().toString()
        val formatted = formatStudentTimeAgo(nowIso)
        assertTrue(formatted == "Just now" || formatted.contains("min ago"))
    }
}

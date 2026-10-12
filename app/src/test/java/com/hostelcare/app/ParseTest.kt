package com.hostelcare.app

import com.google.gson.Gson
import com.hostelcare.app.data.remote.ComplaintResponse
import org.junit.Test

class ParseTest {
    @Test
    fun testParse() {
        val json = """
        {
          "message": "Complaint created successfully",
          "complaint": {
            "student": "60d5ecb8b392d725643e2a9b",
            "title": "thsrth",
            "description": "wyyurgfghfuwrpgfl",
            "category": "plumbing",
            "priority": "low",
            "hostelBlock": "Tagore Block A",
            "roomNumber": "101",
            "photoUrl": "",
            "aiSummary": "",
            "status": "submitted",
            "assignedStaff": null,
            "resolutionNote": "",
            "_id": "6ac6883504b4df920fadd46f",
            "ticketId": "HC-893698",
            "createdAt": "2026-10-07T17:58:13.698Z",
            "updatedAt": "2026-10-07T17:58:13.698Z",
            "__v": 0
          }
        }
        """
        try {
            val response = Gson().fromJson(json, ComplaintResponse::class.java)
            val local = response.complaint.toLocalComplaint()
            println("SUCCESS: " + local.title)
        } catch (e: Exception) {
            println("EXCEPTION: " + e.message)
            e.printStackTrace()
        }
    }
}
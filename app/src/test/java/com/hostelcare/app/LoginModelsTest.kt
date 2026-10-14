package com.hostelcare.app

import com.google.gson.Gson
import com.hostelcare.app.data.remote.LoginRequest
import com.hostelcare.app.data.remote.RetrofitClient
import org.junit.Assert.*
import org.junit.Test

class LoginModelsTest {

    @Test
    fun testLoginRequestWithEmail() {
        val req = LoginRequest(email = "sahilku707@gmail.com", password = "Password123!", portal = "student")
        val json = Gson().toJson(req)
        assertTrue(json.contains("sahilku707@gmail.com"))
        assertTrue(json.contains("student"))
        assertFalse(json.contains("studentId"))
    }

    @Test
    fun testLoginRequestWithStudentId() {
        val req = LoginRequest(studentId = "12305507", password = "Password123!", portal = "student")
        val json = Gson().toJson(req)
        assertTrue(json.contains("12305507"))
        assertTrue(json.contains("student"))
        assertFalse(json.contains("email"))
    }

    @Test
    fun testRetrofitClientCustomBaseUrl() {
        val defaultUrl = RetrofitClient.baseUrl
        assertNotNull(defaultUrl)
        assertTrue(defaultUrl.endsWith("/"))

        RetrofitClient.setCustomBaseUrl("https://example.com/api")
        assertEquals("https://example.com/api/", RetrofitClient.baseUrl)

        // Reset
        RetrofitClient.setCustomBaseUrl(null)
        assertEquals(defaultUrl, RetrofitClient.baseUrl)
    }
}

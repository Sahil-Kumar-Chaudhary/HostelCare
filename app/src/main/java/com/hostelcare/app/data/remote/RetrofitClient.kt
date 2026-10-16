package com.hostelcare.app.data.remote

import android.util.Log
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "HostelCareApi"

    @Volatile
    private var customBaseUrl: String? = null

    fun setCustomBaseUrl(url: String?) {
        customBaseUrl = if (url != null && !url.endsWith("/")) "$url/" else url
    }

    /**
     * Override API base URL dynamically for physical Android device testing:
     * - "http://127.0.0.1:5000/" with `adb reverse tcp:5000 tcp:5000`
     * - "http://<PC_LAN_IP>:5000/" over shared local Wi-Fi
     * Falls back to BuildConfig.BASE_URL ("https://hostelcare-backend-1r6q.onrender.com/" by default)
     */
    private fun isEmulator(): Boolean {
        return try {
            val fp = android.os.Build.FINGERPRINT ?: ""
            val model = android.os.Build.MODEL ?: ""
            val mfg = android.os.Build.MANUFACTURER ?: ""
            val brand = android.os.Build.BRAND ?: ""
            val device = android.os.Build.DEVICE ?: ""
            val prod = android.os.Build.PRODUCT ?: ""
            val hw = android.os.Build.HARDWARE ?: ""

            fp.startsWith("generic")
                || fp.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || mfg.contains("Genymotion")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || prod == "google_sdk"
                || hw.contains("goldfish")
                || hw.contains("ranchu")
                || prod.contains("sdk")
        } catch (_: Throwable) {
            false
        }
    }

    val baseUrl: String
        get() {
            if (customBaseUrl != null) return customBaseUrl!!
            val configured = com.hostelcare.app.BuildConfig.BASE_URL
            // Never rewrite HTTPS production URLs to local addresses
            if (configured.startsWith("https://", ignoreCase = true)) {
                return configured
            }
            if (configured == "http://10.0.2.2:5000/" && !isEmulator()) {
                return "http://127.0.0.1:5000/"
            }
            return configured
        }

    fun create(tokenManager: TokenManager): ApiService {
        val currentBaseUrl = baseUrl
        Log.i(TAG, "Initializing RetrofitClient with baseUrl: $currentBaseUrl (configured: ${com.hostelcare.app.BuildConfig.BASE_URL}, isEmulator: ${isEmulator()})")
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
            level = if (com.hostelcare.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        val fallbackDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e: Exception) {
                    if (hostname == "hostelcare-backend-1r6q.onrender.com") {
                        listOf(
                            InetAddress.getByName("216.24.57.16"),
                            InetAddress.getByName("216.24.57.18")
                        )
                    } else {
                        throw e
                    }
                }
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .dns(fallbackDns)
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

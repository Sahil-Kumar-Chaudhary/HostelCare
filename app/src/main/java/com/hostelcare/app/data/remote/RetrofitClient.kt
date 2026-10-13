package com.hostelcare.app.data.remote

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress

object RetrofitClient {
    @Volatile
    private var customBaseUrl: String? = null

    /**
     * Override API base URL dynamically for physical Android device testing:
     * - "http://127.0.0.1:5000/" with `adb reverse tcp:5000 tcp:5000`
     * - "http://<PC_LAN_IP>:5000/" over shared local Wi-Fi
     * Falls back to BuildConfig.BASE_URL ("http://10.0.2.2:5000/" for emulator)
     */
    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.PRODUCT.contains("sdk"))
    }

    val baseUrl: String
        get() {
            if (customBaseUrl != null) return customBaseUrl!!
            val configured = com.hostelcare.app.BuildConfig.BASE_URL
            if (configured == "http://10.0.2.2:5000/" && !isEmulator()) {
                return "http://127.0.0.1:5000/"
            }
            return configured
        }

    fun create(tokenManager: TokenManager): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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

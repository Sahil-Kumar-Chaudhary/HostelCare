package com.hostelcare.app.data.remote

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress

object RetrofitClient {
    // Dynamic BASE_URL: http://10.0.2.2:5000/ in debug, https://... in release
    private val BASE_URL = com.hostelcare.app.BuildConfig.BASE_URL

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
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

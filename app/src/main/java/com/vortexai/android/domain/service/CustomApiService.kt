package com.vortexai.android.domain.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomApiService @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    suspend fun makeRequest(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: Map<String, Any>?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url)
            
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            
            if (body != null && method == "POST") {
                val jsonBody = JSONObject(body).toString()
                requestBuilder.post(jsonBody.toRequestBody("application/json".toMediaType()))
            }
            
            requestBuilder.method(method, if (method == "POST" && body != null) {
                JSONObject(body).toString().toRequestBody("application/json".toMediaType())
            } else null)
            
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Result.success(responseBody)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e("CustomApiService", "Request failed", e)
            Result.failure(e)
        }
    }
}

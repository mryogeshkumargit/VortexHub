package com.vortexai.android.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SupabaseConnectionTest {
    
    companion object {
        private const val TAG = "SupabaseConnectionTest"
    }
    
    suspend fun testConnection(url: String, anonKey: String): ConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing Supabase connection to: $url")
                
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                
                // Test 1: Check if URL is accessible
                val healthRequest = Request.Builder()
                    .url("$url/rest/v1/")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .get()
                    .build()
                
                val healthResponse = client.newCall(healthRequest).execute()
                
                if (!healthResponse.isSuccessful) {
                    Log.e(TAG, "Supabase health check failed: ${healthResponse.code}")
                    return@withContext ConnectionResult.Failure("Health check failed: ${healthResponse.code}")
                }
                
                // Test 2: Check storage access (more flexible)
                val storageRequest = Request.Builder()
                    .url("$url/storage/v1/bucket/backups")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .get()
                    .build()
                
                val storageResponse = client.newCall(storageRequest).execute()
                
                if (!storageResponse.isSuccessful) {
                    Log.w(TAG, "Storage access failed: ${storageResponse.code}")
                    // Try a different approach - check if we can list buckets
                    val listBucketsRequest = Request.Builder()
                        .url("$url/storage/v1/bucket")
                        .addHeader("apikey", anonKey)
                        .addHeader("Authorization", "Bearer $anonKey")
                        .get()
                        .build()
                    
                    val listBucketsResponse = client.newCall(listBucketsRequest).execute()
                    
                    if (listBucketsResponse.isSuccessful) {
                        return@withContext ConnectionResult.PartialSuccess("Connected to Supabase! Storage policies may need adjustment, but basic access is working.")
                    } else {
                        return@withContext ConnectionResult.PartialSuccess("Connected to Supabase but storage access needs configuration. The app will work with local storage for now.")
                    }
                }
                
                Log.d(TAG, "Supabase connection test successful")
                ConnectionResult.Success("✅ Connected to Supabase successfully!")
                
            } catch (e: Exception) {
                Log.e(TAG, "Supabase connection test failed", e)
                ConnectionResult.Failure("Connection failed: ${e.message}")
            }
        }
    }
}

sealed class ConnectionResult {
    data class Success(val message: String) : ConnectionResult()
    data class PartialSuccess(val message: String) : ConnectionResult()
    data class Failure(val error: String) : ConnectionResult()
}

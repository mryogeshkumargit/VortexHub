package com.vortexai.android.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple connectivity test to verify backend connection
 */
object ConnectivityTest {
    private const val TAG = "ConnectivityTest"
    
    /**
     * Test connection to backend API
     */
    suspend fun testBackendConnection(baseUrl: String = "http://10.0.2.2:5000"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing connection to: $baseUrl/api/characters?limit=1")
                
                val url = URL("$baseUrl/api/characters?limit=1")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 10000 // 10 seconds
                    readTimeout = 30000 // 30 seconds
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "VortexAndroid/1.0")
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Response received, length: ${response.length}")
                    
                    // Check if response contains expected data
                    val hasCharacters = response.contains("\"characters\"")
                    val hasSuccess = response.contains("\"success\"")
                    
                    Log.d(TAG, "Response contains characters: $hasCharacters")
                    Log.d(TAG, "Response contains success: $hasSuccess")
                    
                    return@withContext hasCharacters && hasSuccess
                } else {
                    Log.e(TAG, "HTTP error: $responseCode")
                    return@withContext false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed", e)
                return@withContext false
            }
        }
    }
    
    /**
     * Test connection to a specific character endpoint
     */
    suspend fun testCharacterEndpoint(baseUrl: String = "http://10.0.2.2:5000", characterId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing character endpoint: $baseUrl/api/characters/$characterId")
                
                val url = URL("$baseUrl/api/characters/$characterId")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 15000 // 15 seconds
                    readTimeout = 60000 // 60 seconds for large character data
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "VortexAndroid/1.0")
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Character response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Character response received, length: ${response.length}")
                    
                    // Check for character data including lorebook
                    val hasName = response.contains("\"name\"")
                    val hasBackstory = response.contains("\"backstory\"")
                    val hasCharacterBook = response.contains("\"character_book\"")
                    
                    Log.d(TAG, "Character response has name: $hasName")
                    Log.d(TAG, "Character response has backstory: $hasBackstory")
                    Log.d(TAG, "Character response has character_book: $hasCharacterBook")
                    
                    return@withContext hasName && hasBackstory
                } else {
                    Log.e(TAG, "Character HTTP error: $responseCode")
                    return@withContext false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Character connection test failed", e)
                return@withContext false
            }
        }
    }
} 
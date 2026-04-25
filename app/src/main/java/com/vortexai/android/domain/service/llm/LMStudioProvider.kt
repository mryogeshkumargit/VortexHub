package com.vortexai.android.domain.service.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LMStudioProvider : LLMProvider {
    companion object {
        private const val TAG = "LMStudioProvider"
    }
    
    private var endpoint: String = "http://localhost:1234"
    private var apiKey: String = ""
    private var model: String = ""
    private var isReady: Boolean = false
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    fun setEndpoint(endpoint: String) {
        this.endpoint = endpoint.trim().removeSuffix("/")
        Log.d(TAG, "LMStudio endpoint set to: ${this.endpoint}")
        updateReadyState()
    }

    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Log.d(TAG, "LMStudio API key set (length: ${apiKey.length})")
    }

    fun setModel(model: String) {
        this.model = model
        Log.d(TAG, "LMStudio model set to: $model")
        updateReadyState()
    }
    
    private fun updateReadyState() {
        isReady = endpoint.isNotBlank() && model.isNotBlank()
        Log.d(TAG, "LMStudio ready state: $isReady (endpoint: $endpoint, model: $model)")
    }

    override fun isReady(): Boolean {
        return isReady
    }

    override fun getModelName(): String {
        return model.ifBlank { "default" }
    }

    override fun getMaxTokens(): Int? {
        return 4096 // Default max tokens for LMStudio
    }

    override suspend fun generateResponse(prompt: String, params: GenerationParams): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating response with LMStudio at $endpoint")
            
            val requestBody = JSONObject().apply {
                put("model", model.ifBlank { "default" })
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", params.temperature)
                put("top_p", params.topP)
                put("max_tokens", params.maxTokens)
                put("frequency_penalty", params.frequencyPenalty)
                put("stream", false)
            }

            val request = Request.Builder()
                .url("$endpoint/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            Log.d(TAG, "Sending request to LMStudio: ${request.url}")

            var retries = 0
            val maxRetries = 3
            var lastError: String? = null
            
            while (retries < maxRetries) {
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                        ?: throw Exception("Empty response from LMStudio")

                    val json = JSONObject(responseBody)
                    val choices = json.optJSONArray("choices")
                    
                    if (choices != null && choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val message = firstChoice.optJSONObject("message")
                        val content = message?.optString("content") ?: ""
                        
                        if (content.isNotBlank()) {
                            Log.d(TAG, "LMStudio response: ${content.take(100)}...")
                            isReady = true
                            return@withContext content
                        }
                    }
                }
                
                val errorBody = response.body?.string() ?: "Unknown error"
                lastError = errorBody
                
                if (errorBody.contains("model is loading", ignoreCase = true) || 
                    errorBody.contains("loading", ignoreCase = true)) {
                    retries++
                    Log.d(TAG, "Model loading, retry $retries/$maxRetries...")
                    kotlinx.coroutines.delay(3000)
                } else {
                    Log.e(TAG, "LMStudio error: ${response.code} - $errorBody")
                    throw Exception("LMStudio error: ${response.code} - $errorBody")
                }
            }
            
            throw Exception("Model still loading after $maxRetries retries: $lastError")



        } catch (e: Exception) {
            Log.e(TAG, "Error generating response with LMStudio", e)
            isReady = false
            throw e
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing LMStudio connection at $endpoint")
            
            val request = Request.Builder()
                .url("$endpoint/v1/models")
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "LMStudio connection test successful")
                isReady = true
                true
            } else {
                Log.w(TAG, "LMStudio connection test failed: ${response.code}")
                isReady = false
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "LMStudio connection test error", e)
            isReady = false
            false
        }
    }

    fun getProviderName(): String = "LMStudio"
}

package com.vortexai.android.domain.service.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/"
    private val defaultModel = "gemini-1.5-flash" // Fast and efficient model
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey.trim()
        Timber.i("Gemini API key set: ${if (apiKey.isBlank()) "EMPTY" else "${apiKey.take(8)}..."}")
    }
    
    override fun isReady(): Boolean {
        return !apiKey.isNullOrBlank()
    }
    
    override fun getModelName(): String {
        return defaultModel
    }
    
    override fun getMaxTokens(): Int? {
        return 8192 // Gemini 1.5 Flash max tokens
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            val errorMsg = "Gemini provider not ready - API key: ${if (apiKey.isNullOrBlank()) "NOT SET" else "SET (${apiKey!!.length} chars)"}"
            Timber.e(errorMsg)
            throw IllegalStateException(errorMsg)
        }
        
        try {
            // Build request JSON for Gemini format
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", params.temperature)
                    put("topP", params.topP)
                    if (params.maxTokens > 0) put("maxOutputTokens", params.maxTokens)
                    put("candidateCount", 1)
                })
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("${baseUrl}models/${defaultModel}:generateContent?key=${apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("Gemini API error: ${response.code} - $errorBody")
                throw Exception("Gemini API error: ${response.code} - $errorBody")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Gemini")
            
            // Parse response JSON manually
            val responseJson = JSONObject(responseBody)
            val candidatesArray = responseJson.optJSONArray("candidates")
            
            if (candidatesArray == null || candidatesArray.length() == 0) {
                throw Exception("No candidates in Gemini response")
            }
            
            val firstCandidate = candidatesArray.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val generatedText = parts?.getJSONObject(0)?.optString("text") ?: ""
            
            if (generatedText.isBlank()) {
                throw Exception("Empty text in Gemini response")
            }
            
            Timber.d("Gemini response generated: ${generatedText.length} characters")
            generatedText
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Gemini API: ${e.message}")
            when {
                e.message?.contains("401") == true -> throw Exception("❌ Invalid Gemini API key. Please check your API key in Settings.")
                e.message?.contains("403") == true -> throw Exception("❌ Gemini API access denied. Check your API key permissions.")
                e.message?.contains("404") == true -> throw Exception("❌ Gemini model not found.")
                e.message?.contains("429") == true -> throw Exception("❌ Gemini API rate limit exceeded. Please wait and try again.")
                e.message?.contains("timeout") == true -> throw Exception("❌ Gemini API connection timeout. Please try again.")
                e.message?.contains("Connection") == true -> throw Exception("❌ Network error connecting to Gemini API. Check your internet connection.")
                else -> throw Exception("❌ Gemini API error: ${e.message}")
            }
        }
    }
}
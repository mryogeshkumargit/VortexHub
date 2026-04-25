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
class GrokProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private var selectedModel: String? = null
    private val baseUrl = "https://api.x.ai/v1/"
    private val defaultModel = "grok-4"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey.trim()
    }
    
    fun setModel(model: String) {
        this.selectedModel = model
    }
    
    override fun isReady(): Boolean = !apiKey.isNullOrBlank()
    
    override fun getModelName(): String = selectedModel ?: defaultModel
    
    override fun getMaxTokens(): Int? = 4096
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        generateResponseWithSystem(prompt, "You are a helpful assistant.", params)
    }
    
    suspend fun generateResponseWithSystem(
        prompt: String,
        systemMessage: String = "You are a helpful assistant.",
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            throw IllegalStateException("Grok provider not ready - API key not set")
        }
        
        val modelToUse = selectedModel?.takeIf { it.isNotBlank() } ?: defaultModel
        
        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemMessage)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            
            val requestJson = JSONObject().apply {
                put("model", modelToUse)
                put("messages", messagesArray)
                put("temperature", params.temperature)
                if (params.maxTokens > 0) put("max_tokens", params.maxTokens)
                put("stream", false)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("${baseUrl}chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                    ?: throw Exception("Empty response from Grok")
                
                val responseJson = JSONObject(responseBody)
                val choicesArray = responseJson.getJSONArray("choices")
                
                if (choicesArray.length() == 0) {
                    throw Exception("No choices in Grok response")
                }
                
                val firstChoice = choicesArray.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                val generatedText = message.getString("content")
                
                return@withContext generatedText
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                val error = when (response.code) {
                    401 -> "Invalid Grok API key"
                    403 -> "Grok access denied"
                    429 -> "Grok rate limit exceeded"
                    else -> "Grok API error: ${response.code} - $errorBody"
                }
                throw Exception(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calling Grok API")
            throw Exception("Grok error: ${e.message}")
        }
    }
}

package com.vortexai.android.domain.service.llm

import com.vortexai.android.BuildConfig
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
class TogetherProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private var selectedModel: String? = null
    private val baseUrl = "https://api.together.xyz/v1/"
    private val defaultModel = "meta-llama/Llama-3-70b-chat-hf" // Best for roleplay per documentation
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey.trim()
        Timber.i("Together API key set: ${if (apiKey.isBlank()) "EMPTY" else "${apiKey.take(8)}..."}")
    }
    
    fun setModel(model: String) {
        this.selectedModel = model
        Timber.i("Together model set to: $model")
    }
    
    override fun isReady(): Boolean {
        return !apiKey.isNullOrBlank()
    }
    
    override fun getModelName(): String {
        return defaultModel
    }
    
    override fun getMaxTokens(): Int? {
        return 8192 // Llama 3.2 3B max tokens (increased for longer responses)
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            val errorMsg = "Together provider not ready - API key: ${if (apiKey.isNullOrBlank()) "NOT SET" else "SET (${apiKey!!.length} chars)"}"
            Timber.e(errorMsg)
            throw IllegalStateException(errorMsg)
        }
        
        Timber.d("Together AI: Starting request with model: $defaultModel")
        Timber.d("Together AI: API key length: ${apiKey?.length ?: 0}")
        Timber.d("Together AI: Base URL: $baseUrl")
        
        try {
            // Build request JSON manually
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            
            val modelToUse = selectedModel?.takeIf { it.isNotBlank() } ?: defaultModel
            val requestJson = JSONObject().apply {
                put("model", modelToUse)
                put("messages", messagesArray)
                put("temperature", params.temperature)
                if (params.topP > 0) put("top_p", params.topP)
                if (params.maxTokens > 0) put("max_tokens", params.maxTokens)
                if (params.stop.isNotEmpty()) {
                    val stopArray = JSONArray()
                    params.stop.forEach { stopArray.put(it) }
                    put("stop", stopArray)
                }
                if (params.frequencyPenalty != 0f) put("frequency_penalty", params.frequencyPenalty)
                if (params.presencePenalty != 0f) put("presence_penalty", params.presencePenalty)
                put("stream", false)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val fullUrl = "${baseUrl}chat/completions"
            Timber.d("Together AI: Making request to: $fullUrl")
            Timber.d("Together AI: Request body: ${requestJson.toString()}")
            
            val httpRequest = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            Timber.d("Together AI: Response code: ${response.code}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("Together API error: ${response.code} - $errorBody")
                throw Exception("Together API error: ${response.code} - $errorBody")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Together")
            
            // Parse response JSON manually
            val responseJson = JSONObject(responseBody)
            val choicesArray = responseJson.getJSONArray("choices")
            
            if (choicesArray.length() == 0) {
                throw Exception("No choices in Together response")
            }
            
            val firstChoice = choicesArray.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val generatedText = message.getString("content")
            
            Timber.d("Together response generated: ${generatedText.length} characters")
            generatedText
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Together API: ${e.message}")
            when {
                e.message?.contains("401") == true -> throw Exception("❌ Invalid Together AI API key. Please check your API key in Settings.")
                e.message?.contains("403") == true -> throw Exception("❌ Together AI access denied. Check your API key permissions.")
                e.message?.contains("404") == true -> throw Exception("❌ Model not found on Together AI. Please select a different model.")
                e.message?.contains("429") == true -> throw Exception("❌ Together AI rate limit exceeded. Please wait and try again.")
                e.message?.contains("timeout") == true -> throw Exception("❌ Together AI connection timeout. Please try again.")
                e.message?.contains("Connection") == true -> throw Exception("❌ Network error connecting to Together AI. Check your internet connection.")
                else -> throw Exception("❌ Together AI error: ${e.message}")
            }
        }
    }
} 

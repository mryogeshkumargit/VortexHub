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
class OpenAIProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private val baseUrl = "https://api.openai.com/v1/"
    private val defaultModel = "gpt-4o-mini" // Cost-effective model
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Timber.i("OpenAI API key set")
    }
    
    override fun isReady(): Boolean {
        return !apiKey.isNullOrBlank()
    }
    
    override fun getModelName(): String {
        return defaultModel
    }
    
    override fun getMaxTokens(): Int? {
        return 8192 // gpt-4o-mini max tokens (increased for longer responses)
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            throw IllegalStateException("OpenAI provider not ready - API key not set")
        }
        
        try {
            // Build request JSON manually
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            
            val requestJson = JSONObject().apply {
                put("model", defaultModel)
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
            
            val httpRequest = Request.Builder()
                .url("${baseUrl}chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("OpenAI API error: ${response.code} - $errorBody")
                throw Exception("OpenAI API error: ${response.code} - $errorBody")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from OpenAI")
            
            // Parse response JSON manually
            val responseJson = JSONObject(responseBody)
            val choicesArray = responseJson.getJSONArray("choices")
            
            if (choicesArray.length() == 0) {
                throw Exception("No choices in OpenAI response")
            }
            
            val firstChoice = choicesArray.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val generatedText = message.getString("content")
            
            Timber.d("OpenAI response generated: ${generatedText.length} characters")
            generatedText
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling OpenAI API")
            throw e
        }
    }
} 

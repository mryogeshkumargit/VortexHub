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
class AnthropicProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private val baseUrl = BuildConfig.ANTHROPIC_API_URL
    private val defaultModel = "claude-3-5-haiku-20241022" // Cost-effective model
    private val apiVersion = "2023-06-01"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(BuildConfig.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(BuildConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(BuildConfig.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Timber.i("Anthropic API key set")
    }
    
    override fun isReady(): Boolean {
        return !apiKey.isNullOrBlank()
    }
    
    override fun getModelName(): String {
        return defaultModel
    }
    
    override fun getMaxTokens(): Int {
        return 8192 // Claude Haiku max tokens
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            throw IllegalStateException("Anthropic provider not ready - API key not set")
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
                put("max_tokens", params.maxTokens)
                put("messages", messagesArray)
                put("temperature", params.temperature)
                if (params.topP > 0) put("top_p", params.topP)
                if (params.stop.isNotEmpty()) {
                    val stopArray = JSONArray()
                    params.stop.forEach { stopArray.put(it) }
                    put("stop_sequences", stopArray)
                }
                put("stream", false)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("${baseUrl}messages")
                .addHeader("x-api-key", apiKey!!)
                .addHeader("anthropic-version", apiVersion)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("Anthropic API error: ${response.code} - $errorBody")
                throw Exception("Anthropic API error: ${response.code} - $errorBody")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Anthropic")
            
            // Parse response JSON manually
            val responseJson = JSONObject(responseBody)
            val contentArray = responseJson.getJSONArray("content")
            
            if (contentArray.length() == 0) {
                throw Exception("No content in Anthropic response")
            }
            
            val firstContent = contentArray.getJSONObject(0)
            val generatedText = firstContent.getString("text")
            
            Timber.d("Anthropic response generated: ${generatedText.length} characters")
            generatedText
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Anthropic API")
            throw e
        }
    }
} 

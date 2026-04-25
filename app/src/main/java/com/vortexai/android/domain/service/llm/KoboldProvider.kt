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
class KoboldProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private var endpoint: String = "http://localhost:5000"
    private var model: String = "kobold-model"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    
    fun setEndpoint(endpoint: String) {
        this.endpoint = endpoint.removeSuffix("/")
        Timber.i("Kobold endpoint set to: $endpoint")
    }
    
    fun setModel(model: String) {
        this.model = model
        Timber.i("Kobold model set to: $model")
    }
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Timber.i("Kobold API key set")
    }
    
    override fun isReady(): Boolean {
        return endpoint.isNotBlank() && model.isNotBlank()
    }
    
    override fun getModelName(): String {
        return model
    }
    
    override fun getMaxTokens(): Int {
        return 2048 // Default for Kobold AI
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            throw IllegalStateException("Kobold provider not ready - endpoint not set")
        }
        
        try {
            val requestJson = JSONObject().apply {
                put("prompt", prompt)
                put("max_context_length", 2048)
                put("max_length", if (params.maxTokens > 0) params.maxTokens else 200)
                put("temperature", params.temperature)
                put("top_p", params.topP)
                put("rep_pen", 1.1)
                put("rep_pen_range", 1024)
                put("rep_pen_slope", 0.9)
                put("tfs", 0.95)
                put("top_a", 0.0)
                put("top_k", 0)
                put("typical", 1.0)
                put("frmttriminc", true)
                put("frmtrmblln", false)
                if (params.stop.isNotEmpty()) {
                    val stopSequences = JSONArray()
                    params.stop.forEach { stopSequences.put(it) }
                    put("stop_sequence", stopSequences)
                }
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val requestBuilder = Request.Builder()
                .url("$endpoint/api/v1/generate")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
            
            // Add authorization header if API key is provided
            if (!apiKey.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            
            val httpRequest = requestBuilder.build()
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("Kobold API error: ${response.code} - $errorBody")
                throw Exception("Kobold API error: ${response.code} - $errorBody")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Kobold")
            
            val responseJson = JSONObject(responseBody)
            
            // Kobold returns results in different ways depending on the version
            val generatedText = when {
                responseJson.has("results") -> {
                    val resultsArray = responseJson.getJSONArray("results")
                    if (resultsArray.length() > 0) {
                        resultsArray.getJSONObject(0).getString("text")
                    } else {
                        throw Exception("No results in Kobold response")
                    }
                }
                responseJson.has("text") -> {
                    responseJson.getString("text")
                }
                else -> {
                    throw Exception("Unexpected Kobold response format")
                }
            }
            
            Timber.d("Kobold response generated: ${generatedText.length} characters")
            generatedText
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Kobold API")
            throw e
        }
    }
} 
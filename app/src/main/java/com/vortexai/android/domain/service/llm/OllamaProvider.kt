package com.vortexai.android.domain.service.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private var endpoint: String = "http://localhost:11435"
    private var model: String = "llama2"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    
    fun setEndpoint(endpoint: String) {
        this.endpoint = endpoint.removeSuffix("/")
        Timber.i("Ollama endpoint set to: $endpoint")
    }
    
    fun setModel(model: String) {
        this.model = model
        Timber.i("Ollama model set to: $model")
    }
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey.trim()
        Timber.i("Ollama API key set: ${if (apiKey.isBlank()) "EMPTY (not required for local)" else "${apiKey.take(8)}..."}")
    }
    
    override fun isReady(): Boolean {
        return endpoint.isNotBlank() && model.isNotBlank()
    }
    
    override fun getModelName(): String {
        return model
    }
    
    override fun getMaxTokens(): Int {
        return 4096 // Default for most Ollama models
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            val errorMsg = "Ollama provider not ready - Endpoint: ${if (endpoint.isBlank()) "NOT SET" else endpoint}"
            Timber.e(errorMsg)
            throw IllegalStateException(errorMsg)
        }
        
        try {
            val requestJson = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
                put("stream", false)
                put("options", JSONObject().apply {
                    put("temperature", params.temperature)
                    put("top_p", params.topP)
                    if (params.maxTokens > 0) put("num_predict", params.maxTokens)
                    if (params.stop.isNotEmpty()) {
                        put("stop", params.stop.joinToString(","))
                    }
                })
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val requestBuilder = Request.Builder()
                .url("$endpoint/api/generate")
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
                Timber.e("Ollama API error: ${response.code} - $errorBody")
                throw Exception("Ollama API error: ${response.code} - $errorBody")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Ollama")
            
            val responseJson = JSONObject(responseBody)
            val generatedText = responseJson.getString("response")
            
            Timber.d("Ollama response generated: ${generatedText.length} characters")
            generatedText
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Ollama API: ${e.message}")
            when {
                e.message?.contains("Connection refused") == true -> throw Exception("❌ Ollama is not running. Please start Ollama and try again.")
                e.message?.contains("404") == true -> throw Exception("❌ Model '$model' not found in Ollama. Please pull the model first.")
                e.message?.contains("timeout") == true -> throw Exception("❌ Ollama connection timeout. The model might be loading.")
                e.message?.contains("Connection") == true -> throw Exception("❌ Cannot connect to Ollama at $endpoint. Check if Ollama is running.")
                else -> throw Exception("❌ Ollama error: ${e.message}")
            }
        }
    }
} 
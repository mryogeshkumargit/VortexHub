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
class CustomAPIProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private var customEndpoint: String? = null
    private var apiPrefix: String = "/v1"
    private var modelName: String = "custom-model"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey.trim()
        Timber.i("Custom API key set: ${if (apiKey.isBlank()) "EMPTY" else "${apiKey.take(8)}..."}")
    }
    
    fun setEndpoint(endpoint: String) {
        this.customEndpoint = endpoint.trim().removeSuffix("/")
        Timber.i("Custom API endpoint set: $customEndpoint")
    }
    
    fun setApiPrefix(prefix: String) {
        this.apiPrefix = prefix.trim().let { 
            if (!it.startsWith("/")) "/$it" else it 
        }
        Timber.i("Custom API prefix set: $apiPrefix")
    }
    
    fun setModel(model: String) {
        this.modelName = model
        Timber.i("Custom API model set: $modelName")
    }
    
    /**
     * Get the full endpoint URL with configurable prefix
     */
    private fun getFullEndpoint(path: String): String {
        val baseEndpoint = customEndpoint ?: ""
        return if (baseEndpoint.contains(apiPrefix)) {
            // If endpoint already contains the prefix, use as is
            "$baseEndpoint$path"
        } else {
            // Add the configured prefix
            "$baseEndpoint$apiPrefix$path"
        }
    }
    
    override fun isReady(): Boolean {
        return !apiKey.isNullOrBlank() && !customEndpoint.isNullOrBlank()
    }
    
    override fun getModelName(): String {
        return modelName
    }
    
    override fun getMaxTokens(): Int? {
        return 8192
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            val errorMsg = "Custom API provider not ready - API key: ${if (apiKey.isNullOrBlank()) "NOT SET" else "SET"}, Endpoint: ${if (customEndpoint.isNullOrBlank()) "NOT SET" else customEndpoint}"
            Timber.e(errorMsg)
            throw IllegalStateException(errorMsg)
        }
        
        try {
            // Build request JSON manually following OpenAI format
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            
            val requestJson = JSONObject().apply {
                put("model", modelName)
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
            
            val fullEndpoint = getFullEndpoint("/chat/completions")
            
            // Debug logging for generateResponse
            Timber.d("=== CUSTOM API GENERATE RESPONSE DEBUG ===")
            Timber.d("1. Complete API Endpoint: $fullEndpoint")
            Timber.d("2. Request Body: $requestBody")
            Timber.d("3. API Key (first 8 chars): ${apiKey?.take(8)}...")
            Timber.d("4. Model Name: $modelName")
            Timber.d("5. Prompt: $prompt")
            Timber.d("6. Parameters: temperature=${params.temperature}, topP=${params.topP}, maxTokens=${params.maxTokens}")
            
            val httpRequest = Request.Builder()
                .url(fullEndpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            // Debug response info
            Timber.d("7. Response Code: ${response.code}")
            Timber.d("8. Response Headers: ${response.headers}")
            
            val responseBody = response.body?.string()
            Timber.d("9. Response Body: $responseBody")
            
            if (!response.isSuccessful) {
                val errorBody = responseBody ?: "Unknown error"
                Timber.e("Custom API error: ${response.code} - $errorBody")
                throw Exception("Custom API error: ${response.code} - $errorBody")
            }
            
            if (responseBody.isNullOrBlank()) {
                Timber.e("Empty response from Custom API")
                throw Exception("Empty response from Custom API")
            }
            
            // Check if response is HTML (error page) instead of JSON
            if (responseBody.trim().startsWith("<!doctype", ignoreCase = true) || 
                responseBody.trim().startsWith("<html", ignoreCase = true)) {
                throw Exception("Custom API returned HTML error page instead of JSON. Please check your endpoint URL and API configuration.")
            }
            
            // Parse response JSON manually following OpenAI format
            val responseJson = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                throw Exception("Custom API returned invalid JSON: ${e.message}. Response: ${responseBody.take(200)}...")
            }
            val choicesArray = responseJson.getJSONArray("choices")
            
            if (choicesArray.length() == 0) {
                throw Exception("No choices in Custom API response")
            }
            
            val firstChoice = choicesArray.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val generatedText = message.getString("content")
            
            Timber.d("Custom API response generated: ${generatedText.length} characters")
            generatedText
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Custom API: ${e.message}")
            when {
                e.message?.contains("HTML error page") == true -> throw Exception("❌ Custom API returned HTML instead of JSON. Check your endpoint URL.")
                e.message?.contains("invalid JSON") == true -> throw Exception("❌ Custom API returned invalid JSON. Check your endpoint configuration.")
                e.message?.contains("401") == true -> throw Exception("❌ Invalid Custom API key. Please check your API key.")
                e.message?.contains("403") == true -> throw Exception("❌ Custom API access denied. Check your API key permissions.")
                e.message?.contains("404") == true -> throw Exception("❌ Custom API endpoint not found. Check your endpoint URL.")
                e.message?.contains("timeout") == true -> throw Exception("❌ Custom API connection timeout. Please try again.")
                e.message?.contains("Connection") == true -> throw Exception("❌ Network error connecting to Custom API. Check your endpoint and internet connection.")
                else -> throw Exception("❌ Custom API error: ${e.message}")
            }
        }
    }
    
    /**
     * Test connection to Custom API with detailed debugging
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                Timber.e("Custom API not ready - API key or endpoint not set")
                return@withContext false
            }
            
            val requestJson = JSONObject().apply {
                put("model", modelName)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Hello")
                    })
                })
                put("max_tokens", 10)
                put("temperature", 0.7)
                put("stream", false)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val fullEndpoint = getFullEndpoint("/chat/completions")
            
            // Debug logging
            Timber.d("=== CUSTOM API DEBUG INFO ===")
            Timber.d("1. Complete API Endpoint: $fullEndpoint")
            Timber.d("2. Request Body: $requestBody")
            Timber.d("3. API Key (first 8 chars): ${apiKey?.take(8)}...")
            Timber.d("4. Model Name: $modelName")
            Timber.d("5. Headers: Authorization: Bearer ***, Content-Type: application/json")
            
            val httpRequest = Request.Builder()
                .url(fullEndpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            // Debug response info
            Timber.d("6. Response Code: ${response.code}")
            Timber.d("7. Response Headers: ${response.headers}")
            
            val responseBody = response.body?.string()
            Timber.d("8. Response Body: $responseBody")
            
            if (!response.isSuccessful) {
                Timber.e("Custom API test failed with code: ${response.code}")
                return@withContext false
            }
            
            // Check response body to ensure it's not HTML
            if (responseBody != null && (
                responseBody.trim().startsWith("<!doctype", ignoreCase = true) || 
                responseBody.trim().startsWith("<html", ignoreCase = true))) {
                Timber.e("Custom API test connection returned HTML instead of JSON")
                Timber.e("HTML Response: ${responseBody.take(500)}...")
                return@withContext false
            }
            
            Timber.d("=== CUSTOM API TEST SUCCESS ===")
            response.isSuccessful
            
        } catch (e: Exception) {
            Timber.e(e, "Error testing Custom API connection")
            Timber.e("Exception details: ${e.message}")
            Timber.e("Stack trace: ${e.stackTraceToString()}")
            false
        }
    }
    
    /**
     * Fetch available models from Custom API
     */
    suspend fun fetchModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext Result.failure(Exception("Custom API not ready"))
            }
            
            val httpRequest = Request.Builder()
                .url(getFullEndpoint("/models"))
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch models: ${response.code}"))
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            
            // Check if response is HTML (error page) instead of JSON
            if (responseBody.trim().startsWith("<!doctype", ignoreCase = true) || 
                responseBody.trim().startsWith("<html", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Custom API returned HTML error page instead of JSON. Please check your endpoint URL and API configuration."))
            }
            
            val responseJson = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Custom API returned invalid JSON: ${e.message}. Response: ${responseBody.take(200)}..."))
            }
            val modelsArray = responseJson.optJSONArray("data")
            val models = mutableListOf<String>()
            
            if (modelsArray != null) {
                for (i in 0 until modelsArray.length()) {
                    val model = modelsArray.optJSONObject(i)
                    if (model != null) {
                        val modelId = model.optString("id")
                        if (modelId.isNotBlank()) {
                            models.add(modelId)
                        }
                    }
                }
            }
            
            Result.success(models)
            
        } catch (e: Exception) {
            Timber.e(e, "Error fetching Custom API models")
            Result.failure(e)
        }
    }
    
    /**
     * Debug function that returns detailed information about the Custom API configuration
     */
    suspend fun getDebugInfo(): String = withContext(Dispatchers.IO) {
        val debugInfo = StringBuilder()
        
        debugInfo.append("=== CUSTOM API DEBUG INFORMATION ===\n\n")
        
        // 1. API Endpoint
        debugInfo.append("1. COMPLETE API ENDPOINT:\n")
        debugInfo.append("   ${getFullEndpoint("/chat/completions")}\n\n")
        
        // 2. API Key
        debugInfo.append("2. API KEY:\n")
        val currentApiKey = apiKey
        debugInfo.append("   ${if (currentApiKey.isNullOrBlank()) "NOT SET" else "${currentApiKey.take(8)}... (${currentApiKey.length} characters)"}\n\n")
        
        // 3. Model Name
        debugInfo.append("3. MODEL NAME:\n")
        debugInfo.append("   $modelName\n\n")
        
        // 4. Ready Status
        debugInfo.append("4. PROVIDER READY:\n")
        debugInfo.append("   ${isReady()}\n\n")
        
        // 5. Test Request
        debugInfo.append("5. TEST REQUEST BODY:\n")
        val testRequestJson = JSONObject().apply {
            put("model", modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Hello")
                })
            })
            put("max_tokens", 10)
            put("temperature", 0.7)
            put("stream", false)
        }
        debugInfo.append("   ${testRequestJson.toString(2)}\n\n")
        
        // 6. Headers
        debugInfo.append("6. REQUEST HEADERS:\n")
        debugInfo.append("   Authorization: Bearer ${if (currentApiKey.isNullOrBlank()) "NOT SET" else "***"}\n")
        debugInfo.append("   Content-Type: application/json\n\n")
        
        // 7. Test the connection and get response
        debugInfo.append("7. CONNECTION TEST:\n")
        try {
            if (!isReady()) {
                debugInfo.append("   ❌ FAILED: API not ready (missing endpoint or API key)\n")
            } else {
                val testResult = testConnection()
                debugInfo.append("   ${if (testResult) "✅ SUCCESS" else "❌ FAILED"}: Connection test\n")
            }
        } catch (e: Exception) {
            debugInfo.append("   ❌ ERROR: ${e.message}\n")
        }
        
        debugInfo.append("\n=== END DEBUG INFORMATION ===")
        
        debugInfo.toString()
    }
}

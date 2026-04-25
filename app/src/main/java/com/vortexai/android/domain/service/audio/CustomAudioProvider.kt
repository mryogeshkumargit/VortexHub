package com.vortexai.android.domain.service.audio

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
class CustomAudioProvider @Inject constructor() {
    
    private var apiKey: String? = null
    private var customEndpoint: String? = null
    private var apiPrefix: String = "/v1"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // 2 minutes for audio generation
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Timber.i("Custom Audio API key set")
    }
    
    fun setEndpoint(endpoint: String) {
        this.customEndpoint = endpoint.trim().removeSuffix("/")
        Timber.i("Custom Audio API endpoint set: $customEndpoint")
    }
    
    fun setApiPrefix(prefix: String) {
        this.apiPrefix = prefix.trim().let { 
            if (!it.startsWith("/")) "/$it" else it 
        }
        Timber.i("Custom Audio API prefix set: $apiPrefix")
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
    
    fun isReady(): Boolean {
        return !apiKey.isNullOrBlank() && !customEndpoint.isNullOrBlank()
    }
    
    /**
     * Generate speech using OpenAI-compatible format
     */
    suspend fun generateSpeech(
        input: String,
        model: String = "tts-1",
        voice: String = "alloy",
        responseFormat: String = "mp3",
        speed: Float = 1.0f
    ): Result<CustomAudioResult> = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            return@withContext Result.failure(IllegalStateException("Custom Audio API provider not ready - API key or endpoint not set"))
        }
        
        try {
            // Build request JSON following OpenAI format
            val requestJson = JSONObject().apply {
                put("model", model)
                put("input", input)
                put("voice", voice)
                put("response_format", responseFormat)
                put("speed", speed)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url(getFullEndpoint("/audio/speech"))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(httpRequest).execute()
            val generationTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("Custom Audio API error: ${response.code} - $errorBody")
                return@withContext Result.failure(Exception("Custom Audio API error: ${response.code} - $errorBody"))
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response from Custom Audio API"))
            
            // Check if response is HTML (error page) instead of JSON
            if (responseBody.trim().startsWith("<!doctype", ignoreCase = true) || 
                responseBody.trim().startsWith("<html", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Custom Audio API returned HTML error page instead of JSON. Please check your endpoint URL and API configuration."))
            }
            
            // Parse response - could be direct audio data or JSON with audio URL
            val audioUrl = try {
                // Try to parse as JSON first
                val responseJson = JSONObject(responseBody)
                responseJson.optString("url", "")
            } catch (e: Exception) {
                // If not JSON, assume it's a direct audio URL
                responseBody
            }
            
            if (audioUrl.isBlank()) {
                return@withContext Result.failure(Exception("No audio URL in Custom Audio API response"))
            }
            
            Timber.d("Custom Audio API generated speech in ${generationTime}ms")
            Result.success(CustomAudioResult(audioUrl = audioUrl, generationTime = generationTime))
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Custom Audio API")
            Result.failure(e)
        }
    }
    
    /**
     * Test connection to Custom Audio API
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext false
            }
            
            val requestJson = JSONObject().apply {
                put("model", "tts-1")
                put("input", "test")
                put("voice", "alloy")
                put("response_format", "mp3")
                put("speed", 1.0f)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url(getFullEndpoint("/audio/speech"))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            response.isSuccessful
            
        } catch (e: Exception) {
            Timber.e(e, "Error testing Custom Audio API connection")
            false
        }
    }
    
    /**
     * Fetch available voices from Custom Audio API
     */
    suspend fun fetchVoices(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext Result.failure(Exception("Custom Audio API not ready"))
            }
            
            val httpRequest = Request.Builder()
                .url(getFullEndpoint("/audio/voices"))
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch voices: ${response.code}"))
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            
            val responseJson = JSONObject(responseBody)
            val voicesArray = responseJson.optJSONArray("data")
            val voices = mutableListOf<String>()
            
            if (voicesArray != null) {
                for (i in 0 until voicesArray.length()) {
                    val voice = voicesArray.optJSONObject(i)
                    if (voice != null) {
                        val voiceId = voice.optString("id")
                        if (voiceId.isNotBlank()) {
                            voices.add(voiceId)
                        }
                    }
                }
            }
            
            Result.success(voices)
            
        } catch (e: Exception) {
            Timber.e(e, "Error fetching Custom Audio API voices")
            Result.failure(e)
        }
    }
    
    /**
     * Fetch available models from Custom Audio API
     */
    suspend fun fetchModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext Result.failure(Exception("Custom Audio API not ready"))
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
            
            val responseJson = JSONObject(responseBody)
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
            Timber.e(e, "Error fetching Custom Audio API models")
            Result.failure(e)
        }
    }
}

data class CustomAudioResult(
    val audioUrl: String,
    val generationTime: Long
)

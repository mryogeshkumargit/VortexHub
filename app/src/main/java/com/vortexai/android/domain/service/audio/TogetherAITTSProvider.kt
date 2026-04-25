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
class TogetherAITTSProvider @Inject constructor() {
    
    companion object {
        private const val BASE_URL = "https://api.together.xyz/v1"
        private const val TAG = "TogetherAITTSProvider"
    }
    
    private var apiKey: String? = null
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Timber.i("Together AI TTS API key set")
    }
    
    fun isReady(): Boolean {
        return !apiKey.isNullOrBlank()
    }
    
    /**
     * Generate speech using Together AI TTS API
     */
    suspend fun generateSpeech(
        text: String,
        model: String = "cartesia/sonic",
        voice: String = "79a125e8-cd45-4c13-8a67-188112f4dd22",
        speed: Float = 1.0f
    ): Result<TogetherAITTSResult> = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            return@withContext Result.failure(IllegalStateException("Together AI TTS API key not set"))
        }
        
        try {
            val requestJson = JSONObject().apply {
                put("model", model)
                put("input", text)
                put("voice", voice)
                put("response_format", "mp3")
                put("speed", speed)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$BASE_URL/audio/speech")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(httpRequest).execute()
            val generationTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("Together AI TTS API error: ${response.code} - $errorBody")
                return@withContext Result.failure(Exception("Together AI TTS API error: ${response.code} - $errorBody"))
            }
            
            val audioBytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty audio response from Together AI TTS API"))
            
            Timber.d("Together AI TTS generated speech in ${generationTime}ms, ${audioBytes.size} bytes")
            Result.success(TogetherAITTSResult(audioData = audioBytes, generationTime = generationTime))
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Together AI TTS API")
            Result.failure(e)
        }
    }
    
    /**
     * Test connection to Together AI TTS API
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext false
            }
            
            val requestJson = JSONObject().apply {
                put("model", "cartesia/sonic")
                put("input", "test")
                put("voice", "79a125e8-cd45-4c13-8a67-188112f4dd22")
                put("response_format", "mp3")
                put("speed", 1.0f)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$BASE_URL/audio/speech")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            response.isSuccessful
            
        } catch (e: Exception) {
            Timber.e(e, "Error testing Together AI TTS API connection")
            false
        }
    }
    
    /**
     * Get available TTS models
     */
    fun getAvailableModels(): List<String> {
        return listOf(
            "cartesia/sonic",
            "cartesia/sonic-2"
        )
    }
    
    /**
     * Get available voices with their IDs and names
     */
    fun getAvailableVoices(): List<Pair<String, String>> {
        return listOf(
            "79a125e8-cd45-4c13-8a67-188112f4dd22" to "Barbershop Man",
            "a0e99841-438c-4a64-b679-ae501e7d6091" to "Conversational Woman",
            "2ee87190-8f84-4925-97da-e52547f9462c" to "Customer Service Woman",
            "820a3788-2b37-4d21-847a-b65d8a68c99a" to "Newscaster Man",
            "fb26447f-308b-471e-8b00-8e9f04284eb5" to "Newscaster Woman"
        )
    }
    
    /**
     * Get voice ID from voice name
     */
    fun getVoiceId(voiceName: String): String {
        return getAvailableVoices().find { it.second == voiceName }?.first 
            ?: "79a125e8-cd45-4c13-8a67-188112f4dd22" // Default to Barbershop Man
    }
}

data class TogetherAITTSResult(
    val audioData: ByteArray,
    val generationTime: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TogetherAITTSResult

        if (!audioData.contentEquals(other.audioData)) return false
        if (generationTime != other.generationTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = audioData.contentHashCode()
        result = 31 * result + generationTime.hashCode()
        return result
    }
}
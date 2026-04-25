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
class ElevenLabsTTSProvider @Inject constructor() {
    
    companion object {
        private const val BASE_URL = "https://api.elevenlabs.io/v1"
        private const val TAG = "ElevenLabsTTSProvider"
    }
    
    private var apiKey: String? = null
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Timber.i("ElevenLabs TTS API key set")
    }
    
    fun isReady(): Boolean {
        return !apiKey.isNullOrBlank()
    }
    
    suspend fun generateSpeech(
        text: String,
        model: String = "eleven_multilingual_v2",
        voice: String = "Rachel",
        stability: Float = 0.5f,
        similarityBoost: Float = 0.5f
    ): Result<ElevenLabsTTSResult> = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            return@withContext Result.failure(IllegalStateException("ElevenLabs API key not set"))
        }
        
        try {
            val voiceId = getVoiceId(voice)
            
            val requestJson = JSONObject().apply {
                put("text", text)
                put("model_id", model)
                put("voice_settings", JSONObject().apply {
                    put("stability", stability)
                    put("similarity_boost", similarityBoost)
                })
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$BASE_URL/text-to-speech/$voiceId")
                .addHeader("Accept", "audio/mpeg")
                .addHeader("Content-Type", "application/json")
                .addHeader("xi-api-key", apiKey!!)
                .post(requestBody)
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(httpRequest).execute()
            val generationTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("ElevenLabs API error: ${response.code} - $errorBody")
                return@withContext Result.failure(Exception("ElevenLabs API error: ${response.code} - $errorBody"))
            }
            
            val audioBytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty audio response from ElevenLabs API"))
            
            Timber.d("ElevenLabs generated speech in ${generationTime}ms, ${audioBytes.size} bytes")
            Result.success(ElevenLabsTTSResult(audioData = audioBytes, generationTime = generationTime))
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling ElevenLabs API")
            Result.failure(e)
        }
    }
    
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext false
            }
            
            val voiceId = getVoiceId("Rachel")
            
            val requestJson = JSONObject().apply {
                put("text", "test")
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.5)
                })
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$BASE_URL/text-to-speech/$voiceId")
                .addHeader("Accept", "audio/mpeg")
                .addHeader("Content-Type", "application/json")
                .addHeader("xi-api-key", apiKey!!)
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            response.isSuccessful
            
        } catch (e: Exception) {
            Timber.e(e, "Error testing ElevenLabs API connection")
            false
        }
    }
    
    fun getAvailableModels(): List<String> {
        return listOf(
            "eleven_multilingual_v2",
            "eleven_turbo_v2_5",
            "eleven_turbo_v2",
            "eleven_monolingual_v1",
            "eleven_multilingual_v1"
        )
    }
    
    fun getEnglishVoices(): Map<String, String> {
        return mapOf(
            "Rachel" to "21m00Tcm4TlvDq8ikWAM",
            "Drew" to "29vD33N1CtxCmqQRPOHJ",
            "Clyde" to "2EiwWnXFnvU5JabPnv8n",
            "Paul" to "5Q0t7uMcjvnagumLfvZi",
            "Domi" to "AZnzlk1XvdvUeBnXmlld",
            "Dave" to "CYw3kZ02Hs0563khs1Fj",
            "Fin" to "D38z5RcWu1voky8WS1ja",
            "Sarah" to "EXAVITQu4vr4xnSDxMaL",
            "Antoni" to "ErXwobaYiN019PkySvjV",
            "Thomas" to "GBv7mTt0atIp3Br8iCZE"
        )
    }
    
    fun getHindiVoices(): Map<String, String> {
        return mapOf(
            "Prabhat" to "pNInz6obpgDQGcFmaJgB",
            "Abhishek" to "JBFqnCBsd6RMkjVDRZzb",
            "Aditi" to "Xb7hH8MSUJpSbSDYk0k2",
            "Arjun" to "bVMeCyTHy58xNoL34h3p",
            "Kavya" to "YoX06hSQ4eaAhyEXGBJO"
        )
    }
    
    fun getVoiceId(voiceName: String): String {
        return getEnglishVoices()[voiceName] 
            ?: getHindiVoices()[voiceName] 
            ?: "21m00Tcm4TlvDq8ikWAM" // Default to Rachel
    }
}

data class ElevenLabsTTSResult(
    val audioData: ByteArray,
    val generationTime: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ElevenLabsTTSResult

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
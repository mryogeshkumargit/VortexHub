package com.vortexai.android.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ModelsLab Text-to-Speech API service
 * Handles voice generation using ModelsLab's TTS endpoints
 */
@Singleton
class ModelsLabTTSApi @Inject constructor() {
    
    companion object {
        private const val TAG = "ModelsLabTTSApi"
        private const val BASE_URL = "https://modelslab.com/api/v6"
        
        private val logger = Logger.getLogger(TAG)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // TTS can take longer
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .build()

    /**
     * Generate speech from text using ModelsLab TTS API
     * @param apiKey ModelsLab API key
     * @param request TTS generation request
     * @return Result containing the audio URL or error
     */
    suspend fun textToAudio(apiKey: String, request: TTSRequest): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Starting ModelsLab TTS request")
                
                val payload = JSONObject().apply {
                    put("key", apiKey)
                    put("model_id", request.modelId)
                    put("prompt", request.text)
                    put("voice_id", request.voiceId)
                    put("language", request.language)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$BASE_URL/voice/text_to_audio")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }

                val responseBody = response.body?.string() ?: "{}"
                val json = JSONObject(responseBody)
                
                if (json.optString("status") != "success") {
                    return@withContext Result.failure(Exception(json.optString("message", "TTS generation failed")))
                }
                
                // Get audio URL from response
                val output = json.optJSONArray("output")?.optString(0) ?: json.optString("output")
                if (output.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No audio URL returned"))
                } else {
                    logger.log(Level.INFO, "ModelsLab TTS success")
                    return@withContext Result.success(output)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab TTS error", e)
                return@withContext Result.failure(e)
            }
        }

    /**
     * Get list of available voices from ModelsLab model list API
     * Fetches voices with sound_clip field for voice cloning
     */
    suspend fun getAvailableVoices(apiKey: String): Result<List<Voice>> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Fetching available voices from ModelsLab API")
                
                val payload = JSONObject().apply { put("key", apiKey) }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                
                val req = Request.Builder()
                    .url("https://modelslab.com/api/v4/dreambooth/model_list")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(req).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val voices = parseVoicesFromModelList(responseBody)
                        if (voices.isNotEmpty()) {
                            logger.log(Level.INFO, "Successfully fetched ${voices.size} voices from API")
                            return@withContext Result.success(voices)
                        }
                    }
                }
                
                // If API fails, return default voices including pre-trained ones
                logger.log(Level.WARNING, "Voice API call failed, returning default voices")
                val defaultVoices = getDefaultVoices()
                Result.success(defaultVoices)
                
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error fetching voices", e)
                Result.success(getDefaultVoices()) // Return defaults instead of failing
            }
        }

    /**
     * Parse voices from model list API response
     * Looks for models with sound_clip field (text-to-audio voices)
     */
    private fun parseVoicesFromModelList(responseBody: String): List<Voice> {
        val voices = mutableListOf<Voice>()
        try {
            val jsonArray = org.json.JSONArray(responseBody)
            logger.log(Level.INFO, "Processing ${jsonArray.length()} models for voices")
            
            for (i in 0 until jsonArray.length()) {
                val modelObj = jsonArray.getJSONObject(i)
                
                // Look for voice_cloning category models
                val modelCategory = modelObj.optString("model_category", "")
                if (modelCategory != "Audiogen") {
                    continue
                }
                
                val status = modelObj.optString("status", "")
                if (status != "model_ready") {
                    continue
                }
                
                val voiceId = modelObj.optString("voice_id", "")
                val name = modelObj.optString("name", voiceId)
                val language = modelObj.optString("language", "english")
                val soundClip = modelObj.optString("sound_clip", "")
                
                if (voiceId.isNotBlank() && soundClip.isNotBlank()) {
                    voices.add(Voice(voiceId, name, language))
                }
            }
            
            logger.log(Level.INFO, "Found ${voices.size} voices with sound clips")
            
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing voices from model list: ${e.message}")
        }
        return voices
    }

    /**
     * Get default voices including pre-trained TTS voices from documentation
     */
    private fun getDefaultVoices(): List<Voice> {
        return listOf(
            // Pre-trained TTS voices (American English)
            Voice("nova", "Nova", "english"),
            Voice("madison", "Madison", "english"),
            Voice("jessica", "Jessica", "english"),
            Voice("kimberly", "Kimberly", "english"),
            Voice("bella", "Bella", "english"),
            Voice("nicole", "Nicole", "english"),
            Voice("savannah", "Savannah", "english"),
            Voice("sarah", "Sarah", "english"),
            Voice("sophia", "Sophia", "english"),
            Voice("olivia", "Olivia", "english"),
            Voice("sierra", "Sierra", "english"),
            
            // Pre-trained TTS voices (English)
            Voice("tara", "Tara", "english"),
            Voice("zoe", "Zoe", "english"),
            Voice("tess", "Tess", "english"),
            Voice("leah", "Leah", "english"),
            Voice("mia", "Mia", "english"),
            
            // Pre-trained TTS voices (Hindi)
            Voice("riya", "Riya", "hindi"),
            Voice("anaya", "Anaya", "hindi")
        )
    }
}

/**
 * TTS request parameters
 */
data class TTSRequest(
    val text: String,
    val modelId: String,
    val voiceId: String,
    val language: String = "english"
)

/**
 * Voice information
 */
data class Voice(
    val id: String,
    val name: String,
    val language: String
)

/**
 * TTS response
 */
data class TTSResponse(
    val status: String,
    val audioUrl: String,
    val generationTime: Float? = null,
    val id: Int? = null,
    val proxyLinks: List<String>? = null,
    val meta: TTSMeta? = null
)

/**
 * TTS metadata
 */
data class TTSMeta(
    val base64: String,
    val emotion: String,
    val filename: String,
    val inputSoundClip: List<String>? = null,
    val inputText: String,
    val language: String,
    val speed: Float,
    val temp: String
)
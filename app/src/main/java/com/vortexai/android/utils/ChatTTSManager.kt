package com.vortexai.android.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced TTS Manager that uses ModelsLab TTS with selected voice from settings
 * Falls back to system TTS if ModelsLab is not configured or fails
 */
@Singleton
class ChatTTSManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "ChatTTSManager"
        
        // Preference keys (matching AudioSettingsManager exactly)
        private val TTS_PROVIDER_KEY = stringPreferencesKey("tts_provider")
        private val TTS_API_KEY = stringPreferencesKey("tts_api_key")
        private val MODELSLAB_API_KEY = stringPreferencesKey("modelslab_api_key")
        private val ELEVENLABS_API_KEY = stringPreferencesKey("elevenlabs_api_key")
        private val TTS_VOICE_KEY = stringPreferencesKey("tts_voice")
        private val TTS_VOICE_HINDI_KEY = stringPreferencesKey("tts_voice_hindi")
        private val TTS_MODEL_KEY = stringPreferencesKey("tts_model")
        private val TTS_LANGUAGE_KEY = stringPreferencesKey("tts_language")
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Speak text using the configured TTS provider and voice
     * Returns error message if failed, null if successful
     */
    suspend fun speak(text: String, context: Context): String? {
        try {
            val preferences = dataStore.data.first()
            val ttsProvider = preferences[TTS_PROVIDER_KEY]
            if (ttsProvider.isNullOrBlank()) {
                return "No TTS provider configured. Please configure in Settings → Audio."
            }
            
            val apiKey = preferences[TTS_API_KEY] ?: ""
            val voice = preferences[TTS_VOICE_KEY]
            val model = preferences[TTS_MODEL_KEY]
            
            Log.d(TAG, "=== ChatTTSManager Debug ===")
            Log.d(TAG, "TTS Provider: $ttsProvider")
            Log.d(TAG, "API Key present: ${apiKey.isNotBlank()} (${apiKey.take(8)}...)")
            Log.d(TAG, "Voice: $voice")
            Log.d(TAG, "Model: $model")
            Log.d(TAG, "Text to speak: ${text.take(50)}...")
            Log.d(TAG, "===============================")
            
            return when (ttsProvider) {
                "ModelsLab" -> {
                    Log.d(TAG, "Attempting ModelsLab TTS")
                    speakWithModelsLab(text, preferences)
                }
                "ElevenLabs" -> {
                    Log.d(TAG, "Attempting ElevenLabs TTS")
                    speakWithElevenLabs(text, preferences)
                }
                else -> {
                    Log.w(TAG, "Unknown TTS provider: $ttsProvider")
                    "Unknown TTS provider: $ttsProvider"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTS", e)
            return "TTS Error: ${e.message}"
        }
    }
    
    /**
     * Use ModelsLab TTS API with selected voice
     * Returns error message if failed, null if successful
     */
    private suspend fun speakWithModelsLab(text: String, preferences: Preferences): String? {
        try {
            var apiKey = preferences[TTS_API_KEY]
            if (apiKey.isNullOrBlank()) {
                apiKey = preferences[MODELSLAB_API_KEY]
                if (apiKey.isNullOrBlank()) {
                    return "ModelsLab API key not configured. Please add in Settings → Audio."
                }
            }
            
            val voice = preferences[TTS_VOICE_KEY]
            if (voice.isNullOrBlank()) {
                return "No TTS voice selected. Please select in Settings → Audio."
            }
            
            val model = preferences[TTS_MODEL_KEY]
            if (model.isNullOrBlank()) {
                return "No TTS model selected. Please select in Settings → Audio."
            }
            val language = preferences[TTS_LANGUAGE_KEY] ?: "english"
            
            Log.d(TAG, "ModelsLab TTS API Key: ${apiKey.take(8)}...")
            Log.d(TAG, "Using ModelsLab TTS with voice: $voice, model: $model, language: $language")
            Log.d(TAG, "Text to speak: $text")
            
            // Use the correct API format
            val payload = JSONObject().apply {
                put("key", apiKey)
                put("model_id", model)
                put("prompt", text)
                put("voice_id", voice)
                put("language", language)
            }
            
            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://modelslab.com/api/v6/voice/text_to_audio")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            Log.d(TAG, "Sending request to: ${request.url}")
            Log.d(TAG, "Request payload: ${payload.toString()}")
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response headers: ${response.headers}")
            Log.d(TAG, "ModelsLab response: $responseBody")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "ModelsLab TTS API error: ${response.code}, body: $responseBody")
                val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val errorMsg = json?.optString("message") ?: json?.optString("error") ?: "HTTP ${response.code}"
                return "ModelsLab TTS Error: $errorMsg"
            }
            
            val json = JSONObject(responseBody)
            
            // Check for different possible response formats
            val status = json.optString("status", "")
            val message = json.optString("message", "")
            
            Log.d(TAG, "Response status: $status")
            Log.d(TAG, "Response message: $message")
            
            if (json.optString("status") != "success") {
                val errorMsg = json.optString("message", "Unknown error")
                Log.e(TAG, "ModelsLab TTS failed: $errorMsg")
                return "ModelsLab TTS: $errorMsg"
            }
            
            // Get audio URL from response
            val audioUrl = json.optJSONArray("output")?.optString(0) ?: json.optString("output")
            if (audioUrl.isNullOrBlank()) {
                Log.e(TAG, "No audio URL found in response. Full response: $responseBody")
                return "ModelsLab TTS: No audio generated"
            }
            
            // Play the audio using AudioPlayer
            Log.d(TAG, "Playing audio from URL: $audioUrl")
            AudioPlayer.playAudio(
                audioUrl = audioUrl,
                onCompletion = {
                    Log.d(TAG, "ModelsLab TTS playback completed successfully")
                },
                onError = { error ->
                    Log.e(TAG, "ModelsLab TTS playback error: $error")
                    Log.e(TAG, "Audio URL was: $audioUrl")
                }
            )
            
            return null
            
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "ModelsLab TTS network error: No internet connection", e)
            return "No internet connection"
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "ModelsLab TTS timeout error", e)
            return "Request timeout. Please try again."
        } catch (e: Exception) {
            Log.e(TAG, "ModelsLab TTS error", e)
            return "ModelsLab TTS Error: ${e.message}"
        }
    }
    
    private suspend fun speakWithElevenLabs(text: String, preferences: Preferences): String? {
        try {
            val apiKey = preferences[ELEVENLABS_API_KEY]
            if (apiKey.isNullOrBlank()) {
                return "ElevenLabs API key not configured. Please add in Settings → Audio."
            }
            
            val voice = preferences[TTS_VOICE_KEY] ?: "Rachel"
            val model = preferences[TTS_MODEL_KEY] ?: "eleven_multilingual_v2"
            
            Log.d(TAG, "ElevenLabs TTS API Key: ${apiKey.take(8)}...")
            Log.d(TAG, "Using ElevenLabs TTS with voice: $voice, model: $model")
            
            val voiceId = getElevenLabsVoiceId(voice)
            
            val payload = JSONObject().apply {
                put("text", text)
                put("model_id", model)
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.5)
                })
            }
            
            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .post(requestBody)
                .addHeader("Accept", "audio/mpeg")
                .addHeader("Content-Type", "application/json")
                .addHeader("xi-api-key", apiKey)
                .build()
            
            Log.d(TAG, "Sending ElevenLabs request to: ${request.url}")
            
            val response = httpClient.newCall(request).execute()
            
            Log.d(TAG, "ElevenLabs response code: ${response.code}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "ElevenLabs API error: ${response.code}, body: $errorBody")
                val json = try { JSONObject(errorBody) } catch (e: Exception) { null }
                val errorMsg = json?.optString("detail")?.let { detail ->
                    val detailJson = try { JSONObject(detail) } catch (e: Exception) { null }
                    detailJson?.optString("message") ?: detail
                } ?: "HTTP ${response.code}"
                return "ElevenLabs TTS Error: $errorMsg"
            }
            
            val audioBytes = response.body?.bytes()
            if (audioBytes == null || audioBytes.isEmpty()) {
                Log.e(TAG, "No audio data received from ElevenLabs")
                return "ElevenLabs TTS: No audio generated"
            }
            
            Log.d(TAG, "Received ${audioBytes.size} bytes of audio from ElevenLabs")
            
            // Play the audio using AudioPlayer with byte array
            AudioPlayer.playAudioBytes(
                audioBytes = audioBytes,
                onCompletion = {
                    Log.d(TAG, "ElevenLabs TTS playback completed successfully")
                },
                onError = { error ->
                    Log.e(TAG, "ElevenLabs TTS playback error: $error")
                }
            )
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs TTS error", e)
            return "ElevenLabs TTS Error: ${e.message}"
        }
    }
    
    private fun getElevenLabsVoiceId(voiceName: String): String {
        // If already a voice ID (20+ chars), return as-is
        if (voiceName.length >= 20) {
            return voiceName
        }
        
        val englishVoices = mapOf(
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
        
        val hindiVoices = mapOf(
            "Prabhat" to "pNInz6obpgDQGcFmaJgB",
            "Abhishek" to "JBFqnCBsd6RMkjVDRZzb",
            "Aditi" to "Xb7hH8MSUJpSbSDYk0k2",
            "Arjun" to "bVMeCyTHy58xNoL34h3p",
            "Kavya" to "YoX06hSQ4eaAhyEXGBJO"
        )
        
        return englishVoices[voiceName] ?: hindiVoices[voiceName] ?: voiceName
    }
    

    
    /**
     * Stop any playing audio
     */
    fun stop() {
        AudioPlayer.stopAudio()
    }
    
    /**
     * Force refresh preferences (for immediate voice changes)
     */
    fun refreshPreferences() {
        // This will force the next speak() call to read fresh preferences
        Log.d(TAG, "TTS preferences refreshed")
    }
}

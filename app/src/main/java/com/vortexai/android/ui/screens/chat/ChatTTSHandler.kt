package com.vortexai.android.ui.screens.chat

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vortexai.android.utils.ChatTTSManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatTTSHandler @Inject constructor(
    private val chatTTSManager: ChatTTSManager,
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private const val TAG = "ChatTTSHandler"
    }
    
    suspend fun speakText(text: String, context: Context, onError: ((String) -> Unit)? = null) {
        try {
            val error = chatTTSManager.speak(text, context)
            if (error != null) {
                Log.e(TAG, "TTS Error: $error")
                onError?.invoke(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTS", e)
            onError?.invoke("TTS Error: ${e.message}")
        }
    }
    
    fun stopTTS() {
        chatTTSManager.stop()
    }
    

    
    suspend fun testTTS(text: String, context: Context) {
        Log.d(TAG, "Testing TTS with text: $text")
        android.widget.Toast.makeText(context, "Testing TTS...", android.widget.Toast.LENGTH_SHORT).show()
        speakText(text, context)
    }
    
    suspend fun debugTTSPreferences(context: Context) {
        try {
            android.widget.Toast.makeText(context, "Checking TTS preferences...", android.widget.Toast.LENGTH_SHORT).show()
            
            val preferences = dataStore.data.first()
            
            Log.d(TAG, "=== TTS Preferences Debug ===")
            Log.d(TAG, "All preference keys: ${preferences.asMap().keys}")
            
            val ttsApiKey = preferences[stringPreferencesKey("tts_api_key")]
            val modelslabApiKey = preferences[stringPreferencesKey("modelslab_api_key")]
            val ttsVoice = preferences[stringPreferencesKey("tts_voice")]
            val ttsModel = preferences[stringPreferencesKey("tts_model")]
            val ttsProvider = preferences[stringPreferencesKey("tts_provider")]
            
            Log.d(TAG, "tts_api_key: $ttsApiKey")
            Log.d(TAG, "modelslab_api_key: $modelslabApiKey")
            Log.d(TAG, "tts_voice: $ttsVoice")
            Log.d(TAG, "tts_model: $ttsModel")
            Log.d(TAG, "tts_provider: $ttsProvider")
            
            val finalApiKey = ttsApiKey ?: modelslabApiKey ?: ""
            
            if (finalApiKey.isBlank()) {
                android.widget.Toast.makeText(context, "No API key found in preferences", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, "API key found: ${finalApiKey.take(8)}...", android.widget.Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTS preferences debug", e)
            android.widget.Toast.makeText(context, "Debug error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Get TTS status and configuration
     */
    suspend fun getTTSStatus(): String {
        return try {
            val preferences = dataStore.data.first()
            val provider = preferences[stringPreferencesKey("tts_provider")] ?: "System"
            val voice = preferences[stringPreferencesKey("tts_voice")] ?: "Default"
            val model = preferences[stringPreferencesKey("tts_model")] ?: "Default"
            val apiKey = preferences[stringPreferencesKey("tts_api_key")] ?: ""
            
            buildString {
                appendLine("=== TTS Status ===")
                appendLine("Provider: $provider")
                appendLine("Voice: $voice")
                appendLine("Model: $model")
                appendLine("API Key: ${if (apiKey.isBlank()) "Not configured" else "${apiKey.take(8)}..."}")
                appendLine("Status: ${if (provider == "System" || apiKey.isNotBlank()) "✅ Ready" else "❌ Missing API Key"}")
            }
        } catch (e: Exception) {
            "Error getting TTS status: ${e.message}"
        }
    }
    
    /**
     * Test TTS with different providers
     */
    suspend fun testTTSProvider(provider: String, text: String, context: Context) {
        try {
            Log.d(TAG, "Testing TTS provider: $provider with text: $text")
            
            // Temporarily switch to the test provider
            val originalProvider = dataStore.data.first()[stringPreferencesKey("tts_provider")]
            
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("tts_provider")] = provider
            }
            
            // Test the TTS
            speakText(text, context)
            
            // Restore original provider
            originalProvider?.let { original ->
                dataStore.edit { preferences ->
                    preferences[stringPreferencesKey("tts_provider")] = original
                }
            }
            
            android.widget.Toast.makeText(context, "Testing $provider TTS", android.widget.Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing TTS provider $provider", e)
            android.widget.Toast.makeText(context, "TTS test failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
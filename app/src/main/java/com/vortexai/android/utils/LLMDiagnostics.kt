package com.vortexai.android.utils

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vortexai.android.domain.service.ChatLLMService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMDiagnostics @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val chatLLMService: ChatLLMService
) {
    
    companion object {
        private const val TAG = "LLMDiagnostics"
    }
    
    /**
     * Run comprehensive LLM diagnostics and return detailed report
     */
    suspend fun runDiagnostics(): String {
        val report = StringBuilder()
        
        try {
            report.appendLine("=== VortexAI LLM Diagnostics ===")
            report.appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
            report.appendLine()
            
            // 1. Check current configuration
            val preferences = dataStore.data.first()
            val provider = preferences[stringPreferencesKey("llm_provider")] ?: "Not set"
            val model = preferences[stringPreferencesKey("llm_model")] ?: "Not set"
            
            report.appendLine("1. CURRENT CONFIGURATION:")
            report.appendLine("   Provider: $provider")
            report.appendLine("   Model: $model")
            report.appendLine()
            
            // 2. Check API keys
            report.appendLine("2. API KEY STATUS:")
            val apiKeys = mapOf(
                "Together AI" to preferences[stringPreferencesKey("together_ai_api_key")],
                "Gemini API" to preferences[stringPreferencesKey("gemini_api_key")],
                "Open Router" to preferences[stringPreferencesKey("openrouter_api_key")],
                "Hugging Face" to preferences[stringPreferencesKey("huggingface_api_key")],
                "ModelsLab" to preferences[stringPreferencesKey("modelslab_api_key")],
                "Custom API" to preferences[stringPreferencesKey("custom_llm_api_key")]
            )
            
            apiKeys.forEach { (providerName, key) ->
                val status = when {
                    key.isNullOrBlank() -> "❌ NOT SET"
                    key.length < 10 -> "⚠️ TOO SHORT (${key.length} chars)"
                    else -> "✅ SET (${key.take(8)}...)"
                }
                report.appendLine("   $providerName: $status")
            }
            report.appendLine()
            
            // 3. Check endpoints for local providers
            report.appendLine("3. LOCAL ENDPOINTS:")
            val endpoints = mapOf(
                "Ollama" to preferences[stringPreferencesKey("ollama_endpoint")],
                "Kobold AI" to preferences[stringPreferencesKey("kobold_endpoint")],
                "LMStudio" to preferences[stringPreferencesKey("lmstudio_endpoint")],
                "Custom API" to preferences[stringPreferencesKey("custom_llm_endpoint")]
            )
            
            endpoints.forEach { (providerName, endpoint) ->
                val status = if (endpoint.isNullOrBlank()) "❌ NOT SET" else "✅ SET ($endpoint)"
                report.appendLine("   $providerName: $status")
            }
            report.appendLine()
            
            // 4. Test current provider connection
            report.appendLine("4. CONNECTION TEST:")
            if (provider != "Not set") {
                try {
                    val apiKey = apiKeys[provider] ?: ""
                    val customEndpoint = endpoints[provider]
                    
                    report.appendLine("   Testing $provider connection...")
                    val result = chatLLMService.testConnection(provider, apiKey, model, customEndpoint)
                    
                    if (result.isSuccess) {
                        report.appendLine("   ✅ SUCCESS: Connection to $provider working")
                    } else {
                        report.appendLine("   ❌ FAILED: Connection test failed")
                    }
                } catch (e: Exception) {
                    report.appendLine("   ❌ ERROR: ${e.message}")
                }
            } else {
                report.appendLine("   ⚠️ SKIPPED: No provider selected")
            }
            report.appendLine()
            
            // 5. Quick fixes
            report.appendLine("5. QUICK FIXES:")
            when {
                provider == "Not set" -> {
                    report.appendLine("   • Go to Settings > LLM Config and select a provider")
                }
                model == "Not set" -> {
                    report.appendLine("   • Go to Settings > LLM Config and select a model")
                }
                apiKeys[provider].isNullOrBlank() && provider !in listOf("Ollama", "Kobold AI", "LMStudio") -> {
                    report.appendLine("   • Configure API key for $provider in Settings > LLM Config")
                }
                provider in listOf("Ollama", "Kobold AI", "LMStudio") && endpoints[provider].isNullOrBlank() -> {
                    report.appendLine("   • Configure endpoint for $provider in Settings > LLM Config")
                }
                else -> {
                    report.appendLine("   • Check your internet connection")
                    report.appendLine("   • Verify API key is valid and has sufficient credits")
                    report.appendLine("   • Try a different model or provider")
                }
            }
            
            report.appendLine()
            report.appendLine("=== End Diagnostics ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running diagnostics", e)
            report.appendLine("ERROR: Failed to run diagnostics - ${e.message}")
        }
        
        return report.toString()
    }
    
    /**
     * Get quick status for UI display
     */
    suspend fun getQuickStatus(): String {
        return try {
            val preferences = dataStore.data.first()
            val provider = preferences[stringPreferencesKey("llm_provider")]
            val model = preferences[stringPreferencesKey("llm_model")]
            
            when {
                provider.isNullOrBlank() -> "❌ No provider selected"
                model.isNullOrBlank() -> "⚠️ No model selected"
                else -> "✅ $provider - $model"
            }
        } catch (e: Exception) {
            "❌ Error checking status"
        }
    }
}
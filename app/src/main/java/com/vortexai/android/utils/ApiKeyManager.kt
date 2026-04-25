package com.vortexai.android.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure API key management service
 * Handles storage and retrieval of API keys for various services
 */
@Singleton
class ApiKeyManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        // API Key preference keys
        private val TOGETHER_AI_API_KEY = stringPreferencesKey("together_ai_api_key")
        private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        private val ANTHROPIC_API_KEY = stringPreferencesKey("anthropic_api_key")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val MODELSLAB_API_KEY = stringPreferencesKey("modelslab_api_key")
        private val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        private val HUGGINGFACE_API_KEY = stringPreferencesKey("huggingface_api_key")
        private val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        private val OLLAMA_API_KEY = stringPreferencesKey("ollama_api_key")
        private val KOBOLD_API_KEY = stringPreferencesKey("kobold_api_key")
        
        // Endpoint preference keys
        private val CUSTOM_API_ENDPOINT = stringPreferencesKey("custom_api_endpoint")
        private val OLLAMA_ENDPOINT = stringPreferencesKey("ollama_endpoint")
        private val KOBOLD_ENDPOINT = stringPreferencesKey("kobold_endpoint")
    }
    
    /**
     * Set API key for a specific provider
     */
    suspend fun setApiKey(provider: String, apiKey: String) {
        val key = when (provider.lowercase()) {
            "together ai", "together" -> TOGETHER_AI_API_KEY
            "openai", "gpt" -> OPENAI_API_KEY
            "anthropic", "claude" -> ANTHROPIC_API_KEY
            "gemini", "google" -> GEMINI_API_KEY
            "modelslab" -> MODELSLAB_API_KEY
            "openrouter" -> OPENROUTER_API_KEY
            "huggingface", "hf" -> HUGGINGFACE_API_KEY
            "custom" -> CUSTOM_API_KEY
            "ollama" -> OLLAMA_API_KEY
            "kobold" -> KOBOLD_API_KEY
            else -> throw IllegalArgumentException("Unknown provider: $provider")
        }
        
        dataStore.edit { preferences ->
            preferences[key] = apiKey
        }
    }
    
    /**
     * Get API key for a specific provider
     */
    suspend fun getApiKey(provider: String): String? {
        val key = when (provider.lowercase()) {
            "together ai", "together" -> TOGETHER_AI_API_KEY
            "openai", "gpt" -> OPENAI_API_KEY
            "anthropic", "claude" -> ANTHROPIC_API_KEY
            "gemini", "google" -> GEMINI_API_KEY
            "modelslab" -> MODELSLAB_API_KEY
            "openrouter" -> OPENROUTER_API_KEY
            "huggingface", "hf" -> HUGGINGFACE_API_KEY
            "custom" -> CUSTOM_API_KEY
            "ollama" -> OLLAMA_API_KEY
            "kobold" -> KOBOLD_API_KEY
            else -> return null
        }
        
        return dataStore.data.first()[key]
    }
    
    /**
     * Set endpoint URL for a specific provider
     */
    suspend fun setEndpoint(provider: String, endpoint: String) {
        val key = when (provider.lowercase()) {
            "custom" -> CUSTOM_API_ENDPOINT
            "ollama" -> OLLAMA_ENDPOINT
            "kobold" -> KOBOLD_ENDPOINT
            else -> throw IllegalArgumentException("Endpoint not configurable for provider: $provider")
        }
        
        dataStore.edit { preferences ->
            preferences[key] = endpoint
        }
    }
    
    /**
     * Get endpoint URL for a specific provider
     */
    suspend fun getEndpoint(provider: String): String? {
        val key = when (provider.lowercase()) {
            "custom" -> CUSTOM_API_ENDPOINT
            "ollama" -> OLLAMA_ENDPOINT
            "kobold" -> KOBOLD_ENDPOINT
            else -> return null
        }
        
        return dataStore.data.first()[key]
    }
    
    /**
     * Check if API key is configured for a provider
     */
    suspend fun hasApiKey(provider: String): Boolean {
        return !getApiKey(provider).isNullOrBlank()
    }
    
    /**
     * Get all configured providers
     */
    suspend fun getConfiguredProviders(): List<String> {
        val preferences = dataStore.data.first()
        val providers = mutableListOf<String>()
        
        if (!preferences[TOGETHER_AI_API_KEY].isNullOrBlank()) providers.add("Together AI")
        if (!preferences[OPENAI_API_KEY].isNullOrBlank()) providers.add("OpenAI")
        if (!preferences[ANTHROPIC_API_KEY].isNullOrBlank()) providers.add("Anthropic")
        if (!preferences[GEMINI_API_KEY].isNullOrBlank()) providers.add("Gemini")
        if (!preferences[MODELSLAB_API_KEY].isNullOrBlank()) providers.add("ModelsLab")
        if (!preferences[OPENROUTER_API_KEY].isNullOrBlank()) providers.add("OpenRouter")
        if (!preferences[HUGGINGFACE_API_KEY].isNullOrBlank()) providers.add("HuggingFace")
        if (!preferences[CUSTOM_API_KEY].isNullOrBlank()) providers.add("Custom")
        
        // Local providers don't require API keys
        providers.add("Ollama")
        providers.add("Kobold")
        
        return providers
    }
    
    /**
     * Clear all API keys (for logout/reset)
     */
    suspend fun clearAllKeys() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Validate API key format for a provider
     */
    fun validateApiKey(provider: String, apiKey: String): Boolean {
        return when (provider.lowercase()) {
            "together ai", "together" -> apiKey.length >= 32 && apiKey.all { it.isLetterOrDigit() || it == '-' || it == '_' }
            "openai", "gpt" -> apiKey.startsWith("sk-") && apiKey.length >= 20
            "anthropic", "claude" -> apiKey.startsWith("sk-ant-") && apiKey.length >= 20
            "gemini", "google" -> apiKey.length >= 20 && apiKey.all { it.isLetterOrDigit() || it == '-' || it == '_' }
            "modelslab" -> apiKey.length >= 20 && apiKey.all { it.isLetterOrDigit() }
            "openrouter" -> apiKey.startsWith("sk-or-") && apiKey.length >= 20
            "huggingface", "hf" -> apiKey.startsWith("hf_") && apiKey.length >= 20
            "custom" -> apiKey.isNotBlank() // Custom APIs can have any format
            "ollama", "kobold" -> true // Local providers don't need API keys
            else -> false
        }
    }
}
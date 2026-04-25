package com.vortexai.android.ui.screens.settings.managers

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.domain.service.ChatLLMService
import com.vortexai.android.ui.screens.settings.ModelInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMSettingsManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val chatLLMService: ChatLLMService
) {
    suspend fun loadLLMSettings(currentState: SettingsUiState): SettingsUiState {
        val preferences = settingsDataStore.getPreferences()
        
        return currentState.copy(
            llmProvider = preferences[stringPreferencesKey("llm_provider")] ?: "Together AI",
            llmModel = preferences[stringPreferencesKey("llm_model")] ?: "",
            togetherAiApiKey = preferences[stringPreferencesKey("together_ai_api_key")] ?: "",
            geminiApiKey = preferences[stringPreferencesKey("gemini_api_key")] ?: "",
            openRouterApiKey = preferences[stringPreferencesKey("openrouter_api_key")] ?: "",
            huggingFaceApiKey = preferences[stringPreferencesKey("huggingface_api_key")] ?: "",
            modelsLabApiKey = preferences[stringPreferencesKey("modelslab_api_key")] ?: "",
            grokApiKey = preferences[stringPreferencesKey("grok_api_key")] ?: "",
            customLlmApiKey = preferences[stringPreferencesKey("custom_llm_api_key")] ?: "",
            customLlmApiPrefix = preferences[stringPreferencesKey("custom_llm_api_prefix")] ?: "/v1",
            ollamaEndpoint = preferences[stringPreferencesKey("ollama_endpoint")] ?: "http://localhost:11435",
            koboldEndpoint = preferences[stringPreferencesKey("kobold_endpoint")] ?: "http://localhost:5000",
            lmStudioEndpoint = preferences[stringPreferencesKey("lmstudio_endpoint")] ?: "http://localhost:1234",
            customLlmEndpoint = preferences[stringPreferencesKey("custom_llm_endpoint")] ?: "",
            selectedCustomLlmProviderId = preferences[stringPreferencesKey("selected_custom_llm_provider_id")] ?: "",
            responseTemperature = preferences[floatPreferencesKey("response_temperature")] ?: 0.7f,
            maxTokens = preferences[intPreferencesKey("max_tokens")] ?: 2048,
            topP = preferences[floatPreferencesKey("top_p")] ?: 0.9f,
            frequencyPenalty = preferences[floatPreferencesKey("frequency_penalty")] ?: 0.0f,
            responseLengthStyle = preferences[stringPreferencesKey("response_length_style")] ?: "natural",
            enableResponseFormatting = preferences[stringPreferencesKey("enable_response_formatting")]?.toBoolean() ?: true,
            customMaxTokens = preferences[intPreferencesKey("custom_max_tokens")] ?: 500,
            manuallyAddedLlmModels = preferences[stringPreferencesKey("manually_added_llm_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            manuallyAddedCustomLlmModels = preferences[stringPreferencesKey("manually_added_custom_llm_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    suspend fun saveLLMSettings(state: SettingsUiState) {
        settingsDataStore.savePreferences { preferences: androidx.datastore.preferences.core.MutablePreferences ->
            preferences[stringPreferencesKey("llm_provider")] = state.llmProvider
            preferences[stringPreferencesKey("llm_model")] = state.llmModel
            preferences[stringPreferencesKey("together_ai_api_key")] = state.togetherAiApiKey
            preferences[stringPreferencesKey("gemini_api_key")] = state.geminiApiKey
            preferences[stringPreferencesKey("openrouter_api_key")] = state.openRouterApiKey
            preferences[stringPreferencesKey("huggingface_api_key")] = state.huggingFaceApiKey
            preferences[stringPreferencesKey("modelslab_api_key")] = state.modelsLabApiKey
            preferences[stringPreferencesKey("grok_api_key")] = state.grokApiKey
            preferences[stringPreferencesKey("custom_llm_api_key")] = state.customLlmApiKey
            preferences[stringPreferencesKey("custom_llm_api_prefix")] = state.customLlmApiPrefix
            preferences[stringPreferencesKey("ollama_endpoint")] = state.ollamaEndpoint
            preferences[stringPreferencesKey("kobold_endpoint")] = state.koboldEndpoint
            preferences[stringPreferencesKey("lmstudio_endpoint")] = state.lmStudioEndpoint
            preferences[stringPreferencesKey("custom_llm_endpoint")] = state.customLlmEndpoint
            preferences[stringPreferencesKey("selected_custom_llm_provider_id")] = state.selectedCustomLlmProviderId
            preferences[floatPreferencesKey("response_temperature")] = state.responseTemperature
            preferences[intPreferencesKey("max_tokens")] = state.maxTokens
            preferences[floatPreferencesKey("top_p")] = state.topP
            preferences[floatPreferencesKey("frequency_penalty")] = state.frequencyPenalty
            preferences[stringPreferencesKey("response_length_style")] = state.responseLengthStyle
            preferences[stringPreferencesKey("enable_response_formatting")] = state.enableResponseFormatting.toString()
            preferences[intPreferencesKey("custom_max_tokens")] = state.customMaxTokens
            preferences[stringPreferencesKey("manually_added_llm_models")] = state.manuallyAddedLlmModels.joinToString(",")
            preferences[stringPreferencesKey("manually_added_custom_llm_models")] = state.manuallyAddedCustomLlmModels.joinToString(",")
        }
    }

    suspend fun fetchModels(provider: String, apiKey: String, customEndpoint: String?): Result<List<ModelInfo>> {
        return try {
            val result = chatLLMService.fetchModels(provider, apiKey, customEndpoint)
            result.fold(
                onSuccess = { models ->
                    Result.success(models.map { ModelInfo(it.id, it.name) })
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
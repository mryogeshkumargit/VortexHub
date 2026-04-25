package com.vortexai.android.domain.service.llm

import com.vortexai.android.data.models.ApiProviderType
import com.vortexai.android.data.repository.CustomApiProviderRepository
import com.vortexai.android.domain.service.CustomApiExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseCustomAPIProvider @Inject constructor(
    private val repository: CustomApiProviderRepository,
    private val executor: CustomApiExecutor
) : LLMProvider {
    
    private var selectedProviderId: String? = null
    
    fun setProviderId(providerId: String) {
        selectedProviderId = providerId
    }
    
    override fun setApiKey(apiKey: String) {
        // Not used - API key comes from database
    }
    
    override fun isReady(): Boolean {
        return selectedProviderId != null
    }
    
    override fun getModelName(): String {
        return runBlocking {
            selectedProviderId?.let { providerId ->
                repository.getActiveModelsByProvider(providerId).first().firstOrNull()?.modelId
            } ?: "unknown"
        }
    }
    
    override fun getMaxTokens(): Int? = 8192
    
    override suspend fun generateResponse(prompt: String, params: GenerationParams): String {
        val providerId = selectedProviderId 
            ?: throw IllegalStateException("No provider selected")
        
        val provider = repository.getProviderById(providerId)
            ?: throw IllegalStateException("Provider not found")
        
        val endpoint = repository.getEndpointByPurpose(providerId, "chat")
            ?: throw IllegalStateException("No chat endpoint configured")
        
        val model = repository.getActiveModelsByProvider(providerId).first().firstOrNull()
            ?: throw IllegalStateException("No active model found")
        
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }
        
        // Get saved parameter values
        val savedValues = repository.getParameterValuesMap(model.id)
        
        val requestParams = mutableMapOf<String, Any>(
            "messages" to messages,
            "temperature" to params.temperature,
            "max_tokens" to params.maxTokens,
            "top_p" to params.topP,
            "frequency_penalty" to params.frequencyPenalty
        )
        
        // Merge with saved parameter values
        requestParams.putAll(savedValues)
        
        val result = executor.executeRequest(provider, endpoint, model, requestParams)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: Exception("Request failed")
        }
        
        val responseJson = result.getOrThrow()
        val parseResult = executor.parseResponse(responseJson, endpoint)
        
        return parseResult.getOrThrow()
    }
}

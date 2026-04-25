package com.vortexai.android.domain.service.llm

/**
 * Interface for LLM providers
 */
interface LLMProvider {
    
    /**
     * Set API key for the provider
     */
    fun setApiKey(apiKey: String)
    
    /**
     * Check if the provider is ready to generate responses
     */
    fun isReady(): Boolean
    
    /**
     * Get the model name
     */
    fun getModelName(): String
    
    /**
     * Get max tokens supported by the model
     */
    fun getMaxTokens(): Int?
    
    /**
     * Generate a response using the provider
     */
    suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String
}

/**
 * Parameters for text generation
 */
data class GenerationParams(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val stop: List<String> = emptyList(),
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f
)

/**
 * Response from LLM provider
 */
data class LLMResponse(
    val text: String,
    val tokensUsed: Int = 0,
    val model: String = "",
    val finishReason: String = "",
    val processingTime: Long = 0L
) 
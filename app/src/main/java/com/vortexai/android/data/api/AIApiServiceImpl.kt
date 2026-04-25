package com.vortexai.android.data.api

import android.util.Log
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Message
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AI API service
 * Handles external API calls to Together AI, Gemini AI, and custom APIs
 */
@Singleton
class AIApiServiceImpl @Inject constructor(
    private val togetherRetrofit: Retrofit,
    private val geminiRetrofit: Retrofit,
    private val customRetrofit: Retrofit
) : AIApiService {
    
    companion object {
        private const val TAG = "AIApiServiceImpl"
        
        // API Keys - These should be stored securely in production
        private const val TOGETHER_API_KEY = "your_together_api_key_here"
        private const val GEMINI_API_KEY = "your_gemini_api_key_here"
        private const val CUSTOM_API_KEY = "your_custom_api_key_here"
    }
    
    private val togetherApiService by lazy { togetherRetrofit.create(AIApiService::class.java) }
    private val geminiApiService by lazy { geminiRetrofit.create(AIApiService::class.java) }
    private val customApiService by lazy { customRetrofit.create(AIApiService::class.java) }
    
    override suspend fun togetherAIChat(authorization: String, request: TogetherAIRequest): TogetherAIResponse {
        return togetherApiService.togetherAIChat(authorization, request)
    }
    
    override suspend fun togetherAIImage(authorization: String, request: TogetherImageRequest): TogetherImageResponse {
        return togetherApiService.togetherAIImage(authorization, request)
    }
    
    override suspend fun geminiChat(apiKey: String, request: GeminiRequest): GeminiResponse {
        return geminiApiService.geminiChat(apiKey, request)
    }
    
    override suspend fun geminiImage(apiKey: String, request: GeminiImageRequest): GeminiImageResponse {
        return geminiApiService.geminiImage(apiKey, request)
    }
    
    override suspend fun customChat(authorization: String, request: CustomChatRequest): CustomChatResponse {
        return customApiService.customChat(authorization, request)
    }
    
    override suspend fun customImage(authorization: String, request: CustomImageRequest): CustomImageResponse {
        return customApiService.customImage(authorization, request)
    }
    
    override suspend fun generateTogetherAIResponse(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): AIResponse {
        try {
            Log.d(TAG, "Generating Together AI response for character: ${character?.name}")
            
            val messages = buildTogetherMessages(message, character, conversationHistory)
            
            val request = TogetherAIRequest(
                messages = messages,
                temperature = character?.temperature ?: 0.7f,
                top_p = character?.topP ?: 0.9f,
                max_tokens = character?.maxTokens ?: 512
            )
            
            val response = togetherAIChat("Bearer $TOGETHER_API_KEY", request)
            
            val content = response.choices.firstOrNull()?.message?.content 
                ?: "I'm sorry, I couldn't generate a response right now."
            
            val usage = response.usage?.let { 
                AIUsage(it.prompt_tokens, it.completion_tokens, it.total_tokens) 
            }
            
            return AIResponse(content, usage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Together AI response", e)
            return AIResponse("I'm sorry, I'm having trouble connecting right now. Please try again.", null)
        }
    }
    
    override suspend fun generateGeminiResponse(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): AIResponse {
        try {
            Log.d(TAG, "Generating Gemini AI response for character: ${character?.name}")
            
            val prompt = buildGeminiPrompt(message, character, conversationHistory)
            
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = character?.temperature ?: 0.7f,
                    topP = character?.topP ?: 0.9f,
                    maxOutputTokens = character?.maxTokens ?: 512
                )
            )
            
            val response = geminiChat(GEMINI_API_KEY, request)
            
            val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I'm sorry, I couldn't generate a response right now."
            
            return AIResponse(content, null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Gemini AI response", e)
            return AIResponse("I'm sorry, I'm having trouble connecting right now. Please try again.", null)
        }
    }
    
    override suspend fun generateCustomAPIResponse(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): AIResponse {
        try {
            Log.d(TAG, "Generating custom API response for character: ${character?.name}")
            
            val request = CustomChatRequest(
                message = message,
                character_id = character?.id,
                character_name = character?.name,
                character_personality = character?.personality,
                conversation_history = conversationHistory.map { 
                    CustomMessage(it.role, it.content, it.createdAt) 
                },
                temperature = character?.temperature ?: 0.7f,
                max_tokens = character?.maxTokens ?: 512
            )
            
            val response = customChat("Bearer $CUSTOM_API_KEY", request)
            
            if (response.success) {
                val usage = response.usage?.let { 
                    AIUsage(it.prompt_tokens, it.completion_tokens, it.total_tokens) 
                }
                return AIResponse(response.response, usage)
            } else {
                return AIResponse("I'm sorry, I couldn't generate a response right now.", null)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating custom API response", e)
            return AIResponse("I'm sorry, I'm having trouble connecting right now. Please try again.", null)
        }
    }
    
    override suspend fun generateTogetherImage(prompt: String, style: String): String {
        try {
            Log.d(TAG, "Generating Together AI image with prompt: $prompt")
            
            val request = TogetherImageRequest(
                prompt = "$prompt, $style style",
                width = 1024,
                height = 1024
            )
            
            val response = togetherAIImage("Bearer $TOGETHER_API_KEY", request)
            
            return response.data.firstOrNull()?.url 
                ?: throw Exception("No image URL returned")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Together AI image", e)
            throw e
        }
    }
    
    override suspend fun generateGeminiImage(prompt: String, style: String): String {
        try {
            Log.d(TAG, "Generating Gemini AI image with prompt: $prompt")
            
            // Note: Gemini Pro Vision is primarily for image analysis, not generation
            // This is a placeholder implementation
            throw Exception("Gemini image generation not yet implemented")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Gemini AI image", e)
            throw e
        }
    }
    
    override suspend fun generateCustomImage(prompt: String, style: String): String {
        try {
            Log.d(TAG, "Generating custom API image with prompt: $prompt")
            
            val request = CustomImageRequest(
                prompt = prompt,
                style = style,
                width = 1024,
                height = 1024
            )
            
            val response = customImage("Bearer $CUSTOM_API_KEY", request)
            
            if (response.success) {
                return response.image_url
            } else {
                throw Exception(response.message ?: "Image generation failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating custom API image", e)
            throw e
        }
    }
    
    /**
     * Build messages for Together AI format
     */
    private fun buildTogetherMessages(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): List<TogetherMessage> {
        val messages = mutableListOf<TogetherMessage>()
        
        // Add system message with character context
        if (character != null) {
            val systemPrompt = buildSystemPrompt(character)
            messages.add(TogetherMessage("system", systemPrompt))
        }
        
        // Add conversation history (last 10 messages)
        conversationHistory.takeLast(10).forEach { msg ->
            val role = when (msg.role) {
                "user" -> "user"
                "assistant" -> "assistant"
                else -> "user"
            }
            messages.add(TogetherMessage(role, msg.content))
        }
        
        // Add current user message
        messages.add(TogetherMessage("user", message))
        
        return messages
    }
    
    /**
     * Build prompt for Gemini AI format
     */
    private fun buildGeminiPrompt(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): String {
        val prompt = StringBuilder()
        
        // Add character context
        if (character != null) {
            prompt.append(buildSystemPrompt(character))
            prompt.append("\n\n")
        }
        
        // Add conversation history
        conversationHistory.takeLast(5).forEach { msg ->
            val speaker = if (msg.role == "user") "User" else character?.name ?: "Assistant"
            prompt.append("$speaker: ${msg.content}\n")
        }
        
        // Add current message
        prompt.append("User: $message\n")
        prompt.append("${character?.name ?: "Assistant"}:")
        
        return prompt.toString()
    }
    
    /**
     * Build system prompt from character
     */
    private fun buildSystemPrompt(character: Character): String {
        val prompt = StringBuilder()
        
        prompt.append("You are ${character.name}")
        
        if (!character.displayName.isNullOrBlank() && character.displayName != character.name) {
            prompt.append(" (${character.displayName})")
        }
        
        prompt.append(".\n\n")
        
        if (!character.persona.isNullOrBlank()) {
            prompt.append("Personality: ${character.persona}\n\n")
        }
        
        if (!character.backstory.isNullOrBlank()) {
            prompt.append("Background: ${character.backstory}\n\n")
        }
        
        if (!character.scenario.isNullOrBlank()) {
            prompt.append("Setting: ${character.scenario}\n\n")
        }
        
        prompt.append("Respond as ${character.name} would, staying true to your personality and background. ")
        prompt.append("Keep responses conversational and engaging.")
        
        return prompt.toString()
    }
} 
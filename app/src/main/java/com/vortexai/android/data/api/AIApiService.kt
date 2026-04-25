package com.vortexai.android.data.api

import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Message
import retrofit2.http.*

/**
 * API service for external AI providers
 * Handles Together AI, Gemini AI, and custom API calls
 */
interface AIApiService {
    
    // Together AI API endpoints
    @POST("v1/chat/completions")
    suspend fun togetherAIChat(
        @Header("Authorization") authorization: String,
        @Body request: TogetherAIRequest
    ): TogetherAIResponse
    
    @POST("v1/images/generations")
    suspend fun togetherAIImage(
        @Header("Authorization") authorization: String,
        @Body request: TogetherImageRequest
    ): TogetherImageResponse
    
    // Gemini AI API endpoints
    @POST("v1beta/models/gemini-pro:generateContent")
    suspend fun geminiChat(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
    
    @POST("v1beta/models/gemini-pro-vision:generateContent")
    suspend fun geminiImage(
        @Query("key") apiKey: String,
        @Body request: GeminiImageRequest
    ): GeminiImageResponse
    
    // Custom API endpoints
    @POST("api/chat/generate")
    suspend fun customChat(
        @Header("Authorization") authorization: String,
        @Body request: CustomChatRequest
    ): CustomChatResponse
    
    @POST("api/image/generate")
    suspend fun customImage(
        @Header("Authorization") authorization: String,
        @Body request: CustomImageRequest
    ): CustomImageResponse
    
    /**
     * Generate response using Together AI
     */
    suspend fun generateTogetherAIResponse(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): AIResponse
    
    /**
     * Generate response using Gemini AI
     */
    suspend fun generateGeminiResponse(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): AIResponse
    
    /**
     * Generate response using custom API
     */
    suspend fun generateCustomAPIResponse(
        message: String,
        character: Character?,
        conversationHistory: List<Message>
    ): AIResponse
    
    /**
     * Generate image using Together AI
     */
    suspend fun generateTogetherImage(prompt: String, style: String): String
    
    /**
     * Generate image using Gemini AI
     */
    suspend fun generateGeminiImage(prompt: String, style: String): String
    
    /**
     * Generate image using custom API
     */
    suspend fun generateCustomImage(prompt: String, style: String): String
}

// Together AI Request/Response Models
data class TogetherAIRequest(
    val model: String = "meta-llama/Llama-3.2-11B-Vision-Instruct-Turbo",
    val messages: List<TogetherMessage>,
    val max_tokens: Int = 512,
    val temperature: Float = 0.7f,
    val top_p: Float = 0.9f,
    val stream: Boolean = false
)

data class TogetherMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class TogetherAIResponse(
    val choices: List<TogetherChoice>,
    val usage: TogetherUsage?
)

data class TogetherChoice(
    val message: TogetherMessage,
    val finish_reason: String?
)

data class TogetherUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class TogetherImageRequest(
    val model: String = "black-forest-labs/FLUX.1-schnell-Free",
    val prompt: String,
    val width: Int = 1024,
    val height: Int = 1024,
    val steps: Int = 4,
    val n: Int = 1
)

data class TogetherImageResponse(
    val data: List<TogetherImageData>
)

data class TogetherImageData(
    val url: String
)

// Gemini AI Request/Response Models
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig?
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxOutputTokens: Int = 512
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String?
)

data class GeminiImageRequest(
    val contents: List<GeminiImageContent>
)

data class GeminiImageContent(
    val parts: List<GeminiImagePart>
)

data class GeminiImagePart(
    val text: String?,
    val inlineData: GeminiInlineData?
)

data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

data class GeminiImageResponse(
    val candidates: List<GeminiCandidate>
)

// Custom API Request/Response Models
data class CustomChatRequest(
    val message: String,
    val character_id: String?,
    val character_name: String?,
    val character_personality: String?,
    val conversation_history: List<CustomMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 512
)

data class CustomMessage(
    val role: String,
    val content: String,
    val timestamp: Long
)

data class CustomChatResponse(
    val success: Boolean,
    val response: String,
    val usage: CustomUsage?
)

data class CustomUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class CustomImageRequest(
    val prompt: String,
    val style: String = "anime",
    val width: Int = 1024,
    val height: Int = 1024,
    val quality: String = "standard"
)

data class CustomImageResponse(
    val success: Boolean,
    val image_url: String,
    val message: String?
)

// Common AI Response Model
data class AIResponse(
    val content: String,
    val usage: AIUsage?
)

data class AIUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
) 
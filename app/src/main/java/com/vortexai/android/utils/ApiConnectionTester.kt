package com.vortexai.android.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive API connection tester for all endpoints
 * Tests connectivity and provides detailed error information
 */
@Singleton
class ApiConnectionTester @Inject constructor() {
    
    companion object {
        private const val TAG = "ApiConnectionTester"
        private const val TEST_TIMEOUT = 10L // seconds
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TEST_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TEST_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TEST_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    /**
     * Test LLM endpoint connection
     */
    suspend fun testLLMConnection(
        provider: String,
        apiKey: String,
        model: String? = null,
        customEndpoint: String? = null
    ): ApiConnectionResult = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                "Together AI" -> testTogetherAI(apiKey, model)
                "OpenRouter", "Open Router" -> testOpenRouter(apiKey, model)
                "Gemini API" -> testGemini(apiKey, model)
                "Hugging Face" -> testHuggingFace(apiKey, model)
                "ModelsLab" -> testModelsLabLLM(apiKey, model)
                "Ollama" -> testOllama(customEndpoint ?: "http://localhost:11434", model)
                "Kobold AI" -> testKobold(customEndpoint ?: "http://localhost:5000", model)
                "Custom API" -> testCustomLLM(customEndpoint ?: "", apiKey, model)
                else -> ApiConnectionResult.failure(
                    ApiError(
                        type = ApiErrorType.CONFIGURATION_ERROR,
                        message = "Unsupported LLM provider: $provider",
                        technicalMessage = "Provider not implemented",
                        isRetryable = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM connection test failed for $provider", e)
            ApiConnectionResult.failure(
                ApiErrorHandler.handleApiError(provider, "LLM", e)
            )
        }
    }
    
    /**
     * Test Image Generation endpoint connection
     */
    suspend fun testImageConnection(
        provider: String,
        apiKey: String,
        model: String? = null,
        customEndpoint: String? = null
    ): ApiConnectionResult = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                "Together AI" -> testTogetherAIImage(apiKey, model)
                "Hugging Face" -> testHuggingFaceImage(apiKey, model)
                "ModelsLab" -> testModelsLabImage(apiKey, model)
                "ComfyUI" -> testComfyUI(customEndpoint ?: "http://localhost:8188")
                "Custom API" -> testCustomImage(customEndpoint ?: "", apiKey, model)
                "Replicate" -> testReplicate(apiKey, model)
                else -> ApiConnectionResult.failure(
                    ApiError(
                        type = ApiErrorType.CONFIGURATION_ERROR,
                        message = "Unsupported image provider: $provider",
                        technicalMessage = "Provider not implemented",
                        isRetryable = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image connection test failed for $provider", e)
            ApiConnectionResult.failure(
                ApiErrorHandler.handleApiError(provider, "Image", e)
            )
        }
    }
    
    /**
     * Test Audio/TTS endpoint connection
     */
    suspend fun testAudioConnection(
        provider: String,
        apiKey: String,
        customEndpoint: String? = null
    ): ApiConnectionResult = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                "Together AI" -> testTogetherAITTS(apiKey)
                "OpenRouter" -> testOpenRouterTTS(apiKey)
                "ModelsLab" -> testModelsLabTTS(apiKey)
                "Google TTS" -> testGoogleTTS()
                "Custom API" -> testCustomAudio(customEndpoint ?: "", apiKey)
                else -> ApiConnectionResult.failure(
                    ApiError(
                        type = ApiErrorType.CONFIGURATION_ERROR,
                        message = "Unsupported audio provider: $provider",
                        technicalMessage = "Provider not implemented",
                        isRetryable = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio connection test failed for $provider", e)
            ApiConnectionResult.failure(
                ApiErrorHandler.handleApiError(provider, "Audio", e)
            )
        }
    }
    
    // LLM Connection Tests
    
    private suspend fun testTogetherAI(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Together AI. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        val request = Request.Builder()
            .url("https://api.together.xyz/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        
        return executeRequest(request, "Together AI", "LLM Models")
    }
    
    private suspend fun testOpenRouter(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for OpenRouter. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        
        return executeRequest(request, "OpenRouter", "LLM Models")
    }
    
    private suspend fun testGemini(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Gemini API. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        // Test with a simple model list request
        val testModel = model ?: "gemini-pro"
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$testModel?key=$apiKey")
            .get()
            .build()
        
        return executeRequest(request, "Gemini API", "Model Info")
    }
    
    private suspend fun testHuggingFace(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Hugging Face. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        // Test with a simple model info request
        val testModel = model ?: "microsoft/DialoGPT-large"
        val request = Request.Builder()
            .url("https://api-inference.huggingface.co/models/$testModel")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        
        return executeRequest(request, "Hugging Face", "Model Info")
    }
    
    private suspend fun testModelsLabLLM(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for ModelsLab. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        val payload = JSONObject().apply {
            put("key", apiKey)
        }
        
        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://modelslab.com/api/v4/dreambooth/model_list")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
        
        return executeRequest(request, "ModelsLab", "LLM Models")
    }
    
    private suspend fun testOllama(endpoint: String, model: String?): ApiConnectionResult {
        val cleanEndpoint = endpoint.trim().removeSuffix("/")
        val request = Request.Builder()
            .url("$cleanEndpoint/api/tags")
            .get()
            .build()
        
        return executeRequest(request, "Ollama", "Local Models", isLocal = true)
    }
    
    private suspend fun testKobold(endpoint: String, model: String?): ApiConnectionResult {
        val cleanEndpoint = endpoint.trim().removeSuffix("/")
        val request = Request.Builder()
            .url("$cleanEndpoint/api/v1/model")
            .get()
            .build()
        
        return executeRequest(request, "Kobold AI", "Model Info", isLocal = true)
    }
    
    private suspend fun testCustomLLM(endpoint: String, apiKey: String, model: String?): ApiConnectionResult {
        if (endpoint.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.CONFIGURATION_ERROR,
                    message = "No endpoint configured for Custom API. Please check your settings.",
                    technicalMessage = "Missing endpoint URL",
                    isRetryable = false
                )
            )
        }
        
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Custom API. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        // Test using the CustomAPIProvider to ensure OpenAI compliance
        return try {
            val customProvider = com.vortexai.android.domain.service.llm.CustomAPIProvider().apply {
                setApiKey(apiKey)
                setEndpoint(endpoint)
            }
            
            val isConnected = customProvider.testConnection()
            if (isConnected) {
                ApiConnectionResult.success("Custom API connection successful")
            } else {
                ApiConnectionResult.failure(
                    ApiError(
                        type = ApiErrorType.NETWORK_ERROR,
                        message = "Failed to connect to Custom API. Please check your endpoint and API key.",
                        technicalMessage = "Connection test failed",
                        isRetryable = true
                    )
                )
            }
        } catch (e: Exception) {
            ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.NETWORK_ERROR,
                    message = "Error testing Custom API connection: ${e.message}",
                    technicalMessage = e.toString(),
                    isRetryable = true
                )
            )
        }
    }
    
    // Image Generation Connection Tests
    
    private suspend fun testTogetherAIImage(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Together AI Image. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        val request = Request.Builder()
            .url("https://api.together.xyz/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        
        return executeRequest(request, "Together AI", "Image Models")
    }
    
    private suspend fun testHuggingFaceImage(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Hugging Face Image. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        val testModel = model ?: "runwayml/stable-diffusion-v1-5"
        val request = Request.Builder()
            .url("https://api-inference.huggingface.co/models/$testModel")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        
        return executeRequest(request, "Hugging Face", "Image Model Info")
    }
    
    private suspend fun testModelsLabImage(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for ModelsLab Image. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        val payload = JSONObject().apply {
            put("key", apiKey)
        }
        
        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://modelslab.com/api/v6/realtime/text2img")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
        
        return executeRequest(request, "ModelsLab", "Image Generation", expectError = true)
    }
    
    private suspend fun testComfyUI(endpoint: String): ApiConnectionResult {
        if (endpoint.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.CONFIGURATION_ERROR,
                    message = "No endpoint configured for ComfyUI. Please check your settings.",
                    technicalMessage = "Missing endpoint URL",
                    isRetryable = false
                )
            )
        }
        
        val cleanEndpoint = endpoint.trim().removeSuffix("/")
        val request = Request.Builder()
            .url("$cleanEndpoint/object_info")
            .get()
            .build()
        
        return executeRequest(request, "ComfyUI", "Object Info", isLocal = true)
    }
    
    private suspend fun testCustomImage(endpoint: String, apiKey: String, model: String?): ApiConnectionResult {
        if (endpoint.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.CONFIGURATION_ERROR,
                    message = "No endpoint configured for Custom Image API. Please check your settings.",
                    technicalMessage = "Missing endpoint URL",
                    isRetryable = false
                )
            )
        }
        
        val request = Request.Builder()
            .url("$endpoint/models")
            .apply {
                if (apiKey.isNotBlank()) {
                    addHeader("Authorization", "Bearer $apiKey")
                }
            }
            .get()
            .build()
        
        return executeRequest(request, "Custom Image API", "Models")
    }
    
    private suspend fun testReplicate(apiKey: String, model: String?): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Replicate. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        // Test with a simple model list request
        val request = Request.Builder()
            .url("https://api.replicate.com/v1/models")
            .addHeader("Authorization", "Token $apiKey")
            .get()
            .build()
        
        return executeRequest(request, "Replicate", "Models")
    }
    
    // Audio Connection Tests
    
    private suspend fun testTogetherAITTS(apiKey: String): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for Together AI TTS. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        // Test with a simple TTS request to Together AI audio endpoint
        val payload = JSONObject().apply {
            put("model", "cartesia/sonic-2")
            put("input", "This is a test of Together AI text-to-speech.")
            put("voice", "laidback woman")
        }
        
        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.together.xyz/v1/audio/speech")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()
        
        return executeRequest(request, "Together AI", "TTS", expectError = true)
    }
    
    private suspend fun testOpenRouterTTS(apiKey: String): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for OpenRouter TTS. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        // OpenRouter TTS typically uses chat completions with TTS-capable models
        // Test with the models endpoint first as TTS integration varies
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://vortex-ai.app")
            .addHeader("X-Title", "Vortex AI")
            .get()
            .build()
        
        return executeRequest(request, "OpenRouter", "TTS Models")
    }
    
    private suspend fun testModelsLabTTS(apiKey: String): ApiConnectionResult {
        if (apiKey.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.AUTH_ERROR,
                    message = "No API key configured for ModelsLab TTS. Please check your settings.",
                    technicalMessage = "Missing API key",
                    isRetryable = false
                )
            )
        }
        
        // Test with a simple TTS request
        val payload = JSONObject().apply {
            put("key", apiKey)
            put("text", "test")
            put("voice", "en-US-AriaNeural")
        }
        
        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://modelslab.com/api/v6/realtime/text2audio")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
        
        return executeRequest(request, "ModelsLab", "TTS", expectError = true)
    }
    
    private suspend fun testGoogleTTS(): ApiConnectionResult {
        // Google TTS is handled by Android system, always available
        return ApiConnectionResult.success("Google TTS is available through Android system")
    }
    
    private suspend fun testCustomAudio(endpoint: String, apiKey: String): ApiConnectionResult {
        if (endpoint.isBlank()) {
            return ApiConnectionResult.failure(
                ApiError(
                    type = ApiErrorType.CONFIGURATION_ERROR,
                    message = "No endpoint configured for Custom Audio API. Please check your settings.",
                    technicalMessage = "Missing endpoint URL",
                    isRetryable = false
                )
            )
        }
        
        val request = Request.Builder()
            .url("$endpoint/health")
            .apply {
                if (apiKey.isNotBlank()) {
                    addHeader("Authorization", "Bearer $apiKey")
                }
            }
            .get()
            .build()
        
        return executeRequest(request, "Custom Audio API", "Health Check")
    }
    
    // Helper Methods
    
    private suspend fun executeRequest(
        request: Request,
        provider: String,
        endpoint: String,
        isLocal: Boolean = false,
        expectError: Boolean = false
    ): ApiConnectionResult {
        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            when {
                response.isSuccessful -> {
                    Log.d(TAG, "$provider $endpoint connection successful")
                    ApiConnectionResult.success("$provider connection successful")
                }
                expectError && response.code in 400..499 -> {
                    // For some endpoints, we expect errors but connection is working
                    Log.d(TAG, "$provider $endpoint connection working (expected error: ${response.code})")
                    ApiConnectionResult.success("$provider connection working")
                }
                else -> {
                    Log.w(TAG, "$provider $endpoint connection failed: ${response.code}")
                    val error = ApiErrorHandler.handleApiError(
                        provider = provider,
                        endpoint = endpoint,
                        error = Exception("HTTP ${response.code}"),
                        responseCode = response.code,
                        responseBody = responseBody
                    )
                    ApiConnectionResult.failure(error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "$provider $endpoint connection test failed", e)
            val error = if (isLocal && e is java.net.ConnectException) {
                ApiError(
                    type = ApiErrorType.NETWORK_ERROR,
                    message = "$provider is not running or not accessible at the configured endpoint.",
                    technicalMessage = "Local service connection failed: ${e.message}",
                    isRetryable = true
                )
            } else {
                ApiErrorHandler.handleApiError(provider, endpoint, e)
            }
            ApiConnectionResult.failure(error)
        }
    }
}

/**
 * Result of API connection test
 */
sealed class ApiConnectionResult {
    data class Success(val message: String) : ApiConnectionResult()
    data class Failure(val error: ApiError) : ApiConnectionResult()
    
    companion object {
        fun success(message: String) = Success(message)
        fun failure(error: ApiError) = Failure(error)
    }
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    fun getErrorMessage(): String? = (this as? Failure)?.error?.message
    fun getTechnicalMessage(): String? = (this as? Failure)?.error?.technicalMessage
}
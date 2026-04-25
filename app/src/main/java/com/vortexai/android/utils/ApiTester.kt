package com.vortexai.android.utils

import android.content.Context
import android.util.Log
import com.vortexai.android.domain.service.ModelsLabImageApi
import com.vortexai.android.domain.service.ModelsLabTTSApi
import com.vortexai.android.domain.service.ImageGenerationRequest
import com.vortexai.android.domain.service.TTSRequest
import com.vortexai.android.domain.service.llm.ModelsLabProvider
import com.vortexai.android.domain.service.llm.TogetherProvider
import com.vortexai.android.domain.service.llm.OpenAIProvider
import com.vortexai.android.domain.service.llm.GenerationParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API testing utility for validating endpoint functionality
 * Helps diagnose and fix API integration issues
 */
@Singleton
class ApiTester @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val modelsLabImageApi: ModelsLabImageApi,
    private val modelsLabTTSApi: ModelsLabTTSApi
) {
    
    companion object {
        private const val TAG = "ApiTester"
    }
    
    /**
     * Test all configured API endpoints
     */
    suspend fun testAllApis(): ApiTestResults = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, ApiTestResult>()
        
        // Test LLM APIs
        results["Together AI"] = testTogetherAI()
        results["OpenAI"] = testOpenAI()
        results["ModelsLab LLM"] = testModelsLabLLM()
        
        // Test Image APIs
        results["ModelsLab Image"] = testModelsLabImage()
        
        // Test Audio APIs
        results["ModelsLab TTS"] = testModelsLabTTS()
        
        ApiTestResults(results)
    }
    
    /**
     * Test Together AI LLM endpoint
     */
    private suspend fun testTogetherAI(): ApiTestResult {
        return try {
            val apiKey = apiKeyManager.getApiKey("together")
            if (apiKey.isNullOrBlank()) {
                return ApiTestResult(
                    success = false,
                    message = "API key not configured",
                    details = "Please set your Together AI API key in settings"
                )
            }
            
            val provider = TogetherProvider().apply { setApiKey(apiKey) }
            val response = provider.generateResponse(
                "Hello, this is a test message. Please respond briefly.",
                GenerationParams(maxTokens = 50)
            )
            
            if (response.isNotBlank()) {
                ApiTestResult(
                    success = true,
                    message = "Together AI working correctly",
                    details = "Response: ${response.take(100)}..."
                )
            } else {
                ApiTestResult(
                    success = false,
                    message = "Empty response from Together AI",
                    details = "API returned empty or null response"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Together AI test failed", e)
            ApiTestResult(
                success = false,
                message = "Together AI test failed",
                details = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Test OpenAI LLM endpoint
     */
    private suspend fun testOpenAI(): ApiTestResult {
        return try {
            val apiKey = apiKeyManager.getApiKey("openai")
            if (apiKey.isNullOrBlank()) {
                return ApiTestResult(
                    success = false,
                    message = "API key not configured",
                    details = "Please set your OpenAI API key in settings"
                )
            }
            
            val provider = OpenAIProvider().apply { setApiKey(apiKey) }
            val response = provider.generateResponse(
                "Hello, this is a test message. Please respond briefly.",
                GenerationParams(maxTokens = 50)
            )
            
            if (response.isNotBlank()) {
                ApiTestResult(
                    success = true,
                    message = "OpenAI working correctly",
                    details = "Response: ${response.take(100)}..."
                )
            } else {
                ApiTestResult(
                    success = false,
                    message = "Empty response from OpenAI",
                    details = "API returned empty or null response"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI test failed", e)
            ApiTestResult(
                success = false,
                message = "OpenAI test failed",
                details = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Test ModelsLab LLM endpoint
     */
    private suspend fun testModelsLabLLM(): ApiTestResult {
        return try {
            val apiKey = apiKeyManager.getApiKey("modelslab")
            if (apiKey.isNullOrBlank()) {
                return ApiTestResult(
                    success = false,
                    message = "API key not configured",
                    details = "Please set your ModelsLab API key in settings"
                )
            }
            
            val provider = ModelsLabProvider().apply { setApiKey(apiKey) }
            val response = provider.generateResponse(
                "Hello, this is a test message. Please respond briefly.",
                GenerationParams(maxTokens = 50)
            )
            
            if (response.isNotBlank()) {
                ApiTestResult(
                    success = true,
                    message = "ModelsLab LLM working correctly",
                    details = "Response: ${response.take(100)}..."
                )
            } else {
                ApiTestResult(
                    success = false,
                    message = "Empty response from ModelsLab LLM",
                    details = "API returned empty or null response"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "ModelsLab LLM test failed", e)
            ApiTestResult(
                success = false,
                message = "ModelsLab LLM test failed",
                details = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Test ModelsLab Image generation endpoint
     */
    private suspend fun testModelsLabImage(): ApiTestResult {
        return try {
            val apiKey = apiKeyManager.getApiKey("modelslab")
            if (apiKey.isNullOrBlank()) {
                return ApiTestResult(
                    success = false,
                    message = "API key not configured",
                    details = "Please set your ModelsLab API key in settings"
                )
            }
            
            val request = ImageGenerationRequest(
                prompt = "a simple test image of a cat",
                model = "realistic-vision-v6.0",
                width = 512,
                height = 512,
                steps = 20
            )
            
            val result = modelsLabImageApi.text2Img(apiKey, request)
            
            result.fold(
                onSuccess = { imageUrl ->
                    ApiTestResult(
                        success = true,
                        message = "ModelsLab Image working correctly",
                        details = "Generated image URL: $imageUrl"
                    )
                },
                onFailure = { error ->
                    ApiTestResult(
                        success = false,
                        message = "ModelsLab Image test failed",
                        details = error.message ?: "Unknown error"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "ModelsLab Image test failed", e)
            ApiTestResult(
                success = false,
                message = "ModelsLab Image test failed",
                details = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Test ModelsLab TTS endpoint
     */
    private suspend fun testModelsLabTTS(): ApiTestResult {
        return try {
            val apiKey = apiKeyManager.getApiKey("modelslab")
            if (apiKey.isNullOrBlank()) {
                return ApiTestResult(
                    success = false,
                    message = "API key not configured",
                    details = "Please set your ModelsLab API key in settings"
                )
            }
            
            val request = TTSRequest(
                text = "Hello, this is a test of the text to speech functionality.",
                modelId = "inworld-tts-1",
                voiceId = "Olivia",
                language = "english"
            )
            
            val result = modelsLabTTSApi.textToAudio(apiKey, request)
            
            result.fold(
                onSuccess = { audioUrl ->
                    ApiTestResult(
                        success = true,
                        message = "ModelsLab TTS working correctly",
                        details = "Generated audio URL: $audioUrl"
                    )
                },
                onFailure = { error ->
                    ApiTestResult(
                        success = false,
                        message = "ModelsLab TTS test failed",
                        details = error.message ?: "Unknown error"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "ModelsLab TTS test failed", e)
            ApiTestResult(
                success = false,
                message = "ModelsLab TTS test failed",
                details = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Test network connectivity to various endpoints
     */
    suspend fun testConnectivity(): ConnectivityTestResults = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Boolean>()
        
        val endpoints = mapOf(
            "Together AI" to "https://api.together.xyz",
            "OpenAI" to "https://api.openai.com",
            "ModelsLab" to "https://modelslab.com",
            "Google (Gemini)" to "https://generativelanguage.googleapis.com"
        )
        
        endpoints.forEach { (name, url) ->
            results[name] = EndpointChecker.isReachable(url)
        }
        
        ConnectivityTestResults(results)
    }
}

/**
 * Results of API testing
 */
data class ApiTestResults(
    val results: Map<String, ApiTestResult>
) {
    val allPassed: Boolean get() = results.values.all { it.success }
    val passedCount: Int get() = results.values.count { it.success }
    val totalCount: Int get() = results.size
}

/**
 * Result of individual API test
 */
data class ApiTestResult(
    val success: Boolean,
    val message: String,
    val details: String? = null
)

/**
 * Results of connectivity testing
 */
data class ConnectivityTestResults(
    val results: Map<String, Boolean>
) {
    val allReachable: Boolean get() = results.values.all { it }
    val reachableCount: Int get() = results.values.count { it }
    val totalCount: Int get() = results.size
}
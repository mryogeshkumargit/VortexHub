package com.vortexai.android.domain.service.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterProvider @Inject constructor() : LLMProvider {
    
    private var apiKey: String? = null
    private var selectedModel: String? = null
    private val baseUrl = "https://openrouter.ai/api/v1/"
    private val defaultModel = "nousresearch/hermes-3-llama-3.1-405b" // Best for roleplay per documentation
    
    // Recommended models for uncensored roleplay from documentation
    private val fallbackModels = listOf(
        "nousresearch/hermes-3-llama-3.1-405b",
        "wizardlm/wizardlm-2-8x22b",
        "meta-llama/llama-3-70b-instruct",
        "mistral/unslopnemo-12b",
        "austism/chronos-hermes-13b",
        "openai/gpt-3.5-turbo" // Fallback for compatibility
    )
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey.trim()
        Timber.i("OpenRouter API key set: ${if (apiKey.isBlank()) "EMPTY" else "${apiKey.take(8)}..."}")
    }
    
    fun setModel(model: String) {
        this.selectedModel = model
        Timber.i("OpenRouter model set to: $model")
    }
    
    override fun isReady(): Boolean {
        return !apiKey.isNullOrBlank()
    }
    
    override fun getModelName(): String {
        return defaultModel
    }
    
    override fun getMaxTokens(): Int? {
        return 4096 // Default max tokens
    }
    
    override suspend fun generateResponse(
        prompt: String,
        params: GenerationParams
    ): String = withContext(Dispatchers.IO) {
        
        android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: generateResponse() called")
        android.util.Log.d("OpenRouterProvider", "🔥 API Key: ${if (apiKey.isNullOrBlank()) "NULL/BLANK" else "SET (${apiKey!!.length} chars)"}") 
        android.util.Log.d("OpenRouterProvider", "🔥 Selected Model: $selectedModel")
        android.util.Log.d("OpenRouterProvider", "🔥 Prompt length: ${prompt.length}")
        
        if (!isReady()) {
            val errorMsg = "OpenRouter provider not ready - API key: ${if (apiKey.isNullOrBlank()) "NOT SET" else "SET (${apiKey!!.length} chars)"}"
            android.util.Log.e("OpenRouterProvider", "🔥 DEEP DEBUG: Provider not ready!")
            Timber.e(errorMsg)
            throw IllegalStateException(errorMsg)
        }
        
        val modelToUse = selectedModel?.takeIf { it.isNotBlank() } ?: defaultModel
        Timber.d("OpenRouter: Starting request with model: $modelToUse")
        Timber.d("OpenRouter: API key length: ${apiKey?.length ?: 0}")
        Timber.d("OpenRouter: Base URL: $baseUrl")
        
        try {
            android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: Building request JSON")
            
            // Build request JSON manually
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            
            android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: Messages array created")
            
            // Try the selected model first, then fallback models
            val modelsToTry = mutableListOf<String>()
            val primaryModel = selectedModel?.takeIf { it.isNotBlank() } ?: defaultModel
            modelsToTry.add(primaryModel)
            
            // Only add fallbacks if the primary model is not already in the list
            fallbackModels.forEach { fallback ->
                if (fallback != primaryModel) {
                    modelsToTry.add(fallback)
                }
            }
            
            var lastError: Exception? = null
            
            android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: Models to try: ${modelsToTry.joinToString(", ")}")
            
            for (modelToTry in modelsToTry) {
                try {
                    android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: Trying model: $modelToTry")
                    Timber.d("OpenRouter: Trying model: $modelToTry")
                    
                    val requestJson = JSONObject().apply {
                        put("model", modelToTry)
                        put("messages", messagesArray)
                        put("temperature", params.temperature)
                        if (params.topP > 0) put("top_p", params.topP)
                        if (params.maxTokens > 0) put("max_tokens", params.maxTokens)
                        if (params.stop.isNotEmpty()) {
                            val stopArray = JSONArray()
                            params.stop.forEach { stopArray.put(it) }
                            put("stop", stopArray)
                        }
                        if (params.frequencyPenalty != 0f) put("frequency_penalty", params.frequencyPenalty)
                        if (params.presencePenalty != 0f) put("presence_penalty", params.presencePenalty)
                        put("stream", false)
                    }
                    
                    val requestBody = requestJson.toString()
                        .toRequestBody("application/json".toMediaType())
                    
                    val fullUrl = "${baseUrl}chat/completions"
                    Timber.d("OpenRouter: Making request to: $fullUrl")
                    Timber.d("OpenRouter: Request body: ${requestJson.toString()}")
                    
                    android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: Building HTTP request")
                    android.util.Log.d("OpenRouterProvider", "🔥 URL: $fullUrl")
                    android.util.Log.d("OpenRouterProvider", "🔥 Auth header: Bearer ${apiKey?.take(8)}...")
                    
                    val httpRequest = Request.Builder()
                        .url(fullUrl)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("HTTP-Referer", "https://vortexai.app") // Required by OpenRouter
                        .addHeader("X-Title", "VortexAI Android") // Optional but recommended
                        .post(requestBody)
                        .build()
                    
                    android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: Making HTTP call...")
                    
                    val response = httpClient.newCall(httpRequest).execute()
                    android.util.Log.d("OpenRouterProvider", "🔥 DEEP DEBUG: HTTP response received")
                    android.util.Log.d("OpenRouterProvider", "🔥 Response code: ${response.code}")
                    android.util.Log.d("OpenRouterProvider", "🔥 Response message: ${response.message}")
                    Timber.d("OpenRouter: Response code: ${response.code} for model: $modelToTry")
                    
                    if (response.isSuccessful) {
                        // Success! Parse and return the response
                        val responseBody = response.body?.string()
                            ?: throw Exception("Empty response from OpenRouter")
                        
                        // Parse response JSON manually
                        val responseJson = JSONObject(responseBody)
                        val choicesArray = responseJson.getJSONArray("choices")
                        
                        if (choicesArray.length() == 0) {
                            throw Exception("No choices in OpenRouter response")
                        }
                        
                        val firstChoice = choicesArray.getJSONObject(0)
                        val message = firstChoice.getJSONObject("message")
                        val generatedText = message.getString("content")
                        
                        Timber.d("OpenRouter response generated with model $modelToTry: ${generatedText.length} characters")
                        return@withContext generatedText
                    } else {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        val error = Exception("OpenRouter API error: ${response.code} - $errorBody")
                        Timber.w("OpenRouter: Model $modelToTry failed: ${error.message}")
                        lastError = error
                        
                        // If it's a 404 (model not available) or 400 (bad model format), try next model
                        if (response.code == 404 || response.code == 400) {
                            Timber.w("OpenRouter: Model $modelToTry not available (${response.code}), trying next model")
                            continue
                        } else {
                            // For other errors (auth, rate limit, etc.), don't try other models
                            throw error
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OpenRouterProvider", "🔥 DEEP DEBUG: Exception for model $modelToTry")
                    android.util.Log.e("OpenRouterProvider", "🔥 Exception type: ${e.javaClass.simpleName}")
                    android.util.Log.e("OpenRouterProvider", "🔥 Exception message: ${e.message}")
                    android.util.Log.e("OpenRouterProvider", "🔥 Exception cause: ${e.cause?.message}")
                    Timber.w("OpenRouter: Model $modelToTry failed: ${e.message}")
                    lastError = e
                    continue
                }
            }
            
            // If we get here, all models failed
            throw lastError ?: Exception("All OpenRouter models failed")

            
        } catch (e: Exception) {
            android.util.Log.e("OpenRouterProvider", "🔥 DEEP DEBUG: FINAL CATCH BLOCK")
            android.util.Log.e("OpenRouterProvider", "🔥 Final exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("OpenRouterProvider", "🔥 Final exception message: ${e.message}")
            android.util.Log.e("OpenRouterProvider", "🔥 Final exception cause: ${e.cause?.message}")
            e.stackTrace.take(5).forEach {
                android.util.Log.e("OpenRouterProvider", "🔥   $it")
            }
            
            Timber.e(e, "Error calling OpenRouter API: ${e.message}")
            when {
                e.message?.contains("401") == true -> throw Exception("❌ Invalid OpenRouter API key. Please check your API key in Settings.")
                e.message?.contains("403") == true -> throw Exception("❌ OpenRouter access denied. Check your API key permissions.")
                e.message?.contains("429") == true -> throw Exception("❌ OpenRouter rate limit exceeded. Please wait and try again.")
                e.message?.contains("timeout") == true -> throw Exception("❌ OpenRouter connection timeout. Please try again.")
                e.message?.contains("Connection") == true -> throw Exception("❌ Network error connecting to OpenRouter. Check your internet connection.")
                else -> throw Exception("❌ OpenRouter error: ${e.message}")
            }
        }
    }
}
package com.vortexai.android.domain.service.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomImageProvider @Inject constructor() {
    
    private var apiKey: String? = null
    private var customEndpoint: String? = null
    private var apiPrefix: String = "/v1"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS) // 10 minutes for image generation
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
        Timber.i("Custom Image API key set")
    }
    
    fun setEndpoint(endpoint: String) {
        this.customEndpoint = endpoint.trim().removeSuffix("/")
        Timber.i("Custom Image API endpoint set: $customEndpoint")
    }
    
    fun setApiPrefix(prefix: String) {
        this.apiPrefix = prefix.trim().let { 
            if (!it.startsWith("/")) "/$it" else it 
        }
        Timber.i("Custom Image API prefix set: $apiPrefix")
    }
    
    /**
     * Get the full endpoint URL with configurable prefix
     */
    private fun getFullEndpoint(path: String): String {
        val baseEndpoint = customEndpoint ?: ""
        return if (baseEndpoint.contains(apiPrefix)) {
            // If endpoint already contains the prefix, use as is
            "$baseEndpoint$path"
        } else {
            // Add the configured prefix
            "$baseEndpoint$apiPrefix$path"
        }
    }
    
    fun isReady(): Boolean {
        return !apiKey.isNullOrBlank() && !customEndpoint.isNullOrBlank()
    }
    
    /**
     * Auto-detect the appropriate auth header format based on API key structure.
     * - fal.ai format (uuid:secret): Uses "Key {apiKey}"
     * - Standard format: Uses "Bearer {apiKey}"
     */
    private fun getAuthHeader(key: String): String {
        // fal.ai uses format: uuid:secret (e.g., "1286244d-b070-4ef8-8d3c-3e81c397880b:2ea952307c020ff8bbb73d82f38a22c2")
        // Use case-insensitive regex for hex characters (A-F or a-f)
        val isFalAiFormat = key.matches(Regex("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}:[a-fA-F0-9]{32}$"))
        
        // Also check for simpler colon-separated format that looks like key:secret
        val hasColonFormat = key.contains(":") && !key.startsWith("sk-") && !key.startsWith("Bearer ")
        
        Timber.d("API Key analysis: length=${key.length}, isFalAiFormat=$isFalAiFormat, hasColonFormat=$hasColonFormat")
        
        return if (isFalAiFormat || hasColonFormat) {
            Timber.d("Using 'Key' auth header for fal.ai-style API key")
            "Key $key"
        } else {
            Timber.d("Using standard Bearer auth header")
            "Bearer $key"
        }
    }
    
    /**
     * Generate image - supports both OpenAI-compatible and fal.ai formats
     */
    suspend fun generateImage(
        prompt: String,
        model: String = "dall-e-3",
        size: String = "1024x1024",
        quality: String = "standard",
        style: String = "vivid",
        n: Int = 1
    ): Result<CustomImageResult> = withContext(Dispatchers.IO) {
        
        if (!isReady()) {
            return@withContext Result.failure(IllegalStateException("Custom Image API provider not ready - API key or endpoint not set"))
        }
        
        try {
            val endpoint = customEndpoint!!
            val key = apiKey!!
            
            // Detect if this is a fal.ai endpoint
            val isFalAi = endpoint.contains("fal.run") || endpoint.contains("fal.ai") || 
                          key.contains(":") // fal.ai keys have format uuid:secret
            
            Timber.d("Custom Image API - endpoint: $endpoint, model: $model, isFalAi: $isFalAi")
            
            val (requestUrl, requestJson) = if (isFalAi) {
                // fal.ai format - combine base URL with model to form complete endpoint
                val sizeParts = size.split("x")
                val width = sizeParts.getOrNull(0)?.toIntOrNull() ?: 1024
                val height = sizeParts.getOrNull(1)?.toIntOrNull() ?: 1024
                
                // Build full fal.ai URL properly
                // If endpoint already contains the model, use it as-is
                // Otherwise, append the model to the base URL
                val fullUrl = if (endpoint.contains(model)) {
                    endpoint
                } else {
                    val baseUrl = endpoint.trimEnd('/')
                    val modelPath = model.trimStart('/')
                    "$baseUrl/$modelPath"
                }
                
                Timber.d("fal.ai endpoint: $endpoint, model: $model, fullUrl: $fullUrl")
                
                val json = JSONObject().apply {
                    put("prompt", prompt)
                    put("image_size", JSONObject().apply {
                        put("width", width)
                        put("height", height)
                    })
                    put("num_images", n)
                    put("enable_safety_checker", false)
                }
                
                Timber.d("fal.ai request body: $json")
                Pair(fullUrl, json) // Combined URL
            } else {
                // OpenAI-compatible format
                val json = JSONObject().apply {
                    put("model", model)
                    put("prompt", prompt)
                    put("n", n)
                    put("size", size)
                    put("quality", quality)
                    put("style", style)
                    put("response_format", "url")
                }
                
                Pair(getFullEndpoint("/images/generations"), json)
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            Timber.d("Custom Image API URL: $requestUrl")
            
            val httpRequest = Request.Builder()
                .url(requestUrl)
                .apply {
                    val authHeader = getAuthHeader(key)
                    Timber.d("Using auth header: ${authHeader.take(20)}...")
                    addHeader("Authorization", authHeader)
                }
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(httpRequest).execute()
            val generationTime = System.currentTimeMillis() - startTime
            
            val responseBody = response.body?.string()
            Timber.d("Response code: ${response.code}, body: ${responseBody?.take(500)}")
            
            if (!response.isSuccessful) {
                val errorBody = responseBody ?: "Unknown error"
                Timber.e("Custom Image API error: ${response.code} - $errorBody")
                return@withContext Result.failure(Exception("Custom Image API error: ${response.code} - $errorBody"))
            }
            
            if (responseBody.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Empty response from Custom Image API"))
            }
            
            // Check if response is HTML (error page) instead of JSON
            if (responseBody.trim().startsWith("<!doctype", ignoreCase = true) || 
                responseBody.trim().startsWith("<html", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Custom Image API returned HTML error page instead of JSON. Please check your endpoint URL and API configuration."))
            }
            
            // Parse response JSON
            val responseJson = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Invalid JSON response: ${e.message}"))
            }
            
            // Parse images - support both OpenAI and fal.ai response formats
            val images = mutableListOf<CustomImageData>()
            
            // fal.ai format: { "images": [{"url": "...", "content_type": "..."}] }
            if (responseJson.has("images")) {
                val imagesArray = responseJson.getJSONArray("images")
                for (i in 0 until imagesArray.length()) {
                    val imageObj = imagesArray.getJSONObject(i)
                    val url = imageObj.optString("url")
                    if (url.isNotBlank()) {
                        images.add(CustomImageData(url = url, b64Json = null))
                    }
                }
            }
            // OpenAI format: { "data": [{"url": "..."} or {"b64_json": "..."}] }
            else if (responseJson.has("data")) {
                val dataArray = responseJson.getJSONArray("data")
                for (i in 0 until dataArray.length()) {
                    val imageData = dataArray.getJSONObject(i)
                    val url = imageData.optString("url")
                    val b64Json = imageData.optString("b64_json")
                    if (url.isNotBlank() || b64Json.isNotBlank()) {
                        images.add(CustomImageData(
                            url = url.takeIf { it.isNotBlank() },
                            b64Json = b64Json.takeIf { it.isNotBlank() }
                        ))
                    }
                }
            }
            if (images.isEmpty()) {
                Timber.e("No images found in response: $responseBody")
                return@withContext Result.failure(Exception("No images in Custom Image API response"))
            }
            
            Timber.d("Custom Image API generated ${images.size} images in ${generationTime}ms")
            Result.success(CustomImageResult(images = images, generationTime = generationTime))
            
        } catch (e: Exception) {
            Timber.e(e, "Error calling Custom Image API")
            Result.failure(e)
        }
    }
    
    /**
     * Test connection to Custom Image API
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext false
            }
            
            val requestJson = JSONObject().apply {
                put("model", "dall-e-3")
                put("prompt", "test")
                put("n", 1)
                put("size", "256x256")
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url(getFullEndpoint("/images/generations"))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            response.isSuccessful
            
        } catch (e: Exception) {
            Timber.e(e, "Error testing Custom Image API connection")
            false
        }
    }
    
    /**
     * Fetch available models from Custom Image API
     */
    suspend fun fetchModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!isReady()) {
                return@withContext Result.failure(Exception("Custom Image API not ready"))
            }
            
            val httpRequest = Request.Builder()
                .url(getFullEndpoint("/models"))
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch models: ${response.code}"))
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            
            val responseJson = JSONObject(responseBody)
            val modelsArray = responseJson.optJSONArray("data")
            val models = mutableListOf<String>()
            
            if (modelsArray != null) {
                for (i in 0 until modelsArray.length()) {
                    val model = modelsArray.optJSONObject(i)
                    if (model != null) {
                        val modelId = model.optString("id")
                        if (modelId.isNotBlank()) {
                            models.add(modelId)
                        }
                    }
                }
            }
            
            Result.success(models)
            
        } catch (e: Exception) {
            Timber.e(e, "Error fetching Custom Image API models")
            Result.failure(e)
        }
    }
}

data class CustomImageResult(
    val images: List<CustomImageData>,
    val generationTime: Long
)

data class CustomImageData(
    val url: String? = null,
    val b64Json: String? = null
)

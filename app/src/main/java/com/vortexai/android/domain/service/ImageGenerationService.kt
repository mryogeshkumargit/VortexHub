package com.vortexai.android.domain.service

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vortexai.android.data.models.ModelsLabModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.vortexai.android.domain.service.kobold.KoboldApi
import com.vortexai.android.domain.service.llm.LLMProvider
import com.vortexai.android.domain.service.modelslab.DefaultModels
import com.vortexai.android.domain.service.ollama.OllamaApi
import com.vortexai.android.domain.service.openrouter.OpenRouterApi
import com.vortexai.android.domain.service.together.TogetherApi
import com.vortexai.android.domain.service.TrackedImageGenerationResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.logging.Level
import java.util.logging.Logger

data class ImageGenerationRequest(
    val prompt: String,
    val model: String = "stabilityai/stable-diffusion-xl-base-1.0",
    val width: Int = 1024,
    val height: Int = 1024,
    val steps: Int = 20,
    val guidanceScale: Float = 7.5f,
    val seed: Int? = null,
    val workflow: String? = null,
    val negativePrompt: String? = null,
    val loraModel: String? = null,
    val loraStrength: Float? = null,
    val loraModels: List<String>? = null,
    val loraStrengths: List<Float>? = null,
    val initImageBase64: String? = null,
    val strength: Float? = null,
    val scheduler: String? = null,
    val samples: Int? = null
)

data class ImageGenerationResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val error: String? = null,
    val generationTime: Long = 0,
    val model: String? = null
)

@Singleton
class ImageGenerationService @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val modelsLabApi: ModelsLabImageApi,
    private val apiConnectionTester: com.vortexai.android.utils.ApiConnectionTester,
    private val imageGenerationTracker: ImageGenerationTracker,
    private val imageEditingService: ImageEditingService,
    private val customApiExecutor: CustomApiExecutor,
    private val customApiProviderRepository: com.vortexai.android.data.repository.CustomApiProviderRepository,
    private val logger: GenerationLogger
) {
    companion object {
        private val TAG = ImageGenerationService::class.java.simpleName
        private val logger = Logger.getLogger(TAG)
        private const val TOGETHER_AI_BASE_URL = "https://api.together.xyz/v1/images/generations"
        private const val TOGETHER_AI_MODELS_URL = "https://api.together.xyz/v1/models"
        private const val HUGGINGFACE_BASE_URL = "https://api-inference.huggingface.co/models"
        private const val MODELSLAB_BASE_URL = "https://modelslab.com/api/v6"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    // Use insecure connections for ModelsLab to bypass SSL issues
    
    /**
     * Test connection to Image Generation provider
     */
    suspend fun testConnection(provider: String, apiKey: String, model: String? = null, customEndpoint: String? = null): com.vortexai.android.utils.ApiConnectionResult {
        return apiConnectionTester.testImageConnection(provider, apiKey, model, customEndpoint)
    }
    
    suspend fun generateImage(
        provider: String,
        apiKey: String,
        request: ImageGenerationRequest,
        customEndpoint: String? = null,
        generationId: String? = null,
        sourceLocation: ImageGenerationSource? = null,
        customProviderId: String? = null
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        val finalGenerationId = generationId ?: "gen_${System.currentTimeMillis()}_${(1000..9999).random()}"
        
        // Register the generation if source location is provided
        sourceLocation?.let { location ->
            imageGenerationTracker.registerGeneration(
                id = finalGenerationId,
                prompt = request.prompt,
                sourceLocation = location,
                provider = provider,
                model = request.model
            )
        }
        
        return@withContext try {
            Log.d(TAG, "=== generateImage called ===")
            Log.d(TAG, "Provider: $provider")
            Log.d(TAG, "CustomProviderId: $customProviderId")
            Log.d(TAG, "CustomEndpoint: $customEndpoint")
            Log.d(TAG, "Prompt: ${request.prompt}")
            
            val result = when (provider) {
                "Together AI" -> generateWithTogetherAI(apiKey, request)
                "Hugging Face" -> generateWithHuggingFace(apiKey, request)
                "ComfyUI" -> customEndpoint?.let { generateWithComfyUI(it, apiKey, request) } 
                    ?: Result.failure(Exception("ComfyUI endpoint is required"))
                "Custom API" -> {
                    Log.d(TAG, "Custom API branch - customProviderId.isNullOrBlank(): ${customProviderId.isNullOrBlank()}")
                    // Use schema-driven approach if customProviderId is provided
                    if (!customProviderId.isNullOrBlank()) {
                        Log.d(TAG, "Using schema-driven Custom API with provider ID: $customProviderId")
                        generateWithSchemaBasedCustomAPI(customProviderId, request)
                    } else {
                        Log.d(TAG, "Using legacy Custom API approach")
                        // Fallback to legacy approach (for backward compatibility)
                        customEndpoint?.let { generateWithCustomAPI(it, apiKey, request) }
                            ?: Result.failure(Exception("Custom API endpoint is required"))
                    }
                }
                "ModelsLab" -> generateWithModelsLab(apiKey, request)
                "Replicate" -> {
                    Log.d(TAG, "Replicate provider selected, API key length: ${apiKey.length}")
                    if (apiKey.isBlank()) {
                        Result.failure(Exception("Replicate API key is required. Please check your settings."))
                    } else {
                        Log.d(TAG, "Calling generateWithReplicate with API key: ${apiKey.take(8)}...")
                        // Check if this is an image editing request (has init image and uses qwen-image-edit)
                        if (request.model == "qwen-image-edit") {
                            if (request.initImageBase64 != null) {
                                generateWithQwenImageEdit(apiKey, request)
                            } else {
                                // This should not happen since ChatViewModel now always provides character avatar for qwen-image-edit
                                Log.e(TAG, "qwen-image-edit called without input image - character avatar should be provided")
                                Result.failure(Exception("qwen-image-edit requires character avatar as input image"))
                            }
                        } else {
                            generateWithReplicate(apiKey, request)
                        }
                    }
                }
                "Grok" -> generateWithGrok(apiKey, request)
                "fal.ai" -> generateWithFalAI(apiKey, request)
                else -> Result.failure(Exception("Unsupported image provider: $provider"))
            }
            
            // Track the result
            result.fold(
                onSuccess = { imageResult ->
                    val trackedResult = TrackedImageGenerationResult(
                        id = finalGenerationId,
                        success = true,
                        imageUrl = imageResult.imageUrl,
                        imageBase64 = imageResult.imageBase64,
                        generationTime = imageResult.generationTime,
                        model = imageResult.model,
                        prompt = request.prompt
                    )
                    imageGenerationTracker.markCompleted(trackedResult)
                },
                onFailure = { exception ->
                    val trackedResult = TrackedImageGenerationResult(
                        id = finalGenerationId,
                        success = false,
                        error = exception.message,
                        model = request.model,
                        prompt = request.prompt
                    )
                    imageGenerationTracker.markCompleted(trackedResult)
                }
            )
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating image with $provider", e)
            
            // Track the error
            val trackedResult = TrackedImageGenerationResult(
                id = finalGenerationId,
                success = false,
                error = e.message,
                model = request.model,
                prompt = request.prompt
            )
            imageGenerationTracker.markCompleted(trackedResult)
            
            // Provide specific error messages for image generation
            val errorMessage = when {
                e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true -> 
                    "Authentication failed for $provider. Please check your API key in settings."
                e.message?.contains("403") == true || e.message?.contains("Forbidden") == true -> 
                    "Access denied by $provider. Your API key may not have the required permissions."
                e.message?.contains("404") == true -> 
                    "Image model '${request.model}' not found on $provider. Please select a different model."
                e.message?.contains("429") == true -> 
                    "Rate limit exceeded for $provider. Please wait a moment and try again."
                e.message?.contains("500") == true || e.message?.contains("502") == true || 
                e.message?.contains("503") == true || e.message?.contains("504") == true -> 
                    "$provider is experiencing technical difficulties. Please try again later."
                e.message?.contains("timeout") == true || e.message?.contains("Timeout") == true -> 
                    "$provider is taking too long to respond. Please try again."
                e.message?.contains("Connection") == true || e.message?.contains("network") == true -> 
                    "Unable to connect to $provider. Please check your internet connection."
                apiKey.isBlank() && provider in listOf("Together AI", "Hugging Face", "ModelsLab") -> 
                    "No API key configured for $provider. Please check your settings."
                customEndpoint.isNullOrBlank() && provider in listOf("ComfyUI", "Custom API") -> 
                    "No endpoint configured for $provider. Please check your settings."
                else -> 
                    "Image generation failed with $provider: ${e.message ?: "Unknown error"}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun fetchAvailableModels(
        provider: String,
        apiKey: String,
        customEndpoint: String? = null,
        manuallyAddedImageModels: List<String> = emptyList()
    ): Result<List<String>> {
        return try {
            when (provider) {
                "Together AI" -> fetchTogetherAIModels(apiKey)
                "Hugging Face" -> fetchHuggingFaceModels(apiKey)
                "ComfyUI" -> customEndpoint?.let { fetchComfyUIModels(it, apiKey) } ?: Result.success(getDefaultModelsForProvider("ComfyUI"))
                "Custom API" -> customEndpoint?.let { fetchCustomAPIModels(it, apiKey) } ?: Result.success(getDefaultModelsForProvider("Custom API"))
                "ModelsLab" -> fetchModelsLabModels(apiKey, manuallyAddedImageModels)
                "Replicate" -> Result.success(getDefaultModelsForProvider("Replicate"))
                "Grok" -> Result.success(getDefaultModelsForProvider("Grok"))
                "fal.ai" -> Result.success(getDefaultModelsForProvider("fal.ai"))
                else -> Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching models for provider $provider", e)
            Result.success(getDefaultModelsForProvider(provider))
        }
    }

    private suspend fun fetchModelsLabModels(apiKey: String, manuallyAddedImageModels: List<String> = emptyList()): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Fetching ModelsLab models with API key...")
            
            // Use the ModelsLabImageApi to fetch models
            val result = modelsLabApi.fetchPublicModels(apiKey)
            
            result.fold(
                onSuccess = { models ->
                    Log.i(TAG, "Successfully fetched ${models.size} ModelsLab models")
                    val fetchedModels = if (models.isNotEmpty()) {
                        models.take(100)
                    } else {
                        Log.w(TAG, "No models returned from ModelsLab API, using defaults")
                        getDefaultModelsForProvider("ModelsLab")
                    }
                    
                    // Combine fetched models with manually added models
                    val allModels = (fetchedModels + manuallyAddedImageModels).distinct()
                    Log.i(TAG, "Combined ${fetchedModels.size} fetched models with ${manuallyAddedImageModels.size} manual models = ${allModels.size} total")
                    
                    return@withContext Result.success(allModels)
                },
                onFailure = { exception ->
                    Log.w(TAG, "Failed to fetch ModelsLab models: ${exception.message}", exception)
                    // Return default models combined with manual models instead of failing completely
                    val defaultModels = getDefaultModelsForProvider("ModelsLab")
                    val allModels = (defaultModels + manuallyAddedImageModels).distinct()
                    Log.i(TAG, "Using ${defaultModels.size} default models + ${manuallyAddedImageModels.size} manual models = ${allModels.size} total")
                    return@withContext Result.success(allModels)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ModelsLab models", e)
            // Return default models combined with manual models to prevent crashes
            val defaultModels = getDefaultModelsForProvider("ModelsLab")
            val allModels = (defaultModels + manuallyAddedImageModels).distinct()
            Log.i(TAG, "Using ${defaultModels.size} default models + ${manuallyAddedImageModels.size} manual models = ${allModels.size} total")
            Result.success(allModels)
        }
    }

    /**
     * Fetch LoRA models for the specified provider
     */
    suspend fun fetchLoraModels(
        provider: String,
        apiKey: String,
        customEndpoint: String? = null,
        manuallyAddedLoraModels: List<String> = emptyList()
    ): Result<List<String>> {
        Log.i(TAG, "ImageGenerationService: Fetching LoRA models for provider: $provider")
        return when (provider) {
            "ModelsLab" -> {
                val result = modelsLabApi.fetchLoraModels(apiKey)
                result.map { modelInfoList ->
                    val fetchedModels = modelInfoList.map { it.modelId }
                    // Combine fetched and manually added models
                    val allModels = fetchedModels.toMutableList()
                    allModels.addAll(manuallyAddedLoraModels)
                    allModels.distinct()
                }.onSuccess {
                    Log.i(TAG, "ImageGenerationService: Successfully fetched ${it.size} LoRA models from ModelsLab (including ${manuallyAddedLoraModels.size} manual).")
                }.onFailure { e ->
                    Log.e(TAG, "ImageGenerationService: Failed to fetch LoRA models from ModelsLab: ${e.message}")
                }
            }
            "ComfyUI" -> {
                val endpoint = customEndpoint ?: "http://localhost:8188"
                fetchComfyUILoras(endpoint, apiKey, manuallyAddedLoraModels)
            }
            else -> Result.success(emptyList())
        }
    }

    /**
     * Fetch LoRA models from ComfyUI
     */
    private suspend fun fetchComfyUILoras(endpoint: String, apiKey: String, manuallyAddedLoraModels: List<String>): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpoint.removeSuffix("/")
            val sdxlLoras = mutableListOf<String>()
            val fluxLoras = mutableListOf<String>()

            // Try the dedicated /loras endpoint first
            try {
                val request = Request.Builder()
                    .url("$cleanUrl/loras")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { raw ->
                        try {
                            val arr = JSONArray(raw)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val fileName = obj.optString("filename", obj.optString("name"))
                                if (fileName.isNotBlank()) {
                                    val lower = fileName.lowercase()
                                    when {
                                        lower.contains("flux") || lower.contains("dev") -> fluxLoras.add(fileName)
                                        lower.contains("sdxl") || lower.contains("xl") -> sdxlLoras.add(fileName)
                                        else -> sdxlLoras.add(fileName) // Default to SDXL
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse /loras response: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to hit /loras endpoint: ${e.message}")
            }

            // Fallback to object_info parsing if no LoRAs found
            if (sdxlLoras.isEmpty() && fluxLoras.isEmpty()) {
                try {
                    val request = Request.Builder()
                        .url("$cleanUrl/object_info")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        response.body?.string()?.let { body ->
                            try {
                                val json = JSONObject(body)
                                val loraLoader = json.optJSONObject("LoraLoader")
                                val input = loraLoader?.optJSONObject("input")
                                val required = input?.optJSONObject("required")
                                val loraName = required?.optJSONArray("lora_name")
                                val loraList = loraName?.optJSONArray(0)
                                loraList?.let { list ->
                                    for (i in 0 until list.length()) {
                                        val file = list.optString(i)
                                        if (file.isNotBlank()) {
                                            val lower = file.lowercase()
                                            when {
                                                lower.contains("flux") || lower.contains("dev") -> fluxLoras.add(file)
                                                lower.contains("sdxl") || lower.contains("xl") -> sdxlLoras.add(file)
                                                else -> sdxlLoras.add(file) // Default to SDXL
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Fallback object_info parse failed: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback object_info request failed: ${e.message}")
                }
            }

            // Combine all LoRAs
            val allLoras = (sdxlLoras + fluxLoras + manuallyAddedLoraModels).distinct()
            
            Log.d(TAG, "ComfyUI: Found ${sdxlLoras.size} SDXL LoRAs, ${fluxLoras.size} Flux LoRAs, ${manuallyAddedLoraModels.size} manual LoRAs")
            Result.success(allLoras)
            
        } catch (e: Exception) {
            Log.e(TAG, "ComfyUI: Error fetching LoRAs", e)
            Result.success(manuallyAddedLoraModels)
        }
    }

    /**
     * Fetch comprehensive ModelsLab model information
     * Returns full model info objects instead of just IDs
     */
    suspend fun fetchModelsLabModelInfo(apiKey: String): Result<List<ModelsLabModelInfo>> {
        Log.i(TAG, "ImageGenerationService: Fetching comprehensive ModelsLab model information")
        return modelsLabApi.fetchDreamboothModels(apiKey).onSuccess { models ->
            Log.i(TAG, "ImageGenerationService: Successfully fetched ${models.size} detailed models from ModelsLab.")
        }.onFailure { e ->
            Log.e(TAG, "ImageGenerationService: Failed to fetch detailed models from ModelsLab: ${e.message}")
        }
    }

    /**
     * Fetch comprehensive ModelsLab LoRA information
     * Returns full LoRA info objects instead of just IDs
     */
    suspend fun fetchModelsLabLoraInfo(apiKey: String): Result<List<ModelsLabModelInfo>> {
        Log.i(TAG, "ImageGenerationService: Fetching comprehensive ModelsLab LoRA information")
        return modelsLabApi.fetchLoraModels(apiKey).onSuccess { loras ->
            Log.i(TAG, "ImageGenerationService: Successfully fetched ${loras.size} detailed LoRA models from ModelsLab.")
        }.onFailure { e ->
            Log.e(TAG, "ImageGenerationService: Failed to fetch detailed LoRA models from ModelsLab: ${e.message}")
        }
    }

    private suspend fun fetchTogetherAIModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val httpRequest = Request.Builder()
                .url(TOGETHER_AI_MODELS_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "Failed to fetch Together AI image models: ${response.code}")
                Log.e(TAG, "Together AI image models error body: $errorBody")
                
                val errorMessage = when (response.code) {
                    401 -> "Authentication failed - Invalid API key"
                    403 -> "Access denied - API key doesn't have image model permissions" 
                    404 -> "Together AI models endpoint not found"
                    429 -> "Rate limit exceeded"
                    500, 502, 503, 504 -> "Together AI server error"
                    else -> "Together AI API error: ${response.code}"
                }
                Log.e(TAG, "Together AI image error: $errorMessage")
                return@withContext Result.success(getDefaultModelsForProvider("Together AI"))
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.success(getDefaultModelsForProvider("Together AI"))
            
            Log.d(TAG, "Together AI image models response body length: ${responseBody.length}")
            Log.d(TAG, "Together AI image models response preview: ${responseBody.take(500)}...")
            
            // Together AI returns direct array, not {"data": [...]}
            val dataArray = try {
                JSONArray(responseBody)
            } catch (e: Exception) {
                // Fallback: try parsing as object with data field
                val responseJson = JSONObject(responseBody)
                responseJson.optJSONArray("data")
            }
            val imageModels = mutableListOf<String>()
            
            Log.d(TAG, "Together AI image models array length: ${dataArray?.length() ?: 0}")
            
            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val model = dataArray.getJSONObject(i)
                    val modelType = model.optString("type", "")
                    val modelId = model.optString("id", "")
                    val description = model.optString("description", "")
                    
                    // Use the type field for accurate filtering - Together AI provides clear types
                    val isImageModel = modelType.equals("image", ignoreCase = true)
                    
                    if (isImageModel && modelId.isNotBlank()) {
                        imageModels.add(modelId)
                        Log.d(TAG, "✅ Included image model: $modelId (type: $modelType)")
                    } else {
                        val reason = when {
                            modelId.isBlank() -> "empty ID"
                            modelType.isNotBlank() -> "not image type (type: $modelType)"
                            else -> "unknown type"
                        }
                        Log.d(TAG, "❌ Excluded image model: $modelId ($reason)")
                    }
                }
            }
            
            Log.d(TAG, "Together AI image filtering complete: ${imageModels.size} models included")
            
            if (imageModels.isEmpty()) {
                Log.e(TAG, "❌ NO IMAGE MODELS FOUND AFTER FILTERING - USING DEFAULTS")
                return@withContext Result.success(getDefaultModelsForProvider("Together AI"))
            }
            
            val finalModels = imageModels.sorted().take(100)
            Log.d(TAG, "✅ SUCCESS: Returning ${finalModels.size} Together AI image models to ViewModel")
            Result.success(finalModels)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Together AI models", e)
            Result.success(getDefaultModelsForProvider("Together AI"))
        }
    }
    
    private suspend fun fetchHuggingFaceModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Hugging Face doesn't have a simple API to list all image models
            // Return curated list of popular image generation models
            val popularModels = listOf(
                "runwayml/stable-diffusion-v1-5",
                "stabilityai/stable-diffusion-xl-base-1.0",
                "stabilityai/stable-diffusion-2-1",
                "CompVis/stable-diffusion-v1-4",
                "stabilityai/sdxl-turbo",
                "kandinsky-community/kandinsky-2-2-decoder",
                "playgroundai/playground-v2-1024px-aesthetic"
            )
            
            Result.success(popularModels)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Hugging Face models", e)
            Result.success(getDefaultModelsForProvider("Hugging Face"))
        }
    }
    
    private suspend fun fetchComfyUIModels(endpoint: String, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpoint.removeSuffix("/")
            val request = Request.Builder()
                .url("$cleanUrl/object_info")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val models = mutableListOf<String>()
                        
                        // Get checkpoint models
                        val checkpointLoader = json.optJSONObject("CheckpointLoaderSimple")
                        if (checkpointLoader != null) {
                            val input = checkpointLoader.optJSONObject("input")
                            if (input != null) {
                                val required = input.optJSONObject("required")
                                if (required != null) {
                                    val ckptName = required.optJSONArray("ckpt_name")
                                    if (ckptName != null && ckptName.length() > 0) {
                                        val modelList = ckptName.optJSONArray(0)
                                        if (modelList != null) {
                                            for (i in 0 until modelList.length()) {
                                                val modelName = modelList.optString(i)
                                                if (modelName.isNotBlank()) {
                                                    models.add(modelName)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Log.d(TAG, "ComfyUI: Found ${models.size} models")
                        return@withContext Result.success(models.take(100).ifEmpty { getDefaultModelsForProvider("ComfyUI") })
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "ComfyUI: Error parsing models", e)
                        return@withContext Result.success(getDefaultModelsForProvider("ComfyUI"))
                    }
                }
            } else {
                Log.w(TAG, "ComfyUI: object_info endpoint returned ${response.code}")
                return@withContext Result.success(getDefaultModelsForProvider("ComfyUI"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "ComfyUI: Error fetching models", e)
        }
        
        return@withContext Result.success(getDefaultModelsForProvider("ComfyUI"))
    }
    
    private suspend fun fetchCustomAPIModels(endpoint: String, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Get manually added Custom API models from DataStore
            val preferences = dataStore.data.first()
            val manuallyAddedModels = preferences[stringPreferencesKey("manually_added_custom_image_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            
            val allModels = mutableListOf<String>()
            
            // Add manually added models first
            allModels.addAll(manuallyAddedModels)
            
            if (apiKey.isBlank() || endpoint.isBlank()) {
                // If no API key/endpoint, return only manually added models or defaults
                return@withContext if (allModels.isNotEmpty()) {
                    Result.success(allModels)
                } else {
                    Result.success(getDefaultModelsForProvider("Custom API"))
                }
            }
            
            // Use the new CustomImageProvider for OpenAI-compatible model fetching
            val customProvider = com.vortexai.android.domain.service.image.CustomImageProvider().apply {
                setApiKey(apiKey)
                setEndpoint(endpoint)
            }
            
            val modelsResult = customProvider.fetchModels()
            if (modelsResult.isSuccess) {
                val fetchedModels = modelsResult.getOrNull() ?: emptyList()
                // Add fetched models (avoid duplicates)
                fetchedModels.forEach { modelId ->
                    if (!manuallyAddedModels.contains(modelId)) {
                        allModels.add(modelId)
                    }
                }
                Result.success(allModels.sorted().take(100))
            } else {
                // Return manually added models if fetching fails
                Result.success(if (allModels.isNotEmpty()) allModels else getDefaultModelsForProvider("Custom API"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Custom API models", e)
            // Return manually added models on error
            val preferences = dataStore.data.first()
            val manuallyAddedModels = preferences[stringPreferencesKey("manually_added_custom_image_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            Result.success(if (manuallyAddedModels.isNotEmpty()) manuallyAddedModels else getDefaultModelsForProvider("Custom API"))
        }
    }
    
    private fun getDefaultModelsForProvider(provider: String): List<String> {
        return when (provider) {
            "Together AI" -> listOf(
                "black-forest-labs/FLUX.1-schnell",
                "black-forest-labs/FLUX.1-dev", 
                "stabilityai/stable-diffusion-xl-base-1.0",
                "stabilityai/stable-diffusion-2-1-base",
                "runwayml/stable-diffusion-v1-5",
                "wavymulder/Analog-Diffusion",
                "SG161222/Realistic_Vision_V2.0",
                "prompthero/openjourney-v4"
            )
            "Hugging Face" -> listOf(
                "runwayml/stable-diffusion-v1-5",
                "stabilityai/stable-diffusion-xl-base-1.0",
                "stabilityai/stable-diffusion-2-1",
                "CompVis/stable-diffusion-v1-4",
                "stabilityai/sdxl-turbo"
            )
            "ComfyUI" -> listOf(
                "sd_xl_base_1.0.safetensors",
                "v1-5-pruned-emaonly.ckpt",
                "sd_xl_turbo_1.0_fp16.safetensors"
            )
            "Custom API" -> listOf(
                "stable-diffusion-xl",
                "stable-diffusion-v1-5",
                "flux-dev"
            )
            "ModelsLab" -> listOf(
                "stable-diffusion-v1-5",
                "analog-diffusion", 
                "anything-v3",
                "dreamshaper-8",
                "meinamix",
                "rev-animated",
                "sdxl",
                "realistic-vision-v2",
                "openjourney-v4",
                "flux-dev",
                "kandinsky-2-2",
                "playground-v2",
                "dreamlike-diffusion",
                "sdxl-turbo",
                "realistic-vision-v1-4"
            )
            "Replicate" -> listOf(
                "black-forest-labs/flux-1.1-pro",
                "black-forest-labs/flux-pro",
                "black-forest-labs/flux-dev",
                "black-forest-labs/flux-schnell",
                "black-forest-labs/flux-dev-lora",
                "lucataco/flux2-dev-lora",
                "lucataco/flux2-pro",
                "stability-ai/sdxl",
                "playgroundai/playground-v2.5-1024px-aesthetic",
                "tencent/hunyuan-image-3",
                "qwen/qwen-image-edit"
            )
            "fal.ai" -> listOf(
                "fal-ai/flux-pro/v1.1",
                "fal-ai/flux-pro/v1.1-ultra",
                "fal-ai/flux/dev",
                "fal-ai/flux/schnell",
                "fal-ai/flux-realism",
                "fal-ai/flux-lora",
                "fal-ai/aura-flow",
                "fal-ai/stable-diffusion-v3-medium",
                "fal-ai/fast-sdxl"
            )
            "Grok" -> listOf(
                "grok-2-image-1212",
                "grok-2-image",
                "aurora" // Future Aurora-based models
            )
            else -> listOf("stabilityai/stable-diffusion-xl-base-1.0")
        }
    }
    
    private suspend fun generateWithTogetherAI(
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Together AI image generation - Model: ${request.model}, Prompt: ${request.prompt.take(50)}...")
            
            // Validate model for Together AI
            val validModel = if (request.model.contains("stabilityai") && !request.model.contains("flux", ignoreCase = true)) {
                // Replace Stability AI models with FLUX for Together AI
                "black-forest-labs/FLUX.1-schnell"
            } else {
                request.model
            }
            
            Log.d(TAG, "Using model: $validModel (original: ${request.model})")
            
            val jsonBody = JSONObject().apply {
                put("model", validModel)
                put("prompt", request.prompt)
                put("width", request.width)
                put("height", request.height)
                put("steps", request.steps)
                put("n", 1)
                request.seed?.let { put("seed", it) }
            }
            
            Log.d(TAG, "Together AI request body: ${jsonBody.toString()}")
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url(TOGETHER_AI_BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = client.newCall(httpRequest).execute()
            val generationTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Together AI response code: ${response.code}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Together AI API error: ${response.code} - $errorBody")
                
                // Provide specific error messages based on response
                val errorMessage = when (response.code) {
                    401 -> "Authentication failed for Together AI. Please check your API key."
                    403 -> "Access denied by Together AI. Your API key may not have image generation permissions."
                    404 -> "Image model '${request.model}' not found on Together AI. Please select a different model."
                    429 -> "Rate limit exceeded for Together AI. Please wait a moment and try again."
                    500, 502, 503, 504 -> "Together AI is experiencing technical difficulties. Please try again later."
                    else -> "Together AI API error (${response.code}): $errorBody"
                }
                
                return@withContext Result.failure(Exception(errorMessage))
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                Log.e(TAG, "Together AI returned empty response body")
                return@withContext Result.failure(Exception("Empty response from Together AI"))
            }
            
            Log.d(TAG, "Together AI response body: ${responseBody.take(200)}...")
            
            val responseJson = JSONObject(responseBody)
            val dataArray = responseJson.optJSONArray("data")
            
            if (dataArray != null && dataArray.length() > 0) {
                val imageData = dataArray.getJSONObject(0)
                val imageUrl = imageData.optString("url")
                val imageBase64 = imageData.optString("b64_json")
                
                Log.d(TAG, "Together AI image generation successful - URL: ${imageUrl.isNotBlank()}, Base64: ${imageBase64.isNotBlank()}")
                
                if (imageUrl.isBlank() && imageBase64.isBlank()) {
                    return@withContext Result.failure(Exception("No image URL or base64 data in response"))
                }
                
                Result.success(
                    ImageGenerationResult(
                        success = true,
                        imageUrl = imageUrl.takeIf { it.isNotBlank() },
                        imageBase64 = imageBase64.takeIf { it.isNotBlank() },
                        generationTime = generationTime,
                        model = request.model
                    )
                )
            } else {
                Log.e(TAG, "Together AI response missing data array or empty")
                Result.failure(Exception("No image data in Together AI response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with Together AI image generation", e)
            Result.failure(Exception("Together AI image generation failed: ${e.message}"))
        }
    }
    
    private suspend fun generateWithHuggingFace(
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("inputs", request.prompt)
                put("parameters", JSONObject().apply {
                    put("num_inference_steps", request.steps)
                    put("guidance_scale", request.guidanceScale)
                    put("width", request.width)
                    put("height", request.height)
                    request.seed?.let { put("seed", it) }
                })
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val url = "$HUGGINGFACE_BASE_URL/${request.model}"
            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = client.newCall(httpRequest).execute()
            val generationTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Hugging Face API error: ${response.code} - $errorBody")
                return@withContext Result.failure(
                    Exception("Hugging Face API error: ${response.code} - $errorBody")
                )
            }
            
            // Hugging Face returns binary image data
            val responseBody = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            // Convert to base64 for storage/display
            val base64Image = android.util.Base64.encodeToString(
                responseBody, 
                android.util.Base64.NO_WRAP
            )
            
            Result.success(
                ImageGenerationResult(
                    success = true,
                    imageBase64 = base64Image,
                    generationTime = generationTime,
                    model = request.model
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error with Hugging Face generation", e)
            Result.failure(e)
        }
    }
    
    private suspend fun generateWithComfyUI(
        endpoint: String,
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpoint.removeSuffix("/")
            
            // Load the appropriate workflow based on the request
            val workflowJson = when (request.workflow) {
                "Flux Dev" -> loadFluxWorkflow(request)
                "SDXL" -> loadSDXLWorkflow(request)
                else -> loadSDXLWorkflow(request) // Default to SDXL
            }
            
            // Submit the workflow to ComfyUI
            val promptRequest = JSONObject().apply {
                put("prompt", workflowJson)
                put("client_id", "VortexAndroid")
            }
            
            val requestBody = promptRequest.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$cleanUrl/prompt")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            // Log Request
            logger.logRequest(
                provider = "ComfyUI",
                endpoint = cleanUrl,
                modelOrWorkflow = request.workflow ?: "SDXL",
                character = null,
                requestData = promptRequest.toString()
            )

            val startTime = System.currentTimeMillis()
            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                logger.logError("ComfyUI", "HTTP ${response.code}", errorBody)
                return@withContext Result.failure(
                    Exception("ComfyUI API error: ${response.code} - $errorBody")
                )
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            logger.logResponse("ComfyUI", responseBody)
            
            val responseJson = JSONObject(responseBody)
            val promptId = responseJson.optString("prompt_id")
            
            if (promptId.isBlank()) {
                logger.logError("ComfyUI", "No prompt ID returned", responseBody)
                return@withContext Result.failure(Exception("No prompt ID returned"))
            }
            
            // Poll for completion
            val imageUrl = pollForCompletion(cleanUrl, promptId)
            val generationTime = System.currentTimeMillis() - startTime
            
            Result.success(
                ImageGenerationResult(
                    success = true,
                    imageUrl = imageUrl,
                    imageBase64 = null,
                    generationTime = generationTime,
                    model = request.model
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error with ComfyUI generation", e)
            Result.failure(e)
        }
    }
    
    private fun loadSDXLWorkflow(request: ImageGenerationRequest): JSONObject {
        val workflow = JSONObject().apply {
            put("1", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("ckpt_name", request.model)
                })
                put("class_type", "CheckpointLoaderSimple")
                put("_meta", JSONObject().apply {
                    put("title", "Load Checkpoint")
                })
            })
            
            // Add LoRA loader if LoRA is specified
            if (!request.loraModel.isNullOrBlank()) {
                put("2", JSONObject().apply {
                    put("inputs", JSONObject().apply {
                        put("lora_name", request.loraModel)
                        put("strength_model", request.loraStrength ?: 1.0f)
                        put("strength_clip", request.loraStrength ?: 1.0f)
                        put("model", JSONArray().apply {
                            put("1")
                            put(0)
                        })
                        put("clip", JSONArray().apply {
                            put("1")
                            put(1)
                        })
                    })
                    put("class_type", "LoraLoader")
                    put("_meta", JSONObject().apply {
                        put("title", "Load LoRA")
                    })
                })
            }
            
            val clipSource = if (!request.loraModel.isNullOrBlank()) "2" else "1"
            val modelSource = if (!request.loraModel.isNullOrBlank()) "2" else "1"
            
            put("3", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("text", request.prompt)
                    put("clip", JSONArray().apply {
                        put(clipSource)
                        put(1)
                    })
                })
                put("class_type", "CLIPTextEncode")
                put("_meta", JSONObject().apply {
                    put("title", "CLIP Text Encode (Positive)")
                })
            })
            
            put("4", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("text", request.negativePrompt ?: "")
                    put("clip", JSONArray().apply {
                        put(clipSource)
                        put(1)
                    })
                })
                put("class_type", "CLIPTextEncode")
                put("_meta", JSONObject().apply {
                    put("title", "CLIP Text Encode (Negative)")
                })
            })
            
            put("5", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("width", request.width)
                    put("height", request.height)
                    put("batch_size", 1)
                })
                put("class_type", "EmptyLatentImage")
                put("_meta", JSONObject().apply {
                    put("title", "Empty Latent Image")
                })
            })
            
            put("6", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("seed", request.seed ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
                    put("steps", request.steps)
                    put("cfg", request.guidanceScale)
                    put("sampler_name", "euler_ancestral")
                    put("scheduler", "normal")
                    put("denoise", 1)
                    put("model", JSONArray().apply {
                        put(modelSource)
                        put(0)
                    })
                    put("positive", JSONArray().apply {
                        put("3")
                        put(0)
                    })
                    put("negative", JSONArray().apply {
                        put("4")
                        put(0)
                    })
                    put("latent_image", JSONArray().apply {
                        put("5")
                        put(0)
                    })
                })
                put("class_type", "KSampler")
                put("_meta", JSONObject().apply {
                    put("title", "KSampler")
                })
            })
            
            put("7", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("samples", JSONArray().apply {
                        put("6")
                        put(0)
                    })
                    put("vae", JSONArray().apply {
                        put("1")
                        put(2)
                    })
                })
                put("class_type", "VAEDecode")
                put("_meta", JSONObject().apply {
                    put("title", "VAE Decode")
                })
            })
            
            put("8", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("filename_prefix", "VortexAndroid")
                    put("images", JSONArray().apply {
                        put("7")
                        put(0)
                    })
                })
                put("class_type", "SaveImage")
                put("_meta", JSONObject().apply {
                    put("title", "Save Image")
                })
            })
        }
        
        return workflow
    }
    
    private fun loadFluxWorkflow(request: ImageGenerationRequest): JSONObject {
        val workflow = JSONObject().apply {
            put("1", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("ckpt_name", request.model)
                })
                put("class_type", "CheckpointLoaderSimple")
                put("_meta", JSONObject().apply {
                    put("title", "Load Checkpoint")
                })
            })
            
            // Add LoRA loader if LoRA is specified
            if (!request.loraModel.isNullOrBlank()) {
                put("2", JSONObject().apply {
                    put("inputs", JSONObject().apply {
                        put("lora_name", request.loraModel)
                        put("strength_model", request.loraStrength ?: 1.0f)
                        put("strength_clip", request.loraStrength ?: 1.0f)
                        put("model", JSONArray().apply {
                            put("1")
                            put(0)
                        })
                        put("clip", JSONArray().apply {
                            put("1")
                            put(1)
                        })
                    })
                    put("class_type", "LoraLoader")
                    put("_meta", JSONObject().apply {
                        put("title", "Load LoRA")
                    })
                })
            }
            
            val clipSource = if (!request.loraModel.isNullOrBlank()) "2" else "1"
            val modelSource = if (!request.loraModel.isNullOrBlank()) "2" else "1"
            
            put("3", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("text", request.prompt)
                    put("clip", JSONArray().apply {
                        put(clipSource)
                        put(1)
                    })
                })
                put("class_type", "CLIPTextEncode")
                put("_meta", JSONObject().apply {
                    put("title", "CLIP Text Encode (Positive)")
                })
            })
            
            put("4", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("text", request.negativePrompt ?: "")
                    put("clip", JSONArray().apply {
                        put(clipSource)
                        put(1)
                    })
                })
                put("class_type", "CLIPTextEncode")
                put("_meta", JSONObject().apply {
                    put("title", "CLIP Text Encode (Negative)")
                })
            })
            
            put("5", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("width", request.width)
                    put("height", request.height)
                    put("batch_size", 1)
                })
                put("class_type", "EmptySD3LatentImage")
                put("_meta", JSONObject().apply {
                    put("title", "Empty SD3 Latent Image")
                })
            })
            
            put("6", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("guidance", request.guidanceScale)
                    put("conditioning", JSONArray().apply {
                        put("3")
                        put(0)
                    })
                })
                put("class_type", "FluxGuidance")
                put("_meta", JSONObject().apply {
                    put("title", "Flux Guidance")
                })
            })
            
            put("7", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("seed", request.seed ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
                    put("steps", request.steps)
                    put("cfg", 1)
                    put("sampler_name", "euler")
                    put("scheduler", "simple")
                    put("denoise", 1)
                    put("model", JSONArray().apply {
                        put(modelSource)
                        put(0)
                    })
                    put("positive", JSONArray().apply {
                        put("6")
                        put(0)
                    })
                    put("negative", JSONArray().apply {
                        put("4")
                        put(0)
                    })
                    put("latent_image", JSONArray().apply {
                        put("5")
                        put(0)
                    })
                })
                put("class_type", "KSampler")
                put("_meta", JSONObject().apply {
                    put("title", "KSampler")
                })
            })
            
            put("8", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("samples", JSONArray().apply {
                        put("7")
                        put(0)
                    })
                    put("vae", JSONArray().apply {
                        put("1")
                        put(2)
                    })
                })
                put("class_type", "VAEDecode")
                put("_meta", JSONObject().apply {
                    put("title", "VAE Decode")
                })
            })
            
            put("9", JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("filename_prefix", "VortexAndroid")
                    put("images", JSONArray().apply {
                        put("8")
                        put(0)
                    })
                })
                put("class_type", "SaveImage")
                put("_meta", JSONObject().apply {
                    put("title", "Save Image")
                })
            })
        }
        
        return workflow
    }
    
    private suspend fun pollForCompletion(endpoint: String, promptId: String): String {
        val maxAttempts = 300 // 25 minutes max (5 minutes * 60 seconds / 5 seconds per attempt)
        var attempts = 0
        
        while (attempts < maxAttempts) {
            try {
                val request = Request.Builder()
                    .url("$endpoint/history/$promptId")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val history = json.optJSONObject(promptId)
                        if (history != null) {
                            val outputs = history.optJSONObject("outputs")
                            if (outputs != null) {
                                // Look for SaveImage node output
                                for (key in outputs.keys()) {
                                    val nodeOutput = outputs.optJSONObject(key)
                                    val images = nodeOutput?.optJSONArray("images")
                                    if (images != null && images.length() > 0) {
                                        val image = images.getJSONObject(0)
                                        val filename = image.optString("filename")
                                        if (filename.isNotBlank()) {
                                            return "$endpoint/view?filename=$filename"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                attempts++
                kotlinx.coroutines.delay(5000) // Wait 5 seconds before next attempt
                
            } catch (e: Exception) {
                Log.e(TAG, "Error polling for completion", e)
                attempts++
                kotlinx.coroutines.delay(5000)
            }
        }
        
        throw Exception("Generation timed out after ${maxAttempts * 5} seconds (25 minutes)")
    }
    
    private suspend fun generateWithCustomAPI(
        endpoint: String,
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            // Use the new CustomImageProvider for OpenAI-compatible image generation
            val customProvider = com.vortexai.android.domain.service.image.CustomImageProvider().apply {
                setApiKey(apiKey)
                setEndpoint(endpoint)
            }
            
            // Convert size to OpenAI format
            val size = "${request.width}x${request.height}"
            
            val result = customProvider.generateImage(
                prompt = request.prompt,
                model = request.model,
                size = size,
                quality = "standard",
                style = "vivid",
                n = 1
            )
            
            if (result.isSuccess) {
                val customResult = result.getOrNull()!!
                val firstImage = customResult.images.firstOrNull()
                
                if (firstImage != null) {
                    Result.success(
                        ImageGenerationResult(
                            success = true,
                            imageUrl = firstImage.url,
                            imageBase64 = firstImage.b64Json,
                            generationTime = customResult.generationTime,
                            model = request.model
                        )
                    )
                } else {
                    Result.failure(Exception("No images generated by Custom API"))
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Custom API generation failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with Custom API generation", e)
            Result.failure(e)
        }
    }
    
    /**
     * Schema-driven Custom API image generation that uses user-configured schemas from the database.
     * This method supports any API format including fal.ai, OpenAI, or custom endpoints.
     */
    private suspend fun generateWithSchemaBasedCustomAPI(
        providerId: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Schema-based Custom API generation - Provider ID: $providerId, Model: ${request.model}")
            Log.d(TAG, "Request details - Prompt: ${request.prompt.take(50)}, Width: ${request.width}, Height: ${request.height}")
            
            // Fetch provider from database
            val provider = customApiProviderRepository.getProviderById(providerId)
            if (provider == null) {
                Log.e(TAG, "Provider not found for ID: $providerId")
                return@withContext Result.failure(Exception("Custom API provider not found"))
            }
            Log.d(TAG, "Provider found: ${provider.name}, enabled: ${provider.isEnabled}")
            
            if (!provider.isEnabled) {
                Log.e(TAG, "Provider '${provider.name}' is disabled")
                return@withContext Result.failure(Exception("Custom provider '${provider.name}' is disabled"))
            }
            
            // Fetch endpoint with image_gen purpose
            val endpoint = customApiProviderRepository.getEndpointByPurpose(providerId, "image_gen")
            if (endpoint == null) {
                Log.e(TAG, "No endpoint found with purpose 'image_gen' for provider: ${provider.name}")
                return@withContext Result.failure(Exception("No image generation endpoint configured for '${provider.name}'. Please configure an endpoint with purpose 'image_gen'."))
            }
            Log.d(TAG, "Endpoint found: ${provider.baseUrl}${endpoint.endpointPath}")
            
            // Find the model (first try exact match with request.model, then use first active model)
            val modelsFlow = customApiProviderRepository.getActiveModelsByProvider(providerId)
            val models = modelsFlow.first()
            Log.d(TAG, "Found ${models.size} active models")
            
            val model = models.find { it.modelId == request.model } 
                ?: models.firstOrNull()
            if (model == null) {
                Log.e(TAG, "No active model found for provider: ${provider.name}")
                return@withContext Result.failure(Exception("No active model found for '${provider.name}'"))
            }
            Log.d(TAG, "Using model: ${model.displayName} (${model.modelId})")
            
            // Get saved parameter values for this model
            Log.d(TAG, "Fetching saved parameters for model ID: ${model.id}...")
            val savedParams = try {
                customApiProviderRepository.getParameterValuesMap(model.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch parameter values: ${e.message}", e)
                emptyMap()
            }
            Log.d(TAG, "Saved parameters retrieved: $savedParams")
            
            // Build request parameters with prompt and other values
            val requestParams = mutableMapOf<String, Any>(
                "prompt" to request.prompt,
                "modelId" to model.modelId
            )
            
            // Add size parameters
            requestParams["width"] = request.width
            requestParams["height"] = request.height
            requestParams["num_images"] = 1
            
            // Add optional parameters if present
            request.negativePrompt?.let { requestParams["negative_prompt"] = it }
            request.seed?.let { requestParams["seed"] = it }
            requestParams["steps"] = request.steps
            requestParams["guidance_scale"] = request.guidanceScale
            
            // Merge with saved parameters (saved params can override defaults)
            requestParams.putAll(savedParams)
            
            Log.d(TAG, "Schema-based request params: $requestParams")
            Log.d(TAG, "Using endpoint: ${provider.baseUrl}${endpoint.endpointPath}")
            
            val startTime = System.currentTimeMillis()
            
            // Check if this is an async/polling API (based on responseSchema.statusPath)
            val responseSchema = com.vortexai.android.utils.ApiSchemaParser.parseResponseSchema(endpoint.responseSchemaJson)
            val isAsyncApi = !responseSchema?.statusPath.isNullOrBlank()
            Log.d(TAG, "API type: ${if (isAsyncApi) "Async (polling)" else "Synchronous"}")
            if (isAsyncApi) {
                Log.d(TAG, "Status path: ${responseSchema?.statusPath}")
            }
            
            Log.d(TAG, "Preparing to execute ${if (isAsyncApi) "async" else "sync"} API request...")
            
            val result = if (isAsyncApi) {
                // Async API with polling (like fal.ai queue)
                Log.d(TAG, "Calling executeAsyncSchemaRequest...")
                executeAsyncSchemaRequest(provider, endpoint, model, requestParams)
            } else {
                // Synchronous API (direct request/response)
                Log.d(TAG, "Calling customApiExecutor.executeRequest...")
                customApiExecutor.executeRequest(provider, endpoint, model, requestParams)
            }
            
            Log.d(TAG, "API call returned, processing result...")
            
            val generationTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Executing ${if (isAsyncApi) "async" else "sync"} request...")
            
            result.fold(
                onSuccess = { responseJson ->
                    Log.d(TAG, "Request successful, parsing response...")
                    Log.d(TAG, "Response preview: ${responseJson.take(200)}")
                    // Parse the response to extract image URL
                    val parseResult = customApiExecutor.parseResponse(responseJson, endpoint)
                    parseResult.fold(
                        onSuccess = { imageUrl ->
                            Log.d(TAG, "Schema-based generation successful, image URL: $imageUrl")
                            Result.success(
                                ImageGenerationResult(
                                    success = true,
                                    imageUrl = imageUrl,
                                    imageBase64 = null,
                                    generationTime = generationTime,
                                    model = model.modelId
                                )
                            )
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to parse Custom API response: ${error.message}")
                            Log.e(TAG, "Response was: ${responseJson.take(500)}")
                            Result.failure(Exception("Failed to parse response: ${error.message}"))
                        }
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Custom API request failed: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error with schema-based Custom API generation", e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute an async schema-based request with polling for APIs like fal.ai.
     * This submits the request, then polls for completion.
     */
    private suspend fun executeAsyncSchemaRequest(
        provider: com.vortexai.android.data.models.CustomApiProvider,
        endpoint: com.vortexai.android.data.models.CustomApiEndpoint,
        model: com.vortexai.android.data.models.CustomApiModel,
        params: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val responseSchema = com.vortexai.android.utils.ApiSchemaParser.parseResponseSchema(endpoint.responseSchemaJson)
            val statusPath = responseSchema?.statusPath ?: "status"
            
            Log.d(TAG, "Async API: Submitting request to ${provider.baseUrl}${endpoint.endpointPath}")
            
            // Step 1: Submit initial request
            val submitResult = customApiExecutor.executeRequest(provider, endpoint, model, params)
            
            if (submitResult.isFailure) {
                Log.e(TAG, "Async API: Submit failed - ${submitResult.exceptionOrNull()?.message}")
                return@withContext submitResult
            }
            
            val submitResponse = submitResult.getOrNull()!!
            Log.d(TAG, "Async API: Submit response: ${submitResponse.take(200)}")
            val submitJson = org.json.JSONObject(submitResponse)
            
            // Extract request_id for polling (common pattern for async APIs)
            val requestId = submitJson.optString("request_id")
                ?: submitJson.optString("id")
                ?: submitJson.optString("requestId")
            
            if (requestId.isBlank()) {
                // No request ID - might be a synchronous response, return as-is
                Log.d(TAG, "Async API: No request_id found, treating as synchronous response")
                return@withContext Result.success(submitResponse)
            }
            
            Log.d(TAG, "Async API: Request submitted, polling for request_id: $requestId")
            
            // Step 2: Poll for completion
            val maxAttempts = 60
            var attempts = 0
            
            while (attempts < maxAttempts) {
                // Build polling URL - use default pattern for now
                val baseUrl = provider.baseUrl.trimEnd('/')
                val pollingUrl = "$baseUrl/requests/$requestId/status"
                
                Log.d(TAG, "Async API: Polling attempt $attempts - URL: $pollingUrl")
                
                val pollingRequest = okhttp3.Request.Builder()
                    .url(pollingUrl)
                    .addHeader("Authorization", "Key ${provider.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()
                
                val response = client.newCall(pollingRequest).execute()
                
                Log.d(TAG, "Async API: Poll response code: ${response.code}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        Log.d(TAG, "Async API: Poll response: ${responseBody.take(200)}")
                        val json = org.json.JSONObject(responseBody)
                        val status = com.vortexai.android.utils.ApiSchemaParser.extractValueFromPath(responseBody, statusPath)
                            ?: json.optString("status")
                        
                        Log.d(TAG, "Async API: Poll attempt $attempts: status=$status")
                        
                        when (status?.uppercase()) {
                            "COMPLETED", "SUCCESS", "DONE" -> {
                                Log.d(TAG, "Async API: Request completed successfully")
                                return@withContext Result.success(responseBody)
                            }
                            "FAILED", "ERROR" -> {
                                val error = json.optString("error", "Generation failed")
                                Log.e(TAG, "Async API: Generation failed - $error")
                                return@withContext Result.failure(Exception("Async generation failed: $error"))
                            }
                            "IN_PROGRESS", "PENDING", "PROCESSING", "IN_QUEUE" -> {
                                Log.d(TAG, "Async API: Still processing, continuing to poll...")
                                // Continue polling
                            }
                            else -> {
                                // Unknown status, check if we have result data
                                if (json.has("images") || json.has("data") || json.has("output")) {
                                    Log.d(TAG, "Async API: Found result data despite unknown status")
                                    return@withContext Result.success(responseBody)
                                }
                                Log.d(TAG, "Async API: Unknown status '$status', continuing to poll...")
                            }
                        }
                    }
                } else {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e(TAG, "Async API: Poll failed with code ${response.code}: $errorBody")
                }
                
                attempts++
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(2000) // Poll every 2 seconds
                }
            }
            
            Log.e(TAG, "Async API: Timed out after ${maxAttempts * 2} seconds")
            Result.failure(Exception("Async generation timed out after ${maxAttempts * 2} seconds"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Async API: Error in async schema request", e)
            Result.failure(e)
        }
    }
    
    private suspend fun generateWithModelsLab(
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        // For Flux workflow: always use "flux" model and ignore LoRA settings
        val finalRequest = if (request.workflow == "flux") {
            request.copy(
                model = "flux", // Always "flux" for Flux workflow
                loraModel = null, // No LoRA for Flux
                loraStrength = null // No LoRA strength for Flux
            )
        } else {
            request
        }

        val startTime = System.currentTimeMillis()
        
        val imageUrlResult: Result<String> = when {
            // FLUX workflow identifier - use dedicated Flux endpoint (FLUX doesn't support img2img yet)
            finalRequest.workflow == "flux" -> {
                if (finalRequest.initImageBase64 != null) {
                    Log.w(TAG, "ModelsLab FLUX workflow doesn't support img2img, using text2img instead")
                }
                modelsLabApi.fluxText2Img(apiKey, finalRequest)
            }
            // If an init image is provided, prefer img2img, checking for realtime workflow first
            finalRequest.initImageBase64 != null -> {
                when (finalRequest.workflow) {
                    "realtime" -> modelsLabApi.realtimeImage2Img(apiKey, finalRequest)
                    "lora" -> {
                        // For LoRA workflow with img2img, use regular img2img endpoint 
                        // (LoRA models are automatically applied in the payload)
                        modelsLabApi.img2Img(apiKey, finalRequest)
                    }
                    else -> modelsLabApi.img2Img(apiKey, finalRequest)
                }
            }
            // Realtime SD workflow identifier
            finalRequest.workflow == "realtime" -> modelsLabApi.realtimeText2Img(apiKey, finalRequest)
            // LoRA single-model (not available for Flux)
            finalRequest.loraModel != null && finalRequest.workflow != "flux" -> modelsLabApi.loraText2Img(apiKey, finalRequest)
            else -> modelsLabApi.text2Img(apiKey, finalRequest)
        }

        return@withContext imageUrlResult.fold(
            onSuccess = { url ->
                val generationTime = System.currentTimeMillis() - startTime
                Result.success(ImageGenerationResult(
                    success = true,
                    imageUrl = url,
                    imageBase64 = null,
                    generationTime = generationTime,
                    model = finalRequest.model
                ))
            },
            onFailure = { e -> Result.failure(e) }
        )
    }
    
    private suspend fun generateWithReplicate(
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Replicate image generation - Model: ${request.model}, Prompt: ${request.prompt.take(50)}..., HasInitImage: ${request.initImageBase64 != null}")
            
            // Validate API key
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Replicate API key is required"))
            }
            
            // Validate API key format (Replicate tokens should start with "r8_")
            if (!apiKey.startsWith("r8_")) {
                Log.w(TAG, "Replicate API key does not start with 'r8_', this might cause authentication issues")
                Log.w(TAG, "API key format: ${apiKey.take(10)}...")
                // Don't fail here, just warn - some tokens might have different formats
            }
            
            val startTime = System.currentTimeMillis()
            
            // Step 1: Create prediction
            val predictionId = try {
                createReplicatePrediction(apiKey, request)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create Replicate prediction: ${e.message}")
                return@withContext Result.failure(e)
            }
            
            if (predictionId == null) {
                return@withContext Result.failure(Exception("Failed to create Replicate prediction - no prediction ID returned"))
            }
            
            Log.d(TAG, "Replicate prediction created: $predictionId")
            
            // Step 2: Poll for completion
            val imageUrl = try {
                pollReplicatePrediction(apiKey, predictionId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to poll Replicate prediction: ${e.message}")
                return@withContext Result.failure(e)
            }
            
            if (imageUrl == null) {
                return@withContext Result.failure(Exception("Failed to get result from Replicate prediction - no image URL returned"))
            }
            
            val generationTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Replicate image generation successful - URL: $imageUrl")
            
            Result.success(
                ImageGenerationResult(
                    success = true,
                    imageUrl = imageUrl,
                    imageBase64 = null,
                    generationTime = generationTime,
                    model = request.model
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error with Replicate image generation", e)
            Result.failure(e) // Pass through the original exception
        }
    }
    
    private suspend fun getDisableSafetyCheckerPreference(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences[androidx.datastore.preferences.core.booleanPreferencesKey("replicate_disable_safety_checker")] ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error getting disable_safety_checker preference", e)
            false
        }
    }
    
    private suspend fun fetchReplicateModelVersion(apiKey: String, modelIdentifier: String): String {
        val url = "https://api.replicate.com/v1/models/$modelIdentifier"
        Log.d(TAG, "Fetching Replicate model version from URL: $url")
        Log.d(TAG, "Model identifier: $modelIdentifier")
        Log.d(TAG, "API key prefix: ${apiKey.take(10)}...")
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No error body"
            Log.e(TAG, "Replicate model fetch failed - Status: ${response.code}, Body: $errorBody")
            throw Exception("Failed to fetch model version: ${response.code}")
        }
        
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(responseBody)
        val versionId = json.optJSONObject("latest_version")?.optString("id")
            ?: throw Exception("No version ID found")
        
        return versionId
    }
    
    private suspend fun createReplicatePrediction(apiKey: String, request: ImageGenerationRequest): String? {
        return try {
            Log.d(TAG, "Creating Replicate prediction with model: ${request.model}")
            
            // Step 1: Fetch model version hash
            val versionHash = fetchReplicateModelVersion(apiKey, request.model)
            Log.d(TAG, "Fetched version for ${request.model}: $versionHash")
            
            // Step 2: Get disable_safety_checker preference
            val disableSafetyChecker = getDisableSafetyCheckerPreference()
            
            // Step 3: Build input parameters based on model
            Log.d(TAG, "ImageGenerationRequest - width: ${request.width}, height: ${request.height}")
            val input = JSONObject().apply {
                put("prompt", request.prompt)
                
                when (request.model) {
                    "qwen/qwen-image-edit" -> {
                        if (request.initImageBase64 != null) {
                            val imageParam = if (request.initImageBase64.startsWith("data:image")) {
                                request.initImageBase64
                            } else {
                                "data:image/jpeg;base64,${request.initImageBase64}"
                            }
                            put("image", imageParam)
                        } else {
                            throw Exception("qwen-image-edit requires an input image")
                        }
                        put("go_fast", true)
                        put("output_format", "webp")
                        put("output_quality", 80)
                    }
                    "black-forest-labs/flux-1.1-pro", "black-forest-labs/flux-pro", "lucataco/flux2-pro" -> {
                        // FLUX Pro models support higher resolution and quality
                        put("width", request.width)
                        put("height", request.height)
                        put("steps", request.steps)
                        put("guidance", request.guidanceScale)
                        request.seed?.let { put("seed", it) }
                        put("safety_tolerance", if (disableSafetyChecker) 6 else 2)
                        put("output_format", "jpeg")
                        put("output_quality", 95)
                    }
                    "lucataco/flux2-dev-lora" -> {
                        // FLUX 2 Dev with LoRA support
                        val aspectRatio = when {
                            request.width == request.height -> "1:1"
                            request.width > request.height -> "16:9"
                            else -> "9:16"
                        }
                        put("aspect_ratio", aspectRatio)
                        put("num_inference_steps", request.steps)
                        put("guidance_scale", request.guidanceScale)
                        request.seed?.let { put("seed", it) }
                        put("disable_safety_checker", disableSafetyChecker)
                    }
                    "black-forest-labs/flux-dev", "black-forest-labs/flux-dev-lora", "black-forest-labs/flux-schnell", "tencent/hunyuan-image-3", "qwen/qwen-image" -> {
                        val aspectRatio = when {
                            request.width == request.height -> "1:1"
                            request.width > request.height -> "16:9"
                            else -> "9:16"
                        }
                        put("aspect_ratio", aspectRatio)
                        put("num_inference_steps", request.steps)
                        put("guidance_scale", request.guidanceScale)
                        request.seed?.let { put("seed", it) }
                        put("disable_safety_checker", disableSafetyChecker)
                    }
                    "lucataco/ssd-1b", "adirik/realvisxl-v3.0-turbo", "playgroundai/playground-v2.5-1024px-aesthetic", "stability-ai/sdxl" -> {
                        put("width", request.width)
                        put("height", request.height)
                        put("num_inference_steps", request.steps)
                        put("guidance_scale", request.guidanceScale)
                        request.seed?.let { put("seed", it) }
                        request.negativePrompt?.let { put("negative_prompt", it) }
                        put("disable_safety_checker", disableSafetyChecker)
                    }
                    else -> {
                        put("width", request.width)
                        put("height", request.height)
                        put("num_inference_steps", request.steps)
                        put("guidance_scale", request.guidanceScale)
                        request.seed?.let { put("seed", it) }
                        request.negativePrompt?.let { put("negative_prompt", it) }
                        put("disable_safety_checker", disableSafetyChecker)
                    }
                }
            }
            
            // Step 4: Create prediction request
            val jsonBody = JSONObject().apply {
                put("version", versionHash)
                put("input", input)
            }
            
            Log.d(TAG, "Replicate API request JSON: ${jsonBody.toString()}")
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            
            // Step 5: Send prediction to API with correct authorization format
            val httpRequest = Request.Builder()
                .url("https://api.replicate.com/v1/predictions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                val errorMessage = when (response.code) {
                    401 -> "Invalid Replicate API key"
                    402 -> "Insufficient Replicate credits"
                    404 -> "Model not found: ${request.model}"
                    422 -> "Invalid parameters: $errorBody"
                    429 -> "Rate limit exceeded"
                    else -> "Replicate error (${response.code}): $errorBody"
                }
                throw Exception(errorMessage)
            }
            
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val responseJson = JSONObject(responseBody)
            val predictionId = responseJson.optString("id")
            
            if (predictionId.isBlank()) {
                throw Exception("No prediction ID returned")
            }
            
            predictionId
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Replicate prediction", e)
            throw e
        }
    }
    
    private suspend fun pollReplicatePrediction(apiKey: String, predictionId: String): String? {
        val maxAttempts = 60
        var attempts = 0
        
        while (attempts < maxAttempts) {
            try {
                val request = Request.Builder()
                    .url("https://api.replicate.com/v1/predictions/$predictionId")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val status = json.optString("status")
                        Log.d(TAG, "Replicate poll response: $responseBody")
                        
                        when (status) {
                            "succeeded" -> {
                                val output = json.opt("output")
                                val imageUrl = when (output) {
                                    is JSONArray -> if (output.length() > 0) output.optString(0) else null
                                    is String -> output
                                    else -> null
                                }
                                
                                if (imageUrl.isNullOrBlank()) {
                                    throw Exception("No image URL in response")
                                }
                                
                                return imageUrl
                            }
                            "failed" -> {
                                val error = json.optString("error", "Prediction failed")
                                throw Exception("Prediction failed: $error")
                            }
                            "canceled" -> throw Exception("Prediction canceled")
                        }
                    }
                } else if (response.code == 401) {
                    throw Exception("Invalid API key")
                }
                
                attempts++
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(5000)
                }
                
            } catch (e: Exception) {
                if (e.message?.contains("failed") == true || e.message?.contains("canceled") == true || e.message?.contains("Invalid") == true) {
                    throw e
                }
                attempts++
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(5000)
                }
            }
        }
        
        throw Exception("Prediction timed out")
    }
    
    private suspend fun generateWithQwenImageEdit(
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Qwen image editing - Prompt: ${request.prompt.take(50)}..., HasInitImage: ${request.initImageBase64 != null}")
            
            if (request.initImageBase64.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Qwen image editing requires an input image"))
            }
            
            val editingRequest = ImageEditingRequest(
                imageBase64 = request.initImageBase64,
                prompt = request.prompt,
                goFast = true,
                outputFormat = "webp",
                enhancePrompt = false,
                outputQuality = 80
            )
            
            val result = imageEditingService.editImage(
                provider = "Replicate",
                apiKey = apiKey,
                request = editingRequest
            )
            
            return@withContext result.fold(
                onSuccess = { editingResult ->
                    Result.success(
                        ImageGenerationResult(
                            success = true,
                            imageUrl = editingResult.imageUrl,
                            imageBase64 = null,
                            generationTime = editingResult.generationTime,
                            model = request.model
                        )
                    )
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error with Qwen image editing", e)
            Result.failure(e)
        }
    }
    
    private suspend fun generateWithGrok(
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            // Use the model from request or default to grok-2-image-1212
            val modelToUse = if (request.model.startsWith("grok-")) {
                request.model
            } else {
                "grok-2-image-1212"
            }
            
            val jsonBody = JSONObject().apply {
                put("model", modelToUse)
                put("prompt", request.prompt)
                put("n", minOf(request.samples ?: 1, 10)) // Support 1-10 images as per API spec
                put("response_format", "url") // Default to URL format, can be "url" or "b64_json"
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("https://api.x.ai/v1/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = client.newCall(httpRequest).execute()
            val generationTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                val errorMessage = when (response.code) {
                    401 -> "Invalid Grok API key"
                    429 -> "Grok rate limit exceeded"
                    else -> "Grok API error: ${response.code} - $errorBody"
                }
                return@withContext Result.failure(Exception(errorMessage))
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response from Grok"))
            
            val responseJson = JSONObject(responseBody)
            val dataArray = responseJson.optJSONArray("data")
            
            if (dataArray != null && dataArray.length() > 0) {
                // Handle multiple images if n > 1
                val imageData = dataArray.getJSONObject(0) // Take first image for now
                val imageUrl = imageData.optString("url")
                val imageBase64 = imageData.optString("b64_json")
                
                if (imageUrl.isBlank() && imageBase64.isBlank()) {
                    return@withContext Result.failure(Exception("No image data in Grok response"))
                }
                
                Result.success(
                    ImageGenerationResult(
                        success = true,
                        imageUrl = imageUrl.takeIf { it.isNotBlank() },
                        imageBase64 = imageBase64.takeIf { it.isNotBlank() },
                        generationTime = generationTime,
                        model = modelToUse
                    )
                )
            } else {
                Result.failure(Exception("No image data in Grok response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with Grok image generation", e)
            Result.failure(Exception("Grok image generation failed: ${e.message}"))
        }
    }
    
    private suspend fun generateWithFalAI(
        apiKey: String,
        request: ImageGenerationRequest
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "fal.ai image generation - Model: ${request.model}, Prompt: ${request.prompt.take(50)}...")
            
            val startTime = System.currentTimeMillis()
            
            // Step 1: Submit generation request
            val requestId = submitFalAIRequest(apiKey, request)
            
            // Step 2: Poll for completion
            val imageUrl = pollFalAIRequest(apiKey, requestId)
            
            val generationTime = System.currentTimeMillis() - startTime
            
            Result.success(
                ImageGenerationResult(
                    success = true,
                    imageUrl = imageUrl,
                    imageBase64 = null,
                    generationTime = generationTime,
                    model = request.model
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error with fal.ai image generation", e)
            Result.failure(Exception("fal.ai image generation failed: ${e.message}"))
        }
    }
    
    private suspend fun submitFalAIRequest(apiKey: String, request: ImageGenerationRequest): String {
        val jsonBody = JSONObject().apply {
            put("prompt", request.prompt)
            put("image_size", when {
                request.width == 1024 && request.height == 1024 -> "square_hd"
                request.width > request.height -> "landscape_16_9"
                request.width < request.height -> "portrait_16_9"
                else -> "square"
            })
            put("num_inference_steps", request.steps)
            put("guidance_scale", request.guidanceScale)
            put("num_images", 1)
            put("enable_safety_checker", false)
            request.seed?.let { put("seed", it) }
        }
        
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        
        val httpRequest = Request.Builder()
            .url("https://queue.fal.run/${request.model}")
            .addHeader("Authorization", "Key $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val response = client.newCall(httpRequest).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("fal.ai API error (${response.code}): $errorBody")
        }
        
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val responseJson = JSONObject(responseBody)
        return responseJson.optString("request_id") ?: throw Exception("No request ID returned")
    }
    
    private suspend fun pollFalAIRequest(apiKey: String, requestId: String): String {
        val maxAttempts = 60
        var attempts = 0
        
        while (attempts < maxAttempts) {
            val request = Request.Builder()
                .url("https://queue.fal.run/requests/$requestId/status")
                .addHeader("Authorization", "Key $apiKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    val status = json.optString("status")
                    
                    when (status) {
                        "COMPLETED" -> {
                            val images = json.optJSONArray("images")
                            if (images != null && images.length() > 0) {
                                val imageObj = images.getJSONObject(0)
                                val imageUrl = imageObj.optString("url")
                                if (imageUrl.isNotBlank()) {
                                    return imageUrl
                                }
                            }
                            throw Exception("No image URL in response")
                        }
                        "FAILED" -> {
                            val error = json.optString("error", "Generation failed")
                            throw Exception("fal.ai generation failed: $error")
                        }
                    }
                }
            }
            
            attempts++
            if (attempts < maxAttempts) {
                kotlinx.coroutines.delay(2000) // Poll every 2 seconds
            }
        }
        
        throw Exception("fal.ai generation timed out")
    }
    
    suspend fun fetchComfyUiModels(endpoint: String): Result<Pair<List<String>, List<String>>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpoint.removeSuffix("/")
            val models = mutableListOf<String>()
            val loraModels = mutableListOf<String>()
            
            // Fetch regular models
            val modelsResult = fetchComfyUIModels(cleanUrl, "")
            modelsResult.fold(
                onSuccess = { modelList -> models.addAll(modelList) },
                onFailure = { /* Continue with empty models list */ }
            )
            
            // Fetch LoRA models
            val loraResult = fetchComfyUILoras(cleanUrl, "", emptyList())
            loraResult.fold(
                onSuccess = { loraList -> loraModels.addAll(loraList) },
                onFailure = { /* Continue with empty LoRA list */ }
            )
            
            Result.success(Pair(models, loraModels))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ComfyUI models and LoRAs", e)
            Result.failure(e)
        }
    }

} 
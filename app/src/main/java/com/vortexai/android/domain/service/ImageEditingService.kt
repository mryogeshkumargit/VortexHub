package com.vortexai.android.domain.service

import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import com.vortexai.android.domain.service.together.TogetherApi
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

data class ImageEditingRequest(
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val prompt: String,
    val goFast: Boolean = true,
    val outputFormat: String = "webp",
    val enhancePrompt: Boolean = false,
    val outputQuality: Int = 80,
    val checkpointOverride: String? = null
)

data class ImageEditingResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val error: String? = null,
    val generationTime: Long = 0
)

@Singleton
class ImageEditingService @Inject constructor(
    private val injectedClient: OkHttpClient,
    private val togetherApi: TogetherApi,
    private val dataStore: DataStore<Preferences>,
    private val logger: GenerationLogger
) {
    
    companion object {
        private const val TAG = "ImageEditingService"
        private const val REPLICATE_BASE_URL = "https://api.replicate.com/v1"
    }
    
    private val client = injectedClient.newBuilder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    suspend fun editImage(
        provider: String,
        apiKey: String,
        request: ImageEditingRequest,
        model: String? = null,
        strength: Float = 0.5f
    ): Result<ImageEditingResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image editing with provider: $provider, model: $model")
            
            // Log the incoming request immediately so it shows in the debug window even if it fails
            val endpointDisplay = if (provider.equals("ComfyUI", ignoreCase = true)) apiKey else "Standard API"
            logger.logRequest(
                provider = provider,
                endpoint = endpointDisplay,
                modelOrWorkflow = model ?: "Default Model",
                character = null,
                requestData = "Prompt: ${request.prompt}\nBase64 Image Data Length: ${request.imageBase64?.length ?: 0}\nStrength: $strength"
            )
            
            if (apiKey.isBlank()) {
                logger.logError(provider, "API key or endpoint is empty", "Endpoint: $endpointDisplay, Model: ${model ?: "Default"}")
                return@withContext Result.failure(Exception("API key or endpoint is required for $provider"))
            }
            
            val startTime = System.currentTimeMillis()
            
            val imageUrl = when (provider.lowercase()) {
                "modelslab" -> {
                    Log.d(TAG, "Using Modelslab v7 API")
                    editImageWithModelslab(apiKey, request, model, strength)
                }
                "together ai" -> {
                    val editModel = model ?: "black-forest-labs/FLUX.1-kontext-dev"
                    Log.d(TAG, "Using Together AI with model: $editModel")
                    
                    // Extract original image dimensions and resize if needed
                    val (originalWidth, originalHeight) = extractImageDimensions(request.imageBase64) ?: Pair(1024, 1024)
                    val (finalWidth, finalHeight) = validateDimensions(originalWidth, originalHeight)
                    
                    // Resize the actual image if dimensions changed
                    val resizedImageBase64 = if (originalWidth != finalWidth || originalHeight != finalHeight) {
                        resizeImageBase64(request.imageBase64, finalWidth, finalHeight) ?: request.imageBase64
                    } else {
                        request.imageBase64
                    }
                    
                    Log.d(TAG, "Original dimensions: ${originalWidth}x${originalHeight}, Final dimensions: ${finalWidth}x${finalHeight}")
                    
                    val result = togetherApi.editImage(
                        apiKey = apiKey,
                        prompt = request.prompt,
                        imageUrl = request.imageUrl,
                        imageBase64 = resizedImageBase64,
                        model = editModel,
                        strength = strength,
                        width = finalWidth,
                        height = finalHeight
                    )
                    
                    if (result.isSuccess) {
                        result.getOrNull()
                    } else {
                        return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Together AI editing failed"))
                    }
                }
                "replicate" -> {
                    val predictionId = createEditingPrediction(apiKey, request, model)
                        ?: return@withContext Result.failure(Exception("Failed to create editing prediction"))
                    
                    Log.d(TAG, "Created editing prediction: $predictionId")
                    
                    pollEditingPrediction(apiKey, predictionId)
                        ?: return@withContext Result.failure(Exception("Failed to get edited image"))
                }
                "comfyui" -> {
                    Log.d(TAG, "Using ComfyUI API for image editing")
                    val workflowJson = model // We pass the JSON string in the model parameter for ComfyUI
                    if (workflowJson.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("ComfyUI requires a workflow JSON (passed via model config)"))
                    }
                    val comfyEndpoint = apiKey // We pass the endpoint in the apiKey parameter for ComfyUI
                    if (comfyEndpoint.isBlank()) {
                        return@withContext Result.failure(Exception("ComfyUI requires an endpoint URL (passed via apiKey config)"))
                    }
                    
                    editImageWithComfyUI(comfyEndpoint, request, workflowJson)
                        ?: return@withContext Result.failure(Exception("Failed to get edited image from ComfyUI"))
                }
                else -> {
                    return@withContext Result.failure(Exception("Unsupported provider: $provider"))
                }
            }
            
            val generationTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Image editing successful - URL: $imageUrl")
            
            Result.success(
                ImageEditingResult(
                    success = true,
                    imageUrl = imageUrl,
                    generationTime = generationTime
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error editing image", e)
            Result.failure(e)
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
        val request = Request.Builder()
            .url("$REPLICATE_BASE_URL/models/$modelIdentifier")
            .addHeader("Authorization", "Token $apiKey")
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch model version: ${response.code}")
        }
        
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(responseBody)
        val versionId = json.optJSONObject("latest_version")?.optString("id")
            ?: throw Exception("No version ID found")
        
        return versionId
    }
    
    private suspend fun createEditingPrediction(
        apiKey: String,
        request: ImageEditingRequest,
        model: String? = null
    ): String? {
        return try {
            val modelIdentifier = model ?: "qwen/qwen-image-edit"
            
            val versionHash = fetchReplicateModelVersion(apiKey, modelIdentifier)
            Log.d(TAG, "Fetched version for $modelIdentifier: $versionHash")
            
            val imageInput = when {
                !request.imageUrl.isNullOrBlank() -> request.imageUrl
                !request.imageBase64.isNullOrBlank() -> {
                    if (request.imageBase64.startsWith("data:image")) {
                        request.imageBase64
                    } else {
                        "data:image/jpeg;base64,${request.imageBase64}"
                    }
                }
                else -> throw Exception("Either imageUrl or imageBase64 is required")
            }
            
            val input = when (model) {
                "seedream-4" -> {
                    JSONObject().apply {
                        put("prompt", request.prompt)
                        put("image_input", JSONArray().put(imageInput))
                        put("size", "2K")
                        put("aspect_ratio", "match_input_image")
                        put("sequential_image_generation", "disabled")
                        put("enhance_prompt", request.enhancePrompt)
                    }
                }
                "flux.1-dev", "flux.1-schnell" -> {
                    JSONObject().apply {
                        put("prompt", request.prompt)
                        put("image", imageInput)
                        put("num_inference_steps", 28)
                        put("guidance_scale", 3.5f)
                        val disableSafetyChecker = getDisableSafetyCheckerPreference()
                        put("disable_safety_checker", disableSafetyChecker)
                    }
                }
                "sdxl" -> {
                    JSONObject().apply {
                        put("prompt", request.prompt)
                        put("image", imageInput)
                        put("num_inference_steps", 25)
                        put("guidance_scale", 7.5f)
                        put("strength", 0.8f)
                        val disableSafetyChecker = getDisableSafetyCheckerPreference()
                        put("disable_safety_checker", disableSafetyChecker)
                    }
                }
                "instruct-pix2pix" -> {
                    JSONObject().apply {
                        put("prompt", request.prompt)
                        put("image", imageInput)
                        put("num_inference_steps", 20)
                        put("guidance_scale", 7.5f)
                        put("image_guidance_scale", 1.5f)
                        val disableSafetyChecker = getDisableSafetyCheckerPreference()
                        put("disable_safety_checker", disableSafetyChecker)
                    }
                }
                else -> {
                    JSONObject().apply {
                        put("image", imageInput)
                        put("prompt", request.prompt)
                        put("go_fast", request.goFast)
                        put("output_format", request.outputFormat)
                        put("enhance_prompt", request.enhancePrompt)
                        put("output_quality", request.outputQuality)
                    }
                }
            }
            
            val jsonBody = JSONObject().apply {
                put("version", versionHash)
                put("input", input)
            }
            
            Log.d(TAG, "Replicate editing request: ${jsonBody.toString()}")
            
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$REPLICATE_BASE_URL/predictions")
                .addHeader("Authorization", "Token $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            Log.d(TAG, "Replicate API response code: ${response.code}")
            
            val responseBodyString = response.body?.string()
            if (!response.isSuccessful) {
                val errorBody = responseBodyString ?: "Unknown error"
                Log.e(TAG, "Replicate API error: ${response.code} - ${errorBody.take(200)}...")
                
                val errorMessage = when (response.code) {
                    401 -> "Invalid Replicate API key"
                    402 -> "Insufficient credits on Replicate account"
                    422 -> "Invalid input parameters for Replicate image editing"
                    429 -> "Rate limit exceeded for Replicate API"
                    500, 502, 503, 504 -> "Replicate server error"
                    else -> "Replicate API error (${response.code}): $errorBody"
                }
                
                throw Exception(errorMessage)
            }
            
            if (responseBodyString == null) {
                Log.e(TAG, "Empty response body from Replicate API")
                throw Exception("Empty response from Replicate API")
            }
            
            Log.d(TAG, "Replicate prediction response: ${responseBodyString.take(200)}...")
            
            val responseJson = JSONObject(responseBodyString)
            val predictionId = responseJson.optString("id")
            
            if (predictionId.isBlank()) {
                Log.e(TAG, "No prediction ID in Qwen response")
                val error = responseJson.optString("error", "Unknown error")
                throw Exception("No prediction ID returned. Error: $error")
            }
            
            predictionId
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Replicate prediction", e)
            throw e
        }
    }
    
    private suspend fun pollEditingPrediction(apiKey: String, predictionId: String): String? {
        val maxAttempts = 60 // 5 minutes max
        var attempts = 0
        
        Log.d(TAG, "Polling Replicate prediction: $predictionId")
        
        while (attempts < maxAttempts) {
            try {
                val request = Request.Builder()
                    .url("$REPLICATE_BASE_URL/predictions/$predictionId")
                    .addHeader("Authorization", "Token $apiKey")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                Log.d(TAG, "Poll attempt ${attempts + 1}/$maxAttempts, response: ${response.code}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val status = json.optString("status")
                        
                        Log.d(TAG, "Prediction status: $status")
                        
                        when (status) {
                            "succeeded" -> {
                                val output = json.opt("output")
                                val imageUrl = when (output) {
                                    is org.json.JSONArray -> output.optString(0)
                                    is String -> output
                                    else -> null
                                }
                                
                                if (imageUrl.isNullOrBlank()) {
                                    throw Exception("No image URL in successful prediction")
                                }
                                
                                Log.d(TAG, "Successfully got edited image URL: $imageUrl")
                                return imageUrl
                            }
                            "failed" -> {
                                val error = json.optString("error", "Prediction failed")
                                Log.e(TAG, "Replicate prediction failed: $error")
                                throw Exception("Image editing failed: $error")
                            }
                            "canceled" -> {
                                throw Exception("Image editing was canceled")
                            }
                            "processing", "starting" -> {
                                Log.d(TAG, "Replicate prediction status: $status, continuing...")
                            }
                        }
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Replicate poll error: ${response.code} - $errorBody")
                    
                    if (response.code == 401) {
                        throw Exception("Invalid Replicate API key")
                    } else if (response.code == 404) {
                        throw Exception("Prediction not found")
                    }
                }
                
                attempts++
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(5000)
                }
                
            } catch (e: Exception) {
                if (e.message?.contains("failed") == true || 
                    e.message?.contains("canceled") == true ||
                    e.message?.contains("Invalid") == true) {
                    throw e
                }
                
                Log.e(TAG, "Error polling prediction (attempt ${attempts + 1})", e)
                attempts++
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(5000)
                }
            }
        }
        
        throw Exception("Image editing timed out after ${maxAttempts * 5} seconds")
    }
    
    
    private suspend fun editImageWithModelslab(
        apiKey: String,
        request: ImageEditingRequest,
        model: String?,
        strength: Float = 0.7f
    ): String? {
        return try {
            Log.d(TAG, "Starting Modelslab v7 image-to-image")
            
            val modelId = model ?: "flux-kontext-dev"
            
            // Get image URL - upload to imgbb if needed
            val initImageUrl = when {
                !request.imageUrl.isNullOrBlank() -> request.imageUrl
                !request.imageBase64.isNullOrBlank() -> {
                    Log.d(TAG, "Uploading image to imgbb...")
                    val imageBytes = android.util.Base64.decode(request.imageBase64, android.util.Base64.DEFAULT)
                    uploadImageToImgbb(imageBytes)
                }
                else -> throw Exception("Either imageUrl or imageBase64 is required")
            }
            
            val (requestBody, apiUrl) = when (modelId) {
                "flux-kontext-pro" -> {
                    val aspectRatio = calculateAspectRatio(request.imageBase64)
                    val body = JSONObject().apply {
                        put("init_image", initImageUrl)
                        put("prompt", request.prompt)
                        put("model_id", modelId)
                        put("aspect_ratio", aspectRatio)
                        put("key", apiKey)
                    }
                    Pair(body, "https://modelslab.com/api/v7/images/image-to-image")
                }
                "seedream-4" -> {
                    val body = JSONObject().apply {
                        put("init_image", org.json.JSONArray().put(initImageUrl))
                        put("prompt", request.prompt)
                        put("model_id", modelId)
                        put("key", apiKey)
                    }
                    Pair(body, "https://modelslab.com/api/v7/images/image-to-image")
                }
                "nano-banana" -> {
                    val body = JSONObject().apply {
                        put("prompt", request.prompt)
                        put("model_id", modelId)
                        put("init_image", initImageUrl)
                        put("key", apiKey)
                    }
                    Pair(body, "https://modelslab.com/api/v7/images/image-to-image")
                }
                else -> { // flux-kontext-dev
                    val body = JSONObject().apply {
                        put("init_image", initImageUrl)
                        put("init_image_2", "")
                        put("prompt", request.prompt)
                        put("negative_prompt", getNegativePrompt())
                        put("model_id", modelId)
                        put("num_inference_steps", "28")
                        put("strength", strength.toString())
                        put("scheduler", "DPMSolverMultistepScheduler")
                        put("guidance", "2.5")
                        put("enhance_prompt", false)
                        put("base64", "no")
                        put("key", apiKey)
                    }
                    Pair(body, "https://modelslab.com/api/v6/images/img2img")
                }
            }
            
            Log.d(TAG, "Modelslab v7 request prepared with model: $modelId")
            
            val httpRequest = Request.Builder()
                .url(apiUrl)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            Log.d(TAG, "Modelslab API response code: ${response.code}")
            
            val responseBodyString = response.body?.string()
            if (!response.isSuccessful) {
                val errorBody = responseBodyString ?: "Unknown error"
                Log.e(TAG, "Modelslab API error: ${response.code} - ${errorBody.take(200)}...")
                
                val errorMessage = when (response.code) {
                    401 -> "Invalid Modelslab API key"
                    402 -> "Insufficient credits on Modelslab account"
                    422 -> "Invalid input parameters for Modelslab"
                    429 -> "Rate limit exceeded for Modelslab API"
                    500, 502, 503, 504 -> "Modelslab server error"
                    else -> "Modelslab API error (${response.code}): $errorBody"
                }
                
                throw Exception(errorMessage)
            }
            
            if (responseBodyString == null) {
                Log.e(TAG, "Empty response body from Modelslab API")
                throw Exception("Empty response from Modelslab API")
            }
            
            Log.d(TAG, "Modelslab response: ${responseBodyString.take(200)}...")
            
            val responseJson = JSONObject(responseBodyString)
            
            // Handle both async and sync responses
            handleModelsLabResponse(responseJson, apiKey)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error with Modelslab image editing", e)
            throw e
        }
    }
    
    private suspend fun handleModelsLabResponse(responseJson: JSONObject, apiKey: String): String {
        val status = responseJson.optString("status")
        
        // Check for error status first
        if (status == "error" || status == "failed") {
            val errorMsg = responseJson.optString("message", "Unknown error")
            throw Exception("ModelsLab API error: $errorMsg")
        }
        
        // Check if response is async (requires polling)
        val isAsync = status != "success" && (responseJson.has("fetch_result") || responseJson.has("id"))
        
        return if (isAsync) {
            Log.d(TAG, "Async response detected (status: $status), starting polling...")
            pollModelsLabResult(responseJson, apiKey)
        } else {
            Log.d(TAG, "Immediate response detected (status: $status)")
            val imageUrl = extractImageUrl(responseJson)
            if (imageUrl.isNullOrBlank()) {
                Log.e(TAG, "Response JSON: ${responseJson.toString()}")
                throw Exception("No image URL found in response. Status: $status")
            }
            imageUrl
        }
    }
    
    private suspend fun pollModelsLabResult(initialResponse: JSONObject, apiKey: String): String {
        val taskId = initialResponse.optString("id")
        val fetchUrl = initialResponse.optString("fetch_result")
        val estimatedTime = initialResponse.optInt("eta", 10)
        
        Log.d(TAG, "=== POLLING DIAGNOSTICS START ===")
        Log.d(TAG, "Initial response: ${initialResponse.toString()}")
        Log.d(TAG, "Task ID: $taskId")
        Log.d(TAG, "Fetch URL: $fetchUrl")
        Log.d(TAG, "Estimated time: ${estimatedTime}s")
        
        if (taskId.isEmpty() || fetchUrl.isEmpty()) {
            Log.e(TAG, "POLLING FAILED: Missing required fields - taskId: '$taskId', fetchUrl: '$fetchUrl'")
            throw Exception("Missing polling information in async response")
        }
        
        Log.d(TAG, "Starting polling for task: $taskId, estimated time: ${estimatedTime}s")
        
        // Wait initial estimated time (capped at 30 seconds)
        val waitTime = (estimatedTime * 1000L).coerceAtMost(30000L)
        Log.d(TAG, "Waiting initial ${waitTime}ms before first poll...")
        kotlinx.coroutines.delay(waitTime)
        
        val maxAttempts = 60 // 5 minutes max
        repeat(maxAttempts) { attempt ->
            try {
                Log.d(TAG, "--- Poll Attempt ${attempt + 1}/$maxAttempts ---")
                
                val pollData = JSONObject().apply { put("key", apiKey) }
                Log.d(TAG, "Poll request data: ${pollData.toString()}")
                Log.d(TAG, "Poll URL: $fetchUrl")
                
                val pollRequest = Request.Builder()
                    .url(fetchUrl)
                    .post(pollData.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val pollResponse = client.newCall(pollRequest).execute()
                
                Log.d(TAG, "Poll response code: ${pollResponse.code}")
                Log.d(TAG, "Poll response headers: ${pollResponse.headers}")
                
                if (pollResponse.isSuccessful) {
                    val responseBody = pollResponse.body?.string()
                    Log.d(TAG, "Poll response body: $responseBody")
                    
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val pollStatus = json.optString("status")
                        
                        Log.d(TAG, "Poll status: '$pollStatus'")
                        
                        when (pollStatus) {
                            "success" -> {
                                Log.d(TAG, "✅ POLLING SUCCESS: Task completed!")
                                val imageUrl = extractImageUrl(json)
                                if (imageUrl != null) {
                                    Log.d(TAG, "✅ FINAL SUCCESS: Image URL extracted: $imageUrl")
                                    return imageUrl
                                } else {
                                    Log.e(TAG, "❌ POLLING FAILED: No image URL in successful response")
                                    Log.e(TAG, "Success response JSON: ${json.toString()}")
                                    throw Exception("No image URL in poll result")
                                }
                            }
                            "processing", "queued", "starting" -> {
                                Log.d(TAG, "⏳ POLLING CONTINUE: Task status '$pollStatus', waiting...")
                            }
                            "failed", "error" -> {
                                val errorMsg = json.optString("message", "Task failed")
                                Log.e(TAG, "❌ POLLING FAILED: Task failed with status '$pollStatus'")
                                Log.e(TAG, "Error message: $errorMsg")
                                Log.e(TAG, "Error response JSON: ${json.toString()}")
                                throw Exception("Image editing failed: $errorMsg")
                            }
                            else -> {
                                Log.w(TAG, "⚠️ UNKNOWN STATUS: '$pollStatus' - treating as processing")
                                Log.w(TAG, "Unknown status response: ${json.toString()}")
                            }
                        }
                    } else {
                        Log.e(TAG, "❌ POLL ERROR: Empty response body")
                    }
                } else {
                    val errorBody = pollResponse.body?.string() ?: "No error body"
                    Log.e(TAG, "❌ POLL HTTP ERROR: ${pollResponse.code}")
                    Log.e(TAG, "Error body: $errorBody")
                }
                
                // Wait 5 seconds before next poll (except on last attempt)
                if (attempt < maxAttempts - 1) {
                    Log.d(TAG, "Waiting 5s before next poll...")
                    kotlinx.coroutines.delay(5000L)
                } else {
                    Log.e(TAG, "❌ FINAL ATTEMPT REACHED")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ POLL EXCEPTION on attempt ${attempt + 1}: ${e.message}")
                Log.e(TAG, "Exception details: ", e)
                
                if (attempt < maxAttempts - 1) {
                    Log.d(TAG, "Retrying after exception...")
                    kotlinx.coroutines.delay(5000L)
                } else {
                    Log.e(TAG, "❌ POLLING FAILED: Final attempt exception")
                    throw e
                }
            }
        }
        
        Log.e(TAG, "❌ POLLING TIMEOUT: Failed after ${maxAttempts * 5} seconds")
        Log.e(TAG, "=== POLLING DIAGNOSTICS END ===")
        throw Exception("Polling timed out after ${maxAttempts * 5} seconds")
    }
    
    // --- ComfyUI Integration --
    private suspend fun editImageWithComfyUI(
        endpoint: String,
        request: ImageEditingRequest,
        workflowJson: String
    ): String? {
        return try {
            val baseEndpoint = endpoint.trimEnd('/')
            Log.d(TAG, "Starting ComfyUI image-to-image to $baseEndpoint")
            
            // 1) Get image data
            val imageBytes = when {
                !request.imageBase64.isNullOrBlank() -> {
                    val base64Data = if (request.imageBase64.startsWith("data:image")) {
                        request.imageBase64.substringAfter(",")
                    } else {
                        request.imageBase64
                    }
                    android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                }
                else -> throw Exception("imageBase64 is required for ComfyUI Image Edit")
            }
            
            // 2) Upload image to ComfyUI /upload/image
            val filename = uploadImageToComfyUI(baseEndpoint, imageBytes)
            
            // 3) Parse and modify JSON workflow
            // Determine if workflowJson is the embedded fallback, a filename, or raw JSON
            val finalWorkflowJson = when {
                workflowJson == "Flux2-Klein Image Edit" -> {
                    """{"9":{"inputs":{"filename_prefix":"ComfyUI","images":["108",0]},"class_type":"SaveImage","_meta":{"title":"Save Image"}},"76":{"inputs":{"image":"vortex_avatar.png","upload":"image"},"class_type":"LoadImage","_meta":{"title":"Load Image"}},"107":{"inputs":{"noise_seed":12345678},"class_type":"RandomNoise","_meta":{"title":"RandomNoise"}},"108":{"inputs":{"samples":["111",0],"vae":["112",0]},"class_type":"VAEDecode","_meta":{"title":"VAE Decode"}},"109":{"inputs":{"text":"YOUR PROMPT HERE","clip":["114",1]},"class_type":"CLIPTextEncode","_meta":{"title":"CLIP Text Encode (Positive Prompt)"}},"110":{"inputs":{"text":"","clip":["114",1]},"class_type":"CLIPTextEncode","_meta":{"title":"CLIP Text Encode (Negative Prompt)"}},"111":{"inputs":{"noise":["107",0],"guider":["113",0],"sampler":["116",0],"sigmas":["117",0],"latent_image":["115",0]},"class_type":"SamplerCustomAdvanced","_meta":{"title":"SamplerCustomAdvanced"}},"112":{"inputs":{"vae_name":"ae.safetensors"},"class_type":"VAELoader","_meta":{"title":"Load VAE"}},"113":{"inputs":{"model":["114",0],"conditioning":["109",0]},"class_type":"BasicGuider","_meta":{"title":"BasicGuider"}},"114":{"inputs":{"ckpt_name":"flux1-dev-fp8.safetensors"},"class_type":"CheckpointLoaderSimple","_meta":{"title":"Load Checkpoint"}},"115":{"inputs":{"pixels":["76",0],"vae":["112",0]},"class_type":"VAEEncode","_meta":{"title":"VAE Encode"}},"116":{"inputs":{"sampler_name":"euler"},"class_type":"KSamplerSelect","_meta":{"title":"KSamplerSelect"}},"117":{"inputs":{"scheduler":"simple","steps":20,"denoise":0.8,"model":["114",0]},"class_type":"BasicScheduler","_meta":{"title":"BasicScheduler"}}}"""
                }
                workflowJson.endsWith(".json", ignoreCase = true) -> {
                    // Because we cannot reliably inject context into the singleton ImageEditingService
                    // we must rely on ChatImageGenerator injecting the raw JSON payload when calling editImage
                    // NOTE: this branch should never execute if the generator logic correctly reads the file.
                    throw Exception("ComfyUI workflow file reading must happen in the Generator UI layer.")
                }
                else -> workflowJson // Assume raw JSON was pasted
            }
            
            // 3) Parse JSON into V2 Canonical Graph
            val graph = com.vortexai.android.domain.comfy.v2.ComfyGraphBuilder.parse(finalWorkflowJson)
            
            // 4) Infer Semantic Roles
            com.vortexai.android.domain.comfy.v2.SemanticRoleInferencer.inferRoles(graph)
            
            // 5) Apply Safe Transformations
            com.vortexai.android.domain.comfy.v2.GraphTransformationEngine.injectPrompt(graph, request.prompt)
            com.vortexai.android.domain.comfy.v2.GraphTransformationEngine.injectImage(graph, filename)
            val seedToInject = kotlin.random.Random.nextLong(0, Long.MAX_VALUE)
            com.vortexai.android.domain.comfy.v2.GraphTransformationEngine.injectSeed(graph, seedToInject)
            
            request.checkpointOverride?.let { ckptName ->
                com.vortexai.android.domain.comfy.v2.GraphTransformationEngine.injectCheckpoint(graph, ckptName)
            }
            
            // 6) Validate Graph Structurally
            val validation = com.vortexai.android.domain.comfy.v2.GraphValidator.validate(graph)
            if (!validation.isValid) {
                throw Exception("ComfyUI Graph Validation Failed: \n" + validation.reasons.joinToString("\n"))
            }

            // Compile back to JSON string for logging
            val compiledJson = com.vortexai.android.domain.comfy.v2.ComfyGraphBuilder.compileToJson(graph)

            // Log Request with mutated payload
            logger.logRequest(
                provider = "ComfyUI",
                endpoint = baseEndpoint,
                modelOrWorkflow = "Dynamic ComfyUI V2 Compiler Execution",
                character = null,
                requestData = compiledJson
            )
            
            Log.d(TAG, "Execution Pipeline Validation Passed. Dispatching via Provider...")
            
            // 7) Execute via Strategy pattern
            com.vortexai.android.domain.comfy.provider.LocalComfyProvider(client, baseEndpoint).executeGeneration(graph)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in ComfyUI Image Editing", e)
            throw e
        }
    }
    
    // Abstracted to LocalComfyProvider and ComfyUIProvider
    private suspend fun uploadImageToComfyUI(endpoint: String, imageBytes: ByteArray): String {
        return com.vortexai.android.domain.comfy.provider.LocalComfyProvider(client, endpoint).uploadImage(imageBytes)
    }
    // ----------------------------
    
    private fun extractImageUrl(responseJson: JSONObject): String? {
        return try {
            Log.d(TAG, "Extracting image URL from response: ${responseJson.toString()}")
            
            // Try different response formats in order of priority
            
            // 1. Check output array (most common)
            if (responseJson.has("output")) {
                val output = responseJson.get("output")
                when (output) {
                    is org.json.JSONArray -> {
                        if (output.length() > 0) {
                            val url = output.optString(0)
                            if (url.isNotEmpty()) {
                                Log.d(TAG, "Found image URL in output array: $url")
                                return url
                            }
                        }
                    }
                    is String -> {
                        if (output.isNotEmpty()) {
                            Log.d(TAG, "Found image URL in output string: $output")
                            return output
                        }
                    }
                }
            }
            
            // 2. Check direct image field
            if (responseJson.has("image")) {
                val imageUrl = responseJson.optString("image")
                if (imageUrl.isNotEmpty()) {
                    Log.d(TAG, "Found image URL in image field: $imageUrl")
                    return imageUrl
                }
            }
            
            // 3. Check future_links as backup
            if (responseJson.has("future_links")) {
                val futureLinks = responseJson.optJSONArray("future_links")
                if (futureLinks != null && futureLinks.length() > 0) {
                    val url = futureLinks.optString(0)
                    if (url.isNotEmpty()) {
                        Log.d(TAG, "Found image URL in future_links: $url")
                        return url
                    }
                }
            }
            
            // 4. Check proxy_links
            if (responseJson.has("proxy_links")) {
                val proxyLinks = responseJson.optJSONArray("proxy_links")
                if (proxyLinks != null && proxyLinks.length() > 0) {
                    val url = proxyLinks.optString(0)
                    if (url.isNotEmpty()) {
                        Log.d(TAG, "Found image URL in proxy_links: $url")
                        return url
                    }
                }
            }
            
            Log.w(TAG, "No image URL found in any expected field")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting image URL: ${e.message}")
            null
        }
    }
    
    private suspend fun uploadImageToImgbb(imageBytes: ByteArray): String {
        return try {
            val imgbbApiKey = getImgbbApiKey() ?: throw Exception("imgbb API key not configured")
            
            // Convert image bytes to base64 for imgbb upload
            val base64Data = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            
            val uploadUrl = "https://api.imgbb.com/1/upload"
            val formBody = FormBody.Builder()
                .add("key", imgbbApiKey)
                .add("image", base64Data)
                .add("expiration", "600")
                .build()
            
            val uploadRequest = Request.Builder()
                .url(uploadUrl)
                .post(formBody)
                .build()
            
            val uploadResponse = client.newCall(uploadRequest).execute()
            
            if (!uploadResponse.isSuccessful) {
                throw Exception("Failed to upload image to imgbb: ${uploadResponse.code}")
            }
            
            val responseBody = uploadResponse.body?.string()
                ?: throw Exception("Empty response from imgbb")
            
            val responseJson = JSONObject(responseBody)
            
            if (!responseJson.optBoolean("success", false)) {
                val errorMsg = responseJson.optJSONObject("error")?.optString("message", "Unknown error")
                throw Exception("imgbb upload failed: $errorMsg")
            }
            
            val imageUrl = responseJson.optJSONObject("data")?.optString("url")
                ?: throw Exception("No image URL returned from imgbb")
            
            Log.d(TAG, "Successfully uploaded image to imgbb: $imageUrl")
            imageUrl
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to imgbb", e)
            throw Exception("Failed to upload image: ${e.message}")
        }
    }
    
    private suspend fun getImgbbApiKey(): String? {
        return try {
            val preferences = dataStore.data.first()
            preferences[stringPreferencesKey("imgbb_api_key")]
        } catch (e: Exception) {
            Log.e(TAG, "Error getting imgbb API key", e)
            null
        }
    }
    
    private suspend fun getNegativePrompt(): String {
        return try {
            val preferences = dataStore.data.first()
            preferences[stringPreferencesKey("modelslab_negative_prompt")] ?: "(worst quality:2), (low quality:2), (normal quality:2), (jpeg artifacts), (blurry), (duplicate), (morbid), (mutilated), (out of frame), (extra limbs), (bad anatomy), (disfigured), (deformed), (cross-eye), (glitch), (oversaturated), (overexposed), (underexposed), (bad proportions), (bad hands), (bad feet), (cloned face), (long neck), (missing arms), (missing legs), (extra fingers), (fused fingers), (poorly drawn hands), (poorly drawn face), (mutation), (deformed eyes), watermark, text, logo, signature, grainy, tiling, censored, nsfw, ugly, blurry eyes, noisy image, bad lighting, unnatural skin, asymmetry"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting negative prompt", e)
            "(worst quality:2), (low quality:2), (normal quality:2), (jpeg artifacts), (blurry)"
        }
    }
    
    private fun extractImageDimensions(imageBase64: String?): Pair<Int, Int>? {
        if (imageBase64.isNullOrBlank()) return null
        
        return try {
            val base64Data = if (imageBase64.startsWith("data:image")) {
                imageBase64.substringAfter(",")
            } else {
                imageBase64
            }
            
            val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            if (bitmap != null) {
                val width = bitmap.width
                val height = bitmap.height
                bitmap.recycle()
                Log.d(TAG, "Extracted image dimensions: ${width}x${height}")
                Pair(width, height)
            } else {
                Log.w(TAG, "Failed to decode image for dimension extraction")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting image dimensions", e)
            null
        }
    }
    
    private fun validateDimensions(width: Int, height: Int): Pair<Int, Int> {
        val maxDim = 1500
        
        // Scale down if needed
        val (scaledWidth, scaledHeight) = if (width <= maxDim && height <= maxDim) {
            Pair(width, height)
        } else {
            val aspectRatio = width.toFloat() / height.toFloat()
            if (width > height) {
                val newHeight = (maxDim / aspectRatio).toInt()
                Pair(maxDim, newHeight)
            } else {
                val newWidth = (maxDim * aspectRatio).toInt()
                Pair(newWidth, maxDim)
            }
        }
        
        // Round to nearest multiple of 8
        val finalWidth = (scaledWidth / 8) * 8
        val finalHeight = (scaledHeight / 8) * 8
        
        return Pair(finalWidth, finalHeight)
    }
    
    private fun calculateAspectRatio(imageBase64: String?): String {
        val dimensions = extractImageDimensions(imageBase64)
        return if (dimensions != null) {
            val (width, height) = dimensions
            val gcd = gcd(width, height)
            val ratioWidth = width / gcd
            val ratioHeight = height / gcd
            "$ratioWidth:$ratioHeight"
        } else {
            "1:1" // Default aspect ratio
        }
    }
    
    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }
    
    private fun resizeImageBase64(imageBase64: String?, targetWidth: Int, targetHeight: Int): String? {
        if (imageBase64.isNullOrBlank()) return null
        
        return try {
            val base64Data = if (imageBase64.startsWith("data:image")) {
                imageBase64.substringAfter(",")
            } else {
                imageBase64
            }
            
            val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            if (originalBitmap != null) {
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                originalBitmap.recycle()
                
                val outputStream = java.io.ByteArrayOutputStream()
                resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                resizedBitmap.recycle()
                
                val resizedBytes = outputStream.toByteArray()
                val resizedBase64 = android.util.Base64.encodeToString(resizedBytes, android.util.Base64.NO_WRAP)
                
                Log.d(TAG, "Resized image from ${originalBitmap.width}x${originalBitmap.height} to ${targetWidth}x${targetHeight}")
                
                if (imageBase64.startsWith("data:image")) {
                    "data:image/jpeg;base64,$resizedBase64"
                } else {
                    resizedBase64
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing image", e)
            null
        }
    }
}
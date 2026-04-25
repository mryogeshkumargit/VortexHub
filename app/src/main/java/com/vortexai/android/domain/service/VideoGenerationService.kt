package com.vortexai.android.domain.service

import android.util.Log
import com.vortexai.android.domain.service.ModelsLabImageApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.vortexai.android.ui.screens.settings.managers.SettingsDataStore

data class VideoGenerationRequest(
    val prompt: String,
    val model: String,
    val imageUrl: String? = null,
    val initImageBase64: String? = null,
    val durationSeconds: Int = 3,
    val framesPerSecond: Int = 24,
    val aspectRatio: String = "16:9",
    val negativePrompt: String? = null
)

data class VideoGenerationResult(
    val success: Boolean,
    val videoUrl: String? = null,
    val error: String? = null,
    val generationTime: Long = 0,
    val model: String? = null,
    val providerId: String? = null,
    /** Poll ID returned by async APIs like Fal.ai or Replicate */
    val pollId: String? = null 
)

@Singleton
class VideoGenerationTracker @Inject constructor() {
    // In-memory tracking for generation. Could be backed by Room later.
    private val activeGenerations = mutableMapOf<String, VideoGenerationResult>()

    fun registerGeneration(id: String, result: VideoGenerationResult) {
        activeGenerations[id] = result
    }

    fun getGeneration(id: String): VideoGenerationResult? = activeGenerations[id]
    
    fun removeGeneration(id: String) {
        activeGenerations.remove(id)
    }
}

@Singleton
class VideoGenerationService @Inject constructor(
    private val tracker: VideoGenerationTracker,
    private val modelsLabApi: ModelsLabImageApi, // Reusing for generic client/endpoints where applicable
    private val settingsDataStore: SettingsDataStore,
    private val logger: GenerationLogger
) {
    companion object {
        private val TAG = VideoGenerationService::class.java.simpleName
        private const val REPLICATE_BASE_URL = "https://api.replicate.com/v1"
        private const val FAL_AI_BASE_URL = "https://queue.fal.run"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // Video takes longer
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateVideo(
        provider: String = "",
        apiKey: String = "",
        request: VideoGenerationRequest
    ): Result<VideoGenerationResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            val prefs = settingsDataStore.getPreferences()
            
            // Resolve Provider
            val actualProvider = provider.ifBlank {
                prefs[SettingsDataStore.VIDEO_PROVIDER_KEY] ?: "fal.ai"
            }.lowercase().replace(".", "_") // Normalize fal.ai to fal_ai for easy checking
            
            // Resolve API Key based on provider
            val actualApiKey = apiKey.ifBlank {
                when (actualProvider) {
                    "fal_ai" -> prefs[SettingsDataStore.FAL_AI_VIDEO_API_KEY] ?: ""
                    "replicate" -> prefs[SettingsDataStore.REPLICATE_VIDEO_API_KEY] ?: ""
                    "modelslab" -> prefs[SettingsDataStore.MODELSLAB_VIDEO_API_KEY] ?: ""
                    else -> ""
                }
            }
            
            // Resolve Model
            val actualModel = request.model.ifBlank {
                 when (actualProvider) {
                    "fal_ai" -> prefs[SettingsDataStore.FAL_AI_VIDEO_MODEL_KEY] ?: "fal-ai/kling-video/v1/standard/image-to-video"
                    "replicate" -> prefs[SettingsDataStore.REPLICATE_VIDEO_MODEL_KEY] ?: "stability-ai/stable-video-diffusion"
                    "modelslab" -> prefs[SettingsDataStore.MODELSLAB_VIDEO_MODEL_KEY] ?: "video/text2video"
                    else -> ""
                }
            }
            
            val finalizedRequest = request.copy(model = actualModel)

            Log.d(TAG, "Requesting video generation via $actualProvider. Model: ${finalizedRequest.model}")
            
            val initialResult = when (actualProvider) {
                "replicate" -> submitReplicateVideo(actualApiKey, finalizedRequest)
                "fal_ai" -> submitFalAiVideo(actualApiKey, finalizedRequest)
                "modelslab" -> {
                    // ModelsLab handles its own async polling internally via handleAsyncResponse
                    val res = modelsLabApi.generateVideo(apiKey, request)
                    if (res.isSuccess) {
                        Result.success(VideoGenerationResult(
                            success = true,
                            videoUrl = res.getOrNull(),
                            providerId = "modelslab",
                            model = request.model
                        ))
                    } else {
                        Result.failure(res.exceptionOrNull() ?: Exception("ModelsLab video generation failed"))
                    }
                }
                else -> Result.failure(Exception("Provider $provider does not currently support direct video generation through VideoGenerationService."))
            }
            
            if (initialResult.isSuccess) {
                val pollUrl = initialResult.getOrNull()?.pollId
                if (!pollUrl.isNullOrBlank()) {
                    Log.d(TAG, "Got poll URL: $pollUrl. Starting polling...")
                    val finalUrlResult = pollForVideoCompletion(actualProvider, actualApiKey, pollUrl)
                    if (finalUrlResult.isSuccess) {
                        Result.success(VideoGenerationResult(
                            success = true,
                            videoUrl = finalUrlResult.getOrNull(),
                            providerId = actualProvider,
                            model = finalizedRequest.model
                        ))
                    } else {
                        Result.failure(finalUrlResult.exceptionOrNull() ?: Exception("Polling failed with unknown error"))
                    }
                } else {
                    initialResult // Return as is (maybe it finished immediately)
                }
            } else {
                initialResult // Return failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed video generation request to $provider", e)
            Result.failure(e)
        }
    }

    private suspend fun submitReplicateVideo(apiKey: String, request: VideoGenerationRequest): Result<VideoGenerationResult> {
        // e.g., stability-ai/stable-video-diffusion or luma/ray
        val versionOrDeployment = if (request.model.contains("/")) request.model else "stability-ai/stable-video-diffusion"
        
        val inputBlock = JSONObject().apply {
            put("prompt", request.prompt)
            request.initImageBase64?.let { put("image", "data:image/jpeg;base64,$it") }
            request.negativePrompt?.let { put("negative_prompt", it) }
        }

        val jsonBody = JSONObject().apply {
            put("input", inputBlock)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        
        // Log Request
        val url = "$REPLICATE_BASE_URL/models/$versionOrDeployment/predictions"
        logger.logRequest("Replicate Video", url, request.model, null, jsonBody.toString())
        
        val req = Request.Builder()
            // Using generic models endpoint to trigger execution
            .url(url) 
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()
            
        val response = client.newCall(req).execute()
        val bodyStr = response.body?.string() ?: ""
        
        if (!response.isSuccessful) {
            Log.e(TAG, "Replicate error ${response.code}: $bodyStr")
            logger.logError("Replicate Video", "HTTP ${response.code}", bodyStr)
            return Result.failure(Exception("Replicate API error: ${response.code}"))
        }
        
        logger.logResponse("Replicate Video", bodyStr)
        
        val json = JSONObject(bodyStr)
        val pollUrl = json.optJSONObject("urls")?.optString("get")
        val status = json.optString("status")
        
        if (pollUrl.isNullOrBlank()) {
            logger.logError("Replicate Video", "No polling URL returned by Replicate.", bodyStr)
            return Result.failure(Exception("No polling URL returned by Replicate."))
        }
        
        return Result.success(VideoGenerationResult(
            success = true,
            pollId = pollUrl,
            providerId = "Replicate",
            model = request.model
        ))
    }

    private suspend fun submitFalAiVideo(apiKey: String, request: VideoGenerationRequest): Result<VideoGenerationResult> {
        // e.g. "fal-ai/kling-video/v1/standard/image-to-video"
        val endpoint = if (request.model.startsWith("fal-ai/")) request.model else "fal-ai/kling-video/v1/standard/image-to-video"
        
        val requestObj = JSONObject().apply {
            put("prompt", request.prompt)
            request.initImageBase64?.let { put("image_url", "data:image/jpeg;base64,$it") }
            put("aspect_ratio", request.aspectRatio)
            put("duration", request.durationSeconds)
        }
        
        val requestBody = requestObj.toString().toRequestBody("application/json".toMediaType())
        
        // Log Request
        val url = "$FAL_AI_BASE_URL/$endpoint"
        logger.logRequest("Fal AI Video", url, request.model, null, requestObj.toString())
        
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Key $apiKey")
            .post(requestBody)
            .build()
            
        val response = client.newCall(req).execute()
        val bodyStr = response.body?.string() ?: ""
        
        if (!response.isSuccessful) {
            Log.e(TAG, "Fal AI error ${response.code}: $bodyStr")
            logger.logError("Fal AI Video", "HTTP ${response.code}", bodyStr)
            return Result.failure(Exception("Fal AI API error: ${response.code}"))
        }
        
        logger.logResponse("Fal AI Video", bodyStr)
        
        val json = JSONObject(bodyStr)
        val requestId = json.optString("request_id")
        val statusUrl = json.optString("status_url")
        val fallbackPollUrl = if (requestId.isNotBlank()) "$FAL_AI_BASE_URL/$endpoint/requests/$requestId" else ""
        val pollUrl = statusUrl.ifBlank { fallbackPollUrl }
        
        if (pollUrl.isBlank()) {
             logger.logError("Fal AI Video", "No request_id or status_url returned by Fal AI.", bodyStr)
             return Result.failure(Exception("No request_id or status_url returned by Fal AI. Response: $bodyStr"))
        }
        
        return Result.success(VideoGenerationResult(
            success = true,
            pollId = pollUrl,
            providerId = "fal_ai",
            model = request.model
        ))
    }
    
    /** 
     * Shared polling function for providers that return a polling URL.
     * To be called from a long-running WorkManager or ViewModel Scope.
     */
    suspend fun pollForVideoCompletion(provider: String, apiKey: String, pollUrl: String): Result<String> = withContext(Dispatchers.IO) {
        var attempts = 0
        val maxAttempts = 60 // 60 loops * 5s = ~5 minutes timeout
        
        while (attempts < maxAttempts) {
            try {
                val reqBuilder = Request.Builder().url(pollUrl).get()
                if (provider == "Replicate") {
                    reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                } else if (provider == "fal.ai" || provider == "fal_ai") {
                    reqBuilder.addHeader("Authorization", "Key $apiKey")
                }
                
                val response = client.newCall(reqBuilder.build()).execute()
                val bodyStr = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val status = json.optString("status", "")
                    
                    if (provider == "Replicate" && status == "succeeded") {
                        val outputStr = json.optString("output")
                        // Sometimes output is an array of strings
                        if (json.optJSONArray("output") != null) {
                            return@withContext Result.success(json.optJSONArray("output")!!.getString(0))
                        }
                        return@withContext Result.success(outputStr)
                    } else if ((provider == "fal.ai" || provider == "fal_ai") && (status == "COMPLETED" || json.has("video") || json.has("video_url"))) {
                        // Fall.ai Kling structure often puts result directly if finished
                        val videoObj = json.optJSONObject("video") ?: json.optJSONArray("videos")?.optJSONObject(0)
                        val videoUrlDirect = json.optString("video_url") 
                        val url = videoObj?.optString("url") ?: videoUrlDirect
                        if (!url.isNullOrBlank()) {
                            return@withContext Result.success(url)
                        } else {
                           return@withContext Result.failure(Exception("Generation COMPLETED, but no URL found in response: $bodyStr"))
                        }
                    } else if (status.contains("failed", ignoreCase = true) || status == "INCOMPLETE") {
                        return@withContext Result.failure(Exception("Generation failed on provider end. $bodyStr"))
                    }
                }
                
                // Wait 5 seconds before next poll
                kotlinx.coroutines.delay(5000)
                attempts++
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        
        return@withContext Result.failure(Exception("Polling timed out after 5 minutes."))
    }
}

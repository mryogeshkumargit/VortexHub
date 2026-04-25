package com.vortexai.android.domain.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vortexai.android.data.models.ModelsLabModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight wrapper around ModelsLab image-generation endpoints.
 * Focused on essential functionality with robust error handling.
 */
@Singleton
open class ModelsLabImageApi @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private const val TAG = "ModelsLabImageApi"
        private const val BASE_URL = "https://modelslab.com/api/v6"
        private const val BASE_URL_V1 = "https://modelslab.com/api/v1"
        private const val BASE_URL_V3 = "https://modelslab.com/api/v3"
        private const val BASE_URL_V4 = "https://modelslab.com/api/v4"
        
        private val logger = Logger.getLogger(TAG)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS) // 10 minutes for image generation
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(600, TimeUnit.SECONDS) // 10 minutes overall timeout
        .build()
    
    private val baseUrl: String = BASE_URL
    private val baseUrlV1: String = BASE_URL_V1
    private val baseUrlV3: String = BASE_URL_V3
    private val baseUrlV4: String = BASE_URL_V4

    // Protected methods to allow overriding in tests
    protected open fun createJSONObject(): JSONObject = JSONObject()
    protected open fun parseJSONObject(jsonString: String): JSONObject = JSONObject(jsonString)

    /** Fetch public models list using the correct v4 API endpoint */
    open suspend fun fetchPublicModels(apiKey: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Fetching ModelsLab public models from v4 API...")
                val payload = createJSONObject().apply { put("key", apiKey) }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                
                val req = Request.Builder()
                    .url("$baseUrlV4/dreambooth/model_list")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(req).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val models = parseV4ModelsResponse(responseBody)
                        if (models.isNotEmpty()) {
                            logger.log(Level.INFO, "Successfully fetched ${models.size} models from ModelsLab v4 API")
                            return@withContext Result.success(models)
                        }
                    }
                }
                
                // If API fails, return default models
                logger.log(Level.WARNING, "API call failed, returning default models")
                val defaultModels = getDefaultModels()
                Result.success(defaultModels)
                
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab fetchPublicModels error", e)
                Result.success(getDefaultModels()) // Return defaults instead of failing
            }
        }

    /** Fetch models by category from v4 API */
    open suspend fun fetchModelsByCategory(apiKey: String, category: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Fetching ModelsLab models for category: $category")
                val payload = createJSONObject().apply { put("key", apiKey) }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                
                val req = Request.Builder()
                    .url("$baseUrlV4/dreambooth/model_list")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(req).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val models = parseModelsByCategory(responseBody, category)
                        if (models.isNotEmpty()) {
                            logger.log(Level.INFO, "Successfully fetched ${models.size} $category models")
                            return@withContext Result.success(models)
                        }
                    }
                }
                
                // Return empty list if no models found for this category
                logger.log(Level.WARNING, "No $category models found")
                Result.success(emptyList())
                
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab fetchModelsByCategory error for $category", e)
                Result.success(emptyList())
            }
        }

    /** Fetch LLM models specifically */
    open suspend fun fetchLLMModels(apiKey: String): Result<List<String>> =
        fetchModelsByCategory(apiKey, "LLMaster")

    /** Fetch TTS/Audio models specifically */
    open suspend fun fetchTTSModels(apiKey: String): Result<List<String>> =
        fetchModelsByCategory(apiKey, "Audiogen")

    /** Fetch Image models specifically (includes both stable_diffusion and Image categories) */
    open suspend fun fetchImageModels(apiKey: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Fetching ModelsLab image models")
                val payload = createJSONObject().apply { put("key", apiKey) }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                
                val req = Request.Builder()
                    .url("$baseUrlV4/dreambooth/model_list")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(req).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val models = parseV4ModelsResponse(responseBody) // This already filters for image models
                        if (models.isNotEmpty()) {
                            logger.log(Level.INFO, "Successfully fetched ${models.size} image models")
                            return@withContext Result.success(models)
                        }
                    }
                }
                
                // If API fails, return default models
                logger.log(Level.WARNING, "Image models API call failed, returning default models")
                val defaultModels = getDefaultModels()
                Result.success(defaultModels)
                
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab fetchImageModels error", e)
                Result.success(getDefaultModels())
            }
        }

    /** Fetch LoRA models using hardcoded list for ModelsLab API */
    open suspend fun fetchLoraModels(apiKey: String): Result<List<ModelsLabModelInfo>> =
        withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("ModelsLab API key is required"))
                }
                
                logger.log(Level.INFO, "Using hardcoded ModelsLab LoRA models...")
                
                // Hardcoded ModelsLab LoRA models as requested
                val hardcodedLoraModels = listOf(
                    "flux-image-enhancer-by-dever-v1-0",
                    "dark-gothic-anime-flux-v0-1",
                    "smoke-flux-sdxl-by-dever-flux",
                    "world-of-light-sdxl-pony-flux-flux",
                    "wet-clingy-clothing-flux",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-vivid-realism",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-original",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-realistic-fantasy",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-gothic-lines",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-anime-lines",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-retro",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-v2",
                    "velvet-s-mythic-fantasy-styles-flux-pony-illustrious-flux-digital-lines",
                    "elizabeth-comstock-flux-sdxl-pony-1-5-v1-flux",
                    "celebrit-ai-deathmatch-celebrity-deathmatch-show-style-for-flux-versatile",
                    "flux-skin-texture-fluxrealskin-v2-0",
                    "flux-skin-texture-fluxrealskin",
                    "bouguereau-style-with-hunyuan-flux-v1",
                    "realistic-people-photograph-flux-flux-v0-2",
                    "realistic-people-photograph-flux-flux-v0-1",
                    "assorted-flux-girls-collection-elira-the-fur-wearer",
                    "assorted-flux-girls-collection-nerida-the-sea-nymph",
                    "assorted-flux-girls-collection-princess-sarayana",
                    "assorted-flux-girls-collection-goth-violet",
                    "assorted-flux-girls-collection-zyra-the-2000s-space-girl",
                    "assorted-flux-girls-collection-sivari-the-feline-alien",
                    "assorted-flux-girls-collection-seraphine-duskmoor",
                    "assorted-flux-girls-collection-clara-vintage-bookworm",
                    "assorted-flux-girls-collection-ashila-the-dragon-queen",
                    "assorted-flux-girls-collection-teyla-the-alien-ship-mate",
                    "assorted-flux-girls-collection-kaelen-the-scavenger",
                    "assorted-flux-girls-collection-roisin-the-gaelic-girl",
                    "assorted-flux-girls-collection-cindy-buzz",
                    "assorted-flux-girls-collection-50s-irish-girl-erin",
                    "assorted-flux-girls-collection-neferari-the-egyptian",
                    "assorted-flux-girls-collection-brunhilda-the-dwarf-girl",
                    "assorted-flux-girls-collection-atlas-the-ai-girl",
                    "assorted-flux-girls-collection-grikka-the-goblin-girl",
                    "assorted-flux-girls-collection-nancy-the-60s-hostess",
                    "assorted-flux-girls-collection-rue-the-wind-flower",
                    "assorted-flux-girls-collection-catalina-girl-of-the-dead",
                    "assorted-flux-girls-collection-zara-the-50s-space-girl",
                    "assorted-flux-girls-collection-native-american-aiyana",
                    "assorted-flux-girls-collection-kami-4-the-robotic-girl",
                    "assorted-flux-girls-collection-colette-the-french-girl",
                    "assorted-flux-girls-collection-free-elf-nymbrial",
                    "assorted-flux-girls-collection-princess-elowen",
                    "assorted-flux-girls-collection-silver-haired-mika",
                    "assorted-flux-girls-collection-elaris-the-wood-elf",
                    "assorted-flux-girls-collection-rosie-the-40s-american",
                    "assorted-flux-girls-collection-bohemian-girl-selene",
                    "assorted-flux-girls-collection-scottish-girl-isla",
                    "assorted-flux-girls-collection-cyberpunk-space-pirate",
                    "assorted-flux-girls-collection-snow-lover-tina",
                    "assorted-flux-girls-collection-mary-the-ghost-girl",
                    "assorted-flux-girls-collection-sunny-the-hippie-girl",
                    "assorted-flux-girls-collection-country-girl-sally",
                    "assorted-flux-girls-collection-european-princess-eleanor",
                    "assorted-flux-girls-collection-medieval-peasant-girl",
                    "assorted-flux-girls-collection-the-fairy-queen",
                    "assorted-flux-girls-collection-vampire-elizabeth",
                    "realistic-mona-lisa-lora-flux-realistic-mona-lisa-v1-0"
                )
                
                // Get manually added Lora models from DataStore
                val manuallyAddedModels = try {
                    // We need to access DataStore here, but we don't have direct access
                    // For now, we'll return the hardcoded models and handle manual models in the UI layer
                    emptyList<String>()
                } catch (e: Exception) {
                    emptyList<String>()
                }
                
                // Combine hardcoded and manually added models
                val allModels = hardcodedLoraModels.toMutableList()
                allModels.addAll(manuallyAddedModels)
                
                val loraModels = allModels.map { modelId ->
                    ModelsLabModelInfo(
                        modelId = modelId,
                        modelName = modelId,
                        description = if (manuallyAddedModels.contains(modelId)) "ModelsLab LoRA model (manually added)" else "ModelsLab LoRA model",
                        modelSubcategory = "lora"
                    )
                }
                
                logger.log(Level.INFO, "✅ ModelsLab: Returning ${loraModels.size} LoRA models (${hardcodedLoraModels.size} hardcoded + ${manuallyAddedModels.size} manual)")
                Result.success(loraModels)
                
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "❌ Error with hardcoded ModelsLab LoRA models: ${e.message}")
                Result.success(getDefaultLoraModels())
            }
        }

    /** Fetch Dreambooth models */
    open suspend fun fetchDreamboothModels(apiKey: String): Result<List<ModelsLabModelInfo>> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Fetching ModelsLab Dreambooth models...")
                val payload = createJSONObject().apply { put("key", apiKey) }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                
                val req = Request.Builder()
                    .url("$baseUrlV3/dreambooth_list")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    logger.log(Level.WARNING, "Failed to fetch Dreambooth models: HTTP ${response.code}")
                    return@withContext Result.success(getDefaultDreamboothModels())
                }
                
                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    logger.log(Level.WARNING, "Empty response body for Dreambooth models")
                    return@withContext Result.success(getDefaultDreamboothModels())
                }
                
                val json = parseJSONObject(responseBody)
                val models = parseDreamboothModelsResponse(json)
                
                if (models.isNotEmpty()) {
                    logger.log(Level.INFO, "Successfully fetched ${models.size} Dreambooth models")
                    Result.success(models)
                } else {
                    logger.log(Level.WARNING, "No Dreambooth models found, returning defaults")
                    Result.success(getDefaultDreamboothModels())
                }
                
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab fetchDreamboothModels error", e)
                Result.success(getDefaultDreamboothModels()) // Return defaults instead of failing
            }
        }

    /** Community Models - Text to Image using v6 API */
    open suspend fun text2Img(apiKey: String, request: ImageGenerationRequest): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Starting ModelsLab text2img request with model: ${request.model}")
                
                val payload = createJSONObject().apply {
                    put("key", apiKey)
                    put("model_id", request.model)
                    put("prompt", request.prompt)
                    put("negative_prompt", request.negativePrompt ?: "")
                    put("width", request.width.toString())
                    put("height", request.height.toString())
                    put("samples", (request.samples ?: 1).toString())
                    put("num_inference_steps", request.steps.toString())
                    put("safety_checker", "no")
                    put("enhance_prompt", "yes")
                    put("seed", request.seed)
                    put("guidance_scale", request.guidanceScale)
                    put("panorama", "no")
                    put("self_attention", "no")
                    put("upscale", "no")
                    put("lora_model", request.loraModel)
                    put("tomesd", "yes")
                    put("clip_skip", "2")
                    put("use_karras_sigmas", "yes")
                    put("vae", null)
                    put("lora_strength", request.loraStrength)
                    put("scheduler", request.scheduler ?: "UniPCMultistepScheduler")
                    put("webhook", null)
                    put("track_id", null)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/images/text2img")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }

                val responseBody = response.body?.string() ?: "{}"
                val json = parseJSONObject(responseBody)
                
                if (json.optString("status") != "success") {
                    return@withContext Result.failure(Exception(json.optString("message", "Unknown error")))
                }
                
                val output = json.optJSONArray("output")?.optString(0) ?: json.optString("output")
                if (output.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No image URL returned"))
                } else {
                    logger.log(Level.INFO, "ModelsLab text2img success")
                    return@withContext Result.success(output)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab text2img error", e)
                return@withContext Result.failure(e)
            }
        }

    /** Image to Image using v6 API */
    open suspend fun img2Img(apiKey: String, request: ImageGenerationRequest): Result<String> =
        withContext(Dispatchers.IO) {
            if (request.initImageBase64.isNullOrBlank()) {
                return@withContext Result.failure(Exception("initImageBase64 is required for img2img"))
            }
            try {
                logger.log(Level.INFO, "Starting ModelsLab img2img request")
                
                // ModelsLab requires URL, not base64 - upload to imgbb if needed
                val initImage = if (request.initImageBase64.startsWith("http")) {
                    request.initImageBase64
                } else {
                    uploadToImgbb(apiKey, request.initImageBase64)
                }

                val payload = createJSONObject().apply {
                    put("key", apiKey)
                    put("model_id", request.model)
                    put("prompt", request.prompt)
                    put("negative_prompt", request.negativePrompt)
                    put("init_image", initImage)
                    put("samples", (request.samples ?: 1).toString())
                    put("num_inference_steps", request.steps.toString())
                    put("safety_checker", "yes")
                    put("enhance_prompt", "yes")
                    put("guidance_scale", request.guidanceScale)
                    put("strength", request.strength ?: 0.7)
                    put("scheduler", request.scheduler ?: "UniPCMultistepScheduler")
                    put("seed", request.seed)
                    put("lora_model", request.loraModel)
                    put("tomesd", "yes")
                    put("use_karras_sigmas", "yes")
                    put("vae", null)
                    put("lora_strength", request.loraStrength)
                    put("webhook", null)
                    put("track_id", null)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/images/img2img")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val json = parseJSONObject(responseBody)
                
                handleAsyncResponse(apiKey, json)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab img2img error", e)
                return@withContext Result.failure(e)
            }
        }

    /** FLUX Text to Image using v6 API */
    open suspend fun fluxText2Img(apiKey: String, request: ImageGenerationRequest): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Starting ModelsLab FLUX text2img request")
                
                val payload = createJSONObject().apply {
                    put("key", apiKey)
                    put("model_id", "flux")
                    put("prompt", request.prompt)
                    put("width", request.width.toString())
                    put("height", request.height.toString())
                    put("samples", "1")
                    put("num_inference_steps", request.steps.toString())
                    put("safety_checker", "no")
                    put("enhance_prompt", "yes")
                    put("seed", request.seed)
                    put("guidance_scale", request.guidanceScale)
                    put("tomesd", "yes")
                    put("clip_skip", "2")
                    put("vae", null)
                    put("webhook", null)
                    put("track_id", null)
                }
                
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/images/text2img")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val json = parseJSONObject(responseBody)
                
                if (json.optString("status") != "success") {
                    return@withContext Result.failure(Exception(json.optString("message", "Unknown error")))
                }
                
                val output = json.optJSONArray("output")?.optString(0) ?: json.optString("output")
                if (output.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No image URL returned"))
                } else {
                    logger.log(Level.INFO, "ModelsLab FLUX success")
                    return@withContext Result.success(output)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab FLUX text2img error", e)
                return@withContext Result.failure(e)
            }
        }

    /** Realtime Stable Diffusion - Text to Image */
    open suspend fun realtimeText2Img(apiKey: String, request: ImageGenerationRequest): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Starting ModelsLab realtime text2img request")
                
                val payload = createJSONObject().apply {
                    put("key", apiKey)
                    put("prompt", request.prompt)
                    put("negative_prompt", request.negativePrompt ?: "bad quality")
                    put("width", request.width.toString())
                    put("height", request.height.toString())
                    put("safety_checker", false)
                    put("seed", request.seed)
                    put("samples", request.samples ?: 1)
                    put("base64", false)
                    put("webhook", null)
                    put("track_id", null)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/realtime/text2img")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val json = parseJSONObject(responseBody)
                
                if (json.optString("status") != "success") {
                    return@withContext Result.failure(Exception(json.optString("message", "Unknown error")))
                }
                
                val output = json.optJSONArray("output")?.optString(0) ?: json.optString("output")
                if (output.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No image URL returned"))
                } else {
                    logger.log(Level.INFO, "ModelsLab realtime text2img success")
                    return@withContext Result.success(output)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab realtime text2img error", e)
                return@withContext Result.failure(e)
            }
        }

    /** Realtime Stable Diffusion - Image to Image */
    open suspend fun realtimeImage2Img(apiKey: String, request: ImageGenerationRequest): Result<String> =
        withContext(Dispatchers.IO) {
            if (request.initImageBase64.isNullOrBlank()) {
                return@withContext Result.failure(Exception("initImageBase64 is required for realtime img2img"))
            }
            try {
                logger.log(Level.INFO, "Starting ModelsLab realtime img2img request")
                
                val initImage = if (request.initImageBase64.startsWith("http")) {
                    request.initImageBase64
                } else {
                    uploadToImgbb(apiKey, request.initImageBase64)
                }

                val payload = createJSONObject().apply {
                    put("key", apiKey)
                    put("prompt", request.prompt)
                    put("negative_prompt", request.negativePrompt ?: "bad quality")
                    put("init_image", initImage)
                    put("width", request.width.toString())
                    put("height", request.height.toString())
                    put("samples", (request.samples ?: 1).toString())
                    put("temp", false)
                    put("safety_checker", false)
                    put("strength", request.strength ?: 0.7)
                    put("seed", request.seed)
                    put("webhook", null)
                    put("track_id", null)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/realtime/img2img")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val json = parseJSONObject(responseBody)
                
                handleAsyncResponse(apiKey, json)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab realtime img2img error", e)
                return@withContext Result.failure(e)
            }
        }

    /** LoRA Text to Image (uses regular text2img endpoint with LoRA parameters) */
    open suspend fun loraText2Img(apiKey: String, request: ImageGenerationRequest): Result<String> =
        withContext(Dispatchers.IO) {
            if (request.loraModel.isNullOrBlank()) {
                return@withContext Result.failure(Exception("loraModel is required for LoRA generation"))
            }
            try {
                logger.log(Level.INFO, "Starting ModelsLab LoRA text2img request with LoRA: ${request.loraModel}")
                
                val payload = createJSONObject().apply {
                    put("key", apiKey)
                    put("model_id", request.model)
                    put("prompt", request.prompt)
                    put("negative_prompt", request.negativePrompt ?: "")
                    put("width", request.width.toString())
                    put("height", request.height.toString())
                    put("samples", (request.samples ?: 1).toString())
                    put("num_inference_steps", request.steps.toString())
                    put("safety_checker", "no")
                    put("enhance_prompt", "yes")
                    put("seed", request.seed)
                    put("guidance_scale", request.guidanceScale)
                    put("panorama", "no")
                    put("self_attention", "no")
                    put("upscale", "no")
                    put("lora_strength", request.loraStrength ?: 0.45)
                    put("lora_model", request.loraModel)
                    put("scheduler", request.scheduler ?: "UniPCMultistepScheduler")
                    put("webhook", null)
                    put("track_id", null)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/images/text2img")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val json = parseJSONObject(responseBody)
                
                if (json.optString("status") != "success") {
                    return@withContext Result.failure(Exception(json.optString("message", "Unknown error")))
                }
                
                val output = json.optJSONArray("output")?.optString(0) ?: json.optString("output")
                if (output.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No image URL returned"))
                } else {
                    logger.log(Level.INFO, "ModelsLab LoRA text2img success")
                    return@withContext Result.success(output)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab LoRA text2img error", e)
                return@withContext Result.failure(e)
            }
        }

    /** Video Generation using v6 API */
    open suspend fun generateVideo(apiKey: String, request: VideoGenerationRequest): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                logger.log(Level.INFO, "Starting ModelsLab video generation request with model: ${request.model}")
                
                val initImage = if (!request.initImageBase64.isNullOrBlank()) {
                    if (request.initImageBase64.startsWith("http")) {
                        request.initImageBase64
                    } else {
                        // We use the imgbb upload utility if it's base64, as Video Generation API also needs URLs
                        uploadToImgbb(apiKey, request.initImageBase64)
                    }
                } else null

                val payload = createJSONObject().apply {
                    put("key", apiKey)
                    put("prompt", request.prompt)
                    put("negative_prompt", request.negativePrompt ?: "")
                    if (initImage != null) put("init_image", initImage)
                    put("width", 512)
                    put("height", 512)
                    put("video_length", request.durationSeconds * request.framesPerSecond)
                    put("fps", request.framesPerSecond)
                    put("webhook", null)
                    put("track_id", null)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/video/text2video") // Typical ModelsLab endpoint format for video
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(req).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }

                val responseBody = response.body?.string() ?: "{}"
                val json = parseJSONObject(responseBody)
                
                // Uses the same async polling logic as image-to-image
                handleAsyncResponse(apiKey, json)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "ModelsLab video generation error", e)
                return@withContext Result.failure(e)
            }
        }

    /** Parse v4 API response and separate regular models from LoRA models */
    private fun parseV4ModelsResponse(responseBody: String): List<String> {
        val regularModels = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(responseBody)
            logger.log(Level.INFO, "Processing ${jsonArray.length()} total models from API")
            
            var imagenCount = 0
            var readyCount = 0
            var regularCount = 0
            var loraCount = 0
            var fluxCount = 0
            
            for (i in 0 until jsonArray.length()) {
                val modelObj = jsonArray.getJSONObject(i)
                
                // Filter for image generation models based on documentation
                val feature = modelObj.optString("feature", "")
                val modelCategory = modelObj.optString("model_category", "")
                
                // Accept models with feature "Imagen" OR model_category "Image" (for Flux models)
                val isImageModel = feature == "Imagen" || modelCategory == "Image"
                if (!isImageModel) {
                    continue // Skip non-image models (LLMaster, Audiogen, etc.)
                }
                imagenCount++
                
                val status = modelObj.optString("status", "")
                if (status != "model_ready") {
                    continue // Skip models that aren't ready
                }
                readyCount++
                
                val modelId = modelObj.optString("model_id", "")
                val modelSubcategory = modelObj.optString("model_subcategory", "")
                
                if (modelId.isNotBlank()) {
                    // Categorize models based on subcategory
                    when (modelSubcategory) {
                        "lora" -> {
                            loraCount++
                            // LoRA models will be handled separately
                            // Don't add to regular models list
                        }
                        "flux" -> {
                            fluxCount++
                            // Flux models are regular image models
                            regularModels.add(modelId)
                            regularCount++
                        }
                        null, "", "null" -> {
                            // Regular image models (stable diffusion, etc.)
                            regularModels.add(modelId)
                            regularCount++
                        }
                        else -> {
                            // Other subcategories - treat as regular models for now
                            regularModels.add(modelId)
                            regularCount++
                        }
                    }
                }
            }
            
            logger.log(Level.INFO, "Filtering results: Total=${jsonArray.length()}, Image=$imagenCount, Ready=$readyCount, Regular=$regularCount, LoRA=$loraCount, Flux=$fluxCount")
            
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing v4 ModelsLab response: ${e.message}")
        }
        return regularModels
    }

    /** Parse models by specific category from v4 API response */
    private fun parseModelsByCategory(responseBody: String, category: String): List<String> {
        val models = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(responseBody)
            logger.log(Level.INFO, "Processing ${jsonArray.length()} total models for category: $category")
            
            var categoryCount = 0
            var readyCount = 0
            
            for (i in 0 until jsonArray.length()) {
                val modelObj = jsonArray.getJSONObject(i)
                
                val modelCategory = modelObj.optString("model_category", "")
                if (modelCategory != category) {
                    continue // Skip models not in the requested category
                }
                categoryCount++
                
                val status = modelObj.optString("status", "")
                if (status != "model_ready") {
                    continue // Skip models that aren't ready
                }
                readyCount++
                
                val modelId = modelObj.optString("model_id", "")
                if (modelId.isNotBlank()) {
                    models.add(modelId)
                }
            }
            
            logger.log(Level.INFO, "Category filtering results: Total=${jsonArray.length()}, Category=$categoryCount, Ready=$readyCount, Final=${models.size}")
            
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing ModelsLab response for category $category: ${e.message}")
        }
        return models
    }

    /** Parse v4 API response specifically for LoRA models */
    private fun parseV4LoraModelsResponse(responseBody: String): List<ModelsLabModelInfo> {
        val loraModels = mutableListOf<ModelsLabModelInfo>()
        try {
            val jsonArray = JSONArray(responseBody)
            
            for (i in 0 until jsonArray.length()) {
                val modelObj = jsonArray.getJSONObject(i)
                
                // Strict filtering: MUST have feature "Imagen" AND subcategory "lora"
                val feature = modelObj.optString("feature", "")
                val modelSubcategory = modelObj.optString("model_subcategory", "")
                val status = modelObj.optString("status", "")
                
                if (feature == "Imagen" && modelSubcategory == "lora" && status == "model_ready") {
                    val modelId = modelObj.optString("model_id", "")
                    val modelName = modelObj.optString("model_name", modelId)
                    val description = modelObj.optString("description", "")
                    val screenshots = modelObj.optString("screenshots", "")
                    val isNsfw = modelObj.optString("is_nsfw", "0")
                    
                    if (modelId.isNotBlank()) {
                        loraModels.add(ModelsLabModelInfo(
                            modelId = modelId,
                            modelName = modelName,
                            description = description,
                            screenshots = screenshots,
                            modelSubcategory = "lora",
                            isNsfw = isNsfw
                        ))
                    }
                }
            }
            
            logger.log(Level.INFO, "Parsed ${loraModels.size} valid LoRA models from v4 API response")
            
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing v4 LoRA models response: ${e.message}")
        }
        return loraModels
    }

    /** Parse ModelsLab API response into model list (legacy method) */
    private fun parseModelsResponse(json: JSONObject): List<String> {
        val models = mutableListOf<String>()
        try {
            // Try different response formats
            val dataArray = json.optJSONArray("data") 
                ?: json.optJSONArray("models")
                ?: json.optJSONArray("results")
            
            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val modelObj = dataArray.optJSONObject(i)
                    if (modelObj != null) {
                        val modelId = modelObj.optString("id", modelObj.optString("name", ""))
                        if (modelId.isNotBlank()) {
                            models.add(modelId)
                        }
                    } else {
                        // Handle string-only responses
                        val modelId = dataArray.optString(i)
                        if (modelId.isNotBlank()) {
                            models.add(modelId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing ModelsLab response: ${e.message}")
        }
        return models
    }

    /** Parse LoRA models response */
    private fun parseLoraModelsResponse(json: JSONObject): List<ModelsLabModelInfo> {
        val models = mutableListOf<ModelsLabModelInfo>()
        try {
            val dataArray = json.optJSONArray("data") ?: JSONArray()
            for (i in 0 until dataArray.length()) {
                val modelObj = dataArray.optJSONObject(i)
                if (modelObj != null) {
                    val modelId = modelObj.optString("model_id", modelObj.optString("id", ""))
                    val modelName = modelObj.optString("model_name", modelObj.optString("name", modelId))
                    val description = modelObj.optString("description", "")
                    
                    if (modelId.isNotBlank()) {
                        models.add(ModelsLabModelInfo(
                            modelId = modelId,
                            modelName = modelName,
                            description = description,
                            modelSubcategory = "lora"
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing LoRA models response: ${e.message}")
        }
        return models
    }

    /** Parse Dreambooth models response */
    private fun parseDreamboothModelsResponse(json: JSONObject): List<ModelsLabModelInfo> {
        val models = mutableListOf<ModelsLabModelInfo>()
        try {
            val dataArray = json.optJSONArray("data") ?: JSONArray()
            for (i in 0 until dataArray.length()) {
                val modelObj = dataArray.optJSONObject(i)
                if (modelObj != null) {
                    val modelId = modelObj.optString("model_id", modelObj.optString("id", ""))
                    val modelName = modelObj.optString("model_name", modelObj.optString("name", modelId))
                    val description = modelObj.optString("description", "")
                    
                    if (modelId.isNotBlank()) {
                        models.add(ModelsLabModelInfo(
                            modelId = modelId,
                            modelName = modelName,
                            description = description,
                            modelSubcategory = "dreambooth"
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing Dreambooth models response: ${e.message}")
        }
        return models
    }

    /** Get default ModelsLab models when API fails */
    private fun getDefaultModels(): List<String> {
        return listOf(
            "stable-diffusion-v1-5",
            "stable-diffusion-xl-base-1.0",
            "analog-diffusion",
            "anything-v3",
            "dreamshaper-8",
            "realistic-vision-v2",
            "flux-dev",
            "flux-schnell",
            "sdxl-turbo",
            "kandinsky-2-2",
            "playground-v2",
            "dreamlike-diffusion",
            "realistic-vision-v1-4"
        )
    }

    /** Get default LoRA models when API fails */
    private fun getDefaultLoraModels(): List<ModelsLabModelInfo> {
        return listOf(
            ModelsLabModelInfo(
                modelId = "anime-style",
                modelName = "Anime Style LoRA",
                description = "Anime character style",
                modelSubcategory = "lora"
            ),
            ModelsLabModelInfo(
                modelId = "realistic-portrait",
                modelName = "Realistic Portrait LoRA",
                description = "Realistic portrait style",
                modelSubcategory = "lora"
            ),
            ModelsLabModelInfo(
                modelId = "fantasy-art",
                modelName = "Fantasy Art LoRA",
                description = "Fantasy art style",
                modelSubcategory = "lora"
            )
        )
    }

    /** Get default Dreambooth models when API fails */
    private fun getDefaultDreamboothModels(): List<ModelsLabModelInfo> {
        return listOf(
            ModelsLabModelInfo(
                modelId = "custom-portrait",
                modelName = "Custom Portrait Model",
                description = "Custom trained portrait model",
                modelSubcategory = "dreambooth"
            ),
            ModelsLabModelInfo(
                modelId = "style-transfer",
                modelName = "Style Transfer Model",
                description = "Custom style transfer model",
                modelSubcategory = "dreambooth"
            )
        )
    }
    
    private suspend fun uploadToImgbb(apiKey: String, base64: String): String {
        val prefs = dataStore.data.first()
        val imgbbKey = prefs[androidx.datastore.preferences.core.stringPreferencesKey("imgbb_api_key")] 
            ?: throw Exception("imgbb API key not configured")
        val cleanBase64 = if (base64.startsWith("data:image")) base64.substringAfter(",") else base64
        
        val formBody = okhttp3.FormBody.Builder()
            .add("key", imgbbKey)
            .add("image", cleanBase64)
            .add("expiration", "600")
            .build()
        
        val req = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(formBody)
            .build()
        
        val response = client.newCall(req).execute()
        if (!response.isSuccessful) throw Exception("imgbb upload failed: ${response.code}")
        
        val json = parseJSONObject(response.body?.string() ?: "{}")
        return json.optJSONObject("data")?.optString("url") ?: throw Exception("No URL from imgbb")
    }
    
    private suspend fun handleAsyncResponse(apiKey: String, json: JSONObject): Result<String> {
        val status = json.optString("status")
        
        if (status == "success") {
            val output = json.optJSONArray("output")?.optString(0) ?: json.optString("output")
            return if (output.isNullOrBlank()) {
                Result.failure(Exception("No image URL returned"))
            } else {
                Result.success(output)
            }
        }
        
        // Async response - poll
        val fetchUrl = json.optString("fetch_result")
        if (fetchUrl.isBlank()) return Result.failure(Exception("No fetch URL for async response"))
        
        val eta = json.optInt("eta", 10)
        kotlinx.coroutines.delay((eta * 1000L).coerceAtMost(30000L))
        
        repeat(60) {
            val pollData = createJSONObject().apply { put("key", apiKey) }
            val pollReq = Request.Builder()
                .url(fetchUrl)
                .post(pollData.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val pollResp = client.newCall(pollReq).execute()
            if (pollResp.isSuccessful) {
                val pollJson = parseJSONObject(pollResp.body?.string() ?: "{}")
                when (pollJson.optString("status")) {
                    "success" -> {
                        val output = pollJson.optJSONArray("output")?.optString(0) ?: pollJson.optString("output")
                        return if (output.isNullOrBlank()) {
                            Result.failure(Exception("No image URL"))
                        } else {
                            Result.success(output)
                        }
                    }
                    "failed", "error" -> return Result.failure(Exception(pollJson.optString("message", "Failed")))
                }
            }
            kotlinx.coroutines.delay(5000L)
        }
        
        return Result.failure(Exception("Polling timeout"))
    }
}
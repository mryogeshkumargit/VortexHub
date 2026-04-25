package com.vortexai.android.ui.screens.chat

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vortexai.android.data.models.*
import com.vortexai.android.data.repository.ChatImageSettingsRepository
import com.vortexai.android.data.repository.ChatRepository
import com.vortexai.android.domain.service.ImageGenerationService
import com.vortexai.android.domain.service.ImageGenerationRequest
import com.vortexai.android.domain.service.ImageGenerationSource
import com.vortexai.android.domain.service.SourceType
import com.vortexai.android.domain.service.ImageEditingService
import com.vortexai.android.domain.service.ImageEditingRequest
import com.vortexai.android.domain.service.CustomApiExecutor
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatImageGenerator @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val imageGenerationService: ImageGenerationService,
    private val imageEditingService: ImageEditingService,
    private val chatImageSettingsRepository: ChatImageSettingsRepository,
    private val chatRepository: ChatRepository,
    private val dataStore: DataStore<Preferences>,
    private val imageStorageHelper: com.vortexai.android.utils.ImageStorageHelper,
    private val customApiProviderRepository: com.vortexai.android.data.repository.CustomApiProviderRepository,
    private val customApiExecutor: CustomApiExecutor
) {
    
    companion object {
        private const val TAG = "ChatImageGenerator"
    }
    
    suspend fun generateImageWithChatSettings(
        conversationId: String,
        prompt: String,
        character: Character?
    , messageId: String? = null): Result<MessageResponse> {
        return try {
            // Get chat-specific image settings
            val chatImageSettings = chatImageSettingsRepository.getChatImageSettings(conversationId)
            
            // Determine final prompt based on prediction creation method
            val finalPrompt = when (chatImageSettings?.predictionCreationMethod) {
                com.vortexai.android.data.model.PredictionCreationMethod.MANUAL -> {
                    chatImageSettings.manualPredictionInput ?: prompt
                }
                else -> prompt
            }
            
            // Always get input image for /image command - force image-to-image generation
            val inputImageBase64 = when (chatImageSettings?.inputImageOption) {
                com.vortexai.android.data.model.InputImageOption.CHARACTER_AVATAR -> {
                    getCharacterAvatarAsBase64(character)
                }
                com.vortexai.android.data.model.InputImageOption.LOCAL_IMAGE -> {
                    chatImageSettings.localImagePath?.let { path ->
                        withContext(Dispatchers.IO) {
                            try {
                                val file = java.io.File(path)
                                if (file.exists()) {
                                    val bytes = file.readBytes()
                                    android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                                } else null
                            } catch (e: Exception) {
                                Log.e(TAG, "Error reading local image: ${e.message}")
                                null
                            }
                        }
                    }
                }
                com.vortexai.android.data.model.InputImageOption.CLOUD_IMAGE -> {
                    chatImageSettings.cloudImageUrl
                }
                else -> getCharacterAvatarAsBase64(character) // Default to character avatar
            }
            
            // Always use image editing for /image command
            if (inputImageBase64 != null) {
                generateImageWithEditing(
                    conversationId = conversationId,
                    prompt = finalPrompt,
                    inputImageBase64 = inputImageBase64,
                    character = character
                , messageId = messageId)
            } else {
                Result.failure(Exception("No input image available. Please set a character avatar or configure image input in chat settings."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateImageWithChatSettings", e)
            Result.failure(e)
        }
    }
    
    private suspend fun generateImageInternal(
        conversationId: String,
        prompt: String,
        inputImageBase64: String?,
        character: Character?
    , messageId: String? = null): Result<MessageResponse> {
        return try {
            // Get image generation settings from DataStore
            val preferences = dataStore.data.first()
            val imageProvider = preferences[SettingsViewModel.IMAGE_PROVIDER_KEY] ?: "Together AI"
            
            // For Custom API, we need to fetch from database-backed providers
            var apiKey = ""
            var customEndpoint: String? = null
            var customProviderId: String? = null
            
            if (imageProvider == "Custom API") {
                // Get selected custom provider from database
                customProviderId = preferences[androidx.datastore.preferences.core.stringPreferencesKey("selected_custom_image_provider_id")] ?: ""
                val selectedProviderId = customProviderId
                if (selectedProviderId.isBlank()) {
                    return Result.failure(Exception("No custom image provider selected. Please go to Settings → Image Generation and select a custom provider."))
                }
                
                val customProvider = customApiProviderRepository.getProviderById(selectedProviderId)
                    ?: return Result.failure(Exception("Custom provider not found. Please reconfigure in Settings → Image Generation."))
                
                if (!customProvider.isEnabled) {
                    return Result.failure(Exception("Custom provider '${customProvider.name}' is disabled. Please enable it in Settings."))
                }
                
                apiKey = customProvider.apiKey
                customEndpoint = customProvider.baseUrl
                
                if (apiKey.isBlank()) {
                    return Result.failure(Exception("No API key configured for custom provider '${customProvider.name}'. Please configure it in Settings → Custom API Providers."))
                }
                
                if (customEndpoint.isBlank()) {
                    return Result.failure(Exception("No endpoint URL configured for custom provider '${customProvider.name}'. Please configure it in Settings → Custom API Providers."))
                }
                
                Log.d(TAG, "Using Custom API provider: ${customProvider.name}, endpoint: $customEndpoint")
            } else {
                // Standard provider API key retrieval
                apiKey = when (imageProvider) {
                    "Together AI" -> {
                        val imageKey = preferences[SettingsViewModel.TOGETHER_AI_IMAGE_API_KEY] ?: ""
                        val llmKey = preferences[androidx.datastore.preferences.core.stringPreferencesKey("together_ai_api_key")] ?: ""
                        imageKey.ifBlank { llmKey }
                    }
                    "Hugging Face" -> preferences[SettingsViewModel.HUGGINGFACE_IMAGE_API_KEY] ?: ""
                    "ComfyUI" -> preferences[SettingsViewModel.COMFYUI_API_KEY] ?: ""
                    "ModelsLab" -> preferences[SettingsViewModel.MODELSLAB_IMAGE_API_KEY] ?: ""
                    "Replicate" -> preferences[SettingsViewModel.REPLICATE_API_KEY] ?: ""
                    else -> ""
                }
                
                val requiresApiKey = imageProvider in listOf("Together AI", "Hugging Face", "ModelsLab", "Replicate")
                if (requiresApiKey && apiKey.isBlank()) {
                    return Result.failure(Exception("No API key configured for $imageProvider. Please go to Settings → Image Generation."))
                }
                
                // For ComfyUI, get endpoint
                if (imageProvider == "ComfyUI") {
                    customEndpoint = preferences[SettingsViewModel.COMFYUI_ENDPOINT_KEY] ?: ""
                    if (customEndpoint.isBlank()) {
                        return Result.failure(Exception("No endpoint configured for ComfyUI. Please go to Settings → Image Generation."))
                    }
                }
            }
            
            // Parse image size
            val imageSize = preferences[SettingsViewModel.IMAGE_SIZE_KEY] ?: "1024x1024"
            val sizeParts = imageSize.split("x")
            val width = if (imageProvider == "Replicate") {
                val w = preferences[androidx.datastore.preferences.core.intPreferencesKey("replicate_width")] ?: 1024
                android.util.Log.d("ChatImageGenerator", "Replicate width from settings: $w")
                w
            } else {
                sizeParts.getOrNull(0)?.toIntOrNull() ?: 1024
            }
            val height = if (imageProvider == "Replicate") {
                val h = preferences[androidx.datastore.preferences.core.intPreferencesKey("replicate_height")] ?: 1024
                android.util.Log.d("ChatImageGenerator", "Replicate height from settings: $h")
                h
            } else {
                sizeParts.getOrNull(1)?.toIntOrNull() ?: 1024
            }
            
            val modelsLabWorkflow = preferences[SettingsViewModel.MODELSLAB_WORKFLOW_KEY] ?: "default"
            val loraModelPref = preferences[SettingsViewModel.MODELSLAB_LORA_MODEL_KEY] ?: ""
            val loraStrengthPref = preferences[SettingsViewModel.MODELSLAB_LORA_STRENGTH_KEY] ?: 0.6f
            val negativePromptPref = preferences[SettingsViewModel.NEGATIVE_PROMPT_KEY] ?: ""

            val imageRequest = ImageGenerationRequest(
                prompt = prompt,
                model = when (imageProvider) {
                    "Replicate" -> preferences[SettingsViewModel.REPLICATE_MODEL_KEY] ?: "black-forest-labs/flux-schnell"
                    else -> preferences[SettingsViewModel.IMAGE_MODEL_KEY] ?: run {
                        when (imageProvider) {
                            "Together AI" -> "black-forest-labs/FLUX.1-schnell"
                            "ModelsLab" -> "stable-diffusion-v1-5"
                            "Replicate" -> "black-forest-labs/flux-schnell"
                            else -> "stabilityai/stable-diffusion-xl-base-1.0"
                        }
                    }
                },
                width = width,
                height = height,
                steps = preferences[SettingsViewModel.STEPS_KEY] ?: 20,
                guidanceScale = preferences[SettingsViewModel.GUIDANCE_SCALE_KEY] ?: 7.5f,
                workflow = if (imageProvider == "ModelsLab") modelsLabWorkflow else preferences[SettingsViewModel.COMFYUI_WORKFLOW_KEY],
                loraModel = if (loraModelPref.isNotBlank()) loraModelPref else null,
                loraStrength = if (loraModelPref.isNotBlank()) loraStrengthPref else null,
                negativePrompt = if (negativePromptPref.isNotBlank()) negativePromptPref else null,
                initImageBase64 = inputImageBase64
            )
            
            // Use existing customEndpoint (already set for Custom API and ComfyUI above)
            
            // Generate the image with tracking
            val generationId = "chat_${conversationId}_${System.currentTimeMillis()}"
            val sourceLocation = ImageGenerationSource(
                type = SourceType.CHAT,
                conversationId = conversationId,
                characterId = character?.id
            )
            
            val result = imageGenerationService.generateImage(
                provider = imageProvider,
                apiKey = apiKey,
                request = imageRequest,
                customEndpoint = customEndpoint,
                generationId = generationId,
                sourceLocation = sourceLocation,
                customProviderId = customProviderId
            )
            
            result.fold(
                onSuccess = { imageResult ->
                    val imageUrl = imageResult.imageUrl ?: run {
                        imageResult.imageBase64?.let { "data:image/png;base64,$it" }
                    }
                    
                    val imageMessage = MessageResponse(
                        conversationId = conversationId,
                                id = messageId ?: com.vortexai.android.utils.IdGenerator.generateMessageId(),
                        content = "Generated image: $prompt",
                        senderType = MessageSenderType.SYSTEM,
                        senderId = "system",
                        senderName = "Image Generator",
                        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                        messageType = MessageType.IMAGE,
                        metadata = MessageResponseMetadata(
                            imageUrl = imageUrl,
                            generationTime = imageResult.generationTime,
                            modelUsed = imageResult.model
                        )
                    )
                    
                    // Save to database
                    saveImageMessageToDatabase(conversationId, prompt, imageUrl, imageResult, messageId = messageId)
                    
                    
                            Result.success(imageMessage)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun saveImageMessageToDatabase(
        conversationId: String,
        prompt: String,
        imageUrl: String?,
        imageResult: com.vortexai.android.domain.service.ImageGenerationResult
    , messageId: String? = null) {
        try {
            val metadataJson = org.json.JSONObject().apply {
                put("localPath", imageUrl ?: "")
                put("generationTime", imageResult.generationTime.toString())
                put("modelUsed", imageResult.model ?: "")
            }.toString()

            val finalMessageId = messageId ?: com.vortexai.android.utils.IdGenerator.generateMessageId()

            val imageMessageEntity = Message(
                id = finalMessageId,
                conversationId = conversationId,
                content = "Generated image: $prompt",
                role = "system",
                senderType = "system",
                timestamp = System.currentTimeMillis(),
                messageType = "image",
                metadataJson = metadataJson
            )

            withContext(Dispatchers.IO) {
                chatRepository.insertMessage(imageMessageEntity)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image message to database", e)
        }
    }
    
    private suspend fun getCharacterAvatarAsBase64(character: Character?): String? {
        val avatarPath = character?.avatarUrl
        
        if (avatarPath.isNullOrBlank()) {
            return null
        }
        
        return if (avatarPath.startsWith("data:image")) {
            avatarPath.substringAfter(",")
        } else {
            try {
                withContext(Dispatchers.IO) {
                    val bytes: ByteArray? = when {
                        avatarPath.startsWith("http", ignoreCase = true) -> {
                            val request = okhttp3.Request.Builder()
                                .url(avatarPath)
                                .addHeader("User-Agent", "VortexAI-App/1.0")
                                .build()
                            okhttp3.OkHttpClient.Builder()
                                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                                .newCall(request).execute().use { resp ->
                                    if (resp.isSuccessful) {
                                        resp.body?.bytes()
                                    } else null
                                }
                        }
                        avatarPath.startsWith("file://") -> {
                            when {
                                avatarPath.contains("/android_asset/") -> null
                                else -> {
                                    try {
                                        val file = java.io.File(java.net.URI(avatarPath))
                                        if (file.exists()) file.readBytes() else null
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                        }
                        avatarPath.startsWith("content://") -> {
                            try {
                                val uri = android.net.Uri.parse(avatarPath)
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    stream.readBytes()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to read content URI: $avatarPath", e)
                                null
                            }
                        }
                        avatarPath.startsWith("/") -> {
                            val file = java.io.File(avatarPath)
                            if (file.exists()) file.readBytes() else null
                        }
                        else -> null
                    }
                    bytes?.let { data ->
                        android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load avatar for img2img: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Get image generation status for conversation
     */
    suspend fun getImageGenerationStatus(conversationId: String): String {
        return try {
            buildString {
                appendLine("=== Image Generation Status ===")
                appendLine("Conversation: $conversationId")
                appendLine("Status: Active")
            }
        } catch (e: Exception) {
            "Error getting image generation status: ${e.message}"
        }
    }
    
    private suspend fun generateImageWithEditing(
        conversationId: String,
        prompt: String,
        inputImageBase64: String,
        character: Character?
    , messageId: String? = null): Result<MessageResponse> {
        return try {
            val preferences = dataStore.data.first()
            val imageEditingProvider = preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_provider")] ?: "Replicate"
            
            val apiKey = when (imageEditingProvider) {
                "Replicate" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_api_key")] ?: ""
                "Together AI" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("together_ai_editing_api_key")] ?: ""
                "Modelslab" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_api_key")] ?: ""
                "ComfyUI" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_editing_endpoint")] ?: ""
                else -> ""
            }
            
            if (apiKey.isBlank() && imageEditingProvider != "custom api") {
                return Result.failure(Exception("No API key/endpoint configured for $imageEditingProvider image editing. Please configure in Settings → Image Editing."))
            }
            
            when (imageEditingProvider) {
                "Replicate" -> {
                    val editingModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_model")] ?: "qwen-image-edit"
                    
                    val editingRequest = ImageEditingRequest(
                        imageBase64 = inputImageBase64,
                        prompt = prompt,
                        goFast = true,
                        outputFormat = "webp",
                        enhancePrompt = false,
                        outputQuality = 80
                    )
                    
                    val editingResult = imageEditingService.editImage(
                        provider = "Replicate",
                        apiKey = apiKey,
                        request = editingRequest,
                        model = editingModel
                    )
                    
                    editingResult.fold(
                        onSuccess = { result ->
                            val localImageUrl = downloadAndSaveImage(result.imageUrl, conversationId)
                            
                            val imageMessage = MessageResponse(
                                conversationId = conversationId,
                                id = messageId ?: com.vortexai.android.utils.IdGenerator.generateMessageId(),
                                content = "Edited image: $prompt",
                                senderType = MessageSenderType.SYSTEM,
                                senderId = "system",
                                senderName = "Image Editor",
                                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                                messageType = MessageType.IMAGE,
                                metadata = MessageResponseMetadata(
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    modelUsed = editingModel
                                )
                            )
                            
                            saveImageMessageToDatabase(conversationId, prompt, localImageUrl ?: result.imageUrl, 
                                com.vortexai.android.domain.service.ImageGenerationResult(
                                    success = result.success,
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    model = editingModel
                                )
                            , messageId = messageId)
                            
                            
                            Result.success(imageMessage)
                        },
                        onFailure = { exception ->
                            Result.failure(exception)
                        }
                    )
                }
                "Together AI" -> {
                    val editingModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_model")] ?: "black-forest-labs/FLUX.1-kontext-dev"
                    val editingStrength = when (preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_strength")] ?: "Medium (0.5)") {
                        "Low (0.3)" -> 0.3f
                        "Medium (0.5)" -> 0.5f
                        "High (0.7)" -> 0.7f
                        "Maximum (0.9)" -> 0.9f
                        else -> 0.5f
                    }
                    
                    val editingRequest = ImageEditingRequest(
                        imageBase64 = inputImageBase64,
                        prompt = prompt
                    )
                    
                    val editingResult = imageEditingService.editImage(
                        provider = "Together AI",
                        apiKey = apiKey,
                        request = editingRequest,
                        model = editingModel,
                        strength = editingStrength
                    )
                    
                    editingResult.fold(
                        onSuccess = { result ->
                            val localImageUrl = downloadAndSaveImage(result.imageUrl, conversationId)
                            
                            val imageMessage = MessageResponse(
                                conversationId = conversationId,
                                id = messageId ?: com.vortexai.android.utils.IdGenerator.generateMessageId(),
                                content = "Edited image: $prompt",
                                senderType = MessageSenderType.SYSTEM,
                                senderId = "system",
                                senderName = "Image Editor",
                                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                                messageType = MessageType.IMAGE,
                                metadata = MessageResponseMetadata(
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    modelUsed = editingModel
                                )
                            )
                            
                            saveImageMessageToDatabase(conversationId, prompt, localImageUrl ?: result.imageUrl, 
                                com.vortexai.android.domain.service.ImageGenerationResult(
                                    success = result.success,
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    model = editingModel
                                )
                            , messageId = messageId)
                            
                            
                            Result.success(imageMessage)
                        },
                        onFailure = { exception ->
                            Result.failure(exception)
                        }
                    )
                }
                "Modelslab" -> {
                    val editingModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_model")] ?: "flux-kontext-dev"
                    val editingStrength = when (preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_strength")] ?: "High (0.7)") {
                        "Low (0.3)" -> 0.3f
                        "Medium (0.5)" -> 0.5f
                        "High (0.7)" -> 0.7f
                        "Maximum (0.9)" -> 0.9f
                        else -> 0.7f
                    }
                    
                    val editingRequest = ImageEditingRequest(
                        imageBase64 = inputImageBase64,
                        prompt = prompt
                    )
                    
                    val editingResult = imageEditingService.editImage(
                        provider = "Modelslab",
                        apiKey = apiKey,
                        request = editingRequest,
                        model = editingModel,
                        strength = editingStrength
                    )
                    
                    editingResult.fold(
                        onSuccess = { result ->
                            val localImageUrl = downloadAndSaveImage(result.imageUrl, conversationId)
                            
                            val imageMessage = MessageResponse(
                                conversationId = conversationId,
                                id = messageId ?: com.vortexai.android.utils.IdGenerator.generateMessageId(),
                                content = "Edited image: $prompt",
                                senderType = MessageSenderType.SYSTEM,
                                senderId = "system",
                                senderName = "Image Editor",
                                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                                messageType = MessageType.IMAGE,
                                metadata = MessageResponseMetadata(
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    modelUsed = editingModel
                                )
                            )
                            
                            saveImageMessageToDatabase(conversationId, prompt, localImageUrl ?: result.imageUrl, 
                                com.vortexai.android.domain.service.ImageGenerationResult(
                                    success = result.success,
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    model = editingModel
                                )
                            , messageId = messageId)
                            
                            
                            Result.success(imageMessage)
                        },
                        onFailure = { exception ->
                            Result.failure(exception)
                        }
                    )
                }
                "ComfyUI" -> {
                    var workflowJson = preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_editing_workflow")] ?: ""
                    // Fallback to the requested Flux Klein workflow if empty
                    if (workflowJson.isBlank() || workflowJson == "Flux2-Klein Image Edit") {
                        workflowJson = "Flux2-Klein Image Edit"
                    } else if (workflowJson.endsWith(".json", ignoreCase = true)) {
                        try {
                            val workflowsDir = java.io.File(context.filesDir, "comfy_workflows")
                            val file = java.io.File(workflowsDir, workflowJson)
                            if (file.exists()) {
                                workflowJson = file.readText()
                            }
                        } catch (e: Exception) {
                            Log.e("ChatImageGenerator", "Failed to read ComfyUI workflow: ${e.message}")
                        }
                    }
                    
                    val checkpointOverride = preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_editing_checkpoint")]
                    
                    val editingRequest = ImageEditingRequest(
                        imageBase64 = inputImageBase64,
                        prompt = prompt,
                        checkpointOverride = if (checkpointOverride.isNullOrBlank()) null else checkpointOverride
                    )
                    
                    val editingResult = imageEditingService.editImage(
                        provider = "comfyui",
                        apiKey = apiKey, // Endpoint
                        request = editingRequest,
                        model = workflowJson // JSON Payload
                    )
                    
                    editingResult.fold(
                        onSuccess = { result ->
                            val localImageUrl = downloadAndSaveImage(result.imageUrl, conversationId)
                            
                            val imageMessage = MessageResponse(
                                conversationId = conversationId,
                                id = messageId ?: com.vortexai.android.utils.IdGenerator.generateMessageId(),
                                content = "Edited image: $prompt",
                                senderType = MessageSenderType.SYSTEM,
                                senderId = "system",
                                senderName = "Image Editor (ComfyUI)",
                                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                                messageType = MessageType.IMAGE,
                                metadata = MessageResponseMetadata(
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    modelUsed = "ComfyUI"
                                )
                            )
                            
                            saveImageMessageToDatabase(conversationId, prompt, localImageUrl ?: result.imageUrl, 
                                com.vortexai.android.domain.service.ImageGenerationResult(
                                    success = result.success,
                                    imageUrl = localImageUrl ?: result.imageUrl,
                                    generationTime = result.generationTime,
                                    model = "ComfyUI"
                                )
                            , messageId = messageId)
                            
                            
                            Result.success(imageMessage)
                        },
                        onFailure = { exception ->
                            Result.failure(exception)
                        }
                    )
                }
                "custom api" -> {
                    val selectedProviderId = preferences[androidx.datastore.preferences.core.stringPreferencesKey("selected_custom_image_edit_provider_id")]
                    if (selectedProviderId.isNullOrBlank()) {
                        return Result.failure(Exception("No custom image editing provider selected. Please select one in Settings → Image Editing."))
                    }
                    
                    val provider = customApiProviderRepository.getProviderById(selectedProviderId)
                        ?: return Result.failure(Exception("Custom image editing provider not found"))
                    
                    if (!provider.isEnabled) {
                        return Result.failure(Exception("Custom provider '${provider.name}' is disabled"))
                    }
                    
                    val endpoint = customApiProviderRepository.getEndpointByPurpose(provider.id, "image_edit")
                        ?: return Result.failure(Exception("No image editing endpoint found for provider '${provider.name}'"))
                    
                    val models = customApiProviderRepository.getActiveModelsByProvider(provider.id)
                    val model = models.first().firstOrNull()
                        ?: return Result.failure(Exception("No active model found for provider '${provider.name}'"))
                    
                    val savedParams = customApiProviderRepository.getParameterValuesMap(model.id)
                    val requestParams = mutableMapOf<String, Any>(
                        "prompt" to prompt,
                        "image" to inputImageBase64
                    )
                    requestParams.putAll(savedParams)
                    
                    val startTime = System.currentTimeMillis()
                    val executeResult = customApiExecutor.executeRequest(provider, endpoint, model, requestParams)
                    
                    executeResult.fold(
                        onSuccess = { responseJson ->
                            val parseResult = customApiExecutor.parseResponse(responseJson, endpoint)
                            parseResult.fold(
                                onSuccess = { imageUrl ->
                                    val generationTime = System.currentTimeMillis() - startTime
                                    val localImageUrl = downloadAndSaveImage(imageUrl, conversationId)
                                    
                                    val imageMessage = MessageResponse(
                                        conversationId = conversationId,
                                id = messageId ?: com.vortexai.android.utils.IdGenerator.generateMessageId(),
                                        content = "Edited image: $prompt",
                                        senderType = MessageSenderType.SYSTEM,
                                        senderId = "system",
                                        senderName = "Image Editor",
                                        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                                        messageType = MessageType.IMAGE,
                                        metadata = MessageResponseMetadata(
                                            imageUrl = localImageUrl ?: imageUrl,
                                            generationTime = generationTime,
                                            modelUsed = model.modelId
                                        )
                                    )
                                    
                                    saveImageMessageToDatabase(conversationId, prompt, localImageUrl ?: imageUrl,
                                        com.vortexai.android.domain.service.ImageGenerationResult(
                                            success = true,
                                            imageUrl = localImageUrl ?: imageUrl,
                                            generationTime = generationTime,
                                            model = model.modelId
                                        )
                                    , messageId = messageId)
                                    
                                    
                            Result.success(imageMessage)
                                },
                                onFailure = { exception ->
                                    Result.failure(exception)
                                }
                            )
                        },
                        onFailure = { exception ->
                            Result.failure(exception)
                        }
                    )
                }
                else -> {
                    Result.failure(Exception("Unsupported image editing provider: $imageEditingProvider"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun downloadAndSaveImage(cloudUrl: String?, conversationId: String): String? {
        if (cloudUrl.isNullOrBlank()) return null
        
        return try {
            withContext(Dispatchers.IO) {
                val imageId = "${conversationId}_${System.currentTimeMillis()}"
                val request = okhttp3.Request.Builder().url(cloudUrl).build()
                val response = okhttp3.OkHttpClient().newCall(request).execute()
                
                if (response.isSuccessful) {
                    val imageBytes = response.body?.bytes()
                    imageBytes?.let {
                        imageStorageHelper.saveCharacterImage(imageId, it)
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and save image: ${e.message}")
            null
        }
    }
    
    suspend fun retryImageGeneration(conversationId: String, prompt: String, character: Character?): Result<MessageResponse> {
        Log.d(TAG, "Retrying image generation for prompt: $prompt")
        return generateImageWithChatSettings(conversationId, prompt, character)
    }
}
package com.vortexai.android.ui.screens.chat

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vortexai.android.data.models.*
import com.vortexai.android.domain.service.ImageEditingService
import com.vortexai.android.domain.service.ImageEditingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VortexImageGenerator @Inject constructor(
    private val imageEditingService: com.vortexai.android.domain.service.ImageEditingService,
    private val dataStore: DataStore<Preferences>,
    private val imageStorageHelper: com.vortexai.android.utils.ImageStorageHelper,
    private val chatRepository: com.vortexai.android.data.repository.ChatRepository
) {
    
    companion object {
        private const val TAG = "VortexImageGenerator"
    }
    
    suspend fun generateVortexImage(
        conversationId: String,
        aiResponse: String,
        character: Character?
    ): Result<MessageResponse> {
        return try {
            Log.d(TAG, "Generating Vortex image for AI response: ${aiResponse.take(100)}...")
            
            val imagePrompt = createImagePromptFromResponse(aiResponse, character)
            if (imagePrompt.isBlank()) {
                return Result.failure(Exception("Could not generate image prompt from AI response"))
            }
            
            val inputImageBase64 = getCharacterAvatarAsBase64(character)
            if (inputImageBase64 == null) {
                return Result.failure(Exception("No character avatar available for image editing"))
            }
            
            val preferences = dataStore.data.first()
            val imageEditingProvider = preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_provider")] ?: "Replicate"
            
            val apiKey = when (imageEditingProvider) {
                "Replicate" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_api_key")] ?: ""
                "Together AI" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("together_ai_editing_api_key")] ?: ""
                "Modelslab" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_api_key")] ?: ""
                else -> ""
            }
            
            if (apiKey.isBlank()) {
                return Result.failure(Exception("No API key configured for $imageEditingProvider. Please configure in Settings → Image Editing."))
            }
            
            val editingRequest = ImageEditingRequest(
                imageBase64 = inputImageBase64,
                prompt = imagePrompt,
                goFast = true,
                outputFormat = "webp",
                enhancePrompt = false,
                outputQuality = 80
            )
            
            val result = imageEditingService.editImage(
                provider = imageEditingProvider,
                apiKey = apiKey,
                request = editingRequest
            )
            
            result.fold(
                onSuccess = { editResult ->
                    val localImageUrl = downloadAndSaveImage(editResult.imageUrl, conversationId)
                    
                    val imageMessage = MessageResponse(
                        conversationId = conversationId,
                        content = "Vortex Mode: Generated image based on AI response",
                        senderType = MessageSenderType.SYSTEM,
                        senderId = "vortex",
                        senderName = "Vortex Mode",
                        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                        messageType = MessageType.IMAGE,
                        metadata = MessageResponseMetadata(
                            imageUrl = localImageUrl ?: editResult.imageUrl,
                            generationTime = editResult.generationTime,
                            modelUsed = "vortex-image-edit"
                        )
                    )
                    
                    // Save to database
                    saveImageToDatabase(conversationId, localImageUrl ?: editResult.imageUrl, editResult.generationTime)
                    
                    Result.success(imageMessage)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Vortex image editing failed", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in Vortex image generation", e)
            Result.failure(e)
        }
    }
    
    private fun createImagePromptFromResponse(aiResponse: String, character: Character?): String {
        val sceneDescription = extractSceneFromResponse(aiResponse)
        return if (sceneDescription.isNotBlank()) {
            "$sceneDescription, high quality, detailed"
        } else {
            "${character?.name ?: "character"} portrait, high quality"
        }
    }
    
    private fun extractSceneFromResponse(response: String): String {
        // Look for action words and emotional context
        val actionKeywords = listOf(
            "smiling", "laughing", "crying", "angry", "happy", "sad", "excited",
            "walking", "running", "sitting", "standing", "dancing", "fighting",
            "looking", "staring", "gazing", "watching", "sleeping", "eating"
        )
        
        val locationKeywords = listOf(
            "room", "bedroom", "kitchen", "garden", "forest", "beach", "city",
            "school", "office", "park", "cafe", "restaurant", "home", "outside"
        )
        
        val foundActions = mutableListOf<String>()
        val foundLocations = mutableListOf<String>()
        val words = response.lowercase().split(" ", ",", ".", ";", ":")
        
        for (word in words) {
            if (actionKeywords.any { keyword -> word.contains(keyword) }) {
                foundActions.add(word.trim())
            }
            if (locationKeywords.any { keyword -> word.contains(keyword) }) {
                foundLocations.add(word.trim())
            }
        }
        
        val scene = buildString {
            if (foundActions.isNotEmpty()) {
                append(foundActions.first())
            }
            if (foundLocations.isNotEmpty()) {
                if (isNotEmpty()) append(" in ")
                append(foundLocations.first())
            }
        }
        
        return scene.ifBlank { "portrait, character interaction" }
    }
    
    private suspend fun getCharacterAvatarAsBase64(character: Character?): String? {
        val avatarPath = character?.avatarUrl ?: return null
        
        return if (avatarPath.startsWith("data:image")) {
            avatarPath.substringAfter(",")
        } else {
            try {
                withContext(Dispatchers.IO) {
                    val bytes: ByteArray? = when {
                        avatarPath.startsWith("http", ignoreCase = true) -> {
                            val request = okhttp3.Request.Builder().url(avatarPath).build()
                            okhttp3.OkHttpClient().newCall(request).execute().use { it.body?.bytes() }
                        }
                        avatarPath.startsWith("/") -> {
                            val file = java.io.File(avatarPath)
                            if (file.exists()) file.readBytes() else null
                        }
                        else -> null
                    }
                    bytes?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load avatar: ${e.message}")
                null
            }
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
    
    private suspend fun saveImageToDatabase(conversationId: String, imageUrl: String?, generationTime: Long) {
        try {
            val metadataJson = org.json.JSONObject().apply {
                put("localPath", imageUrl ?: "")
                put("generationTime", generationTime.toString())
                put("modelUsed", "vortex-image-edit")
            }.toString()

            val messageId = com.vortexai.android.utils.IdGenerator.generateMessageId()

            val imageMessageEntity = com.vortexai.android.data.models.Message(
                id = messageId,
                conversationId = conversationId,
                content = "Vortex Mode: Generated image based on AI response",
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
            Log.e(TAG, "Error saving Vortex image to database", e)
        }
    }
}
package com.vortexai.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vortexai.android.MainActivity
import com.vortexai.android.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class GenerationService : Service() {
    
    @Inject
    lateinit var notificationManager: com.vortexai.android.utils.VortexNotificationManager
    
    @Inject
    lateinit var chatLLMService: com.vortexai.android.domain.service.ChatLLMService
    
    @Inject
    lateinit var imageGenerationService: com.vortexai.android.domain.service.ImageGenerationService
    
    @Inject
    lateinit var conversationManager: com.vortexai.android.ui.screens.chat.ChatConversationManager
    
    @Inject
    lateinit var characterRepository: com.vortexai.android.data.repository.CharacterRepository
    
    @Inject
    lateinit var messageDao: com.vortexai.android.data.database.dao.MessageDao
    
    @Inject
    lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    
    @Inject
    lateinit var chatImageGenerator: com.vortexai.android.ui.screens.chat.ChatImageGenerator
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeGenerations = mutableMapOf<String, GenerationInfo>()
    
    companion object {
        private const val CHANNEL_ID = "generation_service"
        private const val NOTIFICATION_ID = 1001
        private const val GENERATION_TIMEOUT = 5 * 60 * 1000L // 5 minutes
        
        const val ACTION_START_AI_GENERATION = "START_AI_GENERATION"
        const val ACTION_START_IMAGE_GENERATION = "START_IMAGE_GENERATION"
        const val ACTION_COMPLETE_GENERATION = "COMPLETE_GENERATION"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        
        const val EXTRA_GENERATION_ID = "generation_id"
        const val EXTRA_CHARACTER_NAME = "character_name"
        const val EXTRA_CHARACTER_ID = "character_id"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_MESSAGE = "user_message"
        const val EXTRA_GENERATION_TYPE = "generation_type"
        const val EXTRA_CONTENT_PREVIEW = "content_preview"
        const val EXTRA_IMAGE_PROMPT = "image_prompt"
        
        fun startAIGeneration(
            context: Context,
            generationId: String,
            conversationId: String,
            characterId: String,
            characterName: String,
            userMessage: String
        ) {
            val intent = Intent(context, GenerationService::class.java).apply {
                action = ACTION_START_AI_GENERATION
                putExtra(EXTRA_GENERATION_ID, generationId)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_CHARACTER_ID, characterId)
                putExtra(EXTRA_CHARACTER_NAME, characterName)
                putExtra(EXTRA_USER_MESSAGE, userMessage)
            }
            context.startService(intent)
        }
        
        fun startImageGeneration(
            context: Context,
            generationId: String,
            conversationId: String,
            characterId: String,
            prompt: String
        ) {
            val intent = Intent(context, GenerationService::class.java).apply {
                action = ACTION_START_IMAGE_GENERATION
                putExtra(EXTRA_GENERATION_ID, generationId)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_CHARACTER_ID, characterId)
                putExtra(EXTRA_IMAGE_PROMPT, prompt)
            }
            context.startService(intent)
        }
        
        fun completeGeneration(context: Context, generationId: String, type: String, content: String) {
            val intent = Intent(context, GenerationService::class.java).apply {
                action = ACTION_COMPLETE_GENERATION
                putExtra(EXTRA_GENERATION_ID, generationId)
                putExtra(EXTRA_GENERATION_TYPE, type)
                putExtra(EXTRA_CONTENT_PREVIEW, content)
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        
        when (intent.action) {
            ACTION_START_AI_GENERATION -> {
                val generationId = intent.getStringExtra(EXTRA_GENERATION_ID) ?: return START_NOT_STICKY
                val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return START_NOT_STICKY
                val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID) ?: return START_NOT_STICKY
                val characterName = intent.getStringExtra(EXTRA_CHARACTER_NAME) ?: "AI"
                val userMessage = intent.getStringExtra(EXTRA_USER_MESSAGE) ?: return START_NOT_STICKY
                
                if (generationId.isBlank() || conversationId.isBlank() || characterId.isBlank()) return START_NOT_STICKY
                
                startForeground(NOTIFICATION_ID, createForegroundNotification("Generating response..."))
                startAIGenerationProcess(generationId, conversationId, characterId, characterName, userMessage)
            }
            ACTION_START_IMAGE_GENERATION -> {
                val generationId = intent.getStringExtra(EXTRA_GENERATION_ID) ?: return START_NOT_STICKY
                val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return START_NOT_STICKY
                val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID) ?: return START_NOT_STICKY
                val prompt = intent.getStringExtra(EXTRA_IMAGE_PROMPT) ?: return START_NOT_STICKY
                
                if (generationId.isBlank() || conversationId.isBlank() || prompt.isBlank()) return START_NOT_STICKY
                
                startForeground(NOTIFICATION_ID, createForegroundNotification("Generating image..."))
                startImageGenerationProcess(generationId, conversationId, characterId, prompt)
            }
            ACTION_COMPLETE_GENERATION -> {
                val generationId = intent.getStringExtra(EXTRA_GENERATION_ID) ?: return START_NOT_STICKY
                val type = intent.getStringExtra(EXTRA_GENERATION_TYPE) ?: "message"
                val content = intent.getStringExtra(EXTRA_CONTENT_PREVIEW) ?: ""
                completeGeneration(generationId, type, "", content)
            }
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
            }
        }
        return START_NOT_STICKY
    }
    
    private data class GenerationInfo(
        val startTime: Long,
        val type: String,
        val characterName: String,
        val job: Job
    )
    
    private fun startAIGenerationProcess(
        generationId: String,
        conversationId: String,
        characterId: String,
        characterName: String,
        userMessage: String
    ) {
        val job = serviceScope.launch {
            try {
                // Timeout protection
                withTimeout(GENERATION_TIMEOUT) {
                    android.util.Log.d("GenerationService", "Starting AI generation")
                    
                    // Generate and save using ConversationManager (handles everything)
                    val saveResult = conversationManager.generateCharacterResponse(
                        conversationId = conversationId,
                        userMessage = userMessage,
                        characterId = characterId,
                        isFirstMessage = false
                    )
                    
                    saveResult.fold(
                        onSuccess = { message ->
                            android.util.Log.d("GenerationService", "AI response saved to database")
                            completeGeneration(generationId, "ai", characterName, message.content)
                        },
                        onFailure = { e ->
                            android.util.Log.e("GenerationService", "Failed to generate/save AI response", e)
                            completeGeneration(generationId, "ai", characterName, "Error: ${e.message}")
                        }
                    )
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.e("GenerationService", "AI generation timed out after 5 minutes")
                completeGeneration(generationId, "ai", characterName, "Error: Generation timed out")
            } catch (e: Exception) {
                android.util.Log.e("GenerationService", "AI generation failed", e)
                completeGeneration(generationId, "ai", characterName, "Error: ${e.message}")
            }
        }
        
        activeGenerations[generationId] = GenerationInfo(System.currentTimeMillis(), "ai", characterName, job)
    }
    
    private fun startImageGenerationProcess(
        generationId: String,
        conversationId: String,
        characterId: String,
        prompt: String
    ) {
        val job = serviceScope.launch {
            try {
                // Timeout protection
                withTimeout(GENERATION_TIMEOUT) {
                    android.util.Log.d("GenerationService", "Starting image-to-image generation with character avatar")
                    
                    // Get character to access avatar
                    val characterResult = characterRepository.getCharacter(characterId).first()
                    val character = characterResult.getOrNull()
                    
                    if (character == null) {
                        android.util.Log.e("GenerationService", "Character not found: $characterId")
                        completeGeneration(generationId, "image", "Image", "Error: Character not found")
                        return@withTimeout
                    }
                    
                    // Use ChatImageGenerator which handles character avatar as source image
                    val imageResult = chatImageGenerator.generateImageWithChatSettings(
                        conversationId = conversationId,
                        prompt = prompt,
                        character = character,
                        messageId = generationId
                    )
                    
                    imageResult.fold(
                        onSuccess = { messageResponse ->
                            android.util.Log.d("GenerationService", "Image generated successfully via ChatImageGenerator")
                            completeGeneration(generationId, "image", "Image", prompt)
                        },
                        onFailure = { e ->
                            android.util.Log.e("GenerationService", "Image generation failed", e)
                            val errorMsg = "Error: ${e.message}"
                            serviceScope.launch {
                                conversationManager.insertMessageDirectly(com.vortexai.android.data.models.Message(
                                    id = generationId,
                                    conversationId = conversationId,
                                    content = errorMsg,
                                    role = "system",
                                    senderType = "system",
                                    timestamp = System.currentTimeMillis(),
                                    messageType = "image",
                                    metadataJson = org.json.JSONObject().apply {
                                        put("modelUsed", "error")
                                    }.toString(),
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                ))
                            }
                            completeGeneration(generationId, "image", "Image", errorMsg)
                        }
                    )
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e("GenerationService", "Image generation timed out")
                val errorMsg = "Error: Generation timed out"
                serviceScope.launch {
                    conversationManager.insertMessageDirectly(com.vortexai.android.data.models.Message(
                        id = generationId,
                        conversationId = conversationId,
                        content = errorMsg,
                        role = "system",
                        senderType = "system",
                        timestamp = System.currentTimeMillis(),
                        messageType = "image",
                        metadataJson = org.json.JSONObject().apply { put("modelUsed", "error") }.toString(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ))
                }
                completeGeneration(generationId, "image", "Image", errorMsg)
            } catch (e: Exception) {
                android.util.Log.e("GenerationService", "Image generation failed", e)
                val errorMsg = "Error: ${e.message}"
                serviceScope.launch {
                    conversationManager.insertMessageDirectly(com.vortexai.android.data.models.Message(
                        id = generationId,
                        conversationId = conversationId,
                        content = errorMsg,
                        role = "system",
                        senderType = "system",
                        timestamp = System.currentTimeMillis(),
                        messageType = "image",
                        metadataJson = org.json.JSONObject().apply { put("modelUsed", "error") }.toString(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ))
                }
                completeGeneration(generationId, "image", "Image", errorMsg)
            }
        }
        
        activeGenerations[generationId] = GenerationInfo(System.currentTimeMillis(), "image", "Image", job)
    }
    

    
    private fun completeGeneration(generationId: String, type: String, characterName: String, content: String) {
        val info = activeGenerations[generationId]
        if (info == null) {
            android.util.Log.w("GenerationService", "Completion for unknown generation")
            return
        }
        
        info.job.cancel()
        activeGenerations.remove(generationId)
        
        serviceScope.launch {
            try {
                when (type) {
                    "ai" -> notificationManager.sendNewMessageNotification(characterName, content.take(100))
                    "image" -> notificationManager.sendImageGenerationNotification(content.take(50))
                }
            } catch (e: Exception) {
                android.util.Log.e("GenerationService", "Notification failed", e)
            }
        }
        
        if (activeGenerations.isEmpty()) {
            stopForegroundService()
        }
    }
    
    private fun stopForegroundService() {
        activeGenerations.values.forEach { it.job.cancel() }
        activeGenerations.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Generation Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing AI and image generation"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createForegroundNotification(message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vortex AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

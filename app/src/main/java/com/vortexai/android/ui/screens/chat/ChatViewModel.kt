package com.vortexai.android.ui.screens.chat

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.models.*
import com.vortexai.android.data.repository.AuthRepository
import com.vortexai.android.data.repository.CharacterRepository
import com.vortexai.android.domain.service.ImageGenerationTracker
import com.vortexai.android.domain.service.ImageGenerationSource
import com.vortexai.android.domain.service.SourceType
import com.vortexai.android.utils.DynamicStatsManager
import com.vortexai.android.utils.LorebookNotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val authRepository: AuthRepository,
    private val imageGenerationTracker: ImageGenerationTracker,
    private val dataStore: DataStore<Preferences>,
    private val dynamicStatsManager: DynamicStatsManager,
    private val lorebookNotificationService: LorebookNotificationService,
    private val conversationManager: ChatConversationManager,
    private val imageGenerator: ChatImageGenerator,
    private val vortexImageGenerator: VortexImageGenerator,
    private val ttsHandler: ChatTTSHandler,
    private val chatLLMService: com.vortexai.android.domain.service.ChatLLMService,
    private val generationServiceHelper: GenerationServiceHelper,
    private val videoGenerationService: com.vortexai.android.domain.service.VideoGenerationService,
    val generationLogger: com.vortexai.android.domain.service.GenerationLogger,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
        private const val TYPING_TIMEOUT = 2000L
        private val AUTO_PLAY_TTS_KEY = booleanPreferencesKey("auto_play_tts")
    }
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    
    private var currentConversationId: String? = null
    private var typingJob: Job? = null
    
    val autoPlayTts: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[AUTO_PLAY_TTS_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val chatBubbleStyle: StateFlow<String> = dataStore.data
        .map { prefs -> prefs[stringPreferencesKey("chat_bubble_style")] ?: "Modern" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Modern")
    

    
    val showCharacterBackground: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[booleanPreferencesKey("show_character_background")] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val characterBackgroundOpacity: StateFlow<Float> = dataStore.data
        .map { prefs -> prefs[floatPreferencesKey("character_background_opacity")] ?: 0.3f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.3f)
    
    val vortexModeEnabled: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[booleanPreferencesKey("vortex_mode_enabled")] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        restoreConversationState()
    }

    fun setAutoPlayTts(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AUTO_PLAY_TTS_KEY] = enabled
            }
        }
    }
    
    fun speakText(text: String, context: Context) {
        viewModelScope.launch {
            ttsHandler.speakText(text, context) { error ->
                showTTSError(error)
            }
        }
    }
    
    fun stopTTS() {
        ttsHandler.stopTTS()
    }
    
    fun testTTS(text: String, context: Context) {
        viewModelScope.launch {
            ttsHandler.testTTS(text, context)
        }
    }
    

    
    fun updateShowCharacterBackground(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[booleanPreferencesKey("show_character_background")] = enabled
            }
        }
    }
    
    fun updateCharacterBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[floatPreferencesKey("character_background_opacity")] = opacity
            }
        }
    }
    
    fun setVortexModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[booleanPreferencesKey("vortex_mode_enabled")] = enabled
            }
        }
    }
    
    fun initializeChat(characterId: String) {
        Log.d(TAG, "🚀 Initializing chat for character: $characterId")
        
        if (characterId.isBlank()) {
            Log.e(TAG, "❌ Character ID is blank")
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid character ID"
                )
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                Log.d(TAG, "🔍 Attempting to load character with ID: '$characterId'")
                characterRepository.getCharacter(characterId).collect { result ->
                    result.fold(
                        onSuccess = { character ->
                            Log.d(TAG, "✅ Successfully loaded character: ${character.name} (ID: ${character.id})")
                            _uiState.update { it.copy(character = character, isLoading = false) }
                            
                            findOrCreateConversation(characterId)
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "❌ Failed to load character with ID '$characterId': ${exception.message}")
                            
                            // Try to get all characters to see what's available
                            try {
                                val allCharacters = characterRepository.getCharacters().first()
                                allCharacters.fold(
                                    onSuccess = { response ->
                                        Log.d(TAG, "📋 Available characters: ${response.characters.map { "${it.name} (${it.id})" }}")
                                    },
                                    onFailure = { e ->
                                        Log.e(TAG, "Failed to get available characters: ${e.message}")
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error checking available characters: ${e.message}")
                            }
                            
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Character not found (ID: $characterId). Please select a different character."
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing chat: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to initialize chat: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun findOrCreateConversation(characterId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "💬 Finding or creating conversation for character: $characterId")
                conversationManager.findOrCreateConversation(characterId).fold(
                    onSuccess = { conversationId ->
                        Log.d(TAG, "✅ Conversation ready: $conversationId")
                        currentConversationId = conversationId
                        _uiState.update { it.copy(isLoading = false) }
                        loadMessages()
                        
                        // Only send character's first message for truly new conversations
                        loadMessages() // Load messages first to check if conversation is empty
                        
                        // Wait a bit for messages to load, then check if we need to send greeting
                        viewModelScope.launch {
                            delay(100) // Small delay to ensure messages are loaded
                            val character = _uiState.value.character
                            if (character != null && _uiState.value.messages.isEmpty()) {
                                Log.d(TAG, "👋 Sending character first message for new conversation: ${character.name}")
                                sendCharacterFirstMessage()
                            } else {
                                Log.d(TAG, "📝 Existing conversation loaded with ${_uiState.value.messages.size} messages")
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "❌ Failed to create conversation: ${exception.message}")
                        
                        if (exception.message?.contains("No access token found") == true ||
                            exception.message?.contains("Authentication required") == true) {
                            Log.d(TAG, "🔑 Attempting guest login...")
                            performGuestLoginAndRetry(characterId)
                        } else {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to create conversation: ${exception.message}"
                                )
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Critical error in findOrCreateConversation: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Critical error: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun loadMessages() {
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            conversationManager.loadMessages(conversationId).fold(
                onSuccess = { messages ->
                    _uiState.update { state ->
                        // Preserve any existing placeholders
                        val placeholders = state.messages.filter { 
                            it.metadata?.modelUsed == "generating_image" || 
                            it.metadata?.modelUsed == "generating_video" 
                        }
                        
                        // Ensure we don't accidentally duplicate placeholders if they were somehow saved
                        val cleanedMessages = messages.filter { 
                            it.metadata?.modelUsed != "generating_image" && 
                            it.metadata?.modelUsed != "generating_video" 
                        }
                        
                        state.copy(
                            messages = cleanedMessages + placeholders,
                            isLoading = false
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to load messages", exception)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load messages: ${exception.message}"
                        )
                    }
                }
            )
        }
    }
    
    fun sendMessage(content: String) {
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            
            try {
                // Check if this is an image generation command
                if (content.startsWith("/image ")) {
                    val prompt = content.removePrefix("/image ").trim()
                    if (prompt.isNotBlank()) {
                        generateImage(prompt)
                        return@launch
                    }
                }
                
                // Check if this is a video generation command
                if (content.startsWith("/video ")) {
                    val prompt = content.removePrefix("/video ").trim()
                    if (prompt.isNotBlank()) {
                        generateVideo(prompt, null)
                        return@launch
                    }
                }
                
                // Process stats for user message
                val character = _uiState.value.character
                if (character?.dynamicStatsEnabled == true) {
                    dynamicStatsManager.processMessage(content)
                }
                
                // Check for lorebook triggers
                character?.let { char ->
                    val lorebookResult = lorebookNotificationService.checkLorebookTriggers(char, content)
                    if (lorebookResult.triggeredEntries.isNotEmpty()) {
                        val notificationMessage = lorebookNotificationService.getNotificationMessage(lorebookResult.triggeredEntries)
                        _uiState.update { it.copy(lorebookNotification = notificationMessage) }
                        
                        viewModelScope.launch {
                            delay(3000)
                            _uiState.update { it.copy(lorebookNotification = null) }
                        }
                    }
                }
                
                conversationManager.sendMessage(
                    conversationId = conversationId,
                    content = content,
                    characterId = _uiState.value.character?.id ?: ""
                ).fold(
                    onSuccess = { message ->
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages + message.toMessageResponse(),
                                isSending = false
                            )
                        }
                        
                        conversationManager.saveConversationState(conversationId)
                        generateCharacterResponse()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to send message", exception)
                        _uiState.update { 
                            it.copy(
                                isSending = false,
                                errorMessage = "Failed to send message: ${exception.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _uiState.update { 
                    it.copy(
                        isSending = false,
                        errorMessage = "Failed to send message: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun generateImage(prompt: String) {
        val conversationId = currentConversationId ?: return
        val character = _uiState.value.character
        
        val generationId = "img_${System.currentTimeMillis()}"
        
        val placeholder = MessageResponse(
            id = generationId,
            conversationId = conversationId,
            content = "Generating image: $prompt",
            senderType = MessageSenderType.SYSTEM,
            senderId = "system",
            senderName = "Image Generator",
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
            messageType = MessageType.IMAGE,
            metadata = MessageResponseMetadata(modelUsed = "generating_image")
        )
        
        _uiState.update { state -> 
            state.copy(
                isSending = false, 
                isGeneratingImage = true,
                messages = state.messages + placeholder
            ) 
        }
        
        val placeholderMessage = Message(
            id = generationId,
            conversationId = conversationId,
            content = "Generating image: $prompt",
            role = "system",
            senderType = "system",
            timestamp = System.currentTimeMillis(),
            messageType = "image",
            metadataJson = org.json.JSONObject().apply {
                put("modelUsed", "generating_image")
            }.toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            conversationManager.insertMessageDirectly(placeholderMessage)
        }
        
        generationServiceHelper.startImageGeneration(
            context = context,
            generationId = generationId,
            conversationId = conversationId,
            characterId = character?.id ?: "",
            prompt = prompt
        )
        
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60
            
            val realMessagesBefore = _uiState.value.messages.count { 
                it.metadata?.modelUsed != "generating_image" && 
                it.metadata?.modelUsed != "generating_video" 
            }
            
            while (attempts < maxAttempts) {
                delay(3000)
                attempts++
                
                loadMessages()
                delay(500)
                
                val currentRealCount = _uiState.value.messages.count { 
                    it.metadata?.modelUsed != "generating_image" && 
                    it.metadata?.modelUsed != "generating_video" 
                }
                
                if (currentRealCount > realMessagesBefore) {
                    Log.d(TAG, "Image generation completed after $attempts attempts")
                    _uiState.update { state ->
                        state.copy(
                            isGeneratingImage = false,
                            messages = state.messages.filter { it.id != generationId }
                        )
                    }
                    viewModelScope.launch {
                        conversationManager.deleteMessage(generationId)
                    }
                    break
                }
            }
            
            _uiState.update { state ->
                state.copy(
                    isGeneratingImage = false,
                    messages = state.messages.filter { it.id != generationId }
                )
            }
        }
    }
    
    private fun sendCharacterFirstMessage() {
        val character = _uiState.value.character ?: return
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isCharacterTyping = true) }
            
            conversationManager.sendCharacterFirstMessage(conversationId, character).fold(
                onSuccess = { message ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + message.toMessageResponse(),
                            isCharacterTyping = false
                        )
                    }
                    loadMessages() // Reload to ensure everything is saved
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to send character first message", exception)
                    _uiState.update { 
                        it.copy(
                            isCharacterTyping = false,
                            errorMessage = "Failed to send greeting: ${exception.message}"
                        )
                    }
                }
            )
        }
    }
    
    private fun generateCharacterResponse() {
        val conversationId = currentConversationId ?: return
        val character = _uiState.value.character ?: return
        
        if (_uiState.value.isCharacterTyping) {
            Log.d(TAG, "Character is already typing, skipping duplicate response generation")
            return
        }
        
        _uiState.update { it.copy(isCharacterTyping = true, errorMessage = null) }
        
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.senderType == MessageSenderType.USER }?.content ?: ""
        val generationId = "ai_${System.currentTimeMillis()}"
        
        generationServiceHelper.startAIGeneration(
            context = context,
            generationId = generationId,
            conversationId = conversationId,
            characterId = character.id,
            characterName = character.name,
            userMessage = lastUserMessage
        )
        
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 90
            
            while (attempts < maxAttempts) {
                delay(2000)
                attempts++
                
                val messagesBefore = _uiState.value.messages.size
                loadMessages()
                delay(500)
                
                if (_uiState.value.messages.size > messagesBefore) {
                    Log.d(TAG, "AI response detected after $attempts attempts")
                    break
                }
            }
            
            _uiState.update { it.copy(isCharacterTyping = false) }
        }
    }
    
    private fun performGuestLoginAndRetry(characterId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting guest login...")
                
                authRepository.guestLogin().collect { result ->
                    result.fold(
                        onSuccess = { 
                            Log.d(TAG, "Guest login successful, retrying conversation creation...")
                            findOrCreateConversation(characterId)
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Guest login failed", exception)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Authentication failed: ${exception.message}"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during guest login", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Authentication error: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun restoreConversationState() {
        viewModelScope.launch {
            val conversationId = conversationManager.restoreConversationState()
            if (conversationId != null && currentConversationId == null) {
                Log.d(TAG, "Restoring conversation state for: $conversationId")
                currentConversationId = conversationId
                refreshMessages()
                checkForPendingResponses()
            }
        }
    }
    
    fun refreshMessages() {
        val conversationId = currentConversationId ?: return
        loadMessages()
    }

    fun checkForPendingResponses() {
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            try {
                // Check if the last message was from user and no character response exists
                val messages = _uiState.value.messages
                val lastMessage = messages.lastOrNull()
                
                if (lastMessage?.senderType == MessageSenderType.USER && !_uiState.value.isCharacterTyping) {
                    Log.d(TAG, "Found pending user message without character response, generating...")
                    generateCharacterResponse()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for pending responses: ${e.message}")
            }
        }
    }
    
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            conversationManager.deleteMessage(messageId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != messageId })
                    }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(errorMessage = "Failed to delete message: ${exception.message}") }
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun hideStats() {
        _uiState.update { it.copy(showStats = false) }
    }
    
    fun hideLorebookNotification() {
        _uiState.update { it.copy(lorebookNotification = null) }
    }
    
    fun hideReplicateDebugInfo() {
        _uiState.update { it.copy(replicateDebugInfo = null) }
    }
    
    fun resumeConversation(conversationId: String) {
        Log.d(TAG, "Resuming conversation: $conversationId")
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                currentConversationId = conversationId
                
                // Load messages for this conversation
                conversationManager.loadMessages(conversationId).fold(
                    onSuccess = { messages ->
                        // Get character ID from the first message or conversation
                        val characterId = messages.firstOrNull { it.senderType == MessageSenderType.CHARACTER }?.senderId
                        
                        if (characterId != null) {
                            // Load the character
                            characterRepository.getCharacter(characterId).collect { result ->
                                result.fold(
                                    onSuccess = { character ->
                                        _uiState.update { 
                                            it.copy(
                                                character = character,
                                                messages = messages,
                                                isLoading = false
                                            )
                                        }
                                    },
                                    onFailure = { exception ->
                                        Log.e(TAG, "Failed to load character for conversation", exception)
                                        _uiState.update { 
                                            it.copy(
                                                messages = messages,
                                                isLoading = false
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            _uiState.update { 
                                it.copy(
                                    messages = messages,
                                    isLoading = false
                                )
                            }
                        }
                        
                        // Save this as the active conversation
                        conversationManager.saveConversationState(conversationId)
                        
                        // Check for any pending responses
                        checkForPendingResponses()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to resume conversation", exception)
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to resume conversation: ${exception.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming conversation", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error resuming conversation: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun applyVideoAvatar(videoUrl: String) {
        val character = _uiState.value.character ?: return
        viewModelScope.launch {
            try {
                // Update the current state immediately for snappier UI
                val updatedChar = character.copy(avatarVideoUrl = videoUrl)
                _uiState.update { it.copy(character = updatedChar) }
                
                // Save to repo
                characterRepository.updateCharacter(updatedChar).fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully updated character avatar video to $videoUrl")
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to apply video avatar to character", e)
                        _uiState.update { it.copy(
                            videoGenerationError = "Failed to update character avatar: ${e.message}"
                        ) }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error applying video avatar", e)
                 _uiState.update { it.copy(
                    videoGenerationError = "Error applying video avatar: ${e.message}"
                ) }
            }
        }
    }
    
    fun animateImageMessage(messageId: String) {
        val message = _uiState.value.messages.find { it.id == messageId }
        val imageUrl = message?.metadata?.imageUrl
        val prompt = message?.content ?: "Animate this character avatar"
        
        if (imageUrl.isNullOrBlank()) {
            _uiState.update { it.copy(videoGenerationError = "Cannot animate: Image URL is missing") }
            return
        }
        
        generateVideo(prompt, imageUrl, messageId)
    }
    
    private fun generateVideo(prompt: String, imageUrl: String?, sourceImageId: String? = null) {
        val conversationId = currentConversationId ?: return
        
        _uiState.update { it.copy(isVideoGenerating = true, videoGenerationError = null) }
        
        val generationId = "vid_${System.currentTimeMillis()}"
        
        val placeholder = MessageResponse(
            id = generationId,
            conversationId = conversationId,
            content = "Generating video: $prompt",
            senderType = MessageSenderType.SYSTEM,
            senderId = "system",
            senderName = "Video Generator",
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
            messageType = MessageType.VIDEO,
            metadata = MessageResponseMetadata(modelUsed = "generating_video", sourceImageId = sourceImageId)
        )
        
        _uiState.update { state -> 
            state.copy(
                isSending = false, 
                isVideoGenerating = true,
                messages = state.messages + placeholder
            ) 
        }
        
        val placeholderMessage = Message(
            id = generationId,
            conversationId = conversationId,
            content = "Generating video: $prompt",
            role = "system",
            senderType = "system",
            timestamp = System.currentTimeMillis(),
            messageType = "video",
            metadataJson = org.json.JSONObject().apply {
                put("modelUsed", "generating_video")
                if (sourceImageId != null) put("sourceImageId", sourceImageId)
            }.toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            conversationManager.insertMessageDirectly(placeholderMessage)
        }
        
        viewModelScope.launch {
            try {
                // Determine model - defaulting to fal-ai for best image-to-video if unconfigured
                val request = com.vortexai.android.domain.service.VideoGenerationRequest(
                    prompt = prompt,
                    imageUrl = imageUrl,
                    model = "" // VideoGenerationService will automatically resolve this from DataStore
                )
                
                val result = videoGenerationService.generateVideo(
                    provider = "", // Auto-resolved
                    apiKey = "",   // Auto-resolved
                    request = request
                )
                
                result.fold(
                    onSuccess = { videoResult ->
                        if (videoResult.success && videoResult.videoUrl != null) {
                            Log.d(TAG, "Video generated successfully: ${videoResult.videoUrl}")
                            
                            // Create video message payload
                            val videoMessage = MessageResponse(
                                id = generationId,
                                conversationId = conversationId,
                                senderId = _uiState.value.character?.id ?: "vortex",
                                senderName = _uiState.value.character?.name ?: "Vortex AI",
                                senderType = MessageSenderType.CHARACTER,
                                content = "Here is the animated video for: $prompt",
                                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                                messageType = MessageType.VIDEO,
                                metadata = MessageResponseMetadata(
                                    videoUrl = videoResult.videoUrl,
                                    modelUsed = videoResult.model,
                                    sourceImageId = sourceImageId
                                )
                            )
                            
                            val finalMessageEntity = Message(
                                id = generationId,
                                conversationId = conversationId,
                                content = videoMessage.content,
                                role = "character",
                                senderType = "character",
                                timestamp = System.currentTimeMillis(),
                                characterId = _uiState.value.character?.id,
                                characterName = _uiState.value.character?.name,
                                messageType = "video",
                                metadataJson = org.json.JSONObject().apply {
                                    put("videoUrl", videoResult.videoUrl)
                                    put("modelUsed", videoResult.model)
                                    if (sourceImageId != null) put("sourceImageId", sourceImageId)
                                }.toString(),
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            
                            conversationManager.insertMessageDirectly(finalMessageEntity)
                            
                            // Add directly to UI state, replacing placeholder
                            _uiState.update { state -> 
                                state.copy(
                                    messages = state.messages.filter { it.id != generationId } + videoMessage,
                                    isVideoGenerating = false
                                )
                            }
                        } else {
                            conversationManager.deleteMessage(generationId)
                            _uiState.update { state -> state.copy(
                                messages = state.messages.filter { it.id != generationId },
                                isVideoGenerating = false,
                                videoGenerationError = "Video generation failed without error"
                            ) }
                        }
                    },
                    onFailure = { exception ->
                        conversationManager.deleteMessage(generationId)
                        _uiState.update { state -> state.copy(
                            messages = state.messages.filter { it.id != generationId },
                            isVideoGenerating = false,
                            videoGenerationError = exception.message ?: "Unknown video generation error"
                        ) }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating video", e)
                conversationManager.deleteMessage(generationId)
                _uiState.update { state -> state.copy(
                    messages = state.messages.filter { it.id != generationId },
                    isVideoGenerating = false,
                    videoGenerationError = "Failed to generate video: ${e.message}"
                ) }
            }
        }
    }
    
    fun checkForCompletedImageGenerations() {
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Checking for completed image generations in conversation: $conversationId")
                // Implementation would check for completed image generations
                // and add them to the conversation
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for completed image generations: ${e.message}")
            }
        }
    }
    
    fun logTTSStatus(context: Context) {
        viewModelScope.launch {
            ttsHandler.debugTTSPreferences(context)
        }
    }
    
    fun forceNewConversation() {
        startNewConversation()
    }
    
    fun clearAllConversationsAndStartNew() {
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            try {
                // Clear messages from current conversation instead of creating new one
                conversationManager.clearConversationMessages(conversationId).fold(
                    onSuccess = {
                        _uiState.update { it.copy(messages = emptyList()) }
                        resetStats()
                        
                        // Send character's first message again
                        val character = _uiState.value.character
                        if (character != null) {
                            sendCharacterFirstMessage()
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(errorMessage = "Failed to clear conversation: ${exception.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Error clearing conversation: ${e.message}")
                }
            }
        }
    }
    
    fun updateTypingStatus(isTyping: Boolean) {
        Log.d(TAG, "Updating typing status: $isTyping")
        
        // Cancel existing typing job if user stops typing
        if (!isTyping) {
            typingJob?.cancel()
            typingJob = null
            return
        }
        
        // Start new typing timeout
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            try {
                delay(TYPING_TIMEOUT)
                
                // If user hasn't typed for TYPING_TIMEOUT, consider stopping typing
                Log.d(TAG, "Typing timeout reached")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in typing status update: ${e.message}")
            }
        }
    }
    
    fun testModelsLabTTS(text: String, context: Context) {
        viewModelScope.launch {
            ttsHandler.testTTS(text, context)
        }
    }
    
    fun testModelsLabAPIRaw(text: String, context: Context) {
        viewModelScope.launch {
            ttsHandler.testTTS(text, context)
        }
    }
    
    fun testElevenMultilingualV2(text: String, context: Context) {
        viewModelScope.launch {
            ttsHandler.testTTS(text, context)
        }
    }
    
    fun debugTTSPreferences(context: Context) {
        viewModelScope.launch {
            ttsHandler.debugTTSPreferences(context)
        }
    }
    
    fun testSystemTTS(text: String, context: Context) {
        viewModelScope.launch {
            ttsHandler.testTTS(text, context)
        }
    }
    
    fun debugReplicateInput(prompt: String, context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Debugging Replicate input with prompt: $prompt")
                
                val debugInfo = StringBuilder()
                debugInfo.append("=== Replicate Debug Info ===\n")
                debugInfo.append("Prompt: $prompt\n")
                
                // Get Replicate settings from DataStore
                val preferences = dataStore.data.first()
                val replicateApiKey = preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_api_key")] ?: ""
                val replicateModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_model")] ?: "flux.1-schnell"
                
                debugInfo.append("API Key: ${if (replicateApiKey.isNotBlank()) "${replicateApiKey.take(8)}..." else "Not set"}\n")
                debugInfo.append("Model: $replicateModel\n")
                debugInfo.append("Prompt Length: ${prompt.length} characters\n")
                
                // Validate prompt
                when {
                    prompt.isBlank() -> debugInfo.append("❌ Error: Prompt is empty\n")
                    prompt.length > 1000 -> debugInfo.append("⚠️ Warning: Prompt is very long (${prompt.length} chars)\n")
                    else -> debugInfo.append("✅ Prompt length is acceptable\n")
                }
                
                // Check API key
                if (replicateApiKey.isBlank()) {
                    debugInfo.append("❌ Error: No Replicate API key configured\n")
                } else {
                    debugInfo.append("✅ API key is configured\n")
                }
                
                val finalDebugInfo = debugInfo.toString()
                Log.d(TAG, finalDebugInfo)
                
                _uiState.update { it.copy(replicateDebugInfo = finalDebugInfo) }
                
                android.widget.Toast.makeText(context, "Replicate debug info logged", android.widget.Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in Replicate debug: ${e.message}")
                android.widget.Toast.makeText(context, "Debug error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    fun debugLLMConnection(context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 Starting LLM connection debug...")
                
                val preferences = dataStore.data.first()
                val llmProvider = preferences[androidx.datastore.preferences.core.stringPreferencesKey("llm_provider")] ?: "Together AI"
                val llmModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("llm_model")] ?: ""
                val apiKey = when (llmProvider) {
                    "Together AI" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("together_ai_api_key")] ?: ""
                    "Gemini API" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("gemini_api_key")] ?: ""
                    "Open Router" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("openrouter_api_key")] ?: ""
                    else -> ""
                }
                
                val debugInfo = StringBuilder()
                debugInfo.append("=== LLM Connection Debug ===\n")
                debugInfo.append("Provider: $llmProvider\n")
                debugInfo.append("Model: $llmModel\n")
                debugInfo.append("API Key: ${if (apiKey.isNotBlank()) "${apiKey.take(8)}..." else "NOT SET"}\n")
                debugInfo.append("API Key Length: ${apiKey.length}\n")
                
                // Test simple LLM call
                try {
                    debugInfo.append("\n--- Testing LLM Call ---\n")
                    val testResponse = chatLLMService.generateResponse(
                        userMessage = "Hello, this is a test message.",
                        character = _uiState.value.character,
                        userName = "TestUser"
                    )
                    debugInfo.append("✅ LLM Test SUCCESS\n")
                    debugInfo.append("Response: ${testResponse.take(100)}...\n")
                } catch (e: Exception) {
                    debugInfo.append("❌ LLM Test FAILED\n")
                    debugInfo.append("Error: ${e.message}\n")
                    debugInfo.append("Exception: ${e.javaClass.simpleName}\n")
                }
                
                val finalDebugInfo = debugInfo.toString()
                Log.d(TAG, finalDebugInfo)
                
                _uiState.update { it.copy(replicateDebugInfo = finalDebugInfo) }
                
                android.widget.Toast.makeText(context, "LLM debug completed - check debug panel", android.widget.Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in LLM debug: ${e.message}")
                android.widget.Toast.makeText(context, "Debug error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    fun testReplicateApiCall(prompt: String, context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing Replicate API call with prompt: $prompt")
                android.widget.Toast.makeText(context, "Testing Replicate API...", android.widget.Toast.LENGTH_SHORT).show()
                
                // Get Replicate settings
                val preferences = dataStore.data.first()
                val replicateApiKey = preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_api_key")] ?: ""
                val replicateModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_model")] ?: "flux.1-schnell"
                
                if (replicateApiKey.isBlank()) {
                    android.widget.Toast.makeText(context, "❌ No Replicate API key configured", android.widget.Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                if (prompt.isBlank()) {
                    android.widget.Toast.makeText(context, "❌ Prompt cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Create a test image generation request
                val testRequest = com.vortexai.android.domain.service.ImageGenerationRequest(
                    prompt = prompt,
                    model = replicateModel,
                    width = 1024,
                    height = 1024,
                    steps = 4,
                    guidanceScale = 3.5f
                )
                
                // Test the API call
                val result = imageGenerator.generateImageWithChatSettings(
                    conversationId = currentConversationId ?: "test",
                    prompt = prompt,
                    character = _uiState.value.character
                )
                
                result.fold(
                    onSuccess = { imageMessage ->
                        Log.d(TAG, "✅ Replicate API test successful")
                        android.widget.Toast.makeText(context, "✅ Replicate API test successful!", android.widget.Toast.LENGTH_LONG).show()
                        
                        // Add the test image to the conversation if we have an active one
                        if (currentConversationId != null) {
                            _uiState.update { state ->
                                state.copy(messages = state.messages + imageMessage)
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "❌ Replicate API test failed: ${exception.message}")
                        android.widget.Toast.makeText(context, "❌ API test failed: ${exception.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in Replicate API test: ${e.message}")
                android.widget.Toast.makeText(context, "Test error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    fun startNewConversation() {
        val characterId = _uiState.value.character?.id ?: return
        
        Log.d(TAG, "Starting new conversation with character: $characterId")
        
        resetStats()
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null, messages = emptyList()) }
        
        viewModelScope.launch {
            conversationManager.forceCreateNewConversation(characterId).fold(
                onSuccess = { conversationId ->
                    currentConversationId = conversationId
                    _uiState.update { it.copy(isLoading = false) }
                    
                    val character = _uiState.value.character
                    if (character != null) {
                        sendCharacterFirstMessage()
                    }
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to create new conversation: ${exception.message}"
                        )
                    }
                }
            )
        }
    }
    
    private fun resetStats() {
        val character = _uiState.value.character
        if (character?.dynamicStatsEnabled == true) {
            dynamicStatsManager.resetStats()
            _uiState.update { 
                it.copy(
                    currentStats = dynamicStatsManager.getCurrentStats(),
                    showStats = false
                )
            }
        } else {
            _uiState.update { 
                it.copy(showStats = false)
            }
        }
    }
    
    private fun generateVortexImage(conversationId: String, aiResponse: String, character: Character?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Generating Vortex Mode image for AI response")
                
                // Add generating indicator
                _uiState.update { it.copy(isGeneratingImage = true) }
                
                vortexImageGenerator.generateVortexImage(conversationId, aiResponse, character).fold(
                    onSuccess = { imageMessage ->
                        Log.d(TAG, "Vortex image generated successfully")
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages + imageMessage,
                                isGeneratingImage = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Vortex image generation failed: ${exception.message}")
                        _uiState.update { it.copy(isGeneratingImage = false) }
                        showVortexImageError(exception.message ?: "Unknown error occurred")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in Vortex image generation: ${e.message}")
                _uiState.update { it.copy(isGeneratingImage = false) }
                showVortexImageError(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    private fun showVortexImageError(errorMessage: String) {
        // Update UI state to show error popup
        _uiState.update { state ->
            state.copy(vortexImageError = "Vortex Mode Image Generation Failed: $errorMessage")
        }
        
        // Auto-hide error after 5 seconds
        viewModelScope.launch {
            delay(5000)
            _uiState.update { state ->
                state.copy(vortexImageError = null)
            }
        }
    }
    
    fun dismissVortexImageError() {
        _uiState.update { state ->
            state.copy(vortexImageError = null)
        }
    }
    
    private fun showTTSError(errorMessage: String) {
        _uiState.update { state ->
            state.copy(ttsError = errorMessage)
        }
        
        viewModelScope.launch {
            delay(5000)
            _uiState.update { state ->
                state.copy(ttsError = null)
            }
        }
    }
    
    fun dismissTTSError() {
        _uiState.update { state ->
            state.copy(ttsError = null)
        }
    }
    
    fun dismissImageGenerationError() {
        _uiState.update { state ->
            state.copy(imageGenerationError = null)
        }
    }
    
    fun dismissVideoGenerationError() {
        _uiState.update { state ->
            state.copy(videoGenerationError = null)
        }
    }
    
    fun dismissLLMResponseError() {
        _uiState.update { state ->
            state.copy(llmResponseError = null)
        }
    }
    
    fun generateUserPromptFromAIResponse() {
        viewModelScope.launch {
            try {
                val lastAIMessage = _uiState.value.messages.lastOrNull { it.senderType == MessageSenderType.CHARACTER }
                if (lastAIMessage == null) {
                    Log.e(TAG, "No AI message found to generate prompt from")
                    return@launch
                }
                
                _uiState.update { it.copy(isGeneratingUserPrompt = true) }
                
                val systemPrompt = "You are the user in a conversation. The AI character just said: '${lastAIMessage.content}'. Write a short, natural reply as the user (max 50 words). Respond in first person as if you are the user talking to the character. Only return the user's message, nothing else."
                
                val generatedPrompt = chatLLMService.generateResponse(
                    userMessage = systemPrompt,
                    character = null,
                    userName = "User"
                )
                
                _uiState.update { state ->
                    state.copy(
                        isGeneratingUserPrompt = false,
                        generatedUserPrompt = generatedPrompt.trim()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating user prompt: ${e.message}")
                _uiState.update { state ->
                    state.copy(
                        isGeneratingUserPrompt = false,
                        llmResponseError = "Failed to generate prompt: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearGeneratedPrompt() {
        _uiState.update { it.copy(generatedUserPrompt = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        val conversationId = currentConversationId
        if (conversationId != null) {
            viewModelScope.launch {
                conversationManager.saveConversationState(conversationId)
            }
        }
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isCharacterTyping: Boolean = false,
    val isGeneratingImage: Boolean = false,
    val isGeneratingUserPrompt: Boolean = false,
    val character: Character? = null,
    val conversation: ConversationResponse? = null,
    val messages: List<MessageResponse> = emptyList(),
    val errorMessage: String? = null,
    val currentStats: com.vortexai.android.utils.CharacterStats = com.vortexai.android.utils.CharacterStats(),
    val showStats: Boolean = false,
    val lorebookNotification: String? = null,
    val replicateDebugInfo: String? = null,
    val vortexImageError: String? = null,
    val ttsError: String? = null,
    val imageGenerationError: String? = null,
    val llmResponseError: String? = null,
    val generatedUserPrompt: String? = null,
    val isVideoGenerating: Boolean = false,
    val videoGenerationError: String? = null
)
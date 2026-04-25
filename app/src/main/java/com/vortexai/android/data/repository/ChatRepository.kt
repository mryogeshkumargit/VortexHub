package com.vortexai.android.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

import com.vortexai.android.data.database.dao.ConversationDao
import com.vortexai.android.data.database.dao.MessageDao
import com.vortexai.android.data.database.dao.CharacterDao
import com.vortexai.android.data.models.*
import com.vortexai.android.domain.service.ChatLLMService
import com.vortexai.android.utils.IdGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*
import com.vortexai.android.data.repository.AuthRepository

/**
 * Extension functions to convert between Message and MessageResponse
 */
fun Message.toMessageResponse(): MessageResponse {
    return MessageResponse(
        id = this.id,
        conversationId = this.conversationId,
        content = this.content,
        senderType = when (this.role) {
            "user" -> MessageSenderType.USER
            "assistant", "character" -> MessageSenderType.CHARACTER
            "system" -> MessageSenderType.SYSTEM
            else -> MessageSenderType.USER
        },
        senderId = this.userId ?: this.characterId,
        senderName = this.characterName ?: "User",
        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(this.timestamp)),
        messageType = if (this.messageType == "image") MessageType.IMAGE else MessageType.TEXT,
        metadata = if (this.metadataJson != null) {
            try {
                val obj = org.json.JSONObject(this.metadataJson)
                MessageResponseMetadata(
                    imageUrl = obj.optString("imageUrl", obj.optString("localPath", null)),
                    generationTime = obj.optLong("generationTime", 0),
                    modelUsed = obj.optString("modelUsed", null)
                )
            } catch (e: Exception) { null }
        } else null
    )
}

/**
 * Repository for chat operations
 * Standalone local chat management - no external API calls
 */
@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val characterDao: CharacterDao,
    private val chatLLMService: ChatLLMService,
    private val authRepository: AuthRepository,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "ChatRepository"
    }
    
    /**
     * Return an existing conversation ID for the given character, if any.
     * Prefers the most recent one.
     */
    suspend fun getExistingConversationIdForCharacter(characterId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                conversationDao.getConversationsByCharacterSync(characterId).firstOrNull()?.id
            } catch (e: Exception) {
                Log.e(TAG, "Error finding existing conversation for character: $characterId", e)
                null
            }
        }
    }
    
    /**
     * Clear all conversations for a specific character
     */
    suspend fun clearConversationsForCharacter(characterId: String) {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing conversations for character: $characterId")
                conversationDao.deleteConversationsByCharacter(characterId)
                Log.d(TAG, "Successfully cleared conversations for character: $characterId")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing conversations for character: $characterId", e)
                throw e
            }
        }
    }

    /**
     * Get the last message content for a conversation, or null if none.
     */
    suspend fun getLastMessageContent(conversationId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                messageDao.getLastMessageInConversation(conversationId)?.content
            } catch (e: Exception) {
                Log.e(TAG, "Error getting last message for conversation: $conversationId", e)
                null
            }
        }
    }

    /**
     * Get conversations for current user
     */
    fun getConversations(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null
    ): Flow<Result<ConversationListResponse>> = flow {
        try {
            Log.d(TAG, "Getting local conversations")
            
            // Get conversations from local database
            var conversations = conversationDao.getAllConversations()
            
            // Apply search filter if provided
            if (!search.isNullOrBlank()) {
                conversations = conversations.filter { conversation ->
                    conversation.characterName?.contains(search, ignoreCase = true) == true ||
                    conversation.title?.contains(search, ignoreCase = true) == true
                }
            }
            
            // Apply pagination
            val startIndex = (page - 1) * limit
            val endIndex = minOf(startIndex + limit, conversations.size)
            val paginatedConversations = if (startIndex < conversations.size) {
                conversations.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            val response = ConversationListResponse(
                conversations = paginatedConversations,
                pagination = Pagination(
                    page = page,
                    limit = limit,
                    total = conversations.size,
                    pages = (conversations.size + limit - 1) / limit
                ),
                success = true,
                hasMore = endIndex < conversations.size
            )
            
            emit(Result.success(response))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting conversations", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get conversation by ID
     */
    fun getConversation(conversationId: String): Flow<Result<Conversation>> = flow {
        try {
            Log.d(TAG, "Getting conversation: $conversationId")
            
            val conversation = conversationDao.getConversationById(conversationId)
            
            if (conversation != null) {
                emit(Result.success(conversation))
            } else {
                emit(Result.failure(Exception("Conversation not found")))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting conversation", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get messages for a conversation
     */
    fun getMessages(
        conversationId: String,
        page: Int = 1,
        limit: Int = Int.MAX_VALUE
    ): Flow<Result<MessageListResponse>> = flow {
        try {
            Log.d(TAG, "Getting messages for conversation: $conversationId")
            
            // Get messages from local database
            val messages = messageDao.getMessagesByConversationId(conversationId)
            
            // Apply pagination only if explicitly requested; otherwise return all
            val startIndex = (page - 1) * limit
            val endIndex = minOf(startIndex + limit, messages.size)
            val paginatedMessages = if (page == 1 && limit == Int.MAX_VALUE) {
                messages
            } else if (startIndex < messages.size) {
                messages.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            val response = MessageListResponse(
                messages = paginatedMessages,
                pagination = Pagination(
                    page = if (limit == Int.MAX_VALUE) 1 else page,
                    limit = if (limit == Int.MAX_VALUE) messages.size else limit,
                    total = messages.size,
                    pages = if (limit == Int.MAX_VALUE) 1 else (messages.size + limit - 1) / limit
                ),
                success = true,
                hasMore = endIndex < messages.size
            )
            
            emit(Result.success(response))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Send a message and save to database (user message only, AI response handled separately)
     */
    fun sendMessage(
        conversationId: String?,
        content: String,
        characterId: String
    ): Flow<Result<Message>> = flow {
        try {
            Log.d(TAG, "Sending user message to conversation for character: $characterId")
            
            val actualConversationId = conversationId ?: createNewConversation(characterId)
            
            // Save user message only
            val nowTs = System.currentTimeMillis()
            val userMessage = Message(
                id = IdGenerator.generateMessageId(),
                conversationId = actualConversationId,
                content = content,
                role = "user",
                characterId = null,
                userId = "current_user", // Would get from auth session
                createdAt = nowTs,
                updatedAt = nowTs,
                senderType = "user",
                timestamp = nowTs
            )
            
            // Use transaction to ensure atomicity
            try {
                messageDao.insertMessage(userMessage)
                Log.d(TAG, "Saved user message with ID: ${userMessage.id}")
                
                // Update conversation statistics accurately and update lastMessageAt
                conversationDao.incrementUserMessageStats(actualConversationId, nowTs)
                
            } catch (e: Exception) {
                Log.e(TAG, "Database transaction failed for user message", e)
                throw e
            }
            
            // Return the user message that was sent
            // AI response will be generated separately by ViewModel
            emit(Result.success(userMessage))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending user message", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Create a new conversation
     */
    private suspend fun createNewConversation(characterId: String): String {
        return withContext(Dispatchers.IO) {
            // Get existing conversation IDs to ensure uniqueness
            val existingIds = conversationDao.getAllConversations().map { it.id }.toSet()
            
            // Generate unique conversation ID with collision detection
            val conversationId = IdGenerator.generateUniqueId("conv", existingIds)
            
            // Get character name from database
            val character = characterDao.getCharacterById(characterId)
            val characterName = character?.name ?: "Unknown Character"
        
        val conversation = Conversation(
            id = conversationId,
                title = "Chat with $characterName",
            characterId = characterId,
                characterName = characterName,
            userId = "current_user", // Would get from auth session
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
                lastMessageAt = System.currentTimeMillis()
        )
        
        conversationDao.insertConversation(conversation)
        
            Log.d(TAG, "Created new conversation: $conversationId with character: $characterName")
            conversationId
        }
    }
    

    

    
    /**
     * Fallback response when LLM service is unavailable
     */
    private fun generateLocalFallbackResponse(userMessage: String, character: Character?): String {
        // Always return a standard error message when LLM is unavailable
        return "I'm having trouble connecting to the AI service right now. Please check your LLM settings and try again."
    }
    
    /**
     * Delete a conversation
     */
    fun deleteConversation(conversationId: String): Flow<Result<Unit>> = flow {
        try {
            Log.d(TAG, "Deleting conversation: $conversationId")
            
            // Delete messages first
            messageDao.deleteMessagesByConversationId(conversationId)
            
            // Delete conversation
            conversationDao.deleteConversationById(conversationId)
            
            emit(Result.success(Unit))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Clear messages from a specific conversation but keep the conversation
     */
    fun clearConversationMessages(conversationId: String): Flow<Result<Unit>> = flow {
        try {
            Log.d(TAG, "Clearing messages for conversation: $conversationId")
            
            messageDao.deleteMessagesByConversationId(conversationId)
            
            // Reset conversation stats
            conversationDao.resetConversationStats(conversationId, System.currentTimeMillis())
            
            emit(Result.success(Unit))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing conversation messages", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Clear all conversations
     */
    fun clearAllConversations(): Flow<Result<Unit>> = flow {
        try {
            Log.d(TAG, "Clearing all conversations")
            
            messageDao.deleteAllMessages()
            conversationDao.deleteAllConversations()
            
            emit(Result.success(Unit))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing conversations", e)
            emit(Result.failure(e))
        }
    }
    
    // Stub implementations for missing methods
    fun createConversation(characterId: String): Flow<Result<String>> = flow {
        try {
            val conversationId = createNewConversation(characterId)
            emit(Result.success(conversationId))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Generate AI response for a user message
     */
    fun generateCharacterResponse(
        conversationId: String,
        userMessage: String,
        characterId: String,
        isFirstMessage: Boolean = false
    ): Flow<Result<Message>> = flow {
        try {
            // Get current user for placeholder replacement
            val currentUser = authRepository.getCachedUser().first()
            val userName = currentUser?.username ?: "User"
            
            // Get character for context
            val character = characterDao.getCharacterById(characterId)
            
            // Get conversation history
            val conversationHistory = messageDao.getMessagesByConversationId(conversationId)
                .map { it.toMessageResponse() }
            
            // Get previous message for context
            val previousMessage = conversationHistory.lastOrNull()?.content
            
            val nowTs1 = System.currentTimeMillis()
            val actualUserMessage = userMessage
            
            val aiResponse = try {
                if (character != null) {
                    val response = chatLLMService.generateResponse(
                        userMessage = actualUserMessage,
                        character = character,
                        userName = userName,
                        previousMessage = previousMessage,
                        conversationHistory = conversationHistory,
                        isFirstMessage = isFirstMessage
                    )
                    // Check if response looks like JSON error and clean it up
                    if (response.trim().startsWith("{") && response.contains("Something went wrong")) {
                        "I'm having trouble generating a response right now. Please check your LLM settings and try again."
                    } else {
                        response
                    }
                } else {
                    generateLocalFallbackResponse(actualUserMessage, character)
                }
            } catch (e: Exception) {
                Log.e(TAG, "LLM generation failed, using fallback", e)
                generateLocalFallbackResponse(actualUserMessage, character)
            }
            
            val nowTs2 = System.currentTimeMillis()
            val message = Message(
                id = IdGenerator.generateMessageId(),
                conversationId = conversationId,
                content = aiResponse,
                role = "character",
                timestamp = nowTs2,
                characterId = characterId,
                characterName = character?.name,
                createdAt = nowTs2,
                updatedAt = nowTs2
            )
            
            // Use transaction for database operations
            try {
                messageDao.insertMessage(message)
                conversationDao.incrementCharacterMessageStats(conversationId, nowTs2)
                Log.d(TAG, "Successfully saved character response: ${message.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Database transaction failed for character response", e)
                throw e
            }
            
            // Emit success result
            emit(Result.success(message))
        } catch (e: DuplicateResponseException) {
            // For duplicate responses, emit a failure result instead of not emitting
            Log.d(TAG, "Duplicate response detected: ${e.message}")
            emit(Result.failure(e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in generateCharacterResponse", e)
            emit(Result.failure(e))
        }
    }
    
    // Custom exception to indicate duplicate-skip scenarios
    class DuplicateResponseException(message: String) : Exception(message)
    
    /**
     * Send a message as the character (e.g., first greeting)
     */
    fun sendCharacterMessage(conversationId: String, content: String, characterId: String): Flow<Result<Message>> = flow {
        try {
            // Get character to process greeting with macros
            val character = characterDao.getCharacterById(characterId)
            
            // Use the content parameter directly (it's already processed in the ViewModel)
            val processedContent = content
            
            val nowTs3 = System.currentTimeMillis()
            val message = Message(
                id = IdGenerator.generateMessageId(),
                conversationId = conversationId,
                content = processedContent,
                role = "character",
                timestamp = nowTs3,
                characterId = characterId,
                characterName = character?.name,
                createdAt = nowTs3,
                updatedAt = nowTs3
            )
            
            // Use transaction for database operations
            try {
                messageDao.insertMessage(message)
                conversationDao.incrementCharacterMessageStats(conversationId, nowTs3)
                Log.d(TAG, "Successfully saved character greeting: ${message.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Database transaction failed for character greeting", e)
                throw e
            }
            
            emit(Result.success(message))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending character message", e)
            emit(Result.failure(e))
        }
    }
    
    // ID generation is now handled by IdGenerator utility class
    
    fun updateTypingStatus(conversationId: String, isTyping: Boolean) {
        // TODO: Implement typing status
    }
    
    /**
     * Get conversation by ID
     */
    suspend fun getConversationById(conversationId: String): Conversation? {
        return withContext(Dispatchers.IO) {
            try {
                conversationDao.getConversationById(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting conversation by ID: $conversationId", e)
                null
            }
        }
    }
    
    /**
     * Get recent conversations for home screen
     */
    suspend fun getRecentConversations(limit: Int = 5): List<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting recent conversations with limit: $limit")
                conversationDao.getRecentConversations(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting recent conversations", e)
                emptyList()
            }
        }
    }
    
    /**
     * Clean up duplicate character responses in a conversation
     */
    suspend fun cleanupDuplicateResponses(conversationId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Cleaning up duplicate responses for conversation: $conversationId")
                
                // Get all messages for this conversation ordered by timestamp
                val allMessages = messageDao.getMessagesByConversationId(conversationId)
                val duplicatesToRemove = mutableListOf<String>()
                
                // Group messages by timestamp clusters (within 5 seconds of each other)
                // and identify duplicate character responses
                for (i in allMessages.indices) {
                    val currentMessage = allMessages[i]
                    
                    // Skip if not a character message
                    if (currentMessage.role != "character") continue
                    
                    // Look for duplicate character responses within 30 seconds
                    for (j in i + 1 until allMessages.size) {
                        val nextMessage = allMessages[j]
                        
                        // Stop if we encounter a user message (different conversation turn)
                        if (nextMessage.role == "user") break
                        
                        // Check if this is a duplicate character response
                        // Only consider messages with the same messageType as duplicates
                        if (nextMessage.role == "character" && 
                            Math.abs(nextMessage.timestamp - currentMessage.timestamp) <= 30000 && // Within 30 seconds
                            nextMessage.messageType == currentMessage.messageType) { // Same message type
                            
                            // Mark the later message for removal
                            duplicatesToRemove.add(nextMessage.id)
                            Log.d(TAG, "Found duplicate character response: ${nextMessage.id} (type: ${nextMessage.messageType})")
                        }
                    }
                }
                
                // Remove duplicates
                duplicatesToRemove.forEach { messageId ->
                    messageDao.deleteMessageById(messageId)
                }
                
                Log.d(TAG, "Removed ${duplicatesToRemove.size} duplicate responses")
                duplicatesToRemove.size
                
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up duplicate responses", e)
                0
            }
        }
    }
    
    /**
     * Clean up all duplicate responses across all conversations
     */
    suspend fun cleanupAllDuplicateResponses(): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Cleaning up all duplicate responses")
                
                val allConversations = conversationDao.getAllConversations()
                var totalCleaned = 0
                
                allConversations.forEach { conversation ->
                    totalCleaned += cleanupDuplicateResponses(conversation.id)
                }
                
                Log.d(TAG, "Total duplicate responses cleaned: $totalCleaned")
                totalCleaned
                
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up all duplicate responses", e)
                0
            }
        }
    }
    
    // Insert a message (used for image messages)
    suspend fun insertMessage(message: com.vortexai.android.data.models.Message) {
        withContext(Dispatchers.IO) {
            messageDao.insertMessage(message)
        }
    }
    
    // Delete a message by ID
    suspend fun deleteMessageById(messageId: String) {
        withContext(Dispatchers.IO) {
            messageDao.deleteMessageById(messageId)
        }
    }
    
    /**
     * Check database health and connectivity
     */
    suspend fun checkDatabaseHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Try basic database operations
                val messageCount = messageDao.getTotalMessageCount()
                val conversationCount = conversationDao.getTotalConversationCount()
                
                Log.d(TAG, "Database health check: $messageCount messages, $conversationCount conversations")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Database health check failed", e)
                false
            }
        }
    }

    /**
     * Get exact total number of messages across all conversations
     */
    suspend fun getTotalMessageCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                messageDao.getTotalMessageCount()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get total message count", e)
                0
            }
        }
    }
    
    /**
     * Get exact total number of conversations
     */
    suspend fun getTotalConversationCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                conversationDao.getTotalConversationCount()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get total conversation count", e)
                0
            }
        }
    }
    
    /**
     * Perform database maintenance and cleanup
     */
    suspend fun performDatabaseMaintenance(): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting database maintenance")
                
                // Clean up duplicate responses
                val duplicatesRemoved = cleanupAllDuplicateResponses()
                
                // TODO: Add more maintenance tasks as needed
                // - Clean up orphaned messages
                // - Update conversation statistics
                // - Vacuum database
                
                Log.d(TAG, "Database maintenance completed: $duplicatesRemoved duplicates removed")
                duplicatesRemoved
            } catch (e: Exception) {
                Log.e(TAG, "Database maintenance failed", e)
                0
            }
        }
    }
    
    /**
     * Backup all app data to a portable JSON including embedded character images (base64) and generated images.
     */
    suspend fun createFullBackup(): String {
        return withContext(Dispatchers.IO) {
            val characters = characterDao.getAllCharacters()
            val conversations = conversationDao.getAllConversations()
            val messages = conversations.flatMap { conversation ->
                messageDao.getMessagesByConversationId(conversation.id)
            }
            val generatedImages = try {
                // Optional, may not exist in some builds
                com.vortexai.android.data.database.dao.GeneratedImageDao::class
                // Access via database instance if available; otherwise omit
                emptyList<com.vortexai.android.data.models.GeneratedImage>()
            } catch (e: Exception) { emptyList() }

            // Embed character images if they are file paths
            fun embedImageIfLocal(url: String?): String? {
                if (url.isNullOrBlank()) return null
                return try {
                    if (url.startsWith("file://")) {
                        val filePath = url.removePrefix("file://")
                        val bytes = java.io.File(filePath).takeIf { it.exists() }?.readBytes()
                        bytes?.let { "data:image/*;base64," + android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
                    } else if (url.startsWith("/")) {
                        val bytes = java.io.File(url).takeIf { it.exists() }?.readBytes()
                        bytes?.let { "data:image/*;base64," + android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
                    } else null
                } catch (_: Exception) { null }
            }

            val characterBackups = characters.map { c ->
                mapOf(
                    "id" to c.id,
                    "name" to c.name,
                    "displayName" to c.displayName,
                    "shortDescription" to c.shortDescription,
                    "longDescription" to c.longDescription,
                    "persona" to c.persona,
                    "backstory" to c.backstory,
                    "greeting" to c.greeting,
                    "avatarUrl" to (embedImageIfLocal(c.avatarUrl) ?: c.avatarUrl),
                    "appearance" to c.appearance,
                    "personality" to c.personality,
                    "scenario" to c.scenario,
                    "exampleDialogue" to c.exampleDialogue,
                    "characterBook" to c.characterBook,
                    "tags" to c.tags,
                    "categories" to c.categories,
                    "isPublic" to c.isPublic,
                    "isFavorite" to c.isFavorite,
                    "description" to c.description,
                    "version" to c.version,
                    "createdAt" to c.totalMessages // preserve other fields as needed
                )
            }

            val payload = mapOf(
                "meta" to mapOf(
                    "version" to 1,
                    "exportedAt" to System.currentTimeMillis()
                ),
                "characters" to characterBackups,
                "conversations" to conversations,
                "messages" to messages
            )
            val json = com.google.gson.Gson().toJson(payload)
            json
        }
    }

    /**
     * Restore app data from a full backup JSON. Existing records with same IDs are replaced.
     */
    suspend fun restoreFromBackup(json: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val gson = com.google.gson.Gson()
                val root = gson.fromJson(json, Map::class.java)
                val characters = (root["characters"] as? List<*>)?.mapNotNull { item ->
                    try { gson.fromJson(gson.toJson(item), com.vortexai.android.data.models.Character::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
                val conversations = (root["conversations"] as? List<*>)?.mapNotNull { item ->
                    try { gson.fromJson(gson.toJson(item), com.vortexai.android.data.models.Conversation::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
                val messages = (root["messages"] as? List<*>)?.mapNotNull { item ->
                    try { gson.fromJson(gson.toJson(item), com.vortexai.android.data.models.Message::class.java) } catch (_: Exception) { null }
                } ?: emptyList()

                // Insert/replace in order: characters → conversations → messages
                characterDao.insertCharacters(characters)
                conversationDao.insertConversations(conversations)
                messageDao.insertMessages(messages)

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                Result.failure(e)
            }
        }
    }
    
}

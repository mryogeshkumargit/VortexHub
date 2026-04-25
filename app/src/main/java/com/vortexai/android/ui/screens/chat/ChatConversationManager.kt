package com.vortexai.android.ui.screens.chat

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vortexai.android.data.models.*
import com.vortexai.android.data.repository.AuthRepository
import com.vortexai.android.data.repository.ChatRepository
import com.vortexai.android.data.repository.CharacterRepository
import com.vortexai.android.utils.MacroProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatConversationManager @Inject constructor(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository,
    private val authRepository: AuthRepository,
    private val macroProcessor: MacroProcessor,
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private const val TAG = "ChatConversationManager"
    }
    
    suspend fun findOrCreateConversation(characterId: String): Result<String> {
        return try {
            val existingId = chatRepository.getExistingConversationIdForCharacter(characterId)
            
            if (existingId != null) {
                Log.d(TAG, "Found existing conversation for character: $existingId - reusing existing chat")
                Result.success(existingId)
            } else {
                Log.d(TAG, "No existing conversation found, creating new one")
                createNewConversation(characterId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding conversations, creating new one", e)
            createNewConversation(characterId)
        }
    }
    
    suspend fun createNewConversation(characterId: String): Result<String> {
        return try {
            chatRepository.createConversation(characterId)
                .flowOn(Dispatchers.IO)
                .first()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating conversation", e)
            Result.failure(e)
        }
    }
    
    suspend fun forceCreateNewConversation(characterId: String): Result<String> {
        return try {
            Log.d(TAG, "Force creating new conversation for character: $characterId")
            chatRepository.createConversation(characterId)
                .flowOn(Dispatchers.IO)
                .first()
        } catch (e: Exception) {
            Log.e(TAG, "Error force creating conversation", e)
            Result.failure(e)
        }
    }
    
    suspend fun loadMessages(conversationId: String): Result<List<MessageResponse>> {
        return try {
            chatRepository.getMessages(conversationId)
                .flowOn(Dispatchers.IO)
                .first()
                .fold(
                    onSuccess = { messageListResponse ->
                        Log.d(TAG, "Loaded ${messageListResponse.messages.size} messages from database")
                        val messages = messageListResponse.messages.map { it.toMessageResponse() }
                        Result.success(messages)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load messages", exception)
                        Result.failure(exception)
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading messages", e)
            Result.failure(e)
        }
    }
    
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        characterId: String
    ): Result<Message> {
        return try {
            chatRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                characterId = characterId
            ).flowOn(Dispatchers.IO)
            .first()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    suspend fun generateCharacterResponse(
        conversationId: String,
        userMessage: String,
        characterId: String,
        isFirstMessage: Boolean = false
    ): Result<Message> {
        return try {
            chatRepository.generateCharacterResponse(
                conversationId = conversationId,
                userMessage = userMessage,
                characterId = characterId,
                isFirstMessage = isFirstMessage
            ).flowOn(Dispatchers.IO)
            .first()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating character response", e)
            Result.failure(e)
        }
    }
    
    suspend fun sendCharacterFirstMessage(
        conversationId: String,
        character: Character
    ): Result<Message> {
        return try {
            delay(500) // Simulate typing
            
            // Send character image if available
            if (!character.avatarUrl.isNullOrBlank()) {
                sendCharacterImage(conversationId, character)
                delay(800)
            }
            
            // Send greeting message
            val currentUser = authRepository.getCachedUser().first()
            val userName = currentUser?.username ?: "User"
            
            val firstMessage = if (!character.greeting.isNullOrBlank()) {
                macroProcessor.processCharacterGreeting(
                    character = character,
                    userName = userName
                )
            } else {
                macroProcessor.processCharacterGreeting(
                    character = character,
                    userName = userName
                )
            }
            
            chatRepository.sendCharacterMessage(
                conversationId = conversationId,
                content = firstMessage,
                characterId = character.id
            ).flowOn(Dispatchers.IO)
            .first()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending character first message", e)
            Result.failure(e)
        }
    }
    
    private suspend fun sendCharacterImage(conversationId: String, character: Character) {
        try {
            val metadataJson = org.json.JSONObject().apply {
                put("localPath", character.avatarUrl ?: "")
                put("imageUrl", character.avatarUrl ?: "")
                put("generationTime", 0)
                put("modelUsed", "character_avatar")
                put("isCharacterIntro", true)
            }.toString()
            
            val messageId = com.vortexai.android.utils.IdGenerator.generateMessageId()
            val nowTs = System.currentTimeMillis()
            
            val imageMessage = Message(
                id = messageId,
                conversationId = conversationId,
                content = "*${character.name} appears*",
                role = "character",
                senderType = "character",
                timestamp = nowTs,
                characterId = character.id,
                characterName = character.name,
                messageType = "image",
                metadataJson = metadataJson,
                createdAt = nowTs,
                updatedAt = nowTs
            )
            
            withContext(Dispatchers.IO) {
                chatRepository.insertMessage(imageMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending character image", e)
        }
    }
    
    suspend fun insertMessageDirectly(message: Message) {
        withContext(Dispatchers.IO) {
            chatRepository.insertMessage(message)
        }
    }
    
    suspend fun saveConversationState(conversationId: String) {
        try {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("last_conversation_id")] = conversationId
                preferences[longPreferencesKey("last_conversation_timestamp")] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving conversation state", e)
        }
    }
    
    suspend fun restoreConversationState(): String? {
        return try {
            val preferences = dataStore.data.first()
            val lastConversationId = preferences[stringPreferencesKey("last_conversation_id")]
            val lastTimestamp = preferences[longPreferencesKey("last_conversation_timestamp")] ?: 0L
            
            val isRecent = System.currentTimeMillis() - lastTimestamp < 5 * 60 * 1000
            
            if (lastConversationId != null && isRecent) {
                lastConversationId
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring conversation state", e)
            null
        }
    }
    
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            chatRepository.deleteMessageById(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get conversation statistics
     */
    suspend fun getConversationStats(conversationId: String): Result<ConversationStats> {
        return try {
            val messages = loadMessages(conversationId)
            messages.fold(
                onSuccess = { messageList ->
                    val userMessages = messageList.count { it.senderType == MessageSenderType.USER }
                    val characterMessages = messageList.count { it.senderType == MessageSenderType.CHARACTER }
                    val totalWords = messageList.sumOf { it.content.split(" ").size }
                    val avgWordsPerMessage = if (messageList.isNotEmpty()) totalWords / messageList.size else 0
                    
                    val stats = ConversationStats(
                        totalMessages = messageList.size,
                        userMessages = userMessages,
                        characterMessages = characterMessages,
                        totalWords = totalWords,
                        averageWordsPerMessage = avgWordsPerMessage,
                        conversationStartTime = messageList.firstOrNull()?.timestamp,
                        lastMessageTime = messageList.lastOrNull()?.timestamp
                    )
                    Result.success(stats)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export conversation to text format
     */
    suspend fun exportConversation(conversationId: String, format: String = "txt"): Result<String> {
        return try {
            val messages = loadMessages(conversationId)
            messages.fold(
                onSuccess = { messageList ->
                    val exported = when (format.lowercase()) {
                        "json" -> {
                            val jsonArray = org.json.JSONArray()
                            messageList.forEach { message ->
                                val jsonMessage = org.json.JSONObject().apply {
                                    put("sender", message.senderName)
                                    put("content", message.content)
                                    put("timestamp", message.timestamp)
                                    put("type", message.messageType.name)
                                }
                                jsonArray.put(jsonMessage)
                            }
                            jsonArray.toString(2)
                        }
                        else -> {
                            // Plain text format
                            val sb = StringBuilder()
                            sb.appendLine("=== Conversation Export ===")
                            sb.appendLine("Exported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
                            sb.appendLine("Total Messages: ${messageList.size}")
                            sb.appendLine("")
                            
                            messageList.forEach { message ->
                                val timestamp = try {
                                    val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).parse(message.timestamp)
                                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(date ?: java.util.Date())
                                } catch (e: Exception) {
                                    "--:--:--"
                                }
                                
                                sb.appendLine("[$timestamp] ${message.senderName}: ${message.content}")
                            }
                            sb.toString()
                        }
                    }
                    Result.success(exported)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear conversation messages but keep the conversation
     */
    suspend fun clearConversationMessages(conversationId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Clearing messages for conversation: $conversationId")
            chatRepository.clearConversationMessages(conversationId)
                .flowOn(Dispatchers.IO)
                .first()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing conversation messages", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clear conversation history but keep the conversation
     */
    suspend fun clearConversationHistory(conversationId: String): Result<Unit> {
        return try {
            // Implementation would need to be added to chatRepository
            Log.d(TAG, "Clearing conversation history for: $conversationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ConversationStats(
    val totalMessages: Int,
    val userMessages: Int,
    val characterMessages: Int,
    val totalWords: Int,
    val averageWordsPerMessage: Int,
    val conversationStartTime: String?,
    val lastMessageTime: String?
)
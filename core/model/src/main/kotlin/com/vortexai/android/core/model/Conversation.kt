package com.vortexai.android.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Conversation model representing a chat session with a character
 */
@Parcelize
@Serializable
data class Conversation(
    val id: String,
    val characterId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val settings: ConversationSettings = ConversationSettings(),
    val metadata: Map<String, String> = emptyMap()
) : Parcelable {
    
    /**
     * Get the display title for the conversation
     */
    val displayTitle: String
        get() = title.ifBlank { "New Conversation" }
    
    /**
     * Get the creation date as Instant
     */
    val createdAtInstant: Instant
        get() = Instant.ofEpochMilli(createdAt)
    
    /**
     * Get the update date as Instant
     */
    val updatedAtInstant: Instant
        get() = Instant.ofEpochMilli(updatedAt)
    
    /**
     * Check if conversation is empty
     */
    val isEmpty: Boolean
        get() = messageCount == 0
}

/**
 * Message model representing individual chat messages
 */
@Parcelize
@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val metadata: MessageMetadata = MessageMetadata(),
    val attachments: List<MessageAttachment> = emptyList(),
    val reactions: List<MessageReaction> = emptyList()
) : Parcelable {
    
    /**
     * Get the timestamp as Instant
     */
    val timestampInstant: Instant
        get() = Instant.ofEpochMilli(timestamp)
    
    /**
     * Get the edited timestamp as Instant
     */
    val editedAtInstant: Instant?
        get() = editedAt?.let { Instant.ofEpochMilli(it) }
    
    /**
     * Check if message is from user
     */
    val isFromUser: Boolean
        get() = role == MessageRole.USER
    
    /**
     * Check if message is from assistant
     */
    val isFromAssistant: Boolean
        get() = role == MessageRole.ASSISTANT
    
    /**
     * Get display content with fallback
     */
    val displayContent: String
        get() = content.ifBlank { "[Empty message]" }
}

/**
 * Message roles in conversation
 */
@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Message metadata for additional information
 */
@Parcelize
@Serializable
data class MessageMetadata(
    val tokenCount: Int = 0,
    val processingTime: Long = 0,
    val model: String? = null,
    val provider: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
    val error: String? = null,
    val retryCount: Int = 0
) : Parcelable

/**
 * Message attachments (for future multimedia support)
 */
@Parcelize
@Serializable
data class MessageAttachment(
    val id: String,
    val type: AttachmentType,
    val url: String,
    val fileName: String,
    val fileSize: Long = 0,
    val mimeType: String? = null,
    val metadata: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Attachment types
 */
@Serializable
enum class AttachmentType {
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    OTHER
}

/**
 * Message reactions for user feedback
 */
@Parcelize
@Serializable
data class MessageReaction(
    val id: String,
    val messageId: String,
    val type: ReactionType,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Reaction types
 */
@Serializable
enum class ReactionType {
    LIKE,
    DISLIKE,
    LOVE,
    LAUGH,
    SURPRISE,
    ANGRY,
    SAD
}

/**
 * Conversation settings for customization
 */
@Parcelize
@Serializable
data class ConversationSettings(
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val topP: Float = 0.9f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stopSequences: List<String> = emptyList(),
    val model: String = "",
    val provider: String = "",
    val contextLength: Int = 4000,
    val enableMemory: Boolean = true,
    val memoryStrength: Float = 0.5f
) : Parcelable

/**
 * Conversation creation request
 */
@Parcelize
@Serializable
data class ConversationRequest(
    val characterId: String,
    val title: String = "",
    val settings: ConversationSettings = ConversationSettings(),
    val initialMessage: String? = null
) : Parcelable

/**
 * Message creation request
 */
@Parcelize
@Serializable
data class MessageRequest(
    val conversationId: String,
    val content: String,
    val role: MessageRole = MessageRole.USER,
    val attachments: List<MessageAttachment> = emptyList()
) : Parcelable

/**
 * Conversation filter options
 */
@Parcelize
@Serializable
data class ConversationFilter(
    val characterId: String? = null,
    val query: String = "",
    val pinnedOnly: Boolean = false,
    val excludeArchived: Boolean = true,
    val sortBy: ConversationSortBy = ConversationSortBy.UPDATED_DESC,
    val createdAfter: Long? = null,
    val createdBefore: Long? = null
) : Parcelable

/**
 * Conversation sorting options
 */
@Serializable
enum class ConversationSortBy {
    TITLE_ASC,
    TITLE_DESC,
    CREATED_ASC,
    CREATED_DESC,
    UPDATED_ASC,
    UPDATED_DESC,
    MESSAGE_COUNT_ASC,
    MESSAGE_COUNT_DESC,
    PINNED_FIRST
}

/**
 * Conversation statistics
 */
@Parcelize
@Serializable
data class ConversationStats(
    val conversationId: String,
    val messageCount: Int = 0,
    val userMessageCount: Int = 0,
    val assistantMessageCount: Int = 0,
    val totalTokens: Long = 0,
    val averageResponseTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Chat session state for real-time updates
 */
@Parcelize
@Serializable
data class ChatSession(
    val conversationId: String,
    val characterId: String,
    val isTyping: Boolean = false,
    val isProcessing: Boolean = false,
    val currentModel: String = "",
    val currentProvider: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastActivity: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Connection status for chat sessions
 */
@Serializable
enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
} 
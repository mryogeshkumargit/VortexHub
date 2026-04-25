package com.vortexai.android.data.models

import com.google.gson.annotations.SerializedName
import com.vortexai.android.utils.IdGenerator

/**
 * Chat conversation model
 */
data class ConversationResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("character_id")
    val characterId: String,
    @SerializedName("character_name")
    val characterName: String,
    @SerializedName("character_avatar_url")
    val characterAvatarUrl: String?,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("last_message")
    val lastMessage: String?,
    @SerializedName("last_message_at")
    val lastMessageAt: String,
    @SerializedName("message_count")
    val messageCount: Int = 0,
    @SerializedName("is_pinned")
    val isPinned: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Chat message model
 */
data class MessageResponse(
    @SerializedName("id")
    val id: String = IdGenerator.generateMessageId(),
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("sender_type")
    val senderType: MessageSenderType,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("sender_name")
    val senderName: String?,
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("message_type")
    val messageType: MessageType = MessageType.TEXT,
    @SerializedName("metadata")
    val metadata: MessageResponseMetadata?? = null,
    @SerializedName("is_edited")
    val isEdited: Boolean = false,
    @SerializedName("edited_at")
    val editedAt: String? = null,
    @SerializedName("reply_to_id")
    val replyToId: String? = null,
    @SerializedName("status")
    val status: MessageStatus = MessageStatus.SENT
)

/**
 * Message sender type
 */
enum class MessageSenderType {
    @SerializedName("user")
    USER,
    @SerializedName("character")
    CHARACTER,
    @SerializedName("system")
    SYSTEM
}

/**
 * Message type
 */
enum class MessageType {
    @SerializedName("text")
    TEXT,
    @SerializedName("image")
    IMAGE,
    @SerializedName("video")
    VIDEO,
    @SerializedName("audio")
    AUDIO,
    @SerializedName("system")
    SYSTEM
}

/**
 * Message status
 */
enum class MessageStatus {
    @SerializedName("sending")
    SENDING,
    @SerializedName("sent")
    SENT,
    @SerializedName("delivered")
    DELIVERED,
    @SerializedName("failed")
    FAILED
}

/**
 * Message metadata for additional information
 */
data class MessageResponseMetadata(
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("source_image_id")
    val sourceImageId: String? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    @SerializedName("audio_duration")
    val audioDuration: Int? = null,
    @SerializedName("generation_time")
    val generationTime: Long? = null,
    @SerializedName("model_used")
    val modelUsed: String? = null,
    @SerializedName("token_count")
    val tokenCount: Int? = null
)

/**
 * Send message request
 */
data class SendMessageRequest(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("message_type")
    val messageType: MessageType = MessageType.TEXT,
    @SerializedName("reply_to_id")
    val replyToId: String? = null
)

/**
 * Create conversation request
 */
data class CreateConversationRequest(
    @SerializedName("character_id")
    val characterId: String,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("initial_message")
    val initialMessage: String? = null
)

/**
 * Conversation list response
 */
data class ConversationResponseListResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("conversations")
    val conversations: List<ConversationResponse>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("has_more")
    val hasMore: Boolean
)

/**
 * Message list response
 */
data class MessageResponseListResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("messages")
    val messages: List<MessageResponse>,
    @SerializedName("conversation")
    val conversation: ConversationResponse??,
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("has_more")
    val hasMore: Boolean
)

/**
 * Typing indicator
 */
data class TypingIndicator(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("is_typing")
    val isTyping: Boolean,
    @SerializedName("timestamp")
    val timestamp: String
)

/**
 * Chat settings
 */
data class ChatSettings(
    @SerializedName("auto_scroll")
    val autoScroll: Boolean = true,
    @SerializedName("show_timestamps")
    val showTimestamps: Boolean = true,
    @SerializedName("message_sound")
    val messageSound: Boolean = true,
    @SerializedName("typing_indicators")
    val typingIndicators: Boolean = true,
    @SerializedName("character_response_delay")
    val characterResponseDelay: Long = 1000L
)

/**
 * Chat statistics
 */
data class ChatStatistics(
    @SerializedName("total_messages")
    val totalMessages: Int,
    @SerializedName("total_conversations")
    val totalConversations: Int,
    @SerializedName("average_response_time")
    val averageResponseTime: Long,
    @SerializedName("favorite_character")
    val favoriteCharacter: String?,
    @SerializedName("most_active_time")
    val mostActiveTime: String?
) 

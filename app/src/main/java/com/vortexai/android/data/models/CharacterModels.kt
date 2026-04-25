package com.vortexai.android.data.models

import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Room Entity for Character
@Entity(
    tableName = "characters",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isActive"]),
        Index(value = ["isPublic"]),
        Index(value = ["totalMessages"]),
        Index(value = ["averageRating"])
    ]
)
data class Character(
    @PrimaryKey
    val id: String,
    
    // Basic Information
    val name: String,
    val displayName: String? = null,
    val shortDescription: String? = null,
    val longDescription: String? = null,
    val persona: String? = null,
    val backstory: String? = null,
    val greeting: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @ColumnInfo(name = "avatar_video_url")
    val avatarVideoUrl: String? = null,
    
    // Enhanced Character Details
    val appearance: String? = null,
    val personality: String? = null,
    val scenario: String? = null,
    val exampleDialogue: String? = null,
    val characterBook: String? = null, // JSON string for character book/lorebook
    
    // Generation Settings
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    
    // Content and Safety
    val nsfwEnabled: Boolean = false,
    @SerializedName("tags")
    val tags: List<String>? = null, // Nullable to handle backend null values
    @SerializedName("categories")
    val categories: List<String>? = null, // Nullable to handle backend null values
    
    // Metadata
    val creatorId: String? = null,
    val creator: String? = null,
    val creatorNotes: String? = null,
    @SerializedName("character_version")
    val characterVersion: String = "1.0",
    val isPublic: Boolean = true,
    val isFeatured: Boolean = false,
    val isFavorite: Boolean = false,
    val description: String? = null,
    val stats: Map<String, Any>? = null, // Nullable to handle backend null values
    val version: Int = 1,
    
    // Statistics
    val totalMessages: Int = 0,
    val totalConversations: Int = 0,
    val averageRating: Float = 0.0f,
    val totalRatings: Int = 0,
    val lastInteraction: String? = null,
    
    // System Fields
    val isActive: Boolean = true,
    
    // Dynamic Stats System
    val dynamicStatsEnabled: Boolean = false
)

// Room Entity for Conversation
@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["characterId"]),
        Index(value = ["userId"]),
        Index(value = ["isActive"]),
        Index(value = ["lastMessageAt"]),
        Index(value = ["createdAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Conversation(
    @PrimaryKey
    val id: String,
    
    // Basic Information
    val title: String? = null,
    val characterId: String,
    val characterName: String,
    
    // User Context
    val userId: String? = null,
    
    // Conversation State
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    
    // Statistics
    val totalMessages: Int = 0,
    val userMessages: Int = 0,
    val characterMessages: Int = 0,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long? = null
)

// Room Entity for Message
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["role"]),
        Index(value = ["createdAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Message(
    @PrimaryKey
    val id: String,
    
    // Core Message Data
    val conversationId: String,
    val content: String,
    val role: String, // 'user', 'character', 'system'
    val senderType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Character Context
    val characterId: String? = null,
    val characterName: String? = null,
    
    // User Interaction
    val userId: String? = null,
    val isEdited: Boolean = false,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // New fields for media etc.
    val messageType: String = "text", // 'text' or 'image'
    val metadataJson: String? = null
)

// Room Entity for User
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["isActive"])
    ]
)
data class User(
    @PrimaryKey
    val id: String,
    
    val username: String,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isPremium: Boolean = false,
    val preferences: String? = null, // JSON string for user preferences
    
    // Statistics
    val totalMessages: Int = 0,
    val totalConversations: Int = 0,
    
    // System Fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

// Room Entity for User Session
@Entity(
    tableName = "user_sessions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["sessionToken"], unique = true),
        Index(value = ["isActive"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSession(
    @PrimaryKey
    val id: String,
    
    val userId: String,
    val sessionToken: String,
    val deviceInfo: String? = null,
    
    // Session State
    val isActive: Boolean = true,
    val expiresAt: Long,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

/**
 * Character creation request
 */
data class CreateCharacterRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("personality")
    val personality: String,
    @SerializedName("greeting")
    val greeting: String,
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("is_public")
    val isPublic: Boolean = false
)

/**
 * Character update request
 */
data class UpdateCharacterRequest(
    @SerializedName("name")
    val name: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("personality")
    val personality: String?,
    @SerializedName("greeting")
    val greeting: String?,
    @SerializedName("tags")
    val tags: List<String>?,
    @SerializedName("is_public")
    val isPublic: Boolean?
)

/**
 * Generate avatar request
 */
data class GenerateAvatarRequest(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("style")
    val style: String = "anime"
)

/**
 * Rate character request
 */
data class RateCharacterRequest(
    @SerializedName("rating")
    val rating: Float // 1.0 to 5.0
)

/**
 * Pagination information
 */
data class Pagination(
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("total")
    val total: Int,
    @SerializedName("pages")
    val pages: Int
)

/**
 * Character list response (matches backend format)
 */
data class CharacterListResponse(
    @SerializedName("characters")
    val characters: List<Character>,
    @SerializedName("pagination")
    val pagination: Pagination? = null,
    // Additional fields for internal use
    val success: Boolean = true,
    val hasMore: Boolean = false
)

/**
 * Conversation list response for API calls
 */
data class ConversationListResponse(
    val conversations: List<Conversation>,
    val pagination: Pagination,
    val success: Boolean,
    val hasMore: Boolean
)

/**
 * Message list response for API calls
 */
data class MessageListResponse(
    val messages: List<Message>,
    val pagination: Pagination,
    val success: Boolean,
    val hasMore: Boolean
)



/**
 * Favorite character request
 */
data class FavoriteCharacterRequest(
    @SerializedName("character_id")
    val characterId: String,
    @SerializedName("is_favorite")
    val isFavorite: Boolean
)

/**
 * User preferences
 */
data class UserPreferences(
    @SerializedName("theme")
    val theme: String = "dark",
    @SerializedName("language")
    val language: String = "en",
    @SerializedName("chat_theme")
    val chatTheme: String = "enhanced",
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean = true,
    @SerializedName("nsfw_enabled")
    val nsfwEnabled: Boolean = false,
    @SerializedName("auto_speak_messages")
    val autoSpeakMessages: Boolean = false,
    @SerializedName("message_history_limit")
    val messageHistoryLimit: String = "50"
)

/**
 * Character statistics for display
 */
data class CharacterStats(
    val messageCount: Int = 0,
    val averageRating: Float = 0.0f,
    val totalRatings: Int = 0,
    val lastUsed: Long? = null
)

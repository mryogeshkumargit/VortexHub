package com.vortexai.android.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Core character model representing an AI companion character
 */
@Parcelize
@Serializable
data class Character(
    val id: String,
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val firstMessage: String,
    val exampleDialogue: String,
    val avatarUrl: String? = null,
    val avatarVideoUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isNsfw: Boolean = false,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Long = 0,
    val isFavorite: Boolean = false,
    val characterBook: CharacterBook? = null,
    val alternateGreetings: List<String> = emptyList(),
    val extensions: Map<String, String> = emptyMap(),
    val version: Int = 1
) : Parcelable {
    
    /**
     * Get the display name for the character
     */
    val displayName: String
        get() = name.ifBlank { "Unnamed Character" }
    
    /**
     * Get a short description for preview
     */
    val shortDescription: String
        get() = description.take(100).let { 
            if (description.length > 100) "$it..." else it 
        }
    
    /**
     * Check if character has complete information
     */
    val isComplete: Boolean
        get() = name.isNotBlank() && 
                description.isNotBlank() && 
                personality.isNotBlank() && 
                firstMessage.isNotBlank()
    
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
}

/**
 * Character creation/update request model
 */
@Parcelize
@Serializable
data class CharacterRequest(
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val firstMessage: String,
    val exampleDialogue: String = "",
    val tags: List<String> = emptyList(),
    val isNsfw: Boolean = false,
    val characterBook: CharacterBook? = null,
    val alternateGreetings: List<String> = emptyList(),
    val extensions: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Character book for advanced character context
 */
@Parcelize
@Serializable
data class CharacterBook(
    val name: String = "",
    val description: String = "",
    val scanDepth: Int = 100,
    val tokenBudget: Int = 512,
    val recursiveScanning: Boolean = false,
    val extensions: Map<String, String> = emptyMap(),
    val entries: List<CharacterBookEntry> = emptyList()
) : Parcelable

/**
 * Individual entry in a character book
 */
@Parcelize
@Serializable
data class CharacterBookEntry(
    val id: String,
    val keys: List<String>,
    val content: String,
    val extensions: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val insertionOrder: Int = 100,
    val caseSensitive: Boolean = false,
    val name: String = "",
    val priority: Int = 0,
    val comment: String = ""
) : Parcelable

/**
 * Character statistics for analytics
 */
@Parcelize
@Serializable
data class CharacterStats(
    val characterId: String,
    val messageCount: Long = 0,
    val conversationCount: Long = 0,
    val averageMessageLength: Double = 0.0,
    val totalTokensUsed: Long = 0,
    val lastUsed: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Character filter options for searching/sorting
 */
@Parcelize
@Serializable
data class CharacterFilter(
    val query: String = "",
    val tags: List<String> = emptyList(),
    val includeNsfw: Boolean = false,
    val favoritesOnly: Boolean = false,
    val sortBy: CharacterSortBy = CharacterSortBy.CREATED_DESC,
    val createdAfter: Long? = null,
    val createdBefore: Long? = null
) : Parcelable

/**
 * Character sorting options
 */
@Serializable
enum class CharacterSortBy {
    NAME_ASC,
    NAME_DESC,
    CREATED_ASC,
    CREATED_DESC,
    UPDATED_ASC,
    UPDATED_DESC,
    MESSAGE_COUNT_ASC,
    MESSAGE_COUNT_DESC,
    FAVORITES_FIRST
}

/**
 * Character import/export formats
 */
@Serializable
enum class CharacterFormat {
    VORTEX_JSON,
    CHARACTER_CARD_V1,
    CHARACTER_CARD_V2,
    TAVERN_AI,
    SILLY_TAVERN
}

/**
 * Character card specification v2 model
 */
@Parcelize
@Serializable
data class CharacterCardV2(
    val spec: String = "chara_card_v2",
    val specVersion: String = "2.0",
    val data: CharacterCardData
) : Parcelable

/**
 * Character card data for v2 specification
 */
@Parcelize
@Serializable
data class CharacterCardData(
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val firstMes: String,
    val mesExample: String,
    val creatorNotes: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val alternateGreetings: List<String> = emptyList(),
    val characterBook: CharacterBook? = null,
    val tags: List<String> = emptyList(),
    val creator: String = "",
    val characterVersion: String = "1.0.0",
    val extensions: Map<String, String> = emptyMap()
) : Parcelable 
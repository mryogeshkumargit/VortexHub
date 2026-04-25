package com.vortexai.android.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vortexai.android.core.model.Character
import com.vortexai.android.core.model.CharacterBook

/**
 * Room entity for Character data
 * Optimized for companion app use cases with proper indexing
 */
@Entity(
    tableName = "characters",
    indices = [
        Index(value = ["name"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"]),
        Index(value = ["is_favorite"]),
        Index(value = ["creator_id"])
    ]
)
data class CharacterEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "personality")
    val personality: String,
    
    @ColumnInfo(name = "scenario")
    val scenario: String,
    
    @ColumnInfo(name = "first_message")
    val firstMessage: String,
    
    @ColumnInfo(name = "example_dialogue")
    val exampleDialogue: String,
    
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,
    
    @ColumnInfo(name = "avatar_video_url")
    val avatarVideoUrl: String? = null,
    
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),
    
    @ColumnInfo(name = "is_nsfw")
    val isNsfw: Boolean = false,
    
    @ColumnInfo(name = "creator_id")
    val creatorId: String? = null,
    
    @ColumnInfo(name = "creator_name")
    val creatorName: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "message_count")
    val messageCount: Long = 0,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "character_book")
    val characterBook: CharacterBook? = null,
    
    @ColumnInfo(name = "alternate_greetings")
    val alternateGreetings: List<String> = emptyList(),
    
    @ColumnInfo(name = "extensions")
    val extensions: Map<String, String> = emptyMap(),
    
    @ColumnInfo(name = "version")
    val version: Int = 1
)

/**
 * Convert database entity to domain model
 */
fun CharacterEntity.toDomainModel(): Character = Character(
    id = id,
    name = name,
    description = description,
    personality = personality,
    scenario = scenario,
    firstMessage = firstMessage,
    exampleDialogue = exampleDialogue,
    avatarUrl = avatarUrl,
    avatarVideoUrl = avatarVideoUrl,
    tags = tags,
    isNsfw = isNsfw,
    creatorId = creatorId,
    creatorName = creatorName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messageCount,
    isFavorite = isFavorite,
    characterBook = characterBook,
    alternateGreetings = alternateGreetings,
    extensions = extensions,
    version = version
)

/**
 * Convert domain model to database entity
 */
fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    name = name,
    description = description,
    personality = personality,
    scenario = scenario,
    firstMessage = firstMessage,
    exampleDialogue = exampleDialogue,
    avatarUrl = avatarUrl,
    avatarVideoUrl = avatarVideoUrl,
    tags = tags,
    isNsfw = isNsfw,
    creatorId = creatorId,
    creatorName = creatorName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messageCount,
    isFavorite = isFavorite,
    characterBook = characterBook,
    alternateGreetings = alternateGreetings,
    extensions = extensions,
    version = version
) 
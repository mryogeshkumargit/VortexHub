package com.vortexai.android.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.vortexai.android.core.model.*

/**
 * Room type converters for complex data types
 * Uses Kotlinx Serialization for reliable JSON conversion
 */
class Converters {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ================== LIST CONVERTERS ==================
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ================== MAP CONVERTERS ==================
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    // ================== CHARACTER BOOK CONVERTERS ==================
    
    @TypeConverter
    fun fromCharacterBook(value: CharacterBook?): String? {
        return value?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun toCharacterBook(value: String?): CharacterBook? {
        return value?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun fromCharacterBookEntryList(value: List<CharacterBookEntry>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toCharacterBookEntryList(value: String): List<CharacterBookEntry> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ================== CONVERSATION SETTINGS CONVERTERS ==================
    
    @TypeConverter
    fun fromConversationSettings(value: ConversationSettings): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toConversationSettings(value: String): ConversationSettings {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            ConversationSettings()
        }
    }
    
    // ================== MESSAGE METADATA CONVERTERS ==================
    
    @TypeConverter
    fun fromMessageMetadata(value: MessageMetadata): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toMessageMetadata(value: String): MessageMetadata {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            MessageMetadata()
        }
    }
    
    @TypeConverter
    fun fromMessageAttachmentList(value: List<MessageAttachment>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toMessageAttachmentList(value: String): List<MessageAttachment> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromMessageReactionList(value: List<MessageReaction>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toMessageReactionList(value: String): List<MessageReaction> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ================== ENUM CONVERTERS ==================
    
    @TypeConverter
    fun fromMessageRole(value: MessageRole): String {
        return value.name
    }
    
    @TypeConverter
    fun toMessageRole(value: String): MessageRole {
        return try {
            MessageRole.valueOf(value)
        } catch (e: Exception) {
            MessageRole.USER
        }
    }
    
    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String {
        return value.name
    }
    
    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType {
        return try {
            AttachmentType.valueOf(value)
        } catch (e: Exception) {
            AttachmentType.OTHER
        }
    }
    
    @TypeConverter
    fun fromReactionType(value: ReactionType): String {
        return value.name
    }
    
    @TypeConverter
    fun toReactionType(value: String): ReactionType {
        return try {
            ReactionType.valueOf(value)
        } catch (e: Exception) {
            ReactionType.LIKE
        }
    }
    
    @TypeConverter
    fun fromCharacterSortBy(value: CharacterSortBy): String {
        return value.name
    }
    
    @TypeConverter
    fun toCharacterSortBy(value: String): CharacterSortBy {
        return try {
            CharacterSortBy.valueOf(value)
        } catch (e: Exception) {
            CharacterSortBy.CREATED_DESC
        }
    }
    
    @TypeConverter
    fun fromConversationSortBy(value: ConversationSortBy): String {
        return value.name
    }
    
    @TypeConverter
    fun toConversationSortBy(value: String): ConversationSortBy {
        return try {
            ConversationSortBy.valueOf(value)
        } catch (e: Exception) {
            ConversationSortBy.UPDATED_DESC
        }
    }
    
    @TypeConverter
    fun fromConnectionStatus(value: ConnectionStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toConnectionStatus(value: String): ConnectionStatus {
        return try {
            ConnectionStatus.valueOf(value)
        } catch (e: Exception) {
            ConnectionStatus.DISCONNECTED
        }
    }
} 
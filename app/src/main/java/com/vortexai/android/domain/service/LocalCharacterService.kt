package com.vortexai.android.domain.service

import android.util.Log
import com.vortexai.android.data.database.dao.CharacterDao
import com.vortexai.android.data.database.dao.ConversationDao
import com.vortexai.android.data.database.dao.MessageDao
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.local.CharacterLocalDataSource
import com.vortexai.android.utils.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCharacterService @Inject constructor(
    private val characterDao: CharacterDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val characterLocalDataSource: CharacterLocalDataSource
) {
    
    /**
     * Get all active characters with pagination
     */
    suspend fun getAllCharacters(
        limit: Int = Int.MAX_VALUE, // Remove limit to show all characters
        offset: Int = 0,
        publicOnly: Boolean = true
    ): List<Character> {
        return try {
            if (publicOnly) {
                characterDao.getPublicCharactersPaginated(limit, offset)
            } else {
                characterDao.getCharactersWithFilters(
                    isPublic = null,
                    limit = limit,
                    offset = offset
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting characters")
            emptyList()
        }
    }
    
    /**
     * Get a character by ID
     */
    suspend fun getCharacterById(characterId: String): Character? {
        return try {
            characterDao.getCharacterById(characterId)
        } catch (e: Exception) {
            Timber.e(e, "Character not found: $characterId")
            null
        }
    }
    
    /**
     * Get a character by ID as Flow for reactive UI
     */
    fun getCharacterByIdFlow(characterId: String): Flow<Character?> {
        return characterDao.getCharacterByIdFlow(characterId)
    }
    
    /**
     * Get a character by name
     */
    suspend fun getCharacterByName(name: String): Character? {
        return try {
            characterDao.getCharacterByName(name)
        } catch (e: Exception) {
            Timber.e(e, "Error getting character by name: $name")
            null
        }
    }
    
    /**
     * Create a new character
     */
    suspend fun createCharacter(
        characterData: Map<String, Any>,
        creatorId: String? = null
    ): Character? {
        return try {
            // Validate required fields
            val requiredFields = listOf("name", "persona", "greeting")
            for (field in requiredFields) {
                if (!characterData.containsKey(field) || characterData[field].toString().isBlank()) {
                    throw IllegalArgumentException("Missing required field: $field")
                }
            }
            
            val name = characterData["name"].toString().trim()
            
            // Check if character name already exists
            val existing = getCharacterByName(name)
            if (existing != null) {
                throw IllegalArgumentException("Character with name '$name' already exists")
            }
            
            // Create character entity
            val character = Character(
                id = IdGenerator.generateCharacterId(),
                name = name,
                displayName = characterData["displayName"]?.toString() ?: name,
                shortDescription = characterData["shortDescription"]?.toString(),
                longDescription = characterData["longDescription"]?.toString(),
                persona = characterData["persona"]?.toString() ?: "",
                backstory = characterData["backstory"]?.toString(),
                greeting = characterData["greeting"]?.toString() ?: "Hello!",
                avatarUrl = characterData["avatarUrl"]?.toString(),
                appearance = characterData["appearance"]?.toString(),
                personality = characterData["personality"]?.toString(),
                scenario = characterData["scenario"]?.toString(),
                exampleDialogue = characterData["exampleDialogue"]?.toString(),
                temperature = (characterData["temperature"] as? Number)?.toFloat() ?: 0.7f,
                topP = (characterData["topP"] as? Number)?.toFloat() ?: 0.9f,
                maxTokens = (characterData["maxTokens"] as? Number)?.toInt() ?: 512,
                nsfwEnabled = characterData["nsfwEnabled"] as? Boolean ?: false,
                tags = when (val tagsData = characterData["tags"]) {
                    is List<*> -> tagsData.filterIsInstance<String>()
                    is String -> tagsData.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    else -> emptyList()
                },
                categories = when (val categoriesData = characterData["categories"]) {
                    is List<*> -> categoriesData.filterIsInstance<String>()
                    is String -> categoriesData.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    else -> emptyList()
                },
                creatorId = creatorId,
                creator = characterData["creator"]?.toString(),
                creatorNotes = characterData["creatorNotes"]?.toString(),
                isPublic = characterData["isPublic"] as? Boolean ?: true,
                isActive = true
            )
            
            characterDao.insertCharacter(character)
            Timber.i("Created character: ${character.name}")
            character
            
        } catch (e: Exception) {
            Timber.e(e, "Error creating character")
            null
        }
    }
    
    /**
     * Update an existing character
     */
    suspend fun updateCharacter(
        characterId: String,
        updateData: Map<String, Any>
    ): Character? {
        return try {
            val character = getCharacterById(characterId) ?: return null
            
            // Create updated character
            val updatedCharacter = character.copy(
                displayName = updateData["displayName"]?.toString() ?: character.displayName,
                shortDescription = updateData["shortDescription"]?.toString() ?: character.shortDescription,
                longDescription = updateData["longDescription"]?.toString() ?: character.longDescription,
                persona = updateData["persona"]?.toString() ?: character.persona,
                backstory = updateData["backstory"]?.toString() ?: character.backstory,
                greeting = updateData["greeting"]?.toString() ?: character.greeting,
                avatarUrl = updateData["avatarUrl"]?.toString() ?: character.avatarUrl,
                appearance = updateData["appearance"]?.toString() ?: character.appearance,
                personality = updateData["personality"]?.toString() ?: character.personality,
                scenario = updateData["scenario"]?.toString() ?: character.scenario,
                exampleDialogue = updateData["exampleDialogue"]?.toString() ?: character.exampleDialogue,
                temperature = (updateData["temperature"] as? Number)?.toFloat() ?: character.temperature,
                topP = (updateData["topP"] as? Number)?.toFloat() ?: character.topP,
                maxTokens = (updateData["maxTokens"] as? Number)?.toInt() ?: character.maxTokens,
                nsfwEnabled = updateData["nsfwEnabled"] as? Boolean ?: character.nsfwEnabled,
                tags = when (val tagsData = updateData["tags"]) {
                    is List<*> -> tagsData.filterIsInstance<String>()
                    is String -> tagsData.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    else -> character.tags
                },
                categories = when (val categoriesData = updateData["categories"]) {
                    is List<*> -> categoriesData.filterIsInstance<String>()
                    is String -> categoriesData.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    else -> character.categories
                },
                creatorNotes = updateData["creatorNotes"]?.toString() ?: character.creatorNotes,
                isPublic = updateData["isPublic"] as? Boolean ?: character.isPublic,
                isFeatured = updateData["isFeatured"] as? Boolean ?: character.isFeatured
            )
            
            characterDao.updateCharacter(updatedCharacter)
            Timber.i("Updated character: ${updatedCharacter.name}")
            updatedCharacter
            
        } catch (e: Exception) {
            Timber.e(e, "Error updating character")
            null
        }
    }
    
    /**
     * Delete a character (soft delete by default, hard delete if specified)
     */
    suspend fun deleteCharacter(
        characterId: String,
        hardDelete: Boolean = false
    ): Boolean {
        return try {
            val character = getCharacterById(characterId) ?: return false
            val characterName = character.name
            
            if (hardDelete) {
                // Clean up all associated data first
                cleanupCharacterData(characterId)
                
                // Hard delete - completely remove from database
                characterDao.deleteCharacterById(characterId)
                Timber.i("Hard deleted character: $characterName")
            } else {
                // Soft delete - mark as inactive
                characterDao.softDeleteCharacter(characterId)
                Timber.i("Soft deleted character: $characterName")
            }
            
            true
            
        } catch (e: Exception) {
            Timber.e(e, "Error deleting character")
            false
        }
    }
    
    /**
     * Delete a character by name
     */
    suspend fun deleteCharacterByName(
        name: String,
        hardDelete: Boolean = false
    ): Boolean {
        return try {
            val character = getCharacterByName(name) ?: return false
            deleteCharacter(character.id, hardDelete)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting character by name")
            false
        }
    }
    
    /**
     * Clean up all data associated with a character
     */
    private suspend fun cleanupCharacterData(characterId: String) {
        try {
            // Delete associated messages
            messageDao.deleteMessagesByCharacter(characterId)
            
            // Delete associated conversations
            conversationDao.deleteConversationsByCharacter(characterId)
            
            Timber.i("Cleaned up data for character $characterId")
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up character data")
        }
    }
    
    /**
     * Search characters by name, description, or tags
     */
    suspend fun searchCharacters(
        query: String,
        limit: Int = 20
    ): List<Character> {
        return try {
            characterDao.searchCharacters(query, limit)
        } catch (e: Exception) {
            Timber.e(e, "Error searching characters")
            emptyList()
        }
    }
    
    /**
     * Get featured characters
     */
    suspend fun getFeaturedCharacters(limit: Int = 10): List<Character> {
        return try {
            characterDao.getFeaturedCharacters(limit)
        } catch (e: Exception) {
            Timber.e(e, "Error getting featured characters")
            emptyList()
        }
    }
    
    /**
     * Get popular characters
     */
    suspend fun getPopularCharacters(limit: Int = 10): List<Character> {
        return try {
            characterDao.getPopularCharacters(limit)
        } catch (e: Exception) {
            Timber.e(e, "Error getting popular characters")
            emptyList()
        }
    }
    
    /**
     * Get recent characters
     */
    suspend fun getRecentCharacters(limit: Int = 10): List<Character> {
        return try {
            characterDao.getRecentCharacters(limit)
        } catch (e: Exception) {
            Timber.e(e, "Error getting recent characters")
            emptyList()
        }
    }
    
    /**
     * Clone an existing character
     */
    suspend fun cloneCharacter(
        characterId: String,
        newName: String,
        creatorId: String? = null
    ): Character? {
        return try {
            val original = getCharacterById(characterId) ?: return null
            
            // Check if new name already exists
            val existing = getCharacterByName(newName)
            if (existing != null) {
                throw IllegalArgumentException("Character with name '$newName' already exists")
            }
            
            // Create clone
            val cloned = original.copy(
                id = IdGenerator.generateCharacterId(),
                name = newName,
                displayName = newName,
                creatorId = creatorId,
                creator = creatorId ?: "cloned",
                totalMessages = 0,
                totalConversations = 0,
                averageRating = 0.0f,
                totalRatings = 0,
                lastInteraction = null
            )
            
            characterDao.insertCharacter(cloned)
            Timber.i("Cloned character $characterId as $newName")
            cloned
            
        } catch (e: Exception) {
            Timber.e(e, "Error cloning character")
            null
        }
    }
    
    /**
     * Export a character in standard format
     */
    suspend fun exportCharacter(characterId: String): Map<String, Any>? {
        return try {
            val character = getCharacterById(characterId) ?: return null
            
            mapOf(
                "id" to character.id,
                "name" to character.name,
                "displayName" to (character.displayName ?: ""),
                "shortDescription" to (character.shortDescription ?: ""),
                "longDescription" to (character.longDescription ?: ""),
                "persona" to (character.persona ?: ""),
                "backstory" to (character.backstory ?: ""),
                "greeting" to (character.greeting ?: ""),
                "avatarUrl" to (character.avatarUrl ?: ""),
                "appearance" to (character.appearance ?: ""),
                "personality" to (character.personality ?: ""),
                "scenario" to (character.scenario ?: ""),
                "exampleDialogue" to (character.exampleDialogue ?: ""),
                "temperature" to character.temperature,
                "topP" to character.topP,
                "maxTokens" to character.maxTokens,
                "nsfwEnabled" to character.nsfwEnabled,
                "tags" to (character.tags ?: ""),
                "categories" to (character.categories ?: ""),
                "creator" to (character.creator ?: ""),
                "creatorNotes" to (character.creatorNotes ?: ""),
                "characterVersion" to character.characterVersion,
                "isPublic" to character.isPublic,
                "isFeatured" to character.isFeatured,
                "export_metadata" to mapOf(
                    "exported_at" to Date().time,
                    "format_version" to "1.0",
                    "source" to "vortex_android"
                )
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error exporting character")
            null
        }
    }
    
    /**
     * Import a character from external data
     */
    suspend fun importCharacter(
        characterData: Map<String, Any>,
        creatorId: String? = null
    ): Character? {
        return try {
            // Clean import data
            val cleanData = characterData.toMutableMap()
            
            // Remove non-model fields
            cleanData.remove("id")
            cleanData.remove("_id")
            cleanData.remove("export_metadata")
            
            // Set creator
            if (creatorId != null) {
                cleanData["creator"] = creatorId
            }
            
            // Create character
            createCharacter(cleanData, creatorId)
            
        } catch (e: Exception) {
            Timber.e(e, "Error importing character")
            null
        }
    }
    
    /**
     * Update character statistics
     */
    suspend fun updateCharacterStats(
        characterId: String,
        messageCount: Int = 1,
        rating: Float? = null
    ) {
        try {
            val character = getCharacterById(characterId) ?: return
            
            val newTotalMessages = character.totalMessages + messageCount
            characterDao.updateCharacterMessageCount(characterId, newTotalMessages)
            
            if (rating != null) {
                val totalScore = character.averageRating * character.totalRatings
                val newTotalRatings = character.totalRatings + 1
                val newAverageRating = (totalScore + rating) / newTotalRatings
                
                characterDao.updateCharacterRating(characterId, newAverageRating, newTotalRatings)
            }
            
            characterDao.updateLastInteraction(characterId, Date())
            
        } catch (e: Exception) {
            Timber.e(e, "Error updating character stats")
        }
    }
    
    /**
     * Get all active characters as Flow for reactive UI
     */
    fun getAllActiveCharactersFlow(): Flow<List<Character>> {
        return characterDao.getAllActiveCharacters()
    }
    
    /**
     * Get public characters as Flow for reactive UI
     */
    fun getPublicCharactersFlow(): Flow<List<Character>> {
        return characterDao.getPublicCharacters()
    }
    
    /**
     * Get characters by category
     */
    suspend fun getCharactersByCategory(category: String): List<Character> {
        return try {
            characterDao.getCharactersByCategory(category)
        } catch (e: Exception) {
            Timber.e(e, "Error getting characters by category")
            emptyList()
        }
    }
    
    /**
     * Get characters by tag
     */
    suspend fun getCharactersByTag(tag: String): List<Character> {
        return try {
            characterDao.getCharactersByTag(tag)
        } catch (e: Exception) {
            Timber.e(e, "Error getting characters by tag")
            emptyList()
        }
    }
    
    /**
     * Check if character exists by name
     */
    suspend fun characterExists(name: String): Boolean {
        return try {
            characterDao.characterExistsByName(name)
        } catch (e: Exception) {
            Timber.e(e, "Error checking character existence")
            false
        }
    }
} 
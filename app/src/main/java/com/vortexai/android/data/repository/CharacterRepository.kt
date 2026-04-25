package com.vortexai.android.data.repository

import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vortexai.android.data.local.CharacterLocalDataSource
import com.vortexai.android.data.models.*
import com.vortexai.android.data.database.dao.CharacterDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified character repository
 */
@Singleton
class CharacterRepository @Inject constructor(
    private val localDataSource: CharacterLocalDataSource,
    private val characterDao: CharacterDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "CharacterRepository"
    }
    
    /**
     * Get characters
     */
    fun getCharacters(
        page: Int = 1,
        limit: Int = Int.MAX_VALUE, // Remove limit to show all characters
        search: String? = null,
        forceRefresh: Boolean = false
    ): Flow<Result<CharacterListResponse>> {
        return characterDao.getAllActiveCharacters().map { characters ->
            try {
                Log.d(TAG, "Getting characters: ${characters.size} found")
                
                // Apply search filter if provided
                val filteredCharacters = if (search.isNullOrBlank()) {
                    characters
                } else {
                    characters.filter { character ->
                        character.name.contains(search, ignoreCase = true) ||
                        character.shortDescription?.contains(search, ignoreCase = true) == true ||
                        character.personality?.contains(search, ignoreCase = true) == true
                    }
                }
                
                // Return all characters (no pagination needed)
                val allCharacters = filteredCharacters
                
                val totalPages = 1
                
                val response = CharacterListResponse(
                    characters = allCharacters,
                    pagination = Pagination(
                        page = 1,
                        limit = allCharacters.size,
                        total = allCharacters.size,
                        pages = 1
                    ),
                    success = true,
                    hasMore = false
                )
                
                Result.success(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing characters", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get character by ID
     */
    fun getCharacterById(id: String): Flow<Result<Character?>> {
        return characterDao.getCharacterByIdFlow(id).map { character ->
            try {
                Log.d(TAG, "Getting character: $id")
                Result.success(character)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting character", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get character by ID (suspend function)
     */
    suspend fun getCharacterByIdSync(id: String): Character? {
        return try {
            Log.d(TAG, "Getting character: $id")
            withContext(Dispatchers.IO) {
                characterDao.getCharacterById(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting character", e)
            null
        }
    }
    
    /**
     * Get character by name (suspend function)
     */
    suspend fun getCharacterByName(name: String): Character? {
        return try {
            Log.d(TAG, "Getting character by name: $name")
            withContext(Dispatchers.IO) {
                characterDao.getCharacterByName(name)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting character by name", e)
            null
        }
    }
    
    /**
     * Save a new character
     */
    suspend fun saveCharacter(character: Character): Result<String> {
        return try {
            Log.d(TAG, "Saving character: ${character.name}")
            withContext(Dispatchers.IO) {
                characterDao.insertCharacter(character)
            }
            Log.d(TAG, "Character saved successfully: ${character.id}")
            Result.success(character.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving character: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing character
     */
    suspend fun updateCharacter(character: Character): Result<Unit> {
        return try {
            Log.d(TAG, "Updating character: ${character.name}")
            withContext(Dispatchers.IO) {
                characterDao.updateCharacter(character)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating character: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a character
     */
    fun deleteCharacter(characterId: String): Flow<Result<Unit>> = flow {
        try {
            Log.d(TAG, "Deleting character: $characterId")
            withContext(Dispatchers.IO) {
                characterDao.softDeleteCharacter(characterId)
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting character: ${e.message}", e)
            emit(Result.failure(e))
        }
    }
    
    // Stub implementations for missing methods
    fun clearExpiredCache() {
        // TODO: Implement cache clearing
    }
    
    fun getPopularCharacters(forceRefresh: Boolean = false, search: String? = null): Flow<Result<CharacterListResponse>> = flow {
        try {
            var popular = characterDao.getPopularCharacters(limit = 50)
            
            // Apply search filter if provided
            if (!search.isNullOrBlank()) {
                popular = popular.filter { character ->
                    character.name.contains(search, ignoreCase = true) ||
                    character.shortDescription?.contains(search, ignoreCase = true) == true ||
                    character.personality?.contains(search, ignoreCase = true) == true
                }
            }
            
            emit(Result.success(CharacterListResponse(
                characters = popular,
                pagination = null,
                success = true,
                hasMore = false
            )))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    fun getCharacter(id: String, forceRefresh: Boolean = false): Flow<Result<Character>> {
        return characterDao.getCharacterByIdFlow(id).map { character ->
            try {
                Log.d(TAG, "Getting character: $id")
                if (character != null) {
                    Result.success(character)
                } else {
                    Log.w(TAG, "Character not found: $id")
                    Result.failure(Exception("Character not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting character: $id", e)
                Result.failure(e)
            }
        }
    }
    
    fun preloadCharacters() {
        // TODO: Implement preloading
    }
    
    fun clearCache() {
        // TODO: Implement cache clearing
    }
    
    fun getCacheInfo(): Flow<Result<Any>> = flow {
        emit(Result.success("No cache info available"))
    }
    
    fun getFavoriteCharacters(search: String? = null): Flow<Result<CharacterListResponse>> = flow {
        val all = characterDao.getAllCharacters()
        var favs = all.filter { it.isFavorite }
        
        // Apply search filter if provided
        if (!search.isNullOrBlank()) {
            favs = favs.filter { character ->
                character.name.contains(search, ignoreCase = true) ||
                character.shortDescription?.contains(search, ignoreCase = true) == true ||
                character.personality?.contains(search, ignoreCase = true) == true
            }
        }
        
        emit(Result.success(CharacterListResponse(
            characters = favs,
            pagination = null,
            success = true,
            hasMore = false
        )))
    }
    
    fun getMyCharacters(search: String? = null): Flow<Result<CharacterListResponse>> = flow {
        // Heuristic: characters with creatorId set belong to current user in this local-only build
        val all = characterDao.getAllCharacters()
        var mine = all.filter { it.creatorId != null }
        
        // Apply search filter if provided
        if (!search.isNullOrBlank()) {
            mine = mine.filter { character ->
                character.name.contains(search, ignoreCase = true) ||
                character.shortDescription?.contains(search, ignoreCase = true) == true ||
                character.personality?.contains(search, ignoreCase = true) == true
            }
        }
        
        emit(Result.success(CharacterListResponse(
            characters = mine,
            pagination = null,
            success = true,
            hasMore = false
        )))
    }
    
    fun toggleFavorite(characterId: String): Flow<Result<Unit>> = flow {
        try {
            val current = characterDao.getCharacterById(characterId)
            if (current != null) {
                characterDao.updateFavoriteById(characterId, !current.isFavorite)
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get featured characters list for home screen
     */
    suspend fun getFeaturedCharactersList(limit: Int = 6): List<Character> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting featured characters with limit: $limit")
                characterDao.getFeaturedCharacters(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting featured characters", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get popular characters list for home screen
     */
    suspend fun getPopularCharactersList(limit: Int = 8): List<Character> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting popular characters with limit: $limit")
                characterDao.getPopularCharacters(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting popular characters", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get recent characters list for home screen
     */
    suspend fun getRecentCharactersList(limit: Int = 10): List<Character> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting recent characters with limit: $limit")
                characterDao.getRecentCharacters(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting recent characters", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get total number of characters
     */
    suspend fun getTotalCharacterCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                characterDao.getActiveCharacterCount()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting total character count", e)
                0
            }
        }
    }
} 
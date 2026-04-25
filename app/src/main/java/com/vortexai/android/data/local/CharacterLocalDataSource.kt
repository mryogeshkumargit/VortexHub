package com.vortexai.android.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.vortexai.android.data.models.Character
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for character data
 * Provides sample characters for standalone app
 */
@Singleton
class CharacterLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "CharacterLocalDataSource"
        private const val PREFS_NAME = "character_cache"
        private const val KEY_CHARACTERS_TIMESTAMP = "characters_timestamp"
        private const val KEY_POPULAR_TIMESTAMP = "popular_timestamp"
        private const val KEY_CHARACTER_COUNT = "character_count"
        private const val KEY_HAS_CHARACTER_LIST = "has_character_list"
        private const val KEY_HAS_POPULAR_LIST = "has_popular_list"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Cache characters data
     */
    fun cacheCharacters(characters: List<Character>) {
        Log.d(TAG, "Caching ${characters.size} characters")
        
        prefs.edit().apply {
            putInt(KEY_CHARACTER_COUNT, characters.size)
            putBoolean(KEY_HAS_CHARACTER_LIST, true)
            putLong(KEY_CHARACTERS_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Cache popular characters data
     */
    fun cachePopularCharacters(characters: List<Character>) {
        Log.d(TAG, "Caching ${characters.size} popular characters")
        
        prefs.edit().apply {
            putBoolean(KEY_HAS_POPULAR_LIST, true)
            putLong(KEY_POPULAR_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Check if character cache is valid
     */
    fun isCacheValid(maxAge: Long = 5 * 60 * 1000): Boolean { // 5 minutes default
        val timestamp = prefs.getLong(KEY_CHARACTERS_TIMESTAMP, 0)
        val age = System.currentTimeMillis() - timestamp
        val isValid = age < maxAge && prefs.getBoolean(KEY_HAS_CHARACTER_LIST, false)
        
        Log.d(TAG, "Cache valid: $isValid (age: ${age}ms, maxAge: ${maxAge}ms)")
        return isValid
    }
    
    /**
     * Check if popular characters cache is valid
     */
    fun isPopularCacheValid(maxAge: Long = 10 * 60 * 1000): Boolean { // 10 minutes default
        val timestamp = prefs.getLong(KEY_POPULAR_TIMESTAMP, 0)
        val age = System.currentTimeMillis() - timestamp
        val isValid = age < maxAge && prefs.getBoolean(KEY_HAS_POPULAR_LIST, false)
        
        Log.d(TAG, "Popular cache valid: $isValid (age: ${age}ms, maxAge: ${maxAge}ms)")
        return isValid
    }
    
    /**
     * Clear character cache
     */
    fun clearCache() {
        Log.d(TAG, "Clearing character cache")
        prefs.edit().clear().apply()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheInfo(): CacheInfo {
        val characterCount = prefs.getInt(KEY_CHARACTER_COUNT, 0)
        val hasCharacterList = prefs.getBoolean(KEY_HAS_CHARACTER_LIST, false)
        val hasPopularList = prefs.getBoolean(KEY_HAS_POPULAR_LIST, false)
        
        return CacheInfo(
            cachedCharacterCount = characterCount,
            hasCharacterList = hasCharacterList,
            hasPopularList = hasPopularList,
            lastCharacterListUpdate = prefs.getLong(KEY_CHARACTERS_TIMESTAMP, 0),
            lastPopularListUpdate = prefs.getLong(KEY_POPULAR_TIMESTAMP, 0)
        )
    }
    
    /**
     * Get all available characters
     */
    fun getAllCharacters(): List<Character> {
        Log.d(TAG, "Getting all local characters")
        return getSampleCharacters()
    }
    
    /**
     * Get character by ID
     */
    fun getCharacterById(id: String): Character? {
        Log.d(TAG, "Getting character by ID: $id")
        return getSampleCharacters().find { it.id == id }
    }
    
    /**
     * Get featured characters
     */
    fun getFeaturedCharacters(): List<Character> {
        Log.d(TAG, "Getting featured characters")
        return getSampleCharacters().filter { it.isFeatured }
    }
    
    /**
     * Search characters by name or description
     */
    fun searchCharacters(query: String): List<Character> {
        Log.d(TAG, "Searching characters with query: $query")
        return getSampleCharacters().filter { character ->
            character.name.contains(query, ignoreCase = true) ||
            character.shortDescription?.contains(query, ignoreCase = true) == true ||
            character.tags?.any { it.contains(query, ignoreCase = true) } == true
        }
    }
    
    /**
     * Get sample characters for offline/demo use
     */
    private fun getSampleCharacters(): List<Character> {
        // Demo characters have been removed
        // Users should create their own characters or import character cards
        return emptyList()
    }
}

/**
 * Cache information data class
 */
data class CacheInfo(
    val cachedCharacterCount: Int,
    val hasCharacterList: Boolean,
    val hasPopularList: Boolean,
    val lastCharacterListUpdate: Long,
    val lastPopularListUpdate: Long
) 
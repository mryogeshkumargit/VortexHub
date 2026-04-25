package com.vortexai.android.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.vortexai.android.core.database.entity.CharacterEntity

/**
 * Data Access Object for Character operations
 * Provides reactive queries with Flow for real-time UI updates
 */
@Dao
interface CharacterDao {
    
    // ================== BASIC CRUD OPERATIONS ==================
    
    @Query("SELECT * FROM characters ORDER BY updated_at DESC")
    fun getAllCharacters(): Flow<List<CharacterEntity>>
    
    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacterById(id: String): Flow<CharacterEntity?>
    
    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterByIdSync(id: String): CharacterEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<CharacterEntity>)
    
    @Update
    suspend fun updateCharacter(character: CharacterEntity)
    
    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
    
    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacterById(id: String)
    
    @Query("DELETE FROM characters")
    suspend fun deleteAllCharacters()
    
    // ================== SEARCH AND FILTERING ==================
    
    @Query("""
        SELECT * FROM characters 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR personality LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN name LIKE '%' || :query || '%' THEN 1 ELSE 2 END,
            updated_at DESC
    """)
    fun searchCharacters(query: String): Flow<List<CharacterEntity>>
    
    @Query("SELECT * FROM characters WHERE is_favorite = 1 ORDER BY updated_at DESC")
    fun getFavoriteCharacters(): Flow<List<CharacterEntity>>
    
    @Query("SELECT * FROM characters WHERE is_nsfw = :includeNsfw ORDER BY updated_at DESC")
    fun getCharactersByNsfwFilter(includeNsfw: Boolean): Flow<List<CharacterEntity>>
    
    @Query("""
        SELECT * FROM characters 
        WHERE (:includeNsfw = 1 OR is_nsfw = 0)
          AND (:favoritesOnly = 0 OR is_favorite = 1)
          AND (CASE WHEN :query = '' THEN 1 
               ELSE (name LIKE '%' || :query || '%' 
                     OR description LIKE '%' || :query || '%'
                     OR personality LIKE '%' || :query || '%') END)
        ORDER BY 
            CASE :sortBy
                WHEN 'NAME_ASC' THEN name
                WHEN 'NAME_DESC' THEN name
                WHEN 'CREATED_ASC' THEN created_at
                WHEN 'CREATED_DESC' THEN created_at
                WHEN 'UPDATED_ASC' THEN updated_at
                WHEN 'UPDATED_DESC' THEN updated_at
                WHEN 'MESSAGE_COUNT_ASC' THEN message_count
                WHEN 'MESSAGE_COUNT_DESC' THEN message_count
                WHEN 'FAVORITES_FIRST' THEN is_favorite
                ELSE updated_at
            END DESC
    """)
    fun getFilteredCharacters(
        query: String,
        includeNsfw: Boolean,
        favoritesOnly: Boolean,
        sortBy: String
    ): Flow<List<CharacterEntity>>
    
    // ================== STATISTICS AND ANALYTICS ==================
    
    @Query("SELECT COUNT(*) FROM characters")
    fun getCharacterCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM characters WHERE is_favorite = 1")
    fun getFavoriteCharacterCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM characters WHERE is_nsfw = 1")
    fun getNsfwCharacterCount(): Flow<Int>
    
    @Query("SELECT SUM(message_count) FROM characters")
    fun getTotalMessageCount(): Flow<Long>
    
    @Query("""
        SELECT * FROM characters 
        WHERE message_count > 0 
        ORDER BY message_count DESC 
        LIMIT :limit
    """)
    fun getMostUsedCharacters(limit: Int = 10): Flow<List<CharacterEntity>>
    
    @Query("""
        SELECT * FROM characters 
        ORDER BY created_at DESC 
        LIMIT :limit
    """)
    fun getRecentlyCreatedCharacters(limit: Int = 10): Flow<List<CharacterEntity>>
    
    @Query("""
        SELECT * FROM characters 
        ORDER BY updated_at DESC 
        LIMIT :limit
    """)
    fun getRecentlyUpdatedCharacters(limit: Int = 10): Flow<List<CharacterEntity>>
    
    // ================== BULK OPERATIONS ==================
    
    @Query("UPDATE characters SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)
    
    @Query("UPDATE characters SET message_count = message_count + 1 WHERE id = :id")
    suspend fun incrementMessageCount(id: String)
    
    @Query("UPDATE characters SET updated_at = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE characters SET is_favorite = 0")
    suspend fun clearAllFavorites()
    
    @Query("DELETE FROM characters WHERE created_at < :timestamp")
    suspend fun deleteCharactersOlderThan(timestamp: Long)
    
    // ================== ADVANCED QUERIES ==================
    
    @Query("""
        SELECT DISTINCT tags FROM characters, json_each(characters.tags)
        WHERE json_each.value != ''
    """)
    fun getAllTags(): Flow<List<String>>
    
    @Query("""
        SELECT * FROM characters 
        WHERE EXISTS (
            SELECT 1 FROM json_each(characters.tags) 
            WHERE json_each.value IN (:tags)
        )
        ORDER BY updated_at DESC
    """)
    fun getCharactersByTags(tags: List<String>): Flow<List<CharacterEntity>>
    
    @Query("""
        SELECT * FROM characters 
        WHERE creator_id = :creatorId 
        ORDER BY created_at DESC
    """)
    fun getCharactersByCreator(creatorId: String): Flow<List<CharacterEntity>>
    
    @Query("""
        SELECT * FROM characters 
        WHERE created_at BETWEEN :startDate AND :endDate 
        ORDER BY created_at DESC
    """)
    fun getCharactersByDateRange(startDate: Long, endDate: Long): Flow<List<CharacterEntity>>
    
    // ================== TRANSACTION OPERATIONS ==================
    
    @Transaction
    suspend fun upsertCharacter(character: CharacterEntity) {
        val existing = getCharacterByIdSync(character.id)
        if (existing != null) {
            updateCharacter(character.copy(
                createdAt = existing.createdAt, // Preserve original creation time
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            insertCharacter(character)
        }
    }
    
    @Transaction
    suspend fun replaceAllCharacters(characters: List<CharacterEntity>) {
        deleteAllCharacters()
        insertCharacters(characters)
    }
} 
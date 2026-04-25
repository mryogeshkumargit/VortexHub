package com.vortexai.android.data.database.dao

import androidx.room.*
import androidx.paging.PagingSource
import com.vortexai.android.data.models.Character
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    
    // Basic CRUD Operations
    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: String): Character?
    
    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacterByIdFlow(id: String): Flow<Character?>
    
    @Query("SELECT * FROM characters WHERE name = :name AND isActive = 1")
    suspend fun getCharacterByName(name: String): Character?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: Character)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<Character>)
    
    @Update
    suspend fun updateCharacter(character: Character)
    
    @Delete
    suspend fun deleteCharacter(character: Character)
    
    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacterById(id: String)
    
    @Query("DELETE FROM characters")
    suspend fun deleteAllCharacters()
    
    // Soft delete (mark as inactive)
    @Query("UPDATE characters SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteCharacter(id: String)
    
    // Restore soft deleted character
    @Query("UPDATE characters SET isActive = 1 WHERE id = :id")
    suspend fun restoreCharacter(id: String)
    
    // Listing and Filtering
    @Query("SELECT * FROM characters WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCharacters(): Flow<List<Character>>

    @Query("SELECT * FROM characters")
    suspend fun getAllCharacters(): List<Character>

    @Query("UPDATE characters SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteById(id: String, isFavorite: Boolean)
    
    @Query("SELECT * FROM characters WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCharactersPaging(): PagingSource<Int, Character>
    
    @Query("SELECT * FROM characters WHERE isActive = 1 AND isPublic = 1 ORDER BY name ASC")
    fun getPublicCharacters(): Flow<List<Character>>
    
    @Query("SELECT * FROM characters WHERE isActive = 1 AND isPublic = 1 ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getPublicCharactersPaginated(limit: Int, offset: Int): List<Character>
    
    @Query("SELECT * FROM characters WHERE isActive = 1 AND isFeatured = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getFeaturedCharacters(limit: Int = 10): List<Character>
    
    @Query("SELECT * FROM characters WHERE isActive = 1 ORDER BY totalConversations DESC, totalMessages DESC, RANDOM() LIMIT :limit")
    suspend fun getPopularCharacters(limit: Int = 10): List<Character>
    
    @Query("SELECT * FROM characters WHERE isActive = 1 ORDER BY name ASC LIMIT :limit")
    suspend fun getRecentCharacters(limit: Int = 10): List<Character>
    
    // Search functionality
    @Query("""
        SELECT * FROM characters 
        WHERE isActive = 1 
        AND (
            name LIKE '%' || :query || '%' 
            OR displayName LIKE '%' || :query || '%'
            OR shortDescription LIKE '%' || :query || '%'
            OR tags LIKE '%' || :query || '%'
        )
        ORDER BY 
            CASE WHEN name LIKE :query || '%' THEN 1 ELSE 2 END,
            name ASC
        LIMIT :limit
    """)
    suspend fun searchCharacters(query: String, limit: Int = 20): List<Character>
    
    @Query("""
        SELECT * FROM characters 
        WHERE isActive = 1 
        AND categories LIKE '%' || :category || '%'
        ORDER BY name ASC
    """)
    suspend fun getCharactersByCategory(category: String): List<Character>
    
    @Query("""
        SELECT * FROM characters 
        WHERE isActive = 1 
        AND tags LIKE '%' || :tag || '%'
        ORDER BY name ASC
    """)
    suspend fun getCharactersByTag(tag: String): List<Character>
    
    // Statistics and Analytics
    @Query("SELECT COUNT(*) FROM characters WHERE isActive = 1")
    suspend fun getActiveCharacterCount(): Int
    
    @Query("SELECT COUNT(*) FROM characters WHERE isActive = 1 AND isPublic = 1")
    suspend fun getPublicCharacterCount(): Int
    
    @Query("SELECT COUNT(*) FROM characters WHERE isActive = 1 AND creatorId = :creatorId")
    suspend fun getCharacterCountByCreator(creatorId: String): Int
    
    @Query("SELECT AVG(averageRating) FROM characters WHERE isActive = 1 AND totalRatings > 0")
    suspend fun getAverageCharacterRating(): Float?
    
    @Query("SELECT SUM(totalMessages) FROM characters WHERE isActive = 1")
    suspend fun getTotalMessagesAcrossAllCharacters(): Int
    
    // Character maintenance
    @Query("SELECT * FROM characters WHERE isActive = 0")
    suspend fun getInactiveCharacters(): List<Character>
    
    @Query("DELETE FROM characters WHERE isActive = 0")
    suspend fun deleteAllInactiveCharacters()
    
    @Query("UPDATE characters SET totalMessages = :totalMessages WHERE id = :id")
    suspend fun updateCharacterMessageCount(id: String, totalMessages: Int)
    
    @Query("UPDATE characters SET totalConversations = :totalConversations WHERE id = :id")
    suspend fun updateCharacterConversationCount(id: String, totalConversations: Int)
    
    @Query("UPDATE characters SET averageRating = :rating, totalRatings = :totalRatings WHERE id = :id")
    suspend fun updateCharacterRating(id: String, rating: Float, totalRatings: Int)
    
    @Query("UPDATE characters SET lastInteraction = :timestamp WHERE id = :id")
    suspend fun updateLastInteraction(id: String, timestamp: java.util.Date)
    
    // Bulk operations
    @Query("UPDATE characters SET isPublic = :isPublic WHERE id IN (:ids)")
    suspend fun updateCharactersPublicStatus(ids: List<String>, isPublic: Boolean)
    
    @Query("UPDATE characters SET isFeatured = :isFeatured WHERE id IN (:ids)")
    suspend fun updateCharactersFeaturedStatus(ids: List<String>, isFeatured: Boolean)
    
    // Advanced filtering
    @Query("""
        SELECT * FROM characters 
        WHERE isActive = 1 
        AND (:isPublic IS NULL OR isPublic = :isPublic)
        AND (:isFeatured IS NULL OR isFeatured = :isFeatured)
        AND (:creatorId IS NULL OR creatorId = :creatorId)
        AND (:minRating IS NULL OR averageRating >= :minRating)
        AND (:category IS NULL OR categories LIKE '%' || :category || '%')
        ORDER BY 
            CASE WHEN :sortBy = 'name' THEN name END ASC,
            CASE WHEN :sortBy = 'rating' THEN averageRating END DESC,
            CASE WHEN :sortBy = 'messages' THEN totalMessages END DESC,
            CASE WHEN :sortBy = 'conversations' THEN totalConversations END DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getCharactersWithFilters(
        isPublic: Boolean? = null,
        isFeatured: Boolean? = null,
        creatorId: String? = null,
        minRating: Float? = null,
        category: String? = null,
        sortBy: String = "name",
        limit: Int = 20,
        offset: Int = 0
    ): List<Character>
    
    // Character existence checks
    @Query("SELECT EXISTS(SELECT 1 FROM characters WHERE name = :name AND isActive = 1)")
    suspend fun characterExistsByName(name: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM characters WHERE id = :id AND isActive = 1)")
    suspend fun characterExistsById(id: String): Boolean
    
    // Character card import support
    @Query("SELECT * FROM characters WHERE name = :name")
    suspend fun getCharacterByNameIncludingInactive(name: String): Character?
    
    @Transaction
    suspend fun upsertCharacter(character: Character) {
        val existing = getCharacterById(character.id)
        if (existing != null) {
            updateCharacter(character)
        } else {
            insertCharacter(character)
        }
    }
    
    @Transaction
    suspend fun replaceInactiveCharacter(character: Character) {
        // If an inactive character with the same name exists, delete it first
        val existing = getCharacterByNameIncludingInactive(character.name)
        if (existing != null && !existing.isActive) {
            deleteCharacterById(existing.id)
        }
        insertCharacter(character)
    }
} 
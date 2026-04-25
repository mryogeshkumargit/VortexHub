package com.vortexai.android.data.database.dao

import androidx.room.*
import androidx.paging.PagingSource
import com.vortexai.android.data.models.Conversation
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ConversationDao {
    
    // Basic CRUD Operations
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): Conversation?
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationByIdFlow(id: String): Flow<Conversation?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<Conversation>)
    
    @Update
    suspend fun updateConversation(conversation: Conversation)
    
    @Delete
    suspend fun deleteConversation(conversation: Conversation)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
    
    // Character-based queries
    @Query("SELECT * FROM conversations WHERE characterId = :characterId ORDER BY lastMessageAt DESC")
    fun getConversationsByCharacter(characterId: String): Flow<List<Conversation>>
    
    @Query("SELECT * FROM conversations WHERE characterId = :characterId ORDER BY lastMessageAt DESC")
    suspend fun getConversationsByCharacterSync(characterId: String): List<Conversation>
    
    @Query("SELECT * FROM conversations WHERE characterId = :characterId AND isActive = 1 ORDER BY lastMessageAt DESC")
    suspend fun getActiveConversationsByCharacter(characterId: String): List<Conversation>
    
    // User-based queries
    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY lastMessageAt DESC")
    fun getConversationsByUser(userId: String): Flow<List<Conversation>>
    
    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY lastMessageAt DESC")
    suspend fun getConversationsByUserSync(userId: String): List<Conversation>
    
    // Active conversations
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY lastMessageAt DESC")
    fun getActiveConversations(): Flow<List<Conversation>>
    
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY lastMessageAt DESC")
    suspend fun getActiveConversationsSync(): List<Conversation>
    
    // Recent conversations
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC LIMIT :limit")
    suspend fun getRecentConversations(limit: Int = 20): List<Conversation>
    
    // Statistics
    @Query("SELECT COUNT(*) FROM conversations WHERE characterId = :characterId")
    suspend fun getConversationCountByCharacter(characterId: String): Int
    
    @Query("SELECT COUNT(*) FROM conversations WHERE userId = :userId")
    suspend fun getConversationCountByUser(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM conversations WHERE isActive = 1")
    suspend fun getActiveConversationCount(): Int
    
    // Search functionality
    @Query("SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' ORDER BY lastMessageAt DESC LIMIT :limit")
    suspend fun searchConversations(query: String, limit: Int = 50): List<Conversation>
    
    // Cleanup operations
    @Query("DELETE FROM conversations WHERE characterId = :characterId")
    suspend fun deleteConversationsByCharacter(characterId: String)
    
    @Query("DELETE FROM conversations WHERE userId = :userId")
    suspend fun deleteConversationsByUser(userId: String)
    
    @Query("DELETE FROM conversations WHERE isActive = 0")
    suspend fun deleteInactiveConversations()
    
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getTotalConversationCount(): Int
    
    // Additional methods needed by repository
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversations(): List<Conversation>
    
    @Query("UPDATE conversations SET lastMessageAt = :timestamp WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, timestamp: Long)
    
    @Query("UPDATE conversations SET characterMessages = characterMessages + :characterMessages WHERE id = :conversationId")
    suspend fun updateConversationStats(conversationId: String, characterMessages: Int)

    // Increment counters when a user sends a message and update timestamps
    @Query(
        "UPDATE conversations SET userMessages = userMessages + 1, totalMessages = totalMessages + 1, lastMessageAt = :timestamp, updatedAt = :timestamp WHERE id = :conversationId"
    )
    suspend fun incrementUserMessageStats(conversationId: String, timestamp: Long)

    // Increment counters when a character sends a message and update timestamps
    @Query(
        "UPDATE conversations SET characterMessages = characterMessages + 1, totalMessages = totalMessages + 1, lastMessageAt = :timestamp, updatedAt = :timestamp WHERE id = :conversationId"
    )
    suspend fun incrementCharacterMessageStats(conversationId: String, timestamp: Long)
    
    // Reset conversation stats when clearing messages
    @Query(
        "UPDATE conversations SET userMessages = 0, characterMessages = 0, totalMessages = 0, updatedAt = :timestamp WHERE id = :conversationId"
    )
    suspend fun resetConversationStats(conversationId: String, timestamp: Long = System.currentTimeMillis())
}

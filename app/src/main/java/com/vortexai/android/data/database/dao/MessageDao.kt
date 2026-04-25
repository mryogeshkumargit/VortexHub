package com.vortexai.android.data.database.dao

import androidx.room.*
import androidx.paging.PagingSource
import com.vortexai.android.data.models.Message
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MessageDao {
    
    // Basic CRUD Operations
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): Message?
    
    @Query("SELECT * FROM messages WHERE id = :id")
    fun getMessageByIdFlow(id: String): Flow<Message?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)
    
    @Update
    suspend fun updateMessage(message: Message)
    
    @Delete
    suspend fun deleteMessage(message: Message)
    
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
    
    // Conversation Messages
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesByConversationSync(conversationId: String): List<Message>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversationPaging(conversationId: String): PagingSource<Int, Message>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessagesByConversation(conversationId: String, limit: Int): List<Message>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastMessageInConversation(conversationId: String): Message?
    
    // Statistics and Analytics
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCountByConversation(conversationId: String): Int
    
    @Query("SELECT COUNT(*) FROM messages WHERE characterId = :characterId")
    suspend fun getMessageCountByCharacter(characterId: String): Int
    
    // Search functionality
    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchMessagesGlobally(query: String, limit: Int = 100): List<Message>
    
    // Message editing
    @Query("UPDATE messages SET content = :content, isEdited = 1 WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String)
    
    // Cleanup operations
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)
    
    @Query("DELETE FROM messages WHERE characterId = :characterId")
    suspend fun deleteMessagesByCharacter(characterId: String)
    
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessageCount(): Int
    
    // Additional methods needed by repository
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesByConversationId(conversationId: String): List<Message>
    
    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    suspend fun getAllMessages(): List<Message>
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversationId(conversationId: String)
}

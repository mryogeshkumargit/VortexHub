package com.vortexai.android.data.database.dao

import androidx.room.*
import com.vortexai.android.data.model.ChatImageSettings
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chat-specific image settings
 */
@Dao
interface ChatImageSettingsDao {
    
    @Query("SELECT * FROM chat_image_settings WHERE chatId = :chatId")
    suspend fun getChatImageSettings(chatId: String): ChatImageSettings?
    
    @Query("SELECT * FROM chat_image_settings WHERE chatId = :chatId")
    fun getChatImageSettingsFlow(chatId: String): Flow<ChatImageSettings?>
    
    @Query("SELECT * FROM chat_image_settings")
    suspend fun getAllChatImageSettings(): List<ChatImageSettings>
    
    @Query("SELECT * FROM chat_image_settings")
    fun getAllChatImageSettingsFlow(): Flow<List<ChatImageSettings>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatImageSettings(settings: ChatImageSettings)
    
    @Update
    suspend fun updateChatImageSettings(settings: ChatImageSettings)
    
    @Delete
    suspend fun deleteChatImageSettings(settings: ChatImageSettings)
    
    @Query("DELETE FROM chat_image_settings WHERE chatId = :chatId")
    suspend fun deleteChatImageSettingsByChatId(chatId: String)
    
    @Query("DELETE FROM chat_image_settings")
    suspend fun deleteAllChatImageSettings()
    
    @Query("SELECT COUNT(*) FROM chat_image_settings")
    suspend fun getChatImageSettingsCount(): Int
}

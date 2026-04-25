package com.vortexai.android.data.repository

import com.vortexai.android.data.database.dao.ChatImageSettingsDao
import com.vortexai.android.data.model.ChatImageSettings
import com.vortexai.android.data.model.InputImageOption
import com.vortexai.android.data.model.PredictionCreationMethod
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing chat-specific image settings
 */
@Singleton
class ChatImageSettingsRepository @Inject constructor(
    private val chatImageSettingsDao: ChatImageSettingsDao
) {
    
    /**
     * Get chat image settings for a specific chat
     */
    suspend fun getChatImageSettings(chatId: String): ChatImageSettings? {
        return chatImageSettingsDao.getChatImageSettings(chatId)
    }
    
    /**
     * Get chat image settings as Flow for reactive updates
     */
    fun getChatImageSettingsFlow(chatId: String): Flow<ChatImageSettings?> {
        return chatImageSettingsDao.getChatImageSettingsFlow(chatId)
    }
    
    /**
     * Get or create default chat image settings for a chat
     */
    suspend fun getOrCreateChatImageSettings(chatId: String): ChatImageSettings {
        val existing = chatImageSettingsDao.getChatImageSettings(chatId)
        return if (existing != null) {
            existing
        } else {
            val defaultSettings = ChatImageSettings(
                chatId = chatId,
                inputImageOption = InputImageOption.CHARACTER_AVATAR,
                predictionCreationMethod = PredictionCreationMethod.AUTO
            )
            chatImageSettingsDao.insertChatImageSettings(defaultSettings)
            defaultSettings
        }
    }
    
    /**
     * Update input image option for a chat
     */
    suspend fun updateInputImageOption(
        chatId: String, 
        inputImageOption: InputImageOption,
        localImagePath: String? = null,
        cloudImageUrl: String? = null
    ) {
        val settings = getOrCreateChatImageSettings(chatId)
        val updatedSettings = settings.copy(
            inputImageOption = inputImageOption,
            localImagePath = localImagePath,
            cloudImageUrl = cloudImageUrl,
            updatedAt = System.currentTimeMillis()
        )
        chatImageSettingsDao.updateChatImageSettings(updatedSettings)
    }
    
    /**
     * Update prediction creation method for a chat
     */
    suspend fun updatePredictionCreationMethod(
        chatId: String,
        method: PredictionCreationMethod,
        manualInput: String? = null
    ) {
        val settings = getOrCreateChatImageSettings(chatId)
        val updatedSettings = settings.copy(
            predictionCreationMethod = method,
            manualPredictionInput = manualInput,
            updatedAt = System.currentTimeMillis()
        )
        chatImageSettingsDao.updateChatImageSettings(updatedSettings)
    }
    
    /**
     * Update manual prediction input for a chat
     */
    suspend fun updateManualPredictionInput(chatId: String, manualInput: String) {
        val settings = getOrCreateChatImageSettings(chatId)
        val updatedSettings = settings.copy(
            manualPredictionInput = manualInput,
            updatedAt = System.currentTimeMillis()
        )
        chatImageSettingsDao.updateChatImageSettings(updatedSettings)
    }
    
    /**
     * Save or update chat image settings
     */
    suspend fun saveChatImageSettings(settings: ChatImageSettings) {
        chatImageSettingsDao.insertChatImageSettings(settings)
    }
    
    /**
     * Delete chat image settings for a specific chat
     */
    suspend fun deleteChatImageSettings(chatId: String) {
        chatImageSettingsDao.deleteChatImageSettingsByChatId(chatId)
    }
    
    /**
     * Get all chat image settings
     */
    suspend fun getAllChatImageSettings(): List<ChatImageSettings> {
        return chatImageSettingsDao.getAllChatImageSettings()
    }
    
    /**
     * Get all chat image settings as Flow
     */
    fun getAllChatImageSettingsFlow(): Flow<List<ChatImageSettings>> {
        return chatImageSettingsDao.getAllChatImageSettingsFlow()
    }
}

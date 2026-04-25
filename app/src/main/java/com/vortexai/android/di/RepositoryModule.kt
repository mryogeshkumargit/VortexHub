package com.vortexai.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vortexai.android.data.local.CharacterLocalDataSource
import com.vortexai.android.data.repository.AuthRepository
import com.vortexai.android.data.repository.CharacterRepository
import com.vortexai.android.data.repository.ChatRepository
import com.vortexai.android.data.database.dao.CharacterDao
import com.vortexai.android.data.database.dao.ConversationDao
import com.vortexai.android.data.database.dao.MessageDao
import com.vortexai.android.utils.ImageStorageHelper
import com.vortexai.android.utils.MacroProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * Provides CharacterLocalDataSource
     */
    @Provides
    @Singleton
    fun provideCharacterLocalDataSource(
        @ApplicationContext context: Context
    ): CharacterLocalDataSource {
        return CharacterLocalDataSource(context)
    }
    
    /**
     * Provides AuthRepository
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        dataStore: DataStore<Preferences>,
        accountDao: com.vortexai.android.data.database.dao.AccountDao,
        @ApplicationContext context: Context
    ): AuthRepository {
        // For now, provide a simplified version without API service
        // TODO: Add AuthApiService when ready
        return AuthRepository(null, dataStore, accountDao, context)
    }

    /**
     * Provides CharacterRepository
     */
    @Provides
    @Singleton
    fun provideCharacterRepository(
        characterLocalDataSource: CharacterLocalDataSource,
        characterDao: CharacterDao,
        dataStore: DataStore<Preferences>
    ): CharacterRepository {
        return CharacterRepository(characterLocalDataSource, characterDao, dataStore)
    }

    /**
     * Provides ChatRepository
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        characterDao: CharacterDao,
        chatLLMService: com.vortexai.android.domain.service.ChatLLMService,
        authRepository: AuthRepository,
        dataStore: DataStore<Preferences>
    ): ChatRepository {
        return ChatRepository(conversationDao, messageDao, characterDao, chatLLMService, authRepository, dataStore)
    }
    
    /**
     * Provides ImageStorageHelper
     */
    @Provides
    @Singleton
    fun provideImageStorageHelper(
        @ApplicationContext context: Context
    ): ImageStorageHelper {
        return ImageStorageHelper(context)
    }
    
    /**
     * Provides MacroProcessor
     */
    @Provides
    @Singleton
    fun provideMacroProcessor(): MacroProcessor {
        return MacroProcessor()
    }
    
    /**
     * Provides DynamicStatsManager
     */
    @Provides
    @Singleton
    fun provideDynamicStatsManager(): com.vortexai.android.utils.DynamicStatsManager {
        return com.vortexai.android.utils.DynamicStatsManager()
    }
    
    /**
     * Provides LorebookNotificationService
     */
    @Provides
    @Singleton
    fun provideLorebookNotificationService(): com.vortexai.android.utils.LorebookNotificationService {
        return com.vortexai.android.utils.LorebookNotificationService()
    }
    
    /**
     * Provides SupabaseBackupService
     */
    @Provides
    @Singleton
    fun provideSupabaseBackupService(
        @ApplicationContext context: Context,
        database: com.vortexai.android.data.database.VortexDatabase
    ): com.vortexai.android.data.remote.SupabaseBackupService {
        return com.vortexai.android.data.remote.SupabaseBackupService(context, database)
    }
    
    /**
     * Provides VortexImageGenerator
     */
    @Provides
    @Singleton
    fun provideVortexImageGenerator(
        imageEditingService: com.vortexai.android.domain.service.ImageEditingService,
        dataStore: DataStore<Preferences>,
        imageStorageHelper: com.vortexai.android.utils.ImageStorageHelper,
        chatRepository: ChatRepository
    ): com.vortexai.android.ui.screens.chat.VortexImageGenerator {
        return com.vortexai.android.ui.screens.chat.VortexImageGenerator(imageEditingService, dataStore, imageStorageHelper, chatRepository)
    }
} 
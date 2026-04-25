package com.vortexai.android.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vortexai.android.core.database.VortexDatabase
import com.vortexai.android.core.database.dao.CharacterDao
import com.vortexai.android.core.database.dao.ConversationDao
import com.vortexai.android.core.database.dao.MessageDao
import com.vortexai.android.core.database.dao.CharacterBookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 * Provides singleton instances of database and DAOs
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVortexDatabase(
        @ApplicationContext context: Context
    ): VortexDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            VortexDatabase::class.java,
            VortexDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .enableMultiInstanceInvalidation()
            .setJournalMode(RoomDatabase.JournalMode.WAL) // Better performance
            .build()
    }

    @Provides
    fun provideCharacterDao(database: VortexDatabase): CharacterDao {
        return database.characterDao()
    }

    @Provides
    fun provideConversationDao(database: VortexDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: VortexDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideCharacterBookDao(database: VortexDatabase): CharacterBookDao {
        return database.characterBookDao()
    }
} 
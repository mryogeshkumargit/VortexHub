package com.vortexai.android.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.vortexai.android.core.database.dao.CharacterDao
import com.vortexai.android.core.database.dao.ConversationDao
import com.vortexai.android.core.database.dao.MessageDao
import com.vortexai.android.core.database.dao.CharacterBookDao
import com.vortexai.android.core.database.entity.CharacterEntity
import com.vortexai.android.core.database.entity.ConversationEntity
import com.vortexai.android.core.database.entity.MessageEntity
import com.vortexai.android.core.database.entity.CharacterBookEntity
import com.vortexai.android.core.database.entity.CharacterBookEntryEntity
import com.vortexai.android.core.database.entity.CharacterStatsEntity
import com.vortexai.android.core.database.entity.ConversationStatsEntity
import com.vortexai.android.core.database.converter.Converters

/**
 * Main Room database for VortexAI Companion App
 * 
 * This database stores all local data including characters, conversations,
 * messages, and related metadata for offline functionality.
 */
@Database(
    entities = [
        CharacterEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        CharacterBookEntity::class,
        CharacterBookEntryEntity::class,
        CharacterStatsEntity::class,
        ConversationStatsEntity::class
    ],
    version = 1,
    exportSchema = true,
    autoMigrations = []
)
@TypeConverters(Converters::class)
abstract class VortexDatabase : RoomDatabase() {
    
    abstract fun characterDao(): CharacterDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun characterBookDao(): CharacterBookDao
    
    companion object {
        const val DATABASE_NAME = "vortex_database"
        
        /**
         * Create database instance with proper configuration
         */
        fun create(context: Context): VortexDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VortexDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration() // For development, remove in production
                .enableMultiInstanceInvalidation()
                .setJournalMode(JournalMode.WAL) // Better performance for concurrent access
                .build()
        }
    }
} 
package com.vortexai.android.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vortexai.android.data.database.VortexDatabase
import com.vortexai.android.data.database.DatabaseInitializer
import com.vortexai.android.data.database.dao.*
import com.vortexai.android.data.database.dao.GeneratedImageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideVortexDatabase(
        @ApplicationContext context: Context
    ): VortexDatabase {
        return Room.databaseBuilder(
            context,
            VortexDatabase::class.java,
            VortexDatabase.DATABASE_NAME
        )
            .addMigrations(VortexDatabase.MIGRATION_1_2, VortexDatabase.MIGRATION_2_3, VortexDatabase.MIGRATION_3_4, VortexDatabase.MIGRATION_4_5, VortexDatabase.MIGRATION_5_6, VortexDatabase.MIGRATION_6_7, VortexDatabase.MIGRATION_7_8, VortexDatabase.MIGRATION_8_9, VortexDatabase.MIGRATION_9_10, VortexDatabase.MIGRATION_10_11, VortexDatabase.MIGRATION_11_12)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    try {
                        // Enable foreign key constraints
                        db.execSQL("PRAGMA foreign_keys = ON")
                        // Optimize database performance
                        db.execSQL("PRAGMA journal_mode = WAL")
                        db.execSQL("PRAGMA synchronous = NORMAL")
                        db.execSQL("PRAGMA cache_size = 10000")
                        db.execSQL("PRAGMA temp_store = MEMORY")
                        android.util.Log.d("DatabaseModule", "Database created and configured successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseModule", "Error configuring database", e)
                    }
                }
                
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    try {
                        // Verify database integrity
                        val cursor = db.query("PRAGMA integrity_check")
                        if (cursor.moveToFirst()) {
                            val result = cursor.getString(0)
                            android.util.Log.d("DatabaseModule", "Database integrity check: $result")
                        }
                        cursor.close()
                        
                        // Enable foreign key constraints
                        db.execSQL("PRAGMA foreign_keys = ON")
                        android.util.Log.d("DatabaseModule", "Database opened and verified successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseModule", "Error opening database", e)
                    }
                }
            })
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
    fun provideUserDao(database: VortexDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideUserSessionDao(database: VortexDatabase): UserSessionDao {
        return database.userSessionDao()
    }
    
    @Provides
    fun provideGeneratedImageDao(database: VortexDatabase): GeneratedImageDao {
        return database.generatedImageDao()
    }
    
    @Provides
    fun provideChatImageSettingsDao(database: VortexDatabase): com.vortexai.android.data.database.dao.ChatImageSettingsDao {
        return database.chatImageSettingsDao()
    }
    
    @Provides
    fun provideAccountDao(database: VortexDatabase): AccountDao {
        return database.accountDao()
    }
    
    @Provides
    fun provideCustomApiProviderDao(database: VortexDatabase): com.vortexai.android.data.database.dao.CustomApiProviderDao {
        return database.customApiProviderDao()
    }
    
    @Provides
    @Singleton
    fun provideDatabaseInitializer(
        database: VortexDatabase,
        characterDao: CharacterDao,
        @ApplicationContext context: Context
    ): DatabaseInitializer {
        return DatabaseInitializer(database, characterDao, context)
    }
}

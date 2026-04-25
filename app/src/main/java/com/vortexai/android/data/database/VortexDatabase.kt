package com.vortexai.android.data.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Conversation
import com.vortexai.android.data.models.Message
import com.vortexai.android.data.models.User
import com.vortexai.android.data.models.UserSession
import com.vortexai.android.data.model.ChatImageSettings
import com.vortexai.android.data.database.dao.*

@Database(
    entities = [
        Character::class,
        Conversation::class,
        Message::class,
        User::class,
        UserSession::class,
        com.vortexai.android.data.models.GeneratedImage::class,
        ChatImageSettings::class,
        com.vortexai.android.data.models.Account::class,
        com.vortexai.android.data.models.CustomApiProvider::class,
        com.vortexai.android.data.models.CustomApiEndpoint::class,
        com.vortexai.android.data.models.CustomApiModel::class,
        com.vortexai.android.data.models.CustomApiParameter::class,
        com.vortexai.android.data.models.CustomApiParameterValue::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VortexDatabase : RoomDatabase() {
    
    // DAO interfaces
    abstract fun characterDao(): CharacterDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun generatedImageDao(): GeneratedImageDao
    abstract fun chatImageSettingsDao(): ChatImageSettingsDao
    abstract fun accountDao(): AccountDao
    abstract fun customApiProviderDao(): CustomApiProviderDao
    
    /**
     * Clear all tables for backup restore
     */
    suspend fun clearAllTablesForBackup() {
        characterDao().deleteAllCharacters()
        conversationDao().deleteAllConversations()
        messageDao().deleteAllMessages()
        userDao().deleteAllUsers()
        userSessionDao().deleteAllSessions()
        generatedImageDao().deleteAllGeneratedImages()
        chatImageSettingsDao().deleteAllChatImageSettings()
    }
    
    companion object {
        const val DATABASE_NAME = "vortex_database"
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS generated_images (id TEXT NOT NULL, prompt TEXT NOT NULL, localPath TEXT NOT NULL, model TEXT, generationTime INTEGER NOT NULL, size TEXT NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(id))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN messageType TEXT NOT NULL DEFAULT 'text'");
                database.execSQL("ALTER TABLE messages ADD COLUMN metadataJson TEXT");
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN dynamicStatsEnabled INTEGER NOT NULL DEFAULT 0");
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS chat_image_settings (" +
                    "chatId TEXT NOT NULL, " +
                    "inputImageOption TEXT NOT NULL DEFAULT 'CHARACTER_AVATAR', " +
                    "localImagePath TEXT, " +
                    "cloudImageUrl TEXT, " +
                    "useCharacterAvatar INTEGER NOT NULL DEFAULT 1, " +
                    "predictionCreationMethod TEXT NOT NULL DEFAULT 'AUTO', " +
                    "manualPredictionInput TEXT, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(chatId))"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS accounts (" +
                    "id TEXT NOT NULL, " +
                    "username TEXT NOT NULL, " +
                    "email TEXT, " +
                    "fullName TEXT, " +
                    "dateOfBirth TEXT, " +
                    "avatarUrl TEXT, " +
                    "isPremium INTEGER NOT NULL DEFAULT 0, " +
                    "accessToken TEXT, " +
                    "refreshToken TEXT, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(id))")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Legacy migration - kept for compatibility
            }
        }
        
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop old tables if they exist
                database.execSQL("DROP TABLE IF EXISTS custom_api_configs")
                database.execSQL("DROP TABLE IF EXISTS custom_api_models")
                
                // Create new custom API tables
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_api_providers (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "baseUrl TEXT NOT NULL, " +
                    "apiKey TEXT NOT NULL, " +
                    "isEnabled INTEGER NOT NULL DEFAULT 1, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL)")
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_api_providers_type " +
                    "ON custom_api_providers(type)")
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_api_endpoints (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "providerId TEXT NOT NULL, " +
                    "endpointPath TEXT NOT NULL, " +
                    "httpMethod TEXT NOT NULL, " +
                    "requestSchemaJson TEXT NOT NULL, " +
                    "responseSchemaJson TEXT NOT NULL, " +
                    "purpose TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "FOREIGN KEY(providerId) REFERENCES custom_api_providers(id) ON DELETE CASCADE)")
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_api_endpoints_providerId " +
                    "ON custom_api_endpoints(providerId)")
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_api_models (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "providerId TEXT NOT NULL, " +
                    "modelId TEXT NOT NULL, " +
                    "displayName TEXT NOT NULL, " +
                    "capabilitiesJson TEXT NOT NULL, " +
                    "isActive INTEGER NOT NULL DEFAULT 1, " +
                    "createdAt INTEGER NOT NULL, " +
                    "FOREIGN KEY(providerId) REFERENCES custom_api_providers(id) ON DELETE CASCADE)")
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_api_models_providerId " +
                    "ON custom_api_models(providerId)")
                
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_api_parameters (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "modelId TEXT NOT NULL, " +
                    "paramName TEXT NOT NULL, " +
                    "paramType TEXT NOT NULL, " +
                    "defaultValue TEXT, " +
                    "minValue TEXT, " +
                    "maxValue TEXT, " +
                    "isRequired INTEGER NOT NULL DEFAULT 0, " +
                    "description TEXT, " +
                    "FOREIGN KEY(modelId) REFERENCES custom_api_models(id) ON DELETE CASCADE)")
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_api_parameters_modelId " +
                    "ON custom_api_parameters(modelId)")
            }
        }
        
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_api_parameter_values (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "modelId TEXT NOT NULL, " +
                    "paramName TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "FOREIGN KEY(modelId) REFERENCES custom_api_models(id) ON DELETE CASCADE)")
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_api_parameter_values_modelId " +
                    "ON custom_api_parameter_values(modelId)")
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_api_parameter_values_paramName " +
                    "ON custom_api_parameter_values(paramName)")
            }
        }
        
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recreate custom_api_parameter_values with composite primary key
                database.execSQL("DROP TABLE IF EXISTS custom_api_parameter_values")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_api_parameter_values (" +
                    "modelId TEXT NOT NULL, " +
                    "paramName TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(modelId, paramName), " +
                    "FOREIGN KEY(modelId) REFERENCES custom_api_models(id) ON DELETE CASCADE)")
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_api_parameter_values_modelId " +
                    "ON custom_api_parameter_values(modelId)")
            }
        }
        
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN avatar_video_url TEXT")
            }
        }
    }
} 
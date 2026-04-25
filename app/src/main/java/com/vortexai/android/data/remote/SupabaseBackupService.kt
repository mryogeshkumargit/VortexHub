package com.vortexai.android.data.remote

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.vortexai.android.data.database.VortexDatabase
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Conversation
import com.vortexai.android.data.models.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseBackupService @Inject constructor(
    private val context: Context,
    private val database: VortexDatabase
) {
    
    companion object {
        private const val TAG = "SupabaseBackupService"
        private const val BACKUP_WORK_NAME = "supabase_auto_backup"
        private const val BACKUP_INTERVAL_MINUTES = 30L
    }
    
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Create a backup and upload to Supabase database
     */
    suspend fun createCloudBackup(supabaseUrl: String, anonKey: String): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting cloud backup to Supabase database")
                
                // Create backup data
                val backupData = createBackupData()
                val backupJson = gson.toJson(backupData)
                val backupId = "vortex_backup_${System.currentTimeMillis()}"
                
                Log.d(TAG, "Backup data size: ${backupJson.length} characters")
                
                // Upload to Supabase database table
                val uploadResult = uploadToSupabaseDatabase(supabaseUrl, anonKey, backupId, backupJson)
                
                if (uploadResult.isSuccess) {
                    Log.d(TAG, "Cloud backup successful: $backupId")
                    BackupResult.Success(backupId, backupData.summary)
                } else {
                    Log.e(TAG, "Cloud backup failed: ${uploadResult.exceptionOrNull()}")
                    BackupResult.Failure(uploadResult.exceptionOrNull()?.message ?: "Unknown error")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Cloud backup failed", e)
                BackupResult.Failure("Backup failed: ${e.message}")
            }
        }
    }
    
    /**
     * Restore backup from Supabase database
     */
    suspend fun restoreFromCloud(supabaseUrl: String, anonKey: String, backupId: String): RestoreResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting cloud restore: $backupId")
                
                // Download from Supabase database
                val downloadResult = downloadFromSupabaseDatabase(supabaseUrl, anonKey, backupId)
                
                if (downloadResult.isSuccess) {
                    val backupJson = downloadResult.getOrNull()!!
                    val backupData = gson.fromJson(backupJson, BackupData::class.java)
                    
                    // Restore data to database
                    restoreBackupData(backupData)
                    
                    Log.d(TAG, "Cloud restore successful: $backupId")
                    RestoreResult.Success(backupData.summary)
                } else {
                    Log.e(TAG, "Cloud restore failed: ${downloadResult.exceptionOrNull()}")
                    RestoreResult.Failure(downloadResult.exceptionOrNull()?.message ?: "Unknown error")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Cloud restore failed", e)
                RestoreResult.Failure("Restore failed: ${e.message}")
            }
        }
    }
    
    /**
     * List available backups from Supabase database
     */
    suspend fun listCloudBackups(supabaseUrl: String, anonKey: String): ListBackupsResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Listing cloud backups from database")
                
                // Query the backups table in Supabase database
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/backups?select=*&order=created_at.desc")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "[]"
                    Log.d(TAG, "Raw response: $responseBody")
                    
                    val backupFiles = gson.fromJson(responseBody, Array<DatabaseBackupFile>::class.java)
                        .map { it.toBackupFile() }
                        .sortedByDescending { it.updated_at }
                    
                    Log.d(TAG, "Found ${backupFiles.size} backup files")
                    ListBackupsResult.Success(backupFiles)
                } else {
                    val errorBody = response.body?.string() ?: "No error details"
                    Log.e(TAG, "Failed to list backups: ${response.code}, Error: $errorBody")
                    ListBackupsResult.Failure("Failed to list backups: ${response.code}. $errorBody")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "List backups failed", e)
                ListBackupsResult.Failure("List backups failed: ${e.message}")
            }
        }
    }
    
    /**
     * Schedule auto-backup every 30 minutes
     */
    fun scheduleAutoBackup(supabaseUrl: String, anonKey: String) {
        Log.d(TAG, "Scheduling auto-backup every $BACKUP_INTERVAL_MINUTES minutes")
        
        val backupWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            BACKUP_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
        .setInputData(workDataOf(
            "supabase_url" to supabaseUrl,
            "anon_key" to anonKey
        ))
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            backupWorkRequest
        )
    }
    
    /**
     * Cancel auto-backup
     */
    fun cancelAutoBackup() {
        Log.d(TAG, "Cancelling auto-backup")
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
    }
    
    /**
     * Create backup data from database
     */
    private suspend fun createBackupData(): BackupData {
        val characters = database.characterDao().getAllCharacters()
        val conversations = database.conversationDao().getAllConversations()
        val messages = database.messageDao().getAllMessages()
        
        val summary = BackupSummary(
            timestamp = System.currentTimeMillis(),
            charactersCount = characters.size,
            conversationsCount = conversations.size,
            messagesCount = messages.size
        )
        
        return BackupData(
            version = "1.0",
            timestamp = System.currentTimeMillis(),
            summary = summary,
            characters = characters,
            conversations = conversations,
            messages = messages
        )
    }
    
    /**
     * Restore backup data to database
     */
    private suspend fun restoreBackupData(backupData: BackupData) {
        // Clear existing data
        database.clearAllTablesForBackup()
        
        // Restore data
        database.characterDao().insertCharacters(backupData.characters)
        database.conversationDao().insertConversations(backupData.conversations)
        database.messageDao().insertMessages(backupData.messages)
        
        Log.d(TAG, "Restored ${backupData.characters.size} characters, ${backupData.conversations.size} conversations, ${backupData.messages.size} messages")
    }
    
    /**
     * Upload backup to Supabase database table
     */
    private suspend fun uploadToSupabaseDatabase(url: String, anonKey: String, backupId: String, content: String): Result<String> {
        return try {
            val requestBody = """
                {
                    "id": "$backupId",
                    "data": ${gson.toJson(content)},
                    "created_at": "${java.time.Instant.now()}",
                    "updated_at": "${java.time.Instant.now()}"
                }
            """.trimIndent().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$url/rest/v1/backups")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Result.success(backupId)
            } else {
                val errorBody = response.body?.string() ?: "No error details"
                Log.e(TAG, "Upload failed: ${response.code}, Error: $errorBody")
                Result.failure(Exception("Upload failed: ${response.code}. $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download backup from Supabase database table
     */
    private suspend fun downloadFromSupabaseDatabase(url: String, anonKey: String, backupId: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url("$url/rest/v1/backups?id=eq.$backupId&select=data")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "[]"
                val backupArray = gson.fromJson(responseBody, Array<DatabaseBackupResponse>::class.java)
                
                if (backupArray.isNotEmpty()) {
                    val backupData = backupArray[0].data
                    Result.success(backupData)
                } else {
                    Result.failure(Exception("Backup not found"))
                }
            } else {
                Result.failure(Exception("Download failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data classes for backup
data class BackupData(
    val version: String,
    val timestamp: Long,
    val summary: BackupSummary,
    val characters: List<Character>,
    val conversations: List<Conversation>,
    val messages: List<Message>
)

data class BackupSummary(
    val timestamp: Long,
    val charactersCount: Int,
    val conversationsCount: Int,
    val messagesCount: Int
)

data class BackupFile(
    val name: String,
    val updated_at: String,
    val created_at: String,
    val last_accessed_at: String,
    val metadata: Map<String, Any>?
)

// Database-specific data classes
data class DatabaseBackupFile(
    val id: String,
    val data: String,
    val created_at: String,
    val updated_at: String
) {
    fun toBackupFile(): BackupFile {
        return BackupFile(
            name = id,
            updated_at = updated_at,
            created_at = created_at,
            last_accessed_at = updated_at,
            metadata = mapOf("size" to data.length)
        )
    }
}

data class DatabaseBackupResponse(
    val data: String
)

// Result classes
sealed class BackupResult {
    data class Success(val fileName: String, val summary: BackupSummary) : BackupResult()
    data class Failure(val error: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val summary: BackupSummary) : RestoreResult()
    data class Failure(val error: String) : RestoreResult()
}

sealed class ListBackupsResult {
    data class Success(val backups: List<BackupFile>) : ListBackupsResult()
    data class Failure(val error: String) : ListBackupsResult()
}

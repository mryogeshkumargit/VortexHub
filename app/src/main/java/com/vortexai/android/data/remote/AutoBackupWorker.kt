package com.vortexai.android.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "AutoBackupWorker"
    }
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoBackupWorkerEntryPoint {
        fun supabaseBackupService(): SupabaseBackupService
    }
    
    private val supabaseBackupService: SupabaseBackupService by lazy {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AutoBackupWorkerEntryPoint::class.java
        )
        entryPoint.supabaseBackupService()
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting auto-backup work")
            
            val supabaseUrl = inputData.getString("supabase_url")
            val anonKey = inputData.getString("anon_key")
            
            if (supabaseUrl.isNullOrBlank() || anonKey.isNullOrBlank()) {
                Log.w(TAG, "Missing Supabase credentials for auto-backup")
                return Result.failure()
            }
            
            Log.d(TAG, "Creating auto-backup to Supabase")
            val result = supabaseBackupService.createCloudBackup(supabaseUrl, anonKey)
            
            when (result) {
                is BackupResult.Success -> {
                    Log.d(TAG, "Auto-backup successful: ${result.fileName}")
                    Result.success()
                }
                is BackupResult.Failure -> {
                    Log.e(TAG, "Auto-backup failed: ${result.error}")
                    Result.retry()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Auto-backup work failed", e)
            Result.retry()
        }
    }
}

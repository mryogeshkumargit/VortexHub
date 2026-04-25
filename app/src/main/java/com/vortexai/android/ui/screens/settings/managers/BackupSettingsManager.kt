package com.vortexai.android.ui.screens.settings.managers

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.data.remote.SupabaseBackupService
import com.vortexai.android.data.remote.BackupResult
import com.vortexai.android.data.remote.ListBackupsResult
import com.vortexai.android.data.remote.RestoreResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSettingsManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val supabaseBackupService: SupabaseBackupService
) {
    suspend fun loadBackupSettings(currentState: SettingsUiState): SettingsUiState {
        val preferences = settingsDataStore.getPreferences()
        
        return currentState.copy(
            supabaseUrl = preferences[stringPreferencesKey("supabase_url")] ?: "",
            supabaseAnonKey = preferences[stringPreferencesKey("supabase_anon_key")] ?: "",
            supabaseEnabled = preferences[booleanPreferencesKey("supabase_enabled")] ?: false,
            autoBackupEnabled = preferences[booleanPreferencesKey("auto_backup_enabled")] ?: false,
            cloudSyncEnabled = preferences[booleanPreferencesKey("cloud_sync_enabled")] ?: false,
            lastBackupTime = preferences[longPreferencesKey("last_backup_time")] ?: 0L,
            analyticsEnabled = preferences[booleanPreferencesKey("analytics_enabled")] ?: false,
            crashReports = preferences[booleanPreferencesKey("crash_reports")] ?: true
        )
    }

    suspend fun saveBackupSettings(state: SettingsUiState) {
        settingsDataStore.savePreferences { preferences: androidx.datastore.preferences.core.MutablePreferences ->
            preferences[stringPreferencesKey("supabase_url")] = state.supabaseUrl
            preferences[stringPreferencesKey("supabase_anon_key")] = state.supabaseAnonKey
            preferences[booleanPreferencesKey("supabase_enabled")] = state.supabaseEnabled
            preferences[booleanPreferencesKey("auto_backup_enabled")] = state.autoBackupEnabled
            preferences[booleanPreferencesKey("cloud_sync_enabled")] = state.cloudSyncEnabled
            preferences[longPreferencesKey("last_backup_time")] = state.lastBackupTime
            preferences[booleanPreferencesKey("analytics_enabled")] = state.analyticsEnabled
            preferences[booleanPreferencesKey("crash_reports")] = state.crashReports
        }
    }

    suspend fun createCloudBackup(supabaseUrl: String, supabaseAnonKey: String): BackupResult {
        return supabaseBackupService.createCloudBackup(supabaseUrl, supabaseAnonKey)
    }

    suspend fun listCloudBackups(supabaseUrl: String, supabaseAnonKey: String): ListBackupsResult {
        return supabaseBackupService.listCloudBackups(supabaseUrl, supabaseAnonKey)
    }

    suspend fun restoreFromCloud(supabaseUrl: String, supabaseAnonKey: String, backupId: String): RestoreResult {
        return supabaseBackupService.restoreFromCloud(supabaseUrl, supabaseAnonKey, backupId)
    }

    fun scheduleAutoBackup(supabaseUrl: String, supabaseAnonKey: String) {
        supabaseBackupService.scheduleAutoBackup(supabaseUrl, supabaseAnonKey)
    }

    fun cancelAutoBackup() {
        supabaseBackupService.cancelAutoBackup()
    }
}
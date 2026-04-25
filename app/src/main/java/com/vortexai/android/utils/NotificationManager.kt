package com.vortexai.android.utils

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VortexNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val CHANNEL_ID = "vortex_notifications"
        private const val CHANNEL_NAME = "Vortex Notifications"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for Vortex AI app updates and messages"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun isAppInForeground(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        
        return runningProcesses.any { processInfo ->
            processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
            processInfo.processName == context.packageName
        }
    }
    
    suspend fun sendNewMessageNotification(characterName: String, messagePreview: String) {
        val prefs = dataStore.data.first()
        val pushEnabled = prefs[booleanPreferencesKey("push_notifications")] ?: false
        
        if (pushEnabled && !isAppInForeground()) {
            sendNotification(
                title = "New message from $characterName",
                message = messagePreview.take(100)
            )
        }
    }
    
    suspend fun sendImageGenerationNotification(prompt: String) {
        val prefs = dataStore.data.first()
        val pushEnabled = prefs[booleanPreferencesKey("push_notifications")] ?: false
        
        if (pushEnabled && !isAppInForeground()) {
            sendNotification(
                title = "Image Generation Complete",
                message = "Your image for '$prompt' is ready"
            )
        }
    }
    
    private fun sendNotification(title: String, message: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("VortexNotificationManager", "Failed to send notification", e)
        }
    }
}

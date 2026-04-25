package com.vortexai.android.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    
    suspend fun sendNewMessageNotification(characterName: String, messagePreview: String) {
        val prefs = dataStore.data.first()
        val emailEnabled = prefs[booleanPreferencesKey("email_notifications")] ?: false
        val userEmail = prefs[stringPreferencesKey("user_email")] ?: ""
        
        if (emailEnabled && userEmail.isNotBlank()) {
            sendEmail(
                to = userEmail,
                subject = "New message from $characterName",
                body = "$characterName sent you a message:\n\n$messagePreview\n\nOpen Vortex AI to continue the conversation."
            )
        }
    }
    
    suspend fun sendImageGenerationNotification(prompt: String) {
        val prefs = dataStore.data.first()
        val emailEnabled = prefs[booleanPreferencesKey("email_notifications")] ?: false
        val userEmail = prefs[stringPreferencesKey("user_email")] ?: ""
        
        if (emailEnabled && userEmail.isNotBlank()) {
            sendEmail(
                to = userEmail,
                subject = "Image Generation Complete",
                body = "Your image has been generated successfully!\n\nPrompt: $prompt\n\nOpen Vortex AI to view your image."
            )
        }
    }
    
    private fun sendEmail(to: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(intent, "Send Email").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        } catch (e: Exception) {
            android.util.Log.e("EmailNotificationService", "Failed to send email", e)
        }
    }
}

package com.vortexai.android.utils

import android.util.Log
import com.vortexai.android.data.models.Character
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for detecting triggered lorebook entries and managing notifications
 */
@Singleton
class LorebookNotificationService @Inject constructor() {
    
    companion object {
        private const val TAG = "LorebookNotificationService"
    }
    
    /**
     * Result of lorebook processing
     */
    data class LorebookTriggerResult(
        val triggeredEntries: List<LorebookEntryInfo> = emptyList(),
        val triggeredContent: String = ""
    )
    
    /**
     * Information about a triggered lorebook entry
     */
    data class LorebookEntryInfo(
        val name: String,
        val keys: List<String>,
        val content: String
    )
    
    /**
     * Check if user message triggers any lorebook entries
     */
    fun checkLorebookTriggers(character: Character, userMessage: String): LorebookTriggerResult {
        val characterBook = character.characterBook ?: return LorebookTriggerResult()
        
        return try {
            val bookJson = JSONObject(characterBook)
            val entries = bookJson.optJSONArray("entries") ?: return LorebookTriggerResult()
            
            val triggeredEntries = mutableListOf<LorebookEntryInfo>()
            val triggeredContent = mutableListOf<String>()
            
            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue
                val keys = entry.optJSONArray("keys")
                val content = entry.optString("content", "")
                val enabled = entry.optBoolean("enabled", true)
                val name = entry.optString("name", "Entry ${i + 1}")
                
                if (enabled && content.isNotBlank() && keys != null) {
                    // Check if any key is present in the user message
                    val userMessageLower = userMessage.lowercase()
                    var isTriggered = false
                    val triggeredKeys = mutableListOf<String>()
                    
                    for (j in 0 until keys.length()) {
                        val key = keys.optString(j, "").lowercase()
                        if (key.isNotEmpty() && userMessageLower.contains(key)) {
                            isTriggered = true
                            triggeredKeys.add(key)
                        }
                    }
                    
                    if (isTriggered) {
                        val entryInfo = LorebookEntryInfo(
                            name = name,
                            keys = triggeredKeys,
                            content = content
                        )
                        triggeredEntries.add(entryInfo)
                        triggeredContent.add(content)
                        
                        Log.d(TAG, "Lorebook entry triggered: '$name' with keys: $triggeredKeys")
                    }
                }
            }
            
            LorebookTriggerResult(
                triggeredEntries = triggeredEntries,
                triggeredContent = triggeredContent.joinToString("\n\n")
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking lorebook triggers", e)
            LorebookTriggerResult()
        }
    }
    
    /**
     * Get notification message for triggered entries
     */
    fun getNotificationMessage(triggeredEntries: List<LorebookEntryInfo>): String? {
        return when {
            triggeredEntries.isEmpty() -> null
            triggeredEntries.size == 1 -> {
                val entry = triggeredEntries.first()
                "📚 Lorebook entry '${entry.name}' activated"
            }
            else -> {
                val entryNames = triggeredEntries.joinToString(", ") { "'${it.name}'" }
                "📚 ${triggeredEntries.size} lorebook entries activated: $entryNames"
            }
        }
    }
}

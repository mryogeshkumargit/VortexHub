package com.vortexai.android.domain.service

import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for processing character books (lorebooks) and injecting context
 */
@Singleton
class LocalLorebookService @Inject constructor() {
    
    /**
     * Result of character book processing
     */
    data class LorebookResult(
        val beforeCharContent: String = "",
        val afterCharContent: String = "",
        val activatedEntries: List<String> = emptyList(),
        val totalTokens: Int = 0
    )
    
    /**
     * Process character book for context injection
     */
    suspend fun processCharacterBook(
        character: Character,
        conversationHistory: List<Message> = emptyList(),
        userMessage: String? = null,
        maxTokens: Int = 1000
    ): LorebookResult = withContext(Dispatchers.Default) {
        
        try {
            // Check if character has a character book
            val characterBook = character.characterBook
            if (characterBook.isNullOrBlank()) {
                return@withContext LorebookResult()
            }
            
            // Parse character book JSON
            val bookJson = try {
                JSONObject(characterBook)
            } catch (e: Exception) {
                Timber.w("Failed to parse character book JSON: ${e.message}")
                return@withContext LorebookResult()
            }
            
            // Get entries array
            val entriesArray = bookJson.optJSONArray("entries") ?: return@withContext LorebookResult()
            
            // Build context for keyword matching
            val contextText = buildContextText(conversationHistory, userMessage)
            
            // Process entries and find matches
            val activatedEntries = mutableListOf<LorebookEntry>()
            
            for (i in 0 until entriesArray.length()) {
                val entryJson = entriesArray.optJSONObject(i) ?: continue
                val entry = parseLorebookEntry(entryJson)
                
                if (entry != null && shouldActivateEntry(entry, contextText)) {
                    activatedEntries.add(entry)
                }
            }
            
            // Sort by priority (higher priority first)
            activatedEntries.sortByDescending { it.priority }
            
            // Build result content
            val beforeCharEntries = activatedEntries.filter { it.position == EntryPosition.BEFORE_CHAR }
            val afterCharEntries = activatedEntries.filter { it.position == EntryPosition.AFTER_CHAR }
            
            val beforeCharContent = beforeCharEntries.joinToString("\n\n") { it.content }
            val afterCharContent = afterCharEntries.joinToString("\n\n") { it.content }
            
            val totalTokens = estimateTokens(beforeCharContent + afterCharContent)
            
            Timber.d("Processed character book: ${activatedEntries.size} entries activated, ~$totalTokens tokens")
            
            LorebookResult(
                beforeCharContent = beforeCharContent,
                afterCharContent = afterCharContent,
                activatedEntries = activatedEntries.map { it.key },
                totalTokens = totalTokens
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error processing character book")
            LorebookResult()
        }
    }
    
    /**
     * Parse a lorebook entry from JSON
     */
    private fun parseLorebookEntry(entryJson: JSONObject): LorebookEntry? {
        return try {
            val key = entryJson.optString("key", "").trim()
            val content = entryJson.optString("content", "").trim()
            
            if (key.isEmpty() || content.isEmpty()) {
                return null
            }
            
            val keywords = mutableListOf<String>()
            
            // Parse keys array
            val keysArray = entryJson.optJSONArray("keys")
            if (keysArray != null) {
                for (i in 0 until keysArray.length()) {
                    val keyword = keysArray.optString(i, "").trim()
                    if (keyword.isNotEmpty()) {
                        keywords.add(keyword.lowercase())
                    }
                }
            } else {
                // Fallback to single key
                keywords.add(key.lowercase())
            }
            
            val priority = entryJson.optInt("priority", 0)
            val enabled = entryJson.optBoolean("enabled", true)
            val position = when (entryJson.optInt("position", 0)) {
                1 -> EntryPosition.AFTER_CHAR
                else -> EntryPosition.BEFORE_CHAR
            }
            
            LorebookEntry(
                key = key,
                keywords = keywords,
                content = content,
                priority = priority,
                enabled = enabled,
                position = position
            )
            
        } catch (e: Exception) {
            Timber.w("Failed to parse lorebook entry: ${e.message}")
            null
        }
    }
    
    /**
     * Check if an entry should be activated based on keywords
     */
    private fun shouldActivateEntry(entry: LorebookEntry, contextText: String): Boolean {
        if (!entry.enabled) return false
        
        val lowercaseContext = contextText.lowercase()
        
        return entry.keywords.any { keyword ->
            lowercaseContext.contains(keyword)
        }
    }
    
    /**
     * Build context text for keyword matching
     */
    private fun buildContextText(
        conversationHistory: List<Message>,
        userMessage: String?
    ): String {
        val contextParts = mutableListOf<String>()
        
        // Add recent conversation history
        conversationHistory.takeLast(5).forEach { message ->
            contextParts.add(message.content)
        }
        
        // Add current user message
        userMessage?.let { contextParts.add(it) }
        
        return contextParts.joinToString(" ")
    }
    
    /**
     * Estimate token count (rough approximation)
     */
    private fun estimateTokens(text: String): Int {
        // Rough estimation: ~4 characters per token
        return (text.length / 4).coerceAtLeast(1)
    }
    
    /**
     * Lorebook entry data class
     */
    private data class LorebookEntry(
        val key: String,
        val keywords: List<String>,
        val content: String,
        val priority: Int,
        val enabled: Boolean,
        val position: EntryPosition
    )
    
    /**
     * Entry position enum
     */
    private enum class EntryPosition {
        BEFORE_CHAR,
        AFTER_CHAR
    }
} 
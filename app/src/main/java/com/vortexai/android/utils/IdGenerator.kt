package com.vortexai.android.utils

import android.util.Log
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Centralized ID generation utility for unique, structured IDs
 * Provides collision-resistant, sortable, and debuggable IDs
 */
object IdGenerator {
    
    private const val TAG = "IdGenerator"
    
    // ID Prefixes for different entity types
    private const val CONVERSATION_PREFIX = "conv"
    private const val MESSAGE_PREFIX = "msg"
    private const val CHARACTER_PREFIX = "char"
    private const val USER_PREFIX = "user"
    private const val SESSION_PREFIX = "sess"
    
    // Random number generator for enhanced security
    private val secureRandom = SecureRandom()
    
    // Date formatter for timestamp component
    private val timestampFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    
    /**
     * Generate unique conversation ID with format: conv_YYYYMMDDHHMMSS_XXXXX_RRRRR
     * Where:
     * - conv: Prefix identifying this as a conversation
     * - YYYYMMDDHHMMSS: Timestamp for sorting and debugging
     * - XXXXX: 5-digit sequential counter (resets every second)
     * - RRRRR: 5-digit random component for collision avoidance
     */
    fun generateConversationId(): String {
        return generateStructuredId(CONVERSATION_PREFIX)
    }
    
    /**
     * Generate unique message ID with format: msg_YYYYMMDDHHMMSS_XXXXX_RRRRR
     */
    fun generateMessageId(): String {
        return generateStructuredId(MESSAGE_PREFIX)
    }
    
    /**
     * Generate unique character ID with format: char_YYYYMMDDHHMMSS_XXXXX_RRRRR
     */
    fun generateCharacterId(): String {
        return generateStructuredId(CHARACTER_PREFIX)
    }
    
    /**
     * Generate unique user ID with format: user_YYYYMMDDHHMMSS_XXXXX_RRRRR
     */
    fun generateUserId(): String {
        return generateStructuredId(USER_PREFIX)
    }
    
    /**
     * Generate unique session ID with format: sess_YYYYMMDDHHMMSS_XXXXX_RRRRR
     */
    fun generateSessionId(): String {
        return generateStructuredId(SESSION_PREFIX)
    }
    
    /**
     * Generate structured ID with timestamp, counter, and random components
     */
    private fun generateStructuredId(prefix: String): String {
        val timestamp = timestampFormat.format(Date())
        val counter = getCounter()
        val random = secureRandom.nextInt(100000).toString().padStart(5, '0')
        
        val id = "${prefix}_${timestamp}_${counter}_${random}"
        Log.d(TAG, "Generated ID: $id")
        return id
    }
    
    // Counter for sequence numbers (resets every second)
    private var lastCounterTimestamp = 0L
    private var counter = 0
    
    /**
     * Get sequential counter that resets every second
     */
    private fun getCounter(): String {
        val currentTimestamp = System.currentTimeMillis() / 1000
        
        if (currentTimestamp != lastCounterTimestamp) {
            // Reset counter for new second
            counter = 0
            lastCounterTimestamp = currentTimestamp
        } else {
            // Increment counter within same second
            counter++
        }
        
        return counter.toString().padStart(5, '0')
    }
    
    /**
     * Generate simple UUID-based ID (fallback)
     */
    fun generateSimpleId(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Generate short ID (8 characters) for display purposes
     */
    fun generateShortId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * Validate ID format
     */
    fun isValidConversationId(id: String): Boolean {
        return id.matches(Regex("^conv_\\d{14}_\\d{5}_\\d{5}$"))
    }
    
    fun isValidMessageId(id: String): Boolean {
        return id.matches(Regex("^msg_\\d{14}_\\d{5}_\\d{5}$"))
    }
    
    fun isValidCharacterId(id: String): Boolean {
        return id.matches(Regex("^char_\\d{14}_\\d{5}_\\d{5}$"))
    }
    
    /**
     * Extract timestamp from structured ID
     */
    fun extractTimestamp(id: String): Date? {
        return try {
            val parts = id.split("_")
            if (parts.size >= 2) {
                timestampFormat.parse(parts[1])
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract timestamp from ID: $id", e)
            null
        }
    }
    
    /**
     * Extract entity type from ID
     */
    fun extractEntityType(id: String): String? {
        return id.split("_").firstOrNull()
    }
    
    /**
     * Generate conversation ID with specific character reference (for debugging)
     */
    fun generateConversationIdForCharacter(characterId: String): String {
        val baseId = generateConversationId()
        val charRef = characterId.takeLast(4)
        Log.d(TAG, "Generated conversation ID for character $charRef: $baseId")
        return baseId
    }
    
    /**
     * Batch generate multiple IDs (ensuring uniqueness)
     */
    fun generateBatchIds(count: Int, type: String = "conv"): List<String> {
        val ids = mutableListOf<String>()
        repeat(count) {
            val id = when (type) {
                "conv" -> generateConversationId()
                "msg" -> generateMessageId()
                "char" -> generateCharacterId()
                "user" -> generateUserId()
                else -> generateSimpleId()
            }
            ids.add(id)
        }
        return ids
    }
    
    /**
     * Database-safe ID generation with collision retry
     */
    fun generateUniqueId(
        prefix: String,
        existingIds: Set<String>,
        maxRetries: Int = 10
    ): String {
        repeat(maxRetries) { attempt ->
            val id = generateStructuredId(prefix)
            if (!existingIds.contains(id)) {
                Log.d(TAG, "Generated unique ID on attempt ${attempt + 1}: $id")
                return id
            }
            Log.w(TAG, "ID collision detected on attempt ${attempt + 1}: $id")
        }
        
        // Fallback to UUID if all structured attempts fail
        val fallbackId = "${prefix}_${UUID.randomUUID()}"
        Log.w(TAG, "Using fallback UUID-based ID after $maxRetries attempts: $fallbackId")
        return fallbackId
    }
} 
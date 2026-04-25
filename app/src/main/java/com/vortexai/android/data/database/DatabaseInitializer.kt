package com.vortexai.android.data.database

import android.content.Context
import android.util.Log
import android.util.Base64
import android.graphics.Bitmap
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Conversation
import com.vortexai.android.data.models.Message
import com.vortexai.android.data.database.dao.CharacterDao
import com.vortexai.android.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.ByteArrayOutputStream

/**
 * Handles database initialization and verification
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val database: VortexDatabase,
    private val characterDao: CharacterDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "DatabaseInitializer"
    }
    
    /**
     * Initialize and verify database functionality
     */
    suspend fun initializeDatabase(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing database...")
                
                // Create sample characters
                createSampleCharacters()
                
                Log.d(TAG, "Database initialization completed successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Database initialization failed", e)
                false
            }
        }
    }
    
    /**
     * Test database health and basic operations
     */
    suspend fun testDatabaseHealth(): Boolean {
        return try {
            Log.d(TAG, "Testing database health...")
            
            // Test 1: Check if database file exists and is accessible
            val dbPath = database.openHelper.readableDatabase.path
            Log.d(TAG, "Database path: $dbPath")
            
            // Test 2: Test basic query operations
            val characterCount = characterDao.getActiveCharacterCount()
            Log.d(TAG, "Current character count: $characterCount")
            
            // Test 3: Test if we can perform a simple insert and delete
            val testCharacter = createTestCharacter()
            characterDao.insertCharacter(testCharacter)
            Log.d(TAG, "Test character inserted successfully")
            
            val retrievedCharacter = characterDao.getCharacterById(testCharacter.id)
            if (retrievedCharacter != null) {
                Log.d(TAG, "Test character retrieved successfully: ${retrievedCharacter.name}")
                characterDao.deleteCharacterById(testCharacter.id)
                Log.d(TAG, "Test character deleted successfully")
                true
            } else {
                Log.e(TAG, "Failed to retrieve test character")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database health check failed", e)
            false
        }
    }
    
    /**
     * Create sample characters for demo purposes
     */
    suspend fun createSampleCharacters(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating sample characters for demo purposes")
                
                // Check existing characters but don't clear them
                val existingCount = characterDao.getActiveCharacterCount()
                Log.d(TAG, "Found $existingCount existing characters")
                
                // Only create Victoria Orlov if she doesn't exist
                val victoriaExists = characterDao.getAllCharacters().any { it.name == "Victoria Orlov" }
                if (victoriaExists) {
                    Log.d(TAG, "Victoria Orlov already exists, skipping creation")
                    return@withContext true
                }
                
                // Load Victoria Orlov's avatar from assets and convert to base64
                val victoriaAvatarBase64 = loadVictoriaOrlovAvatarAsBase64()
                
                // Create only Victoria Orlov character with base64 avatar data
                val victoriaOrlov = createSampleCharacter(
                    name = "Victoria Orlov",
                    description = "A sophisticated and mysterious character with a rich backstory and complex personality. Victoria is an elegant woman with a sharp intellect and a mysterious past that she reveals slowly to those who earn her trust.",
                    personality = "Intelligent, mysterious, sophisticated, caring, witty",
                    isFeatured = true,
                    totalMessages = 0,
                    totalRatings = 0,
                    avatarPath = victoriaAvatarBase64 // Store base64 data instead of asset path
                )
                
                // Insert Victoria Orlov character
                characterDao.insertCharacter(victoriaOrlov)
                Log.d(TAG, "Created sample character: ${victoriaOrlov.name}")
                
                // DO NOT create sample conversations - let users start fresh
                // This prevents pre-existing chats from appearing
                Log.d(TAG, "Skipping sample conversation creation - users will start fresh")
                
                val finalCount = characterDao.getActiveCharacterCount()
                Log.d(TAG, "Sample characters created successfully. Total characters: $finalCount")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error creating sample characters", e)
                false
            }
        }
    }
    
    /**
     * Load Victoria Orlov's avatar image from assets and convert to base64
     */
    private suspend fun loadVictoriaOrlovAvatarAsBase64(): String {
        return withContext(Dispatchers.IO) {
            try {
                // Try to load from assets first
                val assetPath = "victoria_orlov_.png"
                Log.d(TAG, "Loading Victoria Orlov avatar from assets: $assetPath")
                
                // Use ImageUtils to get bitmap from assets
                val bitmap = ImageUtils.getBitmapFromAssets(
                    context,
                    assetPath
                )
                
                if (bitmap != null) {
                    // Convert bitmap to base64
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    val bytes = outputStream.toByteArray()
                    outputStream.close()
                    
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    Log.d(TAG, "Successfully loaded Victoria Orlov avatar as base64 (${base64.length} chars)")
                    
                    // Return as data URI format for compatibility
                    "data:image/jpeg;base64,$base64"
                } else {
                    Log.w(TAG, "Failed to load Victoria Orlov avatar from assets, using fallback")
                    // Fallback to asset path if loading fails
                    "file:///android_asset/victoria_orlov_.png"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Victoria Orlov avatar from assets: ${e.message}", e)
                // Fallback to asset path if loading fails
                "file:///android_asset/victoria_orlov_.png"
            }
        }
    }
    

    
    /**
     * Create sample conversations for recent chats
     */
    private suspend fun createSampleConversations(characters: List<Character>) {
        try {
            val conversationDao = database.conversationDao()
            val messageDao = database.messageDao()
            
            characters.forEachIndexed { index, character ->
                val conversationId = "conv_sample_${character.id}_${System.currentTimeMillis() + index}"
                val messageCount = (5..15).random()
                val userMessageCount = (messageCount * 0.4).toInt().coerceAtLeast(2)
                val characterMessageCount = messageCount - userMessageCount
                
                val conversation = Conversation(
                    id = conversationId,
                    title = "Chat with ${character.name}",
                    characterId = character.id,
                    characterName = character.name,
                    userId = "demo_user",
                    totalMessages = messageCount,
                    userMessages = userMessageCount,
                    characterMessages = characterMessageCount,
                    createdAt = System.currentTimeMillis() - (index * 3600000L), // Stagger creation times
                    lastMessageAt = System.currentTimeMillis() - (index * 1800000L) // Recent activity
                )
                
                conversationDao.insertConversation(conversation)
                
                // Create sample messages for this conversation
                createSampleMessages(conversationId, character, messageCount)
                
                Log.d(TAG, "Created sample conversation with ${character.name} (${messageCount} messages)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sample conversations", e)
        }
    }
    
    /**
     * Create sample messages for a conversation
     */
    private suspend fun createSampleMessages(conversationId: String, character: Character, messageCount: Int) {
        try {
            val messageDao = database.messageDao()
            val baseTime = System.currentTimeMillis() - (messageCount * 300000L) // Start messages 5 minutes apart
            
            val sampleUserMessages = listOf(
                "Hi there! How are you doing today?",
                "That's interesting, tell me more about that.",
                "I've been thinking about what you said.",
                "What do you think about this situation?",
                "That makes a lot of sense to me.",
                "I appreciate you sharing that with me.",
                "How was your day?",
                "That sounds like fun!",
                "I understand what you mean.",
                "Thanks for explaining that."
            )
            
            val sampleCharacterMessages = listOf(
                "Hello! I'm doing great, thanks for asking. How about you?",
                "I'd love to share more about that topic with you.",
                "That's really thoughtful of you to consider it deeply.",
                "Well, from my perspective, I think it's quite fascinating.",
                "I'm glad we're on the same page about this.",
                "You're very welcome! I enjoy our conversations.",
                "My day has been wonderful, full of interesting thoughts.",
                "It really is! I love exploring new ideas and experiences.",
                "I'm happy that my explanation resonated with you.",
                "Always happy to help clarify things for you!"
            )
            
            for (i in 0 until messageCount) {
                val isUserMessage = (i % 2 == 0) // Alternate between user and character
                val messageId = "msg_${conversationId}_${i}_${System.currentTimeMillis()}"
                
                val message = Message(
                    id = messageId,
                    conversationId = conversationId,
                    content = if (isUserMessage) {
                        sampleUserMessages.random()
                    } else {
                        sampleCharacterMessages.random()
                    },
                    role = if (isUserMessage) "user" else "character",
                    timestamp = baseTime + (i * 300000L), // 5 minutes apart
                    characterId = if (isUserMessage) null else character.id,
                    characterName = if (isUserMessage) null else character.name,
                    userId = if (isUserMessage) "demo_user" else null
                )
                
                messageDao.insertMessage(message)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sample messages for conversation $conversationId", e)
        }
    }
    
    /**
     * Clear all character data (for testing purposes)
     */
    suspend fun clearAllCharacters(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing all characters...")
                database.clearAllTables()
                Log.d(TAG, "All characters cleared successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear characters", e)
                false
            }
        }
    }
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            try {
                val characterCount = characterDao.getActiveCharacterCount()
                val publicCharacterCount = characterDao.getPublicCharacterCount()
                
                DatabaseStats(
                    totalCharacters = characterCount,
                    publicCharacters = publicCharacterCount,
                    isHealthy = true,
                    databasePath = database.openHelper.readableDatabase.path ?: "Unknown"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get database stats", e)
                DatabaseStats(
                    totalCharacters = 0,
                    publicCharacters = 0,
                    isHealthy = false,
                    databasePath = "Error: ${e.message}"
                )
            }
        }
    }
    
    private fun createTestCharacter(): Character {
        return Character(
            id = "test_char_${System.currentTimeMillis()}",
            name = "Test Character",
            displayName = "Test Character",
            shortDescription = "A test character for database verification",
            longDescription = "This is a temporary character created to test database functionality",
            persona = "Test persona",
            backstory = "Test backstory",
            greeting = "Hello! I'm a test character.",
            avatarUrl = null,
            appearance = null,
            personality = "Friendly test personality",
            scenario = "Test scenario",
            exampleDialogue = "Test: Hello\nCharacter: Hi there!",
            characterBook = null,
            temperature = 0.7f,
            topP = 0.9f,
            maxTokens = 512,
            nsfwEnabled = false,
            tags = listOf("test"),
            categories = listOf("test"),
            creatorId = null,
            creator = "System",
            creatorNotes = "Auto-generated test character",
            characterVersion = "1.0",
            isPublic = false,
            isFeatured = false,
            isFavorite = false,
            description = "Test character for database verification",
            stats = null,
            version = 1,
            totalMessages = 0,
            totalConversations = 0,
            averageRating = 0.0f,
            totalRatings = 0,
            lastInteraction = null,
            isActive = true
        )
    }
    
    private fun createSampleCharacter(
        name: String, 
        description: String, 
        personality: String,
        isFeatured: Boolean = false,
        totalMessages: Int = 0,
        totalRatings: Int = 0,
        avatarPath: String? = null
    ): Character {
        val personalizedGreeting = when (name) {
            "Victoria Orlov" -> "Good evening. I'm Victoria Orlov. *adjusts her elegant posture with a subtle, knowing smile* I trust you have something interesting to discuss? I find that the most fascinating conversations happen when two minds meet with mutual respect and curiosity."
            else -> "Hello! I'm $name. Nice to meet you!"
        }
        
        return Character(
            id = "sample_${name.lowercase()}_${System.currentTimeMillis()}_${(1000..9999).random()}",
            name = name,
            displayName = name,
            shortDescription = description,
            longDescription = description,
            persona = personality,
            backstory = "A character created as a sample for the Vortex AI app",
            greeting = personalizedGreeting,
            avatarUrl = avatarPath,
            appearance = null,
            personality = personality,
            scenario = "A friendly conversation partner",
            exampleDialogue = "User: Hello!\n$name: Hi there! How can I help you today?",
            characterBook = null,
            temperature = 0.7f,
            topP = 0.9f,
            maxTokens = 512,
            nsfwEnabled = false,
            tags = listOf("sample", "friendly", if (isFeatured) "featured" else "popular"),
            categories = listOf("general"),
            creatorId = null,
            creator = "Vortex AI",
            creatorNotes = "Sample character for demonstration",
            characterVersion = "1.0",
            isPublic = true,
            isFeatured = isFeatured,
            isFavorite = false,
            description = description,
            stats = null,
            version = 1,
            totalMessages = totalMessages,
            totalConversations = (totalMessages / 10).coerceAtLeast(1),
            averageRating = (3.5f + Math.random().toFloat() * 1.5f).toFloat(), // Random rating between 3.5-5.0
            totalRatings = totalRatings,
            lastInteraction = null,
            isActive = true
        )
    }
}

data class DatabaseStats(
    val totalCharacters: Int,
    val publicCharacters: Int,
    val isHealthy: Boolean,
    val databasePath: String
) 
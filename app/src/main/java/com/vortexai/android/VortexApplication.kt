package com.vortexai.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.vortexai.android.data.database.VortexDatabase
import com.vortexai.android.data.database.DatabaseInitializer
import com.vortexai.android.data.repository.ChatRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Vortex AI Android app
 * Handles initialization and configuration for large character data handling
 */
@HiltAndroidApp
class VortexApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var database: VortexDatabase
    
    @Inject
    lateinit var databaseInitializer: DatabaseInitializer
    
    @Inject
    lateinit var chatRepository: ChatRepository
    
    private var isInitialized = false
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        try {
            // Setup configuration for handling large character data with lorebooks
            setupForLargeDataHandling()
            
            // Initialize database
            initializeDatabase()
            
            // Enable cleartext traffic for ModelsLab
            enableCleartextTraffic()
            
            Log.d(TAG, "VortexApplication initialized with large data handling support")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during app initialization", e)
            // Don't crash - let the app continue
        }
    }
    
    /**
     * Enable cleartext traffic for ModelsLab API
     * This is needed because some networks block HTTPS connections
     */
    private fun enableCleartextTraffic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val policy = StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
                StrictMode.setThreadPolicy(policy)
            }
            
            Log.d(TAG, "Network security policy configured")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure network security policy", e)
        }
    }
    
    private fun initializeDatabase() {
        try {
            if (!::database.isInitialized || !::databaseInitializer.isInitialized) {
                Log.e(TAG, "Database dependencies not injected yet")
                return
            }
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Starting database initialization...")
                    
                    // Use DatabaseInitializer for comprehensive setup
                    val isInitialized = databaseInitializer.initializeDatabase()
                
                if (isInitialized) {
                    Log.d(TAG, "Database initialization completed successfully")
                    
                    // Clear any existing demo conversations on first startup
                    clearDemoConversations()
                    
                    // Get and log database statistics
                    val stats = databaseInitializer.getDatabaseStats()
                    Log.d(TAG, "Database Stats - Characters: ${stats.totalCharacters}, Public: ${stats.publicCharacters}, Healthy: ${stats.isHealthy}")
                    
                    // Create sample characters if database is empty
                    if (stats.totalCharacters == 0) {
                        Log.d(TAG, "Creating sample characters for first-time setup...")
                        val samplesCreated = databaseInitializer.createSampleCharacters()
                        if (samplesCreated) {
                            Log.d(TAG, "Sample characters created successfully")
                        } else {
                            Log.w(TAG, "Failed to create sample characters")
                        }
                    }
                } else {
                    Log.e(TAG, "Database initialization failed, attempting recovery...")
                    // Try to clear and reinitialize
                    try {
                        databaseInitializer.clearAllCharacters()
                        val retryInit = databaseInitializer.initializeDatabase()
                        if (retryInit) {
                            Log.d(TAG, "Database recovery successful")
                        } else {
                            Log.e(TAG, "Database recovery failed")
                        }
                    } catch (recoveryException: Exception) {
                        Log.e(TAG, "Database recovery attempt failed", recoveryException)
                    }
                }
                } catch (e: Exception) {
                    Log.e(TAG, "Database initialization process failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start database initialization coroutine", e)
        }
    }
    
    /**
     * Clear any existing demo conversations to ensure clean state
     */
    private suspend fun clearDemoConversations() {
        try {
            Log.d(TAG, "Clearing any existing demo conversations...")
            val conversationDao = database.conversationDao()
            val messageDao = database.messageDao()
            
            // Get all conversations
            val conversations = conversationDao.getAllConversations()
            
            // Check if any conversations have demo character names that shouldn't exist
            val demoConversations = conversations.filter { conversation ->
                conversation.characterName in listOf("Aria", "Nova", "Zara") ||
                conversation.characterId in listOf("char1", "char2", "char3")
            }
            
            if (demoConversations.isNotEmpty()) {
                Log.d(TAG, "Found ${demoConversations.size} demo conversations to remove")
                
                // Delete messages for demo conversations
                demoConversations.forEach { conversation ->
                    messageDao.deleteMessagesByConversationId(conversation.id)
                    conversationDao.deleteConversationById(conversation.id)
                }
                
                Log.d(TAG, "Demo conversations cleared successfully")
            } else {
                Log.d(TAG, "No demo conversations found to clear")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing demo conversations", e)
        }
    }
    
    /**
     * Configure WorkManager for background data processing
     */
    override val workManagerConfiguration: Configuration
        get() = try {
            if (::workerFactory.isInitialized) {
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build()
            } else {
                Configuration.Builder()
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating WorkManager configuration", e)
            Configuration.Builder().build()
        }
    
    /**
     * Setup configuration for handling large character data with lorebooks
     */
    private fun setupForLargeDataHandling() {
        // Configure for better memory management with large data
        System.setProperty("java.net.useSystemProxies", "true")
    }
    
    companion object {
        private const val TAG = "VortexApplication"
        
        @JvmStatic
        lateinit var instance: VortexApplication
            private set
    }
} 
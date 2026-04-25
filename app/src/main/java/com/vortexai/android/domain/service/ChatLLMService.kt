package com.vortexai.android.domain.service

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.MessageResponse
import com.vortexai.android.data.models.MessageSenderType
import com.vortexai.android.domain.service.llm.GenerationParams
import com.vortexai.android.domain.service.llm.KoboldProvider
import com.vortexai.android.domain.service.llm.LLMProvider
import com.vortexai.android.domain.service.llm.OllamaProvider
import com.vortexai.android.domain.service.llm.OpenAIProvider
import com.vortexai.android.domain.service.llm.CustomAPIProvider
import com.vortexai.android.domain.service.llm.AnthropicProvider
import com.vortexai.android.domain.service.llm.TogetherProvider
import com.vortexai.android.domain.service.llm.ModelsLabProvider
import com.vortexai.android.domain.service.llm.OpenRouterProvider
import com.vortexai.android.domain.service.llm.GeminiProvider
import com.vortexai.android.utils.MacroProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import com.vortexai.android.utils.EndpointChecker
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.vortexai.android.domain.service.llm.LMStudioProvider

@Serializable
data class LLMModel(
    val id: String,
    val name: String,
    val provider: String,
    val description: String? = null
)

@Singleton
class ChatLLMService @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val macroProcessor: MacroProcessor,
    private val apiConnectionTester: com.vortexai.android.utils.ApiConnectionTester,
    private val customApiProviderRepository: com.vortexai.android.data.repository.CustomApiProviderRepository
) {
    
    companion object {
        private const val TAG = "ChatLLMService"
        
        // Settings keys
        private val LLM_PROVIDER_KEY = stringPreferencesKey("llm_provider")
        private val TOGETHER_AI_API_KEY = stringPreferencesKey("together_ai_api_key")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        private val HUGGINGFACE_API_KEY = stringPreferencesKey("huggingface_api_key")
        private val CUSTOM_LLM_API_KEY = stringPreferencesKey("custom_llm_api_key")
        private val CUSTOM_LLM_ENDPOINT_KEY = stringPreferencesKey("custom_llm_endpoint")
        private val CUSTOM_LLM_API_PREFIX_KEY = stringPreferencesKey("custom_llm_api_prefix")
        
        // New LLM Providers
        private val OLLAMA_API_KEY = stringPreferencesKey("ollama_api_key")
        private val OLLAMA_ENDPOINT_KEY = stringPreferencesKey("ollama_endpoint")
        private val KOBOLD_API_KEY = stringPreferencesKey("kobold_api_key")
        private val KOBOLD_ENDPOINT_KEY = stringPreferencesKey("kobold_endpoint")
        private val MODELSLAB_API_KEY = stringPreferencesKey("modelslab_api_key")
        private val GROK_API_KEY = stringPreferencesKey("grok_api_key")
        private val LMSTUDIO_ENDPOINT_KEY = stringPreferencesKey("lmstudio_endpoint")
        private val SELECTED_CUSTOM_LLM_PROVIDER_ID = stringPreferencesKey("selected_custom_llm_provider_id")
        
        private val LLM_MODEL_KEY = stringPreferencesKey("llm_model")
        private val RESPONSE_TEMPERATURE_KEY = floatPreferencesKey("response_temperature")
        private val MAX_TOKENS_KEY = intPreferencesKey("max_tokens")
        private val TOP_P_KEY = floatPreferencesKey("top_p")
        private val FREQUENCY_PENALTY_KEY = floatPreferencesKey("frequency_penalty")
        
        // New response formatting and length settings
        private val RESPONSE_LENGTH_STYLE_KEY = stringPreferencesKey("response_length_style") // "short", "natural", "long"
        private val ENABLE_RESPONSE_FORMATTING_KEY = stringPreferencesKey("enable_response_formatting") // "true", "false"
        private val CUSTOM_MAX_TOKENS_KEY = intPreferencesKey("custom_max_tokens") // User-defined token limit
        private val CHARACTER_SPECIFIC_TOKENS_KEY = stringPreferencesKey("character_specific_tokens") // JSON of character-specific settings
    }
    
    /**
     * Test connection to LLM provider
     */
    suspend fun testConnection(provider: String, apiKey: String, model: String? = null, customEndpoint: String? = null): com.vortexai.android.utils.ApiConnectionResult {
        return apiConnectionTester.testLLMConnection(provider, apiKey, model, customEndpoint)
    }
    
    /**
     * Get debug information for Custom API
     */
    suspend fun getCustomAPIDebugInfo(): String {
        return try {
            val preferences = dataStore.data.first()
            val endpoint = preferences[CUSTOM_LLM_ENDPOINT_KEY] ?: ""
            val apiKey = preferences[CUSTOM_LLM_API_KEY] ?: ""
            val apiPrefix = preferences[CUSTOM_LLM_API_PREFIX_KEY] ?: "/v1"
            val selectedModel = preferences[LLM_MODEL_KEY] ?: "custom-model"
            
            val customProvider = CustomAPIProvider().apply {
                setApiKey(apiKey)
                setEndpoint(endpoint)
                setApiPrefix(apiPrefix)
                setModel(selectedModel) // Use the actual selected model
            }
            
            customProvider.getDebugInfo()
        } catch (e: Exception) {
            "Error getting debug info: ${e.message}"
        }
    }
    
    /**
     * Fetch available models from the selected provider
     */
    suspend fun fetchModels(provider: String, apiKey: String, customEndpoint: String? = null): Result<List<LLMModel>> {
        return withContext(Dispatchers.IO) {
            try {
                when (provider) {
                    "Together AI" -> fetchTogetherAIModels(apiKey)
                    "Gemini API" -> fetchGeminiModels(apiKey)
                    "Open Router" -> fetchOpenRouterModels(apiKey)
                    "Hugging Face" -> fetchHuggingFaceModels(apiKey)
                    "Ollama" -> fetchOllamaModels(customEndpoint ?: "http://localhost:11435")
                    "Kobold AI" -> fetchKoboldModels(customEndpoint ?: "http://localhost:5000")
                    "LMStudio" -> fetchLMStudioModels(customEndpoint ?: "http://localhost:1234")
                    "Custom API" -> fetchCustomAPIModels()
                    "ModelsLab" -> fetchModelsLabModels(apiKey)
                    "Grok" -> fetchGrokModels(apiKey)
                    else -> Result.failure(Exception("Unsupported provider: $provider"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching models for $provider", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generate a response using the configured LLM with character context
     */
    suspend fun generateResponse(
        userMessage: String, 
        character: Character? = null, 
        userName: String = "User",
        previousMessage: String? = null,
        conversationHistory: List<MessageResponse> = emptyList(),
        isFirstMessage: Boolean = false
    ): String {
        Log.d(TAG, "🔥 === DEEP DEBUG: generateResponse START ===")
        return try {
            // Get LLM settings from datastore
            val preferences = dataStore.data.first()
            val llmProvider = preferences[LLM_PROVIDER_KEY] ?: "Together AI"
            val llmModel = preferences[LLM_MODEL_KEY] ?: "meta-llama/Llama-2-7b-chat-hf"
            val temperature = preferences[RESPONSE_TEMPERATURE_KEY] ?: 0.7f
            val maxTokens = preferences[MAX_TOKENS_KEY] ?: 4096
            val topP = preferences[TOP_P_KEY] ?: 0.9f
            val frequencyPenalty = preferences[FREQUENCY_PENALTY_KEY] ?: 0.0f
            
            Log.d(TAG, "🔥 DEEP DEBUG: Settings loaded")
            Log.d(TAG, "🔥 Provider: $llmProvider")
            Log.d(TAG, "🔥 Model: $llmModel")
            Log.d(TAG, "🔥 User message: ${userMessage.take(50)}...")
            
            // Get new response settings
            val responseLengthStyle = preferences[RESPONSE_LENGTH_STYLE_KEY] ?: "natural"
            val enableFormatting = preferences[ENABLE_RESPONSE_FORMATTING_KEY] != "false" // Default to true
            val customMaxTokens = preferences[CUSTOM_MAX_TOKENS_KEY] ?: 0
            
            // Determine token limit based on settings and character needs
            val characterId = character?.id ?: ""
            val finalMaxTokens = getTokenLimitForCharacter(characterId, responseLengthStyle, maxTokens, customMaxTokens, preferences)
            
            // Get API key based on provider
            val apiKey = when (llmProvider) {
                "Together AI" -> preferences[TOGETHER_AI_API_KEY] ?: ""
                "Gemini API" -> preferences[GEMINI_API_KEY] ?: ""
                "Open Router" -> preferences[OPENROUTER_API_KEY] ?: ""
                "Hugging Face" -> preferences[HUGGINGFACE_API_KEY] ?: ""
                "Ollama" -> "" // Ollama doesn't require API keys
                "Kobold AI" -> "" // Kobold AI doesn't require API keys
                "LMStudio" -> "" // LMStudio doesn't require API keys
                "Custom API" -> preferences[CUSTOM_LLM_API_KEY] ?: ""
                "ModelsLab" -> preferences[MODELSLAB_API_KEY] ?: ""
                "Grok" -> preferences[GROK_API_KEY] ?: ""
                else -> ""
            }
            
            Log.d(TAG, "🔥 DEEP DEBUG: API Key retrieved")
            Log.d(TAG, "🔥 API Key length: ${apiKey.length}")
            Log.d(TAG, "🔥 API Key preview: ${if (apiKey.length > 8) apiKey.take(8) + "..." else "EMPTY"}")
            
            // For local providers (Ollama, Kobold, LMStudio), don't require API keys
            val requiresApiKey = llmProvider !in listOf("Ollama", "Kobold AI", "LMStudio")
            
            // If no API key configured for providers that need one, return specific error
            if (requiresApiKey && apiKey.isBlank()) {
                Log.e(TAG, "❌ No API key configured for $llmProvider")
                return "❌ No API key configured for $llmProvider. Please go to Settings → LLM Configuration and enter your API key."
            }
            
            // Create LLM provider based on settings
            Log.d(TAG, "🔥 DEEP DEBUG: Creating provider for: $llmProvider")
            val provider = createLLMProvider(llmProvider, apiKey, llmModel)
            Log.d(TAG, "🔥 DEEP DEBUG: Provider created: ${provider.javaClass.simpleName}")
            Log.d(TAG, "🔥 DEEP DEBUG: Provider ready: ${provider.isReady()}")
            
            // Verify provider is ready
            if (!provider.isReady()) {
                Log.e(TAG, "❌ Provider $llmProvider is not ready after initialization")
                return "❌ $llmProvider provider not ready. Please check your API key and model configuration in Settings → LLM Configuration."
            }
            
            // Build character-aware prompt with conversation history and formatting instructions
            val prompt = buildCharacterPrompt(userMessage, character, conversationHistory, isFirstMessage, responseLengthStyle, enableFormatting)
            
            // Debug log the prompt being sent
            Log.d(TAG, "Prompt being sent to LLM: $prompt")
            
            // Create generation parameters with dynamic token limits
            val params = GenerationParams(
                temperature = temperature,
                topP = topP,
                maxTokens = finalMaxTokens,
                frequencyPenalty = frequencyPenalty
            )
            
            // Generate response
            val rawResponse = try {
                Log.d(TAG, "🔥 DEEP DEBUG: About to call provider.generateResponse()")
                Log.d(TAG, "🔥 Prompt preview: ${prompt.take(200)}...")
                Log.d(TAG, "🔥 Params: temp=${params.temperature}, tokens=${params.maxTokens}")
                
                val response = provider.generateResponse(prompt, params)
                Log.d(TAG, "🔥 DEEP DEBUG: Provider call SUCCESS")
                Log.d(TAG, "🔥 Response length: ${response.length}")
                Log.d(TAG, "🔥 Response preview: ${response.take(100)}...")
                
                // Post-process response based on settings
                if (enableFormatting) {
                    formatResponseForRoleplay(response, character, responseLengthStyle)
                } else {
                    processAndTruncateResponse(response, responseLengthStyle)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ LLM API call failed for $llmProvider: ${e.message}", e)
                Log.e(TAG, "🔍 Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "🔍 Stack trace: ${e.stackTrace.take(3).joinToString { it.toString() }}")
                
                // Provide specific error messages based on the exception
                val errorMessage = when {
                    e.message?.contains("HTML error page") == true -> 
                        "❌ Custom API returned HTML instead of JSON. Check your endpoint URL."
                    e.message?.contains("invalid JSON") == true -> 
                        "❌ Custom API returned invalid JSON. Check your endpoint configuration."
                    e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true -> 
                        "❌ Invalid API key for $llmProvider. Please check your API key in Settings."
                    e.message?.contains("403") == true || e.message?.contains("Forbidden") == true -> 
                        "❌ Access denied by $llmProvider. Check your API key permissions."
                    e.message?.contains("404") == true -> 
                        "❌ Model '$llmModel' not found on $llmProvider. Please select a different model in Settings."
                    e.message?.contains("429") == true -> 
                        "❌ Rate limit exceeded for $llmProvider. Please wait and try again."
                    e.message?.contains("500") == true || e.message?.contains("502") == true || 
                    e.message?.contains("503") == true || e.message?.contains("504") == true -> 
                        "❌ $llmProvider server error. Please try again later."
                    e.message?.contains("timeout") == true || e.message?.contains("Timeout") == true -> 
                        "❌ $llmProvider connection timeout. Please try again."
                    e.message?.contains("Connection") == true || e.message?.contains("network") == true -> 
                        "❌ Network error connecting to $llmProvider. Check your internet connection."
                    apiKey.isBlank() && requiresApiKey -> 
                        "❌ No API key configured for $llmProvider. Please go to Settings > LLM Config."
                    llmModel.isBlank() -> 
                        "❌ No model selected for $llmProvider. Please select a model in Settings > LLM Config."
                    else -> 
                        "❌ $llmProvider error: ${e.message ?: "Unknown error"}"
                }
                
                errorMessage
            }
            
            // Validate response before processing
            if (rawResponse.isBlank() || rawResponse.trim() == "null" || rawResponse.trim() == "null.") {
                Log.e(TAG, "LLM returned empty or null response")
                return "❌ AI returned an empty response. Please try again or check your LLM configuration."
            }
            
            // Process macros in the response
            macroProcessor.processAIResponse(
                response = rawResponse,
                character = character,
                userName = userName,
                lastUserInput = userMessage,
                previousMessage = previousMessage
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "🔥 DEEP DEBUG: CRITICAL ERROR in generateResponse")
            Log.e(TAG, "🔥 Exception: ${e.javaClass.simpleName}")
            Log.e(TAG, "🔥 Message: ${e.message}")
            Log.e(TAG, "🔥 Cause: ${e.cause?.message}")
            Log.e(TAG, "🔥 Stack trace:")
            e.stackTrace.take(10).forEach { 
                Log.e(TAG, "🔥   $it")
            }
            
            val errorResponse = "❌ Critical error: ${e.message}. Please check your LLM configuration in Settings."
            macroProcessor.processAIResponse(
                response = errorResponse,
                character = character,
                userName = userName,
                lastUserInput = userMessage,
                previousMessage = previousMessage
            )
        }
    }
    
    /**
     * Create LLM provider based on settings
     */
    private suspend fun createLLMProvider(providerName: String, apiKey: String, model: String): LLMProvider {
        val preferences = dataStore.data.first()
        
        return when (providerName) {
            "Together AI" -> TogetherProvider().apply { 
                setApiKey(apiKey)
                if (model.isNotBlank()) setModel(model)
            }
            "Gemini API" -> GeminiProvider().apply { setApiKey(apiKey) }
            "Open Router" -> OpenRouterProvider().apply { 
                setApiKey(apiKey)
                if (model.isNotBlank()) {
                    Log.d(TAG, "Setting OpenRouter model to: $model")
                    setModel(model)
                }
            }
            "Hugging Face" -> TogetherProvider().apply { 
                setApiKey(apiKey)
                if (model.isNotBlank()) setModel(model)
            } // Using Together as placeholder
            "ModelsLab" -> ModelsLabProvider().apply { 
                setApiKey(apiKey)
                if (model.isNotBlank()) setModel(model)
            }
            "Ollama" -> {
                val endpoint = preferences[OLLAMA_ENDPOINT_KEY] ?: "http://localhost:11435"
                val sanitizedEndpoint = endpoint.trim().removeSuffix("/")
                OllamaProvider().apply { 
                    setEndpoint(sanitizedEndpoint)
                    setApiKey(apiKey)
                    setModel(model)
                }
            }
            "Kobold AI" -> {
                val endpoint = preferences[KOBOLD_ENDPOINT_KEY] ?: "http://localhost:5000"
                KoboldProvider().apply { 
                    setEndpoint(endpoint)
                    setApiKey(apiKey)
                    setModel(model)
                }
            }
            "LMStudio" -> {
                val endpoint = preferences[LMSTUDIO_ENDPOINT_KEY] ?: "http://localhost:1234"
                val sanitizedEndpoint = endpoint.trim().removeSuffix("/")
                LMStudioProvider().apply {
                    setEndpoint(sanitizedEndpoint)
                    setApiKey(apiKey)
                    setModel(model)
                }
            }
            "Custom API" -> {
                val selectedProviderId = preferences[SELECTED_CUSTOM_LLM_PROVIDER_ID]
                if (selectedProviderId.isNullOrBlank()) {
                    throw Exception("No custom API provider selected. Please select a provider in Settings → LLM Configuration.")
                }
                val provider = customApiProviderRepository.getProviderById(selectedProviderId)
                    ?: throw Exception("Custom API provider not found. Please reconfigure in Settings → LLM Configuration.")
                
                com.vortexai.android.domain.service.llm.DatabaseCustomAPIProvider(
                    customApiProviderRepository,
                    com.vortexai.android.domain.service.CustomApiExecutor()
                ).apply {
                    setProviderId(selectedProviderId)
                }
            }
            "Grok" -> {
                com.vortexai.android.domain.service.llm.GrokProvider().apply {
                    setApiKey(apiKey)
                    if (model.isNotBlank()) setModel(model)
                }
            }
            else -> TogetherProvider().apply { setApiKey(apiKey) }
        }
    }
    
    /**
     * Build character-aware prompt with conversation history, personality, and scenario.
     * This prompt structure is inspired by SillyTavern prompt format for better roleplaying.
     * Enhanced with character book support and improved formatting instructions.
     */
    private fun buildCharacterPrompt(
        userMessage: String,
        character: Character?,
        conversationHistory: List<MessageResponse> = emptyList(),
        isFirstMessage: Boolean = false,
        responseLengthStyle: String,
        enableFormatting: Boolean
    ): String {
        if (character == null) return userMessage

        val promptBuilder = StringBuilder()

        //
        // This prompt construction follows SillyTavern's character card format
        // and best practices for roleplay character definition.
        //

        // Character Identity and Core Information
        promptBuilder.append("### ${character.name}'s Character Card\n")
        character.name.let { promptBuilder.append("**Name:** $it\n") }
        character.shortDescription?.takeIf { it.isNotBlank() }?.let { 
            promptBuilder.append("**Description:** $it\n") 
        }
        promptBuilder.append("\n")

        // Character Details - Using categorized format (inspired by SillyTavern best practices)
        promptBuilder.append("### Character Details\n")
        
        // Appearance
        character.appearance?.takeIf { it.isNotBlank() }?.let { 
            promptBuilder.append("**Appearance:** $it\n") 
        }
        
        // Personality (most important - place early for better adherence)
        character.personality?.takeIf { it.isNotBlank() }?.let { 
            promptBuilder.append("**Personality:** $it\n") 
        }
        
        // Persona/character description
        character.persona?.takeIf { it.isNotBlank() }?.let { 
            promptBuilder.append("**Persona:** $it\n") 
        }
        
        // Long description for additional context
        character.longDescription?.takeIf { it.isNotBlank() }?.let { 
            promptBuilder.append("**Background:** $it\n") 
        }
        
        // Backstory
        character.backstory?.takeIf { it.isNotBlank() }?.let { 
            promptBuilder.append("**Backstory:** $it\n") 
        }
        
        promptBuilder.append("\n")

        // Character Book/Lorebook (if available)
        character.characterBook?.takeIf { it.isNotBlank() }?.let { characterBookJson ->
            try {
                // Parse character book as JSON and extract relevant entries
                val bookJson = org.json.JSONObject(characterBookJson)
                val entries = bookJson.optJSONArray("entries")
                if (entries != null && entries.length() > 0) {
                    val triggeredEntries = mutableListOf<String>()
                    
                    // Check which entries are triggered by the current user message
                    for (i in 0 until entries.length()) {
                        val entry = entries.optJSONObject(i)
                        if (entry != null) {
                            val keys = entry.optJSONArray("keys")
                            val content = entry.optString("content", "")
                            val enabled = entry.optBoolean("enabled", true)
                            
                            if (enabled && content.isNotBlank() && keys != null) {
                                // Check if any key is present in the user message
                                val userMessageLower = userMessage.lowercase()
                                var isTriggered = false
                                
                                for (j in 0 until keys.length()) {
                                    val key = keys.optString(j, "").lowercase()
                                    if (key.isNotEmpty() && userMessageLower.contains(key)) {
                                        isTriggered = true
                                        break
                                    }
                                }
                                
                                if (isTriggered) {
                                    triggeredEntries.add(content)
                                }
                            }
                        }
                    }
                    
                    // Add triggered entries to the prompt
                    if (triggeredEntries.isNotEmpty()) {
                        promptBuilder.append("### Relevant Context\n")
                        triggeredEntries.forEach { content ->
                            promptBuilder.append("$content\n")
                        }
                        promptBuilder.append("\n")
                    } else {
                        // No triggered entries
                    }
                } else {
                    // No entries to process
                }
            } catch (e: Exception) {
                // If parsing fails, skip lorebook processing
                Log.w("ChatLLMService", "Failed to parse character book: ${e.message}")
            }
        }

        // World Scenario and Setting
        character.scenario?.takeIf { it.isNotBlank() }?.let {
            promptBuilder.append("### Current Scenario\n")
            promptBuilder.append(it).append("\n\n")
        }

        // Example Dialogue (crucial for speech patterns)
        character.exampleDialogue?.takeIf { it.isNotBlank() }?.let {
            promptBuilder.append("### Example Dialogue\n")
            promptBuilder.append("```\n$it\n```\n\n")
        }

        // Response Format Instructions (conversational roleplay chat style)
        if (enableFormatting) {
            promptBuilder.append("### Response Format Instructions\n")
            promptBuilder.append("- Respond as ${character.name} in a natural, conversational chat style\n")
            promptBuilder.append("- Write like you're texting or chatting, not writing a story\n")
            promptBuilder.append("- Keep responses direct and engaging - avoid long descriptions or narration\n")
            promptBuilder.append("- Use *asterisks* sparingly for brief actions only when needed\n")
            promptBuilder.append("- Stay consistent with ${character.name}'s personality and speech patterns\n")
            promptBuilder.append("- React naturally to what the user says, like a real conversation\n")
            
            when (responseLengthStyle) {
                "short" -> promptBuilder.append("- Keep responses brief: 1-2 sentences max\n")
                "long" -> promptBuilder.append("- You can give longer responses but stay conversational, not narrative\n")
                else -> promptBuilder.append("- Match your response length to the conversation naturally\n")
            }
            promptBuilder.append("\n")
        }


        // Conversation History
        if (conversationHistory.isNotEmpty()) {
            promptBuilder.append("### Recent Conversation\n")
            val historyLimit = when (responseLengthStyle) {
                "short" -> 10  // Shorter history for concise responses
                "long" -> 20   // Longer history for detailed responses
                else -> 15     // Natural balance
            }
            val history = conversationHistory.takeLast(historyLimit)
            
            history.forEach { message ->
                val speaker = if (message.senderType == MessageSenderType.USER) "User" else character.name
                val content = message.content.trim()
                promptBuilder.append("**$speaker:** $content\n")
            }
            promptBuilder.append("\n")
        }

        // Current Turn Setup
        promptBuilder.append("### Current Turn\n")
        promptBuilder.append("**User:** $userMessage\n")
        promptBuilder.append("**${character.name}:**")

        return promptBuilder.toString()
    }
    
    /**
     * Generate character greeting with macro processing - show complete greeting
     */
    fun generateCharacterGreeting(character: Character, userName: String = "User"): String {
        // Process greeting with macros and return the complete message
        val processedGreeting = macroProcessor.processCharacterGreeting(
            character = character,
            userName = userName
        )
        
        // Return the complete greeting without truncation
        // Only apply reasonable limits to prevent extremely long greetings (>2000 chars)
        return if (processedGreeting.length > 2000) {
            val sentences = processedGreeting.split(". ", "! ", "? ")
            if (sentences.size > 1) {
                // Take first few sentences that fit within limit
                val builder = StringBuilder()
                for (sentence in sentences) {
                    if (builder.length + sentence.length + 2 <= 2000) {
                        if (builder.isNotEmpty()) builder.append(". ")
                        builder.append(sentence)
                    } else {
                        break
                    }
                }
                val result = builder.toString()
                result + if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) "." else ""
            } else {
                processedGreeting.take(2000) + "..."
            }
        } else {
            processedGreeting
        }
    }

    /**
     * Generate a local fallback response when API calls fail
     */
    private fun generateLocalFallbackResponse(
        userMessage: String, 
        character: Character?, 
        isFirstMessage: Boolean = false,
        conversationHistory: List<MessageResponse> = emptyList(),
        responseLengthStyle: String
    ): String {
        // Return a helpful error message with guidance
        return "❌ Couldn't connect to AI endpoint. Please check your LLM settings in Settings → LLM Configuration and ensure you have a valid API key and model selected."
    }
    
    // Model fetching methods for different providers
    
    private suspend fun fetchTogetherAIModels(apiKey: String): Result<List<LLMModel>> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("Together AI API key is required"))
                }
                
                val httpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
                    .url("https://api.together.xyz/v1/models")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
                
                Log.d(TAG, "Fetching Together AI models with API key: ${apiKey.take(8)}...")
                val response = httpClient.newCall(request).execute()
                Log.d(TAG, "Together AI API response code: ${response.code}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Together AI API response body length: ${responseBody.length}")
                    Log.d(TAG, "Together AI API response preview: ${responseBody.take(500)}...")
                    
                    // Together AI returns direct array, not {"data": [...]}
                    val modelsArray = try {
                        org.json.JSONArray(responseBody)
                    } catch (e: Exception) {
                        // Fallback: try parsing as object with data field
                        val json = org.json.JSONObject(responseBody)
                        json.optJSONArray("data") ?: org.json.JSONArray()
                    }
                    Log.d(TAG, "Together AI models array length: ${modelsArray.length()}")
                    
                    // Log first few models to see structure
                    for (i in 0 until minOf(5, modelsArray.length())) {
                        val modelObj = modelsArray.getJSONObject(i)
                        Log.d(TAG, "Model $i: ${modelObj.toString()}")
                    }
                    
                    val models = mutableListOf<LLMModel>()
                    var totalModels = 0
                    var excludedModels = 0
                    
                    for (i in 0 until modelsArray.length()) {
                        val modelObj = modelsArray.getJSONObject(i)
                        val id = modelObj.optString("id", "")
                        val displayName = modelObj.optString("display_name", id)
                        val description = modelObj.optString("description", "")
                        val type = modelObj.optString("type", "")
                        totalModels++
                        
                        // Use the type field for accurate filtering - Together AI provides clear types
                        val isLLMModel = type.equals("chat", ignoreCase = true) ||
                                       type.equals("language", ignoreCase = true)
                        
                        if (isLLMModel && id.isNotBlank()) {
                            models.add(LLMModel(id, displayName.ifBlank { id }, "Together AI", description))
                            Log.d(TAG, "✅ Included LLM model: $id")
                        } else {
                            excludedModels++
                            val reason = when {
                                id.isBlank() -> "empty ID"
                                type.isNotBlank() -> "not chat/language type (type: $type)"
                                else -> "unknown type"
                            }
                            Log.d(TAG, "❌ Excluded model: $id ($reason)")
                        }
                    }
                    
                    Log.d(TAG, "Together AI filtering: $totalModels total, ${models.size} included, $excludedModels excluded")
                    
                    if (models.isEmpty()) {
                        // Return default models if API doesn't return any
                        Log.e(TAG, "❌ NO MODELS FOUND AFTER FILTERING - USING DEFAULTS")
                        return@withContext Result.success(getDefaultTogetherAIModels())
                    }
                    
                    Log.d(TAG, "✅ SUCCESS: Returning ${models.size} Together AI models to ViewModel")
                    Result.success(models)
                } else {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e(TAG, "Together AI API call failed: ${response.code}")
                    Log.e(TAG, "Together AI API error body: $errorBody")
                    
                    val errorMessage = when (response.code) {
                        401 -> "Authentication failed - Invalid API key"
                        403 -> "Access denied - API key doesn't have permissions" 
                        404 -> "Together AI models endpoint not found"
                        429 -> "Rate limit exceeded"
                        500, 502, 503, 504 -> "Together AI server error"
                        else -> "Together AI API error: ${response.code}"
                    }
                    Log.e(TAG, "Together AI error: $errorMessage")
                    Result.success(getDefaultTogetherAIModels())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch Together AI models, using defaults: ${e.message}")
                Result.success(getDefaultTogetherAIModels())
            }
        }
    }
    
    private fun getDefaultTogetherAIModels(): List<LLMModel> {
        return listOf(
            // Top recommended models for uncensored roleplay per documentation
            LLMModel("Gryphe/MythoMax-L2-13b", "MythoMax L2 13B", "Together AI", "Excellent for creative roleplay and storytelling"),
            LLMModel("NousResearch/Nous-Hermes-2-Mixtral-8x7B-DPO", "Nous Hermes 2 Mixtral", "Together AI", "Advanced reasoning and roleplay model"),
            LLMModel("teknium/OpenHermes-2.5-Mistral-7B", "OpenHermes 2.5 Mistral 7B", "Together AI", "High-quality conversational model"),
            LLMModel("Austism/chronos-hermes-13b", "Chronos Hermes 13B", "Together AI", "Creative and unfiltered responses"),
            LLMModel("meta-llama/Llama-3-70b-chat-hf", "Llama 3 70B Chat", "Together AI", "Powerful model for complex conversations"),
            // Additional models for variety
            LLMModel("meta-llama/Llama-3.2-3B-Instruct-Turbo", "Llama 3.2 3B Instruct Turbo", "Together AI", "Fast and efficient chat model"),
            LLMModel("meta-llama/Llama-2-13b-chat-hf", "Llama 2 13B Chat", "Together AI", "Balanced performance and quality"),
            LLMModel("mistralai/Mixtral-8x7B-Instruct-v0.1", "Mixtral 8x7B", "Together AI", "Multilingual instruction model")
        )
    }
    
    private suspend fun fetchGeminiModels(apiKey: String): Result<List<LLMModel>> {
        // Gemini doesn't have a public models endpoint, so return known models
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API key is required"))
        }
        
        return Result.success(listOf(
            LLMModel("gemini-1.5-pro", "Gemini 1.5 Pro", "Gemini API", "Google's most capable model"),
            LLMModel("gemini-1.5-flash", "Gemini 1.5 Flash", "Gemini API", "Fast and efficient model"),
            LLMModel("gemini-pro", "Gemini Pro", "Gemini API", "Google's advanced AI model"),
            LLMModel("gemini-pro-vision", "Gemini Pro Vision", "Gemini API", "Multimodal model with vision")
        ))
    }
    
    private suspend fun fetchOpenRouterModels(apiKey: String): Result<List<LLMModel>> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("OpenRouter API key is required"))
                }
                
                val httpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
                    .url("https://openrouter.ai/api/v1/models")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val json = org.json.JSONObject(responseBody)
                    val modelsArray = json.optJSONArray("data") ?: org.json.JSONArray()
                    
                    val models = mutableListOf<LLMModel>()
                    for (i in 0 until modelsArray.length()) {
                        val modelObj = modelsArray.getJSONObject(i)
                        val id = modelObj.optString("id", "")
                        val name = modelObj.optString("name", id)
                        val description = modelObj.optString("description", "")
                        
                        models.add(LLMModel(id, name, "Open Router", description))
                    }
                    
                    if (models.isEmpty()) {
                        return@withContext Result.success(getDefaultOpenRouterModels())
                    }
                    
                    Result.success(models.take(100)) // Limit to first 100 models
                } else {
                    Log.w(TAG, "OpenRouter API call failed: ${response.code}")
                    Result.success(getDefaultOpenRouterModels())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch OpenRouter models, using defaults: ${e.message}")
                Result.success(getDefaultOpenRouterModels())
            }
        }
    }
    
    private fun getDefaultOpenRouterModels(): List<LLMModel> {
        return listOf(
            // Top recommended models for uncensored roleplay per documentation
            LLMModel("nousresearch/hermes-3-llama-3.1-405b", "Hermes 3 Llama 3.1 405B", "Open Router", "High-capacity model with strong creative output"),
            LLMModel("wizardlm/wizardlm-2-8x22b", "WizardLM 2 8x22B", "Open Router", "Popular for unmoderated responses and cost-effectiveness"),
            LLMModel("meta-llama/llama-3-70b-instruct", "Llama 3 70B Instruct", "Open Router", "Balanced model with strong conversational abilities"),
            LLMModel("mistral/unslopnemo-12b", "Mistral Unslop Nemo 12B", "Open Router", "Fully uncensored, compact model"),
            LLMModel("austism/chronos-hermes-13b", "Chronos Hermes 13B", "Open Router", "Known for creative and unfiltered responses"),
            // Fallback compatible models
            LLMModel("openai/gpt-3.5-turbo", "GPT-3.5 Turbo", "Open Router", "Most widely available model"),
            LLMModel("meta-llama/llama-3.1-8b-instruct:free", "Llama 3.1 8B (Free)", "Open Router", "Free model"),
            LLMModel("google/gemma-7b-it:free", "Gemma 7B IT (Free)", "Open Router", "Google's free model")
        )
    }
    
    private suspend fun fetchHuggingFaceModels(apiKey: String): Result<List<LLMModel>> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Hugging Face API key is required"))
        }
        
        // HuggingFace Inference API doesn't have a simple models list endpoint
        // Return popular chat models that work with their API
        return Result.success(listOf(
            LLMModel("microsoft/DialoGPT-large", "DialoGPT Large", "Hugging Face", "Conversational AI model"),
            LLMModel("facebook/blenderbot-400M-distill", "BlenderBot 400M", "Hugging Face", "Facebook's chatbot model"),
            LLMModel("microsoft/DialoGPT-medium", "DialoGPT Medium", "Hugging Face", "Medium-sized conversational model"),
            LLMModel("HuggingFaceH4/zephyr-7b-beta", "Zephyr 7B Beta", "Hugging Face", "Fine-tuned chat model"),
            LLMModel("mistralai/Mistral-7B-Instruct-v0.1", "Mistral 7B Instruct", "Hugging Face", "Instruction-following model")
        ))
    }
    
    private suspend fun fetchOllamaModels(endpoint: String): Result<List<LLMModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Try to fetch models from Ollama API
                val sanitizedEndpoint = endpoint.trim().removeSuffix("/")
                val httpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
                    .url("$sanitizedEndpoint/api/tags")
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val json = org.json.JSONObject(responseBody)
                    val modelsArray = json.getJSONArray("models")
                    
                    val models = mutableListOf<LLMModel>()
                    for (i in 0 until modelsArray.length()) {
                        val modelObj = modelsArray.getJSONObject(i)
                        val name = modelObj.getString("name")
                        models.add(LLMModel(name, name, "Ollama", "Local Ollama model"))
                    }
                    
                    if (models.isEmpty() && sanitizedEndpoint.endsWith(":11435")) {
                        // The request succeeded but returned no models—try the default port once
                        return@withContext fetchOllamaModels(sanitizedEndpoint.replace(":11435", ":11434"))
                    }
                    // If we have models, use them; otherwise fall back to defaults
                    return@withContext if (models.isNotEmpty()) Result.success(models) else Result.success(getDefaultOllamaModels())
                } else {
                    // If the server responded but with an error code, retry on 11434 when the user entered 11435
                    if (sanitizedEndpoint.endsWith(":11435")) {
                        return@withContext fetchOllamaModels(sanitizedEndpoint.replace(":11435", ":11434"))
                    }
                    // Return default models if API call fails
                    return@withContext Result.success(getDefaultOllamaModels())
                }
            } catch (e: Exception) {
                // If the first attempt failed and the user is using the non-standard port (11435), try the default 11434 once before falling back.
                if (endpoint.endsWith(":11435")) {
                    return@withContext fetchOllamaModels(endpoint.replace(":11435", ":11434"))
                }
                Log.w(TAG, "Failed to fetch Ollama models, using defaults: ${e.message}")
                return@withContext Result.success(getDefaultOllamaModels())
            }
        }
    }
    
    private fun getDefaultOllamaModels(): List<LLMModel> {
        return listOf(
            LLMModel("llama2", "Llama 2", "Ollama", "Meta's Llama 2 model"),
            LLMModel("llama2:7b", "Llama 2 7B", "Ollama", "7 billion parameter model"),
            LLMModel("llama2:13b", "Llama 2 13B", "Ollama", "13 billion parameter model"),
            LLMModel("codellama", "Code Llama", "Ollama", "Code generation model"),
            LLMModel("mistral", "Mistral 7B", "Ollama", "Mistral AI's 7B model"),
            LLMModel("neural-chat", "Neural Chat", "Ollama", "Intel's neural chat model"),
            LLMModel("dolphin-mixtral", "Dolphin Mixtral", "Ollama", "Uncensored Mixtral model")
        )
    }
    
    private suspend fun fetchKoboldModels(endpoint: String): Result<List<LLMModel>> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("$endpoint/api/v1/model")
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "Kobold AI models endpoint returned ${response.code}")
                return@withContext Result.success(getDefaultModelsForProvider("Kobold AI"))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.success(getDefaultModelsForProvider("Kobold AI"))

            try {
                val json = JSONObject(responseBody)
                val modelName = json.optString("result", "")
                
                val models = if (modelName.isNotBlank()) {
                    listOf(LLMModel(
                        id = modelName,
                        name = modelName,
                        provider = "Kobold AI"
                    ))
                } else {
                    getDefaultModelsForProvider("Kobold AI")
                }
                
                Log.d(TAG, "Kobold AI: Found model: $modelName")
                Result.success(models)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Kobold AI models", e)
                Result.success(getDefaultModelsForProvider("Kobold AI"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Kobold AI models", e)
            Result.success(getDefaultModelsForProvider("Kobold AI"))
        }
    }

    private suspend fun fetchLMStudioModels(endpoint: String): Result<List<LLMModel>> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("$endpoint/v1/models")
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "LMStudio models endpoint returned ${response.code}")
                return@withContext Result.success(getDefaultModelsForProvider("LMStudio"))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.success(getDefaultModelsForProvider("LMStudio"))

            try {
                val json = JSONObject(responseBody)
                val modelsArray = json.optJSONArray("data")
                
                val models = mutableListOf<LLMModel>()
                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val modelObj = modelsArray.getJSONObject(i)
                        val modelId = modelObj.optString("id", "")
                        val modelName = modelObj.optString("name", modelId)
                        if (modelId.isNotBlank()) {
                            models.add(LLMModel(
                                id = modelId,
                                name = modelName,
                                provider = "LMStudio"
                            ))
                        }
                    }
                }
                
                Log.d(TAG, "LMStudio: Found ${models.size} models")
                Result.success(models.ifEmpty { getDefaultModelsForProvider("LMStudio") })
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing LMStudio models", e)
                Result.success(getDefaultModelsForProvider("LMStudio"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching LMStudio models", e)
            Result.success(getDefaultModelsForProvider("LMStudio"))
        }
    }
    
    private suspend fun fetchCustomAPIModels(): Result<List<LLMModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val preferences = dataStore.data.first()
                val apiKey = preferences[CUSTOM_LLM_API_KEY] ?: ""
                val endpoint = preferences[CUSTOM_LLM_ENDPOINT_KEY] ?: ""
                val apiPrefix = preferences[CUSTOM_LLM_API_PREFIX_KEY] ?: "/v1"
                
                // Get manually added Custom API models
                val manuallyAddedModels = preferences[stringPreferencesKey("manually_added_custom_llm_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                
                val allModels = mutableListOf<LLMModel>()
                
                // Add manually added models first
                manuallyAddedModels.forEach { modelId ->
                    allModels.add(LLMModel(
                        id = modelId,
                        name = modelId,
                        provider = "Custom API",
                        description = "Custom API model (manually added)"
                    ))
                }
                
                if (apiKey.isBlank() || endpoint.isBlank()) {
                    // If no API key/endpoint, return only manually added models or default
                    return@withContext if (allModels.isNotEmpty()) {
                        Result.success(allModels)
                    } else {
                        Result.success(listOf(
                            LLMModel("custom-model", "Custom Model", "Custom API", "User-defined custom model")
                        ))
                    }
                }
                
                val customProvider = CustomAPIProvider().apply {
                    setApiKey(apiKey)
                    setEndpoint(endpoint)
                    setApiPrefix(apiPrefix)
                }
                
                val modelsResult = customProvider.fetchModels()
                if (modelsResult.isSuccess) {
                    val fetchedModels = modelsResult.getOrNull() ?: emptyList()
                    // Add fetched models (avoid duplicates)
                    fetchedModels.forEach { modelId ->
                        if (!manuallyAddedModels.contains(modelId)) {
                            allModels.add(LLMModel(
                                id = modelId,
                                name = modelId,
                                provider = "Custom API",
                                description = "Custom API model"
                            ))
                        }
                    }
                    Result.success(allModels)
                } else {
                    // Return manually added models if fetching fails
                    Result.success(if (allModels.isNotEmpty()) allModels else listOf(
                        LLMModel("custom-model", "Custom Model", "Custom API", "User-defined custom model")
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Custom API models", e)
                // Return manually added models on error
                val preferences = dataStore.data.first()
                val manuallyAddedModels = preferences[stringPreferencesKey("manually_added_custom_llm_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                val allModels = manuallyAddedModels.map { modelId ->
                    LLMModel(
                        id = modelId,
                        name = modelId,
                        provider = "Custom API",
                        description = "Custom API model (manually added)"
                    )
                }
                Result.success(if (allModels.isNotEmpty()) allModels else listOf(
                    LLMModel("custom-model", "Custom Model", "Custom API", "User-defined custom model")
                ))
            }
        }
    }

    private suspend fun fetchModelsLabModels(apiKey: String): Result<List<LLMModel>> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("ModelsLab API key is required"))
                }
                
                Log.d(TAG, "🔍 Using hardcoded ModelsLab LLM models...")
                
                // Hardcoded ModelsLab LLM models as requested
                val hardcodedModels = listOf(
                    "Yarn-Mistral-7b-128k",
                    "MistralLite", 
                    "OpenHermes-2.5-Mistral-7B",
                    "dolphin-2.2.1-mistral-7b",
                    "deepseek-ai-DeepSeek-R1-Distill-Llama-70B",
                    "Qwen-Qwen3-235B-A22B-fp8-tput",
                    "meta-llama-Llama-3.3-70B-Instruct-Turbo-Free",
                    "deepseek-ai-DeepSeek-R1-Distill-Llama-70B-free"
                )
                
                // Get manually added models from DataStore
                val manuallyAddedModels = dataStore.data.first()[stringPreferencesKey("manually_added_llm_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                
                // Combine hardcoded and manually added models
                val allModels = hardcodedModels.toMutableList()
                allModels.addAll(manuallyAddedModels)
                
                val llmModels = allModels.map { modelId ->
                    LLMModel(
                        id = modelId,
                        name = modelId,
                        provider = "ModelsLab",
                        description = if (manuallyAddedModels.contains(modelId)) "ModelsLab LLM model (manually added)" else "ModelsLab LLM model"
                    )
                }
                
                Log.d(TAG, "✅ ModelsLab: Returning ${llmModels.size} LLM models (${hardcodedModels.size} hardcoded + ${manuallyAddedModels.size} manual)")
                Result.success(llmModels)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error with hardcoded ModelsLab models: ${e.message}")
                Result.success(getKnownModelsLabModels())
            }
        }
    }
    
    private fun getKnownModelsLabModels(): List<LLMModel> {
        return listOf(
            LLMModel("ModelsLab/Llama-3.1-8b-Uncensored-Dare", "Llama 3.1 8B Uncensored", "ModelsLab", "Uncensored chat model for roleplay and creative writing"),
            LLMModel("ModelsLab/Llama-3.1-8b-Uncensored", "Llama 3.1 8B Uncensored (Alternative)", "ModelsLab", "Alternative uncensored chat model"),
            LLMModel("ModelsLab/Llama-3.1-8b-Chat", "Llama 3.1 8B Chat", "ModelsLab", "Standard chat model"),
            LLMModel("ModelsLab/Llama-3.1-8b-Instruct", "Llama 3.1 8B Instruct", "ModelsLab", "Instruction-following model")
        )
    }
    
    private suspend fun fetchGrokModels(apiKey: String): Result<List<LLMModel>> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Grok API key is required"))
        }
        
        // Grok doesn't have a public models endpoint, return known models
        return Result.success(listOf(
            LLMModel("grok-4", "Grok 4", "Grok", "xAI's most advanced reasoning model"),
            LLMModel("grok-3", "Grok 3", "Grok", "xAI's powerful language model"),
            LLMModel("grok-3-mini", "Grok 3 Mini", "Grok", "Compact version of Grok 3")
        ))
    }
    
    private fun getDefaultModelsLabModels(): List<LLMModel> {
        return listOf(
            LLMModel("ModelsLab/Llama-3.1-8b-Uncensored-Dare", "Llama 3.1 8B Uncensored", "ModelsLab", "Uncensored chat model"),
            LLMModel("default", "Default Model", "ModelsLab", "ModelsLab's default chat model")
        )
    }

    /**
     * Process and truncate response based on selected length style
     */
    private fun processAndTruncateResponse(response: String, responseLengthStyle: String): String {
        var processedResponse = response.trim()
            .removePrefix("Assistant:")
            .removePrefix("AI:")
            .trim()
        
        // First, preserve newlines by converting escaped ones
        processedResponse = processedResponse
            .replace("\\n", "\n") // Convert escaped newlines to actual newlines
            .replace("\\r\\n", "\n") // Convert Windows line endings
            .replace("\\r", "\n") // Convert Mac line endings
        
        // Clean up formatting artifacts while preserving natural conversational flow
        processedResponse = processedResponse
            .replace(Regex("[ \t]+"), " ") // Replace multiple spaces/tabs with single space, but preserve newlines
            .replace(Regex("\n{3,}"), "\n\n") // Clean up excessive line breaks
            .trim()
        
        // Apply length-specific processing
        when (responseLengthStyle) {
            "short" -> {
                // For short responses: 1-2 sentences max, very conversational
                val sentences = processedResponse.split(Regex("[.!?]")).filter { it.trim().isNotEmpty() }
                val result = sentences.take(2).joinToString(". ") { it.trim() }
                return if (result.isBlank() || result.trim() == ".") {
                    processedResponse.take(500).trim()
                } else if (result.length > 500) {
                    // Find the last complete sentence within 500 characters
                    val lastPunctuation = result.take(500).lastIndexOfAny(charArrayOf('.', '!', '?'))
                    if (lastPunctuation > 300) {
                        result.take(lastPunctuation + 1)
                    } else {
                        result.take(500)
                    }
                } else {
                    result + if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) "." else ""
                }
            }
            
            "natural" -> {
                // For natural responses: 2-4 sentences, balanced conversation
                val sentences = processedResponse.split(Regex("[.!?]")).filter { it.trim().isNotEmpty() }
                val result = sentences.take(6).joinToString(". ") { it.trim() }
                return if (result.isBlank() || result.trim() == ".") {
                    processedResponse.take(1500).trim()
                } else if (result.length > 1500) {
                    // Find the last complete sentence within 1500 characters
                    val lastPunctuation = result.take(1500).lastIndexOfAny(charArrayOf('.', '!', '?'))
                    if (lastPunctuation > 1000) {
                        result.take(lastPunctuation + 1)
                    } else {
                        result.take(1500)
                    }
                } else {
                    result + if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) "." else ""
                }
            }
            
            "long" -> {
                // For long responses: Allow full paragraph, minimal truncation
                return if (processedResponse.length > 3000) {
                    // Find the last complete sentence within 3000 characters
                    val lastPunctuation = processedResponse.take(3000).lastIndexOfAny(charArrayOf('.', '!', '?'))
                    if (lastPunctuation > 2000) {
                        processedResponse.take(lastPunctuation + 1)
                    } else {
                        processedResponse.take(3000)
                    }
                } else {
                    processedResponse + if (!processedResponse.endsWith(".") && !processedResponse.endsWith("!") && !processedResponse.endsWith("?")) "." else ""
                }
            }
            
            "unlimited" -> {
                // For unlimited responses: No artificial truncation, let the LLM decide
                return processedResponse + if (!processedResponse.endsWith(".") && !processedResponse.endsWith("!") && !processedResponse.endsWith("?")) "." else ""
            }
            
            else -> {
                // Default to natural processing - avoid recursion
                val sentences = processedResponse.split(Regex("[.!?]")).filter { it.trim().isNotEmpty() }
                val result = sentences.take(6).joinToString(". ") { it.trim() }
                return if (result.isBlank() || result.trim() == ".") {
                    processedResponse.take(1500).trim()
                } else if (result.length > 1500) {
                    // Find the last complete sentence within 1500 characters
                    val lastPunctuation = result.take(1500).lastIndexOfAny(charArrayOf('.', '!', '?'))
                    if (lastPunctuation > 1000) {
                        result.take(lastPunctuation + 1)
                    } else {
                        result.take(1500)
                    }
                } else {
                    result + if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) "." else ""
                }
            }
        }
    }

    /**
     * Format response for conversational roleplay chat
     */
    private fun formatResponseForRoleplay(response: String, character: Character?, responseLengthStyle: String): String {
        var processedResponse = processAndTruncateResponse(response, responseLengthStyle)
        
        // Preserve natural chat formatting - don't strip asterisks/underscores used for actions/emphasis
        // Only clean up excessive or broken formatting
        processedResponse = processedResponse
            .replace(Regex("\\*{3,}"), "**") // Reduce excessive asterisks
            .replace(Regex("_{3,}"), "__") // Reduce excessive underscores
            .trim()
        
        // Apply clean formatting
        processedResponse = formatWithRoleplayStyle(processedResponse, character)
        
        return processedResponse
    }
    
    /**
     * Apply clean text formatting for conversational chat
     */
    private fun formatWithRoleplayStyle(text: String, character: Character?): String {
        // Preserve original formatting (line breaks, paragraphs) for natural conversation
        return text.trim()
            .replace("\\n", "\n") // Convert escaped newlines to actual newlines
            .replace("\\r\\n", "\n") // Convert Windows line endings
            .replace("\\r", "\n") // Convert Mac line endings
            .replace("\n{3,}".toRegex(), "\n\n") // Clean up excessive line breaks
    }
    


    /**
     * Get appropriate token limit based on character needs and user preferences
     */
    private fun getTokenLimitForCharacter(
        characterId: String, 
        responseLengthStyle: String, 
        maxTokens: Int, 
        customMaxTokens: Int, 
        preferences: Preferences
    ): Int {
        // Check for character-specific token settings
        val characterSpecificTokens = preferences[CHARACTER_SPECIFIC_TOKENS_KEY]
        if (!characterSpecificTokens.isNullOrEmpty()) {
            try {
                // Parse JSON to get character-specific settings (simplified implementation)
                if (characterSpecificTokens.contains(characterId)) {
                    // Extract token limit for this character (simplified parsing)
                    val regex = "\"$characterId\":(\\d+)".toRegex()
                    val match = regex.find(characterSpecificTokens)
                    match?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing character-specific token settings: ${e.message}")
            }
        }
        
        // Use custom max tokens if set
        if (customMaxTokens > 0) {
            return customMaxTokens
        }
        
        // Return token limit based on response length style
        return when (responseLengthStyle) {
            "short" -> minOf(maxTokens, 300) // ~200 words, 2-3 sentences
            "natural" -> minOf(maxTokens, 1000) // ~700 words, 3-5 sentences
            "long" -> minOf(maxTokens, 3000) // ~2000 words, detailed response
            "unlimited" -> maxTokens // Use full configured limit for maximum length
            "custom" -> maxTokens // Use full configured limit
            else -> minOf(maxTokens, 1000) // Default to natural
        }
    }

    /**
     * Lightweight endpoint connectivity probe that the UI can call before attempting model fetches.
     */
    suspend fun isEndpointReachable(endpoint: String, probePath: String = "/"): Boolean {
        return EndpointChecker.isReachable(endpoint, probePath)
    }
    
    /**
     * Get comprehensive LLM provider status and configuration info
     */
    suspend fun getLLMProviderStatus(): String {
        return try {
            val preferences = dataStore.data.first()
            val provider = preferences[LLM_PROVIDER_KEY] ?: "Together AI"
            val model = preferences[LLM_MODEL_KEY] ?: "Not selected"
            val temperature = preferences[RESPONSE_TEMPERATURE_KEY] ?: 0.7f
            val maxTokens = preferences[MAX_TOKENS_KEY] ?: 4096
            
            val apiKey = when (provider) {
                "Together AI" -> preferences[TOGETHER_AI_API_KEY]
                "Gemini API" -> preferences[GEMINI_API_KEY]
                "Open Router" -> preferences[OPENROUTER_API_KEY]
                "Hugging Face" -> preferences[HUGGINGFACE_API_KEY]
                "Ollama" -> "Local (no key required)"
                "Kobold AI" -> "Local (no key required)"
                "LMStudio" -> "Local (no key required)"
                "Custom API" -> preferences[CUSTOM_LLM_API_KEY]
                "ModelsLab" -> preferences[MODELSLAB_API_KEY]
                else -> "Unknown"
            } ?: "Not configured"
            
            buildString {
                appendLine("=== LLM Provider Status ===")
                appendLine("Provider: $provider")
                appendLine("Model: $model")
                appendLine("API Key: ${if (apiKey == "Local (no key required)" || apiKey == "Not configured") apiKey else "${apiKey.take(8)}..."}") 
                appendLine("Temperature: $temperature")
                appendLine("Max Tokens: $maxTokens")
                appendLine("Status: ${if (apiKey != "Not configured") "✅ Configured" else "❌ Missing API Key"}")
            }
        } catch (e: Exception) {
            "Error getting provider status: ${e.message}"
        }
    }

    /**
     * Validate current LLM configuration
     */
    suspend fun validateLLMConfiguration(): Result<String> {
        return try {
            val preferences = dataStore.data.first()
            val provider = preferences[LLM_PROVIDER_KEY] ?: return Result.failure(Exception("No LLM provider selected"))
            val model = preferences[LLM_MODEL_KEY] ?: return Result.failure(Exception("No model selected"))
            
            val apiKey = when (provider) {
                "Together AI" -> preferences[TOGETHER_AI_API_KEY]
                "Gemini API" -> preferences[GEMINI_API_KEY]
                "Open Router" -> preferences[OPENROUTER_API_KEY]
                "Hugging Face" -> preferences[HUGGINGFACE_API_KEY]
                "Custom API" -> preferences[CUSTOM_LLM_API_KEY]
                "ModelsLab" -> preferences[MODELSLAB_API_KEY]
                "Grok" -> preferences[GROK_API_KEY]
                "Ollama", "Kobold AI", "LMStudio" -> "local" // Local providers don't need API keys
                else -> null
            }
            
            val requiresApiKey = provider !in listOf("Ollama", "Kobold AI", "LMStudio")
            
            if (requiresApiKey && apiKey.isNullOrBlank()) {
                return Result.failure(Exception("API key required for $provider but not configured"))
            }
            
            Result.success("✅ LLM configuration is valid")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get default models for LLM providers
     */
    private fun getDefaultModelsForProvider(provider: String): List<LLMModel> {
        return when (provider) {
            "Together AI" -> listOf(
                LLMModel("meta-llama/Llama-2-7b-chat-hf", "Llama 2 7B Chat", "Together AI", "Fast and efficient chat model"),
                LLMModel("meta-llama/Llama-2-13b-chat-hf", "Llama 2 13B Chat", "Together AI", "Balanced performance and quality"),
                LLMModel("meta-llama/Llama-2-70b-chat-hf", "Llama 2 70B Chat", "Together AI", "High-quality responses"),
                LLMModel("togethercomputer/llama-2-7b-chat", "Llama 2 7B Chat (Together)", "Together AI", "Optimized for Together AI")
            )
            "Gemini API" -> listOf(
                LLMModel("gemini-1.5-pro", "Gemini 1.5 Pro", "Gemini API", "Google's most capable model"),
                LLMModel("gemini-1.5-flash", "Gemini 1.5 Flash", "Gemini API", "Fast and efficient model"),
                LLMModel("gemini-pro", "Gemini Pro", "Gemini API", "Google's advanced AI model"),
                LLMModel("gemini-pro-vision", "Gemini Pro Vision", "Gemini API", "Multimodal model with vision")
            )
            "Open Router" -> listOf(
                LLMModel("meta-llama/llama-2-7b-chat", "Llama 2 7B Chat", "Open Router", "Fast and efficient"),
                LLMModel("meta-llama/llama-2-13b-chat", "Llama 2 13B Chat", "Open Router", "Balanced performance"),
                LLMModel("anthropic/claude-3-haiku", "Claude 3 Haiku", "Open Router", "Fast and helpful")
            )
            "Hugging Face" -> listOf(
                LLMModel("meta-llama/Llama-2-7b-chat-hf", "Llama 2 7B Chat", "Hugging Face", "Open source chat model"),
                LLMModel("microsoft/DialoGPT-medium", "DialoGPT Medium", "Hugging Face", "Conversational AI model")
            )
            "Ollama" -> listOf(
                LLMModel("llama2", "Llama 2", "Ollama", "Meta's Llama 2 model"),
                LLMModel("llama2:7b", "Llama 2 7B", "Ollama", "7 billion parameter model"),
                LLMModel("llama2:13b", "Llama 2 13B", "Ollama", "13 billion parameter model"),
                LLMModel("codellama", "Code Llama", "Ollama", "Code generation model"),
                LLMModel("mistral", "Mistral 7B", "Ollama", "Mistral AI's 7B model"),
                LLMModel("neural-chat", "Neural Chat", "Ollama", "Intel's neural chat model"),
                LLMModel("dolphin-mixtral", "Dolphin Mixtral", "Ollama", "Uncensored Mixtral model")
            )
            "Kobold AI" -> listOf(
                LLMModel("kobold-model", "Kobold Model", "Kobold AI", "Currently loaded Kobold model"),
                LLMModel("pygmalion-6b", "Pygmalion 6B", "Kobold AI", "Character roleplay model"),
                LLMModel("pygmalion-7b", "Pygmalion 7B", "Kobold AI", "Enhanced character model")
            )
            "LMStudio" -> listOf(
                LLMModel("default", "Default Model", "LMStudio", "Currently loaded model in LMStudio"),
                LLMModel("llama-2-7b-chat", "Llama 2 7B Chat", "LMStudio", "Llama 2 7B chat model"),
                LLMModel("llama-2-13b-chat", "Llama 2 13B Chat", "LMStudio", "Llama 2 13B chat model"),
                LLMModel("mistral-7b-instruct", "Mistral 7B Instruct", "LMStudio", "Mistral 7B instruction model"),
                LLMModel("codellama-7b-instruct", "Code Llama 7B Instruct", "LMStudio", "Code generation model")
            )
            "ModelsLab" -> getDefaultModelsLabModels()
            "Grok" -> listOf(
                LLMModel("grok-4", "Grok 4", "Grok", "xAI's most advanced reasoning model"),
                LLMModel("grok-3", "Grok 3", "Grok", "xAI's powerful language model"),
                LLMModel("grok-3-mini", "Grok 3 Mini", "Grok", "Compact version of Grok 3")
            )
            "Custom API" -> listOf(
                LLMModel("custom-model", "Custom Model", "Custom API", "User-defined custom model")
            )
            else -> listOf(
                LLMModel("default", "Default Model", provider, "Default model for $provider")
            )
        }
    }
    
    /**
     * Get response length recommendations based on character and context
     */
    fun getResponseLengthRecommendation(character: Character?, conversationLength: Int): String {
        return when {
            character?.personality?.contains("verbose", ignoreCase = true) == true -> "long"
            character?.personality?.contains("concise", ignoreCase = true) == true -> "short"
            character?.personality?.contains("shy", ignoreCase = true) == true -> "short"
            character?.personality?.contains("talkative", ignoreCase = true) == true -> "long"
            conversationLength < 5 -> "natural"
            conversationLength > 20 -> "short"
            else -> "natural"
        }
    }
    
    /**
     * Get character-specific generation parameters
     */
    suspend fun getCharacterSpecificParams(character: Character?): GenerationParams {
        val preferences = dataStore.data.first()
        val baseTemperature = preferences[RESPONSE_TEMPERATURE_KEY] ?: 0.7f
        val baseMaxTokens = preferences[MAX_TOKENS_KEY] ?: 4096
        val baseTopP = preferences[TOP_P_KEY] ?: 0.9f
        val baseFrequencyPenalty = preferences[FREQUENCY_PENALTY_KEY] ?: 0.0f
        
        val adjustedTemperature = character?.personality?.let { personality ->
            when {
                personality.contains("creative", ignoreCase = true) -> (baseTemperature + 0.1f).coerceAtMost(1.0f)
                personality.contains("logical", ignoreCase = true) -> (baseTemperature - 0.1f).coerceAtLeast(0.1f)
                personality.contains("unpredictable", ignoreCase = true) -> (baseTemperature + 0.2f).coerceAtMost(1.0f)
                personality.contains("consistent", ignoreCase = true) -> (baseTemperature - 0.1f).coerceAtLeast(0.1f)
                else -> baseTemperature
            }
        } ?: baseTemperature
        
        val adjustedMaxTokens = character?.personality?.let { personality ->
            when {
                personality.contains("verbose", ignoreCase = true) -> (baseMaxTokens * 1.5f).toInt()
                personality.contains("concise", ignoreCase = true) -> (baseMaxTokens * 0.7f).toInt()
                else -> baseMaxTokens
            }
        } ?: baseMaxTokens
        
        return GenerationParams(
            temperature = adjustedTemperature,
            topP = baseTopP,
            maxTokens = adjustedMaxTokens,
            frequencyPenalty = baseFrequencyPenalty
        )
    }
} 
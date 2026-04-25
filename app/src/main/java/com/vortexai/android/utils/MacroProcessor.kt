package com.vortexai.android.utils

import com.vortexai.android.data.models.Character
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for processing SillyTavern macros in AI responses
 */
@Singleton
class MacroProcessor @Inject constructor() {
    
    companion object {
        private const val TAG = "MacroProcessor"
        
        // Common SillyTavern macros
        private const val USER_MACRO = "{{user}}"
        private const val CHAR_MACRO = "{{char}}"
        private const val BOT_MACRO = "{{bot}}"
        private const val USER_PERSONA_MACRO = "{{user_persona}}"
        private const val CHAR_PERSONA_MACRO = "{{char_persona}}"
        private const val TIME_MACRO = "{{time}}"
        private const val DATE_MACRO = "{{date}}"
        private const val LOCATION_MACRO = "{{location}}"
        private const val MEMORY_MACRO = "{{memory}}"
        private const val INPUT_MACRO = "{{input}}"
        private const val CONTEXT_MACRO = "{{context}}"
        private const val ID_MACRO = "{{id}}"
        private const val PREV_MACRO = "{{prev}}"
        private const val DIALOGUE_MACRO = "{{dialogue}}"
        
        // Date/time formatters
        private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        private val dateFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    }
    
    /**
     * Process all macros in a text string
     */
    fun processMacros(
        text: String,
        character: Character? = null,
        userName: String = "User",
        userPersona: String? = null,
        location: String? = null,
        lastUserInput: String? = null,
        previousMessage: String? = null,
        memory: String? = null,
        context: String? = null
    ): String {
        var processedText = text
        
        // Character-related macros
        character?.let { char ->
            processedText = processedText.replace(CHAR_MACRO, char.name, ignoreCase = true)
            processedText = processedText.replace(BOT_MACRO, char.name, ignoreCase = true)
            
            // Character persona/description
            val charPersona = char.description ?: char.shortDescription ?: char.personality ?: ""
            processedText = processedText.replace(CHAR_PERSONA_MACRO, charPersona, ignoreCase = true)
            
            // Character ID
            processedText = processedText.replace(ID_MACRO, char.id, ignoreCase = true)
        }
        
        // User-related macros
        processedText = processedText.replace(USER_MACRO, userName, ignoreCase = true)
        
        userPersona?.let { persona ->
            processedText = processedText.replace(USER_PERSONA_MACRO, persona, ignoreCase = true)
        }
        
        // Time and date macros
        val currentTime = Date()
        processedText = processedText.replace(TIME_MACRO, timeFormatter.format(currentTime), ignoreCase = true)
        processedText = processedText.replace(DATE_MACRO, dateFormatter.format(currentTime), ignoreCase = true)
        
        // Location macro
        location?.let { loc ->
            processedText = processedText.replace(LOCATION_MACRO, loc, ignoreCase = true)
        }
        
        // Conversation context macros
        lastUserInput?.let { input ->
            processedText = processedText.replace(INPUT_MACRO, input, ignoreCase = true)
        }
        
        previousMessage?.let { prev ->
            processedText = processedText.replace(PREV_MACRO, prev, ignoreCase = true)
        }
        
        memory?.let { mem ->
            processedText = processedText.replace(MEMORY_MACRO, mem, ignoreCase = true)
        }
        
        context?.let { ctx ->
            processedText = processedText.replace(CONTEXT_MACRO, ctx, ignoreCase = true)
        }
        
        // Dialogue macro (placeholder - could be conversation summary)
        processedText = processedText.replace(DIALOGUE_MACRO, "the ongoing conversation", ignoreCase = true)
        
        // Process random macro (simple implementation)
        processedText = processRandomMacros(processedText)
        
        return processedText
    }
    
    /**
     * Process character greeting with macros
     */
    fun processCharacterGreeting(
        character: Character,
        userName: String = "User",
        userPersona: String? = null,
        location: String? = null
    ): String {
        val greeting = character.greeting ?: "Hello! I'm ${character.name}. How can I help you today?"
        
        return processMacros(
            text = greeting,
            character = character,
            userName = userName,
            userPersona = userPersona,
            location = location
        )
    }
    
    /**
     * Process AI response with full context
     */
    fun processAIResponse(
        response: String,
        character: Character? = null,
        userName: String = "User",
        userPersona: String? = null,
        location: String? = null,
        lastUserInput: String? = null,
        previousMessage: String? = null,
        conversationMemory: String? = null,
        context: String? = null
    ): String {
        return processMacros(
            text = response,
            character = character,
            userName = userName,
            userPersona = userPersona,
            location = location,
            lastUserInput = lastUserInput,
            previousMessage = previousMessage,
            memory = conversationMemory,
            context = context
        )
    }
    
    /**
     * Process random macros like {{random: option1, option2, option3}}
     */
    private fun processRandomMacros(text: String): String {
        val randomPattern = Regex("\\{\\{random:\\s*([^}]+)\\}\\}", RegexOption.IGNORE_CASE)
        return randomPattern.replace(text) { matchResult ->
            val options = matchResult.groupValues[1]
                .split(",")
                .map { it.trim() }
            
            if (options.isNotEmpty()) {
                options.random()
            } else {
                matchResult.value // Return original if no options
            }
        }
    }
    
    /**
     * Extract user name from user persona or input
     */
    fun extractUserName(userPersona: String?): String {
        // Simple extraction - look for "I am [name]" or "My name is [name]"
        userPersona?.let { persona ->
            val namePatterns = listOf(
                Regex("I am ([A-Z][a-z]+)", RegexOption.IGNORE_CASE),
                Regex("My name is ([A-Z][a-z]+)", RegexOption.IGNORE_CASE),
                Regex("Call me ([A-Z][a-z]+)", RegexOption.IGNORE_CASE)
            )
            
            namePatterns.forEach { pattern ->
                pattern.find(persona)?.let { match ->
                    return match.groupValues[1]
                }
            }
        }
        
        return "User" // Default fallback
    }
} 
package com.vortexai.android.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic Stats Manager for tracking XP, Affection, Score, and other stats
 * Implements the system described in Dynamic_Stats_System_Guide.md
 */
@Singleton
class DynamicStatsManager @Inject constructor() {
    
    companion object {
        private const val TAG = "DynamicStatsManager"
    }
    
    // Current stats state
    private val _stats = MutableStateFlow(CharacterStats())
    val stats: StateFlow<CharacterStats> = _stats.asStateFlow()
    
    // Triggers for stat updates
    private val triggers = listOf(
        // Positive interactions
        Trigger(
            pattern = Pattern.compile("\\b(thank|thanks|appreciate|love|like|good|great|wonderful|amazing|beautiful|sweet|kind|gentle|caring|helpful)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    affection = stats.affection + 1,
                    score = stats.score + 2
                )
            }
        ),
        Trigger(
            pattern = Pattern.compile("\\b(give|gift|present|flower|rose|chocolate|hug|kiss|embrace|hold|touch)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    affection = stats.affection + 2,
                    xp = stats.xp + 1,
                    score = stats.score + 3
                )
            }
        ),
        Trigger(
            pattern = Pattern.compile("\\b(compliment|praise|admire|respect|honor|cherish|treasure|value)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    affection = stats.affection + 1,
                    xp = stats.xp + 1,
                    score = stats.score + 2
                )
            }
        ),
        
        // Negative interactions
        Trigger(
            pattern = Pattern.compile("\\b(insult|hate|dislike|bad|terrible|awful|horrible|mean|cruel|rude|disrespect)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    affection = (stats.affection - 2).coerceAtLeast(0),
                    score = stats.score - 3
                )
            }
        ),
        Trigger(
            pattern = Pattern.compile("\\b(fight|argue|yell|shout|angry|mad|furious|rage|attack|hurt|harm)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    affection = (stats.affection - 3).coerceAtLeast(0),
                    xp = stats.xp + 2, // Fighting gives XP but reduces affection
                    score = stats.score - 5
                )
            }
        ),
        
        // Quest/Story progression
        Trigger(
            pattern = Pattern.compile("\\b(quest|mission|task|challenge|adventure|journey|explore|discover|find|solve|complete|finish)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    xp = stats.xp + 5,
                    score = stats.score + 3
                )
            }
        ),
        
        // Emotional support
        Trigger(
            pattern = Pattern.compile("\\b(comfort|console|support|help|assist|protect|defend|save|rescue|heal|cure)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    affection = stats.affection + 2,
                    xp = stats.xp + 2,
                    score = stats.score + 4
                )
            }
        ),
        
        // Learning/Teaching
        Trigger(
            pattern = Pattern.compile("\\b(teach|learn|study|read|write|practice|train|improve|develop|grow|understand|explain)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    xp = stats.xp + 3,
                    score = stats.score + 2
                )
            }
        ),
        
        // Social interactions
        Trigger(
            pattern = Pattern.compile("\\b(talk|chat|conversation|discuss|share|listen|hear|speak|tell|story|joke|laugh|smile)\\b", Pattern.CASE_INSENSITIVE),
            action = { stats ->
                stats.copy(
                    affection = stats.affection + 1,
                    xp = stats.xp + 1,
                    score = stats.score + 1
                )
            }
        )
    )
    
    /**
     * Process a message and update stats based on triggers
     */
    fun processMessage(message: String) {
        val currentStats = _stats.value
        var updatedStats = currentStats
        
        // Apply all matching triggers
        triggers.forEach { trigger ->
            if (trigger.pattern.matcher(message).find()) {
                updatedStats = trigger.action(updatedStats)
                Log.d(TAG, "Trigger matched: ${trigger.pattern.pattern()}")
            }
        }
        
        // Update stats if changed
        if (updatedStats != currentStats) {
            _stats.value = updatedStats
            Log.d(TAG, "Stats updated: $updatedStats")
        }
    }
    
    /**
     * Get formatted stats string for display
     */
    fun getFormattedStats(): String {
        val currentStats = _stats.value
        return "XP: ${currentStats.xp} | Affection: ${currentStats.affection} | Score: ${currentStats.score}"
    }
    
    /**
     * Get level based on XP
     */
    fun getLevel(): Int {
        return (_stats.value.xp / 10) + 1
    }
    
    /**
     * Reset stats for a new conversation
     */
    fun resetStats() {
        _stats.value = CharacterStats()
        Log.d(TAG, "Stats reset")
    }
    
    /**
     * Set initial stats for a character
     */
    fun setInitialStats(initialStats: CharacterStats) {
        _stats.value = initialStats
        Log.d(TAG, "Initial stats set: $initialStats")
    }
    
    /**
     * Get current stats
     */
    fun getCurrentStats(): CharacterStats {
        return _stats.value
    }
}

/**
 * Data class for character stats
 */
data class CharacterStats(
    val xp: Int = 0,
    val affection: Int = 5, // Start with neutral affection
    val score: Int = 0,
    val loyalty: Int = 0,
    val trust: Int = 0,
    val respect: Int = 0
)

/**
 * Data class for triggers
 */
private data class Trigger(
    val pattern: Pattern,
    val action: (CharacterStats) -> CharacterStats
)

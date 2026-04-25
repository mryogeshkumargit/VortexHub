package com.vortexai.android.domain.service

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class PendingImageGeneration(
    val id: String,
    val prompt: String,
    val sourceLocation: ImageGenerationSource,
    val timestamp: Long,
    val provider: String,
    val model: String
)

data class ImageGenerationSource(
    val type: SourceType,
    val conversationId: String? = null,
    val characterId: String? = null
)

enum class SourceType {
    CHAT,
    IMAGE_GENERATION_TAB
}

data class TrackedImageGenerationResult(
    val id: String,
    val success: Boolean,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val error: String? = null,
    val generationTime: Long = 0,
    val model: String? = null,
    val prompt: String = ""
)

@Singleton
class ImageGenerationTracker @Inject constructor() {
    companion object {
        private const val TAG = "ImageGenerationTracker"
    }

    private val pendingGenerations = ConcurrentHashMap<String, PendingImageGeneration>()
    private val completedGenerations = ConcurrentHashMap<String, TrackedImageGenerationResult>()
    
    private val _pendingGenerationsFlow = MutableStateFlow<List<PendingImageGeneration>>(emptyList())
    val pendingGenerationsFlow: StateFlow<List<PendingImageGeneration>> = _pendingGenerationsFlow.asStateFlow()
    
    private val _completedGenerationsFlow = MutableStateFlow<List<TrackedImageGenerationResult>>(emptyList())
    val completedGenerationsFlow: StateFlow<List<TrackedImageGenerationResult>> = _completedGenerationsFlow.asStateFlow()

    /**
     * Register a new image generation request
     */
    fun registerGeneration(
        id: String,
        prompt: String,
        sourceLocation: ImageGenerationSource,
        provider: String,
        model: String
    ) {
        val pending = PendingImageGeneration(
            id = id,
            prompt = prompt,
            sourceLocation = sourceLocation,
            timestamp = System.currentTimeMillis(),
            provider = provider,
            model = model
        )
        
        pendingGenerations[id] = pending
        updatePendingGenerationsFlow()
        
        Log.d(TAG, "Registered image generation: $id for prompt: $prompt")
    }

    /**
     * Mark an image generation as completed
     */
    fun markCompleted(result: TrackedImageGenerationResult) {
        pendingGenerations.remove(result.id)
        completedGenerations[result.id] = result
        updatePendingGenerationsFlow()
        updateCompletedGenerationsFlow()
        
        Log.d(TAG, "Marked image generation as completed: ${result.id}, success: ${result.success}")
    }

    /**
     * Get pending generations for a specific source
     */
    fun getPendingGenerationsForSource(sourceLocation: ImageGenerationSource): List<PendingImageGeneration> {
        return pendingGenerations.values.filter { it.sourceLocation == sourceLocation }
    }

    /**
     * Get completed generations for a specific source
     */
    fun getCompletedGenerationsForSource(sourceLocation: ImageGenerationSource): List<TrackedImageGenerationResult> {
        return completedGenerations.values.filter { 
            val pending = pendingGenerations[it.id]
            pending?.sourceLocation == sourceLocation
        }
    }

    /**
     * Check if there are any pending generations for a source
     */
    fun hasPendingGenerations(sourceLocation: ImageGenerationSource): Boolean {
        return pendingGenerations.values.any { it.sourceLocation == sourceLocation }
    }

    /**
     * Get all pending generations
     */
    fun getAllPendingGenerations(): List<PendingImageGeneration> {
        return pendingGenerations.values.toList()
    }

    /**
     * Clear completed generations (to prevent memory buildup)
     */
    fun clearOldCompletedGenerations(maxAgeMs: Long = 24 * 60 * 60 * 1000) { // 24 hours
        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        val toRemove = completedGenerations.entries.filter { 
            val pending = pendingGenerations[it.key]
            pending?.timestamp ?: 0 < cutoffTime
        }.map { it.key }
        
        toRemove.forEach { completedGenerations.remove(it) }
        updateCompletedGenerationsFlow()
        
        Log.d(TAG, "Cleared ${toRemove.size} old completed generations")
    }

    private fun updatePendingGenerationsFlow() {
        _pendingGenerationsFlow.value = pendingGenerations.values.toList()
    }

    private fun updateCompletedGenerationsFlow() {
        _completedGenerationsFlow.value = completedGenerations.values.toList()
    }
}

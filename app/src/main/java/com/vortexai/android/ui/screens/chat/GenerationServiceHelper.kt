package com.vortexai.android.ui.screens.chat

import android.content.Context
import com.vortexai.android.services.GenerationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerationServiceHelper @Inject constructor() {
    
    fun startAIGeneration(
        context: Context,
        generationId: String,
        conversationId: String,
        characterId: String,
        characterName: String,
        userMessage: String
    ) {
        GenerationService.startAIGeneration(
            context,
            generationId,
            conversationId,
            characterId,
            characterName,
            userMessage
        )
    }
    
    fun startImageGeneration(
        context: Context,
        generationId: String,
        conversationId: String,
        characterId: String,
        prompt: String
    ) {
        GenerationService.startImageGeneration(
            context,
            generationId,
            conversationId,
            characterId,
            prompt
        )
    }
}

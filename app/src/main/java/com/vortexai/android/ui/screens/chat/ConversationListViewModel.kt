package com.vortexai.android.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.models.ConversationResponse
import com.vortexai.android.data.models.Conversation
import com.vortexai.android.data.repository.ChatRepository
import com.vortexai.android.data.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ConversationListViewModel"
    }

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()
    
    private var currentSearchQuery = ""

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Loading conversations from repository...")
                
                chatRepository.getConversations(page = 1, limit = 50, search = if (currentSearchQuery.isNotBlank()) currentSearchQuery else null).collect { result ->
                    result.fold(
                        onSuccess = { conversationListResponse ->
                            Log.d(TAG, "Loaded ${conversationListResponse.conversations.size} conversations")
                            
                            // Convert database Conversation models to UI ConversationResponse models
                            // and fetch character avatars
                            val uiConversations = mutableListOf<ConversationResponse>()
                            
                            for (conversation in conversationListResponse.conversations) {
                                val conversationResponse = convertToConversationResponse(conversation)
                                uiConversations.add(conversationResponse)
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                conversations = uiConversations,
                                error = null
                            )
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to load conversations", exception)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load conversations"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun refreshConversations() {
        loadConversations()
    }
    
    /**
     * Search conversations by query
     */
    fun searchConversations(query: String) {
        currentSearchQuery = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadConversations()
    }
    
    /**
     * Delete a specific conversation
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting conversation: $conversationId")
                chatRepository.deleteConversation(conversationId).collect { result ->
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Conversation deleted successfully")
                            // Reload conversations after deletion
                            loadConversations()
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to delete conversation", exception)
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to delete conversation: ${exception.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting conversation", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error deleting conversation: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear all conversations (for demo cleanup)
     */
    fun clearAllConversations() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Clearing all conversations...")
                chatRepository.clearAllConversations().collect { result ->
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "All conversations cleared successfully")
                            // Reload conversations after clearing
                            loadConversations()
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to clear conversations", exception)
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to clear conversations: ${exception.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing conversations", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error clearing conversations: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Convert database Conversation model to UI ConversationResponse model
     * Now fetches character avatar URL and actual last message
     */
    private suspend fun convertToConversationResponse(conversation: Conversation): ConversationResponse {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        
        // Fetch character avatar URL from character repository
        var characterAvatarUrl: String? = null
        try {
            // Use first() to get the single result instead of collect
            val result = characterRepository.getCharacter(conversation.characterId).first()
            result.fold(
                onSuccess = { character ->
                    characterAvatarUrl = character?.avatarUrl
                },
                onFailure = { 
                    Log.w(TAG, "Failed to fetch character for avatar: ${conversation.characterId}")
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching character avatar", e)
        }
        
        // Fetch actual last message
        val lastMessage = getLastMessagePreview(conversation)
        
        return ConversationResponse(
            id = conversation.id,
            characterId = conversation.characterId,
            characterName = conversation.characterName,
            characterAvatarUrl = characterAvatarUrl,
            userId = conversation.userId ?: "current_user",
            title = conversation.title ?: "Chat with ${conversation.characterName}",
            lastMessage = lastMessage,
            lastMessageAt = dateFormat.format(Date(conversation.lastMessageAt ?: conversation.updatedAt)),
            messageCount = conversation.totalMessages,
            isPinned = conversation.isFavorite, // Using favorite as pinned for now
            createdAt = dateFormat.format(Date(conversation.createdAt)),
            updatedAt = dateFormat.format(Date(conversation.updatedAt))
        )
    }
    
    /**
     * Get a preview of the last message for the conversation
     * Now fetches actual message content from the database
     */
    private suspend fun getLastMessagePreview(conversation: Conversation): String {
        return try {
            // Always ask repository for the actual last message content to avoid stale counts
            val content = chatRepository.getLastMessageContent(conversation.id)
            if (!content.isNullOrBlank()) {
                val preview = content.take(100)
                if (content.length > 100) "$preview..." else preview
            } else {
                "No messages yet"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting last message preview", e)
            when {
                conversation.totalMessages == 0 -> "No messages yet"
                conversation.totalMessages == 1 -> "Conversation started"
                else -> "Chat in progress"
            }
        }
    }
}

data class ConversationListUiState(
    val isLoading: Boolean = false,
    val conversations: List<ConversationResponse> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
) 
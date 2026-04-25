package com.vortexai.android.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.repository.CharacterRepository
import com.vortexai.android.data.repository.ChatRepository
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyPrompt(
    val characterName: String,
    val characterId: String,
    val avatarUrl: String?,
    val prompt: String
)

data class UserStats(
    val totalConversations: Int = 0,
    val totalMessages: Int = 0,
    val favoriteCharacterName: String? = null,
    val totalCharacters: Int = 0
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val recentChats: List<Conversation> = emptyList(),
    val featuredCharacters: List<Character> = emptyList(),
    val popularCharacters: List<Character> = emptyList(),
    val newCharacters: List<Character> = emptyList(),
    val videoAvatarCharacters: List<Character> = emptyList(),
    val categories: List<String> = emptyList(),
    val userStats: UserStats = UserStats(),
    val lastMessagePreviews: Map<String, String> = emptyMap(),
    val characterAvatars: Map<String, String?> = emptyMap(),
    val dailyPrompt: DailyPrompt? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    init {
        loadHomeData()
    }
    
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Load recent conversations
                val recentChats = chatRepository.getRecentConversations(limit = 5)
                
                // Load featured characters  
                val featuredChars = characterRepository.getFeaturedCharactersList(limit = 6)
                
                // Load popular characters
                val popularChars = characterRepository.getPopularCharactersList(limit = 8)
                
                // Load all characters for categories, stats, and filtering
                val allCharacters = try {
                    characterRepository.getRecentCharactersList(limit = 100)
                } catch (e: Exception) { emptyList() }
                
                // Extract distinct categories and tags
                val allCategories = allCharacters
                    .flatMap { (it.categories ?: emptyList()) + (it.tags ?: emptyList()) }
                    .filter { it.isNotBlank() }
                    .map { tag -> 
                        val lower = tag.trim().lowercase()
                        if (lower.startsWith("#")) lower.replaceFirst("#", "# ").replace("  ", " ") else "# $lower"
                    }
                    .distinct()
                    .take(15)
                
                // New characters (most recently added — sort by version or use popular as fallback)
                val newChars = allCharacters
                    .sortedByDescending { it.id } // IDs typically contain timestamps
                    .take(8)
                
                // Characters with video avatars
                val videoChars = allCharacters
                    .filter { !it.avatarVideoUrl.isNullOrBlank() }
                    .take(6)
                
                // Load last message previews for recent chats
                val messagePreviews = mutableMapOf<String, String>()
                val avatarMap = mutableMapOf<String, String?>()
                for (chat in recentChats) {
                    try {
                        val lastMsg = chatRepository.getLastMessageContent(chat.id)
                        if (lastMsg != null) {
                            messagePreviews[chat.id] = lastMsg
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not fetch last message for ${chat.id}")
                    }
                    // Map character avatars
                    val character = allCharacters.find { it.id == chat.characterId }
                    avatarMap[chat.characterId] = character?.avatarUrl
                }
                
                // User stats
                val totalConversations = chatRepository.getTotalConversationCount()
                val totalMessages = chatRepository.getTotalMessageCount()
                val totalCharacters = characterRepository.getTotalCharacterCount()
                val favoriteChar = allCharacters
                    .filter { it.totalMessages > 0 }
                    .maxByOrNull { it.totalMessages }
                
                val stats = UserStats(
                    totalConversations = totalConversations,
                    totalMessages = totalMessages,
                    favoriteCharacterName = favoriteChar?.name,
                    totalCharacters = totalCharacters
                )
                
                // Daily prompt — pick a random character with a greeting
                val promptChar = allCharacters
                    .filter { !it.greeting.isNullOrBlank() }
                    .randomOrNull()
                val dailyPrompt = if (promptChar != null) {
                    val prompts = listOf(
                        "Ask ${promptChar.name} about their day",
                        "Start an adventure with ${promptChar.name}",
                        "Get to know ${promptChar.name} better",
                        "Ask ${promptChar.name} for advice",
                        "Challenge ${promptChar.name} to a debate"
                    )
                    DailyPrompt(
                        characterName = promptChar.name,
                        characterId = promptChar.id,
                        avatarUrl = promptChar.avatarUrl,
                        prompt = prompts.random()
                    )
                } else null
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recentChats = recentChats,
                    featuredCharacters = featuredChars,
                    popularCharacters = popularChars,
                    newCharacters = newChars,
                    videoAvatarCharacters = videoChars,
                    categories = allCategories,
                    userStats = stats,
                    lastMessagePreviews = messagePreviews,
                    characterAvatars = avatarMap,
                    dailyPrompt = dailyPrompt
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load home data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load data: ${e.message}"
                )
            }
        }
    }
    
    fun refreshData() {
        loadHomeData()
    }
    
    /**
     * Delete a character and refresh the home data
     */
    fun deleteCharacter(character: Character) {
        viewModelScope.launch {
            try {
                characterRepository.deleteCharacter(character.id)
                loadHomeData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete character: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

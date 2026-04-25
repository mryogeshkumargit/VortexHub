package com.vortexai.android.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.CharacterListResponse
import com.vortexai.android.data.repository.CharacterRepository
import com.vortexai.android.data.local.CacheInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for character list screen with local caching support
 * Handles full character data including massive lorebook entries
 */
@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val characterRepository: CharacterRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState: StateFlow<CharactersUiState> = _uiState.asStateFlow()
    
    // Cache info
    private val _cacheInfo = MutableStateFlow<CacheInfo?>(null)
    val cacheInfo: StateFlow<CacheInfo?> = _cacheInfo.asStateFlow()
    
    init {
        loadCharacters()
        loadCacheInfo()
        // Clear expired cache on startup
        viewModelScope.launch {
            characterRepository.clearExpiredCache()
        }
    }
    
    /**
     * Load characters with cache-first strategy
     */
    fun loadCharacters(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            characterRepository.getCharacters(forceRefresh = forceRefresh)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                characters = response.characters,
                                error = null,
                                isFromCache = !forceRefresh
                            )
                            loadCacheInfo() // Update cache info
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load characters"
                            )
                        }
                    )
                }
        }
    }
    
    /**
     * Load popular characters
     */
    fun loadPopularCharacters(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPopular = true)
            
            characterRepository.getPopularCharacters(forceRefresh = forceRefresh)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPopular = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _uiState.value = _uiState.value.copy(
                                isLoadingPopular = false,
                                popularCharacters = response.characters,
                                error = null
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoadingPopular = false,
                                error = exception.message ?: "Failed to load popular characters"
                            )
                        }
                    )
                }
        }
    }
    
    /**
     * Get character details with full lorebook data
     */
    fun getCharacterDetails(characterId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            characterRepository.getCharacter(characterId, forceRefresh)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to load character details"
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { character ->
                            // Update the character in the list if it exists
                            val updatedCharacters = _uiState.value.characters.map { existingCharacter ->
                                if (existingCharacter.id == character.id) character else existingCharacter
                            }
                            _uiState.value = _uiState.value.copy(
                                characters = updatedCharacters,
                                selectedCharacter = character,
                                error = null
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                error = exception.message ?: "Failed to load character details"
                            )
                        }
                    )
                }
        }
    }
    
    /**
     * Search characters
     */
    fun searchCharacters(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, searchQuery = query)
            
            characterRepository.getCharacters(search = query.ifEmpty { null })
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Search failed"
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                characters = response.characters,
                                error = null
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Search failed"
                            )
                        }
                    )
                }
        }
    }
    
    /**
     * Preload characters for offline use
     */
    fun preloadCharactersForOffline() {
        viewModelScope.launch {
            try {
                characterRepository.preloadCharacters()
                loadCacheInfo() // Update cache info after preloading
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to preload characters: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear all cached data
     */
    fun clearCache() {
        viewModelScope.launch {
            characterRepository.clearCache()
            loadCacheInfo()
            // Reload characters from API
            loadCharacters(forceRefresh = true)
        }
    }
    
    /**
     * Load cache information
     */
    private fun loadCacheInfo() {
        viewModelScope.launch {
            try {
                // TODO: Implement cache info loading
                _cacheInfo.value = null
            } catch (e: Exception) {
                // Ignore cache info errors
            }
        }
    }
    
    /**
     * Refresh data from server
     */
    fun refresh() {
        loadCharacters(forceRefresh = true)
        loadPopularCharacters(forceRefresh = true)
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Select character
     */
    fun selectCharacter(character: Character) {
        _uiState.value = _uiState.value.copy(selectedCharacter = character)
    }
    
    /**
     * Clear selected character
     */
    fun clearSelectedCharacter() {
        _uiState.value = _uiState.value.copy(selectedCharacter = null)
    }
}

/**
 * UI State for characters screen
 */
data class CharactersUiState(
    val isLoading: Boolean = false,
    val isLoadingPopular: Boolean = false,
    val characters: List<Character> = emptyList(),
    val popularCharacters: List<Character> = emptyList(),
    val selectedCharacter: Character? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val isFromCache: Boolean = false
) 
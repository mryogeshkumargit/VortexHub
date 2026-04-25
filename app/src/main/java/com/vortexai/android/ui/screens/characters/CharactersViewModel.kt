package com.vortexai.android.ui.screens.characters

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for characters screen
 * Handles character list, search, filtering, and favorites
 */
@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val characterRepository: CharacterRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "CharactersViewModel"
    }
    
    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState = _uiState.asStateFlow()
    
    private var currentPage = 1
    private var hasMorePages = true
    private var currentQuery = ""
    private var currentFilter = "All"
    
    /**
     * Load characters with current filters
     */
    fun loadCharacters(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                currentPage = 1
                hasMorePages = true
                _uiState.update { it.copy(characters = emptyList()) }
            }
            
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val searchQuery = if (currentQuery.isNotBlank()) currentQuery else null
                val result = when (currentFilter) {
                    "Favorites" -> characterRepository.getFavoriteCharacters(search = searchQuery)
                    "My Characters" -> characterRepository.getMyCharacters(search = searchQuery)
                    "Popular" -> characterRepository.getPopularCharacters(search = searchQuery)
                    else -> characterRepository.getCharacters(search = searchQuery)
                }
                
                result.collect { response ->
                    response.fold(
                        onSuccess = { characterList ->
                            Log.d(TAG, "Loaded ${characterList.characters.size} characters")
                            
                            val updatedCharacters = if (refresh || currentPage == 1) {
                                characterList.characters
                            } else {
                                _uiState.value.characters + characterList.characters
                            }
                            
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    characters = updatedCharacters,
                                    errorMessage = null,
                                    hasMorePages = characterList.hasMore
                                )
                            }
                            
                            hasMorePages = characterList.hasMore
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to load characters", exception)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = exception.message ?: "Failed to load characters"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading characters", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
    
    /**
     * Load next page of characters
     */
    fun loadMoreCharacters() {
        if (_uiState.value.isLoading || !hasMorePages) return
        
        currentPage++
        loadCharacters()
    }
    
    /**
     * Search characters by query
     */
    fun searchCharacters(query: String) {
        currentQuery = query
        currentPage = 1
        hasMorePages = true
        loadCharacters(refresh = true)
    }
    
    /**
     * Apply filter to character list
     */
    fun applyFilter(filter: String) {
        if (currentFilter == filter) return
        
        currentFilter = filter
        currentPage = 1
        hasMorePages = true
        
        _uiState.update { it.copy(selectedFilter = filter) }
        loadCharacters(refresh = true)
    }
    
    /**
     * Toggle character favorite status
     */
    fun toggleFavorite(characterId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                characterRepository.toggleFavorite(characterId).collect { result ->
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Successfully toggled favorite for character: $characterId")
                            
                            // Update character in local state
                            _uiState.update { state ->
                                state.copy(
                                    characters = state.characters.map { character ->
                                        if (character.id == characterId) {
                                            character.copy(isFavorite = isFavorite)
                                        } else {
                                            character
                                        }
                                    }
                                )
                            }
                            
                            // If we're viewing favorites and unfavoriting, remove from list
                            if (currentFilter == "Favorites" && !isFavorite) {
                                _uiState.update { state ->
                                    state.copy(
                                        characters = state.characters.filter { it.id != characterId }
                                    )
                                }
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to toggle favorite", exception)
                            // Could show a snackbar or toast here
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite", e)
            }
        }
    }
    
    /**
     * Refresh characters list
     */
    fun refresh() {
        loadCharacters(refresh = true)
    }
    
    /**
     * Delete a character
     */
    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            try {
                characterRepository.deleteCharacter(characterId).collect { result ->
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Successfully deleted character: $characterId")
                            
                            // Remove character from local state
                            _uiState.update { state ->
                                state.copy(
                                    characters = state.characters.filter { it.id != characterId }
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to delete character", exception)
                            _uiState.update { 
                                it.copy(errorMessage = "Failed to delete character: ${exception.message}")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting character", e)
                _uiState.update { 
                    it.copy(errorMessage = "Error deleting character: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for characters screen
 */
data class CharactersUiState(
    val isLoading: Boolean = false,
    val characters: List<Character> = emptyList(),
    val errorMessage: String? = null,
    val selectedFilter: String = "All",
    val hasMorePages: Boolean = true,
    val searchQuery: String = ""
) 
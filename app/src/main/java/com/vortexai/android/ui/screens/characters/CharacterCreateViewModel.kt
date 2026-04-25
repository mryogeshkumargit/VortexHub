package com.vortexai.android.ui.screens.characters

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.repository.CharacterRepository
import com.vortexai.android.data.models.Character
import com.vortexai.android.domain.service.VideoGenerationService
import com.vortexai.android.utils.SillyTavernCardParser
import com.vortexai.android.utils.ImageStorageHelper
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Base64

@HiltViewModel
class CharacterCreateViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val cardParser: SillyTavernCardParser,
    private val imageStorageHelper: ImageStorageHelper,
    private val characterBookParser: com.vortexai.android.utils.CharacterBookParser,
    private val videoGenerationService: VideoGenerationService,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterCreateUiState())
    val uiState: StateFlow<CharacterCreateUiState> = _uiState.asStateFlow()

    // Basic Information Updates
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(name = name),
            isFormValid = validateForm(_uiState.value.character.copy(name = name))
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(description = description)
        )
    }

    fun updatePersonality(personality: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(personality = personality)
        )
    }

    fun updateScenario(scenario: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(scenario = scenario)
        )
    }

    fun updateAvatarUrl(avatarUrl: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(avatarUrl = avatarUrl)
        )
    }

    fun updateAvatarBase64(avatarBase64: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(avatarBase64 = avatarBase64)
        )
    }

    // Character Details Updates
    fun updateAge(age: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(age = age)
        )
    }

    fun updateGender(gender: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(gender = gender)
        )
    }

    fun updateOccupation(occupation: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(occupation = occupation)
        )
    }

    fun updateTags(tags: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(tags = tags)
        )
    }

    // Roleplay Settings Updates
    fun updateGreeting(greeting: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(greeting = greeting)
        )
    }

    fun updateExampleDialogue(exampleDialogue: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(exampleDialogue = exampleDialogue)
        )
    }

    fun updateSystemPrompt(systemPrompt: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(systemPrompt = systemPrompt)
        )
    }

    // Advanced Settings Updates
    fun updateCreator(creator: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(creator = creator)
        )
    }

    fun updateCharacterVersion(version: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(characterVersion = version)
        )
    }

    fun updateNsfw(isNsfw: Boolean) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(isNsfw = isNsfw)
        )
    }

    fun updatePublic(isPublic: Boolean) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(isPublic = isPublic)
        )
    }

    fun updateTemperature(temperature: Float) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(temperature = temperature)
        )
    }

    fun updateMaxTokens(maxTokens: Int) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(maxTokens = maxTokens)
        )
    }

    // Additional field updates
    fun updateDisplayName(displayName: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(displayName = displayName)
        )
    }

    fun updateShortDescription(shortDescription: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(shortDescription = shortDescription)
        )
    }

    fun updateLongDescription(longDescription: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(longDescription = longDescription)
        )
    }

    fun updatePersona(persona: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(persona = persona)
        )
    }

    fun updateBackstory(backstory: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(backstory = backstory)
        )
    }

    fun updateAppearance(appearance: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(appearance = appearance)
        )
    }

    fun updateTopP(topP: Float) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(topP = topP)
        )
    }

    fun updateCategories(categories: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(categories = categories)
        )
    }

    fun updateCreatorNotes(creatorNotes: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(creatorNotes = creatorNotes)
        )
    }

    fun updateIsFeatured(isFeatured: Boolean) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(isFeatured = isFeatured)
        )
    }

    fun updateDynamicStatsEnabled(dynamicStatsEnabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(dynamicStatsEnabled = dynamicStatsEnabled)
        )
    }

    fun updatePostHistoryInstructions(postHistoryInstructions: String) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(postHistoryInstructions = postHistoryInstructions)
        )
    }

    fun updateAlternateGreetings(alternateGreetings: List<String>) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(alternateGreetings = alternateGreetings)
        )
    }

    fun addAlternateGreeting(greeting: String) {
        val currentGreetings = _uiState.value.character.alternateGreetings.toMutableList()
        currentGreetings.add(greeting)
        updateAlternateGreetings(currentGreetings)
    }

    fun removeAlternateGreeting(index: Int) {
        val currentGreetings = _uiState.value.character.alternateGreetings.toMutableList()
        if (index in currentGreetings.indices) {
            currentGreetings.removeAt(index)
            updateAlternateGreetings(currentGreetings)
        }
    }

    fun updateCharacterBook(characterBook: CharacterBook?) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(characterBook = characterBook)
        )
    }

    // Character Book Management Functions
    fun updateCharacterBookName(name: String) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null) {
            val updatedBook = currentBook.copy(name = name)
            updateCharacterBook(updatedBook)
        }
    }

    fun updateCharacterBookDescription(description: String) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null) {
            val updatedBook = currentBook.copy(description = description)
            updateCharacterBook(updatedBook)
        }
    }

    fun updateCharacterBookScanDepth(scanDepth: Int) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null) {
            val updatedBook = currentBook.copy(scanDepth = scanDepth)
            updateCharacterBook(updatedBook)
        }
    }

    fun updateCharacterBookTokenBudget(tokenBudget: Int) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null) {
            val updatedBook = currentBook.copy(tokenBudget = tokenBudget)
            updateCharacterBook(updatedBook)
        }
    }

    fun updateCharacterBookRecursiveScanning(recursiveScanning: Boolean) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null) {
            val updatedBook = currentBook.copy(recursiveScanning = recursiveScanning)
            updateCharacterBook(updatedBook)
        }
    }

    fun addLorebookEntry(entry: LorebookEntry) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null) {
            val updatedEntries = currentBook.entries.toMutableList()
            updatedEntries.add(entry)
            val updatedBook = currentBook.copy(entries = updatedEntries)
            updateCharacterBook(updatedBook)
        }
    }

    fun createNewCharacterBook() {
        val newBook = CharacterBook(
            name = "New Character Book",
            description = "Character book for ${_uiState.value.character.name}",
            scanDepth = 50,
            tokenBudget = 500,
            recursiveScanning = false,
            entries = emptyList()
        )
        updateCharacterBook(newBook)
    }

    fun addNewLorebookEntry() {
        val newEntry = LorebookEntry(
            id = "entry_${System.currentTimeMillis()}",
            keys = listOf("keyword1"),
            content = "Enter your lorebook entry content here...",
            enabled = true,
            insertionOrder = 100,
            caseSensitive = false,
            name = "New Entry",
            priority = 100,
            comment = "",
            selective = false,
            secondaryKeys = emptyList(),
            constant = false,
            position = "before_char"
        )
        
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null) {
            addLorebookEntry(newEntry)
        } else {
            // Create a new character book if none exists
            val newBook = CharacterBook(
                name = "Character Book for ${_uiState.value.character.name}",
                description = "Character book for ${_uiState.value.character.name}",
                scanDepth = 50,
                tokenBudget = 500,
                recursiveScanning = false,
                entries = listOf(newEntry)
            )
            updateCharacterBook(newBook)
        }
    }

    fun updateLorebookEntry(index: Int, entry: LorebookEntry) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null && index in currentBook.entries.indices) {
            val updatedEntries = currentBook.entries.toMutableList()
            updatedEntries[index] = entry
            val updatedBook = currentBook.copy(entries = updatedEntries)
            updateCharacterBook(updatedBook)
        }
    }

    fun removeLorebookEntry(index: Int) {
        val currentBook = _uiState.value.character.characterBook
        if (currentBook != null && index in currentBook.entries.indices) {
            val updatedEntries = currentBook.entries.toMutableList()
            updatedEntries.removeAt(index)
            val updatedBook = currentBook.copy(entries = updatedEntries)
            updateCharacterBook(updatedBook)
        }
    }

    fun updateExtensionFields(extensionFields: Map<String, String>) {
        _uiState.value = _uiState.value.copy(
            character = _uiState.value.character.copy(extensionFields = extensionFields)
        )
    }

    /**
     * Handle image selection from gallery/camera
     */
    fun handleImageSelection(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Read the image from URI
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    // Convert to base64
                    val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                    
                    // Update the character state with the base64 image
                    _uiState.value = _uiState.value.copy(
                        character = _uiState.value.character.copy(avatarBase64 = base64Image),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to read image file"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to process image: ${e.message}"
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

    // Character Creation
    fun createCharacter(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Check for duplicate character name first
                val characterName = _uiState.value.character.name.trim()
                val existingCharacter = characterRepository.getCharacterByName(characterName)
                
                if (existingCharacter != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "A character with the name '$characterName' already exists. Please choose a different name or modify the existing character."
                    )
                    return@launch
                }
                
                // Convert CharacterCreateState to Character
                val character = convertToCharacter(_uiState.value.character)
                
                // Handle character image storage if present
                val finalCharacter = if (_uiState.value.character.avatarBase64.isNotBlank()) {
                    try {
                        val localImagePath = imageStorageHelper.saveCharacterImageFromBase64(
                            character.id, 
                            _uiState.value.character.avatarBase64
                        )
                        character.copy(avatarUrl = localImagePath)
                    } catch (e: Exception) {
                        android.util.Log.w("CharacterCreateVM", "Failed to save character image", e)
                        character // Continue without image if image save fails
                    }
                } else {
                    character
                }
                
                // Save character using repository
                val result = characterRepository.saveCharacter(finalCharacter)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(finalCharacter.id)
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = when {
                        exception?.message?.contains("UNIQUE constraint failed") == true -> 
                            "A character with this name already exists. Please choose a different name."
                        exception?.message?.contains("SQLITE") == true -> 
                            "Database error occurred. Please try again."
                        else -> "Failed to save character: ${exception?.message ?: "Unknown error"}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create character: ${e.message}"
                )
            }
        }
    }

    // Import from file
    fun importCharacterCard(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val characterData = cardParser.parseCharacterCard(context, uri)
                
                android.util.Log.d("CharacterCreateVM", "Imported character: ${characterData.name}")
                android.util.Log.d("CharacterCreateVM", "Character book: ${characterData.characterBook != null}")
                android.util.Log.d("CharacterCreateVM", "Character book entries: ${characterData.characterBook?.entries?.size ?: 0}")
                
                _uiState.value = _uiState.value.copy(
                    character = characterData,
                    isLoading = false,
                    isFormValid = validateForm(characterData)
                )
                
            } catch (e: Exception) {
                android.util.Log.e("CharacterCreateVM", "Failed to import character card", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to import character card: ${e.message}"
                )
            }
        }
    }

    // Import from URL
    fun importCharacterFromUrl(url: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val characterData = cardParser.parseCharacterCardFromUrl(url)
                
                android.util.Log.d("CharacterCreateVM", "Imported character from URL: ${characterData.name}")
                android.util.Log.d("CharacterCreateVM", "Character book: ${characterData.characterBook != null}")
                android.util.Log.d("CharacterCreateVM", "Character book entries: ${characterData.characterBook?.entries?.size ?: 0}")
                
                _uiState.value = _uiState.value.copy(
                    character = characterData,
                    isLoading = false,
                    isFormValid = validateForm(characterData)
                )
                
            } catch (e: Exception) {
                android.util.Log.e("CharacterCreateVM", "Failed to import character from URL", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to import character from URL: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Import character book from JSON file
     */
    fun importCharacterBookFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val result = characterBookParser.parseCharacterBook(context, uri)
                
                result.fold(
                    onSuccess = { parsedBook ->
                        // Convert to internal CharacterBook format
                        val entries = parsedBook.entries?.map { (_, entry) ->
                            LorebookEntry(
                                id = "entry_${entry.uid ?: System.currentTimeMillis()}",
                                keys = entry.key,
                                content = entry.content,
                                enabled = entry.enabled,
                                insertionOrder = entry.priority ?: 100,
                                caseSensitive = entry.caseSensitive ?: false,
                                name = entry.name ?: "",
                                priority = entry.priority ?: 100,
                                comment = "",
                                selective = false,
                                secondaryKeys = entry.keysecondary,
                                constant = false,
                                position = "before_char"
                            )
                        } ?: emptyList()
                        
                        val characterBook = CharacterBook(
                            name = parsedBook.name ?: "Imported Character Book",
                            description = parsedBook.description ?: "",
                            scanDepth = parsedBook.scanDepth ?: 100,
                            tokenBudget = parsedBook.tokenBudget ?: 512,
                            recursiveScanning = parsedBook.recursiveScanning ?: false,
                            entries = entries
                        )
                        
                        updateCharacterBook(characterBook)
                        
                        android.util.Log.d("CharacterCreateVM", "Imported character book: ${characterBook.name}, ${entries.size} entries")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    },
                    onFailure = { e ->
                        android.util.Log.e("CharacterCreateVM", "Failed to import character book", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to import character book: ${e.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("CharacterCreateVM", "Error importing character book", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error importing character book: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Generate video face from the current avatar image
     */
    fun generateVideoFace(provider: String, model: String, prompt: String) {
        val currentImage = _uiState.value.character.avatarBase64
        if (currentImage.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please upload an avatar image first before animating it."
            )
            return
        }
        
        viewModelScope.launch {
            try {
                // Keep track that generation started (maybe show a toast in UI or a small loading indicator next to avatar)
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
                
                // Construct request 
                val request = com.vortexai.android.domain.service.VideoGenerationRequest(
                    prompt = prompt,
                    model = model,
                    initImageBase64 = currentImage,
                    durationSeconds = 3,
                    framesPerSecond = 24
                )
                
                // Call the service with empty API key, letting the lower levels resolve it if configured
                val result = videoGenerationService.generateVideo(provider, "", request)
                
                if (result.isSuccess) {
                    val videoResult = result.getOrNull()
                    if (videoResult?.success == true && videoResult.videoUrl != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            character = _uiState.value.character.copy(avatarVideoUrl = videoResult.videoUrl)
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Video generation failed: ${videoResult?.error ?: "Unknown error"}"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Video generation failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CharacterCreateVM", "Error generating video face", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error generating video face: ${e.message}"
                )
            }
        }
    }
    
    // Load character for editing
    fun loadCharacterForEditing(characterId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val character = characterRepository.getCharacterByIdSync(characterId)
                if (character != null) {
                    val characterCreateState = convertToCharacterCreateState(character)
                    _uiState.value = _uiState.value.copy(
                        character = characterCreateState,
                        isLoading = false,
                        isFormValid = validateForm(characterCreateState)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Character not found"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load character: ${e.message}"
                )
            }
        }
    }
    
    // Update existing character
    fun updateCharacter(characterId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Check for duplicate character name (excluding current character)
                val characterName = _uiState.value.character.name.trim()
                val existingCharacter = characterRepository.getCharacterByName(characterName)
                
                if (existingCharacter != null && existingCharacter.id != characterId) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "A character with the name '$characterName' already exists. Please choose a different name."
                    )
                    return@launch
                }
                
                // Convert CharacterCreateState to Character with existing ID
                val character = convertToCharacterForUpdate(_uiState.value.character, characterId)
                
                // Handle character image storage if present
                val finalCharacter = if (_uiState.value.character.avatarBase64.isNotBlank()) {
                    try {
                        val localImagePath = imageStorageHelper.saveCharacterImageFromBase64(
                            characterId, 
                            _uiState.value.character.avatarBase64
                        )
                        character.copy(avatarUrl = localImagePath)
                    } catch (e: Exception) {
                        android.util.Log.w("CharacterCreateVM", "Failed to save character image", e)
                        character // Continue without image if image save fails
                    }
                } else {
                    character
                }
                
                // Update character using repository
                val result = characterRepository.updateCharacter(finalCharacter)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(characterId)
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = when {
                        exception?.message?.contains("UNIQUE constraint failed") == true -> 
                            "A character with this name already exists. Please choose a different name."
                        exception?.message?.contains("SQLITE") == true -> 
                            "Database error occurred. Please try again."
                        else -> "Failed to update character: ${exception?.message ?: "Unknown error"}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update character: ${e.message}"
                )
            }
        }
    }

    private fun validateForm(character: CharacterCreateState): Boolean {
        return character.name.isNotBlank()
    }
    
    private fun convertToCharacter(createState: CharacterCreateState): Character {
        val characterId = "char_${System.currentTimeMillis()}_${(1000..9999).random()}"
        
        return convertToCharacterForUpdate(createState, characterId)
    }
    
    private fun convertToCharacterForUpdate(createState: CharacterCreateState, characterId: String): Character {
        
        // Serialize character book to JSON if present
        val characterBookJson = createState.characterBook?.let { 
            try {
                gson.toJson(it)
            } catch (e: Exception) {
                null
            }
        }
        
        // Prepare tags list
        val tagsList = if (createState.tags.isNotBlank()) {
            createState.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        
        // Prepare categories list
        val categoriesList = if (createState.categories.isNotBlank()) {
            createState.categories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        
        return Character(
            id = characterId,
            name = createState.name,
            displayName = createState.displayName.ifBlank { createState.name },
            shortDescription = createState.shortDescription.ifBlank { createState.description },
            longDescription = createState.longDescription.ifBlank { createState.description },
            persona = createState.persona.ifBlank { createState.personality },
            backstory = createState.backstory.ifBlank { createState.scenario },
            greeting = createState.greeting,
            avatarUrl = createState.avatarUrl.ifBlank { null },
            avatarVideoUrl = createState.avatarVideoUrl.ifBlank { null },
            appearance = createState.appearance.ifBlank { null },
            personality = createState.personality,
            scenario = createState.scenario,
            exampleDialogue = createState.exampleDialogue,
            characterBook = characterBookJson,
            temperature = createState.temperature,
            topP = createState.topP,
            maxTokens = createState.maxTokens,
            nsfwEnabled = createState.isNsfw,
            tags = tagsList,
            categories = categoriesList,
            creatorId = null,
            creator = createState.creator.ifBlank { null },
            creatorNotes = createState.creatorNotes.ifBlank { null },
            characterVersion = createState.characterVersion,
            isPublic = createState.isPublic,
            isFeatured = createState.isFeatured,
            isFavorite = false,
            description = createState.description,
            stats = null,
            version = 1,
            totalMessages = 0,
            totalConversations = 0,
            averageRating = 0.0f,
            totalRatings = 0,
            lastInteraction = null,
            isActive = true,
            dynamicStatsEnabled = createState.dynamicStatsEnabled
        )
    }
    
    private fun convertToCharacterCreateState(character: Character): CharacterCreateState {
        // Parse character book from JSON if present
        val characterBook = character.characterBook?.let { json ->
            try {
                gson.fromJson(json, CharacterBook::class.java)
            } catch (e: Exception) {
                android.util.Log.e("CharacterCreateVM", "Failed to parse character book JSON", e)
                null
            }
        }
        
        android.util.Log.d("CharacterCreateVM", "Converting character: ${character.name}")
        android.util.Log.d("CharacterCreateVM", "Character book JSON: ${character.characterBook}")
        android.util.Log.d("CharacterCreateVM", "Parsed character book: ${characterBook != null}")
        android.util.Log.d("CharacterCreateVM", "Character book entries: ${characterBook?.entries?.size ?: 0}")
        
        return CharacterCreateState(
            // Basic Information
            name = character.name,
            displayName = character.displayName ?: "",
            description = character.description ?: "",
            shortDescription = character.shortDescription ?: "",
            longDescription = character.longDescription ?: "",
            persona = character.persona ?: "",
            backstory = character.backstory ?: "",
            avatarUrl = character.avatarUrl ?: "",
            avatarVideoUrl = character.avatarVideoUrl ?: "",
            avatarBase64 = "", // Not loading base64 for editing
            
            // Enhanced Character Details
            appearance = character.appearance ?: "",
            personality = character.personality ?: "",
            scenario = character.scenario ?: "",
            exampleDialogue = character.exampleDialogue ?: "",
            
            // Generation Settings
            temperature = character.temperature,
            topP = character.topP,
            maxTokens = character.maxTokens,
            
            // Content and Safety
            isNsfw = character.nsfwEnabled,
            tags = character.tags?.joinToString(", ") ?: "",
            categories = character.categories?.joinToString(", ") ?: "",
            
            // Metadata
            creator = character.creator ?: "",
            creatorNotes = character.creatorNotes ?: "",
            characterVersion = character.characterVersion ?: "1.0",
            isPublic = character.isPublic,
            isFeatured = character.isFeatured,
            
            // Roleplay Settings
            greeting = character.greeting ?: "",
            systemPrompt = "", // Not available in Character model
            postHistoryInstructions = "", // Not available in Character model
            alternateGreetings = emptyList(), // Not available in Character model
            
            // Extension fields (age, gender, occupation, etc.)
            age = "", // Not available in Character model
            gender = "", // Not available in Character model
            occupation = "", // Not available in Character model
            extensionFields = emptyMap(), // Not available in Character model
            
            // Character Book/Lorebook
            characterBook = characterBook,
            
            // Dynamic Stats System
            dynamicStatsEnabled = character.dynamicStatsEnabled
        )
    }
}

data class CharacterCreateUiState(
    val character: CharacterCreateState = CharacterCreateState(),
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val errorMessage: String? = null
)

data class CharacterCreateState(
    // Basic Information
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val shortDescription: String = "",
    val longDescription: String = "",
    val persona: String = "",
    val backstory: String = "",
    val avatarUrl: String = "",
    val avatarVideoUrl: String = "",
    val avatarBase64: String = "", // For embedded images from PNG
    
    // Enhanced Character Details
    val appearance: String = "",
    val personality: String = "",
    val scenario: String = "",
    val exampleDialogue: String = "",
    
    // Generation Settings
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 1000,
    
    // Content and Safety
    val isNsfw: Boolean = false,
    val tags: String = "",
    val categories: String = "",
    
    // Metadata
    val creator: String = "",
    val creatorNotes: String = "",
    val characterVersion: String = "1.0",
    val isPublic: Boolean = true,
    val isFeatured: Boolean = false,
    
    // Roleplay Settings
    val greeting: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val alternateGreetings: List<String> = emptyList(),
    
    // Extension fields (age, gender, occupation, etc.)
    val age: String = "",
    val gender: String = "",
    val occupation: String = "",
    val extensionFields: Map<String, String> = emptyMap(),
    
    // Character Book/Lorebook
    val characterBook: CharacterBook? = null,
    
    // Dynamic Stats System
    val dynamicStatsEnabled: Boolean = false
)

data class CharacterBook(
    val name: String = "",
    val description: String = "",
    val scanDepth: Int = 100,
    val tokenBudget: Int = 512,
    val recursiveScanning: Boolean = false,
    val extensions: Map<String, Any> = emptyMap(),
    val entries: List<LorebookEntry> = emptyList()
)

data class LorebookEntry(
    val id: String = "",
    val keys: List<String> = emptyList(),
    val content: String = "",
    val extensions: Map<String, Any> = emptyMap(),
    val enabled: Boolean = true,
    val insertionOrder: Int = 100,
    val caseSensitive: Boolean = false,
    val name: String = "",
    val priority: Int = 100,
    val comment: String = "",
    val selective: Boolean = false,
    val secondaryKeys: List<String> = emptyList(),
    val constant: Boolean = false,
    val position: String = "before_char" // before_char, after_char, top, bottom, before_example, after_example
) 
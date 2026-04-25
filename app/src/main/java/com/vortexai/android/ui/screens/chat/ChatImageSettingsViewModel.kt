package com.vortexai.android.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.model.ChatImageSettings
import com.vortexai.android.data.model.InputImageOption
import com.vortexai.android.data.model.PredictionCreationMethod
import com.vortexai.android.data.repository.ChatImageSettingsRepository
import com.vortexai.android.domain.service.ImageInputService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing chat-specific image settings
 */
@HiltViewModel
class ChatImageSettingsViewModel @Inject constructor(
    private val chatImageSettingsRepository: ChatImageSettingsRepository,
    private val imageInputService: ImageInputService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatImageSettingsUiState())
    val uiState: StateFlow<ChatImageSettingsUiState> = _uiState.asStateFlow()
    
    /**
     * Load chat image settings for a specific chat
     */
    fun loadChatImageSettings(chatId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val settings = chatImageSettingsRepository.getOrCreateChatImageSettings(chatId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    chatId = chatId,
                    inputImageOption = settings.inputImageOption,
                    localImagePath = settings.localImagePath,
                    cloudImageUrl = settings.cloudImageUrl,
                    useCharacterAvatar = settings.useCharacterAvatar,
                    predictionCreationMethod = settings.predictionCreationMethod,
                    manualPredictionInput = settings.manualPredictionInput,
                    selectedImageBase64 = null,
                    imageInfo = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load chat image settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update input image option
     */
    fun updateInputImageOption(option: InputImageOption) {
        updateState { it.copy(inputImageOption = option) }
        saveSettings()
    }
    
    /**
     * Select local image from storage
     */
    fun selectLocalImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Save image to local storage
                val fileName = "chat_${_uiState.value.chatId}_${System.currentTimeMillis()}.jpg"
                val localPath = imageInputService.saveImageToLocalStorage(context, imageUri, fileName)
                
                if (localPath != null) {
                    // Convert to base64 for preview
                    val base64 = imageInputService.convertImageToBase64(localPath)
                    val imageInfo = imageInputService.getImageInfo(localPath)
                    
                    updateState {
                        it.copy(
                            isLoading = false,
                            inputImageOption = InputImageOption.LOCAL_IMAGE,
                            localImagePath = localPath,
                            selectedImageBase64 = base64,
                            imageInfo = imageInfo
                        )
                    }
                    
                    saveSettings()
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Failed to save image to local storage"
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Failed to select local image: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Upload image to cloud storage
     */
    fun uploadImageToCloud(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val cloudUrl = imageInputService.uploadImageToCloud(context, imageUri)
                
                if (cloudUrl != null) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            inputImageOption = InputImageOption.CLOUD_IMAGE,
                            cloudImageUrl = cloudUrl
                        )
                    }
                    
                    saveSettings()
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Failed to upload image to cloud storage"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to upload image to cloud: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update prediction creation method
     */
    fun updatePredictionCreationMethod(method: PredictionCreationMethod) {
        updateState { it.copy(predictionCreationMethod = method) }
        saveSettings()
    }
    
    /**
     * Update manual prediction input
     */
    fun updateManualPredictionInput(input: String) {
        updateState { it.copy(manualPredictionInput = input) }
        saveSettings()
    }
    
    /**
     * Clear selected image
     */
    fun clearSelectedImage() {
        updateState {
            it.copy(
                inputImageOption = InputImageOption.CHARACTER_AVATAR,
                localImagePath = null,
                cloudImageUrl = null,
                selectedImageBase64 = null,
                imageInfo = null
            )
        }
        saveSettings()
    }
    
    /**
     * Get input image for API call
     */
    suspend fun getInputImageForApi(): String? {
        val currentState = _uiState.value
        
        return when (currentState.inputImageOption) {
            InputImageOption.CHARACTER_AVATAR -> {
                // This will be handled by the calling code with character avatar
                null
            }
            InputImageOption.LOCAL_IMAGE -> {
                currentState.localImagePath?.let { path ->
                    imageInputService.convertImageToBase64(path)
                }
            }
            InputImageOption.CLOUD_IMAGE -> {
                currentState.cloudImageUrl
            }
            InputImageOption.MANUAL_BASE64 -> {
                currentState.selectedImageBase64
            }
        }
    }
    
    /**
     * Get prediction text for API call
     */
    fun getPredictionText(userPrompt: String): String {
        val currentState = _uiState.value
        
        return when (currentState.predictionCreationMethod) {
            PredictionCreationMethod.AUTO -> {
                userPrompt
            }
            PredictionCreationMethod.MANUAL -> {
                currentState.manualPredictionInput ?: userPrompt
            }
        }
    }
    
    /**
     * Save current settings to database (public method)
     */
    fun saveChatSettings() {
        saveSettings()
    }
    
    /**
     * Save current settings to database
     */
    private fun saveSettings() {
        val currentState = _uiState.value
        if (currentState.chatId.isBlank()) return
        
        viewModelScope.launch {
            try {
                val settings = ChatImageSettings(
                    chatId = currentState.chatId,
                    inputImageOption = currentState.inputImageOption,
                    localImagePath = currentState.localImagePath,
                    cloudImageUrl = currentState.cloudImageUrl,
                    useCharacterAvatar = currentState.useCharacterAvatar,
                    predictionCreationMethod = currentState.predictionCreationMethod,
                    manualPredictionInput = currentState.manualPredictionInput,
                    updatedAt = System.currentTimeMillis()
                )
                
                chatImageSettingsRepository.saveChatImageSettings(settings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        updateState { it.copy(error = null) }
    }
    
    /**
     * Helper function to centralize state updates
     */
    private fun updateState(block: (ChatImageSettingsUiState) -> ChatImageSettingsUiState) {
        _uiState.value = block(_uiState.value)
    }
}

/**
 * UI state for chat image settings
 */
data class ChatImageSettingsUiState(
    val isLoading: Boolean = false,
    val chatId: String = "",
    val inputImageOption: InputImageOption = InputImageOption.CHARACTER_AVATAR,
    val localImagePath: String? = null,
    val cloudImageUrl: String? = null,
    val useCharacterAvatar: Boolean = true,
    val predictionCreationMethod: PredictionCreationMethod = PredictionCreationMethod.AUTO,
    val manualPredictionInput: String? = null,
    val selectedImageBase64: String? = null,
    val imageInfo: com.vortexai.android.domain.service.ImageInfo? = null,
    val error: String? = null
)

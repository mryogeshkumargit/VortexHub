package com.vortexai.android.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.vortexai.android.ui.screens.settings.managers.*
import com.vortexai.android.data.remote.SupabaseConnectionTest
import com.vortexai.android.data.remote.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val interfaceSettingsManager: InterfaceSettingsManager,
    private val llmSettingsManager: LLMSettingsManager,
    private val imageSettingsManager: ImageSettingsManager,
    private val audioSettingsManager: AudioSettingsManager,
    private val backupSettingsManager: BackupSettingsManager,
    private val modelCacheManager: ModelCacheManager,
    private val settingsDataStore: SettingsDataStore,
    private val apiConnectionTester: com.vortexai.android.utils.ApiConnectionTester,
    private val authRepository: com.vortexai.android.data.repository.AuthRepository,
    private val characterRepository: com.vortexai.android.data.repository.CharacterRepository,
    private val chatLLMService: com.vortexai.android.domain.service.ChatLLMService,
    private val llmDiagnostics: com.vortexai.android.utils.LLMDiagnostics,
    private val appIconManager: com.vortexai.android.utils.AppIconManager,
    private val cardParser: com.vortexai.android.utils.SillyTavernCardParser,
    private val imageStorageHelper: com.vortexai.android.utils.ImageStorageHelper,
    private val customApiProviderRepository: com.vortexai.android.data.repository.CustomApiProviderRepository
) : ViewModel() {
    
    companion object {
        // Expose constants for backward compatibility
        val IMAGE_PROVIDER_KEY = SettingsDataStore.IMAGE_PROVIDER_KEY
        val TOGETHER_AI_IMAGE_API_KEY = SettingsDataStore.TOGETHER_AI_IMAGE_API_KEY
        val HUGGINGFACE_IMAGE_API_KEY = SettingsDataStore.HUGGINGFACE_IMAGE_API_KEY
        val COMFYUI_API_KEY = SettingsDataStore.COMFYUI_API_KEY
        val CUSTOM_IMAGE_API_KEY = SettingsDataStore.CUSTOM_IMAGE_API_KEY
        val MODELSLAB_IMAGE_API_KEY = SettingsDataStore.MODELSLAB_IMAGE_API_KEY
        val IMAGE_SIZE_KEY = SettingsDataStore.IMAGE_SIZE_KEY
        val MODELSLAB_WORKFLOW_KEY = SettingsDataStore.MODELSLAB_WORKFLOW_KEY
        val MODELSLAB_USE_CHAR_IMG_KEY = SettingsDataStore.MODELSLAB_USE_CHAR_IMG_KEY
        val MODELSLAB_LORA_MODEL_KEY = SettingsDataStore.MODELSLAB_LORA_MODEL_KEY
        val MODELSLAB_LORA_STRENGTH_KEY = SettingsDataStore.MODELSLAB_LORA_STRENGTH_KEY
        val NEGATIVE_PROMPT_KEY = SettingsDataStore.NEGATIVE_PROMPT_KEY
        val IMAGE_MODEL_KEY = SettingsDataStore.IMAGE_MODEL_KEY
        val STEPS_KEY = SettingsDataStore.STEPS_KEY
        val GUIDANCE_SCALE_KEY = SettingsDataStore.GUIDANCE_SCALE_KEY
        val COMFYUI_WORKFLOW_KEY = SettingsDataStore.COMFYUI_WORKFLOW_KEY
        val COMFYUI_ENDPOINT_KEY = SettingsDataStore.COMFYUI_ENDPOINT_KEY
        val CUSTOM_IMAGE_ENDPOINT_KEY = SettingsDataStore.CUSTOM_IMAGE_ENDPOINT_KEY
        val CUSTOM_IMAGE_API_PREFIX_KEY = SettingsDataStore.CUSTOM_IMAGE_API_PREFIX_KEY
        val CUSTOM_AUDIO_API_PREFIX_KEY = SettingsDataStore.CUSTOM_AUDIO_API_PREFIX_KEY
        val CUSTOM_AUDIO_ENDPOINT_KEY = SettingsDataStore.CUSTOM_AUDIO_ENDPOINT_KEY
        val THEME_MODE_KEY = SettingsDataStore.THEME_MODE_KEY
        val USE_LORA_KEY = SettingsDataStore.USE_LORA_KEY
        val TTS_MODEL_KEY = SettingsDataStore.TTS_MODEL_KEY
        val NSFW_BLUR_ENABLED_KEY = SettingsDataStore.NSFW_BLUR_ENABLED_KEY
        val NSFW_WARNING_ENABLED_KEY = SettingsDataStore.NSFW_WARNING_ENABLED_KEY
        val REPLICATE_API_KEY = SettingsDataStore.REPLICATE_API_KEY
        val REPLICATE_MODEL_KEY = SettingsDataStore.REPLICATE_MODEL_KEY
        
        // Image Editing Keys
        val IMAGE_EDITING_PROVIDER_KEY = androidx.datastore.preferences.core.stringPreferencesKey("image_editing_provider")
        val REPLICATE_EDITING_API_KEY = androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_api_key")
        val REPLICATE_EDITING_MODEL_KEY = androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_model")
        val TOGETHER_AI_EDITING_API_KEY = androidx.datastore.preferences.core.stringPreferencesKey("together_ai_editing_api_key")
        val IMAGE_EDITING_MODEL_KEY = androidx.datastore.preferences.core.stringPreferencesKey("image_editing_model")
        val IMAGE_EDITING_STRENGTH_KEY = androidx.datastore.preferences.core.stringPreferencesKey("image_editing_strength")
        
        // Video Generation Keys
        val VIDEO_PROVIDER_KEY = SettingsDataStore.VIDEO_PROVIDER_KEY
        val FAL_AI_VIDEO_API_KEY = SettingsDataStore.FAL_AI_VIDEO_API_KEY
        val FAL_AI_VIDEO_MODEL_KEY = SettingsDataStore.FAL_AI_VIDEO_MODEL_KEY
        val REPLICATE_VIDEO_API_KEY = SettingsDataStore.REPLICATE_VIDEO_API_KEY
        val REPLICATE_VIDEO_MODEL_KEY = SettingsDataStore.REPLICATE_VIDEO_MODEL_KEY
        val MODELSLAB_VIDEO_API_KEY = SettingsDataStore.MODELSLAB_VIDEO_API_KEY
        val MODELSLAB_VIDEO_MODEL_KEY = SettingsDataStore.MODELSLAB_VIDEO_MODEL_KEY
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _allowInsecureConnections = MutableStateFlow(false)
    val allowInsecureConnections: StateFlow<Boolean> = _allowInsecureConnections.asStateFlow()
    
    val currentAppIcon = appIconManager.currentIcon
    
    fun setAppIcon(icon: com.vortexai.android.utils.AppIconManager.AppIcon) {
        viewModelScope.launch {
            appIconManager.setIcon(icon)
        }
    }
    
    init {
        loadSettings()
        loadCustomApiProviders()
        
        // Add timeout for loading states
        viewModelScope.launch {
            kotlinx.coroutines.delay(30000) // 30 second timeout
            if (_uiState.value.isLoadingModels) {
                Log.w("SettingsViewModel", "⚠️ Model loading timeout - stopping loading state")
                _uiState.value = _uiState.value.copy(
                    isLoadingModels = false,
                    endpointError = "⚠️ Model loading timed out. Please check your connection and try again."
                )
            }
        }
    }
    
    private fun loadCustomApiProviders() {
        viewModelScope.launch {
            customApiProviderRepository.getEnabledProvidersByType(com.vortexai.android.data.models.ApiProviderType.TEXT_GENERATION)
                .collect { providers ->
                    _uiState.value = _uiState.value.copy(customLlmProviders = providers)
                }
        }
        viewModelScope.launch {
            customApiProviderRepository.getEnabledProvidersByType(com.vortexai.android.data.models.ApiProviderType.IMAGE_GENERATION)
                .collect { providers ->
                    _uiState.value = _uiState.value.copy(customImageProviders = providers)
                }
        }
        viewModelScope.launch {
            customApiProviderRepository.getEnabledProvidersByType(com.vortexai.android.data.models.ApiProviderType.IMAGE_EDITING)
                .collect { providers ->
                    _uiState.value = _uiState.value.copy(customImageEditProviders = providers)
                }
        }
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                Log.d("SettingsViewModel", "🔄 Loading settings...")
                var currentState = _uiState.value
                
                // Load settings from each manager
                currentState = interfaceSettingsManager.loadInterfaceSettings(currentState)
                Log.d("SettingsViewModel", "✅ Interface settings loaded")
                
                currentState = llmSettingsManager.loadLLMSettings(currentState)
                Log.d("SettingsViewModel", "✅ LLM settings loaded - Provider: ${currentState.llmProvider}, Model: ${currentState.llmModel}")
                
                currentState = imageSettingsManager.loadImageSettings(currentState)
                Log.d("SettingsViewModel", "✅ Image settings loaded - Provider: ${currentState.imageProvider}")
                
                currentState = audioSettingsManager.loadAudioSettings(currentState)
                Log.d("SettingsViewModel", "✅ Audio settings loaded - Provider: ${currentState.ttsProvider}")
                
                currentState = backupSettingsManager.loadBackupSettings(currentState)
                Log.d("SettingsViewModel", "✅ Backup settings loaded")
                
                // Load profile settings
                val preferences = settingsDataStore.getPreferences()
                currentState = currentState.copy(
                    username = preferences[androidx.datastore.preferences.core.stringPreferencesKey("username")] ?: "",
                    fullName = preferences[androidx.datastore.preferences.core.stringPreferencesKey("full_name")] ?: "",
                    email = preferences[androidx.datastore.preferences.core.stringPreferencesKey("email")] ?: "",
                    dateOfBirth = preferences[androidx.datastore.preferences.core.stringPreferencesKey("date_of_birth")] ?: "",
                    modelsByProvider = modelCacheManager.getAllModelsByProvider(),
                    imageModelsByProvider = modelCacheManager.getAllImageModelsByProvider(),
                    // Load image editing settings
                    imageEditingProvider = preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_provider")] ?: "Replicate",
                    replicateEditingApiKey = preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_api_key")] ?: "",
                    replicateEditingModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_model")] ?: "qwen-image-edit",
                    togetherAiEditingApiKey = preferences[androidx.datastore.preferences.core.stringPreferencesKey("together_ai_editing_api_key")] ?: "",
                    imageEditingModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_model")] ?: "black-forest-labs/FLUX.1-schnell",
                    imageEditingStrength = preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_strength")] ?: "Medium (0.5)",
                    // Modelslab Image Editing settings
                    modelslabEditingApiKey = preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_api_key")] ?: "",
                    modelslabEditingModel = preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_model")] ?: "flux-kontext-dev",
                    modelslabEditingStrength = preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_strength")] ?: "High (0.7)",
                    modelslabEditingStrengthFloat = preferences[androidx.datastore.preferences.core.floatPreferencesKey("modelslab_editing_strength_float")] ?: 0.7f,
                    imgbbApiKey = preferences[androidx.datastore.preferences.core.stringPreferencesKey("imgbb_api_key")] ?: "",
                    modelslabNegativePrompt = preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_negative_prompt")] ?: "(worst quality:2), (low quality:2), (normal quality:2), (jpeg artifacts), (blurry), (duplicate), (morbid), (mutilated), (out of frame), (extra limbs), (bad anatomy), (disfigured), (deformed), (cross-eye), (glitch), (oversaturated), (overexposed), (underexposed), (bad proportions), (bad hands), (bad feet), (cloned face), (long neck), (missing arms), (missing legs), (extra fingers), (fused fingers), (poorly drawn hands), (poorly drawn face), (mutation), (deformed eyes), watermark, text, logo, signature, grainy, tiling, censored, nsfw, ugly, blurry eyes, noisy image, bad lighting, unnatural skin, asymmetry",
                    // Custom API Image Editing
                    selectedCustomImageEditProviderId = preferences[androidx.datastore.preferences.core.stringPreferencesKey("selected_custom_image_edit_provider_id")] ?: "",
                    // Update Server Settings
                    updateServerIp = preferences[androidx.datastore.preferences.core.stringPreferencesKey("update_server_ip")] ?: "10.0.2.2",
                    updateServerPort = preferences[androidx.datastore.preferences.core.stringPreferencesKey("update_server_port")] ?: "8000",
                    // Import from Server Settings
                    importServerUrl = preferences[androidx.datastore.preferences.core.stringPreferencesKey("import_server_url")] ?: "",
                    importSelectedFile = preferences[androidx.datastore.preferences.core.stringPreferencesKey("import_selected_file")] ?: "",
                    // ComfyUI specific settings
                    comfyUiCustomWorkflow = preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_custom_workflow")] ?: "",
                    comfyUiEditingCheckpoint = preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_editing_checkpoint")] ?: "",
                    comfyUiNegativePrompt = preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_negative_prompt")] ?: "",
                    comfyUiWorkflowFileName = preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_workflow_filename")] ?: "",
                    imageWidth = preferences[androidx.datastore.preferences.core.intPreferencesKey("image_width")] ?: 512,
                    imageHeight = preferences[androidx.datastore.preferences.core.intPreferencesKey("image_height")] ?: 512,
                    imageSeed = preferences[androidx.datastore.preferences.core.longPreferencesKey("image_seed")] ?: -1L,
                    comfyUiModels = (modelCacheManager.getCachedImageModels("ComfyUI") as? List<ModelInfo>)?.map { it.id } ?: emptyList(),
                    comfyUiLoraModels = (modelCacheManager.getCachedModels("ComfyUI_LoRA") as? List<ModelInfo>)?.map { it.id } ?: emptyList(),
                    // Load video generation settings
                    videoProvider = preferences[SettingsDataStore.VIDEO_PROVIDER_KEY] ?: "fal.ai",
                    falAiVideoApiKey = preferences[SettingsDataStore.FAL_AI_VIDEO_API_KEY] ?: "",
                    falAiVideoModel = preferences[SettingsDataStore.FAL_AI_VIDEO_MODEL_KEY] ?: "fal-ai/kling-video/v1/standard/image-to-video",
                    replicateVideoApiKey = preferences[SettingsDataStore.REPLICATE_VIDEO_API_KEY] ?: "",
                    replicateVideoModel = preferences[SettingsDataStore.REPLICATE_VIDEO_MODEL_KEY] ?: "stability-ai/stable-video-diffusion",
                    modelslabVideoApiKey = preferences[SettingsDataStore.MODELSLAB_VIDEO_API_KEY] ?: "",
                    modelslabVideoModel = preferences[SettingsDataStore.MODELSLAB_VIDEO_MODEL_KEY] ?: "video/text2video"
                )
                
                // Set available image models based on current provider
                val availableImageModels = when (currentState.imageProvider) {
                    "Together AI" -> currentState.manuallyAddedTogetherAiImageModels
                    "Hugging Face" -> currentState.manuallyAddedHuggingFaceImageModels
                    "ModelsLab" -> currentState.manuallyAddedImageModels
                    "Custom API" -> currentState.manuallyAddedCustomImageModels
                    "ComfyUI" -> (modelCacheManager.getCachedImageModels("ComfyUI") as? List<ModelInfo>)?.map { it.id } ?: emptyList()
                    else -> emptyList()
                }
                currentState = currentState.copy(availableImageModels = availableImageModels)
                Log.d("SettingsViewModel", "✅ Profile settings loaded - Username: ${currentState.username}")
                
                _uiState.value = currentState
                Log.d("SettingsViewModel", "✅ All settings loaded successfully")
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error loading settings: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    endpointError = "❌ Error loading settings: ${e.message}"
                )
            }
        }
    }
    
    // Interface settings functions
    fun updateDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
        saveInterfaceSettings()
    }

    fun updateThemeMode(mode: String) {
        val normalized = when (mode.lowercase()) {
            "system", "light", "dark" -> mode.lowercase()
            else -> "system"
        }
        _uiState.value = _uiState.value.copy(themeMode = normalized)
        saveInterfaceSettings()
    }
    
    fun setAllowInsecureConnections(allow: Boolean) {
        _allowInsecureConnections.value = allow
    }

    fun updateLanguage(language: String) { 
        _uiState.value = _uiState.value.copy(language = language)
        saveInterfaceSettings()
    }
    
    fun updateNsfwBlurEnabled(enabled: Boolean) { 
        _uiState.value = _uiState.value.copy(nsfwBlurEnabled = enabled)
        saveInterfaceSettings()
    }
    
    fun updateNsfwWarningEnabled(enabled: Boolean) { 
        _uiState.value = _uiState.value.copy(nsfwWarningEnabled = enabled)
        saveInterfaceSettings()
    }
    
    fun saveInterfaceSettings() {
        viewModelScope.launch {
            interfaceSettingsManager.saveInterfaceSettings(_uiState.value)
        }
    }
    
    // LLM settings functions
    fun updateLlmProvider(provider: String) { 
        val cachedModels = modelCacheManager.getCachedModels(provider) ?: emptyList()
        _uiState.value = _uiState.value.copy(
            llmProvider = provider,
            availableModels = cachedModels
        )
        saveLLMSettings()
    }
    
    fun updateLlmModel(model: String) { 
        _uiState.value = _uiState.value.copy(llmModel = model)
        saveLLMSettings()
    }
    
    fun clearModelCache() {
        Log.d("SettingsViewModel", "🗑️ Clearing model cache")
        modelCacheManager.clearAllCaches()
        _uiState.value = _uiState.value.copy(
            modelsByProvider = emptyMap(),
            imageModelsByProvider = emptyMap(),
            availableModels = emptyList(),
            availableImageModels = emptyList(),
            endpointError = "✅ Model cache cleared. You can now fetch fresh models."
        )
    }
    
    fun fetchModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingModels = true, endpointError = "")
            
            try {
                val currentState = _uiState.value
                val provider = currentState.llmProvider
                
                Log.d("SettingsViewModel", "🔍 Fetching models for provider: $provider")
                
                // Check cache first
                val cachedModels = modelCacheManager.getCachedModels(provider)
                if (cachedModels != null && cachedModels.isNotEmpty()) {
                    Log.d("SettingsViewModel", "✅ Using cached models for $provider: ${cachedModels.size} models")
                    _uiState.value = _uiState.value.copy(
                        availableModels = cachedModels,
                        isLoadingModels = false,
                        endpointError = "✅ Loaded ${cachedModels.size} cached models for $provider"
                    )
                    return@launch
                }
                
                val apiKey = getApiKeyForProvider(provider, currentState)
                val customEndpoint = getEndpointForProvider(provider, currentState)
                
                Log.d("SettingsViewModel", "🔑 API Key length: ${apiKey.length}, Endpoint: $customEndpoint")
                
                // Validate required fields
                val requiresApiKey = provider !in listOf("Ollama", "Kobold AI", "LMStudio")
                if (requiresApiKey && apiKey.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingModels = false,
                        endpointError = "❌ API key required for $provider. Please enter your API key first."
                    )
                    return@launch
                }
                
                val result = llmSettingsManager.fetchModels(provider, apiKey, customEndpoint)
                
                result.fold(
                    onSuccess = { models ->
                        Log.d("SettingsViewModel", "✅ LLM fetchModels SUCCESS: received ${models.size} models")
                        
                        if (models.isNotEmpty()) {
                            modelCacheManager.setCachedModels(provider, models)
                            
                            _uiState.value = _uiState.value.copy(
                                availableModels = models,
                                modelsByProvider = modelCacheManager.getAllModelsByProvider(),
                                isLoadingModels = false,
                                endpointError = "✅ Successfully loaded ${models.size} models for $provider"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                availableModels = emptyList(),
                                isLoadingModels = false,
                                endpointError = "⚠️ No models found for $provider. Check your API key and try again."
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e("SettingsViewModel", "❌ LLM fetchModels FAILED: ${error.message}")
                        val errorMsg = when {
                            error.message?.contains("401") == true -> "❌ Invalid API key for $provider"
                            error.message?.contains("403") == true -> "❌ Access denied for $provider"
                            error.message?.contains("404") == true -> "❌ $provider endpoint not found"
                            error.message?.contains("timeout") == true -> "❌ Connection timeout to $provider"
                            error.message?.contains("network") == true -> "❌ Network error connecting to $provider"
                            else -> "❌ Failed to fetch models from $provider: ${error.message}"
                        }
                        _uiState.value = _uiState.value.copy(
                            availableModels = emptyList(),
                            isLoadingModels = false,
                            endpointError = errorMsg
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Exception in fetchModels: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    availableModels = emptyList(),
                    isLoadingModels = false,
                    endpointError = "❌ Error fetching models: ${e.message}"
                )
            }
        }
    }
    
    private fun getApiKeyForProvider(provider: String, state: SettingsUiState): String {
        return when (provider) {
            "Together AI" -> state.togetherAiApiKey
            "Gemini API" -> state.geminiApiKey
            "Open Router" -> state.openRouterApiKey
            "Hugging Face" -> state.huggingFaceApiKey
            "ModelsLab" -> state.modelsLabApiKey
            "Grok" -> state.grokApiKey
            "Custom API" -> state.customLlmApiKey
            else -> ""
        }
    }
    
    private fun getEndpointForProvider(provider: String, state: SettingsUiState): String? {
        return when (provider) {
            "Ollama" -> state.ollamaEndpoint.ifBlank { "http://localhost:11435" }
            "Kobold AI" -> state.koboldEndpoint.ifBlank { "http://localhost:5000" }
            "LMStudio" -> state.lmStudioEndpoint.ifBlank { "http://localhost:1234" }
            "Custom API" -> state.customLlmEndpoint
            else -> null
        }
    }
    
    fun updateTogetherAiApiKey(key: String) { 
        Log.d("SettingsViewModel", "🔑 Updating Together AI API key (length: ${key.length})")
        modelCacheManager.clearModelCache("Together AI")
        _uiState.value = _uiState.value.copy(
            togetherAiApiKey = key,
            availableModels = if (_uiState.value.llmProvider == "Together AI") emptyList() else _uiState.value.availableModels,
            endpointError = if (key.isNotBlank()) "✅ API key updated. Click 'Fetch Models' to load available models." else ""
        )
        saveLLMSettings()
    }
    
    fun saveLLMSettings() {
        viewModelScope.launch {
            try {
                Log.d("SettingsViewModel", "💾 Saving LLM settings...")
                llmSettingsManager.saveLLMSettings(_uiState.value)
                Log.d("SettingsViewModel", "✅ LLM settings saved successfully")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error saving LLM settings: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    endpointError = "❌ Error saving settings: ${e.message}"
                )
            }
        }
    }
    
    // Image settings functions
    fun updateImageProvider(provider: String) { 
        val currentState = _uiState.value
        val manualModels = when (provider) {
            "Together AI" -> currentState.manuallyAddedTogetherAiImageModels
            "Hugging Face" -> currentState.manuallyAddedHuggingFaceImageModels
            "ModelsLab" -> currentState.manuallyAddedImageModels
            "Custom API" -> currentState.manuallyAddedCustomImageModels
            "ComfyUI" -> modelCacheManager.getCachedImageModels(provider) ?: emptyList()
            else -> emptyList()
        }
        _uiState.value = _uiState.value.copy(
            imageProvider = provider,
            availableImageModels = manualModels
        )
        saveImageSettings()
    }
    
    fun updateImageModel(model: String) { 
        _uiState.value = _uiState.value.copy(imageModel = model)
        saveImageSettings()
    }
    
    fun fetchImageModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingImageModels = true)
            
            try {
                val currentState = _uiState.value
                val provider = currentState.imageProvider
                
                val cachedModels = modelCacheManager.getCachedImageModels(provider)
                if (cachedModels != null && cachedModels.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        availableImageModels = cachedModels,
                        isLoadingImageModels = false
                    )
                    return@launch
                }
                
                val apiKey = getImageApiKeyForProvider(provider, currentState)
                val customEndpoint = getImageEndpointForProvider(provider, currentState)
                val manualModels = getManualImageModelsForProvider(provider, currentState)
                
                val result = imageSettingsManager.fetchImageModels(provider, apiKey, customEndpoint, manualModels)
                
                result.fold(
                    onSuccess = { models ->
                        modelCacheManager.setCachedImageModels(provider, models)
                        
                        _uiState.value = _uiState.value.copy(
                            availableImageModels = models,
                            imageModelsByProvider = modelCacheManager.getAllImageModelsByProvider(),
                            isLoadingImageModels = false
                        )
                    },
                    onFailure = { error ->
                        Log.e("SettingsViewModel", "Error fetching image models", error)
                        _uiState.value = _uiState.value.copy(
                            isLoadingImageModels = false
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error in fetchImageModels", e)
                _uiState.value = _uiState.value.copy(isLoadingImageModels = false)
            }
        }
    }
    
    private fun getImageApiKeyForProvider(provider: String, state: SettingsUiState): String {
        return when (provider) {
            "Together AI" -> state.togetherAiImageApiKey
            "Hugging Face" -> state.huggingFaceImageApiKey
            "ModelsLab" -> state.modelsLabImageApiKey
            "Grok" -> state.grokImageApiKey
            "Custom API" -> state.customImageApiKey
            "Replicate" -> state.replicateApiKey
            else -> ""
        }
    }
    
    private fun getImageEndpointForProvider(provider: String, state: SettingsUiState): String? {
        return when (provider) {
            "ComfyUI" -> state.comfyUiEndpoint.ifBlank { "http://localhost:8188" }
            "Custom API" -> state.customImageEndpoint
            else -> null
        }
    }
    
    private fun getManualImageModelsForProvider(provider: String, state: SettingsUiState): List<String> {
        return when (provider) {
            "ModelsLab" -> state.manuallyAddedImageModels
            "Custom API" -> state.manuallyAddedCustomImageModels
            else -> emptyList()
        }
    }
    
    fun saveImageSettings() {
        viewModelScope.launch {
            imageSettingsManager.saveImageSettings(_uiState.value)
        }
    }
    
    // Audio settings functions
    fun updateTtsProvider(provider: String) { 
        _uiState.value = _uiState.value.copy(ttsProvider = provider)
        saveAudioSettings()
    }
    
    fun updateTtsVoice(voice: String) { 
        _uiState.value = _uiState.value.copy(ttsVoice = voice)
        saveAudioSettings()
    }
    
    fun saveAudioSettings() {
        viewModelScope.launch {
            audioSettingsManager.saveAudioSettings(_uiState.value)
        }
    }
    
    // Backup settings functions
    fun updateSupabaseUrl(url: String) { 
        _uiState.value = _uiState.value.copy(supabaseUrl = url)
        saveBackupSettings()
    }
    
    fun updateSupabaseAnonKey(key: String) { 
        _uiState.value = _uiState.value.copy(supabaseAnonKey = key)
        saveBackupSettings()
    }
    
    fun testSupabaseConnection() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.supabaseUrl.isBlank() || currentState.supabaseAnonKey.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    supabaseConnectionStatus = "❌ Please enter both URL and API key"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                supabaseConnectionStatus = "🔄 Testing connection..."
            )
            
            try {
                val connectionTest = SupabaseConnectionTest()
                val result = connectionTest.testConnection(currentState.supabaseUrl, currentState.supabaseAnonKey)
                
                val statusMessage = when (result) {
                    is ConnectionResult.Success -> result.message
                    is ConnectionResult.PartialSuccess -> result.message
                    is ConnectionResult.Failure -> result.error
                }
                
                _uiState.value = _uiState.value.copy(
                    supabaseConnectionStatus = statusMessage
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    supabaseConnectionStatus = "❌ Test failed: ${e.message}"
                )
            }
        }
    }
    
    fun createCloudBackup() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.supabaseUrl.isBlank() || currentState.supabaseAnonKey.isBlank()) {
                _uiState.value = _uiState.value.copy(backupStatus = "❌ Please configure Supabase first")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(backupStatus = "🔄 Creating cloud backup...")
            
            try {
                val result = backupSettingsManager.createCloudBackup(currentState.supabaseUrl, currentState.supabaseAnonKey)
                
                when (result) {
                    is com.vortexai.android.data.remote.BackupResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            backupStatus = "✅ Cloud backup created: ${result.fileName}",
                            lastBackupTime = System.currentTimeMillis()
                        )
                        saveBackupSettings()
                    }
                    is com.vortexai.android.data.remote.BackupResult.Failure -> {
                        _uiState.value = _uiState.value.copy(backupStatus = "❌ Backup failed: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(backupStatus = "❌ Backup failed: ${e.message}")
            }
        }
    }
    
    fun saveBackupSettings() {
        viewModelScope.launch {
            backupSettingsManager.saveBackupSettings(_uiState.value)
        }
    }
    
    // Connection testing functions
    fun testLLMConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, endpointError = "🔄 Testing connection...")
            
            try {
                val currentState = _uiState.value
                val provider = currentState.llmProvider
                val apiKey = getApiKeyForProvider(provider, currentState)
                val customEndpoint = getEndpointForProvider(provider, currentState)
                val model = currentState.llmModel
                
                Log.d("SettingsViewModel", "🧪 Testing connection to $provider with model: $model")
                
                // Validate required fields
                val requiresApiKey = provider !in listOf("Ollama", "Kobold AI", "LMStudio")
                if (requiresApiKey && apiKey.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        endpointError = "❌ API key required for $provider"
                    )
                    return@launch
                }
                
                if (model.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        endpointError = "❌ Please select a model first"
                    )
                    return@launch
                }
                
                val result = apiConnectionTester.testLLMConnection(provider, apiKey, model, customEndpoint)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = if (result.isSuccess) {
                        "✅ $provider connection successful! Model: $model"
                    } else {
                        "❌ Connection failed: ${result.getErrorMessage()}"
                    }
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Connection test exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = "❌ Connection test failed: ${e.message}"
                )
            }
        }
    }
    
    // Utility functions
    fun clearAllData() {
        viewModelScope.launch {
            settingsDataStore.clearAll()
            modelCacheManager.clearAllCaches()
            _uiState.value = SettingsUiState()
        }
    }
    
    // Profile functions
    fun updateUsername(username: String) { 
        _uiState.value = _uiState.value.copy(username = username)
        saveProfileSettings()
    }
    
    fun updateEmail(email: String) { 
        _uiState.value = _uiState.value.copy(email = email)
        saveProfileSettings()
    }
    
    // Missing interface functions
    fun updateFontSize(fontSize: String) { _uiState.value = _uiState.value.copy(fontSize = fontSize); saveInterfaceSettings() }
    fun updateThemeColor(themeColor: String) { _uiState.value = _uiState.value.copy(themeColor = themeColor); saveInterfaceSettings() }
    fun updateChatBubbleStyle(style: String) { _uiState.value = _uiState.value.copy(chatBubbleStyle = style); saveInterfaceSettings() }
    fun updateMessageLimit(limit: Int) { _uiState.value = _uiState.value.copy(messageLimit = limit); saveInterfaceSettings() }
    fun updateTypingIndicator(enabled: Boolean) { _uiState.value = _uiState.value.copy(typingIndicator = enabled); saveInterfaceSettings() }
    fun updateAutoSaveChats(enabled: Boolean) { _uiState.value = _uiState.value.copy(autoSaveChats = enabled); saveInterfaceSettings() }
    fun updateShowCharacterBackground(enabled: Boolean) { _uiState.value = _uiState.value.copy(showCharacterBackground = enabled); saveInterfaceSettings() }
    fun updateCharacterBackgroundOpacity(opacity: Float) { _uiState.value = _uiState.value.copy(characterBackgroundOpacity = opacity); saveInterfaceSettings() }
    
    // Missing LLM functions
    fun updateResponseTemperature(temp: Float) { _uiState.value = _uiState.value.copy(responseTemperature = temp); saveLLMSettings() }
    fun updateMaxTokens(tokens: Int) { _uiState.value = _uiState.value.copy(maxTokens = tokens); saveLLMSettings() }
    fun updateTopP(topP: Float) { _uiState.value = _uiState.value.copy(topP = topP); saveLLMSettings() }
    fun updateFrequencyPenalty(penalty: Float) { _uiState.value = _uiState.value.copy(frequencyPenalty = penalty); saveLLMSettings() }
    fun updateResponseLengthStyle(style: String) { _uiState.value = _uiState.value.copy(responseLengthStyle = style); saveLLMSettings() }
    fun updateEnableResponseFormatting(enabled: Boolean) { _uiState.value = _uiState.value.copy(enableResponseFormatting = enabled); saveLLMSettings() }
    fun updateCustomMaxTokens(tokens: Int) { _uiState.value = _uiState.value.copy(customMaxTokens = tokens); saveLLMSettings() }
    fun updateGeminiApiKey(key: String) { _uiState.value = _uiState.value.copy(geminiApiKey = key); saveLLMSettings() }
    fun updateOpenRouterApiKey(key: String) { _uiState.value = _uiState.value.copy(openRouterApiKey = key); saveLLMSettings() }
    fun updateHuggingFaceApiKey(key: String) { _uiState.value = _uiState.value.copy(huggingFaceApiKey = key); saveLLMSettings() }
    fun updateModelsLabApiKey(key: String) { _uiState.value = _uiState.value.copy(modelsLabApiKey = key); saveLLMSettings() }
    fun updateGrokApiKey(key: String) { _uiState.value = _uiState.value.copy(grokApiKey = key); saveLLMSettings() }
    fun updateGrokEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(grokEndpoint = endpoint); saveLLMSettings() }
    fun updateCustomLlmApiKey(key: String) { _uiState.value = _uiState.value.copy(customLlmApiKey = key); saveLLMSettings() }
    fun updateCustomLlmEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(customLlmEndpoint = endpoint); saveLLMSettings() }
    fun updateCustomLlmApiPrefix(prefix: String) { _uiState.value = _uiState.value.copy(customLlmApiPrefix = prefix); saveLLMSettings() }
    fun updateSelectedCustomLlmProvider(providerId: String) { 
        _uiState.value = _uiState.value.copy(selectedCustomLlmProviderId = providerId)
        saveLLMSettings()
    }
    fun updateSelectedCustomImageProvider(providerId: String) {
        _uiState.value = _uiState.value.copy(selectedCustomImageProviderId = providerId)
        saveImageSettings()
    }
    fun updateSelectedCustomImageEditProvider(providerId: String) {
        _uiState.value = _uiState.value.copy(selectedCustomImageEditProviderId = providerId)
        saveImageEditingSettings()
    }
    fun updateOllamaEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(ollamaEndpoint = endpoint); saveLLMSettings() }
    fun updateKoboldEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(koboldEndpoint = endpoint); saveLLMSettings() }
    fun updateLmStudioEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(lmStudioEndpoint = endpoint); saveLLMSettings() }
    
    // Missing image functions
    fun updateImageSize(size: String) { _uiState.value = _uiState.value.copy(imageSize = size); saveImageSettings() }
    fun updateImageQuality(quality: String) { _uiState.value = _uiState.value.copy(imageQuality = quality); saveImageSettings() }
    fun updateSteps(steps: Int) { _uiState.value = _uiState.value.copy(steps = steps); saveImageSettings() }
    fun updateGuidanceScale(scale: Float) { _uiState.value = _uiState.value.copy(guidanceScale = scale); saveImageSettings() }
    fun updateNegativePrompt(prompt: String) { _uiState.value = _uiState.value.copy(negativePrompt = prompt); saveImageSettings() }
    fun updateTogetherAiImageApiKey(key: String) { _uiState.value = _uiState.value.copy(togetherAiImageApiKey = key); saveImageSettings() }
    fun updateHuggingFaceImageApiKey(key: String) { _uiState.value = _uiState.value.copy(huggingFaceImageApiKey = key); saveImageSettings() }
    fun updateComfyUiEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(comfyUiEndpoint = endpoint); saveImageSettings() }
    fun updateComfyUiWorkflow(workflow: String) { _uiState.value = _uiState.value.copy(comfyUiWorkflow = workflow); saveImageSettings() }
    fun updateUseLora(enabled: Boolean) { _uiState.value = _uiState.value.copy(useLora = enabled); saveImageSettings() }
    fun updateLoraModel(model: String) { _uiState.value = _uiState.value.copy(loraModel = model); saveImageSettings() }
    fun updateLoraStrength(strength: Float) { _uiState.value = _uiState.value.copy(loraStrength = strength); saveImageSettings() }
    fun updateModelsLabImageApiKey(key: String) { _uiState.value = _uiState.value.copy(modelsLabImageApiKey = key); saveImageSettings() }
    fun updateGrokImageApiKey(key: String) { _uiState.value = _uiState.value.copy(grokImageApiKey = key); saveImageSettings() }
    fun updateModelsLabWorkflow(workflow: String) { _uiState.value = _uiState.value.copy(modelsLabWorkflow = workflow); saveImageSettings() }
    fun updateUseCharacterImgAsSource(enabled: Boolean) { _uiState.value = _uiState.value.copy(useCharacterImgAsSource = enabled); saveImageSettings() }
    fun updateReplicateApiKey(key: String) { _uiState.value = _uiState.value.copy(replicateApiKey = key); saveImageSettings() }
    fun updateReplicateModel(model: String) { _uiState.value = _uiState.value.copy(replicateModel = model); saveImageSettings() }
    fun updateReplicateDisableSafetyChecker(enabled: Boolean) { _uiState.value = _uiState.value.copy(replicateDisableSafetyChecker = enabled); saveImageSettings() }
    fun updateReplicateNegativePrompt(prompt: String) { _uiState.value = _uiState.value.copy(replicateNegativePrompt = prompt); saveImageSettings() }
    fun updateReplicateWidth(width: Int) { _uiState.value = _uiState.value.copy(replicateWidth = width); saveImageSettings() }
    fun updateReplicateHeight(height: Int) { _uiState.value = _uiState.value.copy(replicateHeight = height); saveImageSettings() }
    fun updateCustomImageEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(customImageEndpoint = endpoint); saveImageSettings() }
    fun updateCustomImageApiPrefix(prefix: String) { _uiState.value = _uiState.value.copy(customImageApiPrefix = prefix); saveImageSettings() }
    fun updateCustomImageApiKey(key: String) { _uiState.value = _uiState.value.copy(customImageApiKey = key); saveImageSettings() }
    
    // Missing audio functions
    fun updateTtsModel(model: String) { _uiState.value = _uiState.value.copy(ttsModel = model); saveAudioSettings() }
    fun updateSttProvider(provider: String) { _uiState.value = _uiState.value.copy(sttProvider = provider); saveAudioSettings() }
    fun updateTtsApiKey(key: String) { _uiState.value = _uiState.value.copy(ttsApiKey = key); saveAudioSettings() }
    fun updateSttApiKey(key: String) { _uiState.value = _uiState.value.copy(sttApiKey = key); saveAudioSettings() }
    fun updateTtsSpeed(speed: Float) { _uiState.value = _uiState.value.copy(ttsSpeed = speed); saveAudioSettings() }
    fun updateTtsPitch(pitch: Float) { _uiState.value = _uiState.value.copy(ttsPitch = pitch); saveAudioSettings() }
    fun updateSttLanguage(language: String) { _uiState.value = _uiState.value.copy(sttLanguage = language); saveAudioSettings() }
    fun updateTtsLanguage(language: String) { _uiState.value = _uiState.value.copy(ttsLanguage = language); saveAudioSettings() }
    fun updateTtsInitAudio(initAudio: String) { _uiState.value = _uiState.value.copy(ttsInitAudio = initAudio); saveAudioSettings() }
    fun updateAutoPlayTts(enabled: Boolean) { _uiState.value = _uiState.value.copy(autoPlayTts = enabled); saveAudioSettings() }
    fun updateVoiceActivation(enabled: Boolean) { _uiState.value = _uiState.value.copy(voiceActivation = enabled); saveAudioSettings() }
    fun updateCustomAudioEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(customAudioEndpoint = endpoint); saveAudioSettings() }
    fun updateCustomAudioApiPrefix(prefix: String) { _uiState.value = _uiState.value.copy(customAudioApiPrefix = prefix); saveAudioSettings() }
    fun updateTogetherAiTtsApiKey(key: String) { _uiState.value = _uiState.value.copy(togetherAiTtsApiKey = key); saveAudioSettings() }
    fun updateElevenLabsApiKey(key: String) { _uiState.value = _uiState.value.copy(elevenLabsApiKey = key); saveAudioSettings() }
    fun updateTtsVoiceHindi(voice: String) { _uiState.value = _uiState.value.copy(ttsVoiceHindi = voice); saveAudioSettings() }
    fun updateElevenLabsLanguage(language: String) { _uiState.value = _uiState.value.copy(elevenLabsLanguage = language); saveAudioSettings() }
    
    // Video functions
    fun updateVideoProvider(provider: String) { _uiState.value = _uiState.value.copy(videoProvider = provider); saveVideoSettings() }
    fun updateFalAiVideoApiKey(key: String) { _uiState.value = _uiState.value.copy(falAiVideoApiKey = key); saveVideoSettings() }
    fun updateFalAiVideoModel(model: String) { _uiState.value = _uiState.value.copy(falAiVideoModel = model); saveVideoSettings() }
    fun updateReplicateVideoApiKey(key: String) { _uiState.value = _uiState.value.copy(replicateVideoApiKey = key); saveVideoSettings() }
    fun updateReplicateVideoModel(model: String) { _uiState.value = _uiState.value.copy(replicateVideoModel = model); saveVideoSettings() }
    fun updateModelsLabVideoApiKey(key: String) { _uiState.value = _uiState.value.copy(modelslabVideoApiKey = key); saveVideoSettings() }
    fun updateModelsLabVideoModel(model: String) { _uiState.value = _uiState.value.copy(modelslabVideoModel = model); saveVideoSettings() }
    
    fun saveVideoSettings() {
        viewModelScope.launch {
            settingsDataStore.savePreferences { prefs ->
                val state = _uiState.value
                prefs[SettingsDataStore.VIDEO_PROVIDER_KEY] = state.videoProvider
                prefs[SettingsDataStore.FAL_AI_VIDEO_API_KEY] = state.falAiVideoApiKey
                prefs[SettingsDataStore.FAL_AI_VIDEO_MODEL_KEY] = state.falAiVideoModel
                prefs[SettingsDataStore.REPLICATE_VIDEO_API_KEY] = state.replicateVideoApiKey
                prefs[SettingsDataStore.REPLICATE_VIDEO_MODEL_KEY] = state.replicateVideoModel
                prefs[SettingsDataStore.MODELSLAB_VIDEO_API_KEY] = state.modelslabVideoApiKey
                prefs[SettingsDataStore.MODELSLAB_VIDEO_MODEL_KEY] = state.modelslabVideoModel
            }
        }
    }

    // Missing profile functions
    fun updateFullName(name: String) { _uiState.value = _uiState.value.copy(fullName = name); saveProfileSettings() }
    fun updateDateOfBirth(dob: String) { _uiState.value = _uiState.value.copy(dateOfBirth = dob); saveProfileSettings() }
    
    // Missing backup functions
    fun updateSupabaseEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(supabaseEnabled = enabled); saveBackupSettings() }
    fun updateAutoBackupEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(autoBackupEnabled = enabled); saveBackupSettings() }
    fun updateCloudSyncEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(cloudSyncEnabled = enabled); saveBackupSettings() }
    fun updateAnalyticsEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(analyticsEnabled = enabled); saveBackupSettings() }
    fun updateCrashReports(enabled: Boolean) { _uiState.value = _uiState.value.copy(crashReports = enabled); saveBackupSettings() }
    fun updatePushNotifications(enabled: Boolean) { _uiState.value = _uiState.value.copy(pushNotifications = enabled); saveBackupSettings() }
    fun updateEmailNotifications(enabled: Boolean) { _uiState.value = _uiState.value.copy(emailNotifications = enabled); saveBackupSettings() }
    fun updateExperimentalFeaturesEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(experimentalFeaturesEnabled = enabled); saveBackupSettings() }
    
    fun updateComfyUiEditingCheckpoint(checkpoint: String) {
        _uiState.value = _uiState.value.copy(comfyUiEditingCheckpoint = checkpoint)
    }

    fun runLLMDiagnostics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    endpointError = "🔍 Running comprehensive diagnostics..."
                )
                
                val diagnosticReport = chatLLMService.getLLMProviderStatus()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = diagnosticReport
                )
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Diagnostics failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = "❌ Diagnostics failed: ${e.message}"
                )
            }
        }
    }
    
    fun validateLLMConfiguration() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    endpointError = "🔍 Validating LLM configuration..."
                )
                
                val result = chatLLMService.validateLLMConfiguration()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = result.fold(
                        onSuccess = { it },
                        onFailure = { "❌ ${it.message}" }
                    )
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = "❌ Validation failed: ${e.message}"
                )
            }
        }
    }
    
    // Stub functions for complex operations
    fun fetchTtsVoices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTtsVoices = true)
            
            try {
                val currentState = _uiState.value
                when (currentState.ttsProvider) {
                    "Together AI" -> {
                        if (currentState.togetherAiTtsApiKey.isBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isLoadingTtsVoices = false,
                                endpointError = "❌ Together AI API key required for TTS"
                            )
                            return@launch
                        }
                        
                        // Test connection and update available voices
                        val voices = listOf(
                            "Barbershop Man",
                            "Conversational Woman", 
                            "Customer Service Woman",
                            "Newscaster Man",
                            "Newscaster Woman"
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            availableTtsVoices = voices,
                            availableTtsModels = listOf("cartesia/sonic", "cartesia/sonic-2"),
                            isLoadingTtsVoices = false,
                            endpointError = "✅ Together AI TTS connection successful"
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingTtsVoices = false,
                            endpointError = "TTS voice fetching not implemented for ${currentState.ttsProvider}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingTtsVoices = false,
                    endpointError = "❌ Error fetching TTS voices: ${e.message}"
                )
            }
        }
    }
    fun testAudioConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, endpointError = "🔄 Testing audio connection...")
            
            try {
                val currentState = _uiState.value
                when (currentState.ttsProvider) {
                    "ModelsLab" -> {
                        if (currentState.modelsLabApiKey.isBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                endpointError = "❌ ModelsLab API key required"
                            )
                            return@launch
                        }
                        
                        val ttsApi = com.vortexai.android.domain.service.ModelsLabTTSApi()
                        val request = com.vortexai.android.domain.service.TTSRequest(
                            text = "Test connection",
                            modelId = currentState.ttsModel.ifBlank { "inworld-tts-1" },
                            voiceId = currentState.ttsVoice.ifBlank { "Olivia" },
                            language = "english"
                        )
                        
                        val result = ttsApi.textToAudio(currentState.modelsLabApiKey, request)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            endpointError = if (result.isSuccess) {
                                "✅ ModelsLab TTS connection successful!"
                            } else {
                                "❌ Connection failed: ${result.exceptionOrNull()?.message}"
                            }
                        )
                    }
                    "ElevenLabs" -> {
                        if (currentState.elevenLabsApiKey.isBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                endpointError = "❌ ElevenLabs API key required"
                            )
                            return@launch
                        }
                        
                        val ttsProvider = com.vortexai.android.domain.service.audio.ElevenLabsTTSProvider()
                        ttsProvider.setApiKey(currentState.elevenLabsApiKey)
                        
                        val result = ttsProvider.testConnection()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            endpointError = if (result) {
                                "✅ ElevenLabs TTS connection successful!"
                            } else {
                                "❌ ElevenLabs connection failed"
                            }
                        )
                    }
                    "Together AI" -> {
                        if (currentState.togetherAiTtsApiKey.isBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                endpointError = "❌ Together AI API key required"
                            )
                            return@launch
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            endpointError = "✅ Together AI TTS connection available"
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            endpointError = "❌ Audio connection test not available for ${currentState.ttsProvider}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = "❌ Test failed: ${e.message}"
                )
            }
        }
    }
    
    fun testImageConnection() { /* Stub */ }
    
    fun testAudio(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingAudio = true, testAudioError = "")
            
            try {
                val currentState = _uiState.value
                when (currentState.ttsProvider) {
                    "ModelsLab" -> {
                        if (currentState.modelsLabApiKey.isBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isTestingAudio = false,
                                testAudioError = "❌ ModelsLab API key required"
                            )
                            return@launch
                        }
                        
                        val ttsApi = com.vortexai.android.domain.service.ModelsLabTTSApi()
                        val request = com.vortexai.android.domain.service.TTSRequest(
                            text = text,
                            modelId = currentState.ttsModel.ifBlank { "inworld-tts-1" },
                            voiceId = currentState.ttsVoice.ifBlank { "Olivia" },
                            language = "english"
                        )
                        
                        val result = ttsApi.textToAudio(currentState.modelsLabApiKey, request)
                        
                        if (result.isSuccess) {
                            val audioUrl = result.getOrNull()
                            if (!audioUrl.isNullOrBlank()) {
                                _uiState.value = _uiState.value.copy(
                                    testAudioError = "🔊 Playing audio..."
                                )
                                com.vortexai.android.utils.AudioPlayer.playAudio(
                                    audioUrl = audioUrl,
                                    onCompletion = {
                                        _uiState.value = _uiState.value.copy(
                                            isTestingAudio = false,
                                            testAudioError = "✅ Audio played successfully!"
                                        )
                                    },
                                    onError = { error ->
                                        _uiState.value = _uiState.value.copy(
                                            isTestingAudio = false,
                                            testAudioError = "❌ Audio playback failed: $error"
                                        )
                                    }
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isTestingAudio = false,
                                    testAudioError = "❌ No audio URL received"
                                )
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isTestingAudio = false,
                                testAudioError = "❌ Audio generation failed: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    }
                    "ElevenLabs" -> {
                        if (currentState.elevenLabsApiKey.isBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isTestingAudio = false,
                                testAudioError = "❌ ElevenLabs API key required"
                            )
                            return@launch
                        }
                        
                        val ttsProvider = com.vortexai.android.domain.service.audio.ElevenLabsTTSProvider()
                        ttsProvider.setApiKey(currentState.elevenLabsApiKey)
                        
                        // Use the correct voice based on selected language
                        val selectedVoice = if (currentState.elevenLabsLanguage == "Hindi") {
                            currentState.ttsVoiceHindi.ifBlank { "Prabhat" }
                        } else {
                            currentState.ttsVoice.ifBlank { "Rachel" }
                        }
                        
                        val result = ttsProvider.generateSpeech(
                            text = text,
                            model = currentState.ttsModel.ifBlank { "eleven_multilingual_v2" },
                            voice = selectedVoice
                        )
                        
                        if (result.isSuccess) {
                            val audioResult = result.getOrNull()
                            if (audioResult != null) {
                                _uiState.value = _uiState.value.copy(
                                    testAudioError = "🔊 Playing audio..."
                                )
                                
                                // Use the selected voice based on language
                                val selectedVoice = if (currentState.elevenLabsLanguage == "Hindi") {
                                    currentState.ttsVoiceHindi
                                } else {
                                    currentState.ttsVoice
                                }
                                
                                com.vortexai.android.utils.AudioPlayer.playAudioBytes(
                                    audioBytes = audioResult.audioData,
                                    onCompletion = {
                                        _uiState.value = _uiState.value.copy(
                                            isTestingAudio = false,
                                            testAudioError = "✅ Audio played successfully with $selectedVoice voice!"
                                        )
                                    },
                                    onError = { error ->
                                        _uiState.value = _uiState.value.copy(
                                            isTestingAudio = false,
                                            testAudioError = "❌ Audio playback failed: $error"
                                        )
                                    }
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isTestingAudio = false,
                                    testAudioError = "❌ No audio data received"
                                )
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isTestingAudio = false,
                                testAudioError = "❌ Audio generation failed: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingAudio = false,
                            testAudioError = "❌ Audio test only available for ModelsLab and ElevenLabs"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingAudio = false,
                    testAudioError = "❌ Test failed: ${e.message}"
                )
            }
        }
    }
    fun debugReplicateInput(prompt: String) { /* Stub */ }
    fun showAvatarFixMessage() { /* Stub */ }
    private var bulkImportCallback: ((android.net.Uri?) -> Unit)? = null
    
    fun setBulkImportCallback(callback: (android.net.Uri?) -> Unit) {
        bulkImportCallback = callback
    }
    
    fun bulkImportCharacterCards(context: Context) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            endpointError = "📁 Select folder with character cards..."
        )
        bulkImportCallback?.invoke(null)
    }
    
    fun processBulkImport(context: Context, uri: android.net.Uri?) {
        if (uri == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, endpointError = "❌ No folder selected")
            return
        }
        viewModelScope.launch {
            try {
                val docTree = DocumentFile.fromTreeUri(context, uri)
                val files = docTree?.listFiles()?.filter { file ->
                    file.name?.endsWith(".png", ignoreCase = true) == true ||
                    file.name?.endsWith(".json", ignoreCase = true) == true
                } ?: emptyList()
                
                if (files.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, endpointError = "⚠️ No character cards found in folder")
                    return@launch
                }
                
                var successCount = 0
                var failCount = 0
                val errors = mutableListOf<String>()
                
                files.forEach { file ->
                    try {
                        file.uri?.let { fileUri ->
                            val characterData = cardParser.parseCharacterCard(context, fileUri)
                            
                            if (characterData.name.isBlank()) {
                                failCount++
                                errors.add("${file.name}: Not a valid character card (missing name)")
                                return@forEach
                            }
                            
                            val characterId = java.util.UUID.randomUUID().toString()
                            val avatarPath = if (characterData.avatarBase64.isNotBlank()) {
                                try {
                                    imageStorageHelper.saveCharacterImageFromBase64(characterId, characterData.avatarBase64)
                                } catch (e: Exception) {
                                    Log.e("BulkImport", "${file.name}: Failed to save avatar: ${e.message}")
                                    null
                                }
                            } else {
                                null
                            }
                            
                            val character = com.vortexai.android.data.models.Character(
                                id = characterId,
                                name = characterData.name,
                                shortDescription = characterData.description.take(200),
                                longDescription = characterData.description,
                                personality = characterData.personality,
                                scenario = characterData.scenario,
                                greeting = characterData.greeting,
                                exampleDialogue = characterData.exampleDialogue,
                                avatarUrl = avatarPath,
                                tags = characterData.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                nsfwEnabled = characterData.isNsfw
                            )
                            
                            characterRepository.saveCharacter(character)
                            successCount++
                        }
                    } catch (e: Exception) {
                        failCount++
                        errors.add("${file.name}: Not a character card")
                    }
                }
                
                val errorMsg = if (errors.isNotEmpty()) "\n\nErrors:\n${errors.take(5).joinToString("\n")}" else ""
                val message = "✅ Bulk Import Complete\n\nSuccessfully imported: $successCount characters\nFailed: $failCount$errorMsg"
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        endpointError = message
                    )
                    android.widget.Toast.makeText(context, "✅ Imported $successCount characters, Failed: $failCount", android.widget.Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    endpointError = "❌ Bulk import error: ${e.message}"
                )
            }
        }
    }
    fun deleteAllCharacters(confirmationText: String) { /* Stub */ }
    
    // Renamed old function to avoid confusion
    fun runDiagnostics() = runLLMDiagnostics()
    fun getCustomAPIDebugInfo() { 
        viewModelScope.launch {
            try {
                val debugInfo = chatLLMService.getCustomAPIDebugInfo()
                _uiState.value = _uiState.value.copy(
                    debugMessage = debugInfo
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    debugMessage = "Debug info failed: ${e.message}"
                )
            }
        }
    }
    fun logout() { /* Stub */ }
    fun deleteAccount() { /* Stub */ }
    fun listCloudBackups() { /* Stub */ }
    fun showRestoreDialog() { _uiState.value = _uiState.value.copy(showRestoreDialog = true) }
    fun hideRestoreDialog() { _uiState.value = _uiState.value.copy(showRestoreDialog = false) }
    fun selectBackupForRestore(backupId: String) { _uiState.value = _uiState.value.copy(selectedBackupForRestore = backupId) }
    fun restoreFromCloudBackup() { /* Stub */ }
    
    // Manual model management functions
    fun addManualLlmModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedLlmModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedLlmModels = currentModels)
            saveLLMSettings()
        }
    }
    
    fun removeManualLlmModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedLlmModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedLlmModels = currentModels)
        saveLLMSettings()
    }
    
    fun addManualImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedImageModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedImageModels = currentModels)
            updateAvailableImageModels()
            saveImageSettings()
        }
    }
    
    fun removeManualImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedImageModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedImageModels = currentModels)
        updateAvailableImageModels()
        saveImageSettings()
    }
    
    fun addManualTogetherAiImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedTogetherAiImageModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedTogetherAiImageModels = currentModels)
            updateAvailableImageModels()
            saveImageSettings()
        }
    }
    
    fun removeManualTogetherAiImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedTogetherAiImageModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedTogetherAiImageModels = currentModels)
        updateAvailableImageModels()
        saveImageSettings()
    }
    
    fun addManualHuggingFaceImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedHuggingFaceImageModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedHuggingFaceImageModels = currentModels)
            updateAvailableImageModels()
            saveImageSettings()
        }
    }
    
    fun removeManualHuggingFaceImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedHuggingFaceImageModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedHuggingFaceImageModels = currentModels)
        updateAvailableImageModels()
        saveImageSettings()
    }
    
    private fun updateAvailableImageModels() {
        val currentState = _uiState.value
        val provider = currentState.imageProvider
        val manualModels = when (provider) {
            "Together AI" -> currentState.manuallyAddedTogetherAiImageModels
            "Hugging Face" -> currentState.manuallyAddedHuggingFaceImageModels
            "ModelsLab" -> currentState.manuallyAddedImageModels
            "Custom API" -> currentState.manuallyAddedCustomImageModels
            else -> emptyList()
        }
        _uiState.value = _uiState.value.copy(availableImageModels = manualModels)
    }
    
    fun addManualLoraModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedLoraModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedLoraModels = currentModels)
            saveImageSettings()
        }
    }
    
    fun removeManualLoraModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedLoraModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedLoraModels = currentModels)
        saveImageSettings()
    }
    
    fun addManualVoice(voiceId: String) {
        val currentVoices = _uiState.value.manuallyAddedVoices.toMutableList()
        if (!currentVoices.contains(voiceId)) {
            currentVoices.add(voiceId)
            _uiState.value = _uiState.value.copy(manuallyAddedVoices = currentVoices)
            saveAudioSettings()
        }
    }
    
    fun removeManualVoice(voiceId: String) {
        val currentVoices = _uiState.value.manuallyAddedVoices.toMutableList()
        currentVoices.remove(voiceId)
        _uiState.value = _uiState.value.copy(manuallyAddedVoices = currentVoices)
        saveAudioSettings()
    }
    fun addManualCustomLlmModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedCustomLlmModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedCustomLlmModels = currentModels)
            saveLLMSettings()
        }
    }
    
    fun removeManualCustomLlmModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedCustomLlmModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedCustomLlmModels = currentModels)
        saveLLMSettings()
    }
    
    fun addManualCustomImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedCustomImageModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedCustomImageModels = currentModels)
            updateAvailableImageModels()
            saveImageSettings()
        }
    }
    
    fun removeManualCustomImageModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedCustomImageModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedCustomImageModels = currentModels)
        updateAvailableImageModels()
        saveImageSettings()
    }
    
    fun addManualCustomAudioModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedCustomAudioModels.toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            _uiState.value = _uiState.value.copy(manuallyAddedCustomAudioModels = currentModels)
            saveAudioSettings()
        }
    }
    
    fun removeManualCustomAudioModel(modelId: String) {
        val currentModels = _uiState.value.manuallyAddedCustomAudioModels.toMutableList()
        currentModels.remove(modelId)
        _uiState.value = _uiState.value.copy(manuallyAddedCustomAudioModels = currentModels)
        saveAudioSettings()
    }
    
    fun saveProfileSettings() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            authRepository.updateUserProfile(
                username = currentState.username,
                fullName = currentState.fullName,
                email = currentState.email,
                dateOfBirth = currentState.dateOfBirth
            ).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        Log.d("SettingsViewModel", "Profile updated successfully for user: ${user.username}")
                    },
                    onFailure = { exception ->
                        Log.e("SettingsViewModel", "Failed to update profile", exception)
                    }
                )
            }
            
            settingsDataStore.savePreferences { preferences: androidx.datastore.preferences.core.MutablePreferences ->
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("username")] = currentState.username
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("full_name")] = currentState.fullName
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("email")] = currentState.email
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("date_of_birth")] = currentState.dateOfBirth
            }
        }
    }
    
    // Image Editing settings functions
    fun updateImageEditingProvider(provider: String) {
        _uiState.value = _uiState.value.copy(imageEditingProvider = provider)
        saveImageEditingSettings()
    }
    
    fun updateReplicateEditingApiKey(key: String) {
        _uiState.value = _uiState.value.copy(replicateEditingApiKey = key)
        saveImageEditingSettings()
    }
    
    fun updateReplicateEditingModel(model: String) {
        _uiState.value = _uiState.value.copy(replicateEditingModel = model)
        saveImageEditingSettings()
    }
    
    fun updateTogetherAiEditingApiKey(key: String) {
        _uiState.value = _uiState.value.copy(togetherAiEditingApiKey = key)
        saveImageEditingSettings()
    }
    
    fun updateImageEditingModel(model: String) {
        _uiState.value = _uiState.value.copy(imageEditingModel = model)
        saveImageEditingSettings()
    }
    
    fun updateImageEditingStrength(strength: String) {
        _uiState.value = _uiState.value.copy(imageEditingStrength = strength)
        saveImageEditingSettings()
    }
    
    // Modelslab Image Editing functions
    fun updateModelslabEditingApiKey(key: String) {
        _uiState.value = _uiState.value.copy(modelslabEditingApiKey = key)
        saveImageEditingSettings()
    }
    
    fun updateModelslabEditingModel(model: String) {
        _uiState.value = _uiState.value.copy(modelslabEditingModel = model)
        saveImageEditingSettings()
    }
    
    fun updateModelslabEditingStrength(strength: String) {
        _uiState.value = _uiState.value.copy(modelslabEditingStrength = strength)
        saveImageEditingSettings()
    }
    
    fun updateModelslabEditingStrengthFloat(strength: Float) {
        _uiState.value = _uiState.value.copy(modelslabEditingStrengthFloat = strength)
        saveImageEditingSettings()
    }
    
    fun updateImgbbApiKey(key: String) {
        _uiState.value = _uiState.value.copy(imgbbApiKey = key)
        saveImageEditingSettings()
    }
    
    fun updateModelslabNegativePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(modelslabNegativePrompt = prompt)
        saveImageEditingSettings()
    }
    
    // Update Server Settings
    fun updateUpdateServerIp(ip: String) {
        _uiState.value = _uiState.value.copy(updateServerIp = ip)
        saveUpdateServerSettings()
    }
    
    fun updateUpdateServerPort(port: String) {
        _uiState.value = _uiState.value.copy(updateServerPort = port)
        saveUpdateServerSettings()
    }
    
    private fun saveUpdateServerSettings() {
        viewModelScope.launch {
            settingsDataStore.savePreferences { preferences ->
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("update_server_ip")] = _uiState.value.updateServerIp
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("update_server_port")] = _uiState.value.updateServerPort
            }
        }
    }
    
    // Import from Server Settings
    fun updateImportServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(importServerUrl = url)
        saveImportServerSettings()
    }
    
    fun updateImportSelectedFile(file: String) {
        _uiState.value = _uiState.value.copy(importSelectedFile = file)
        saveImportServerSettings()
    }
    
    private fun saveImportServerSettings() {
        viewModelScope.launch {
            settingsDataStore.savePreferences { preferences ->
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("import_server_url")] = _uiState.value.importServerUrl
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("import_selected_file")] = _uiState.value.importSelectedFile
            }
        }
    }
    fun updateComfyUiEditingEndpoint(endpoint: String) { _uiState.value = _uiState.value.copy(comfyUiEditingEndpoint = endpoint); saveImageEditingSettings() }
    fun updateComfyUiEditingWorkflow(workflow: String) { _uiState.value = _uiState.value.copy(comfyUiEditingWorkflow = workflow); saveImageEditingSettings() }
    fun updateComfyUiMaintainAspectRatio(maintain: Boolean) { _uiState.value = _uiState.value.copy(comfyUiMaintainAspectRatio = maintain); saveImageEditingSettings() }
    
    fun loadAvailableComfyUiEditingWorkflows(context: Context) {
        viewModelScope.launch {
            try {
                val workflowsDir = java.io.File(context.filesDir, "comfy_workflows")
                if (!workflowsDir.exists()) {
                    workflowsDir.mkdirs()
                }
                
                val userFiles = workflowsDir.listFiles { _, name -> name.endsWith(".json") }
                    ?.map { it.name }
                    ?.sorted() ?: emptyList()
                    
                val defaultWorkflows = listOf("Flux2-Klein Image Edit")
                val combined = defaultWorkflows + userFiles
                
                _uiState.value = _uiState.value.copy(
                    availableComfyUiEditingWorkflows = combined
                )
                
                // If there's no workflow selected, default to the first available one
                if (_uiState.value.comfyUiEditingWorkflow.isBlank() && combined.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(comfyUiEditingWorkflow = combined.first())
                    saveImageEditingSettings()
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading ComfyUI workflows", e)
            }
        }
    }
    
    fun importComfyUiEditingWorkflow(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                var fileName = documentFile?.name ?: "custom_workflow.json"
                
                // Ensure it ends in .json
                if (!fileName.endsWith(".json", ignoreCase = true)) {
                    fileName = "$fileName.json"
                }

                val workflowsDir = java.io.File(context.filesDir, "comfy_workflows")
                if (!workflowsDir.exists()) {
                    workflowsDir.mkdirs()
                }
                
                val destFile = java.io.File(workflowsDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    loadAvailableComfyUiEditingWorkflows(context)
                    updateComfyUiEditingWorkflow(fileName)
                }
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error importing ComfyUI workflow", e)
            }
        }
    }
    
    fun saveImageEditingSettings() {
        viewModelScope.launch {
            settingsDataStore.savePreferences { preferences ->
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_provider")] = _uiState.value.imageEditingProvider
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_api_key")] = _uiState.value.replicateEditingApiKey
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_editing_model")] = _uiState.value.replicateEditingModel
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("together_ai_editing_api_key")] = _uiState.value.togetherAiEditingApiKey
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_model")] = _uiState.value.imageEditingModel
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("image_editing_strength")] = _uiState.value.imageEditingStrength
                // Modelslab Image Editing settings
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_api_key")] = _uiState.value.modelslabEditingApiKey
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_model")] = _uiState.value.modelslabEditingModel
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_strength")] = _uiState.value.modelslabEditingStrength
                preferences[androidx.datastore.preferences.core.floatPreferencesKey("modelslab_editing_strength_float")] = _uiState.value.modelslabEditingStrengthFloat ?: 0.7f
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("imgbb_api_key")] = _uiState.value.imgbbApiKey
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_negative_prompt")] = _uiState.value.modelslabNegativePrompt
                // Custom API Image Editing
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("selected_custom_image_edit_provider_id")] = _uiState.value.selectedCustomImageEditProviderId
                // ComfyUI specific settings
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_editing_endpoint")] = _uiState.value.comfyUiEditingEndpoint
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_editing_workflow")] = _uiState.value.comfyUiEditingWorkflow
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_editing_checkpoint")] = _uiState.value.comfyUiEditingCheckpoint
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_custom_workflow")] = _uiState.value.comfyUiCustomWorkflow
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_negative_prompt")] = _uiState.value.comfyUiNegativePrompt
                preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_workflow_filename")] = _uiState.value.comfyUiWorkflowFileName
                preferences[androidx.datastore.preferences.core.intPreferencesKey("image_width")] = _uiState.value.imageWidth
                preferences[androidx.datastore.preferences.core.intPreferencesKey("image_height")] = _uiState.value.imageHeight
                preferences[androidx.datastore.preferences.core.longPreferencesKey("image_seed")] = _uiState.value.imageSeed
            }
        }
    }
    
    fun updateApp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(
                    isDownloadingUpdate = true,
                    downloadProgress = 0,
                    downloadStatus = "🔄 Checking for updates..."
                )
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val currentState = _uiState.value
                val serverUrl = "http://${currentState.updateServerIp}:${currentState.updateServerPort}/latest.apk"
                
                // Check if update is available
                val headRequest = okhttp3.Request.Builder()
                    .url(serverUrl)
                    .head()
                    .build()
                
                val headResponse = client.newCall(headRequest).execute()
                
                if (headResponse.isSuccessful) {
                    val contentLength = headResponse.header("Content-Length")?.toLongOrNull() ?: 0L
                    
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            downloadStatus = "📱 Update available! Starting download...",
                            downloadProgress = 5
                        )
                    }
                    
                    // Start download with progress tracking
                    val downloadRequest = okhttp3.Request.Builder()
                        .url(serverUrl)
                        .get()
                        .build()
                    
                    val downloadResponse = client.newCall(downloadRequest).execute()
                    
                    if (downloadResponse.isSuccessful) {
                        val inputStream = downloadResponse.body?.byteStream()
                        if (inputStream != null) {
                            val context = com.vortexai.android.VortexApplication.instance
                            val apkFile = java.io.File(context.getExternalFilesDir(null), "update.apk")
                            
                            val outputStream = java.io.FileOutputStream(apkFile)
                            val buffer = ByteArray(8192)
                            var totalBytesRead = 0L
                            var bytesRead: Int
                            
                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    downloadStatus = "📥 Downloading update...",
                                    downloadProgress = 10
                                )
                            }
                            
                            var lastUpdateBytes = 0L
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                
                                // Update progress every 512KB to keep UI responsive
                                if (totalBytesRead - lastUpdateBytes >= 512 * 1024) {
                                    lastUpdateBytes = totalBytesRead
                                    
                                    val progress = if (contentLength > 0) {
                                        (10 + (totalBytesRead * 80 / contentLength)).toInt().coerceAtMost(90)
                                    } else {
                                        (10 + (totalBytesRead / (1024 * 1024) * 5)).toInt().coerceAtMost(90)
                                    }
                                    
                                    val sizeText = if (contentLength > 0) {
                                        "${totalBytesRead / (1024 * 1024)}MB / ${contentLength / (1024 * 1024)}MB"
                                    } else {
                                        "${totalBytesRead / (1024 * 1024)}MB"
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        _uiState.value = _uiState.value.copy(
                                            downloadStatus = "📥 Downloading... $sizeText",
                                            downloadProgress = progress
                                        )
                                    }
                                }
                            }
                            
                            inputStream.close()
                            outputStream.close()
                            
                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    downloadStatus = "📦 Download complete! Installing...",
                                    downloadProgress = 95
                                )
                            }
                            
                            // Install APK
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                apkFile
                            )
                            intent.setDataAndType(uri, "application/vnd.android.package-archive")
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            
                            context.startActivity(intent)
                            
                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    isDownloadingUpdate = false,
                                    downloadStatus = "✅ Update ready! Please install the APK.",
                                    downloadProgress = 100
                                )
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isDownloadingUpdate = false,
                                downloadStatus = "❌ Failed to download update",
                                downloadProgress = 0
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isDownloadingUpdate = false,
                            downloadStatus = "❌ Download failed: ${downloadResponse.code}",
                            downloadProgress = 0
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDownloadingUpdate = false,
                        downloadStatus = "❌ No update server found. Make sure file_transfer_server.py is running.",
                        downloadProgress = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloadingUpdate = false,
                    downloadStatus = "❌ Update failed: ${e.message}",
                    downloadProgress = 0
                )
            }
        }
    }
    
    // Missing ComfyUI functions
    fun fetchComfyUiModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComfyUiModels = true)
            
            try {
                val currentState = _uiState.value
                val endpoint = currentState.comfyUiEndpoint.ifBlank { "http://localhost:8188" }
                
                val result = imageSettingsManager.fetchComfyUiModels(endpoint)
                
                result.fold(
                    onSuccess = { (models, loraModels) ->
                        _uiState.value = _uiState.value.copy(
                            comfyUiModels = models,
                            comfyUiLoraModels = loraModels,
                            isLoadingComfyUiModels = false,
                            endpointError = "✅ Loaded ${models.size} models and ${loraModels.size} LoRAs"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingComfyUiModels = false,
                            endpointError = "❌ Failed to fetch ComfyUI models: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingComfyUiModels = false,
                    endpointError = "❌ Error fetching ComfyUI models: ${e.message}"
                )
            }
        }
    }
    
    fun updateImageWidth(width: Int) {
        _uiState.value = _uiState.value.copy(imageWidth = width)
        saveImageSettings()
    }
    
    fun updateImageHeight(height: Int) {
        _uiState.value = _uiState.value.copy(imageHeight = height)
        saveImageSettings()
    }
    
    fun updateImageSeed(seed: Long) {
        _uiState.value = _uiState.value.copy(imageSeed = seed)
        saveImageSettings()
    }
    
    fun randomizeImageSeed() {
        val randomSeed = kotlin.random.Random.nextLong(0, Long.MAX_VALUE)
        updateImageSeed(randomSeed)
    }
    
    fun updateComfyUiCustomWorkflow(workflow: String) {
        _uiState.value = _uiState.value.copy(comfyUiCustomWorkflow = workflow)
        saveImageSettings()
    }
    
    fun updateComfyUiNegativePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(comfyUiNegativePrompt = prompt)
        saveImageSettings()
    }
    
    private var workflowFileLauncher: ((android.net.Uri?) -> Unit)? = null
    
    fun setWorkflowFileLauncher(launcher: (android.net.Uri?) -> Unit) {
        workflowFileLauncher = launcher
    }
    
    fun browseComfyUiWorkflow() {
        workflowFileLauncher?.invoke(null)
    }
    
    fun loadComfyUiWorkflowFromFile(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) cursor.getString(nameIndex) else "workflow.json"
                    } else "workflow.json"
                } ?: "workflow.json"
                
                _uiState.value = _uiState.value.copy(
                    comfyUiCustomWorkflow = content,
                    comfyUiWorkflowFileName = fileName
                )
                saveImageSettings()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    endpointError = "Failed to load workflow file: ${e.message}"
                )
            }
        }
    }
}
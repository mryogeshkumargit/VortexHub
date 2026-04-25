package com.vortexai.android.ui.screens.image

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.domain.service.ImageGenerationService
import com.vortexai.android.domain.service.ImageGenerationRequest
import com.vortexai.android.domain.service.ImageGenerationResult
import com.vortexai.android.domain.service.ImageGenerationTracker
import com.vortexai.android.domain.service.ImageGenerationSource
import com.vortexai.android.domain.service.SourceType
import com.vortexai.android.domain.service.ImageEditingService
import com.vortexai.android.domain.service.ImageEditingRequest
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.graphics.drawable.toBitmap
import coil.Coil

data class ImageGenerationUiState(
    val isLoading: Boolean = false,
    val generatedImages: List<GeneratedImageData> = emptyList(),
    val errorMessage: String? = null,
    // Edit mode state
    val isEditMode: Boolean = false,
    val selectedImagePath: String? = null,
    // Parameter control state
    val availableParameters: List<com.vortexai.android.data.models.CustomApiParameter> = emptyList(),
    val currentParameterValues: Map<String, Any> = emptyMap(),
    val showParameterPanel: Boolean = false
)

@HiltViewModel
class ImageGenerationViewModel @Inject constructor(
    private val imageGenerationService: ImageGenerationService,
    private val imageEditingService: ImageEditingService,
    private val imageGenerationTracker: ImageGenerationTracker,
    private val dataStore: DataStore<Preferences>,
    private val generatedImageDao: com.vortexai.android.data.database.dao.GeneratedImageDao,
    private val customApiProviderRepository: com.vortexai.android.data.repository.CustomApiProviderRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "ImageGenerationViewModel"
    }
    
    private val _uiState = MutableStateFlow(ImageGenerationUiState())
    val uiState: StateFlow<ImageGenerationUiState> = _uiState.asStateFlow()
    
    init {
        // Load persisted images
        viewModelScope.launch(Dispatchers.IO) {
            val images = generatedImageDao.getAllImages()
            val uiList = images.map {
                GeneratedImageData(
                    id = it.id,
                    prompt = it.prompt,
                    imageUrl = null,
                    imageBase64 = null,
                    model = it.model,
                    generationTime = it.generationTime,
                    size = it.size,
                    timestamp = it.timestamp,
                    localPath = it.localPath
                ).copy()
            }
            _uiState.update { state -> state.copy(generatedImages = uiList) }
        }
        
        // Load available parameters for Custom API provider
        loadAvailableParameters()
    }
    
    fun editImage(prompt: String, inputImagePath: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                // Get image editing settings from DataStore
                val preferences = dataStore.data.first()
                val imageEditingProvider = preferences[SettingsViewModel.IMAGE_EDITING_PROVIDER_KEY] ?: "Together AI"
                
                val apiKey = when (imageEditingProvider) {
                    "Together AI" -> preferences[SettingsViewModel.TOGETHER_AI_EDITING_API_KEY] ?: ""
                    "Replicate" -> preferences[SettingsViewModel.REPLICATE_EDITING_API_KEY] ?: ""
                    "Modelslab" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_api_key")] ?: ""
                    else -> ""
                }
                
                if (apiKey.isBlank()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "No $imageEditingProvider API key configured. Please check your image editing settings."
                        )
                    }
                    return@launch
                }
                
                // Convert local image to base64
                val imageBase64 = convertImageToBase64(inputImagePath)
                if (imageBase64.isBlank()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to process input image. Please try a different image."
                        )
                    }
                    return@launch
                }
                
                // Get model and strength settings
                val editingModel = when (imageEditingProvider) {
                    "Together AI" -> preferences[SettingsViewModel.IMAGE_EDITING_MODEL_KEY] ?: "black-forest-labs/FLUX.1-kontext-dev"
                    "Replicate" -> preferences[SettingsViewModel.REPLICATE_EDITING_MODEL_KEY] ?: "qwen-image-edit"
                    "Modelslab" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("modelslab_editing_model")] ?: "flux-kontext-dev"
                    else -> "black-forest-labs/FLUX.1-kontext-dev"
                }
                
                val editingStrength = when (preferences[SettingsViewModel.IMAGE_EDITING_STRENGTH_KEY] ?: "Medium (0.5)") {
                    "Low (0.3)" -> 0.3f
                    "Medium (0.5)" -> 0.5f
                    "High (0.7)" -> 0.7f
                    "Maximum (0.9)" -> 0.9f
                    else -> 0.5f
                }
                
                val imageEditingRequest = ImageEditingRequest(
                    imageBase64 = imageBase64,
                    prompt = prompt
                )
                
                val result = imageEditingService.editImage(
                    provider = imageEditingProvider,
                    apiKey = apiKey,
                    request = imageEditingRequest,
                    model = editingModel,
                    strength = editingStrength
                )
                
                result.fold(
                    onSuccess = { editResult ->
                        val generatedId = java.util.UUID.randomUUID().toString()

                        // Convert ImageEditingResult to ImageGenerationResult for compatibility
                        val imageResult = ImageGenerationResult(
                            success = editResult.success,
                            imageUrl = editResult.imageUrl,
                            imageBase64 = null,
                            generationTime = editResult.generationTime,
                            model = editingModel
                        )

                        // Save bitmap to internal storage (IO thread)
                        val localPath = saveImageInternal(generatedId, imageResult)

                        val generatedImage = GeneratedImageData(
                            id = generatedId,
                            prompt = "[EDITED] $prompt",
                            imageUrl = null,
                            imageBase64 = null,
                            model = editingModel,
                            generationTime = editResult.generationTime,
                            size = "1024x1024",
                            timestamp = System.currentTimeMillis(),
                            localPath = localPath
                        )

                        // Persist entity
                        viewModelScope.launch(Dispatchers.IO) {
                            generatedImageDao.insertImage(
                                com.vortexai.android.data.models.GeneratedImage(
                                    id = generatedId,
                                    prompt = "[EDITED] $prompt",
                                    localPath = localPath,
                                    model = editingModel,
                                    generationTime = editResult.generationTime,
                                    size = "1024x1024",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                        
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                generatedImages = listOf(generatedImage) + state.generatedImages,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to edit image", exception)
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to edit image: ${exception.message}"
                            )
                        }
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error editing image", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to edit image: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun generateImage(prompt: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                // Get image generation settings from DataStore
                val preferences = dataStore.data.first()
                val imageProvider = preferences[SettingsViewModel.IMAGE_PROVIDER_KEY] ?: "Together AI"
                
                // For Custom API, we need to fetch from database-backed providers
                var apiKey = ""
                var customEndpoint: String? = null
                var customProviderId: String? = null
                
                if (imageProvider == "Custom API") {
                    // Get selected custom provider from database
                    customProviderId = preferences[androidx.datastore.preferences.core.stringPreferencesKey("selected_custom_image_provider_id")] ?: ""
                    val selectedProviderId = customProviderId
                    if (selectedProviderId.isBlank()) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "No custom image provider selected. Please go to Settings → Image Generation and select a custom provider."
                            )
                        }
                        return@launch
                    }
                    
                    val customProvider = customApiProviderRepository.getProviderById(selectedProviderId)
                    if (customProvider == null) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "Custom provider not found. Please reconfigure in Settings → Image Generation."
                            )
                        }
                        return@launch
                    }
                    
                    if (!customProvider.isEnabled) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "Custom provider '${customProvider.name}' is disabled. Please enable it in Settings."
                            )
                        }
                        return@launch
                    }
                    
                    apiKey = customProvider.apiKey
                    customEndpoint = customProvider.baseUrl
                    
                    if (apiKey.isBlank()) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "No API key configured for custom provider '${customProvider.name}'. Please configure it in Settings → Custom API Providers."
                            )
                        }
                        return@launch
                    }
                    
                    if (customEndpoint.isBlank()) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "No endpoint URL configured for custom provider '${customProvider.name}'. Please configure it in Settings → Custom API Providers."
                            )
                        }
                        return@launch
                    }
                    
                    Log.d(TAG, "Using Custom API provider: ${customProvider.name} (ID: $customProviderId), endpoint: $customEndpoint")
                } else {
                    // Standard provider API key retrieval
                    apiKey = when (imageProvider) {
                        "Together AI" -> preferences[SettingsViewModel.TOGETHER_AI_IMAGE_API_KEY] ?: ""
                        "Hugging Face" -> preferences[SettingsViewModel.HUGGINGFACE_IMAGE_API_KEY] ?: ""
                        "ComfyUI" -> preferences[SettingsViewModel.COMFYUI_API_KEY] ?: ""
                        "ModelsLab" -> preferences[SettingsViewModel.MODELSLAB_IMAGE_API_KEY] ?: ""
                        "Replicate" -> preferences[SettingsViewModel.REPLICATE_API_KEY] ?: ""
                        "Grok" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("grok_image_api_key")] ?: ""
                        else -> ""
                    }
                    
                    val requiresApiKey = imageProvider in listOf("Together AI", "Hugging Face", "ModelsLab", "Replicate", "Grok")
                    if (requiresApiKey && apiKey.isBlank()) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "No API key configured for $imageProvider. Please go to Settings → Image Generation and enter your API key."
                            )
                        }
                        return@launch
                    }
                    
                    // For ComfyUI, check endpoint
                    if (imageProvider == "ComfyUI") {
                        customEndpoint = preferences[SettingsViewModel.COMFYUI_ENDPOINT_KEY] ?: ""
                        if (customEndpoint.isBlank()) {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "No endpoint configured for ComfyUI. Please go to Settings → Image Generation and enter your endpoint URL."
                                )
                            }
                            return@launch
                        }
                    }
                }
                
                // Parse image size
                val imageSize = preferences[SettingsViewModel.IMAGE_SIZE_KEY] ?: "1024x1024"
                val sizeParts = imageSize.split("x")
                val width = if (imageProvider == "Replicate") {
                    val w = preferences[androidx.datastore.preferences.core.intPreferencesKey("replicate_width")] ?: 1024
                    Log.d(TAG, "Replicate width from settings: $w")
                    w
                } else {
                    sizeParts.getOrNull(0)?.toIntOrNull() ?: 1024
                }
                val height = if (imageProvider == "Replicate") {
                    val h = preferences[androidx.datastore.preferences.core.intPreferencesKey("replicate_height")] ?: 1024
                    Log.d(TAG, "Replicate height from settings: $h")
                    h
                } else {
                    sizeParts.getOrNull(1)?.toIntOrNull() ?: 1024
                }
                
                val modelsLabWorkflow = preferences[SettingsViewModel.MODELSLAB_WORKFLOW_KEY] ?: "default"
                val loraModelPref = preferences[SettingsViewModel.MODELSLAB_LORA_MODEL_KEY] ?: ""
                val loraStrengthPref = preferences[SettingsViewModel.MODELSLAB_LORA_STRENGTH_KEY] ?: 0.6f
                val negativePromptPref = preferences[SettingsViewModel.NEGATIVE_PROMPT_KEY] ?: ""
                
                // Get LoRA settings for ComfyUI
                val useLora = preferences[SettingsViewModel.USE_LORA_KEY] ?: false
                val comfyUILoraModel = if (imageProvider == "ComfyUI" && useLora) {
                    preferences[androidx.datastore.preferences.core.stringPreferencesKey("comfyui_lora_model")] ?: ""
                } else ""
                val comfyUILoraStrength = if (imageProvider == "ComfyUI" && useLora) {
                    preferences[androidx.datastore.preferences.core.floatPreferencesKey("comfyui_lora_strength")] ?: 1.0f
                } else 1.0f

                val finalNegativePrompt = when (imageProvider) {
                    "Replicate" -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("replicate_negative_prompt")] ?: ""
                    else -> negativePromptPref
                }
                
                // Get model based on provider
                val model = if (imageProvider == "Replicate") {
                    preferences[SettingsViewModel.REPLICATE_MODEL_KEY] ?: "black-forest-labs/flux-schnell"
                } else {
                    preferences[SettingsViewModel.IMAGE_MODEL_KEY] ?: "stabilityai/stable-diffusion-xl-base-1.0"
                }
                
                val imageRequest = ImageGenerationRequest(
                    prompt = prompt,
                    model = model,
                    width = width,
                    height = height,
                    steps = preferences[SettingsViewModel.STEPS_KEY] ?: 20,
                    guidanceScale = preferences[SettingsViewModel.GUIDANCE_SCALE_KEY] ?: 7.5f,
                    workflow = if (imageProvider == "ModelsLab") modelsLabWorkflow else preferences[SettingsViewModel.COMFYUI_WORKFLOW_KEY],
                    loraModel = when {
                        imageProvider == "ComfyUI" && useLora && comfyUILoraModel.isNotBlank() -> comfyUILoraModel
                        imageProvider == "ModelsLab" && loraModelPref.isNotBlank() -> loraModelPref
                        else -> null
                    },
                    loraStrength = when {
                        imageProvider == "ComfyUI" && useLora && comfyUILoraModel.isNotBlank() -> comfyUILoraStrength
                        imageProvider == "ModelsLab" && loraModelPref.isNotBlank() -> loraStrengthPref
                        else -> null
                    },
                    negativePrompt = if (finalNegativePrompt.isNotBlank()) finalNegativePrompt else null
                )
                
                // Use existing customEndpoint (already set for Custom API and ComfyUI above)
                // Only set it here for ComfyUI if it wasn't already set
                if (imageProvider == "ComfyUI" && customEndpoint == null) {
                    customEndpoint = preferences[SettingsViewModel.COMFYUI_ENDPOINT_KEY]
                }
                
                // Generate the image with tracking
                val generationId = "image_tab_${System.currentTimeMillis()}"
                val sourceLocation = ImageGenerationSource(
                    type = SourceType.IMAGE_GENERATION_TAB
                )
                
                // For Custom API: temporarily save UI parameter values so they're used in generation
                if (imageProvider == "Custom API" && customProviderId != null && _uiState.value.currentParameterValues.isNotEmpty()) {
                    try {
                        val models = customApiProviderRepository.getActiveModelsByProvider(customProviderId!!).first()
                        val model = models.firstOrNull()
                        if (model != null) {
                            val paramValues = _uiState.value.currentParameterValues.map { (name, value) ->
                                com.vortexai.android.data.models.CustomApiParameterValue(
                                    modelId = model.id,
                                    paramName = name,
                                    value = value.toString()
                                )
                            }
                            customApiProviderRepository.saveParameterValues(paramValues)
                            Log.d(TAG, "Saved ${paramValues.size} UI parameter values for generation")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save UI parameter values", e)
                    }
                }
                
                val result = imageGenerationService.generateImage(
                    provider = imageProvider,
                    apiKey = apiKey,
                    request = imageRequest,
                    customEndpoint = customEndpoint,
                    generationId = generationId,
                    sourceLocation = sourceLocation,
                    customProviderId = customProviderId
                )
                
                result.fold(
                    onSuccess = { imageResult ->
                        val generatedId = java.util.UUID.randomUUID().toString()

                        // Save bitmap to internal storage (IO thread)
                        val localPath = saveImageInternal(generatedId, imageResult)

                        val generatedImage = GeneratedImageData(
                            id = generatedId,
                            prompt = prompt,
                            imageUrl = null,
                            imageBase64 = null,
                            model = imageResult.model,
                            generationTime = imageResult.generationTime,
                            size = imageSize,
                            timestamp = System.currentTimeMillis(),
                            localPath = localPath
                        )

                        // Persist entity
                        viewModelScope.launch(Dispatchers.IO) {
                            generatedImageDao.insertImage(
                                com.vortexai.android.data.models.GeneratedImage(
                                    id = generatedId,
                                    prompt = prompt,
                                    localPath = localPath,
                                    model = imageResult.model,
                                    generationTime = imageResult.generationTime,
                                    size = imageSize,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                        
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                generatedImages = listOf(generatedImage) + state.generatedImages,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to generate image", exception)
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to generate image: ${exception.message}"
                            )
                        }
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating image", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate image: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Check for completed image generations and display them
     */
    fun checkForCompletedImageGenerations() {
        val sourceLocation = ImageGenerationSource(
            type = SourceType.IMAGE_GENERATION_TAB
        )
        
        val completedGenerations = imageGenerationTracker.getCompletedGenerationsForSource(sourceLocation)
        
        completedGenerations.forEach { result ->
            if (result.success && result.imageUrl != null) {
                val generatedId = java.util.UUID.randomUUID().toString()
                
                // Save bitmap to internal storage (IO thread)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val localPath = saveImageInternal(generatedId, com.vortexai.android.domain.service.ImageGenerationResult(
                            success = true,
                            imageUrl = result.imageUrl,
                            imageBase64 = result.imageBase64,
                            generationTime = result.generationTime,
                            model = result.model
                        ))
                        
                        val generatedImage = GeneratedImageData(
                            id = generatedId,
                            prompt = result.prompt,
                            imageUrl = null,
                            imageBase64 = null,
                            model = result.model,
                            generationTime = result.generationTime,
                            size = "1024x1024", // Default size
                            timestamp = System.currentTimeMillis(),
                            localPath = localPath
                        )
                        
                        // Persist entity
                        generatedImageDao.insertImage(
                            com.vortexai.android.data.models.GeneratedImage(
                                id = generatedId,
                                prompt = result.prompt,
                                localPath = localPath,
                                model = result.model,
                                generationTime = result.generationTime,
                                size = "1024x1024",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        
                        _uiState.update { state ->
                            state.copy(
                                generatedImages = listOf(generatedImage) + state.generatedImages
                            )
                        }
                        
                        Log.d(TAG, "Added completed image generation: ${result.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save completed image generation", e)
                    }
                }
            }
        }
    }
    
    fun removeImage(imageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            generatedImageDao.deleteImageById(imageId)
        }
        _uiState.update { state ->
            state.copy(generatedImages = state.generatedImages.filter { it.id != imageId })
        }
    }

    private suspend fun saveImageInternal(id: String, imageResult: com.vortexai.android.domain.service.ImageGenerationResult): String {
        return withContext(Dispatchers.IO) {
            try {
                val dir = java.io.File(appContext.filesDir, "generated_images")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, "$id.jpg")
                val loader = coil.Coil.imageLoader(appContext)
                val dataSrc = imageResult.imageUrl ?: imageResult.imageBase64
                val request = coil.request.ImageRequest.Builder(appContext).data(dataSrc).build()
                val bitmap = loader.execute(request).drawable?.toBitmap()
                bitmap?.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, java.io.FileOutputStream(file))
                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save image internally", e)
                ""
            }
        }
    }
    
    private suspend fun convertImageToBase64(imagePath: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val file = java.io.File(imagePath)
                if (!file.exists()) {
                    Log.e(TAG, "Image file does not exist: $imagePath")
                    return@withContext ""
                }
                
                val bytes = file.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert image to base64", e)
                ""
            }
        }
    }
    
    // Parameter management functions
    fun loadAvailableParameters() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentEditMode = _uiState.value.isEditMode
                Log.d(TAG, "Loading available parameters... Edit mode: $currentEditMode")
                
                // Get current provider ID from DataStore based on mode
                val preferences = dataStore.data.first()
                val providerKey = if (currentEditMode) {
                    "selected_custom_image_edit_provider_id"  // Load EDITING provider
                } else {
                    "selected_custom_image_provider_id"  // Load GENERATION provider
                }
                
                Log.d(TAG, "Using DataStore key: $providerKey")
                val customProviderId = preferences[androidx.datastore.preferences.core.stringPreferencesKey(providerKey)]
                
                Log.d(TAG, "Custom provider ID from DataStore: $customProviderId")
                
                if (!customProviderId.isNullOrBlank()) {
                    // Get the provider details
                    val provider = customApiProviderRepository.getProviderById(customProviderId)
                    Log.d(TAG, "Provider found: ${provider?.name} (Type: ${if (currentEditMode) "EDITING" else "GENERATION"})")
                    
                    // Get the provider's active models
                    val models = customApiProviderRepository.getActiveModelsByProvider(customProviderId).first()
                    Log.d(TAG, "Found ${models.size} models for provider")
                    
                    val model = models.firstOrNull()
                    
                    if (model != null) {
                        Log.d(TAG, "Using model: ${model.displayName} (ID: ${model.id})")
                        
                        // Fetch parameters for this model
                        val parameters = customApiProviderRepository.getParametersByModel(model.id).first()
                        Log.d(TAG, "Found ${parameters.size} parameters for model")
                        
                        // Fetch current saved parameter values
                        val savedValues = customApiProviderRepository.getParameterValuesMap(model.id)
                        Log.d(TAG, "Loaded ${savedValues.size} saved parameter values")
                        
                        _uiState.update { state ->
                            state.copy(
                                availableParameters = parameters,
                                currentParameterValues = savedValues
                            )
                        }
                        
                        Log.d(TAG, "✅ Successfully loaded ${parameters.size} parameters for ${if (currentEditMode) "editing" else "generation"} model: ${model.displayName}")
                    } else {
                        Log.w(TAG, "No active model found for provider")
                    }
                } else {
                    Log.w(TAG, "No custom provider ID configured in DataStore for ${if (currentEditMode) "editing" else "generation"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load parameters", e)
            }
        }
    }
    
    fun updateParameterValue(name: String, value: Any) {
        _uiState.update { state ->
            state.copy(
                currentParameterValues = state.currentParameterValues + (name to value)
            )
        }
        Log.d(TAG, "Updated parameter: $name = $value")
    }
    
    fun toggleParameterPanel() {
        _uiState.update { state ->
            state.copy(showParameterPanel = !state.showParameterPanel)
        }
    }
    
    fun resetParametersToDefault() {
        viewModelScope.launch(Dispatchers.IO) {
            // Get default values from parameter definitions
            val defaultValues = _uiState.value.availableParameters.associate { param ->
                param.paramName to (param.defaultValue ?: when (param.paramType) {
                    com.vortexai.android.data.models.ParameterType.STRING -> ""
                    com.vortexai.android.data.models.ParameterType.INTEGER -> 0
                    com.vortexai.android.data.models.ParameterType.FLOAT -> 0f
                    com.vortexai.android.data.models.ParameterType.BOOLEAN -> false
                    com.vortexai.android.data.models.ParameterType.ARRAY -> "[]"
                    com.vortexai.android.data.models.ParameterType.OBJECT -> "{}"
                })
            }
            
            _uiState.update { state ->
                state.copy(currentParameterValues = defaultValues)
            }
        }
    }
    
    // Edit mode management
    fun enterEditMode(imagePath: String) {
        Log.d(TAG, "Entering edit mode with image: $imagePath")
        _uiState.update { state ->
            state.copy(
                isEditMode = true,
                selectedImagePath = imagePath
            )
        }
        // Reload parameters for editing provider
        loadAvailableParameters()
    }
    
    fun exitEditMode() {
        Log.d(TAG, "Exiting edit mode")
        _uiState.update { state ->
            state.copy(
                isEditMode = false,
                selectedImagePath = null
            )
        }
        // Reload parameters for generation provider
        loadAvailableParameters()
    }
} 

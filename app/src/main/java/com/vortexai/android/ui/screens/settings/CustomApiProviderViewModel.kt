package com.vortexai.android.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.models.*
import com.vortexai.android.data.repository.CustomApiProviderRepository
import com.vortexai.android.domain.service.CustomApiExecutor
import com.vortexai.android.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomApiProviderUiState(
    val providers: List<CustomApiProvider> = emptyList(),
    val selectedProvider: CustomApiProvider? = null,
    val endpoints: List<CustomApiEndpoint> = emptyList(),
    val models: List<CustomApiModel> = emptyList(),
    val selectedModel: CustomApiModel? = null,
    val parameters: List<CustomApiParameter> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val testResult: TestResult? = null
)

data class TestResult(
    val type: ApiProviderType,
    val textResponse: String? = null,
    val imageUrl: String? = null
)

@HiltViewModel
class CustomApiProviderViewModel @Inject constructor(
    private val repository: CustomApiProviderRepository,
    private val executor: CustomApiExecutor
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CustomApiProviderUiState())
    val uiState: StateFlow<CustomApiProviderUiState> = _uiState.asStateFlow()
    
    fun loadProviders(type: ApiProviderType) {
        viewModelScope.launch {
            repository.getProvidersByType(type).collect { providers ->
                _uiState.update { it.copy(providers = providers) }
            }
        }
    }
    
    fun selectProvider(provider: CustomApiProvider) {
        _uiState.update { it.copy(selectedProvider = provider) }
        loadEndpoints(provider.id)
        loadModels(provider.id)
    }
    
    private fun loadEndpoints(providerId: String) {
        viewModelScope.launch {
            repository.getEndpointsByProvider(providerId).collect { endpoints ->
                _uiState.update { it.copy(endpoints = endpoints) }
            }
        }
    }
    
    private fun loadModels(providerId: String) {
        viewModelScope.launch {
            repository.getModelsByProvider(providerId).collect { models ->
                _uiState.update { it.copy(models = models) }
            }
        }
    }
    
    fun selectModel(model: CustomApiModel) {
        _uiState.update { it.copy(selectedModel = model) }
        loadParameters(model.id)
    }
    
    private fun loadParameters(modelId: String) {
        viewModelScope.launch {
            repository.getParametersByModel(modelId).collect { parameters ->
                _uiState.update { it.copy(parameters = parameters) }
            }
        }
    }
    
    fun saveProvider(
        name: String,
        type: ApiProviderType,
        baseUrl: String,
        apiKey: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val provider = CustomApiProvider(
                    id = IdGenerator.generateSimpleId(),
                    name = name,
                    type = type,
                    baseUrl = baseUrl.trimEnd('/'),
                    apiKey = apiKey
                )
                
                repository.saveProvider(provider)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "Provider saved successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save provider"
                    )
                }
            }
        }
    }
    
    fun updateProvider(provider: CustomApiProvider) {
        viewModelScope.launch {
            try {
                repository.updateProvider(provider)
                _uiState.update { it.copy(successMessage = "Provider updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update provider") }
            }
        }
    }
    
    fun deleteProvider(provider: CustomApiProvider) {
        viewModelScope.launch {
            try {
                repository.deleteProvider(provider)
                _uiState.update { 
                    it.copy(
                        selectedProvider = null,
                        successMessage = "Provider deleted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete provider") }
            }
        }
    }
    
    fun toggleProviderEnabled(provider: CustomApiProvider) {
        viewModelScope.launch {
            try {
                repository.setProviderEnabled(provider.id, !provider.isEnabled)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to toggle provider") }
            }
        }
    }
    
    fun saveEndpoint(
        providerId: String,
        endpointPath: String,
        httpMethod: HttpMethod,
        requestSchema: String,
        responseSchema: String,
        purpose: String
    ) {
        viewModelScope.launch {
            try {
                val endpoint = CustomApiEndpoint(
                    id = IdGenerator.generateSimpleId(),
                    providerId = providerId,
                    endpointPath = endpointPath,
                    httpMethod = httpMethod,
                    requestSchemaJson = requestSchema,
                    responseSchemaJson = responseSchema,
                    purpose = purpose
                )
                
                repository.saveEndpoint(endpoint)
                _uiState.update { it.copy(successMessage = "Endpoint saved") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save endpoint") }
            }
        }
    }
    
    fun deleteEndpoint(endpoint: CustomApiEndpoint) {
        viewModelScope.launch {
            try {
                repository.deleteEndpoint(endpoint)
                _uiState.update { it.copy(successMessage = "Endpoint deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete endpoint") }
            }
        }
    }
    
    fun updateEndpoint(endpoint: CustomApiEndpoint) {
        viewModelScope.launch {
            try {
                repository.updateEndpoint(endpoint)
                _uiState.update { it.copy(successMessage = "Endpoint updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update endpoint") }
            }
        }
    }
    
    fun saveModel(
        providerId: String,
        modelId: String,
        displayName: String,
        capabilities: Map<String, Boolean>
    ) {
        viewModelScope.launch {
            try {
                val model = CustomApiModel(
                    id = IdGenerator.generateSimpleId(),
                    providerId = providerId,
                    modelId = modelId,
                    displayName = displayName,
                    capabilitiesJson = org.json.JSONObject(capabilities).toString()
                )
                
                repository.saveModel(model)
                _uiState.update { it.copy(successMessage = "Model saved") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save model") }
            }
        }
    }
    
    fun deleteModel(model: CustomApiModel) {
        viewModelScope.launch {
            try {
                repository.deleteModel(model)
                _uiState.update { 
                    it.copy(
                        selectedModel = null,
                        successMessage = "Model deleted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete model") }
            }
        }
    }
    
    fun updateModel(model: CustomApiModel) {
        viewModelScope.launch {
            try {
                repository.updateModel(model)
                _uiState.update { it.copy(successMessage = "Model updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update model") }
            }
        }
    }
    
    fun saveParameter(
        modelId: String,
        paramName: String,
        paramType: ParameterType,
        defaultValue: String?,
        minValue: String?,
        maxValue: String?,
        isRequired: Boolean,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                val parameter = CustomApiParameter(
                    id = IdGenerator.generateSimpleId(),
                    modelId = modelId,
                    paramName = paramName,
                    paramType = paramType,
                    defaultValue = defaultValue,
                    minValue = minValue,
                    maxValue = maxValue,
                    isRequired = isRequired,
                    description = description
                )
                
                repository.saveParameter(parameter)
                _uiState.update { it.copy(successMessage = "Parameter saved") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save parameter") }
            }
        }
    }
    
    fun deleteParameter(parameter: CustomApiParameter) {
        viewModelScope.launch {
            try {
                repository.deleteParameter(parameter)
                _uiState.update { it.copy(successMessage = "Parameter deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete parameter") }
            }
        }
    }
    
    fun testConnection(
        provider: CustomApiProvider,
        endpoint: CustomApiEndpoint,
        model: CustomApiModel,
        sourceImageBase64: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, testResult = null) }
                
                val testParams = when (provider.type) {
                    ApiProviderType.TEXT_GENERATION -> mapOf(
                        "messages" to org.json.JSONArray().put(
                            org.json.JSONObject().put("role", "user").put("content", "Say hello")
                        ),
                        "temperature" to 0.7,
                        "max_tokens" to 50
                    )
                    ApiProviderType.IMAGE_GENERATION -> mapOf(
                        "prompt" to "A beautiful sunset over mountains",
                        "num_images" to 1
                    )
                    ApiProviderType.IMAGE_EDITING -> mapOf(
                        "prompt" to "Make it more colorful and vibrant",
                        "image" to (sourceImageBase64 ?: "https://picsum.photos/512")
                    )
                }
                
                val result = executor.executeRequest(provider, endpoint, model, testParams)
                
                result.fold(
                    onSuccess = { response ->
                        val parsed = executor.parseResponse(response, endpoint)
                        parsed.fold(
                            onSuccess = { data ->
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        successMessage = "✅ Connection successful!",
                                        testResult = when (provider.type) {
                                            ApiProviderType.TEXT_GENERATION -> TestResult(provider.type, textResponse = data)
                                            ApiProviderType.IMAGE_GENERATION, ApiProviderType.IMAGE_EDITING -> TestResult(provider.type, imageUrl = data)
                                        }
                                    )
                                }
                            },
                            onFailure = { error ->
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        error = "Response parsing failed: ${error.message}"
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Connection failed: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Test failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null, testResult = null) }
    }
    
    fun importFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val result = com.vortexai.android.utils.CustomApiImporter.importFromJson(jsonString)
                
                result.fold(
                    onSuccess = { importResult ->
                        repository.saveProvider(importResult.provider)
                        importResult.endpoints.forEach { repository.saveEndpoint(it) }
                        importResult.models.forEach { repository.saveModel(it) }
                        importResult.parameters.forEach { repository.saveParameter(it) }
                        
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                successMessage = "✅ Imported: ${importResult.provider.name} with ${importResult.models.size} model(s)"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Import failed"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun getParameterValues(modelId: String): Flow<Map<String, Any>> {
        return repository.getParameterValuesByModel(modelId).map { values ->
            values.associate { pv ->
                pv.paramName to parseValue(pv.value)
            }
        }
    }
    
    fun saveParameterValues(modelId: String, values: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val paramValues = values.map { (name, value) ->
                    CustomApiParameterValue(
                        modelId = modelId,
                        paramName = name,
                        value = value.toString()
                    )
                }
                repository.saveParameterValues(paramValues)
                _uiState.update { it.copy(successMessage = "Parameters saved") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save parameters: ${e.message}") }
            }
        }
    }
    
    private fun parseValue(value: String): Any {
        return when {
            value == "true" || value == "false" -> value.toBoolean()
            value.toIntOrNull() != null -> value.toInt()
            value.toFloatOrNull() != null -> value.toFloat()
            else -> value
        }
    }
}

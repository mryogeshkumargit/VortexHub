package com.vortexai.android.data.repository

import com.vortexai.android.data.database.dao.CustomApiProviderDao
import com.vortexai.android.data.models.*
import com.vortexai.android.utils.ApiKeyEncryption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomApiProviderRepository @Inject constructor(
    private val dao: CustomApiProviderDao
) {
    
    // Provider operations
    fun getProvidersByType(type: ApiProviderType): Flow<List<CustomApiProvider>> =
        dao.getProvidersByType(type).map { providers ->
            providers.map { it.copy(apiKey = ApiKeyEncryption.decrypt(it.apiKey)) }
        }
    
    suspend fun getProviderById(id: String): CustomApiProvider? =
        dao.getProviderById(id)?.let {
            it.copy(apiKey = ApiKeyEncryption.decrypt(it.apiKey))
        }
    
    fun getEnabledProvidersByType(type: ApiProviderType): Flow<List<CustomApiProvider>> =
        dao.getEnabledProvidersByType(type).map { providers ->
            providers.map { it.copy(apiKey = ApiKeyEncryption.decrypt(it.apiKey)) }
        }
    
    suspend fun saveProvider(provider: CustomApiProvider) {
        val encrypted = provider.copy(
            apiKey = ApiKeyEncryption.encrypt(provider.apiKey),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertProvider(encrypted)
    }
    
    suspend fun updateProvider(provider: CustomApiProvider) {
        val encrypted = provider.copy(
            apiKey = ApiKeyEncryption.encrypt(provider.apiKey),
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProvider(encrypted)
    }
    
    suspend fun deleteProvider(provider: CustomApiProvider) =
        dao.deleteProvider(provider)
    
    suspend fun setProviderEnabled(id: String, enabled: Boolean) =
        dao.setProviderEnabled(id, enabled)
    
    // Endpoint operations
    fun getEndpointsByProvider(providerId: String): Flow<List<CustomApiEndpoint>> =
        dao.getEndpointsByProvider(providerId)
    
    suspend fun getEndpointById(id: String): CustomApiEndpoint? =
        dao.getEndpointById(id)
    
    suspend fun getEndpointByPurpose(providerId: String, purpose: String): CustomApiEndpoint? =
        dao.getEndpointByPurpose(providerId, purpose)
    
    suspend fun saveEndpoint(endpoint: CustomApiEndpoint) =
        dao.insertEndpoint(endpoint)
    
    suspend fun updateEndpoint(endpoint: CustomApiEndpoint) =
        dao.updateEndpoint(endpoint)
    
    suspend fun deleteEndpoint(endpoint: CustomApiEndpoint) =
        dao.deleteEndpoint(endpoint)
    
    // Model operations
    fun getModelsByProvider(providerId: String): Flow<List<CustomApiModel>> =
        dao.getModelsByProvider(providerId)
    
    suspend fun getModelById(id: String): CustomApiModel? =
        dao.getModelById(id)
    
    fun getActiveModelsByProvider(providerId: String): Flow<List<CustomApiModel>> =
        dao.getActiveModelsByProvider(providerId)
    
    suspend fun saveModel(model: CustomApiModel) =
        dao.insertModel(model)
    
    suspend fun updateModel(model: CustomApiModel) =
        dao.updateModel(model)
    
    suspend fun deleteModel(model: CustomApiModel) =
        dao.deleteModel(model)
    
    suspend fun setModelActive(id: String, active: Boolean) =
        dao.setModelActive(id, active)
    
    // Parameter operations
    fun getParametersByModel(modelId: String): Flow<List<CustomApiParameter>> =
        dao.getParametersByModel(modelId)
    
    suspend fun getParameterById(id: String): CustomApiParameter? =
        dao.getParameterById(id)
    
    suspend fun saveParameter(parameter: CustomApiParameter) =
        dao.insertParameter(parameter)
    
    suspend fun updateParameter(parameter: CustomApiParameter) =
        dao.updateParameter(parameter)
    
    suspend fun deleteParameter(parameter: CustomApiParameter) =
        dao.deleteParameter(parameter)
    
    suspend fun deleteParametersByModel(modelId: String) =
        dao.deleteParametersByModel(modelId)
    
    // Batch operations
    suspend fun saveParameters(parameters: List<CustomApiParameter>) =
        dao.insertParameters(parameters)
    
    suspend fun saveModels(models: List<CustomApiModel>) =
        dao.insertModels(models)
    
    // Parameter value operations
    fun getParameterValuesByModel(modelId: String): Flow<List<CustomApiParameterValue>> =
        dao.getParameterValuesByModel(modelId)
    
    suspend fun getParameterValue(modelId: String, paramName: String): CustomApiParameterValue? =
        dao.getParameterValue(modelId, paramName)
    
    suspend fun saveParameterValue(value: CustomApiParameterValue) =
        dao.insertParameterValue(value)
    
    suspend fun saveParameterValues(values: List<CustomApiParameterValue>) =
        dao.insertParameterValues(values)
    
    suspend fun deleteParameterValuesByModel(modelId: String) =
        dao.deleteParameterValuesByModel(modelId)
    
    // Get parameter values as map
    suspend fun getParameterValuesMap(modelId: String): Map<String, Any> {
        val values = mutableMapOf<String, Any>()
        // Use .first() to get first emission from Flow and complete
        // .collect() would hang forever waiting for Flow to complete
        val paramValues = dao.getParameterValuesByModel(modelId).first()
        paramValues.forEach { pv ->
            values[pv.paramName] = parseParameterValue(pv.value)
        }
        return values
    }
    
    private fun parseParameterValue(value: String): Any {
        return when {
            value == "true" || value == "false" -> value.toBoolean()
            value.toIntOrNull() != null -> value.toInt()
            value.toFloatOrNull() != null -> value.toFloat()
            value.startsWith("[") || value.startsWith("{") -> value // JSON
            else -> value
        }
    }
}

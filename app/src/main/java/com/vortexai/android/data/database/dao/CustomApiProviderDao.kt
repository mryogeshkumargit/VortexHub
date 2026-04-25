package com.vortexai.android.data.database.dao

import androidx.room.*
import com.vortexai.android.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomApiProviderDao {
    
    // Provider operations
    @Query("SELECT * FROM custom_api_providers WHERE type = :type ORDER BY name ASC")
    fun getProvidersByType(type: ApiProviderType): Flow<List<CustomApiProvider>>
    
    @Query("SELECT * FROM custom_api_providers WHERE id = :id")
    suspend fun getProviderById(id: String): CustomApiProvider?
    
    @Query("SELECT * FROM custom_api_providers WHERE type = :type AND isEnabled = 1")
    fun getEnabledProvidersByType(type: ApiProviderType): Flow<List<CustomApiProvider>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: CustomApiProvider)
    
    @Update
    suspend fun updateProvider(provider: CustomApiProvider)
    
    @Delete
    suspend fun deleteProvider(provider: CustomApiProvider)
    
    @Query("UPDATE custom_api_providers SET isEnabled = :enabled WHERE id = :id")
    suspend fun setProviderEnabled(id: String, enabled: Boolean)
    
    // Endpoint operations
    @Query("SELECT * FROM custom_api_endpoints WHERE providerId = :providerId")
    fun getEndpointsByProvider(providerId: String): Flow<List<CustomApiEndpoint>>
    
    @Query("SELECT * FROM custom_api_endpoints WHERE id = :id")
    suspend fun getEndpointById(id: String): CustomApiEndpoint?
    
    @Query("SELECT * FROM custom_api_endpoints WHERE providerId = :providerId AND purpose = :purpose LIMIT 1")
    suspend fun getEndpointByPurpose(providerId: String, purpose: String): CustomApiEndpoint?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: CustomApiEndpoint)
    
    @Update
    suspend fun updateEndpoint(endpoint: CustomApiEndpoint)
    
    @Delete
    suspend fun deleteEndpoint(endpoint: CustomApiEndpoint)
    
    // Model operations
    @Query("SELECT * FROM custom_api_models WHERE providerId = :providerId ORDER BY displayName ASC")
    fun getModelsByProvider(providerId: String): Flow<List<CustomApiModel>>
    
    @Query("SELECT * FROM custom_api_models WHERE id = :id")
    suspend fun getModelById(id: String): CustomApiModel?
    
    @Query("SELECT * FROM custom_api_models WHERE providerId = :providerId AND isActive = 1")
    fun getActiveModelsByProvider(providerId: String): Flow<List<CustomApiModel>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: CustomApiModel)
    
    @Update
    suspend fun updateModel(model: CustomApiModel)
    
    @Delete
    suspend fun deleteModel(model: CustomApiModel)
    
    @Query("UPDATE custom_api_models SET isActive = :active WHERE id = :id")
    suspend fun setModelActive(id: String, active: Boolean)
    
    // Parameter operations
    @Query("SELECT * FROM custom_api_parameters WHERE modelId = :modelId ORDER BY paramName ASC")
    fun getParametersByModel(modelId: String): Flow<List<CustomApiParameter>>
    
    @Query("SELECT * FROM custom_api_parameters WHERE id = :id")
    suspend fun getParameterById(id: String): CustomApiParameter?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParameter(parameter: CustomApiParameter)
    
    @Update
    suspend fun updateParameter(parameter: CustomApiParameter)
    
    @Delete
    suspend fun deleteParameter(parameter: CustomApiParameter)
    
    @Query("DELETE FROM custom_api_parameters WHERE modelId = :modelId")
    suspend fun deleteParametersByModel(modelId: String)
    
    // Batch operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParameters(parameters: List<CustomApiParameter>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<CustomApiModel>)
    
    // Parameter value operations
    @Query("SELECT * FROM custom_api_parameter_values WHERE modelId = :modelId")
    fun getParameterValuesByModel(modelId: String): Flow<List<CustomApiParameterValue>>
    
    @Query("SELECT * FROM custom_api_parameter_values WHERE modelId = :modelId AND paramName = :paramName LIMIT 1")
    suspend fun getParameterValue(modelId: String, paramName: String): CustomApiParameterValue?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParameterValue(value: CustomApiParameterValue)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParameterValues(values: List<CustomApiParameterValue>)
    
    @Query("DELETE FROM custom_api_parameter_values WHERE modelId = :modelId")
    suspend fun deleteParameterValuesByModel(modelId: String)
}

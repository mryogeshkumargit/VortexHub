package com.vortexai.android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

enum class ApiProviderType {
    TEXT_GENERATION,
    IMAGE_GENERATION,
    IMAGE_EDITING
}

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

enum class ParameterType {
    STRING, INTEGER, FLOAT, BOOLEAN, ARRAY, OBJECT
}

@Entity(
    tableName = "custom_api_providers",
    indices = [Index(value = ["type"])]
)
data class CustomApiProvider(
    @PrimaryKey val id: String,
    val name: String,
    val type: ApiProviderType,
    val baseUrl: String,
    val apiKey: String, // Encrypted
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_api_endpoints",
    foreignKeys = [
        ForeignKey(
            entity = CustomApiProvider::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["providerId"])]
)
data class CustomApiEndpoint(
    @PrimaryKey val id: String,
    val providerId: String,
    val endpointPath: String,
    val httpMethod: HttpMethod,
    val requestSchemaJson: String, // JSON schema
    val responseSchemaJson: String, // JSON schema
    val purpose: String, // "chat", "image_gen", "image_edit"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_api_models",
    foreignKeys = [
        ForeignKey(
            entity = CustomApiProvider::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["providerId"])]
)
data class CustomApiModel(
    @PrimaryKey val id: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val capabilitiesJson: String, // JSON: {"streaming": true, "vision": false}
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_api_parameters",
    foreignKeys = [
        ForeignKey(
            entity = CustomApiModel::class,
            parentColumns = ["id"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["modelId"])]
)
data class CustomApiParameter(
    @PrimaryKey val id: String,
    val modelId: String,
    val paramName: String,
    val paramType: ParameterType,
    val defaultValue: String?,
    val minValue: String?,
    val maxValue: String?,
    val isRequired: Boolean = false,
    val description: String? = null
)

// Data classes for UI
data class ProviderWithEndpoints(
    val provider: CustomApiProvider,
    val endpoints: List<CustomApiEndpoint>
)

data class ProviderWithModels(
    val provider: CustomApiProvider,
    val models: List<CustomApiModel>
)

data class ModelWithParameters(
    val model: CustomApiModel,
    val parameters: List<CustomApiParameter>
)

// Schema data classes
data class RequestSchema(
    val headers: Map<String, String>,
    val body: Map<String, String>,
    val parameterMapping: Map<String, String>
)

data class ResponseSchema(
    val dataPath: String?,
    val streamingPath: String?,
    val errorPath: String?,
    val imageUrlPath: String?,
    val statusPath: String?
)

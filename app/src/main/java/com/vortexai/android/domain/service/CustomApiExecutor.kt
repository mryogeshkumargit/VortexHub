package com.vortexai.android.domain.service

import com.vortexai.android.data.models.*
import com.vortexai.android.utils.ApiSchemaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomApiExecutor @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    suspend fun executeRequest(
        provider: CustomApiProvider,
        endpoint: CustomApiEndpoint,
        model: CustomApiModel,
        parameters: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("CustomApiExecutor", ">>> executeRequest STARTED <<<")
            android.util.Log.d("CustomApiExecutor", "Provider: ${provider.name}, Endpoint: ${endpoint.endpointPath}")
            
            val requestSchema = ApiSchemaParser.parseRequestSchema(endpoint.requestSchemaJson)
                ?: return@withContext Result.failure(Exception("Invalid request schema"))
            
            android.util.Log.d("CustomApiExecutor", "Request schema parsed successfully")
            
            val url = buildUrl(provider.baseUrl, endpoint.endpointPath)
            val headers = buildHeaders(requestSchema.headers, provider.apiKey, parameters)
            val body = buildBody(requestSchema.body, model.modelId, parameters)
            
            // Debug logging
            android.util.Log.d("CustomApiExecutor", "=== Custom API Request Debug ===")
            android.util.Log.d("CustomApiExecutor", "URL: $url")
            android.util.Log.d("CustomApiExecutor", "API Key length: ${provider.apiKey.length}, starts with: ${provider.apiKey.take(8)}...")
            android.util.Log.d("CustomApiExecutor", "Headers: $headers")
            android.util.Log.d("CustomApiExecutor", "Body: $body")
            
            val request = Request.Builder()
                .url(url)
                .apply {
                    headers.forEach { (key, value) -> addHeader(key, value) }
                }
                .method(
                    endpoint.httpMethod.name,
                    if (endpoint.httpMethod == HttpMethod.POST || endpoint.httpMethod == HttpMethod.PUT) {
                        body.toRequestBody("application/json".toMediaType())
                    } else null
                )
                .build()
            
            android.util.Log.d("CustomApiExecutor", "Starting HTTP request to $url...")
            val response = client.newCall(request).execute()
            android.util.Log.d("CustomApiExecutor", "HTTP request completed")
            val responseBody = response.body?.string() ?: ""
            
            android.util.Log.d("CustomApiExecutor", "Response code: ${response.code}")
            android.util.Log.d("CustomApiExecutor", "Response: ${responseBody.take(500)}")
            
            if (response.isSuccessful) {
                Result.success(responseBody)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomApiExecutor", "Request failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun parseResponse(
        responseJson: String,
        endpoint: CustomApiEndpoint
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val responseSchema = ApiSchemaParser.parseResponseSchema(endpoint.responseSchemaJson)
                ?: return@withContext Result.failure(Exception("Invalid response schema"))
            
            // Check for error first
            responseSchema.errorPath?.let { errorPath ->
                ApiSchemaParser.extractValueFromPath(responseJson, errorPath)?.let { error ->
                    return@withContext Result.failure(Exception(error))
                }
            }
            
            // Extract data based on purpose
            val dataPath = when (endpoint.purpose) {
                "chat" -> responseSchema.dataPath
                "image_gen", "image_edit" -> responseSchema.imageUrlPath
                else -> responseSchema.dataPath
            }
            
            val data = ApiSchemaParser.extractValueFromPath(responseJson, dataPath)
                ?: return@withContext Result.failure(Exception("Failed to extract data from response"))
            
            Result.success(data)
        } catch (e: Exception) {
            android.util.Log.e("CustomApiExecutor", "Response parsing failed", e)
            Result.failure(e)
        }
    }
    
    private fun buildUrl(baseUrl: String, endpointPath: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = endpointPath.trimStart('/')
        return "$cleanBase/$cleanPath"
    }
    
    private fun buildHeaders(
        headerTemplate: Map<String, String>,
        apiKey: String,
        parameters: Map<String, Any>
    ): Map<String, String> {
        val values = parameters.toMutableMap()
        values["apiKey"] = apiKey
        
        return headerTemplate.mapValues { (_, template) ->
            ApiSchemaParser.replacePlaceholders(template, values)
        }
    }
    
    private fun buildBody(
        bodyTemplate: Map<String, String>,
        modelId: String,
        parameters: Map<String, Any>
    ): String {
        val values = parameters.toMutableMap()
        values["modelId"] = modelId
        
        val bodyObj = JSONObject()
        bodyTemplate.forEach { (key, template) ->
            val value = when {
                template.startsWith("{") && template.endsWith("}") -> {
                    val paramName = template.substring(1, template.length - 1)
                    parameters[paramName] ?: template
                }
                else -> ApiSchemaParser.replacePlaceholders(template, values)
            }
            
            // Handle different types
            when (value) {
                is JSONObject, is JSONArray -> bodyObj.put(key, value)
                is Number -> bodyObj.put(key, value)
                is Boolean -> bodyObj.put(key, value)
                is String -> {
                    // Try to parse as JSON if it looks like JSON
                    if (value.startsWith("{") || value.startsWith("[")) {
                        try {
                            if (value.startsWith("{")) {
                                bodyObj.put(key, JSONObject(value))
                            } else {
                                bodyObj.put(key, JSONArray(value))
                            }
                        } catch (e: Exception) {
                            bodyObj.put(key, value)
                        }
                    } else {
                        bodyObj.put(key, value)
                    }
                }
                else -> bodyObj.put(key, value.toString())
            }
        }
        
        return bodyObj.toString()
    }
    
    fun validateParameters(
        parameters: List<CustomApiParameter>,
        values: Map<String, Any>
    ): Result<Unit> {
        parameters.forEach { param ->
            if (param.isRequired && !values.containsKey(param.paramName)) {
                return Result.failure(Exception("Required parameter missing: ${param.paramName}"))
            }
            
            values[param.paramName]?.let { value ->
                when (param.paramType) {
                    ParameterType.INTEGER -> {
                        val intValue = value.toString().toIntOrNull()
                            ?: return Result.failure(Exception("${param.paramName} must be an integer"))
                        
                        param.minValue?.toIntOrNull()?.let { min ->
                            if (intValue < min) {
                                return Result.failure(Exception("${param.paramName} must be >= $min"))
                            }
                        }
                        param.maxValue?.toIntOrNull()?.let { max ->
                            if (intValue > max) {
                                return Result.failure(Exception("${param.paramName} must be <= $max"))
                            }
                        }
                    }
                    ParameterType.FLOAT -> {
                        val floatValue = value.toString().toFloatOrNull()
                            ?: return Result.failure(Exception("${param.paramName} must be a number"))
                        
                        param.minValue?.toFloatOrNull()?.let { min ->
                            if (floatValue < min) {
                                return Result.failure(Exception("${param.paramName} must be >= $min"))
                            }
                        }
                        param.maxValue?.toFloatOrNull()?.let { max ->
                            if (floatValue > max) {
                                return Result.failure(Exception("${param.paramName} must be <= $max"))
                            }
                        }
                    }
                    ParameterType.BOOLEAN -> {
                        if (value !is Boolean && value.toString().lowercase() !in listOf("true", "false")) {
                            return Result.failure(Exception("${param.paramName} must be true or false"))
                        }
                    }
                    else -> {} // STRING, ARRAY, OBJECT - no validation
                }
            }
        }
        
        return Result.success(Unit)
    }
}

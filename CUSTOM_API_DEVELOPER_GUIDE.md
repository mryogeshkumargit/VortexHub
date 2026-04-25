# Custom API System - Developer Integration Guide

## Overview

This guide explains how to integrate the custom API system with new providers or extend existing functionality.

## Architecture

### Component Hierarchy

```
┌─────────────────────────────────────┐
│   CustomApiProviderScreen (UI)     │
│   - Add/Edit/Delete Providers      │
│   - Configure Endpoints & Models   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  CustomApiProviderViewModel         │
│  - Business Logic                   │
│  - State Management                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  CustomApiProviderRepository        │
│  - Data Access Layer                │
│  - CRUD Operations                  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  CustomApiProviderDao (Room)        │
│  - Database Operations              │
│  - Queries                          │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Room Database                      │
│  - Persistent Storage               │
└─────────────────────────────────────┘
```

### Data Flow

```
User Input → UI → ViewModel → Repository → DAO → Database
                                    ↓
                            CustomApiExecutor
                                    ↓
                              HTTP Client
                                    ↓
                              External API
```

## Data Models

### CustomApiProvider

```kotlin
@Entity(tableName = "custom_api_providers")
data class CustomApiProvider(
    @PrimaryKey val id: String,
    val name: String,
    val type: ApiProviderType,  // TEXT_GENERATION, IMAGE_GENERATION, IMAGE_EDITING
    val baseUrl: String,
    val apiKey: String,         // Encrypted
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### CustomApiEndpoint

```kotlin
@Entity(tableName = "custom_api_endpoints")
data class CustomApiEndpoint(
    @PrimaryKey val id: String,
    val providerId: String,
    val endpointPath: String,
    val httpMethod: HttpMethod,
    val requestSchemaJson: String,
    val responseSchemaJson: String,
    val purpose: String,        // "chat", "image_gen", "image_edit"
    val createdAt: Long = System.currentTimeMillis()
)
```

### CustomApiModel

```kotlin
@Entity(tableName = "custom_api_models")
data class CustomApiModel(
    @PrimaryKey val id: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val capabilitiesJson: String,  // {"streaming": true, "vision": false}
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

## Integration Steps

### 1. Add New Provider Type

If adding a new provider type (e.g., AUDIO_GENERATION):

```kotlin
// In CustomApiEntities.kt
enum class ApiProviderType {
    TEXT_GENERATION,
    IMAGE_GENERATION,
    IMAGE_EDITING,
    AUDIO_GENERATION  // New type
}
```

### 2. Create Provider Service

```kotlin
@Singleton
class CustomAudioProvider @Inject constructor(
    private val repository: CustomApiProviderRepository,
    private val executor: CustomApiExecutor
) {
    suspend fun generateAudio(
        providerId: String,
        prompt: String,
        params: Map<String, Any>
    ): Result<ByteArray> {
        // 1. Get provider configuration
        val provider = repository.getProviderById(providerId)
            ?: return Result.failure(Exception("Provider not found"))
        
        // 2. Get endpoint
        val endpoint = repository.getEndpointsByProvider(providerId)
            .first()
            .firstOrNull { it.purpose == "audio_gen" }
            ?: return Result.failure(Exception("No audio endpoint found"))
        
        // 3. Get model
        val model = repository.getModelsByProvider(providerId)
            .first()
            .firstOrNull { it.isActive }
            ?: return Result.failure(Exception("No active model found"))
        
        // 4. Build request parameters
        val requestParams = buildMap {
            put("prompt", prompt)
            put("model", model.modelId)
            putAll(params)
        }
        
        // 5. Execute request
        return executor.executeRequest(provider, endpoint, model, requestParams)
            .mapCatching { response ->
                // Parse response and extract audio data
                parseAudioResponse(response, endpoint)
            }
    }
    
    private fun parseAudioResponse(response: String, endpoint: CustomApiEndpoint): ByteArray {
        // Parse response using endpoint's response schema
        val schema = parseResponseSchema(endpoint.responseSchemaJson)
        val audioUrl = extractValue(response, schema.audioUrlPath)
        
        // Download audio from URL
        return downloadAudio(audioUrl)
    }
}
```

### 3. Add UI Configuration

```kotlin
// In SettingsScreen.kt or new tab
@Composable
fun AudioGenerationTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigateToCustomApi: () -> Unit
) {
    LazyColumn {
        item {
            Button(
                onClick = { onNavigateToCustomApi() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Api, null)
                Spacer(Modifier.width(8.dp))
                Text("Configure Custom Audio Generation APIs")
            }
        }
        
        // Provider selection dropdown
        item {
            SettingsDropdownItem(
                title = "Audio Provider",
                selectedValue = uiState.audioProvider,
                options = getAudioProviders(),
                onValueChange = viewModel::updateAudioProvider
            )
        }
    }
}
```

### 4. Add Schema Templates

```kotlin
// In SchemaTemplates.kt
object SchemaTemplates {
    fun getAudioGenerationTemplates(): List<Template> {
        return listOf(
            Template(
                name = "ElevenLabs TTS",
                endpointPath = "/v1/text-to-speech/{voice_id}",
                requestSchema = """
                    {
                      "headers": {
                        "xi-api-key": "{{apiKey}}"
                      },
                      "body": {
                        "text": "{{prompt}}",
                        "model_id": "{{model}}",
                        "voice_settings": {
                          "stability": "{{stability}}",
                          "similarity_boost": "{{similarity}}"
                        }
                      }
                    }
                """.trimIndent(),
                responseSchema = """
                    {
                      "audioDataPath": "audio_base64",
                      "errorPath": "error.message"
                    }
                """.trimIndent()
            ),
            Template(
                name = "OpenAI TTS",
                endpointPath = "/v1/audio/speech",
                requestSchema = """
                    {
                      "headers": {
                        "Authorization": "Bearer {{apiKey}}"
                      },
                      "body": {
                        "model": "{{model}}",
                        "input": "{{prompt}}",
                        "voice": "{{voice}}"
                      }
                    }
                """.trimIndent(),
                responseSchema = """
                    {
                      "audioDataPath": "data",
                      "errorPath": "error.message"
                    }
                """.trimIndent()
            )
        )
    }
}
```

### 5. Implement CustomApiExecutor Integration

```kotlin
// In CustomApiExecutor.kt
class CustomApiExecutor @Inject constructor(
    private val httpClient: OkHttpClient
) {
    suspend fun executeRequest(
        provider: CustomApiProvider,
        endpoint: CustomApiEndpoint,
        model: CustomApiModel,
        parameters: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Parse request schema
            val requestSchema = parseRequestSchema(endpoint.requestSchemaJson)
            
            // 2. Build request
            val request = buildRequest(
                baseUrl = provider.baseUrl,
                endpointPath = endpoint.endpointPath,
                httpMethod = endpoint.httpMethod,
                headers = buildHeaders(requestSchema, provider, parameters),
                body = buildBody(requestSchema, model, parameters)
            )
            
            // 3. Execute request
            val response = httpClient.newCall(request).execute()
            
            // 4. Handle response
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("HTTP ${response.code}: ${response.message}")
                )
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            
            Result.success(responseBody)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildHeaders(
        schema: RequestSchema,
        provider: CustomApiProvider,
        parameters: Map<String, Any>
    ): Map<String, String> {
        return schema.headers.mapValues { (_, value) ->
            replacePlaceholders(value, provider, parameters)
        }
    }
    
    private fun buildBody(
        schema: RequestSchema,
        model: CustomApiModel,
        parameters: Map<String, Any>
    ): String {
        val bodyMap = schema.body.mapValues { (_, value) ->
            replacePlaceholders(value, model, parameters)
        }
        return JSONObject(bodyMap).toString()
    }
    
    private fun replacePlaceholders(
        template: String,
        provider: CustomApiProvider,
        parameters: Map<String, Any>
    ): String {
        var result = template
        
        // Replace {{apiKey}}
        result = result.replace("{{apiKey}}", provider.apiKey)
        
        // Replace {{model}}
        if (parameters.containsKey("model")) {
            result = result.replace("{{model}}", parameters["model"].toString())
        }
        
        // Replace other parameters
        parameters.forEach { (key, value) ->
            result = result.replace("{{$key}}", value.toString())
        }
        
        return result
    }
}
```

## Testing

### Unit Tests

```kotlin
@Test
fun `test custom API provider creation`() = runTest {
    // Arrange
    val provider = CustomApiProvider(
        id = "test-id",
        name = "Test Provider",
        type = ApiProviderType.TEXT_GENERATION,
        baseUrl = "https://api.test.com",
        apiKey = "test-key"
    )
    
    // Act
    repository.saveProvider(provider)
    val saved = repository.getProviderById("test-id")
    
    // Assert
    assertEquals(provider.name, saved?.name)
    assertEquals(provider.baseUrl, saved?.baseUrl)
}

@Test
fun `test endpoint configuration`() = runTest {
    // Arrange
    val endpoint = CustomApiEndpoint(
        id = "endpoint-id",
        providerId = "test-id",
        endpointPath = "/v1/chat/completions",
        httpMethod = HttpMethod.POST,
        requestSchemaJson = "{}",
        responseSchemaJson = "{}",
        purpose = "chat"
    )
    
    // Act
    repository.saveEndpoint(endpoint)
    val endpoints = repository.getEndpointsByProvider("test-id").first()
    
    // Assert
    assertTrue(endpoints.isNotEmpty())
    assertEquals(endpoint.endpointPath, endpoints.first().endpointPath)
}
```

### Integration Tests

```kotlin
@Test
fun `test API call execution`() = runTest {
    // Arrange
    val provider = createTestProvider()
    val endpoint = createTestEndpoint()
    val model = createTestModel()
    val params = mapOf("prompt" to "test")
    
    // Act
    val result = executor.executeRequest(provider, endpoint, model, params)
    
    // Assert
    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
}
```

## Best Practices

### 1. Error Handling

```kotlin
suspend fun makeApiCall(): Result<String> {
    return try {
        // API call logic
        Result.success(response)
    } catch (e: IOException) {
        Result.failure(Exception("Network error: ${e.message}"))
    } catch (e: JsonException) {
        Result.failure(Exception("Invalid response format: ${e.message}"))
    } catch (e: Exception) {
        Result.failure(Exception("Unexpected error: ${e.message}"))
    }
}
```

### 2. Schema Validation

```kotlin
fun validateRequestSchema(schema: String): Boolean {
    return try {
        val json = JSONObject(schema)
        json.has("headers") && json.has("body")
    } catch (e: Exception) {
        false
    }
}
```

### 3. Placeholder Replacement

```kotlin
fun replacePlaceholders(
    template: String,
    values: Map<String, Any>
): String {
    var result = template
    values.forEach { (key, value) ->
        result = result.replace("{{$key}}", value.toString())
    }
    return result
}
```

### 4. Response Parsing

```kotlin
fun parseResponse(
    response: String,
    schema: ResponseSchema
): String {
    val json = JSONObject(response)
    return extractValue(json, schema.dataPath)
}

fun extractValue(json: JSONObject, path: String): String {
    val parts = path.split(".")
    var current: Any = json
    
    for (part in parts) {
        current = when {
            part.contains("[") -> {
                val arrayName = part.substringBefore("[")
                val index = part.substringAfter("[").substringBefore("]").toInt()
                (current as JSONObject).getJSONArray(arrayName).get(index)
            }
            else -> (current as JSONObject).get(part)
        }
    }
    
    return current.toString()
}
```

## Security Considerations

### 1. API Key Encryption

```kotlin
// Encrypt API keys before storing
fun encryptApiKey(key: String): String {
    // Use Android Keystore or similar
    return EncryptionManager.encrypt(key)
}

// Decrypt when needed
fun decryptApiKey(encrypted: String): String {
    return EncryptionManager.decrypt(encrypted)
}
```

### 2. Secure Storage

```kotlin
// Store sensitive data in encrypted database
@Entity(tableName = "custom_api_providers")
data class CustomApiProvider(
    // ...
    @ColumnInfo(name = "api_key")
    val apiKey: String  // Encrypted before storage
)
```

### 3. Network Security

```kotlin
// Use HTTPS only
val httpClient = OkHttpClient.Builder()
    .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
    .build()
```

## Performance Optimization

### 1. Caching

```kotlin
class CustomApiCache {
    private val cache = LruCache<String, String>(100)
    
    fun get(key: String): String? = cache.get(key)
    fun put(key: String, value: String) = cache.put(key, value)
}
```

### 2. Connection Pooling

```kotlin
val httpClient = OkHttpClient.Builder()
    .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
    .build()
```

### 3. Request Batching

```kotlin
suspend fun batchRequests(
    requests: List<Request>
): List<Result<String>> {
    return requests.map { request ->
        async { executeRequest(request) }
    }.awaitAll()
}
```

## Debugging

### 1. Logging

```kotlin
private fun logRequest(request: Request) {
    Timber.d("=== API Request ===")
    Timber.d("URL: ${request.url}")
    Timber.d("Method: ${request.method}")
    Timber.d("Headers: ${request.headers}")
    Timber.d("Body: ${request.body}")
}

private fun logResponse(response: Response) {
    Timber.d("=== API Response ===")
    Timber.d("Code: ${response.code}")
    Timber.d("Message: ${response.message}")
    Timber.d("Body: ${response.body?.string()}")
}
```

### 2. Error Tracking

```kotlin
fun trackError(error: Exception) {
    // Send to analytics/crash reporting
    FirebaseCrashlytics.getInstance().recordException(error)
}
```

## Migration Guide

### From Old System to New System

```kotlin
suspend fun migrateOldSettings() {
    // 1. Read old settings from DataStore
    val oldEndpoint = dataStore.data.first()[CUSTOM_LLM_ENDPOINT_KEY]
    val oldApiKey = dataStore.data.first()[CUSTOM_LLM_API_KEY]
    val oldPrefix = dataStore.data.first()[CUSTOM_LLM_API_PREFIX_KEY]
    
    // 2. Create new provider
    val provider = CustomApiProvider(
        id = IdGenerator.generateSimpleId(),
        name = "Migrated Provider",
        type = ApiProviderType.TEXT_GENERATION,
        baseUrl = oldEndpoint ?: "",
        apiKey = oldApiKey ?: ""
    )
    
    // 3. Create endpoint
    val endpoint = CustomApiEndpoint(
        id = IdGenerator.generateSimpleId(),
        providerId = provider.id,
        endpointPath = "$oldPrefix/chat/completions",
        httpMethod = HttpMethod.POST,
        requestSchemaJson = getDefaultRequestSchema(),
        responseSchemaJson = getDefaultResponseSchema(),
        purpose = "chat"
    )
    
    // 4. Save to database
    repository.saveProvider(provider)
    repository.saveEndpoint(endpoint)
    
    // 5. Clear old settings (optional)
    dataStore.edit { prefs ->
        prefs.remove(CUSTOM_LLM_ENDPOINT_KEY)
        prefs.remove(CUSTOM_LLM_API_KEY)
        prefs.remove(CUSTOM_LLM_API_PREFIX_KEY)
    }
}
```

## Resources

- **Room Documentation**: https://developer.android.com/training/data-storage/room
- **Kotlin Coroutines**: https://kotlinlang.org/docs/coroutines-overview.html
- **OkHttp**: https://square.github.io/okhttp/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Hilt Dependency Injection**: https://developer.android.com/training/dependency-injection/hilt-android

## Support

For questions or issues:
1. Check existing documentation
2. Review code comments
3. Run unit tests
4. Check app logs
5. Contact development team

---

**Last Updated**: 2024
**Version**: 1.0
**Maintainer**: Vortex Android Team

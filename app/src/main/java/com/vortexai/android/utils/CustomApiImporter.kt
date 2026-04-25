package com.vortexai.android.utils

import com.vortexai.android.data.models.*
import org.json.JSONObject
import org.json.JSONArray

object CustomApiImporter {
    
    data class ImportResult(
        val provider: CustomApiProvider,
        val endpoints: List<CustomApiEndpoint>,
        val models: List<CustomApiModel>,
        val parameters: List<CustomApiParameter>
    )
    
    fun importFromJson(jsonString: String): Result<ImportResult> {
        return try {
            val json = JSONObject(jsonString)
            
            // Parse provider
            val providerJson = json.getJSONObject("provider")
            val providerId = IdGenerator.generateSimpleId()
            val provider = CustomApiProvider(
                id = providerId,
                name = providerJson.getString("name"),
                type = ApiProviderType.valueOf(providerJson.getString("type")),
                baseUrl = providerJson.getString("baseUrl"),
                apiKey = providerJson.optString("apiKey", ""),
                isEnabled = providerJson.optBoolean("isEnabled", true)
            )
            
            // Parse endpoints
            val endpoints = mutableListOf<CustomApiEndpoint>()
            val endpointsJson = json.getJSONArray("endpoints")
            for (i in 0 until endpointsJson.length()) {
                val endpointJson = endpointsJson.getJSONObject(i)
                endpoints.add(
                    CustomApiEndpoint(
                        id = IdGenerator.generateSimpleId(),
                        providerId = providerId,
                        endpointPath = endpointJson.getString("path"),
                        httpMethod = HttpMethod.valueOf(endpointJson.getString("method")),
                        requestSchemaJson = endpointJson.getJSONObject("requestSchema").toString(),
                        responseSchemaJson = endpointJson.getJSONObject("responseSchema").toString(),
                        purpose = endpointJson.getString("purpose")
                    )
                )
            }
            
            // Parse models
            val models = mutableListOf<CustomApiModel>()
            val parameters = mutableListOf<CustomApiParameter>()
            val modelsJson = json.getJSONArray("models")
            for (i in 0 until modelsJson.length()) {
                val modelJson = modelsJson.getJSONObject(i)
                val modelId = IdGenerator.generateSimpleId()
                
                models.add(
                    CustomApiModel(
                        id = modelId,
                        providerId = providerId,
                        modelId = modelJson.getString("modelId"),
                        displayName = modelJson.getString("displayName"),
                        capabilitiesJson = modelJson.optJSONObject("capabilities")?.toString() ?: "{}",
                        isActive = modelJson.optBoolean("isActive", true)
                    )
                )
                
                // Parse parameters for this model
                if (modelJson.has("parameters")) {
                    val paramsJson = modelJson.getJSONArray("parameters")
                    for (j in 0 until paramsJson.length()) {
                        val paramJson = paramsJson.getJSONObject(j)
                        parameters.add(
                            CustomApiParameter(
                                id = IdGenerator.generateSimpleId(),
                                modelId = modelId,
                                paramName = paramJson.getString("name"),
                                paramType = ParameterType.valueOf(paramJson.getString("type")),
                                defaultValue = if (paramJson.has("defaultValue")) paramJson.getString("defaultValue") else null,
                                minValue = if (paramJson.has("minValue")) paramJson.getString("minValue") else null,
                                maxValue = if (paramJson.has("maxValue")) paramJson.getString("maxValue") else null,
                                isRequired = paramJson.optBoolean("required", false),
                                description = if (paramJson.has("description")) paramJson.getString("description") else null
                            )
                        )
                    }
                }
            }
            
            Result.success(ImportResult(provider, endpoints, models, parameters))
        } catch (e: Exception) {
            Result.failure(Exception("Import failed: ${e.message}"))
        }
    }
    
    fun generateTemplate(type: ApiProviderType): String {
        val template = when (type) {
            ApiProviderType.TEXT_GENERATION -> """
{
  "provider": {
    "name": "My LLM Provider",
    "type": "TEXT_GENERATION",
    "baseUrl": "https://api.example.com",
    "apiKey": "",
    "isEnabled": true
  },
  "endpoints": [
    {
      "path": "/v1/chat/completions",
      "method": "POST",
      "purpose": "chat",
      "requestSchema": {
        "headers": {
          "Authorization": "Bearer {apiKey}",
          "Content-Type": "application/json"
        },
        "body": {
          "model": "{modelId}",
          "messages": "{messages}",
          "temperature": "{temperature}",
          "max_tokens": "{maxTokens}"
        }
      },
      "responseSchema": {
        "dataPath": "choices[0].message.content",
        "errorPath": "error.message"
      }
    }
  ],
  "models": [
    {
      "modelId": "gpt-3.5-turbo",
      "displayName": "GPT-3.5 Turbo",
      "isActive": true,
      "capabilities": {
        "streaming": true,
        "vision": false
      },
      "parameters": [
        {
          "name": "temperature",
          "type": "FLOAT",
          "defaultValue": "0.7",
          "minValue": "0.0",
          "maxValue": "2.0",
          "required": false,
          "description": "Controls randomness"
        },
        {
          "name": "maxTokens",
          "type": "INTEGER",
          "defaultValue": "2048",
          "minValue": "1",
          "maxValue": "4096",
          "required": false,
          "description": "Maximum response length"
        }
      ]
    }
  ]
}
            """.trimIndent()
            
            ApiProviderType.IMAGE_GENERATION -> """
{
  "provider": {
    "name": "My Image Provider",
    "type": "IMAGE_GENERATION",
    "baseUrl": "https://api.example.com",
    "apiKey": "",
    "isEnabled": true
  },
  "endpoints": [
    {
      "path": "/v1/images/generate",
      "method": "POST",
      "purpose": "image_gen",
      "requestSchema": {
        "headers": {
          "Authorization": "Bearer {apiKey}",
          "Content-Type": "application/json"
        },
        "body": {
          "prompt": "{prompt}",
          "model": "{modelId}",
          "width": "{width}",
          "height": "{height}",
          "steps": "{steps}"
        }
      },
      "responseSchema": {
        "imageUrlPath": "data[0].url",
        "errorPath": "error.message"
      }
    }
  ],
  "models": [
    {
      "modelId": "stable-diffusion-xl",
      "displayName": "Stable Diffusion XL",
      "isActive": true,
      "capabilities": {},
      "parameters": [
        {
          "name": "prompt",
          "type": "STRING",
          "required": true,
          "description": "Image description"
        },
        {
          "name": "width",
          "type": "INTEGER",
          "defaultValue": "1024",
          "minValue": "512",
          "maxValue": "2048",
          "required": false,
          "description": "Image width"
        },
        {
          "name": "height",
          "type": "INTEGER",
          "defaultValue": "1024",
          "minValue": "512",
          "maxValue": "2048",
          "required": false,
          "description": "Image height"
        },
        {
          "name": "steps",
          "type": "INTEGER",
          "defaultValue": "30",
          "minValue": "1",
          "maxValue": "100",
          "required": false,
          "description": "Inference steps"
        }
      ]
    }
  ]
}
            """.trimIndent()
            
            ApiProviderType.IMAGE_EDITING -> """
{
  "provider": {
    "name": "My Image Edit Provider",
    "type": "IMAGE_EDITING",
    "baseUrl": "https://api.example.com",
    "apiKey": "",
    "isEnabled": true
  },
  "endpoints": [
    {
      "path": "/v1/images/edit",
      "method": "POST",
      "purpose": "image_edit",
      "requestSchema": {
        "headers": {
          "Authorization": "Bearer {apiKey}",
          "Content-Type": "application/json"
        },
        "body": {
          "image": "{image}",
          "prompt": "{prompt}",
          "model": "{modelId}"
        }
      },
      "responseSchema": {
        "imageUrlPath": "data[0].url",
        "errorPath": "error.message"
      }
    }
  ],
  "models": [
    {
      "modelId": "instruct-pix2pix",
      "displayName": "InstructPix2Pix",
      "isActive": true,
      "capabilities": {},
      "parameters": [
        {
          "name": "image",
          "type": "STRING",
          "required": true,
          "description": "Base64 or URL of source image"
        },
        {
          "name": "prompt",
          "type": "STRING",
          "required": true,
          "description": "Edit instruction"
        }
      ]
    }
  ]
}
            """.trimIndent()
        }
        
        return template
    }
}

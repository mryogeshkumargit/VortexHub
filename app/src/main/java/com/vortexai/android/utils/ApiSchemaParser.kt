package com.vortexai.android.utils

import com.vortexai.android.data.models.RequestSchema
import com.vortexai.android.data.models.ResponseSchema
import org.json.JSONObject

object ApiSchemaParser {
    
    fun parseRequestSchema(json: String): RequestSchema? {
        return try {
            val obj = JSONObject(json)
            val headers = parseMap(obj.optJSONObject("headers"))
            val body = parseMap(obj.optJSONObject("body"))
            val paramMapping = parseMap(obj.optJSONObject("parameterMapping"))
            
            RequestSchema(headers, body, paramMapping)
        } catch (e: Exception) {
            android.util.Log.e("ApiSchemaParser", "Failed to parse request schema", e)
            null
        }
    }
    
    fun parseResponseSchema(json: String): ResponseSchema? {
        return try {
            val obj = JSONObject(json)
            ResponseSchema(
                dataPath = obj.optString("dataPath", null),
                streamingPath = obj.optString("streamingPath", null),
                errorPath = obj.optString("errorPath", null),
                imageUrlPath = obj.optString("imageUrlPath", null),
                statusPath = obj.optString("statusPath", null)
            )
        } catch (e: Exception) {
            android.util.Log.e("ApiSchemaParser", "Failed to parse response schema", e)
            null
        }
    }
    
    fun buildRequestSchema(
        headers: Map<String, String>,
        body: Map<String, String>,
        paramMapping: Map<String, String>
    ): String {
        val obj = JSONObject()
        obj.put("headers", JSONObject(headers))
        obj.put("body", JSONObject(body))
        obj.put("parameterMapping", JSONObject(paramMapping))
        return obj.toString()
    }
    
    fun buildResponseSchema(
        dataPath: String? = null,
        streamingPath: String? = null,
        errorPath: String? = null,
        imageUrlPath: String? = null,
        statusPath: String? = null
    ): String {
        val obj = JSONObject()
        dataPath?.let { obj.put("dataPath", it) }
        streamingPath?.let { obj.put("streamingPath", it) }
        errorPath?.let { obj.put("errorPath", it) }
        imageUrlPath?.let { obj.put("imageUrlPath", it) }
        statusPath?.let { obj.put("statusPath", it) }
        return obj.toString()
    }
    
    fun extractValueFromPath(json: String, path: String?): String? {
        if (path.isNullOrBlank()) return null
        
        return try {
            val parts = path.split(".")
            var current: Any = JSONObject(json)
            
            for (part in parts) {
                when {
                    part.contains("[") && part.contains("]") -> {
                        val arrayName = part.substringBefore("[")
                        val index = part.substringAfter("[").substringBefore("]").toInt()
                        current = (current as JSONObject).getJSONArray(arrayName).get(index)
                    }
                    current is JSONObject -> {
                        current = current.get(part)
                    }
                    else -> return null
                }
            }
            
            current.toString()
        } catch (e: Exception) {
            android.util.Log.e("ApiSchemaParser", "Failed to extract value from path: $path", e)
            null
        }
    }
    
    fun replacePlaceholders(
        template: String,
        values: Map<String, Any>
    ): String {
        var result = template
        values.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        return result
    }
    
    private fun parseMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key ->
            map[key] = obj.getString(key)
        }
        return map
    }
}

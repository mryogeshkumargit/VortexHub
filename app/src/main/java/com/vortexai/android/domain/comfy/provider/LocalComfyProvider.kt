package com.vortexai.android.domain.comfy.provider

import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Standard HTTP Polling executor for a Local Network ComfyUI instance (or direct port-forwarded IP).
 */
class LocalComfyProvider(
    private val client: OkHttpClient,
    private val endpointUrl: String
) : ComfyUIProvider {

    private val TAG = "LocalComfyProvider"
    private val baseEndpoint = endpointUrl.trimEnd('/')

    override suspend fun uploadImage(imageBytes: ByteArray): String {
        return try {
            val fileName = "vortex_${System.currentTimeMillis()}.png"
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    fileName,
                    imageBytes.toRequestBody("image/png".toMediaType())
                )
                .addFormDataPart("type", "input")
                .addFormDataPart("overwrite", "true")
                .build()
                
            val request = Request.Builder()
                .url("$baseEndpoint/upload/image")
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Local ComfyUI image upload failed: ${response.code} - ${response.body?.string()}")
            }
            
            val responseBody = response.body?.string() ?: throw Exception("Empty upload response")
            val responseJson = JSONObject(responseBody)
            
            val uploadedName = responseJson.optString("name")
            if (uploadedName.isBlank()) throw Exception("Upload missing 'name' attribute")
            Log.d(TAG, "Uploaded to ComfyUI input directory: $uploadedName")
            uploadedName
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for $baseEndpoint", e)
            throw e
        }
    }

    override suspend fun executeGeneration(graph: com.vortexai.android.domain.comfy.v2.CanonicalGraph): String {
        val workflowJsonString = com.vortexai.android.domain.comfy.v2.ComfyGraphBuilder.compileToJson(graph)
        
        Log.d(TAG, "Dispatching compiled CanonicalGraph to local ComfyUI instance -> /prompt")
        val promptRequestData = JSONObject().apply {
            put("prompt", JSONObject(workflowJsonString))
            put("client_id", "vortex-comfy-${System.currentTimeMillis()}")
        }
        
        val promptRequest = Request.Builder()
            .url("$baseEndpoint/prompt")
            .post(promptRequestData.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val promptResponse = client.newCall(promptRequest).execute()
        if (!promptResponse.isSuccessful) {
            throw Exception("Local ComfyUI /prompt failed: ${promptResponse.code} - ${promptResponse.body?.string()}")
        }
        
        val promptResponseBody = promptResponse.body?.string() ?: throw Exception("Empty response from /prompt")
        val promptId = JSONObject(promptResponseBody).optString("prompt_id")
        
        if (promptId.isBlank()) {
            throw Exception("Local ComfyUI did not return a valid prompt_id")
        }
        Log.d(TAG, "Prompt accepted ($promptId). Initiating asynchronous long poll...")
        
        return pollForCompletion(promptId)
    }

    private suspend fun pollForCompletion(promptId: String): String {
        val maxAttempts = 300 // Max 25 minutes
        var attempts = 0
        
        while (attempts < maxAttempts) {
            try {
                val request = Request.Builder()
                    .url("$baseEndpoint/history/$promptId")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val history = JSONObject(responseBody).optJSONObject(promptId)
                        if (history != null) {
                            val outputs = history.optJSONObject("outputs")
                            if (outputs != null) {
                                // Search any Node output for image array buffers
                                for (key in outputs.keys()) {
                                    val nodeOutput = outputs.optJSONObject(key)
                                    val images = nodeOutput?.optJSONArray("images")
                                    if (images != null && images.length() > 0) {
                                        val imageObj = images.getJSONObject(0)
                                        val filename = imageObj.getString("filename")
                                        val subfolder = imageObj.optString("subfolder", "")
                                        val type = imageObj.optString("type", "output")
                                        
                                        var urlParams = "?filename=$filename&type=$type"
                                        if (subfolder.isNotEmpty()) {
                                            urlParams += "&subfolder=$subfolder"
                                        }
                                        val finalOutputUrl = "$baseEndpoint/view$urlParams"
                                        Log.d(TAG, "Polling Success: ComfyUI generated image: $finalOutputUrl")
                                        return finalOutputUrl
                                    }
                                }
                            }
                        }
                    }
                }
                if (attempts < maxAttempts - 1) delay(5000L)
            } catch (e: Exception) {
                Log.e(TAG, "Poll attempt exception -> ${e.message}")
                if (attempts < maxAttempts - 1) delay(5000L) else throw e
            }
            attempts++
        }
        throw Exception("Polling timed out after ${maxAttempts * 5} seconds")
    }
}

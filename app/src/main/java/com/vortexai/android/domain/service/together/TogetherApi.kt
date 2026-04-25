package com.vortexai.android.domain.service.together

import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TogetherApi @Inject constructor() {
    
    companion object {
        private const val TAG = "TogetherApi"
        private const val BASE_URL = "https://api.together.xyz/v1"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    suspend fun editImage(
        apiKey: String,
        prompt: String,
        imageUrl: String? = null,
        imageBase64: String? = null,
        model: String = "black-forest-labs/FLUX.1-kontext-dev",
        strength: Float = 0.5f,
        width: Int? = null,
        height: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Together AI image-to-image with model: $model")
            
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Together AI API key is required"))
            }
            
            // Prepare image input
            val imageInput = when {
                !imageUrl.isNullOrBlank() -> imageUrl
                !imageBase64.isNullOrBlank() -> {
                    if (imageBase64.startsWith("data:image")) {
                        imageBase64
                    } else {
                        "data:image/jpeg;base64,$imageBase64"
                    }
                }
                else -> return@withContext Result.failure(Exception("Either imageUrl or imageBase64 is required"))
            }
            
            // Extract original image dimensions and resize image if needed
            val (originalWidth, originalHeight) = if (width != null && height != null) {
                Pair(width, height)
            } else {
                extractImageDimensions(imageBase64) ?: Pair(1024, 1024)
            }
            
            // Validate and adjust dimensions (max 1500px on larger side)
            val (finalWidth, finalHeight) = validateDimensions(originalWidth, originalHeight)
            
            // Resize the actual image if dimensions changed
            val finalImageInput = if (originalWidth != finalWidth || originalHeight != finalHeight) {
                resizeImageBase64(imageInput, finalWidth, finalHeight) ?: imageInput
            } else {
                imageInput
            }
            
            Log.d(TAG, "Original dimensions: ${originalWidth}x${originalHeight}, Final dimensions: ${finalWidth}x${finalHeight}")
            
            val requestBody = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
                put("image_url", finalImageInput)
                put("strength", strength)
                put("width", finalWidth)
                put("height", finalHeight)
                put("steps", 20)
                put("n", 1)
                put("disable_safety_checker", true)
            }
            
            Log.d(TAG, "Request body: ${requestBody.toString()}")
            
            val request = Request.Builder()
                .url("$BASE_URL/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            Log.d(TAG, "Response code: ${response.code}")
            
            val responseBody = response.body?.string()
            if (!response.isSuccessful) {
                val errorMessage = when (response.code) {
                    401 -> "Invalid Together AI API key"
                    402 -> "Insufficient credits on Together AI account"
                    422 -> "Invalid input parameters for image editing"
                    429 -> "Rate limit exceeded for Together AI API"
                    500, 502, 503, 504 -> "Together AI server error"
                    else -> "Together AI API error (${response.code}): ${responseBody ?: "Unknown error"}"
                }
                Log.e(TAG, "API error: $errorMessage")
                return@withContext Result.failure(Exception(errorMessage))
            }
            
            if (responseBody == null) {
                return@withContext Result.failure(Exception("Empty response from Together AI API"))
            }
            
            Log.d(TAG, "Response: ${responseBody.take(200)}...")
            
            val responseJson = JSONObject(responseBody)
            val dataArray = responseJson.optJSONArray("data")
            
            if (dataArray == null || dataArray.length() == 0) {
                return@withContext Result.failure(Exception("No image data in response"))
            }
            
            val firstImage = dataArray.getJSONObject(0)
            val imageUrl = firstImage.optString("url")
            
            if (imageUrl.isBlank()) {
                return@withContext Result.failure(Exception("No image URL in response"))
            }
            
            Log.d(TAG, "Successfully generated image: $imageUrl")
            Result.success(imageUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error editing image", e)
            Result.failure(e)
        }
    }
    
    suspend fun generateImage(
        endpoint: String,
        apiKey: String,
        prompt: String,
        model: String = "black-forest-labs/flux-schnell",
        width: Int = 1024,
        height: Int = 1024,
        steps: Int = 20
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Basic implementation - can be expanded later
            Result.failure(Exception("Together provider not implemented"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun fetchModels(endpoint: String, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Return the FLUX kontext models for image editing
            Result.success(listOf(
                "black-forest-labs/FLUX.1-kontext-dev",
                "black-forest-labs/FLUX.1-kontext-pro",
                "black-forest-labs/FLUX.1-kontext-max"
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateSpeech(
        apiKey: String,
        text: String,
        model: String = "cartesia/sonic",
        voice: String = "79a125e8-cd45-4c13-8a67-188112f4dd22"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Together AI TTS with model: $model, voice: $voice")
            
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Together AI API key is required"))
            }
            
            val requestBody = JSONObject().apply {
                put("model", model)
                put("input", text)
                put("voice", voice)
                put("response_format", "mp3")
                put("speed", 1.0)
            }
            
            Log.d(TAG, "TTS Request body: ${requestBody.toString()}")
            
            val request = Request.Builder()
                .url("$BASE_URL/audio/speech")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            Log.d(TAG, "TTS Response code: ${response.code}")
            
            if (!response.isSuccessful) {
                val errorMessage = when (response.code) {
                    401 -> "Invalid Together AI API key"
                    402 -> "Insufficient credits on Together AI account"
                    422 -> "Invalid input parameters for TTS"
                    429 -> "Rate limit exceeded for Together AI API"
                    500, 502, 503, 504 -> "Together AI server error"
                    else -> "Together AI TTS API error (${response.code}): ${response.body?.string() ?: "Unknown error"}"
                }
                Log.e(TAG, "TTS API error: $errorMessage")
                return@withContext Result.failure(Exception(errorMessage))
            }
            
            val audioBytes = response.body?.bytes()
            if (audioBytes == null) {
                return@withContext Result.failure(Exception("Empty audio response from Together AI TTS API"))
            }
            
            Log.d(TAG, "Successfully generated TTS audio: ${audioBytes.size} bytes")
            Result.success(audioBytes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating TTS", e)
            Result.failure(e)
        }
    }
    
    suspend fun fetchTtsVoices(apiKey: String): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        try {
            // Together AI uses Cartesia voices - return predefined list
            val voices = listOf(
                "79a125e8-cd45-4c13-8a67-188112f4dd22" to "Barbershop Man",
                "a0e99841-438c-4a64-b679-ae501e7d6091" to "Conversational Woman",
                "2ee87190-8f84-4925-97da-e52547f9462c" to "Customer Service Woman",
                "820a3788-2b37-4d21-847a-b65d8a68c99a" to "Newscaster Man",
                "fb26447f-308b-471e-8b00-8e9f04284eb5" to "Newscaster Woman"
            )
            Result.success(voices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun fetchTtsModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Return available TTS models
            Result.success(listOf(
                "cartesia/sonic",
                "cartesia/sonic-2"
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun extractImageDimensions(imageBase64: String?): Pair<Int, Int>? {
        if (imageBase64.isNullOrBlank()) return null
        
        return try {
            val base64Data = if (imageBase64.startsWith("data:image")) {
                imageBase64.substringAfter(",")
            } else {
                imageBase64
            }
            
            val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            if (bitmap != null) {
                val width = bitmap.width
                val height = bitmap.height
                bitmap.recycle()
                Log.d(TAG, "Extracted image dimensions: ${width}x${height}")
                Pair(width, height)
            } else {
                Log.w(TAG, "Failed to decode image for dimension extraction")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting image dimensions", e)
            null
        }
    }
    
    private fun resizeImageBase64(imageBase64: String, targetWidth: Int, targetHeight: Int): String? {
        return try {
            val base64Data = if (imageBase64.startsWith("data:image")) {
                imageBase64.substringAfter(",")
            } else {
                imageBase64
            }
            
            val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            if (originalBitmap != null) {
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                originalBitmap.recycle()
                
                val outputStream = java.io.ByteArrayOutputStream()
                resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                resizedBitmap.recycle()
                
                val resizedBytes = outputStream.toByteArray()
                val resizedBase64 = android.util.Base64.encodeToString(resizedBytes, android.util.Base64.NO_WRAP)
                
                Log.d(TAG, "Resized image from ${originalBitmap.width}x${originalBitmap.height} to ${targetWidth}x${targetHeight}")
                
                if (imageBase64.startsWith("data:image")) {
                    "data:image/jpeg;base64,$resizedBase64"
                } else {
                    resizedBase64
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing image", e)
            null
        }
    }
    
    private fun validateDimensions(width: Int, height: Int): Pair<Int, Int> {
        val maxDim = 1500
        
        // Scale down if needed
        val (scaledWidth, scaledHeight) = if (width <= maxDim && height <= maxDim) {
            Pair(width, height)
        } else {
            val aspectRatio = width.toFloat() / height.toFloat()
            if (width > height) {
                val newHeight = (maxDim / aspectRatio).toInt()
                Pair(maxDim, newHeight)
            } else {
                val newWidth = (maxDim * aspectRatio).toInt()
                Pair(newWidth, maxDim)
            }
        }
        
        // Round to nearest multiple of 8
        val finalWidth = (scaledWidth / 8) * 8
        val finalHeight = (scaledHeight / 8) * 8
        
        return Pair(finalWidth, finalHeight)
    }
} 
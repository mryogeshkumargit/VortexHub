package com.vortexai.android.domain.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling image input operations for qwen-image-edit
 * Supports local image selection, cloud upload, and base64 conversion
 */
@Singleton
class ImageInputService @Inject constructor() {
    
    companion object {
        private const val TAG = "ImageInputService"
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val COMPRESSION_QUALITY = 85
    }
    
    /**
     * Convert image file to base64 data URI
     */
    suspend fun convertImageToBase64(imagePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e(TAG, "Image file does not exist: $imagePath")
                return@withContext null
            }
            
            if (file.length() > MAX_IMAGE_SIZE) {
                Log.e(TAG, "Image file too large: ${file.length()} bytes")
                return@withContext null
            }
            
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image: $imagePath")
                return@withContext null
            }
            
            val base64 = bitmapToBase64(bitmap)
            bitmap.recycle()
            
            Log.d(TAG, "Successfully converted image to base64: ${base64.length} characters")
            base64
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to base64: ${e.message}", e)
            null
        }
    }
    
    /**
     * Convert image URI to base64 data URI
     */
    suspend fun convertImageUriToBase64(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $imageUri")
                return@withContext null
            }
            
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image from URI: $imageUri")
                return@withContext null
            }
            
            val base64 = bitmapToBase64(bitmap)
            bitmap.recycle()
            
            Log.d(TAG, "Successfully converted URI image to base64: ${base64.length} characters")
            base64
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI image to base64: ${e.message}", e)
            null
        }
    }
    
    /**
     * Save image from URI to local storage
     */
    suspend fun saveImageToLocalStorage(
        context: Context, 
        imageUri: Uri, 
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $imageUri")
                return@withContext null
            }
            
            // Create app-specific directory for images
            val imagesDir = File(context.filesDir, "character_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val outputFile = File(imagesDir, fileName)
            val outputStream = FileOutputStream(outputFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            val savedPath = outputFile.absolutePath
            Log.d(TAG, "Image saved to local storage: $savedPath")
            savedPath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to local storage: ${e.message}", e)
            null
        }
    }
    
    /**
     * Upload image to cloud storage and get URL
     * Note: This is a placeholder implementation. You'll need to integrate with your preferred cloud service
     */
    suspend fun uploadImageToCloud(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement actual cloud upload logic
            // This could be AWS S3, Google Cloud Storage, Firebase Storage, etc.
            
            // For now, return a placeholder URL
            Log.d(TAG, "Cloud upload not implemented yet. Returning placeholder URL.")
            "https://example.com/uploaded-image.jpg"
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to cloud: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get image info (size, dimensions, format)
     */
    suspend fun getImageInfo(imagePath: String): ImageInfo? = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                return@withContext null
            }
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)
            
            ImageInfo(
                fileSize = file.length(),
                width = options.outWidth,
                height = options.outHeight,
                mimeType = options.outMimeType ?: "unknown"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image info: ${e.message}", e)
            null
        }
    }
    
    /**
     * Convert bitmap to base64 data URI
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
        return "data:image/jpeg;base64,$base64"
    }
    
    /**
     * Validate image file
     */
    fun validateImageFile(imagePath: String): ValidationResult {
        val file = File(imagePath)
        
        if (!file.exists()) {
            return ValidationResult(false, "Image file does not exist")
        }
        
        if (file.length() > MAX_IMAGE_SIZE) {
            return ValidationResult(false, "Image file is too large (max 10MB)")
        }
        
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)
        
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return ValidationResult(false, "Invalid image format")
        }
        
        return ValidationResult(true, "Image is valid")
    }
}

/**
 * Data class for image information
 */
data class ImageInfo(
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
)

/**
 * Data class for validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

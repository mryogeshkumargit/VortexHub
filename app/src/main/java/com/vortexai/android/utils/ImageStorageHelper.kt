package com.vortexai.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing character images locally
 */
@Singleton
class ImageStorageHelper @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ImageStorageHelper"
        private const val CHARACTER_IMAGES_DIR = "character_images"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
    }
    
    /**
     * Save character image to local storage
     */
    suspend fun saveCharacterImage(characterId: String, imageData: ByteArray): String? {
        return withContext(Dispatchers.IO) {
            try {
                val imagesDir = getCharacterImagesDirectory()
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                
                val imageFile = File(imagesDir, "$characterId.jpg")
                
                // Compress image if needed
                val compressedData = compressImageIfNeeded(imageData)
                
                FileOutputStream(imageFile).use { output ->
                    output.write(compressedData)
                }
                
                Log.d(TAG, "Character image saved: ${imageFile.absolutePath}")
                imageFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save character image for $characterId", e)
                null
            }
        }
    }
    
    /**
     * Save character image from base64 string
     */
    suspend fun saveCharacterImageFromBase64(characterId: String, base64Image: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Remove data URL prefix if present
                val base64Data = if (base64Image.contains(",")) {
                    base64Image.substringAfter(",")
                } else {
                    base64Image
                }
                
                val imageData = Base64.decode(base64Data, Base64.DEFAULT)
                saveCharacterImage(characterId, imageData)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save character image from base64 for $characterId", e)
                null
            }
        }
    }
    
    /**
     * Load character image from local storage
     */
    suspend fun loadCharacterImage(characterId: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val imagesDir = getCharacterImagesDirectory()
                val imageFile = File(imagesDir, "$characterId.jpg")
                
                if (imageFile.exists()) {
                    FileInputStream(imageFile).use { input ->
                        input.readBytes()
                    }
                } else {
                    Log.d(TAG, "Character image not found for $characterId")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load character image for $characterId", e)
                null
            }
        }
    }
    
    /**
     * Get character image as base64 string
     */
    suspend fun getCharacterImageAsBase64(characterId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val imageData = loadCharacterImage(characterId)
                imageData?.let {
                    "data:image/jpeg;base64," + Base64.encodeToString(it, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get character image as base64 for $characterId", e)
                null
            }
        }
    }
    
    /**
     * Check if character image exists
     */
    fun hasCharacterImage(characterId: String): Boolean {
        return try {
            val imagesDir = getCharacterImagesDirectory()
            val imageFile = File(imagesDir, "$characterId.jpg")
            imageFile.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check character image existence for $characterId", e)
            false
        }
    }
    
    /**
     * Delete character image
     */
    suspend fun deleteCharacterImage(characterId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imagesDir = getCharacterImagesDirectory()
                val imageFile = File(imagesDir, "$characterId.jpg")
                
                if (imageFile.exists()) {
                    val deleted = imageFile.delete()
                    Log.d(TAG, "Character image deleted for $characterId: $deleted")
                    deleted
                } else {
                    Log.d(TAG, "Character image not found for deletion: $characterId")
                    true // Consider it deleted if it doesn't exist
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete character image for $characterId", e)
                false
            }
        }
    }
    
    /**
     * Get local image URL for character
     */
    fun getLocalImageUrl(characterId: String): String? {
        return if (hasCharacterImage(characterId)) {
            "file://${getCharacterImagesDirectory()}/$characterId.jpg"
        } else {
            null
        }
    }
    
    /**
     * Clean up old or unused character images
     */
    suspend fun cleanupUnusedImages(activeCharacterIds: List<String>): Int {
        return withContext(Dispatchers.IO) {
            try {
                val imagesDir = getCharacterImagesDirectory()
                if (!imagesDir.exists()) return@withContext 0
                
                var deletedCount = 0
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".jpg")) {
                        val characterId = file.nameWithoutExtension
                        if (characterId !in activeCharacterIds) {
                            if (file.delete()) {
                                deletedCount++
                                Log.d(TAG, "Deleted unused character image: $characterId")
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Cleanup completed. Deleted $deletedCount unused images")
                deletedCount
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup unused images", e)
                0
            }
        }
    }
    
    /**
     * Get storage statistics
     */
    suspend fun getStorageStats(): ImageStorageStats {
        return withContext(Dispatchers.IO) {
            try {
                val imagesDir = getCharacterImagesDirectory()
                if (!imagesDir.exists()) {
                    return@withContext ImageStorageStats(0, 0L, imagesDir.absolutePath)
                }
                
                var totalFiles = 0
                var totalSize = 0L
                
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        totalFiles++
                        totalSize += file.length()
                    }
                }
                
                ImageStorageStats(totalFiles, totalSize, imagesDir.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get storage stats", e)
                ImageStorageStats(0, 0L, "Error: ${e.message}")
            }
        }
    }
    
    private fun getCharacterImagesDirectory(): File {
        return File(context.filesDir, CHARACTER_IMAGES_DIR)
    }
    
    private fun compressImageIfNeeded(imageData: ByteArray): ByteArray {
        return try {
            if (imageData.size <= MAX_IMAGE_SIZE) {
                return imageData
            }
            
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode bitmap for compression")
                return imageData
            }
            
            val outputStream = ByteArrayOutputStream()
            var quality = 85
            
            do {
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                quality -= 10
            } while (outputStream.size() > MAX_IMAGE_SIZE && quality > 10)
            
            val compressedData = outputStream.toByteArray()
            Log.d(TAG, "Image compressed from ${imageData.size} to ${compressedData.size} bytes")
            
            compressedData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image", e)
            imageData
        }
    }
}

data class ImageStorageStats(
    val totalImages: Int,
    val totalSizeBytes: Long,
    val storagePath: String
) {
    val totalSizeMB: Float get() = totalSizeBytes / (1024f * 1024f)
} 
package com.vortexai.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream
import android.util.Base64

/**
 * Utility class for handling images from assets and external storage
 */
object ImageUtils {
    private const val TAG = "ImageUtils"
    
    /**
     * Check if an asset exists in the assets folder
     */
    fun assetExists(context: Context, assetPath: String): Boolean {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open(assetPath)
            inputStream.close()
            true
        } catch (e: IOException) {
            Log.w(TAG, "Asset not found: $assetPath")
            false
        }
    }
    
    /**
     * Get the proper URI for an image asset
     */
    fun getAssetUri(assetPath: String): String {
        return "file:///android_asset/$assetPath"
    }
    
    /**
     * Get the proper URI for Victoria Orlov's character image
     */
    fun getVictoriaOrlovImageUri(context: Context): String? {
        // Check if the image exists in assets
        if (assetExists(context, "victoria_orlov_.png")) {
            return getAssetUri("victoria_orlov_.png")
        }
        
        // Check if the image exists in Character Card folder
        val characterCardFile = File(context.filesDir.parentFile, "Character Card/victoria_orlov_.png")
        if (characterCardFile.exists()) {
            return "file://${characterCardFile.absolutePath}"
        }
        
        // Check if the image exists in external files
        val externalFile = File(context.getExternalFilesDir(null), "victoria_orlov_.png")
        if (externalFile.exists()) {
            return "file://${externalFile.absolutePath}"
        }
        
        Log.w(TAG, "Victoria Orlov image not found in any location")
        return null
    }
    
    /**
     * Copy an image from Character Card folder to assets (for development)
     */
    fun copyCharacterCardImageToAssets(context: Context, fileName: String): Boolean {
        return try {
            val sourceFile = File(context.filesDir.parentFile, "Character Card/$fileName")
            if (!sourceFile.exists()) {
                Log.e(TAG, "Source file not found: ${sourceFile.absolutePath}")
                return false
            }
            
            val assetsDir = File(context.filesDir, "assets")
            if (!assetsDir.exists()) {
                assetsDir.mkdirs()
            }
            
            val targetFile = File(assetsDir, fileName)
            sourceFile.copyTo(targetFile, overwrite = true)
            
            Log.d(TAG, "Image copied from Character Card to assets: $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image to assets", e)
            false
        }
    }
    
    /**
     * Ensure Victoria Orlov image is available and return proper URI
     */
    fun ensureVictoriaOrlovImage(context: Context): String? {
        // First check if image exists in assets
        var imageUri = getVictoriaOrlovImageUri(context)
        if (imageUri != null) {
            return imageUri
        }
        
        // Try to copy from Character Card folder
        if (copyCharacterCardImageToAssets(context, "victoria_orlov_.png")) {
            imageUri = getVictoriaOrlovImageUri(context)
        }
        
        return imageUri
    }
    
    /**
     * Get a bitmap from assets
     */
    fun getBitmapFromAssets(context: Context, assetPath: String): Bitmap? {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load bitmap from assets: $assetPath", e)
            null
        }
    }
    
    /**
     * Convert Victoria Orlov's asset image to base64 and return data URI
     * This function can be called to fix the avatar storage issue
     */
    fun convertVictoriaOrlovAssetToBase64(context: Context): String? {
        return try {
            val assetPath = "victoria_orlov_.png"
            Log.d(TAG, "Converting Victoria Orlov asset to base64: $assetPath")
            
            val bitmap = getBitmapFromAssets(context, assetPath)
            if (bitmap != null) {
                // Convert bitmap to base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val bytes = outputStream.toByteArray()
                outputStream.close()
                
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Log.d(TAG, "Successfully converted Victoria Orlov asset to base64 (${base64.length} chars)")
                
                // Return as data URI format for compatibility
                "data:image/jpeg;base64,$base64"
            } else {
                Log.w(TAG, "Failed to load Victoria Orlov asset for conversion")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Victoria Orlov asset to base64: ${e.message}", e)
            null
        }
    }
} 
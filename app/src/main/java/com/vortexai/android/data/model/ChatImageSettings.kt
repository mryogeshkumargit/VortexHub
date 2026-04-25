package com.vortexai.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Chat-specific image settings for qwen-image-edit functionality
 * Each chat conversation can have its own image input preferences
 */
@Entity(tableName = "chat_image_settings")
data class ChatImageSettings(
    @PrimaryKey
    val chatId: String,
    
    // Input Image Options
    val inputImageOption: InputImageOption = InputImageOption.CHARACTER_AVATAR,
    val localImagePath: String? = null, // Path to locally selected image
    val cloudImageUrl: String? = null, // URL to cloud-uploaded image
    val useCharacterAvatar: Boolean = true, // Use character's avatar as input
    
    // Prediction Creation Options
    val predictionCreationMethod: PredictionCreationMethod = PredictionCreationMethod.AUTO,
    val manualPredictionInput: String? = null, // Manual prediction text
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Available input image options for qwen-image-edit
 */
enum class InputImageOption {
    CHARACTER_AVATAR,    // Use character's avatar
    LOCAL_IMAGE,         // Browse and select local image
    CLOUD_IMAGE,         // Upload to cloud and use URL
    MANUAL_BASE64        // Manual base64 input
}

/**
 * Available prediction creation methods
 */
enum class PredictionCreationMethod {
    AUTO,               // Automatically create prediction from prompt
    MANUAL              // Use manual prediction input
}

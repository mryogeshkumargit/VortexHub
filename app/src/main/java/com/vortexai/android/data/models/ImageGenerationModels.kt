package com.vortexai.android.data.models

import com.google.gson.annotations.SerializedName

/**
 * ModelsLab model information with comprehensive details
 * Based on actual API response format
 */
data class ModelsLabModelInfo(
    @SerializedName("model_id")
    val modelId: String,
    @SerializedName("model_name")
    val modelName: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("screenshots")
    val screenshots: String? = null, // Single screenshot URL in actual API
    @SerializedName("model_subcategory")
    val modelSubcategory: String? = null,
    @SerializedName("model_category")
    val modelCategory: String? = null,
    @SerializedName("model_format")
    val modelFormat: String? = null,
    @SerializedName("is_nsfw")
    val isNsfw: String? = null, // String "0"/"1" in actual API
    @SerializedName("featured")
    val featured: String? = null, // String "yes"/"no" in actual API
    @SerializedName("feature")
    val feature: String? = null, // "Imagen" for valid models
    @SerializedName("status")
    val status: String? = null, // "model_ready"
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("instance_prompt")
    val instancePrompt: String? = null,
    @SerializedName("api_calls")
    val apiCalls: String? = null
) {
    /**
     * Get display name for the model (preferring model_name over model_id)
     */
    fun getDisplayName(): String = modelName.ifBlank { modelId }
    
    /**
     * Get formatted description with fallback
     */
    fun getDisplayDescription(): String = description?.takeIf { it.isNotBlank() } ?: "No description available"
    
    /**
     * Check if model is a LoRA
     */
    fun isLoRA(): Boolean = modelSubcategory?.lowercase() == "lora"
    
    /**
     * Check if model is valid for image generation (feature should be "Imagen")
     */
    fun isValidImageModel(): Boolean = feature == "Imagen"
    
    /**
     * Get primary screenshot URL
     */
    fun getPrimaryScreenshot(): String? = screenshots?.takeIf { it.isNotBlank() && it != "N/A" }
    
    /**
     * Check if model is NSFW
     */
    fun isNSFW(): Boolean = when (isNsfw?.lowercase()) {
        "1", "yes", "true" -> true
        else -> false
    }
    
    /**
     * Check if model is featured
     */
    fun isFeatured(): Boolean = featured?.lowercase() == "yes"
    
    /**
     * Get model status display
     */
    fun getStatusDisplay(): String = when (status) {
        "model_ready" -> "Ready"
        else -> status ?: "Unknown"
    }
}

/**
 * ModelsLab models API response
 */
data class ModelsLabModelsResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: List<ModelsLabModelInfo>?,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("next_page_id")
    val nextPageId: String? = null,
    @SerializedName("next_page")
    val nextPage: Int? = null,
    @SerializedName("total")
    val total: Int? = null,
    @SerializedName("page")
    val page: Int? = null,
    @SerializedName("limit")
    val limit: Int? = null
)

/**
 * Popular bundled models for fallback when API is unavailable
 * Generated from actual ModelsLab CSV data
 */
object PopularModelsLabModels {
    val POPULAR_MODELS = listOf(
        ModelsLabModelInfo(
            modelId = "nova-reality-xl-v4.0",
            modelName = "Nova Reality XL V4.0",
            description = "Popular image generation model",
            screenshots = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/e01bf64b-41d2-4ab3-866c-a7e023ca9b1a/original=true,quality=90/00000_2986622191.jpeg",
            modelSubcategory = null,
            modelCategory = "stable_diffusion",
            modelFormat = "safetensors",
            isNsfw = "0",
            featured = "no",
            feature = "Imagen",
            status = "model_ready",
            createdAt = null,
            instancePrompt = null,
            apiCalls = "999"
        ),
        ModelsLabModelInfo(
            modelId = "prefect-pony-xl-v5.0",
            modelName = "Prefect Pony XL V5.0",
            description = "Popular image generation model",
            screenshots = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/dc720ef4-dca7-4530-92ae-0b99939dc3ed/original=true,quality=90/00000-2554603248.jpeg",
            modelSubcategory = null,
            modelCategory = "stable_diffusion",
            modelFormat = "safetensors",
            isNsfw = "0",
            featured = "no",
            feature = "Imagen",
            status = "model_ready",
            createdAt = null,
            instancePrompt = null,
            apiCalls = "999"
        ),
        ModelsLabModelInfo(
            modelId = "cyberRealistic-xl-v5",
            modelName = "Cyberrealistic XL V5",
            description = "Popular image generation model",
            screenshots = "https://civitai.com/images/64885914",
            modelSubcategory = null,
            modelCategory = "stable_diffusion",
            modelFormat = "safetensors",
            isNsfw = "0",
            featured = "no",
            feature = "Imagen",
            status = "model_ready",
            createdAt = null,
            instancePrompt = null,
            apiCalls = "999"
        ),
        ModelsLabModelInfo(
            modelId = "AnimeMixV2",
            modelName = "Animemixv2",
            description = "Popular anime-style image generation model",
            screenshots = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/31d17e81-602d-44f8-ab89-93d268b15938/original=true,quality=90/00192-2422532239.jpeg",
            modelSubcategory = null,
            modelCategory = "stable_diffusion",
            modelFormat = "safetensors",
            isNsfw = "0",
            featured = "no",
            feature = "Imagen",
            status = "model_ready",
            createdAt = null,
            instancePrompt = null,
            apiCalls = "999"
        ),
        ModelsLabModelInfo(
            modelId = "Pony-Diffusion-V6-XL",
            modelName = "Pony Diffusion V6 XL",
            description = "Advanced pony-style image generation model",
            screenshots = "https://cdn2.stablediffusionapi.com/generations/1a14cab3-cc79-4cee-ab28-29379bff766b-0.png",
            modelSubcategory = null,
            modelCategory = "stable_diffusion",
            modelFormat = "safetensors",
            isNsfw = "0",
            featured = "no",
            feature = "Imagen",
            status = "model_ready",
            createdAt = null,
            instancePrompt = null,
            apiCalls = "999"
        )
    )
    
    val POPULAR_LORAS = listOf(
        ModelsLabModelInfo(
            modelId = "pclb00001",
            modelName = "Pclb00001",
            description = "Popular LoRA enhancement model",
            screenshots = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/cd0413ef-0629-4cee-a8ac-ca0a2b523116/width=450/sdxl_simple_example.jpeg",
            modelSubcategory = "lora",
            modelCategory = "stable_diffusion",
            modelFormat = "safetensors",
            isNsfw = "0",
            featured = "no",
            feature = "Imagen",
            status = "model_ready",
            createdAt = null,
            instancePrompt = null,
            apiCalls = "999"
        )
    )
} 
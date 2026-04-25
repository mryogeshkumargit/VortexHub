package com.vortexai.android.ui.screens.settings.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val IMAGE_PROVIDER_KEY = stringPreferencesKey("image_provider")
        val TOGETHER_AI_IMAGE_API_KEY = stringPreferencesKey("together_ai_image_api_key")
        val HUGGINGFACE_IMAGE_API_KEY = stringPreferencesKey("huggingface_image_api_key")
        val COMFYUI_API_KEY = stringPreferencesKey("comfyui_api_key")
        val CUSTOM_IMAGE_API_KEY = stringPreferencesKey("custom_image_api_key")
        val MODELSLAB_IMAGE_API_KEY = stringPreferencesKey("modelslab_image_api_key")
        val IMAGE_SIZE_KEY = stringPreferencesKey("image_size")
        val MODELSLAB_WORKFLOW_KEY = stringPreferencesKey("modelslab_workflow")
        val MODELSLAB_USE_CHAR_IMG_KEY = booleanPreferencesKey("modelslab_use_char_img")
        val MODELSLAB_LORA_MODEL_KEY = stringPreferencesKey("modelslab_lora_model")
        val MODELSLAB_LORA_STRENGTH_KEY = floatPreferencesKey("modelslab_lora_strength")
        val COMFYUI_LORA_MODEL_KEY = stringPreferencesKey("comfyui_lora_model")
        val COMFYUI_LORA_STRENGTH_KEY = floatPreferencesKey("comfyui_lora_strength")
        val NEGATIVE_PROMPT_KEY = stringPreferencesKey("negative_prompt")
        val IMAGE_MODEL_KEY = stringPreferencesKey("image_model")
        val STEPS_KEY = intPreferencesKey("steps")
        val GUIDANCE_SCALE_KEY = floatPreferencesKey("guidance_scale")
        val COMFYUI_WORKFLOW_KEY = stringPreferencesKey("comfyui_workflow")
        val COMFYUI_ENDPOINT_KEY = stringPreferencesKey("comfyui_endpoint")
        val CUSTOM_IMAGE_ENDPOINT_KEY = stringPreferencesKey("custom_image_endpoint")
        val CUSTOM_IMAGE_API_PREFIX_KEY = stringPreferencesKey("custom_image_api_prefix")
        val CUSTOM_AUDIO_API_PREFIX_KEY = stringPreferencesKey("custom_audio_api_prefix")
        val CUSTOM_AUDIO_ENDPOINT_KEY = stringPreferencesKey("custom_audio_endpoint")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val USE_LORA_KEY = booleanPreferencesKey("use_lora")
        val TTS_MODEL_KEY = stringPreferencesKey("tts_model")
        val NSFW_BLUR_ENABLED_KEY = booleanPreferencesKey("nsfw_blur_enabled")
        val NSFW_WARNING_ENABLED_KEY = booleanPreferencesKey("nsfw_warning_enabled")
        val REPLICATE_API_KEY = stringPreferencesKey("replicate_api_key")
        val REPLICATE_MODEL_KEY = stringPreferencesKey("replicate_model")
        val VORTEX_MODE_ENABLED_KEY = booleanPreferencesKey("vortex_mode_enabled")
        
        // Video Generation Keys
        val VIDEO_PROVIDER_KEY = stringPreferencesKey("video_provider")
        val FAL_AI_VIDEO_API_KEY = stringPreferencesKey("fal_ai_video_api_key")
        val FAL_AI_VIDEO_MODEL_KEY = stringPreferencesKey("fal_ai_video_model")
        val REPLICATE_VIDEO_API_KEY = stringPreferencesKey("replicate_video_api_key")
        val REPLICATE_VIDEO_MODEL_KEY = stringPreferencesKey("replicate_video_model")
        val MODELSLAB_VIDEO_API_KEY = stringPreferencesKey("modelslab_video_api_key")
        val MODELSLAB_VIDEO_MODEL_KEY = stringPreferencesKey("modelslab_video_model")
    }

    suspend fun getPreferences(): Preferences = dataStore.data.first()

    suspend fun savePreferences(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { preferences ->
            block(preferences)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
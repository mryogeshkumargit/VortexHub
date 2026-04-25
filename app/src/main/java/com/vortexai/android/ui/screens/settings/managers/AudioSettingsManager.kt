package com.vortexai.android.ui.screens.settings.managers

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.vortexai.android.ui.screens.settings.SettingsUiState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSettingsManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun loadAudioSettings(currentState: SettingsUiState): SettingsUiState {
        val preferences = settingsDataStore.getPreferences()
        
        return currentState.copy(
            ttsProvider = preferences[stringPreferencesKey("tts_provider")] ?: "ModelsLab",
            ttsModel = preferences[SettingsDataStore.TTS_MODEL_KEY] ?: "inworld-tts-1",
            sttProvider = preferences[stringPreferencesKey("stt_provider")] ?: "Together AI",
            ttsApiKey = preferences[stringPreferencesKey("tts_api_key")] ?: "",
            sttApiKey = preferences[stringPreferencesKey("stt_api_key")] ?: "",
            ttsVoice = preferences[stringPreferencesKey("tts_voice")] ?: "Alex",
            ttsSpeed = preferences[floatPreferencesKey("tts_speed")] ?: 1.0f,
            ttsPitch = preferences[floatPreferencesKey("tts_pitch")] ?: 0.0f,
            sttLanguage = preferences[stringPreferencesKey("stt_language")] ?: "english",
            ttsLanguage = preferences[stringPreferencesKey("tts_language")] ?: "english",
            ttsInitAudio = preferences[stringPreferencesKey("tts_init_audio")] ?: "",
            autoPlayTts = preferences[booleanPreferencesKey("auto_play_tts")] ?: false,
            voiceActivation = preferences[booleanPreferencesKey("voice_activation")] ?: false,
            customAudioApiPrefix = preferences[SettingsDataStore.CUSTOM_AUDIO_API_PREFIX_KEY] ?: "/v1",
            customAudioEndpoint = preferences[SettingsDataStore.CUSTOM_AUDIO_ENDPOINT_KEY] ?: "",
            togetherAiTtsApiKey = preferences[stringPreferencesKey("together_ai_tts_api_key")] ?: "",
            elevenLabsApiKey = preferences[stringPreferencesKey("elevenlabs_api_key")] ?: "",
            ttsVoiceHindi = preferences[stringPreferencesKey("tts_voice_hindi")] ?: "Prabhat",
            elevenLabsLanguage = preferences[stringPreferencesKey("elevenlabs_language")] ?: "English",
            manuallyAddedVoices = preferences[stringPreferencesKey("manually_added_voices")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            manuallyAddedCustomAudioModels = preferences[stringPreferencesKey("manually_added_custom_audio_models")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    suspend fun saveAudioSettings(state: SettingsUiState) {
        settingsDataStore.savePreferences { preferences: androidx.datastore.preferences.core.MutablePreferences ->
            preferences[stringPreferencesKey("tts_provider")] = state.ttsProvider
            preferences[SettingsDataStore.TTS_MODEL_KEY] = state.ttsModel
            preferences[stringPreferencesKey("stt_provider")] = state.sttProvider
            preferences[stringPreferencesKey("tts_api_key")] = state.ttsApiKey
            preferences[stringPreferencesKey("stt_api_key")] = state.sttApiKey
            preferences[stringPreferencesKey("tts_voice")] = state.ttsVoice
            preferences[floatPreferencesKey("tts_speed")] = state.ttsSpeed
            preferences[floatPreferencesKey("tts_pitch")] = state.ttsPitch
            preferences[stringPreferencesKey("stt_language")] = state.sttLanguage
            preferences[stringPreferencesKey("tts_language")] = state.ttsLanguage
            preferences[stringPreferencesKey("tts_init_audio")] = state.ttsInitAudio
            preferences[booleanPreferencesKey("auto_play_tts")] = state.autoPlayTts
            preferences[booleanPreferencesKey("voice_activation")] = state.voiceActivation
            preferences[SettingsDataStore.CUSTOM_AUDIO_API_PREFIX_KEY] = state.customAudioApiPrefix
            preferences[SettingsDataStore.CUSTOM_AUDIO_ENDPOINT_KEY] = state.customAudioEndpoint
            preferences[stringPreferencesKey("together_ai_tts_api_key")] = state.togetherAiTtsApiKey
            preferences[stringPreferencesKey("elevenlabs_api_key")] = state.elevenLabsApiKey
            preferences[stringPreferencesKey("tts_voice_hindi")] = state.ttsVoiceHindi
            preferences[stringPreferencesKey("elevenlabs_language")] = state.elevenLabsLanguage
            preferences[stringPreferencesKey("manually_added_voices")] = state.manuallyAddedVoices.joinToString(",")
            preferences[stringPreferencesKey("manually_added_custom_audio_models")] = state.manuallyAddedCustomAudioModels.joinToString(",")
        }
    }
}
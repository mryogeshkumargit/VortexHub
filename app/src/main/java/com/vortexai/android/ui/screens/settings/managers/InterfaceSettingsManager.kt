package com.vortexai.android.ui.screens.settings.managers

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import com.vortexai.android.ui.screens.settings.SettingsUiState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterfaceSettingsManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun loadInterfaceSettings(currentState: SettingsUiState): SettingsUiState {
        val preferences = settingsDataStore.getPreferences()
        
        return currentState.copy(
            isDarkMode = preferences[booleanPreferencesKey("dark_mode")] ?: false,
            themeMode = preferences[SettingsDataStore.THEME_MODE_KEY] ?: "system",
            language = preferences[stringPreferencesKey("language")] ?: "English",
            fontSize = preferences[stringPreferencesKey("font_size")] ?: "Medium",
            themeColor = preferences[stringPreferencesKey("theme_color")] ?: "Blue",
            chatBubbleStyle = preferences[stringPreferencesKey("chat_bubble_style")] ?: "Modern",
            messageLimit = preferences[intPreferencesKey("message_limit")] ?: 100,
            typingIndicator = preferences[booleanPreferencesKey("typing_indicator")] ?: true,
            autoSaveChats = preferences[booleanPreferencesKey("auto_save_chats")] ?: true,
            showCharacterBackground = preferences[booleanPreferencesKey("show_character_background")] ?: false,
            characterBackgroundOpacity = preferences[floatPreferencesKey("character_background_opacity")] ?: 0.3f,
            nsfwBlurEnabled = preferences[SettingsDataStore.NSFW_BLUR_ENABLED_KEY] ?: true,
            nsfwWarningEnabled = preferences[SettingsDataStore.NSFW_WARNING_ENABLED_KEY] ?: true
        )
    }

    suspend fun saveInterfaceSettings(state: SettingsUiState) {
        settingsDataStore.savePreferences { preferences: androidx.datastore.preferences.core.MutablePreferences ->
            preferences[booleanPreferencesKey("dark_mode")] = state.isDarkMode
            preferences[SettingsDataStore.THEME_MODE_KEY] = state.themeMode
            preferences[stringPreferencesKey("language")] = state.language
            preferences[stringPreferencesKey("font_size")] = state.fontSize
            preferences[stringPreferencesKey("theme_color")] = state.themeColor
            preferences[stringPreferencesKey("chat_bubble_style")] = state.chatBubbleStyle
            preferences[intPreferencesKey("message_limit")] = state.messageLimit
            preferences[booleanPreferencesKey("typing_indicator")] = state.typingIndicator
            preferences[booleanPreferencesKey("auto_save_chats")] = state.autoSaveChats
            preferences[booleanPreferencesKey("show_character_background")] = state.showCharacterBackground
            preferences[floatPreferencesKey("character_background_opacity")] = state.characterBackgroundOpacity
            preferences[SettingsDataStore.NSFW_BLUR_ENABLED_KEY] = state.nsfwBlurEnabled
            preferences[SettingsDataStore.NSFW_WARNING_ENABLED_KEY] = state.nsfwWarningEnabled
        }
    }
}
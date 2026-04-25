package com.vortexai.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val THEME_COLOR_KEY = stringPreferencesKey("theme_color")
    }
    
    fun isDarkModeEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    }

    fun getThemeModeFlow(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[THEME_MODE_KEY] ?: "system"
        }
    }

    fun getThemeColorFlow(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[THEME_COLOR_KEY] ?: "Blue"
        }
    }
}

@Composable
fun VortexThemeProvider(
    themeProvider: ThemeProvider,
    content: @Composable () -> Unit
) {
    // Read directly from DataStore for immediate updates
    val themeMode by themeProvider.getThemeModeFlow().collectAsState(initial = "system")
    val themeColor by themeProvider.getThemeColorFlow().collectAsState(initial = "Blue")
    val legacyDark by themeProvider.isDarkModeEnabled().collectAsState(initial = false)
    val systemDark = isSystemInDarkTheme()
    // Respect explicit theme mode; fall back to legacy dark flag or system
    val effectiveDark = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        "system" -> systemDark
        else -> legacyDark || systemDark
    }

    VortexAndroidTheme(
        darkTheme = effectiveDark,
        themeColor = themeColor,
        content = content
    )
} 
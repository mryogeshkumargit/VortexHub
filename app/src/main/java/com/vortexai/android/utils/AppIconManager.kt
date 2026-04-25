package com.vortexai.android.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ICON_KEY = stringPreferencesKey("app_icon")
        private const val PACKAGE_NAME = "com.vortexai.android"
    }
    
    enum class AppIcon(val alias: String, val displayName: String) {
        DEFAULT("$PACKAGE_NAME.MainActivityDefault", "Default"),
        ICON1("$PACKAGE_NAME.MainActivityIcon1", "Icon 1"),
        ICON2("$PACKAGE_NAME.MainActivityIcon2", "Icon 2"),
        ICON3("$PACKAGE_NAME.MainActivityIcon3", "Icon 3"),
        ICON4("$PACKAGE_NAME.MainActivityIcon4", "Icon 4"),
        ICON5("$PACKAGE_NAME.MainActivityIcon5", "Icon 5"),
        ICON6("$PACKAGE_NAME.MainActivityIcon6", "Icon 6"),
        ICON7("$PACKAGE_NAME.MainActivityIcon7", "Icon 7"),
        ICON8("$PACKAGE_NAME.MainActivityIcon8", "Icon 8"),
        ICON9("$PACKAGE_NAME.MainActivityIcon9", "Icon 9"),
        ICON10("$PACKAGE_NAME.MainActivityIcon10", "Icon 10"),
        ICON11("$PACKAGE_NAME.MainActivityIcon11", "Icon 11"),
        ICON12("$PACKAGE_NAME.MainActivityIcon12", "Icon 12"),
        ICON13("$PACKAGE_NAME.MainActivityIcon13", "Icon 13"),
        ICON14("$PACKAGE_NAME.MainActivityIcon14", "Icon 14"),
        ICON15("$PACKAGE_NAME.MainActivityIcon15", "Icon 15"),
        ICON16("$PACKAGE_NAME.MainActivityIcon16", "Icon 16"),
        ICON17("$PACKAGE_NAME.MainActivityIcon17", "Icon 17")
    }
    
    val currentIcon = dataStore.data.map { prefs ->
        val saved = prefs[ICON_KEY] ?: AppIcon.DEFAULT.name
        AppIcon.values().find { it.name == saved } ?: AppIcon.DEFAULT
    }
    
    suspend fun setIcon(icon: AppIcon) {
        val pm = context.packageManager
        
        AppIcon.values().forEach { appIcon ->
            val state = if (appIcon == icon) 
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
            else 
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            
            pm.setComponentEnabledSetting(
                ComponentName(context, appIcon.alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        
        dataStore.edit { prefs ->
            prefs[ICON_KEY] = icon.name
        }
    }
}

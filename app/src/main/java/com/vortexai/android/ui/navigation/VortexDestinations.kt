package com.vortexai.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object VortexDestinations {
    // Authentication routes
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    
    // Main app routes
    const val HOME = "home"
    const val CHAT = "chat"
    const val CHARACTERS = "characters"
    const val IMAGE_GENERATION = "image_generation"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    
    // Additional destinations that don't appear in bottom nav
    const val CHARACTER_DETAIL = "character_detail"
    const val CHARACTER_CREATE = "character_create"
    const val CHARACTER_EDIT = "character_edit"
    const val LOREBOOK = "lorebook"
    const val VOICE_SETTINGS = "voice_settings"
    const val API_SETTINGS = "api_settings"
    const val CUSTOM_API_TEXT = "custom_api_text"
    const val CUSTOM_API_IMAGE = "custom_api_image"
    const val CUSTOM_API_EDIT = "custom_api_edit"
    const val CUSTOM_API_HELP = "custom_api_help"
    const val MODEL_PARAMETERS = "model_parameters/{modelId}/{modelName}"
    const val SSL_SETTINGS = "ssl_settings"
    const val ABOUT = "about"
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "Home",
        route = VortexDestinations.HOME,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        title = "Characters",
        route = VortexDestinations.CHARACTERS,
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    ),
    BottomNavItem(
        title = "Chat",
        route = VortexDestinations.CHAT,
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat
    ),
    BottomNavItem(
        title = "Images",
        route = VortexDestinations.IMAGE_GENERATION,
        selectedIcon = Icons.Filled.Image,
        unselectedIcon = Icons.Outlined.Image
    ),
    BottomNavItem(
        title = "Settings",
        route = VortexDestinations.SETTINGS,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
) 
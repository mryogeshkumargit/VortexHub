package com.vortexai.android.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.vortexai.android.ui.screens.chat.ChatScreen
import com.vortexai.android.ui.screens.chat.ChatImageSettingsScreen
import com.vortexai.android.ui.screens.chat.ConversationListScreen
import com.vortexai.android.ui.screens.characters.CharactersScreen
import com.vortexai.android.ui.screens.characters.CharacterCreateScreen
import com.vortexai.android.ui.screens.home.HomeScreen
import com.vortexai.android.ui.screens.image.ImageGenerationScreen
import com.vortexai.android.ui.screens.profile.ProfileScreen
import com.vortexai.android.ui.screens.settings.SettingsScreen
import com.vortexai.android.ui.screens.settings.SSLSettingsScreen

@Composable
fun VortexNavigation(
    navController: NavHostController = rememberNavController(),
    onCreateBackup: (String) -> Unit,
    onOpenBackup: () -> Unit
) {
    Scaffold(
        bottomBar = {
            VortexBottomNavigation(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = VortexDestinations.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(VortexDestinations.HOME) {
                HomeScreen(
                    onNavigateToChat = { characterId ->
                        navController.navigate("${VortexDestinations.CHAT}/$characterId")
                    },
                    onNavigateToCharacters = { query ->
                        if (query != null) {
                            navController.navigate("${VortexDestinations.CHARACTERS}?search=${android.net.Uri.encode(query)}")
                        } else {
                            navController.navigate(VortexDestinations.CHARACTERS)
                        }
                    },
                    onNavigateToImageGeneration = {
                        navController.navigate(VortexDestinations.IMAGE_GENERATION)
                    }
                )
            }
            
            // Conversation List Screen (when clicking Chat in bottom nav)
            composable(VortexDestinations.CHAT) {
                ConversationListScreen(
                    onConversationClick = { conversationId ->
                        navController.navigate("${VortexDestinations.CHAT}/conversation/$conversationId")
                    },
                    onStartNewChat = {
                        navController.navigate(VortexDestinations.CHARACTERS)
                    }
                )
            }
            
            // Individual Chat Screen (with specific character) - NEW CHAT
            composable("${VortexDestinations.CHAT}/{characterId}") { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
                ChatScreen(
                    characterId = characterId,
                    conversationId = null, // New chat
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Individual Chat Screen (resuming existing conversation) - EXISTING CONVERSATION
            composable("${VortexDestinations.CHAT}/conversation/{conversationId}") { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                ChatScreen(
                    characterId = null, // Will be loaded from conversation
                    conversationId = conversationId, // Resume existing
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToImageSettings = { convId ->
                        if (convId.isNotBlank()) {
                            navController.navigate("${VortexDestinations.CHAT}/conversation/${convId}/image-settings")
                        }
                    }
                )
            }

            // Chat-specific Image Settings screen
            composable("${VortexDestinations.CHAT}/conversation/{conversationId}/image-settings") { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                ChatImageSettingsScreen(
                    chatId = conversationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "${VortexDestinations.CHARACTERS}?search={search}",
                arguments = listOf(
                    navArgument("search") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val searchQuery = backStackEntry.arguments?.getString("search")
                
                CharactersScreen(
                    initialSearchQuery = searchQuery,
                    onCharacterClick = { character ->
                        // Navigate to chat with character - will reuse existing conversation if available
                        navController.navigate("${VortexDestinations.CHAT}/${character.id}")
                    },
                    onCreateCharacter = {
                        navController.navigate(VortexDestinations.CHARACTER_CREATE)
                    },
                    onEditCharacter = { character ->
                        navController.navigate("${VortexDestinations.CHARACTER_CREATE}?edit=${character.id}")
                    }
                )
            }
            
            composable(VortexDestinations.CHARACTER_CREATE) {
                CharacterCreateScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onCharacterCreated = { characterId ->
                        // Navigate to chat with the newly created character
                        navController.navigate("${VortexDestinations.CHAT}/$characterId") {
                            // Clear the character creation screen from the back stack
                            popUpTo(VortexDestinations.CHARACTERS) {
                                inclusive = false
                            }
                        }
                    }
                )
            }
            
            // Character edit route
            composable("${VortexDestinations.CHARACTER_CREATE}?edit={characterId}") { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId")
                CharacterCreateScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onCharacterCreated = { characterId ->
                        // Navigate to chat with the updated character
                        navController.navigate("${VortexDestinations.CHAT}/$characterId") {
                            // Clear the character creation screen from the back stack
                            popUpTo(VortexDestinations.CHARACTERS) {
                                inclusive = false
                            }
                        }
                    },
                    characterIdToEdit = characterId
                )
            }
            
            composable(VortexDestinations.IMAGE_GENERATION) {
                ImageGenerationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(VortexDestinations.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSSLSettings = { navController.navigate(VortexDestinations.SSL_SETTINGS) },
                    onNavigateToCustomApiText = { navController.navigate(VortexDestinations.CUSTOM_API_TEXT) },
                    onNavigateToCustomApiImage = { navController.navigate(VortexDestinations.CUSTOM_API_IMAGE) },
                    onNavigateToCustomApiEdit = { navController.navigate(VortexDestinations.CUSTOM_API_EDIT) },
                    onCreateBackup = onCreateBackup,
                    onOpenBackup = onOpenBackup
                )
            }
            
            composable(VortexDestinations.PROFILE) {
                ProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(VortexDestinations.SSL_SETTINGS) {
                SSLSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(VortexDestinations.CUSTOM_API_TEXT) {
                com.vortexai.android.ui.screens.settings.CustomApiProviderScreen(
                    apiType = com.vortexai.android.data.models.ApiProviderType.TEXT_GENERATION,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHelp = { navController.navigate(VortexDestinations.CUSTOM_API_HELP) }
                )
            }
            
            composable(VortexDestinations.CUSTOM_API_IMAGE) {
                com.vortexai.android.ui.screens.settings.CustomApiProviderScreen(
                    apiType = com.vortexai.android.data.models.ApiProviderType.IMAGE_GENERATION,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHelp = { navController.navigate(VortexDestinations.CUSTOM_API_HELP) }
                )
            }
            
            composable(VortexDestinations.CUSTOM_API_EDIT) {
                com.vortexai.android.ui.screens.settings.CustomApiProviderScreen(
                    apiType = com.vortexai.android.data.models.ApiProviderType.IMAGE_EDITING,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHelp = { navController.navigate(VortexDestinations.CUSTOM_API_HELP) }
                )
            }
            
            composable(VortexDestinations.CUSTOM_API_HELP) {
                com.vortexai.android.ui.screens.settings.CustomApiHelpScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun VortexBottomNavigation(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar(
        modifier = Modifier.height(48.dp),
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (currentDestination?.hierarchy?.any { it.route == item.route } == true) {
                            item.selectedIcon
                        } else {
                            item.unselectedIcon
                        },
                        contentDescription = item.title,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = null,
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                alwaysShowLabel = false
            )
        }
    }
} 
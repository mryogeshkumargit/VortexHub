package com.vortexai.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.vortexai.android.data.repository.ChatRepository
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vortexai.android.ui.navigation.VortexDestinations
import com.vortexai.android.ui.navigation.VortexNavigation
import com.vortexai.android.ui.screens.auth.AuthViewModel
import com.vortexai.android.ui.screens.auth.LoginScreen
import com.vortexai.android.ui.screens.auth.RegisterScreen
import com.vortexai.android.ui.screens.chat.ChatScreen
import com.vortexai.android.ui.theme.VortexAndroidTheme
import com.vortexai.android.ui.theme.VortexThemeProvider
import com.vortexai.android.ui.theme.ThemeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var themeProvider: ThemeProvider
    @Inject
    lateinit var chatRepository: ChatRepository
    
    companion object {
        private const val TAG = "VortexMainActivity"
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission: $isGranted")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() started")
        
        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "super.onCreate() completed")
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            
            // Activity result launchers for backup/restore
            val createBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) {
                    lifecycleScope.launch {
                        try {
                            val json = chatRepository.createFullBackup()
                            contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(json.toByteArray())
                                out.flush()
                            }
                            Toast.makeText(this@MainActivity, "Backup saved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            val openBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    lifecycleScope.launch {
                        try {
                            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                            val result = chatRepository.restoreFromBackup(json)
                            if (result.isSuccess) {
                                Toast.makeText(this@MainActivity, "Restore completed", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Restore error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            setContent {
                val errorState = remember { mutableStateOf<String?>(null) }
                
                VortexThemeProvider(themeProvider = themeProvider) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (errorState.value != null) {
                            ErrorScreen(errorState.value!!)
                        } else {
                            VortexApp(
                                onCreateBackup = { filename ->
                                    createBackupLauncher.launch(filename)
                                },
                                onOpenBackup = {
                                    openBackupLauncher.launch(arrayOf("application/json"))
                                }
                            )
                        }
                    }
                }
            }
            Log.d(TAG, "onCreate() completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate()", e)
        }
    }
}

@Composable
fun VortexApp() {
    VortexApp(
        onCreateBackup = {},
        onOpenBackup = {}
    )
}

@Composable
fun VortexApp(
    onCreateBackup: (String) -> Unit,
    onOpenBackup: () -> Unit
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle auto-login and navigation based on authentication state
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(VortexDestinations.HOME) {
                popUpTo(VortexDestinations.LOGIN) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (authState.isLoggedIn) VortexDestinations.HOME else VortexDestinations.LOGIN
    ) {
        // Authentication screens
        composable(VortexDestinations.LOGIN) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(VortexDestinations.REGISTER)
                },
                onNavigateToHome = {
                    navController.navigate(VortexDestinations.HOME) {
                        popUpTo(VortexDestinations.LOGIN) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    // TODO: Implement forgot password
                }
            )
        }
        
        composable(VortexDestinations.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(VortexDestinations.HOME) {
                        popUpTo(VortexDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        // Main app with bottom navigation
        composable(VortexDestinations.HOME) {
            VortexNavigation(onCreateBackup = onCreateBackup, onOpenBackup = onOpenBackup)
        }
        
        // Individual chat screen (outside bottom nav)
        composable(
            route = "${VortexDestinations.CHAT}/{characterId}",
            arguments = listOf(navArgument("characterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId")
            ChatScreen(
                characterId = characterId,
                conversationId = null, // New chat
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Resume existing conversation screen
        composable(
            route = "${VortexDestinations.CHAT}/conversation/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            ChatScreen(
                characterId = null,
                conversationId = conversationId, // Resume existing
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "❌ VortexAI Error",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Red
            )
            Text(
                text = "",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Error Details:",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VortexAppPreview() {
    VortexAndroidTheme {
        VortexApp(onCreateBackup = {}, onOpenBackup = {})
    }
}

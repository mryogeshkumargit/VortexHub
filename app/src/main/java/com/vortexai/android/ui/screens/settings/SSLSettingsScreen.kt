package com.vortexai.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@Composable
fun SSLSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val allowInsecureConnections by viewModel.allowInsecureConnections.collectAsState()
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSL Security Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SSL Security Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            Text(
                text = "Warning: These settings should only be used if you're experiencing SSL connection issues with ModelsLab API.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow Insecure Connections",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Bypasses SSL certificate validation for API connections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = allowInsecureConnections,
                    onCheckedChange = { checked ->
                        if (checked) {
                            // Show confirmation dialog when enabling insecure connections
                            showConfirmDialog = true
                        } else {
                            viewModel.setAllowInsecureConnections(false)
                        }
                    }
                )
            }
            
            Divider()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Troubleshooting SSL Issues",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text("If you're experiencing SSL errors when using ModelsLab API:")
                    
                    Text("1. Check your internet connection")
                    Text("2. Try disabling any VPN or proxy services")
                    Text("3. Make sure your device's date and time are correct")
                    Text("4. As a last resort, enable 'Allow Insecure Connections'")
                    
                    Text(
                        text = "Note: Enabling insecure connections may expose your API requests to security risks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Security Warning") },
            text = {
                Text("Enabling insecure connections will bypass SSL certificate validation. This is a security risk and should only be used for troubleshooting. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAllowInsecureConnections(true)
                        showConfirmDialog = false
                    }
                ) {
                    Text("Enable Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 
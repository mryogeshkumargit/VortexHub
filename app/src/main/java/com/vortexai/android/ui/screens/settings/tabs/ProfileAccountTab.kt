package com.vortexai.android.ui.screens.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import com.vortexai.android.ui.screens.settings.components.*

@Composable
fun ProfileAccountTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onShowClearDataDialog: () -> Unit,
    onCreateBackup: (String) -> Unit,
    onOpenBackup: () -> Unit
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(title = "Account Information") {
                SettingsTextFieldItem(
                    title = "Username",
                    value = uiState.username,
                    onValueChange = viewModel::updateUsername,
                    placeholder = "Enter your username"
                )
                
                SettingsTextFieldItem(
                    title = "Full Name",
                    value = uiState.fullName,
                    onValueChange = viewModel::updateFullName,
                    placeholder = "Enter your full name"
                )
                
                SettingsTextFieldItem(
                    title = "Email",
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    placeholder = "Enter your email address"
                )
                
                SettingsDatePickerItem(
                    title = "Date of Birth",
                    value = uiState.dateOfBirth,
                    onValueChange = viewModel::updateDateOfBirth,
                    placeholder = "YYYY-MM-DD"
                )
            }
        }
        
        item {
            SettingsSection(title = "Account Actions") {
                SettingsActionItem(
                    title = "Change Password",
                    description = "Update your account password",
                    icon = Icons.Default.Lock,
                    onClick = { 
                        // TODO: Show change password dialog
                    }
                )
                
                SettingsActionItem(
                    title = "Logout",
                    description = "Sign out of your account",
                    icon = Icons.Default.ExitToApp,
                    onClick = { viewModel.logout() }
                )
                
                SettingsActionItem(
                    title = "Delete Account",
                    description = "Permanently delete your account and conversations",
                    icon = Icons.Default.Warning,
                    onClick = { showDeleteAccountDialog = true },
                    isDestructive = true
                )
            }
        }
        
        item {
            SettingsSection(title = "Data & Sync") {
                SettingsSwitchItem(
                    title = "Cloud Sync",
                    description = "Sync settings and chats across devices",
                    checked = uiState.cloudSyncEnabled,
                    onCheckedChange = viewModel::updateCloudSyncEnabled
                )
                
                SettingsSwitchItem(
                    title = "Auto Backup",
                    description = "Automatically backup your data",
                    checked = uiState.autoBackupEnabled,
                    onCheckedChange = viewModel::updateAutoBackupEnabled
                )
                
                SettingsSwitchItem(
                    title = "Supabase Integration",
                    description = "Connect to external database for cloud storage",
                    checked = uiState.supabaseEnabled,
                    onCheckedChange = viewModel::updateSupabaseEnabled
                )
                
                if (uiState.supabaseEnabled) {
                    SettingsTextFieldItem(
                        title = "Supabase URL",
                        value = uiState.supabaseUrl,
                        onValueChange = viewModel::updateSupabaseUrl,
                        placeholder = "https://your-project.supabase.co"
                    )
                    
                    SettingsPasswordFieldItem(
                        title = "Supabase Anon Key",
                        value = uiState.supabaseAnonKey,
                        onValueChange = viewModel::updateSupabaseAnonKey,
                        placeholder = "your-anon-key"
                    )
                    
                    SettingsActionItem(
                        title = "Test Supabase Connection",
                        description = "Verify your Supabase credentials",
                        icon = Icons.Default.Info,
                        onClick = { viewModel.testSupabaseConnection() }
                    )
                    
                    if (uiState.supabaseConnectionStatus.isNotBlank()) {
                        SettingsInfoItem(
                            title = "Connection Status",
                            description = uiState.supabaseConnectionStatus
                        )
                    }
                }
            }
        }
        
        item {
            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    title = "Push Notifications",
                    description = "Receive notifications for app updates",
                    checked = uiState.pushNotifications,
                    onCheckedChange = viewModel::updatePushNotifications
                )
                
                SettingsSwitchItem(
                    title = "Email Notifications",
                    description = "Receive updates via email",
                    checked = uiState.emailNotifications,
                    onCheckedChange = viewModel::updateEmailNotifications
                )
            }
        }
        
        item {
            SettingsSection(title = "Privacy") {
                SettingsSwitchItem(
                    title = "NSFW Content Blur",
                    description = "Blur NSFW character images in galleries",
                    checked = uiState.nsfwBlurEnabled,
                    onCheckedChange = viewModel::updateNsfwBlurEnabled
                )
                
                SettingsSwitchItem(
                    title = "NSFW Content Warning",
                    description = "Show warning before displaying NSFW content",
                    checked = uiState.nsfwWarningEnabled,
                    onCheckedChange = viewModel::updateNsfwWarningEnabled
                )
            }
        }
        
        item {
            SettingsSection(title = "Actions & Backup") {
                SettingsActionItem(
                    title = "Clear All Data",
                    description = "Reset all settings and clear stored data",
                    icon = Icons.Default.DeleteForever,
                    onClick = onShowClearDataDialog,
                    isDestructive = true
                )
                SettingsActionItem(
                    title = "Create Local Backup",
                    description = "Export characters, conversations, and messages to a file",
                    icon = Icons.Default.Image,
                    onClick = { onCreateBackup.invoke("vortex_backup_${System.currentTimeMillis()}.json") }
                )
                SettingsActionItem(
                    title = "Create Cloud Backup",
                    description = "Upload backup to Supabase cloud storage",
                    icon = Icons.Default.Image,
                    onClick = { viewModel.createCloudBackup() }
                )
                SettingsActionItem(
                    title = "List Cloud Backups",
                    description = "View available backups in Supabase",
                    icon = Icons.Default.Info,
                    onClick = { viewModel.listCloudBackups() }
                )
                SettingsActionItem(
                    title = "Restore From Cloud Backup",
                    description = "Restore from a cloud backup in Supabase",
                    icon = Icons.Default.Image,
                    onClick = { viewModel.showRestoreDialog() }
                )
                SettingsActionItem(
                    title = "Restore From Local Backup",
                    description = "Import from a previously saved backup file",
                    icon = Icons.Default.Image,
                    onClick = { onOpenBackup.invoke() }
                )
                
                if (uiState.backupStatus.isNotBlank()) {
                    SettingsInfoItem(
                        title = "Backup Status",
                        description = uiState.backupStatus
                    )
                }
                
                SettingsInfoItem(
                    title = "App Version",
                    description = "VortexAI v1.0.0 (Build 1)"
                )
                
                SettingsTextFieldItem(
                    title = "Update Server IP",
                    value = uiState.updateServerIp,
                    onValueChange = viewModel::updateUpdateServerIp,
                    placeholder = "10.0.2.2 (for emulator) or your PC IP"
                )
                
                SettingsTextFieldItem(
                    title = "Update Server Port",
                    value = uiState.updateServerPort,
                    onValueChange = viewModel::updateUpdateServerPort,
                    placeholder = "8000"
                )
                
                if (uiState.isDownloadingUpdate) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Updating App",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = uiState.downloadStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${uiState.downloadProgress}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = uiState.downloadProgress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    SettingsActionItem(
                        title = "Scan QR Code",
                        description = "Scan QR code from file_transfer_server.py to auto-configure",
                        icon = Icons.Default.QrCodeScanner,
                        onClick = { 
                            android.util.Log.d("ProfileAccountTab", "QR Scanner button clicked")
                            showQRScanner = true
                            android.util.Log.d("ProfileAccountTab", "showQRScanner set to: $showQRScanner")
                        }
                    )
                    
                    SettingsActionItem(
                        title = "Update App",
                        description = "Download and install from http://${uiState.updateServerIp}:${uiState.updateServerPort}",
                        icon = Icons.Default.SystemUpdate,
                        onClick = { viewModel.updateApp() }
                    )
                    
                    if (uiState.downloadStatus.isNotEmpty()) {
                        SettingsInfoItem(
                            title = "Update Status",
                            description = uiState.downloadStatus
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveProfileSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Profile Settings")
            }
        }
    }
    
    // QR Scanner Dialog - wrapped in Dialog to ensure composition
    android.util.Log.d("ProfileAccountTab", "Composing ProfileAccountTab, showQRScanner = $showQRScanner")
    
    if (showQRScanner) {
        android.util.Log.d("ProfileAccountTab", ">>> Rendering QR Scanner dialog <<<")
        
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { 
                android.util.Log.d("ProfileAccountTab", "QR Scanner dismissed")
                showQRScanner = false 
            }
        ) {
            // Force context resolution inside the dialog
            val context = androidx.compose.ui.platform.LocalContext.current
            
            com.vortexai.android.ui.components.QRCodeScanner(
                onQRCodeScanned = { scannedUrl ->
                    android.util.Log.d("QRScanner", "QR Code scanned: $scannedUrl")
                    
                    // Parse URL like "http://192.168.1.100:8000"
                    try {
                        val url = java.net.URL(
                            if (scannedUrl.startsWith("http")) scannedUrl else "http://$scannedUrl"
                        )
                        val host = url.host
                        val port = if (url.port != -1) url.port.toString() else "8000"
                        
                        viewModel.updateUpdateServerIp(host)
                        viewModel.updateUpdateServerPort(port)
                        showQRScanner = false
                        
                        android.widget.Toast.makeText(
                            context,
                            "✅ Server: $host:$port",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        // If not a valid URL, try to parse as "IP:PORT"
                        val parts = scannedUrl.split(":")
                        if (parts.size >= 2) {
                            val ip = parts[0].replace("http://", "").replace("https://", "")
                            val port = parts[parts.size - 1]
                            viewModel.updateUpdateServerIp(ip)
                            viewModel.updateUpdateServerPort(port)
                            showQRScanner = false
                            
                            android.widget.Toast.makeText(
                                context,
                                "✅ Server: $ip:$port",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "❌ Invalid QR: $scannedUrl",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onDismiss = { 
                    android.util.Log.d("ProfileAccountTab", "QR Scanner dismissed via scanner")
                    showQRScanner = false 
                }
            )
        }
    }
    
    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = { 
                Text("Are you sure you want to permanently delete your account? This will:\n\n• Delete all your conversations\n• Remove your profile information\n• Clear all app data\n• Keep characters available for other users\n\nThis action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Restore Cloud Backup Dialog
    if (uiState.showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRestoreDialog() },
            title = { Text("Restore from Cloud Backup") },
            text = { 
                if (uiState.availableBackups.isEmpty()) {
                    Text("No cloud backups available. Please create a backup first or check your Supabase connection.")
                } else {
                    Column {
                        Text("Select a backup to restore from:")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(uiState.availableBackups) { backup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = uiState.selectedBackupForRestore == backup.name,
                                        onClick = { viewModel.selectBackupForRestore(backup.name) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = backup.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Created: ${backup.created_at}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.restoreFromCloudBackup() },
                    enabled = uiState.selectedBackupForRestore.isNotBlank()
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRestoreDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
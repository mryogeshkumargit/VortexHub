package com.vortexai.android.ui.screens.settings.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import com.vortexai.android.ui.screens.settings.components.*

@Composable
fun InterfaceSettingsTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(title = "App Icon") {
                val currentIcon by viewModel.currentAppIcon.collectAsState(initial = com.vortexai.android.utils.AppIconManager.AppIcon.DEFAULT)
                var showIconDialog by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showIconDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Icon", style = MaterialTheme.typography.titleMedium)
                            Text(currentIcon.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(
                                id = when(currentIcon) {
                                    com.vortexai.android.utils.AppIconManager.AppIcon.DEFAULT -> com.vortexai.android.R.drawable.ic_launcher
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON1 -> com.vortexai.android.R.drawable.ic_launcher_icon1
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON2 -> com.vortexai.android.R.drawable.ic_launcher_icon2
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON3 -> com.vortexai.android.R.drawable.ic_launcher_icon3
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON4 -> com.vortexai.android.R.drawable.ic_launcher_icon4
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON5 -> com.vortexai.android.R.drawable.ic_launcher_icon5
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON6 -> com.vortexai.android.R.drawable.ic_launcher_icon6
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON7 -> com.vortexai.android.R.drawable.ic_launcher_icon7
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON8 -> com.vortexai.android.R.drawable.ic_launcher_icon8
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON9 -> com.vortexai.android.R.drawable.ic_launcher_icon9
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON10 -> com.vortexai.android.R.drawable.ic_launcher_icon10
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON11 -> com.vortexai.android.R.drawable.ic_launcher_icon11
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON12 -> com.vortexai.android.R.drawable.ic_launcher_icon12
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON13 -> com.vortexai.android.R.drawable.ic_launcher_icon13
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON14 -> com.vortexai.android.R.drawable.ic_launcher_icon14
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON15 -> com.vortexai.android.R.drawable.ic_launcher_icon15
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON16 -> com.vortexai.android.R.drawable.ic_launcher_icon16
                                    com.vortexai.android.utils.AppIconManager.AppIcon.ICON17 -> com.vortexai.android.R.drawable.ic_launcher_icon17
                                }
                            ),
                            contentDescription = currentIcon.displayName,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                if (showIconDialog) {
                    AlertDialog(
                        onDismissRequest = { showIconDialog = false },
                        title = { Text("Select App Icon") },
                        text = {
                            androidx.compose.foundation.lazy.LazyColumn {
                                items(com.vortexai.android.utils.AppIconManager.AppIcon.values().size) { index ->
                                    val icon = com.vortexai.android.utils.AppIconManager.AppIcon.values()[index]
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        onClick = {
                                            viewModel.setAppIcon(icon)
                                            showIconDialog = false
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            androidx.compose.foundation.Image(
                                                painter = androidx.compose.ui.res.painterResource(
                                                    id = when(icon) {
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.DEFAULT -> com.vortexai.android.R.drawable.ic_launcher
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON1 -> com.vortexai.android.R.drawable.ic_launcher_icon1
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON2 -> com.vortexai.android.R.drawable.ic_launcher_icon2
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON3 -> com.vortexai.android.R.drawable.ic_launcher_icon3
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON4 -> com.vortexai.android.R.drawable.ic_launcher_icon4
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON5 -> com.vortexai.android.R.drawable.ic_launcher_icon5
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON6 -> com.vortexai.android.R.drawable.ic_launcher_icon6
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON7 -> com.vortexai.android.R.drawable.ic_launcher_icon7
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON8 -> com.vortexai.android.R.drawable.ic_launcher_icon8
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON9 -> com.vortexai.android.R.drawable.ic_launcher_icon9
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON10 -> com.vortexai.android.R.drawable.ic_launcher_icon10
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON11 -> com.vortexai.android.R.drawable.ic_launcher_icon11
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON12 -> com.vortexai.android.R.drawable.ic_launcher_icon12
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON13 -> com.vortexai.android.R.drawable.ic_launcher_icon13
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON14 -> com.vortexai.android.R.drawable.ic_launcher_icon14
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON15 -> com.vortexai.android.R.drawable.ic_launcher_icon15
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON16 -> com.vortexai.android.R.drawable.ic_launcher_icon16
                                                        com.vortexai.android.utils.AppIconManager.AppIcon.ICON17 -> com.vortexai.android.R.drawable.ic_launcher_icon17
                                                    }
                                                ),
                                                contentDescription = icon.displayName,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(icon.displayName)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showIconDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
        
        item {
            SettingsSection(title = "Appearance") {
                SettingsDropdownItem(
                    title = "Theme Mode",
                    description = "Choose light, dark, or follow system",
                    selectedValue = when (uiState.themeMode.lowercase()) {
                        "system" -> "System"
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System"
                    },
                    options = listOf("System", "Light", "Dark"),
                    onValueChange = { sel ->
                        val mode = when (sel) {
                            "Light" -> "light"
                            "Dark" -> "dark"
                            else -> "system"
                        }
                        viewModel.updateThemeMode(mode)
                    }
                )
                
                SettingsSwitchItem(
                    title = "Force Dark Mode",
                    description = "Override to always use dark theme",
                    checked = uiState.isDarkMode,
                    onCheckedChange = viewModel::updateDarkMode
                )
                
                SettingsDropdownItem(
                    title = "Theme Color",
                    description = "Choose your accent color",
                    selectedValue = uiState.themeColor,
                    options = listOf("Blue", "Purple", "Green", "Orange", "Red", "Pink"),
                    onValueChange = {
                        viewModel.updateThemeColor(it)
                        viewModel.saveInterfaceSettings()
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Chat Interface") {
                SettingsDropdownItem(
                    title = "Chat Bubble Style",
                    description = "Customize message appearance",
                    selectedValue = uiState.chatBubbleStyle,
                    options = listOf("Modern", "Classic", "Minimal", "Rounded"),
                    onValueChange = {
                        viewModel.updateChatBubbleStyle(it)
                        viewModel.saveInterfaceSettings()
                    }
                )
                
                SettingsSwitchItem(
                    title = "Typing Indicator",
                    description = "Show when AI is generating response",
                    checked = uiState.typingIndicator,
                    onCheckedChange = viewModel::updateTypingIndicator
                )
                
                SettingsSwitchItem(
                    title = "Auto-save Chats",
                    description = "Automatically save conversation history",
                    checked = uiState.autoSaveChats,
                    onCheckedChange = viewModel::updateAutoSaveChats
                )
            }
        }
        
        item {
            SettingsSection(title = "Character Management") {
                val context = androidx.compose.ui.platform.LocalContext.current
                val bulkImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    viewModel.processBulkImport(context, uri)
                }
                
                LaunchedEffect(Unit) {
                    viewModel.setBulkImportCallback { bulkImportLauncher.launch(null) }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.bulkImportCharacterCards(context) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("📁 Bulk Import Character Cards", style = MaterialTheme.typography.titleMedium)
                            Text("Import multiple .png or .json character cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveInterfaceSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Interface Settings")
            }
        }
    }
}
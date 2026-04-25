package com.vortexai.android.ui.screens.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isTTSEnabled: Boolean,
    isSTTEnabled: Boolean,
    isVortexModeEnabled: Boolean,
    onTTSToggle: () -> Unit,
    onSTTToggle: () -> Unit,
    onVortexModeToggle: () -> Unit,
    onBackgroundSettings: () -> Unit,
    onImageSettings: () -> Unit,
    onNewConversation: () -> Unit,
    onClearAll: () -> Unit,
    onGenerationLogs: () -> Unit,
    onDeleteChat: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTTSEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isTTSEnabled) "Disable TTS" else "Enable TTS")
                }
            },
            onClick = {
                onTTSToggle()
                onDismiss()
            }
        )
        
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSTTEnabled) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSTTEnabled) "Disable STT" else "Enable STT")
                }
            },
            onClick = {
                onSTTToggle()
                onDismiss()
            }
        )
        
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVortexModeEnabled) Icons.Default.AutoAwesome else Icons.Default.AutoAwesomeMotion,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isVortexModeEnabled) "Disable Vortex Mode" else "Enable Vortex Mode")
                }
            },
            onClick = {
                onVortexModeToggle()
                onDismiss()
            }
        )
        
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Background Settings")
                }
            },
            onClick = {
                onBackgroundSettings()
                onDismiss()
            }
        )
        
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Image Input Settings",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Image Input Settings")
                }
            },
            onClick = {
                onImageSettings()
                onDismiss()
            }
        )
        

        
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Chat History")
                }
            },
            onClick = {
                onClearAll()
                onDismiss()
            }
        )
        
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generation Logs")
                }
            },
            onClick = {
                onGenerationLogs()
                onDismiss()
            }
        )
        
        HorizontalDivider()
        
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete Chat",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            onClick = {
                onDeleteChat()
                onDismiss()
            }
        )
    }
}
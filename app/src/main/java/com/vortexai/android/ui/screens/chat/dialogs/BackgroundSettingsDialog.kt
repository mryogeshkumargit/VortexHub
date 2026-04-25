package com.vortexai.android.ui.screens.chat.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vortexai.android.ui.screens.chat.ChatViewModel

@Composable
fun ChatBackgroundSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: ChatViewModel,
    showCharacterBackground: Boolean,
    characterBackgroundOpacity: Float
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Background Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Character Background Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Character Background",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Show character image as background",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showCharacterBackground,
                        onCheckedChange = { enabled ->
                            viewModel.updateShowCharacterBackground(enabled)
                        }
                    )
                }
                
                // Opacity Slider (only shown when background is enabled)
                if (showCharacterBackground) {
                    Column {
                        Text(
                            text = "Background Opacity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Adjust transparency of the background image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "0%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Slider(
                                value = characterBackgroundOpacity,
                                onValueChange = { opacity ->
                                    viewModel.updateCharacterBackgroundOpacity(opacity)
                                },
                                valueRange = 0.05f..0.8f,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Text(
                                text = "80%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = "Current: ${(characterBackgroundOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Preview section
                if (showCharacterBackground) {
                    Column {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background image preview
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(viewModel.uiState.collectAsStateWithLifecycle().value.character?.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Background preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(characterBackgroundOpacity),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Sample message bubble
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Sample message",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
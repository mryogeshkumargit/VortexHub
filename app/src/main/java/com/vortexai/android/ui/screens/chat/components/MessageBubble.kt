package com.vortexai.android.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vortexai.android.data.models.MessageResponse
import com.vortexai.android.data.models.MessageSenderType
import com.vortexai.android.data.models.MessageType
import com.vortexai.android.ui.components.ImageViewer
import com.vortexai.android.ui.components.SimpleClickableImage
import com.vortexai.android.ui.components.RichFormattedText
import com.vortexai.android.ui.components.VideoAvatar
import com.vortexai.android.ui.screens.chat.utils.saveChatImageToGallery
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubbleResponse(
    message: MessageResponse,
    isFromUser: Boolean,
    characterAvatarUrl: String?,
    onDeleteMessage: (String) -> Unit,
    onAnimateImage: (String) -> Unit = {},
    onApplyVideoAvatar: ((String) -> Unit)? = null,
    bubbleStyle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromUser) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(characterAvatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Character",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        val isCharacterIntroMsg = message.messageType == MessageType.IMAGE && try {
            message.metadata?.modelUsed == "character_avatar" || 
            message.content.contains("appears") ||
            message.content.startsWith("*")
        } catch (e: Exception) { false }
        
        Column(
            modifier = Modifier.widthIn(
                max = if (isCharacterIntroMsg) 320.dp else 280.dp
            ),
            horizontalAlignment = if (isCharacterIntroMsg) {
                Alignment.CenterHorizontally
            } else if (isFromUser) {
                Alignment.End
            } else {
                Alignment.Start
            }
        ) {
            val shape = when (bubbleStyle.lowercase()) {
                "classic" -> RoundedCornerShape(8.dp)
                "minimal" -> RoundedCornerShape(0.dp)
                "rounded" -> RoundedCornerShape(24.dp)
                else -> RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromUser) 4.dp else 16.dp
                )
            }
            
            Surface(
                shape = shape,
                color = if (isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (message.messageType == MessageType.IMAGE) {
                        MessageImageContent(
                            message = message,
                            isFromUser = isFromUser,
                            isCharacterIntro = isCharacterIntroMsg,
                            onDeleteMessage = onDeleteMessage,
                            onAnimateImage = onAnimateImage
                        )
                    } else if (message.messageType == MessageType.VIDEO) {
                        MessageVideoContent(
                            message = message,
                            isFromUser = isFromUser,
                            onDeleteMessage = onDeleteMessage,
                            onApplyVideoAvatar = onApplyVideoAvatar
                        )
                    } else {
                        SelectionContainer {
                            RichFormattedText(
                                text = message.content,
                                isFromUser = isFromUser
                            )
                        }
                    }
                }
            }
            
            Text(
                text = formatTimestampFromString(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        
        if (isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageImageContent(
    message: MessageResponse,
    isFromUser: Boolean,
    isCharacterIntro: Boolean,
    onDeleteMessage: (String) -> Unit,
    onAnimateImage: (String) -> Unit = {}
) {
    val imageUrl = message.metadata?.imageUrl
    val isGenerating = message.metadata?.modelUsed == "generating_image" || message.metadata?.modelUsed == "generating_video"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    
    if (!isCharacterIntro && message.content.isNotBlank() && !message.content.startsWith("*") && !message.content.contains("appears")) {
        RichFormattedText(
            text = message.content,
            isFromUser = isFromUser
        )
        Spacer(modifier = Modifier.height(8.dp))
    } else if (isCharacterIntro) {
        Text(
            text = "✨ ${message.senderName ?: "Character"} joins the conversation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
    
    Column {
        if (isGenerating) {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square placeholder
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (message.metadata?.modelUsed == "generating_image") "Generating Image..." else "Generating Video...",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            SimpleClickableImage(
                imageUrl = imageUrl,
                imageBase64 = null,
                localPath = null,
                contentDescription = if (isCharacterIntro) "Character introduction" else "Generated image",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        if (isCharacterIntro) RoundedCornerShape(12.dp) else RoundedCornerShape(8.dp)
                    ),
                contentScale = ContentScale.Fit,
                onClick = { showImageViewer = true }
            )
        }
        
        if (!isCharacterIntro && !isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onAnimateImage(message.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = "Animate",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Image") },
            text = { Text("Save this image to your gallery?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            saveChatImageToGallery(context, message)
                        }
                        showSaveDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Image") },
            text = { Text("Delete this message permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(message.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    message.metadata?.let { metadata ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isCharacterIntro) {
                "Character Introduction"
            } else {
                buildString {
                    metadata.modelUsed?.let { 
                        if (it != "character_avatar") append("Model: $it") 
                    }
                    if (metadata.generationTime != null && metadata.generationTime > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("${metadata.generationTime}ms")
                    }
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isFromUser) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }
        )
    }
    
    if (showImageViewer) {
        ImageViewer(
            imageUrl = imageUrl,
            imageBase64 = null,
            localPath = null,
            onDismiss = { showImageViewer = false }
        )
    }
}

@Composable
fun TypingIndicator(
    characterAvatarUrl: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(characterAvatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Character",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                    if (index < 2) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}

private fun formatTimestampFromString(timestamp: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = sdf.parse(timestamp)
        val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        date?.let { displayFormat.format(it) } ?: "Time"
    } catch (e: Exception) {
        "Time"
    }
}

@Composable
private fun MessageVideoContent(
    message: MessageResponse,
    isFromUser: Boolean,
    onDeleteMessage: (String) -> Unit,
    onApplyVideoAvatar: ((String) -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val videoUrl = message.metadata?.videoUrl
    
    // Optional description text
    if (message.content.isNotBlank()) {
        RichFormattedText(
            text = message.content,
            isFromUser = isFromUser
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!videoUrl.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp))) {
                VideoAvatar(
                    videoUrl = videoUrl,
                    imageUrl = message.metadata?.imageUrl ?: videoUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) {
                Text("Video unavailable", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set as Avatar Action
            if (!isFromUser && onApplyVideoAvatar != null && !videoUrl.isNullOrBlank()) {
                FilledTonalButton(
                    onClick = { onApplyVideoAvatar(videoUrl) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = "Set Avatar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Avatar", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            
            // Delete Action
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Metadata
        message.metadata?.let { metadata ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    metadata.modelUsed?.let { append("Model: $it") }
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isFromUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Video") },
            text = { Text("Delete this message permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(message.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
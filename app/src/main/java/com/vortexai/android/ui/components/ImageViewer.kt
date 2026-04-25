package com.vortexai.android.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.max
import kotlin.math.min

@Composable
fun ImageViewer(
    imageUrl: String?,
    imageBase64: String?,
    localPath: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (imageUrl == null && imageBase64 == null && localPath == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        ImageViewerContent(
            imageUrl = imageUrl,
            imageBase64 = imageBase64,
            localPath = localPath,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

@Composable
private fun ImageViewerContent(
    imageUrl: String?,
    imageBase64: String?,
    localPath: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Zoom state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        // Close button - positioned at top right, outside gesture area
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Image content with gesture detection - positioned below close button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Handle pinch to zoom and pan with immediate response (no animations)
                        scale = (scale * zoom).coerceIn(0.5f..3f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                    
                    detectTapGestures(
                        onDoubleTap = {
                            // Double tap to close
                            onDismiss()
                        }
                    ) {
                        // Single tap - do nothing
                    }
                }
        ) {
            // Determine image source
            val imageSource = when {
                imageUrl != null -> imageUrl
                imageBase64 != null -> "data:image/png;base64,$imageBase64"
                localPath != null -> "file://$localPath"
                else -> null
            }

            if (imageSource != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageSource)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Enlarged Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,  // Direct scale without animation
                            scaleY = scale,  // Direct scale without animation
                            translationX = offsetX,  // Direct translation without animation
                            translationY = offsetY   // Direct translation without animation
                        )
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ClickableImage(
    imageUrl: String?,
    imageBase64: String?,
    localPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onImageClick: () -> Unit = {}
) {
    var showImageViewer by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Determine image source
    val imageSource = when {
        imageUrl != null -> imageUrl
        imageBase64 != null -> "data:image/png;base64,$imageBase64"
        localPath != null -> "file://$localPath"
        else -> null
    }

    if (imageSource != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageSource)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
                .clickable { 
                    onImageClick()
                    showImageViewer = true 
                },
            contentScale = contentScale
        )
    }

    if (showImageViewer) {
        ImageViewer(
            imageUrl = imageUrl,
            imageBase64 = imageBase64,
            localPath = localPath,
            onDismiss = { showImageViewer = false }
        )
    }
}

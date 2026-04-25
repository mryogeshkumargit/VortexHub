package com.vortexai.android.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

@Composable
fun SimpleClickableImage(
    imageUrl: String?,
    imageBase64: String? = null,
    localPath: String? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onClick: (() -> Unit)? = null
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    val imageData = when {
        !imageUrl.isNullOrBlank() -> imageUrl
        !imageBase64.isNullOrBlank() -> {
            if (imageBase64.startsWith("data:image")) imageBase64 
            else "data:image/jpeg;base64,$imageBase64"
        }
        !localPath.isNullOrBlank() -> localPath
        else -> null
    }
    
    Box(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick?.invoke() }
                    )
                }
            } else Modifier
        ),
        contentAlignment = Alignment.Center
    ) {
        if (imageData != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageData)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    isLoading = state is AsyncImagePainter.State.Loading
                    hasError = state is AsyncImagePainter.State.Error
                }
            )
        }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
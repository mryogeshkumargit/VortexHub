package com.vortexai.android.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * A reusable component that plays a looping, muted video for character avatars.
 * Falls back to a standard static image if the video URL is null or playback fails.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoAvatar(
    videoUrl: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    crossfade: Boolean = true
) {
    val context = LocalContext.current
    var isVideoReady by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    
    // Manage ExoPlayer lifecycle bounds to the composition
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE // Loop indefinitely
            volume = 0f // Muted by default for avatars
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }
    }

    DisposableEffect(videoUrl) {
        if (!videoUrl.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            exoPlayer.setMediaItem(mediaItem)
            
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isVideoReady = true
                            hasError = false
                        }
                        Player.STATE_ENDED -> {}
                        Player.STATE_BUFFERING -> {}
                        Player.STATE_IDLE -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    hasError = true
                    isVideoReady = false
                }
            }
            
            exoPlayer.addListener(listener)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        } else {
            hasError = true
            isVideoReady = false
            onDispose {}
        }
    }
    
    Box(modifier = modifier.background(Color.Transparent)) {
        // Fallback Image
        if (!isVideoReady || hasError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(crossfade)
                    .build(),
                contentDescription = "Character Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
        
        // Video Player
        if (!videoUrl.isNullOrBlank() && !hasError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // No playback controls
                        
                        // Map Compose ContentScale to PlayerView ResizeMode
                        resizeMode = when (contentScale) {
                            ContentScale.Crop -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            ContentScale.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            ContentScale.FillBounds -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                        
                        // Transparent background to show fallback image while buffering
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
                // Do not hide the view even if not ready to avoid view flicker.
                // The transparency allows the AsyncImage to show underneath.
            )
        }
    }
}

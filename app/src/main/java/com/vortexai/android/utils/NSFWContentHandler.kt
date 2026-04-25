package com.vortexai.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import javax.inject.Inject
import javax.inject.Singleton
import java.io.ByteArrayOutputStream

/**
 * Utility class for handling NSFW content blurring and warnings
 */
@Singleton
class NSFWContentHandler @Inject constructor(
    private val context: Context
) {
    
    /**
     * Blur a bitmap image
     */
    fun blurBitmap(bitmap: Bitmap, radius: Float = 25f): Bitmap {
        val blurredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurredBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            maskFilter = android.graphics.BlurMaskFilter(radius, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return blurredBitmap
    }
    
    /**
     * Add a dark overlay to a bitmap
     */
    fun addDarkOverlay(bitmap: Bitmap): Bitmap {
        val overlayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlayBitmap)
        
        // Draw the original image
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        
        // Draw dark overlay
        val paint = Paint().apply {
            color = Color.BLACK
            alpha = 128 // 50% opacity
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        
        return overlayBitmap
    }
    
    /**
     * Convert bitmap to base64 string
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}

/**
 * Composable for NSFW content warning dialog
 */
@Composable
fun NSFWWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    characterName: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NSFW Content Warning") },
        text = { 
            Text(
                "This character ($characterName) contains NSFW (Not Safe For Work) content. " +
                "Are you sure you want to view this content?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            ) {
                Text("View Content")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Composable for NSFW blurred character image
 */
@Composable
fun NSFWBlurredCharacterImage(
    imageUrl: String?,
    videoUrl: String? = null,
    characterName: String,
    isNsfw: Boolean,
    onImageClick: () -> Unit,
    nsfwBlurEnabled: Boolean = true,
    nsfwWarningEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showNsfwWarning by remember { mutableStateOf(false) }
    var showUnblurredImage by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val shouldBlur = remember(isNsfw, nsfwBlurEnabled) {
        isNsfw && nsfwBlurEnabled
    }
    
    Box(
        modifier = modifier
            .clickable { 
                if (shouldBlur && !showUnblurredImage && nsfwWarningEnabled) {
                    showNsfwWarning = true
                } else {
                    onImageClick()
                }
            }
    ) {
        if (imageUrl?.isNotEmpty() == true || videoUrl?.isNotEmpty() == true) {
            com.vortexai.android.ui.components.VideoAvatar(
                imageUrl = imageUrl,
                videoUrl = videoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (shouldBlur && !showUnblurredImage) {
                            Modifier.blur(radius = 25.dp)
                        } else {
                            Modifier
                        }
                    ),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            // NSFW overlay
            if (shouldBlur && !showUnblurredImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "NSFW Content",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        } else {
            // Default avatar placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default avatar",
                    modifier = Modifier.size(48.dp),
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // NSFW warning dialog
    if (showNsfwWarning) {
        NSFWWarningDialog(
            onDismiss = { showNsfwWarning = false },
            onConfirm = { 
                showNsfwWarning = false
                showUnblurredImage = true
            },
            characterName = characterName
        )
    }
}

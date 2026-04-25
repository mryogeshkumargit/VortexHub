package com.vortexai.android.ui.screens.chat.utils

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.vortexai.android.data.models.MessageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

suspend fun saveChatImageToGallery(context: Context, message: MessageResponse) {
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val filename = "VortexChat_${System.currentTimeMillis()}.jpg"
            val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val uri = resolver.insert(imageCollection, contentValues) ?: return@withContext

            resolver.openOutputStream(uri)?.use { outStream ->
                if (message.metadata?.imageUrl != null && message.metadata.imageUrl!!.startsWith("file://")) {
                    val filePath = message.metadata.imageUrl!!.removePrefix("file://")
                    File(filePath).inputStream().copyTo(outStream)
                } else {
                    val dataSrc = message.metadata?.imageUrl ?: return@use
                    val loader = Coil.imageLoader(context)
                    val request = ImageRequest.Builder(context).data(dataSrc).build()
                    val bitmap = loader.execute(request).drawable?.toBitmap()
                    bitmap?.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outStream)
                }
            }

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Image saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatImageSave", "Error saving image", e)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Failed to save image", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    return try {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    } catch (e: Exception) {
        "Now"
    }
}
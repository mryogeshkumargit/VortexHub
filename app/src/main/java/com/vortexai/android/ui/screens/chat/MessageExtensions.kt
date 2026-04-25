package com.vortexai.android.ui.screens.chat

import com.vortexai.android.data.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension function to convert Message to MessageResponse
 */
fun Message.toMessageResponse(): MessageResponse {
    return MessageResponse(
        id = this.id,
        conversationId = this.conversationId,
        content = this.content,
        senderType = when (this.role) {
            "user" -> MessageSenderType.USER
            "assistant", "character" -> MessageSenderType.CHARACTER
            "system" -> MessageSenderType.SYSTEM
            else -> MessageSenderType.USER
        },
        senderId = this.userId ?: this.characterId,
        senderName = this.characterName ?: "User",
        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(this.timestamp)),
        messageType = if (this.messageType == "image") MessageType.IMAGE else MessageType.TEXT,
        metadata = if (this.metadataJson != null) {
            try {
                val obj = org.json.JSONObject(this.metadataJson)
                MessageResponseMetadata(
                    imageUrl = obj.optString("imageUrl", obj.optString("localPath", null)),
                    generationTime = obj.optLong("generationTime", 0),
                    modelUsed = obj.optString("modelUsed", null)
                )
            } catch (e: Exception) { null }
        } else null
    )
}
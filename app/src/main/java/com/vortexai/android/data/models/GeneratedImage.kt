package com.vortexai.android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_images")
data class GeneratedImage(
    @PrimaryKey val id: String,
    val prompt: String,
    val localPath: String,
    val model: String?,
    val generationTime: Long,
    val size: String,
    val timestamp: Long
) 
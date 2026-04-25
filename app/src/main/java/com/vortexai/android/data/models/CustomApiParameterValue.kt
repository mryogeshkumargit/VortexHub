package com.vortexai.android.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "custom_api_parameter_values",
    primaryKeys = ["modelId", "paramName"],
    foreignKeys = [
        ForeignKey(
            entity = CustomApiModel::class,
            parentColumns = ["id"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("modelId")]
)
data class CustomApiParameterValue(
    val modelId: String,
    val paramName: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

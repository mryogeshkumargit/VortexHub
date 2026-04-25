package com.vortexai.android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    val id: String,
    val username: String,
    val email: String?,
    val fullName: String?,
    val dateOfBirth: String?,
    val avatarUrl: String?,
    val isPremium: Boolean = false,
    val accessToken: String?,
    val refreshToken: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

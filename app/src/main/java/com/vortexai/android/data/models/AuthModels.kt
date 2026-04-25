package com.vortexai.android.data.models

import com.google.gson.annotations.SerializedName

/**
 * Login request model
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("remember_me")
    val rememberMe: Boolean = false
)

/**
 * Registration request model
 */
data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("confirm_password")
    val confirmPassword: String
)

/**
 * Authentication response model
 */
data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("user")
    val user: User?,
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_in")
    val expiresIn: Long?
)

/**
 * User model
 */
data class AuthUser(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("is_premium")
    val isPremium: Boolean = false,
    @SerializedName("preferences")
    val preferences: UserPreferences?
)

/**
 * User preferences model
 */
data class AuthUserPreferences(
    @SerializedName("theme")
    val theme: String = "system",
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean = true,
    @SerializedName("voice_enabled")
    val voiceEnabled: Boolean = true,
    @SerializedName("auto_save_conversations")
    val autoSaveConversations: Boolean = true
)

/**
 * Token refresh request
 */
data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * Password reset request
 */
data class PasswordResetRequest(
    @SerializedName("email")
    val email: String
)

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: T?,
    @SerializedName("error")
    val error: String?
) 

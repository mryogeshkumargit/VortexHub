package com.vortexai.android.data.api

import com.vortexai.android.data.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Authentication API service interface
 * Defines all authentication-related network endpoints
 */
interface AuthApiService {
    
    /**
     * User login
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    /**
     * User registration
     */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    /**
     * Refresh access token
     */
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>
    
    /**
     * User logout
     */
    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<ApiResponse<Unit>>
    
    /**
     * Password reset request
     */
    @POST("api/auth/forgot-password")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<ApiResponse<Unit>>
    
    /**
     * Get current user profile
     */
    @GET("api/auth/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<ApiResponse<User>>
    
    /**
     * Update user profile
     */
    @PUT("api/auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body user: User
    ): Response<ApiResponse<User>>
    
    /**
     * Change password
     */
    @POST("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Unit>>
    
    /**
     * Delete user account
     */
    @DELETE("api/auth/account")
    suspend fun deleteAccount(@Header("Authorization") token: String): Response<ApiResponse<Unit>>
}

/**
 * Change password request model
 */
data class ChangePasswordRequest(
    @com.google.gson.annotations.SerializedName("current_password")
    val currentPassword: String,
    @com.google.gson.annotations.SerializedName("new_password")
    val newPassword: String,
    @com.google.gson.annotations.SerializedName("confirm_password")
    val confirmPassword: String
) 
package com.vortexai.android.utils

import android.util.Log
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Centralized API error handling utility
 * Provides consistent error handling and user-friendly error messages
 */
object ApiErrorHandler {
    
    private const val TAG = "ApiErrorHandler"
    
    /**
     * Handle API errors and return user-friendly messages
     */
    fun handleApiError(
        provider: String,
        endpoint: String,
        error: Throwable,
        responseCode: Int? = null,
        responseBody: String? = null
    ): ApiError {
        
        Log.e(TAG, "API Error - Provider: $provider, Endpoint: $endpoint", error)
        
        return when (error) {
            is UnknownHostException -> ApiError(
                type = ApiErrorType.NETWORK_ERROR,
                message = "Unable to connect to $provider. Please check your internet connection.",
                technicalMessage = "DNS resolution failed for $endpoint",
                isRetryable = true
            )
            
            is ConnectException -> ApiError(
                type = ApiErrorType.NETWORK_ERROR,
                message = "Connection to $provider failed. The service might be temporarily unavailable.",
                technicalMessage = "Connection refused to $endpoint",
                isRetryable = true
            )
            
            is SocketTimeoutException -> ApiError(
                type = ApiErrorType.TIMEOUT_ERROR,
                message = "$provider is taking too long to respond. Please try again.",
                technicalMessage = "Request timeout for $endpoint",
                isRetryable = true
            )
            
            is SSLException -> ApiError(
                type = ApiErrorType.SECURITY_ERROR,
                message = "Secure connection to $provider failed. Please check your network settings.",
                technicalMessage = "SSL/TLS error for $endpoint",
                isRetryable = false
            )
            
            else -> {
                // Handle HTTP errors based on response code
                when (responseCode) {
                    400 -> handleBadRequestError(provider, responseBody)
                    401 -> handleAuthError(provider)
                    403 -> handleForbiddenError(provider)
                    404 -> handleNotFoundError(provider, endpoint)
                    429 -> handleRateLimitError(provider)
                    500, 502, 503, 504 -> handleServerError(provider, responseCode)
                    else -> ApiError(
                        type = ApiErrorType.UNKNOWN_ERROR,
                        message = "An unexpected error occurred with $provider. Please try again.",
                        technicalMessage = error.message ?: "Unknown error",
                        isRetryable = true
                    )
                }
            }
        }
    }
    
    private fun handleBadRequestError(provider: String, responseBody: String?): ApiError {
        val errorMessage = parseErrorMessage(responseBody) ?: "Invalid request parameters"
        
        return ApiError(
            type = ApiErrorType.VALIDATION_ERROR,
            message = "Request to $provider failed: $errorMessage",
            technicalMessage = responseBody ?: "Bad request (400)",
            isRetryable = false
        )
    }
    
    private fun handleAuthError(provider: String): ApiError {
        return ApiError(
            type = ApiErrorType.AUTH_ERROR,
            message = "Authentication failed for $provider. Please check your API key.",
            technicalMessage = "Unauthorized (401)",
            isRetryable = false
        )
    }
    
    private fun handleForbiddenError(provider: String): ApiError {
        return ApiError(
            type = ApiErrorType.AUTH_ERROR,
            message = "Access denied by $provider. Your API key may not have the required permissions.",
            technicalMessage = "Forbidden (403)",
            isRetryable = false
        )
    }
    
    private fun handleNotFoundError(provider: String, endpoint: String): ApiError {
        return ApiError(
            type = ApiErrorType.CONFIGURATION_ERROR,
            message = "The requested $provider service is not available. The endpoint may have changed.",
            technicalMessage = "Not found (404) for $endpoint",
            isRetryable = false
        )
    }
    
    private fun handleRateLimitError(provider: String): ApiError {
        return ApiError(
            type = ApiErrorType.RATE_LIMIT_ERROR,
            message = "You've exceeded the rate limit for $provider. Please wait a moment and try again.",
            technicalMessage = "Rate limit exceeded (429)",
            isRetryable = true
        )
    }
    
    private fun handleServerError(provider: String, responseCode: Int): ApiError {
        return ApiError(
            type = ApiErrorType.SERVER_ERROR,
            message = "$provider is experiencing technical difficulties. Please try again later.",
            technicalMessage = "Server error ($responseCode)",
            isRetryable = true
        )
    }
    
    /**
     * Parse error message from API response body
     */
    private fun parseErrorMessage(responseBody: String?): String? {
        if (responseBody.isNullOrBlank()) return null
        
        return try {
            val json = JSONObject(responseBody)
            
            // Try common error message fields
            json.optString("error")?.takeIf { it.isNotBlank() }
                ?: json.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("detail")?.takeIf { it.isNotBlank() }
                ?: json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            // If JSON parsing fails, try to extract readable text
            responseBody.take(200) // Limit length for display
        }
    }
    
    /**
     * Get retry delay based on error type
     */
    fun getRetryDelay(error: ApiError, attemptNumber: Int): Long {
        return when (error.type) {
            ApiErrorType.RATE_LIMIT_ERROR -> (attemptNumber * 2000L).coerceAtMost(30000L) // 2s, 4s, 6s... max 30s
            ApiErrorType.SERVER_ERROR -> (attemptNumber * 1000L).coerceAtMost(10000L) // 1s, 2s, 3s... max 10s
            ApiErrorType.NETWORK_ERROR, ApiErrorType.TIMEOUT_ERROR -> (attemptNumber * 500L).coerceAtMost(5000L) // 0.5s, 1s, 1.5s... max 5s
            else -> 0L // No retry for non-retryable errors
        }
    }
    
    /**
     * Check if error should trigger a retry
     */
    fun shouldRetry(error: ApiError, attemptNumber: Int, maxRetries: Int = 3): Boolean {
        return error.isRetryable && attemptNumber < maxRetries
    }
}

/**
 * Structured API error information
 */
data class ApiError(
    val type: ApiErrorType,
    val message: String, // User-friendly message
    val technicalMessage: String, // Technical details for logging
    val isRetryable: Boolean = false
)

/**
 * Types of API errors
 */
enum class ApiErrorType {
    NETWORK_ERROR,      // Connection issues
    TIMEOUT_ERROR,      // Request timeout
    AUTH_ERROR,         // Authentication/authorization issues
    VALIDATION_ERROR,   // Bad request parameters
    RATE_LIMIT_ERROR,   // Rate limiting
    SERVER_ERROR,       // Server-side errors
    SECURITY_ERROR,     // SSL/TLS issues
    CONFIGURATION_ERROR, // Endpoint/service configuration issues
    UNKNOWN_ERROR       // Unexpected errors
}
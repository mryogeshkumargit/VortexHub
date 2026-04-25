package com.vortexai.android.core.common.result

import kotlinx.serialization.Serializable

/**
 * Represents different types of errors that can occur in the app.
 * This provides structured error handling for better user experience.
 */
@Serializable
sealed class AppError {
    abstract val message: String
    abstract val code: String
    
    @Serializable
    data class Network(
        override val message: String,
        override val code: String = "NETWORK_ERROR",
        val statusCode: Int? = null
    ) : AppError()
    
    @Serializable
    data class Api(
        override val message: String,
        override val code: String,
        val statusCode: Int,
        val errorBody: String? = null
    ) : AppError()
    
    @Serializable
    data class Database(
        override val message: String,
        override val code: String = "DATABASE_ERROR"
    ) : AppError()
    
    @Serializable
    data class Authentication(
        override val message: String,
        override val code: String = "AUTH_ERROR"
    ) : AppError()
    
    @Serializable
    data class Validation(
        override val message: String,
        override val code: String = "VALIDATION_ERROR",
        val field: String? = null
    ) : AppError()
    
    @Serializable
    data class Permission(
        override val message: String,
        override val code: String = "PERMISSION_ERROR",
        val permission: String? = null
    ) : AppError()
    
    @Serializable
    data class FileSystem(
        override val message: String,
        override val code: String = "FILE_ERROR"
    ) : AppError()
    
    @Serializable
    data class LLM(
        override val message: String,
        override val code: String = "LLM_ERROR",
        val provider: String? = null,
        val modelId: String? = null
    ) : AppError()
    
    @Serializable
    data class Character(
        override val message: String,
        override val code: String = "CHARACTER_ERROR",
        val characterId: String? = null
    ) : AppError()
    
    @Serializable
    data class Conversation(
        override val message: String,
        override val code: String = "CONVERSATION_ERROR",
        val conversationId: String? = null
    ) : AppError()
    
    @Serializable
    data class Unknown(
        override val message: String,
        override val code: String = "UNKNOWN_ERROR"
    ) : AppError()

    companion object {
        fun fromException(exception: Exception): AppError = when (exception) {
            is java.net.UnknownHostException -> Network(
                message = "No internet connection available",
                code = "NO_INTERNET"
            )
            is java.net.SocketTimeoutException -> Network(
                message = "Request timed out",
                code = "TIMEOUT"
            )
            is java.io.IOException -> Network(
                message = "Network error occurred",
                code = "IO_ERROR"
            )
            is SecurityException -> Permission(
                message = "Permission denied: ${exception.message}",
                code = "SECURITY_ERROR"
            )
            else -> Unknown(
                message = exception.message ?: "An unknown error occurred",
                code = "EXCEPTION_${exception::class.simpleName?.uppercase()}"
            )
        }
    }
}

/**
 * Error handler interface for processing and logging errors
 */
interface ErrorHandler {
    fun handleError(error: AppError)
    fun logError(error: AppError, throwable: Throwable? = null)
}

/**
 * Default implementation of ErrorHandler
 */
class DefaultErrorHandler : ErrorHandler {
    override fun handleError(error: AppError) {
        // Log error for debugging
        logError(error)
        
        // Could add crash reporting here (Firebase Crashlytics, etc.)
        // Could add analytics tracking here
    }
    
    override fun logError(error: AppError, throwable: Throwable?) {
        timber.log.Timber.e(throwable, "AppError [${error.code}]: ${error.message}")
    }
} 
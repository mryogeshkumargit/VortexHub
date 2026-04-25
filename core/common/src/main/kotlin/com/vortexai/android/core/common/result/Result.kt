package com.vortexai.android.core.common.result

import kotlinx.serialization.Serializable

/**
 * A generic wrapper for handling success and error states.
 * This provides a type-safe way to handle operations that can fail.
 */
@Serializable
sealed class Result<out T> {
    @Serializable
    data class Success<T>(val data: T) : Result<T>()
    
    @Serializable
    data class Error(val exception: AppError) : Result<Nothing>()
    
    @Serializable
    data object Loading : Result<Nothing>()
}

/**
 * Returns true if this Result is Success
 */
val <T> Result<T>.isSuccess: Boolean
    get() = this is Result.Success

/**
 * Returns true if this Result is Error
 */
val <T> Result<T>.isError: Boolean
    get() = this is Result.Error

/**
 * Returns true if this Result is Loading
 */
val <T> Result<T>.isLoading: Boolean
    get() = this is Result.Loading

/**
 * Returns the data if Success, null otherwise
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Returns the data if Success, or the default value if Error/Loading
 */
fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> data
    else -> default
}

/**
 * Transforms the data if Success, preserves Error/Loading state
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Transforms the data if Success with another Result, flattens nested Results
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Executes the given action if this Result is Success
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Executes the given action if this Result is Error
 */
inline fun <T> Result<T>.onError(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}

/**
 * Executes the given action if this Result is Loading
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) action()
    return this
}

/**
 * Converts a nullable value to a Result
 */
fun <T> T?.toResult(error: AppError = AppError.Unknown("Value is null")): Result<T> =
    if (this != null) Result.Success(this) else Result.Error(error)

/**
 * Safely executes a block and wraps the result
 */
inline fun <T> safeCall(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(AppError.fromException(e))
} 
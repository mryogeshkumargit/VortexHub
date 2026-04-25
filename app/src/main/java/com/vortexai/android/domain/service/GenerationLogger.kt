package com.vortexai.android.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class LogType {
    INFO, REQUEST, RESPONSE, ERROR
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val provider: String,
    val endpoint: String? = null,
    val modelOrWorkflow: String? = null,
    val character: String? = null,
    val requestData: String? = null,
    val responseData: String? = null,
    val message: String? = null,
    val isError: Boolean = false
)

@Singleton
class GenerationLogger @Inject constructor() {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun logInfo(provider: String, message: String) {
        addLog(
            LogEntry(
                type = LogType.INFO,
                provider = provider,
                message = message
            )
        )
    }

    fun logRequest(
        provider: String,
        endpoint: String,
        modelOrWorkflow: String,
        character: String?,
        requestData: String
    ) {
        addLog(
            LogEntry(
                type = LogType.REQUEST,
                provider = provider,
                endpoint = endpoint,
                modelOrWorkflow = modelOrWorkflow,
                character = character,
                requestData = requestData
            )
        )
    }

    fun logResponse(
        provider: String,
        responseData: String,
        isError: Boolean = false
    ) {
        addLog(
            LogEntry(
                type = LogType.RESPONSE,
                provider = provider,
                responseData = responseData,
                isError = isError
            )
        )
    }

    fun logError(provider: String, message: String, errorData: String? = null) {
        addLog(
            LogEntry(
                type = LogType.ERROR,
                provider = provider,
                message = message,
                responseData = errorData,
                isError = true
            )
        )
    }

    private fun addLog(entry: LogEntry) {
        _logs.value = listOf(entry) + _logs.value
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}

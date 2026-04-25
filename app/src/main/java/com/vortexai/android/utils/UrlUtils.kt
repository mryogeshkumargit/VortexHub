package com.vortexai.android.utils

/**
 * Utility helpers for URL manipulation.
 */
object UrlUtils {
    /**
     * Return a base URL suitable for API calls.
     * - Trims whitespace
     * - Removes trailing slashes
     * - Strips paths like "/api/..." so only the scheme, host and port remain.
     *
     * Examples:
     *  " http://192.168.1.7:11435/api/tags " -> "http://192.168.1.7:11435"
     *  "http://localhost:11434/"             -> "http://localhost:11434"
     */
    fun cleanBaseUrl(raw: String): String {
        var url = raw.trim()
        // Fast exit
        if (url.isEmpty()) return url

        // Remove trailing slash
        url = url.removeSuffix("/")

        // If user pasted a full path that includes /api, strip everything from /api onwards
        val apiIndex = url.indexOf("/api")
        if (apiIndex != -1) {
            url = url.substring(0, apiIndex)
        }
        return url
    }
} 
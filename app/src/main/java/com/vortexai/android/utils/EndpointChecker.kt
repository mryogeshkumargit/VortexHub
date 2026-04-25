package com.vortexai.android.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Utility for checking if a given API endpoint is reachable. Performs a HEAD (or GET fallback)
 * request to the specified path with a short timeout so it can be safely called from the UI
 * without blocking for long periods.
 */
object EndpointChecker {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * Quick reachability test.
     * @param baseUrl The base endpoint (e.g. "http://192.168.1.7:11435") without trailing slash.
     * @param testPath Path to hit for the reachability probe. Defaults to root.
     * @return true if the server responded within timeout, false otherwise.
     */
    suspend fun isReachable(baseUrl: String, testPath: String = "/"): Boolean =
        withContext(Dispatchers.IO) {
            val sanitized = baseUrl.trim().removeSuffix("/")
            val url = sanitized + testPath
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .build()
                val response = httpClient.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                // HEAD not allowed on some servers – fall back to GET once
                try {
                    val getReq = Request.Builder()
                        .url(url)
                        .get()
                        .build()
                    val resp = httpClient.newCall(getReq).execute()
                    resp.isSuccessful
                } catch (_: Exception) {
                    false
                }
            }
        }
} 
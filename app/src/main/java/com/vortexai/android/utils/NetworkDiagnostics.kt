package com.vortexai.android.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Utility class for network diagnostics
 */
object NetworkDiagnostics {
    private const val TAG = "NetworkDiagnostics"
    
    /**
     * Check if the device has an active internet connection
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
    
    /**
     * Check if the device can resolve a hostname
     */
    suspend fun canResolveHost(hostname: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val addresses = InetAddress.getAllByName(hostname)
            val result = addresses.isNotEmpty()
            
            if (result) {
                Log.d(TAG, "Successfully resolved $hostname to ${addresses.joinToString { it.hostAddress }}")
            } else {
                Log.d(TAG, "Could not resolve $hostname (no addresses returned)")
            }
            
            result
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Failed to resolve $hostname: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving $hostname", e)
            false
        }
    }
    
    /**
     * Run a full network diagnostic
     * @return A diagnostic report as a string
     */
    suspend fun runDiagnostics(context: Context): String = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        
        // Check basic connectivity
        val isNetworkAvailable = isNetworkAvailable(context)
        report.appendLine("Network available: $isNetworkAvailable")
        
        // Check DNS resolution for key domains
        val domains = listOf(
            "modelslab.com",
            "api.modelslab.com",
            "google.com",
            "cloudflare.com"
        )
        
        for (domain in domains) {
            val canResolve = canResolveHost(domain)
            report.appendLine("Can resolve $domain: $canResolve")
            
            if (canResolve) {
                try {
                    val addresses = InetAddress.getAllByName(domain)
                    report.appendLine("  IP addresses: ${addresses.joinToString { it.hostAddress }}")
                } catch (e: Exception) {
                    report.appendLine("  Error getting IP addresses: ${e.message}")
                }
            }
        }
        
        report.toString()
    }
} 
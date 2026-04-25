package com.vortexai.android.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Utility class to test ModelsLab API connectivity
 */
object ModelsLabConnectionTest {
    private const val TAG = "ModelsLabConnectionTest"
    private const val TEST_URL = "https://modelslab.com/api/v6/health"
    private const val ALTERNATE_IP_1 = "104.21.32.117" // Update if IP changes
    private const val ALTERNATE_IP_2 = "172.67.170.249" // Update if IP changes
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Test connectivity to ModelsLab API using multiple methods
     * @return A report of the connectivity test results
     */
    suspend fun testConnection(context: Context): String = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        report.appendLine("=== ModelsLab API Connection Test ===")
        
        // Test DNS resolution
        try {
            report.appendLine("\nDNS Resolution Test:")
            val addresses = InetAddress.getAllByName("modelslab.com")
            if (addresses.isNotEmpty()) {
                report.appendLine("✓ DNS resolution successful")
                report.appendLine("IP addresses: ${addresses.joinToString { it.hostAddress }}")
            } else {
                report.appendLine("✗ DNS resolution failed (no addresses returned)")
            }
        } catch (e: Exception) {
            report.appendLine("✗ DNS resolution failed: ${e.message}")
        }
        
        // Test standard HTTP connection
        try {
            report.appendLine("\nStandard HTTP Connection Test:")
            val request = Request.Builder().url(TEST_URL).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                report.appendLine("✓ API connection successful (HTTP ${response.code})")
            } else {
                report.appendLine("✗ API connection failed (HTTP ${response.code})")
            }
        } catch (e: Exception) {
            report.appendLine("✗ API connection failed: ${e.message}")
        }
        
        // Test connection using IP address directly (bypass DNS)
        try {
            report.appendLine("\nDirect IP Connection Test 1:")
            val ipRequest = Request.Builder()
                .url("https://$ALTERNATE_IP_1/api/v6/health")
                .header("Host", "modelslab.com")
                .build()
            val ipResponse = client.newCall(ipRequest).execute()
            if (ipResponse.isSuccessful) {
                report.appendLine("✓ Direct IP connection successful (HTTP ${ipResponse.code})")
            } else {
                report.appendLine("✗ Direct IP connection failed (HTTP ${ipResponse.code})")
            }
        } catch (e: Exception) {
            report.appendLine("✗ Direct IP connection failed: ${e.message}")
        }
        
        // Test connection using alternate IP address
        try {
            report.appendLine("\nDirect IP Connection Test 2:")
            val ipRequest2 = Request.Builder()
                .url("https://$ALTERNATE_IP_2/api/v6/health")
                .header("Host", "modelslab.com")
                .build()
            val ipResponse2 = client.newCall(ipRequest2).execute()
            if (ipResponse2.isSuccessful) {
                report.appendLine("✓ Alternate IP connection successful (HTTP ${ipResponse2.code})")
            } else {
                report.appendLine("✗ Alternate IP connection failed (HTTP ${ipResponse2.code})")
            }
        } catch (e: Exception) {
            report.appendLine("✗ Alternate IP connection failed: ${e.message}")
        }
        
        // Test network availability
        val isNetworkAvailable = NetworkDiagnostics.isNetworkAvailable(context)
        report.appendLine("\nNetwork Status:")
        report.appendLine(if (isNetworkAvailable) "✓ Network is available" else "✗ Network is not available")
        
        report.toString()
    }
} 
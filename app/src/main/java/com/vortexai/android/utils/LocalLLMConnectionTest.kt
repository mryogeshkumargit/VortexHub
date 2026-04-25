package com.vortexai.android.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Utility class to test connectivity to local LLM services (Ollama and Kobold)
 */
object LocalLLMConnectionTest {
    private const val TAG = "LocalLLMConnectionTest"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Test Ollama service connectivity and capabilities
     */
    suspend fun testOllamaConnection(
        context: Context,
        baseUrl: String = "http://192.168.1.7:11435"
    ): String = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        report.appendLine("=== OLLAMA SERVICE TEST ===")
        report.appendLine("Testing endpoint: $baseUrl")
        report.appendLine()
        
        val cleanUrl = baseUrl.removeSuffix("/")
        
        // Test 1: Basic connectivity
        report.appendLine("1. Basic Connectivity Test:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/api/tags")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                report.appendLine("   ✓ Ollama service is reachable (HTTP ${response.code})")
                
                // Parse available models
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val models = json.getJSONArray("models")
                        report.appendLine("   ✓ Found ${models.length()} available models:")
                        
                        for (i in 0 until models.length()) {
                            val model = models.getJSONObject(i)
                            val name = model.getString("name")
                            val size = model.optLong("size", 0)
                            val sizeStr = if (size > 0) " (${formatBytes(size)})" else ""
                            report.appendLine("     - $name$sizeStr")
                        }
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Could not parse models list: ${e.message}")
                    }
                }
            } else {
                report.appendLine("   ✗ Ollama service not reachable (HTTP ${response.code})")
                report.appendLine("   Error: ${response.body?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            report.appendLine("   ✗ Connection failed: ${e.message}")
        }
        
        // Test 2: Version info
        report.appendLine("\n2. Version Information:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/api/version")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val version = json.optString("version", "unknown")
                        report.appendLine("   ✓ Ollama version: $version")
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Version info available but could not parse")
                    }
                }
            } else {
                report.appendLine("   ⚠ Version endpoint not available (HTTP ${response.code})")
            }
        } catch (e: Exception) {
            report.appendLine("   ⚠ Version check failed: ${e.message}")
        }
        
        // Test 3: Simple generation test
        report.appendLine("\n3. Generation Test:")
        try {
            // First, get the first available model
            val tagsRequest = Request.Builder()
                .url("$cleanUrl/api/tags")
                .get()
                .build()
            
            val tagsResponse = client.newCall(tagsRequest).execute()
            if (tagsResponse.isSuccessful) {
                val tagsBody = tagsResponse.body?.string()
                if (tagsBody != null) {
                    val json = JSONObject(tagsBody)
                    val models = json.getJSONArray("models")
                    
                    if (models.length() > 0) {
                        val firstModel = models.getJSONObject(0).getString("name")
                        report.appendLine("   Testing with model: $firstModel")
                        
                        // Test generation
                        val genRequest = JSONObject().apply {
                            put("model", firstModel)
                            put("prompt", "Hello! Please respond with 'Test successful'")
                            put("stream", false)
                            put("options", JSONObject().apply {
                                put("temperature", 0.1)
                                put("num_predict", 50)
                            })
                        }
                        
                        val requestBody = genRequest.toString()
                            .toRequestBody("application/json".toMediaType())
                        
                        val generateRequest = Request.Builder()
                            .url("$cleanUrl/api/generate")
                            .post(requestBody)
                            .build()
                        
                        val generateResponse = client.newCall(generateRequest).execute()
                        if (generateResponse.isSuccessful) {
                            val generateBody = generateResponse.body?.string()
                            if (generateBody != null) {
                                val genJson = JSONObject(generateBody)
                                val response = genJson.getString("response")
                                report.appendLine("   ✓ Generation successful!")
                                report.appendLine("   Response: ${response.take(100)}${if (response.length > 100) "..." else ""}")
                            }
                        } else {
                            report.appendLine("   ✗ Generation failed (HTTP ${generateResponse.code})")
                            report.appendLine("   Error: ${generateResponse.body?.string()}")
                        }
                    } else {
                        report.appendLine("   ⚠ No models available for testing")
                    }
                }
            } else {
                report.appendLine("   ⚠ Could not get models list for testing")
            }
        } catch (e: Exception) {
            report.appendLine("   ✗ Generation test failed: ${e.message}")
        }
        
        // Test 4: Network diagnostics
        report.appendLine("\n4. Network Diagnostics:")
        try {
            val host = baseUrl.substringAfter("://").substringBefore(":")
            val addresses = InetAddress.getAllByName(host)
            report.appendLine("   ✓ DNS resolution successful")
            report.appendLine("   IP addresses: ${addresses.joinToString { it.hostAddress }}")
        } catch (e: Exception) {
            report.appendLine("   ✗ DNS resolution failed: ${e.message}")
        }
        
        // Test network availability
        val isNetworkAvailable = NetworkDiagnostics.isNetworkAvailable(context)
        report.appendLine("   Network available: $isNetworkAvailable")
        
        report.toString()
    }
    
    /**
     * Test Kobold service connectivity and capabilities
     */
    suspend fun testKoboldConnection(
        context: Context,
        baseUrl: String = "http://192.168.1.7:5000"
    ): String = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        report.appendLine("=== KOBOLD AI SERVICE TEST ===")
        report.appendLine("Testing endpoint: $baseUrl")
        report.appendLine()
        
        val cleanUrl = baseUrl.removeSuffix("/")
        
        // Test 1: Basic connectivity
        report.appendLine("1. Basic Connectivity Test:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/api/v1/info/version")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                report.appendLine("   ✓ Kobold service is reachable (HTTP ${response.code})")
                
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val version = json.optString("result", "unknown")
                        report.appendLine("   ✓ Kobold version: $version")
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Version info available but could not parse")
                    }
                }
            } else {
                report.appendLine("   ✗ Kobold service not reachable (HTTP ${response.code})")
                report.appendLine("   Error: ${response.body?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            report.appendLine("   ✗ Connection failed: ${e.message}")
        }
        
        // Test 2: Model info
        report.appendLine("\n2. Model Information:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/api/v1/model")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val result = json.optString("result", "No model loaded")
                        report.appendLine("   ✓ Current model: $result")
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Model info available but could not parse")
                    }
                }
            } else {
                report.appendLine("   ⚠ Model endpoint not available (HTTP ${response.code})")
            }
        } catch (e: Exception) {
            report.appendLine("   ⚠ Model check failed: ${e.message}")
        }
        
        // Test 3: Configuration check
        report.appendLine("\n3. Configuration Check:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/api/v1/config/max_length")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val maxLength = json.optInt("result", 0)
                        report.appendLine("   ✓ Max length: $maxLength tokens")
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Config info available but could not parse")
                    }
                }
            } else {
                report.appendLine("   ⚠ Config endpoint not available (HTTP ${response.code})")
            }
        } catch (e: Exception) {
            report.appendLine("   ⚠ Config check failed: ${e.message}")
        }
        
        // Test 4: Simple generation test
        report.appendLine("\n4. Generation Test:")
        try {
            val genRequest = JSONObject().apply {
                put("prompt", "Hello! Please respond with 'Test successful'")
                put("max_context_length", 2048)
                put("max_length", 50)
                put("temperature", 0.1)
                put("top_p", 0.9)
                put("rep_pen", 1.1)
                put("rep_pen_range", 1024)
                put("rep_pen_slope", 0.9)
                put("tfs", 0.95)
                put("top_a", 0.0)
                put("top_k", 0)
                put("typical", 1.0)
                put("frmttriminc", true)
                put("frmtrmblln", false)
            }
            
            val requestBody = genRequest.toString()
                .toRequestBody("application/json".toMediaType())
            
            val generateRequest = Request.Builder()
                .url("$cleanUrl/api/v1/generate")
                .post(requestBody)
                .build()
            
            val generateResponse = client.newCall(generateRequest).execute()
            if (generateResponse.isSuccessful) {
                val generateBody = generateResponse.body?.string()
                if (generateBody != null) {
                    val genJson = JSONObject(generateBody)
                    val response = when {
                        genJson.has("results") -> {
                            val resultsArray = genJson.getJSONArray("results")
                            if (resultsArray.length() > 0) {
                                resultsArray.getJSONObject(0).getString("text")
                            } else {
                                "No results in response"
                            }
                        }
                        genJson.has("text") -> {
                            genJson.getString("text")
                        }
                        else -> {
                            "Unexpected response format"
                        }
                    }
                    report.appendLine("   ✓ Generation successful!")
                    report.appendLine("   Response: ${response.take(100)}${if (response.length > 100) "..." else ""}")
                }
            } else {
                report.appendLine("   ✗ Generation failed (HTTP ${generateResponse.code})")
                report.appendLine("   Error: ${generateResponse.body?.string()}")
            }
        } catch (e: Exception) {
            report.appendLine("   ✗ Generation test failed: ${e.message}")
        }
        
        // Test 5: Network diagnostics
        report.appendLine("\n5. Network Diagnostics:")
        try {
            val host = baseUrl.substringAfter("://").substringBefore(":")
            val addresses = InetAddress.getAllByName(host)
            report.appendLine("   ✓ DNS resolution successful")
            report.appendLine("   IP addresses: ${addresses.joinToString { it.hostAddress }}")
        } catch (e: Exception) {
            report.appendLine("   ✗ DNS resolution failed: ${e.message}")
        }
        
        // Test network availability
        val isNetworkAvailable = NetworkDiagnostics.isNetworkAvailable(context)
        report.appendLine("   Network available: $isNetworkAvailable")
        
        report.toString()
    }
    
    /**
     * Run comprehensive tests for both services
     */
    suspend fun testBothServices(
        context: Context,
        ollamaUrl: String = "http://192.168.1.7:11435",
        koboldUrl: String = "http://192.168.1.7:5000"
    ): String = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        report.appendLine("=== LOCAL LLM SERVICES CONNECTIVITY TEST ===")
        report.appendLine("Testing both Ollama and Kobold services on local network")
        report.appendLine("Timestamp: ${System.currentTimeMillis()}")
        report.appendLine()
        
        // Test Ollama
        try {
            val ollamaReport = testOllamaConnection(context, ollamaUrl)
            report.appendLine(ollamaReport)
        } catch (e: Exception) {
            report.appendLine("=== OLLAMA SERVICE TEST ===")
            report.appendLine("✗ Ollama test failed completely: ${e.message}")
        }
        
        report.appendLine("\n" + "=".repeat(50) + "\n")
        
        // Test Kobold
        try {
            val koboldReport = testKoboldConnection(context, koboldUrl)
            report.appendLine(koboldReport)
        } catch (e: Exception) {
            report.appendLine("=== KOBOLD AI SERVICE TEST ===")
            report.appendLine("✗ Kobold test failed completely: ${e.message}")
        }
        
        report.appendLine("\n" + "=".repeat(50))
        report.appendLine("TEST COMPLETED")
        
        report.toString()
    }
    
    /**
     * Test connectivity to a custom endpoint
     */
    suspend fun testCustomEndpoint(
        context: Context,
        endpoint: String,
        serviceName: String = "Custom Service"
    ): String = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        report.appendLine("=== $serviceName TEST ===")
        report.appendLine("Testing endpoint: $endpoint")
        report.appendLine()
        
        // Basic connectivity test
        try {
            val request = Request.Builder()
                .url(endpoint)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            report.appendLine("✓ Service reachable (HTTP ${response.code})")
            
            val responseBody = response.body?.string()
            if (responseBody != null) {
                report.appendLine("Response length: ${responseBody.length} characters")
                if (responseBody.length < 500) {
                    report.appendLine("Response: $responseBody")
                } else {
                    report.appendLine("Response preview: ${responseBody.take(200)}...")
                }
            }
        } catch (e: Exception) {
            report.appendLine("✗ Connection failed: ${e.message}")
        }
        
        report.toString()
    }
    
    /**
     * Format bytes to human readable format
     */
    private fun formatBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]
        return "%.1f %sB".format(bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
} 
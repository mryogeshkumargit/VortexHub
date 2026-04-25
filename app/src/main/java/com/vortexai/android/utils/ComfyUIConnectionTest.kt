package com.vortexai.android.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ComfyUIConnectionTest {
    private const val TAG = "ComfyUIConnectionTest"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class ComfyUITestResult(
        val success: Boolean,
        val report: String,
        val availableModels: List<String> = emptyList(),
        val availableSDXLLoras: List<String> = emptyList(),
        val availableFluxLoras: List<String> = emptyList()
    )

    /**
     * Fetch LoRA filenames via the dedicated /loras endpoint (introduced in ComfyUI 0.0.20-ish).
     * Falls back to object_info parsing if the endpoint is unavailable.
     */
    suspend fun fetchAvailableLoras(baseUrl: String): Pair<List<String>, List<String>> = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.removeSuffix("/")
        val sdxl = mutableListOf<String>()
        val flux = mutableListOf<String>()

        var usedLorasEndpoint = false
        try {
            val request = Request.Builder()
                .url("$cleanUrl/loras")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { raw ->
                    try {
                        val arr = JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val fileName = obj.optString("filename", obj.optString("name"))
                            if (fileName.isNotBlank()) {
                                val lower = fileName.lowercase()
                                when {
                                    lower.contains("flux") || lower.contains("dev") -> flux.add(fileName)
                                    lower.contains("sdxl") || lower.contains("xl") -> sdxl.add(fileName)
                                    else -> sdxl.add(fileName)
                                }
                            }
                        }
                        usedLorasEndpoint = sdxl.isNotEmpty() || flux.isNotEmpty()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse /loras response: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to hit /loras endpoint: ${e.message}")
        }

        // Fallback to object_info parsing if no LoRAs found
        if (!usedLorasEndpoint) {
            try {
                val req = Request.Builder().url("$cleanUrl/object_info").get().build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    resp.body?.string()?.let { body ->
                        try {
                            val json = JSONObject(body)
                            val loraLoader = json.optJSONObject("LoraLoader")
                            val input = loraLoader?.optJSONObject("input")
                            val required = input?.optJSONObject("required")
                            val loraName = required?.optJSONArray("lora_name")
                            // Replace null-check if with let to avoid expression issues
                            val loraList = loraName?.optJSONArray(0)
                            loraList?.let { list ->
                                for (i in 0 until list.length()) {
                                    val file = list.optString(i)
                                    val lower = file.lowercase()
                                    if (lower.contains("flux") || lower.contains("dev")) flux.add(file)
                                    else sdxl.add(file)
                                }
                            }
                        } catch (ie: Exception) {
                            Log.w(TAG, "Fallback object_info parse failed: ${ie.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback object_info request failed: ${e.message}")
            }
        }

        return@withContext Pair(sdxl, flux)
    }

    suspend fun testComfyUIConnectivity(baseUrl: String): ComfyUITestResult = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        val cleanUrl = baseUrl.removeSuffix("/")
        var success = false
        val models = mutableListOf<String>()
        val sdxlLoras = mutableListOf<String>()
        val fluxLoras = mutableListOf<String>()
        
        report.appendLine("=== COMFYUI SERVICE TEST ===")
        report.appendLine("Testing endpoint: $cleanUrl")
        report.appendLine()
        
        // Validate URL format
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            report.appendLine("✗ Invalid URL format. Please use http:// or https://")
            return@withContext ComfyUITestResult(false, report.toString())
        }
        
        if (cleanUrl.length < 10) {
            report.appendLine("✗ URL too short. Please enter a complete URL like http://localhost:8188")
            return@withContext ComfyUITestResult(false, report.toString())
        }
        
        // Test 1: Basic connectivity
        report.appendLine("1. Basic Connectivity Test:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/system_stats")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                report.appendLine("   ✓ ComfyUI service is reachable (HTTP ${response.code})")
                success = true
                
                // Parse system stats if available
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val system = json.optJSONObject("system")
                        if (system != null) {
                            val ram = system.optJSONObject("ram")
                            val vram = system.optJSONObject("vram")
                            if (ram != null) {
                                val totalRam = ram.optLong("total", 0) / (1024 * 1024 * 1024)
                                val freeRam = ram.optLong("free", 0) / (1024 * 1024 * 1024)
                                report.appendLine("   ✓ System RAM: ${freeRam}GB free / ${totalRam}GB total")
                            }
                            if (vram != null) {
                                val totalVram = vram.optLong("total", 0) / (1024 * 1024 * 1024)
                                val freeVram = vram.optLong("free", 0) / (1024 * 1024 * 1024)
                                report.appendLine("   ✓ GPU VRAM: ${freeVram}GB free / ${totalVram}GB total")
                            }
                        }
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ System stats available but could not parse")
                    }
                }
            } else {
                report.appendLine("   ✗ ComfyUI service not reachable (HTTP ${response.code})")
                return@withContext ComfyUITestResult(false, report.toString())
            }
        } catch (e: Exception) {
            report.appendLine("   ✗ Connection failed: ${e.message}")
            return@withContext ComfyUITestResult(false, report.toString())
        }
        
        // Test 2: Fetch available models
        report.appendLine("\n2. Available Models Test:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/object_info")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        
                        // Get checkpoint models
                        val checkpointLoader = json.optJSONObject("CheckpointLoaderSimple")
                        if (checkpointLoader != null) {
                            val input = checkpointLoader.optJSONObject("input")
                            if (input != null) {
                                val required = input.optJSONObject("required")
                                if (required != null) {
                                    val ckptName = required.optJSONArray("ckpt_name")
                                    if (ckptName != null && ckptName.length() > 0) {
                                        val modelList = ckptName.optJSONArray(0)
                                        if (modelList != null) {
                                            for (i in 0 until modelList.length()) {
                                                val modelName = modelList.optString(i)
                                                if (modelName.isNotBlank()) {
                                                    models.add(modelName)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        report.appendLine("   ✓ Found ${models.size} available models:")
                        models.take(10).forEach { model ->
                            report.appendLine("     - $model")
                        }
                        if (models.size > 10) {
                            report.appendLine("     ... and ${models.size - 10} more")
                        }
                        
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Could not parse model list: ${e.message}")
                    }
                }
            } else {
                report.appendLine("   ⚠ Model info endpoint not available (HTTP ${response.code})")
            }
        } catch (e: Exception) {
            report.appendLine("   ⚠ Model fetch failed: ${e.message}")
        }
        
        // Test 3: Fetch available LoRAs
        report.appendLine("\n3. Available LoRAs Test:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/object_info")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        
                        // Get LoRA models
                        val loraLoader = json.optJSONObject("LoraLoader")
                        if (loraLoader != null) {
                            val input = loraLoader.optJSONObject("input")
                            if (input != null) {
                                val required = input.optJSONObject("required")
                                if (required != null) {
                                    val loraName = required.optJSONArray("lora_name")
                                    if (loraName != null && loraName.length() > 0) {
                                        val loraList = loraName.optJSONArray(0)
                                        if (loraList != null) {
                                            for (i in 0 until loraList.length()) {
                                                val loraFileName = loraList.optString(i)
                                                if (loraFileName.isNotBlank()) {
                                                    // Categorize LoRAs based on filename patterns
                                                    val lowerName = loraFileName.lowercase()
                                                    when {
                                                        lowerName.contains("flux") || lowerName.contains("dev") -> {
                                                            fluxLoras.add(loraFileName)
                                                        }
                                                        lowerName.contains("sdxl") || lowerName.contains("xl") -> {
                                                            sdxlLoras.add(loraFileName)
                                                        }
                                                        else -> {
                                                            // Default to SDXL for unknown types
                                                            sdxlLoras.add(loraFileName)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        report.appendLine("   ✓ Found ${sdxlLoras.size} SDXL LoRAs:")
                        sdxlLoras.take(5).forEach { lora ->
                            report.appendLine("     - $lora")
                        }
                        if (sdxlLoras.size > 5) {
                            report.appendLine("     ... and ${sdxlLoras.size - 5} more")
                        }
                        
                        report.appendLine("   ✓ Found ${fluxLoras.size} Flux LoRAs:")
                        fluxLoras.take(5).forEach { lora ->
                            report.appendLine("     - $lora")
                        }
                        if (fluxLoras.size > 5) {
                            report.appendLine("     ... and ${fluxLoras.size - 5} more")
                        }
                        
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Could not parse LoRA list: ${e.message}")
                    }
                }
            } else {
                report.appendLine("   ⚠ LoRA info endpoint not available (HTTP ${response.code})")
            }
        } catch (e: Exception) {
            report.appendLine("   ⚠ LoRA fetch failed: ${e.message}")
        }
        
        // Test 4: Queue status
        report.appendLine("\n4. Queue Status Test:")
        try {
            val request = Request.Builder()
                .url("$cleanUrl/queue")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val queueRunning = json.optJSONArray("queue_running")
                        val queuePending = json.optJSONArray("queue_pending")
                        
                        val runningCount = queueRunning?.length() ?: 0
                        val pendingCount = queuePending?.length() ?: 0
                        
                        report.appendLine("   ✓ Queue status: $runningCount running, $pendingCount pending")
                        
                        if (runningCount == 0 && pendingCount == 0) {
                            report.appendLine("   ✓ ComfyUI is ready to process requests")
                        } else {
                            report.appendLine("   ⚠ ComfyUI is currently processing requests")
                        }
                        
                    } catch (e: Exception) {
                        report.appendLine("   ⚠ Could not parse queue status: ${e.message}")
                    }
                }
            } else {
                report.appendLine("   ⚠ Queue endpoint not available (HTTP ${response.code})")
            }
        } catch (e: Exception) {
            report.appendLine("   ⚠ Queue check failed: ${e.message}")
        }
        
        // Test 5: Network diagnostics
        report.appendLine("\n5. Network Diagnostics:")
        try {
            val host = cleanUrl.substringAfter("://").substringBefore(":")
            val addresses = InetAddress.getAllByName(host)
            report.appendLine("   ✓ DNS resolution successful")
            report.appendLine("   IP addresses: ${addresses.joinToString(", ") { it.hostAddress ?: "unknown" }}")
            
            // Check if it's a local network address
            val isLocal = addresses.any { addr ->
                val ip = addr.hostAddress ?: ""
                ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.") || ip == "127.0.0.1"
            }
            report.appendLine("   Network type: ${if (isLocal) "Local network" else "Internet"}")
            
        } catch (e: Exception) {
            report.appendLine("   ⚠ DNS resolution failed: ${e.message}")
        }
        
        return@withContext ComfyUITestResult(
            success = success,
            report = report.toString(),
            availableModels = models,
            availableSDXLLoras = sdxlLoras,
            availableFluxLoras = fluxLoras
        )
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(value, units[unitIndex])
    }
} 
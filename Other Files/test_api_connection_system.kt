/**
 * Test script to verify API connection status and error reporting system
 * This tests the comprehensive error handling across all endpoints
 */

import kotlinx.coroutines.runBlocking
import com.vortexai.android.utils.ApiConnectionTester
import com.vortexai.android.utils.ApiConnectionResult

fun main() = runBlocking {
    println("🧪 Testing API Connection Status & Error Reporting System")
    println("=" * 60)
    
    val tester = ApiConnectionTester()
    
    // Test LLM Connections
    println("\n📡 Testing LLM Connections")
    println("-" * 40)
    
    val llmProviders = listOf(
        "Together AI" to "test-key",
        "OpenRouter" to "test-key", 
        "Gemini API" to "test-key",
        "Hugging Face" to "test-key",
        "ModelsLab" to "test-key",
        "Ollama" to "",
        "Kobold AI" to "",
        "Custom API" to "test-key"
    )
    
    llmProviders.forEach { (provider, apiKey) ->
        testLLMProvider(tester, provider, apiKey)
    }
    
    // Test Image Generation Connections
    println("\n🎨 Testing Image Generation Connections")
    println("-" * 40)
    
    val imageProviders = listOf(
        "Together AI" to "test-key",
        "Hugging Face" to "test-key",
        "ModelsLab" to "test-key",
        "ComfyUI" to "",
        "Custom API" to "test-key"
    )
    
    imageProviders.forEach { (provider, apiKey) ->
        testImageProvider(tester, provider, apiKey)
    }
    
    // Test Audio Connections
    println("\n🔊 Testing Audio/TTS Connections")
    println("-" * 40)
    
    val audioProviders = listOf(
        "ModelsLab" to "test-key",
        "Google TTS" to "",
        "Custom API" to "test-key"
    )
    
    audioProviders.forEach { (provider, apiKey) ->
        testAudioProvider(tester, provider, apiKey)
    }
    
    println("\n✅ API Connection System Tests Completed!")
    println("\n📊 Expected Error Types:")
    println("- ❌ Authentication failed (401/403)")
    println("- ❌ No API key configured")
    println("- ❌ No endpoint configured") 
    println("- ❌ Model not found (404)")
    println("- ❌ Rate limit exceeded (429)")
    println("- ❌ Server errors (500/502/503/504)")
    println("- ❌ Network/timeout errors")
    println("- ❌ Local service not running")
}

suspend fun testLLMProvider(tester: ApiConnectionTester, provider: String, apiKey: String) {
    try {
        println("🔍 Testing $provider LLM...")
        
        // Test different scenarios
        val scenarios = listOf(
            "Valid Key" to apiKey,
            "Empty Key" to "",
            "Invalid Key" to "invalid-key-123"
        )
        
        scenarios.forEach { (scenario, key) ->
            val result = tester.testLLMConnection(provider, key, "test-model", getEndpoint(provider))
            
            when (result) {
                is ApiConnectionResult.Success -> {
                    println("  ✅ $scenario: ${result.message}")
                }
                is ApiConnectionResult.Failure -> {
                    println("  ❌ $scenario: ${result.error.message}")
                    println("     Technical: ${result.error.technicalMessage}")
                }
            }
        }
        
    } catch (e: Exception) {
        println("  ⚠️  Test error: ${e.message}")
    }
    println()
}

suspend fun testImageProvider(tester: ApiConnectionTester, provider: String, apiKey: String) {
    try {
        println("🔍 Testing $provider Image...")
        
        val result = tester.testImageConnection(provider, apiKey, "test-model", getEndpoint(provider))
        
        when (result) {
            is ApiConnectionResult.Success -> {
                println("  ✅ Connection: ${result.message}")
            }
            is ApiConnectionResult.Failure -> {
                println("  ❌ Connection: ${result.error.message}")
                println("     Technical: ${result.error.technicalMessage}")
                println("     Retryable: ${result.error.isRetryable}")
            }
        }
        
    } catch (e: Exception) {
        println("  ⚠️  Test error: ${e.message}")
    }
    println()
}

suspend fun testAudioProvider(tester: ApiConnectionTester, provider: String, apiKey: String) {
    try {
        println("🔍 Testing $provider Audio...")
        
        val result = tester.testAudioConnection(provider, apiKey, getEndpoint(provider))
        
        when (result) {
            is ApiConnectionResult.Success -> {
                println("  ✅ Connection: ${result.message}")
            }
            is ApiConnectionResult.Failure -> {
                println("  ❌ Connection: ${result.error.message}")
                println("     Technical: ${result.error.technicalMessage}")
            }
        }
        
    } catch (e: Exception) {
        println("  ⚠️  Test error: ${e.message}")
    }
    println()
}

fun getEndpoint(provider: String): String? {
    return when (provider) {
        "Ollama" -> "http://localhost:11434"
        "Kobold AI" -> "http://localhost:5000"
        "ComfyUI" -> "http://localhost:8188"
        "Custom API" -> "http://localhost:8080"
        else -> null
    }
}

// Extension function for string repetition
operator fun String.times(n: Int): String = this.repeat(n)

/**
 * Expected Test Results:
 * 
 * LLM Providers:
 * - Together AI: ❌ Authentication failed (invalid key)
 * - OpenRouter: ❌ Authentication failed (invalid key)  
 * - Gemini API: ❌ Authentication failed (invalid key)
 * - Hugging Face: ❌ Authentication failed (invalid key)
 * - ModelsLab: ❌ Authentication failed (invalid key)
 * - Ollama: ❌ Local service not running
 * - Kobold AI: ❌ Local service not running
 * - Custom API: ❌ No endpoint configured
 * 
 * Image Providers:
 * - Together AI: ❌ No API key configured
 * - Hugging Face: ❌ Authentication failed
 * - ModelsLab: ❌ Authentication failed
 * - ComfyUI: ❌ Local service not running
 * - Custom API: ❌ No endpoint configured
 * 
 * Audio Providers:
 * - ModelsLab: ❌ Authentication failed
 * - Google TTS: ✅ Available through Android system
 * - Custom API: ❌ No endpoint configured
 */
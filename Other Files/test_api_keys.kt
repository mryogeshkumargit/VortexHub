import com.vortexai.android.domain.service.llm.TogetherProvider
import com.vortexai.android.domain.service.llm.OpenRouterProvider
import com.vortexai.android.domain.service.llm.GeminiProvider
import com.vortexai.android.domain.service.llm.GenerationParams
import kotlinx.coroutines.runBlocking

/**
 * Test script to verify the provided API keys work correctly
 */
fun main() {
    println("Testing provided API keys...")
    
    val togetherApiKey = ""
    val openRouterApiKey = ""
    
    runBlocking {
        // Test Together AI
        println("\n=== Testing Together AI ===")
        try {
            val togetherProvider = TogetherProvider().apply { setApiKey(togetherApiKey) }
            val response = togetherProvider.generateResponse(
                "Hello, this is a test. Please respond with 'Together AI working!'",
                GenerationParams(maxTokens = 50)
            )
            println("✅ Together AI Success: $response")
        } catch (e: Exception) {
            println("❌ Together AI Error: ${e.message}")
        }
        
        // Test OpenRouter
        println("\n=== Testing OpenRouter ===")
        try {
            val openRouterProvider = OpenRouterProvider().apply { setApiKey(openRouterApiKey) }
            val response = openRouterProvider.generateResponse(
                "Hello, this is a test. Please respond with 'OpenRouter working!'",
                GenerationParams(maxTokens = 50)
            )
            println("✅ OpenRouter Success: $response")
        } catch (e: Exception) {
            println("❌ OpenRouter Error: ${e.message}")
        }
        
        // Test Gemini (would need API key)
        println("\n=== Testing Gemini ===")
        println("ℹ️  Gemini test skipped - no API key provided")
    }
    
    println("\nAPI key testing completed!")
}

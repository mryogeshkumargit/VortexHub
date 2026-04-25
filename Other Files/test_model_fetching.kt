import com.vortexai.android.domain.service.ChatLLMService
import kotlinx.coroutines.runBlocking

/**
 * Simple test script to verify model fetching functionality
 */
fun main() {
    println("Testing model fetching functionality...")
    
    // Test with empty API key (should return error)
    runBlocking {
        val chatLLMService = ChatLLMService(null, null) // This won't work in real app, just for testing
        
        // Test Together AI
        println("\n=== Testing Together AI ===")
        val togetherResult = chatLLMService.fetchModels("Together AI", "")
        togetherResult.fold(
            onSuccess = { models ->
                println("Success: Found ${models.size} models")
                models.take(3).forEach { println("- ${it.name} (${it.id})") }
            },
            onFailure = { error ->
                println("Expected error: ${error.message}")
            }
        )
        
        // Test with dummy API key (should return default models)
        println("\n=== Testing with dummy API key ===")
        val dummyResult = chatLLMService.fetchModels("Together AI", "dummy_key_123")
        dummyResult.fold(
            onSuccess = { models ->
                println("Success: Found ${models.size} models (defaults)")
                models.take(3).forEach { println("- ${it.name} (${it.id})") }
            },
            onFailure = { error ->
                println("Error: ${error.message}")
            }
        )
        
        // Test ModelsLab
        println("\n=== Testing ModelsLab ===")
        val modelsLabResult = chatLLMService.fetchModels("ModelsLab", "")
        modelsLabResult.fold(
            onSuccess = { models ->
                println("Success: Found ${models.size} models")
                models.forEach { println("- ${it.name} (${it.id})") }
            },
            onFailure = { error ->
                println("Expected error: ${error.message}")
            }
        )
    }
    
    println("\nModel fetching test completed!")
}
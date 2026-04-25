import com.vortexai.android.domain.service.ImageGenerationRequest
import com.vortexai.android.domain.service.ModelsLabImageApi
import kotlinx.coroutines.runBlocking
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple test script to verify the ModelsLab Flux API implementation
 * Run with: kotlinc -cp <path-to-dependencies> test_flux_api.kt -run
 */
fun main() {
    val logger = Logger.getLogger("FluxApiTest")
    logger.info("Starting Flux API test")
    
    // Replace with your actual API key
    val apiKey = "YOUR_API_KEY_HERE"
    
    val modelsLabApi = ModelsLabImageApi()
    
    val request = ImageGenerationRequest(
        prompt = "ultra realistic close up portrait ((beautiful pale cyberpunk female with heavy black eyeliner)), blue eyes, shaved side haircut, hyper detail, cinematic lighting, magic neon, dark red city, Canon EOS R3, nikon, f/1.4, ISO 200, 1/160s, 8K, RAW, unedited, symmetrical balance, in-frame, 8K",
        model = "flux", // Will be set to "flux" automatically for Flux workflow
        width = 512,
        height = 512,
        steps = 31, // num_inference_steps from settings
        guidanceScale = 7.5f, // Default value (not configurable for Flux)
        samples = 1, // Always 1 for Flux
        workflow = "flux", // Flux workflow
        negativePrompt = null, // Not used in Flux configuration
        seed = null, // Optional
        // LoRA settings are ignored for Flux workflow
        loraModel = null,
        loraStrength = null
    )
    
    logger.info("Sending Flux API request with exact configuration...")
    logger.info("Using endpoint: https://modelslab.com/api/v6/images/text2img")
    logger.info("Model ID: flux (hardcoded)")
    logger.info("Only accepting: user prompt, image size (${request.width}x${request.height}), and steps (${request.steps})")
    
    runBlocking {
        val result = modelsLabApi.fluxText2Img(apiKey, request)
        
        result.fold(
            onSuccess = { imageUrl ->
                logger.info("Success! Image URL: $imageUrl")
            },
            onFailure = { error ->
                logger.log(Level.SEVERE, "Error generating image", error)
                logger.severe("Error message: ${error.message}")
            }
        )
    }
    
    logger.info("Test completed")
} 
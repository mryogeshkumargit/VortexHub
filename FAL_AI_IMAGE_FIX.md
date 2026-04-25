# FAL AI Image Generation Fix

## Issue
Getting "404 - not found" error when using FAL AI for image generation via Custom API.

## Root Cause
The URL construction for FAL AI was not properly handling the combination of base URL and model name. FAL AI requires the full endpoint to be: `base_url/model_name`

For example:
- Base URL: `https://fal.run`
- Model: `fal-ai/flux-schnell`
- Expected: `https://fal.run/fal-ai/flux-schnell`

## Fix Applied
Modified `CustomImageProvider.kt` to properly construct FAL AI URLs:

1. **Check if endpoint already contains the model** - If the user has already configured the full URL, use it as-is
2. **Otherwise, combine base URL + model** - Properly trim slashes and combine the parts

### Code Changes
File: `app/src/main/java/com/vortexai/android/domain/service/image/CustomImageProvider.kt`

```kotlin
// Build full fal.ai URL properly
// If endpoint already contains the model, use it as-is
// Otherwise, append the model to the base URL
val fullUrl = if (endpoint.contains(model)) {
    endpoint
} else {
    val baseUrl = endpoint.trimEnd('/')
    val modelPath = model.trimStart('/')
    "$baseUrl/$modelPath"
}
```

## How to Configure FAL AI

### Option 1: Separate Base URL and Model (Recommended)
1. Go to Settings → Image Generation → Configure Custom Image Generation APIs
2. Add a new provider:
   - **Name**: FAL AI
   - **Type**: Image Generation
   - **Base URL**: `https://fal.run`
   - **API Key**: Your FAL AI key (format: `uuid:secret`)
3. Add models:
   - `fal-ai/flux-schnell`
   - `fal-ai/flux-dev`
   - `fal-ai/flux-pro/v1.1`
   - etc.

### Option 2: Full URL in Base URL
1. **Base URL**: `https://fal.run/fal-ai/flux-schnell`
2. **Model**: Leave empty or use same value

## FAL AI Request Structure
The fix ensures the request is sent to FAL AI with the correct format:

```json
{
  "prompt": "your prompt here",
  "image_size": {
    "width": 1024,
    "height": 1024
  },
  "num_images": 1,
  "enable_safety_checker": false
}
```

With headers:
```
Authorization: Key {uuid:secret}
Content-Type: application/json
```

## Testing
After applying this fix:
1. Configure FAL AI as a custom provider
2. Select it in Image Generation settings
3. Try generating an image
4. Check logs for proper URL construction: `fal.ai endpoint: ..., model: ..., fullUrl: ...`

## Additional Notes
- FAL AI uses `Key` authorization header instead of `Bearer`
- The API key format is `uuid:secret` (e.g., `1286244d-b070-4ef8-8d3c-3e81c397880b:2ea952307c020ff8bbb73d82f38a22c2`)
- Response format is different from OpenAI - uses `images` array instead of `data` array

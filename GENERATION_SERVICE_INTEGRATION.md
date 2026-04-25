# Generation Service Integration Guide

## Service Created
- `GenerationService.kt` - Foreground service for tracking ongoing generations

## Integration Points

### 1. In ChatViewModel - Add Injection

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // ... existing dependencies
    private val generationServiceHelper: GenerationServiceHelper,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {
```

### 2. In generateCharacterResponse() - Start Service

Add after line `_uiState.update { it.copy(isCharacterTyping = true, errorMessage = null) }`:

```kotlin
// Start generation service
val generationId = "ai_${System.currentTimeMillis()}"
generationServiceHelper.startAIGeneration(context, generationId, character?.name ?: "AI")
```

Add in success block:

```kotlin
// Complete generation
generationServiceHelper.completeGeneration(context, generationId, "ai", message.content)
```

### 3. In generateImage() - Start Service

Add after `_uiState.update { state -> state.copy(isGeneratingImage = true) }`:

```kotlin
// Start image generation service
val generationId = "img_${System.currentTimeMillis()}"
generationServiceHelper.startImageGeneration(context, generationId, prompt)
```

Add in success block:

```kotlin
// Complete generation
generationServiceHelper.completeGeneration(context, generationId, "image", prompt)
```

### 4. In generateVortexImage() - Start Service

Add after `_uiState.update { it.copy(isGeneratingImage = true) }`:

```kotlin
// Start vortex image generation service
val generationId = "vortex_${System.currentTimeMillis()}"
generationServiceHelper.startImageGeneration(context, generationId, "Vortex Mode: ${aiResponse.take(50)}")
```

Add in success block:

```kotlin
// Complete generation
generationServiceHelper.completeGeneration(context, generationId, "image", "Vortex Mode image")
```

## How It Works

1. **User sends message** → Service starts with "Generating response from [Character]..."
2. **App goes to background** → Service keeps running (foreground notification visible)
3. **Response completes** → Notification shows "New message from [Character]"
4. **Service stops** → No more active generations

## Permissions Added
- `FOREGROUND_SERVICE` - Required for foreground service
- `FOREGROUND_SERVICE_DATA_SYNC` - Specifies service type

## Manifest Changes
- Service registered with `foregroundServiceType="dataSync"`

## Battery Impact
- Service only runs during active generation
- Automatically stops when generation completes
- No persistent background service

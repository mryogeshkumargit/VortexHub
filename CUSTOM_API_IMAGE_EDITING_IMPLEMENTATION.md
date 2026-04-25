# Custom API Image Editing Implementation

## Overview
Implemented custom API support for image editing in chat, allowing users to use database-backed custom API providers for editing character avatar images with AI prompts.

## Changes Made

### 1. ChatImageGenerator.kt
**File**: `d:\AI\VortexAndroid\app\src\main\java\com\vortexai\android\ui\screens\chat\ChatImageGenerator.kt`

**Changes**:
- Added `CustomApiProviderRepository` and `CustomApiExecutor` as constructor dependencies
- Added import for `CustomApiExecutor`
- Implemented "custom api" case in `generateImageWithEditing()` function
- Loads selected custom provider from DataStore preferences
- Validates provider exists and is enabled
- Finds IMAGE_EDITING endpoint and active model
- Loads saved parameter values from database
- Merges saved parameters with request parameters (prompt, image)
- Executes custom API request using `CustomApiExecutor`
- Parses response to extract image URL
- Downloads and saves generated image locally
- Creates message response with edited image

**Key Features**:
- Full integration with database-backed custom API system
- Parameter persistence support
- Error handling for missing/disabled providers
- Automatic parameter merging

### 2. SettingsViewModel.kt
**File**: `d:\AI\VortexAndroid\app\src\main\java\com\vortexai\android\ui\screens\settings\SettingsViewModel.kt`

**Changes**:
- Added `selectedCustomImageEditProviderId` to `loadSettings()` function
- Added `selectedCustomImageEditProviderId` to `saveImageEditingSettings()` function
- Ensures custom image editing provider selection persists across app restarts

## How It Works

### User Flow
1. User configures custom API provider with IMAGE_EDITING type in "Configure Custom APIs" screen
2. User goes to Settings → Image Editing
3. User selects "Custom API" as image editing provider
4. User selects specific custom provider from dropdown
5. User saves settings
6. In chat, user types `/image <prompt>` command
7. System uses character avatar as source image
8. System loads custom API configuration and parameters
9. System sends request to custom API
10. System displays edited image in chat

### Technical Flow
```
Chat Command (/image)
    ↓
ChatImageGenerator.generateImageWithChatSettings()
    ↓
generateImageWithEditing()
    ↓
Load imageEditingProvider from DataStore
    ↓
If "custom api":
    ↓
Load selectedCustomImageEditProviderId
    ↓
Get provider from CustomApiProviderRepository
    ↓
Validate provider is enabled
    ↓
Get IMAGE_EDITING endpoint
    ↓
Get active model
    ↓
Load saved parameter values
    ↓
Merge with request params (prompt, image)
    ↓
CustomApiExecutor.executeRequest()
    ↓
CustomApiExecutor.parseResponse()
    ↓
Download and save image
    ↓
Display in chat
```

## Database Schema Used

### Tables
- `custom_api_providers` - Provider configuration
- `custom_api_endpoints` - Endpoint definitions (purpose = "image_edit")
- `custom_api_models` - Model configurations
- `custom_api_parameters` - Parameter definitions
- `custom_api_parameter_values` - User-configured parameter values

### Key Fields
- Provider: `id`, `name`, `baseUrl`, `apiKey`, `isEnabled`, `type` (IMAGE_EDITING)
- Endpoint: `purpose` ("image_edit"), `requestSchemaJson`, `responseSchemaJson`
- Model: `modelId`, `isActive`
- Parameter Values: `modelId`, `paramName`, `value` (composite primary key)

## Configuration Example

### Fal AI Image Editing JSON
```json
{
  "provider": {
    "name": "Fal AI - Nano Banana Pro Edit",
    "baseUrl": "https://fal.run",
    "apiKey": "YOUR_API_KEY",
    "type": "IMAGE_EDITING"
  },
  "endpoints": [
    {
      "name": "Image Edit",
      "path": "/fal-ai/nano-banana-pro-edit",
      "method": "POST",
      "purpose": "image_edit",
      "requestSchema": {
        "headers": {
          "Authorization": "Key {apiKey}",
          "Content-Type": "application/json"
        },
        "body": {
          "prompt": "{prompt}",
          "image_url": "{image}"
        }
      },
      "responseSchema": {
        "dataPath": "images[0].url",
        "errorPath": "error.message"
      }
    }
  ],
  "models": [
    {
      "modelId": "nano-banana-pro-edit",
      "modelName": "Nano Banana Pro Edit",
      "isActive": true,
      "parameters": []
    }
  ]
}
```

## Testing

### Prerequisites
1. Import custom API configuration for IMAGE_EDITING type
2. Configure API key in provider settings
3. Select "Custom API" in Settings → Image Editing
4. Select specific provider from dropdown
5. Save settings

### Test Steps
1. Open chat with character that has avatar image
2. Type `/image make the background blue`
3. Verify image is edited and displayed in chat
4. Check that parameters are loaded from database
5. Verify error handling for missing/disabled providers

## Error Handling

### Errors Handled
- No custom provider selected
- Provider not found in database
- Provider is disabled
- No IMAGE_EDITING endpoint found
- No active model found
- API request failure
- Response parsing failure
- Image download failure

### Error Messages
- "No custom image editing provider selected. Please select one in Settings → Image Editing."
- "Custom image editing provider not found"
- "Custom provider '{name}' is disabled"
- "No image editing endpoint found for provider '{name}'"
- "No active model found for provider '{name}'"

## Limitations

### Current Limitations
1. Only supports single active model per provider
2. Image must be provided as base64 or URL in request
3. Response must contain image URL (not base64 response)
4. No streaming support
5. No progress indication during API call

### Future Enhancements
1. Support multiple models per provider
2. Support base64 image responses
3. Add progress indicator
4. Add retry mechanism
5. Add request timeout configuration
6. Support batch image editing

## Integration Status

### ✅ Implemented
- Custom API provider selection in settings
- Database-backed configuration
- Parameter persistence
- Request execution
- Response parsing
- Image download and storage
- Chat integration
- Error handling

### ❌ Not Implemented
- Image generation (only editing)
- Streaming responses
- Progress indication
- Batch processing
- Custom timeout configuration

## Related Files

### Core Implementation
- `ChatImageGenerator.kt` - Main integration point
- `CustomApiExecutor.kt` - Request execution
- `CustomApiProviderRepository.kt` - Database access
- `SettingsViewModel.kt` - Settings persistence

### UI Components
- `ImageEditingTab.kt` - Settings UI
- `CustomApiProviderScreen.kt` - Provider management
- `CustomApiDialogs.kt` - Configuration dialogs

### Database
- `CustomApiProvider.kt` - Provider entity
- `CustomApiEndpoint.kt` - Endpoint entity
- `CustomApiModel.kt` - Model entity
- `CustomApiParameter.kt` - Parameter entity
- `CustomApiParameterValue.kt` - Parameter value entity
- `CustomApiProviderDao.kt` - Database operations

## Build Status
✅ **Build Successful** - All changes compile correctly

## Commit Message
```
feat: implement custom API support for image editing in chat

- Add CustomApiExecutor integration to ChatImageGenerator
- Load custom provider configuration from database
- Support parameter persistence and merging
- Add error handling for missing/disabled providers
- Save selectedCustomImageEditProviderId in settings
- Enable image editing with custom APIs in chat flow
```

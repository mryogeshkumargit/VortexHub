# Vortex Mode Implementation

## Overview
Vortex Mode is a new feature that automatically generates contextual images based on AI character responses during chat conversations. When enabled, the system analyzes each AI response and creates relevant images that enhance the conversation experience.

## Implementation Details

### 1. Core Components Added

#### VortexImageGenerator.kt
- Analyzes AI responses to extract visual elements
- Creates image prompts based on character descriptions and response content
- Generates images using the configured image generation provider
- Handles errors gracefully without disrupting chat flow

#### Settings Integration
- Added `VORTEX_MODE_ENABLED_KEY` to SettingsDataStore
- Integrated with existing DataStore preferences system
- Persists user preference across app sessions

#### ChatViewModel Updates
- Added `vortexModeEnabled` StateFlow for reactive UI updates
- Added `setVortexModeEnabled()` function for toggling the feature
- Integrated Vortex image generation into character response flow
- Added error handling with popup notifications instead of chat disruption

#### UI Components
- Added Vortex Mode checkbox to ChatOptionsMenu
- Added error popup dialog for image generation failures
- Uses AutoAwesome icons to represent the feature

### 2. Key Features

#### Intelligent Prompt Generation
- Extracts visual traits from character descriptions (hair, eyes, clothing, colors)
- Identifies actions and emotions from AI responses (smiling, walking, etc.)
- Detects scene/location context (room, garden, cafe, etc.)
- Combines elements into coherent image prompts

#### Error Handling
- Shows error popups instead of adding errors to conversation
- Auto-dismisses errors after 5 seconds
- Logs detailed error information for debugging
- Graceful fallback when image generation fails

#### Provider Integration
- Works with existing image generation providers (Together AI, ModelsLab, Replicate)
- Uses current user settings for image size, model, and quality
- Respects API key configurations

### 3. User Experience

#### Enabling Vortex Mode
1. Open chat options menu (three dots in chat header)
2. Toggle "Enable Vortex Mode" checkbox
3. Setting is automatically saved and persists across sessions

#### When Active
- AI responses trigger automatic image generation
- Images appear as separate messages in the chat
- Labeled as "Vortex Mode: Generated image based on AI response"
- Generation happens asynchronously without blocking chat

#### Error Scenarios
- Missing API keys: Shows configuration error popup
- Network issues: Shows connection error popup
- Invalid prompts: Shows generation error popup
- All errors are non-blocking and dismissible

### 4. Technical Architecture

#### Dependency Injection
- VortexImageGenerator added to RepositoryModule
- Injected into ChatViewModel constructor
- Singleton pattern ensures efficient resource usage

#### State Management
- Reactive UI updates using StateFlow
- Error state managed separately from chat messages
- Clean separation of concerns

#### Performance Considerations
- Image generation runs asynchronously
- Does not block chat functionality
- Efficient prompt generation algorithms
- Proper error boundaries

### 5. Configuration Requirements

#### API Keys
Users need to configure image generation API keys in Settings:
- Together AI: Settings → Image Generation → Together AI API Key
- ModelsLab: Settings → Image Generation → ModelsLab API Key  
- Replicate: Settings → Image Generation → Replicate API Key

#### Image Settings
Vortex Mode respects existing image generation settings:
- Image size (1024x1024, 512x512, etc.)
- Generation steps and guidance scale
- Model selection per provider

### 6. Future Enhancements

#### Potential Improvements
- More sophisticated prompt analysis using NLP
- Character-specific image styles and preferences
- User customizable prompt templates
- Image caching and optimization
- Batch generation for multiple responses

#### Integration Opportunities
- Character avatar integration for consistent appearance
- Lorebook integration for world-building context
- Dynamic stats integration for character state visualization
- Custom image editing based on character emotions

## Usage Instructions

1. **Enable Vortex Mode**: Open chat options menu and toggle "Enable Vortex Mode"
2. **Configure API Keys**: Ensure image generation provider API keys are set in Settings
3. **Start Chatting**: AI responses will automatically trigger contextual image generation
4. **Handle Errors**: If image generation fails, dismiss error popups and check API configuration

## Error Troubleshooting

- **"No API key configured"**: Go to Settings → Image Generation and add API key
- **"Could not generate image prompt"**: AI response may lack visual context
- **Network errors**: Check internet connection and API service status
- **Generation timeouts**: Try different image generation provider or smaller image sizes
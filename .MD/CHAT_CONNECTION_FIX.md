# Chat Connection & Model Selection Fix

## Issues Fixed

### 1. **"Couldn't connect to AI endpoint" during chat** ✅
**Problem**: API keys entered in settings weren't being saved to DataStore, so chat generation couldn't access them.

**Root Cause**: 
- SettingsViewModel only stored API keys in UI state
- ChatLLMService tried to read API keys from DataStore preferences
- The keys were never actually saved to persistent storage

**Solution**:
- Added DataStore dependency to SettingsViewModel
- Implemented proper `saveLLMSettings()` method that saves to DataStore
- Added `loadSettings()` method to load saved settings on app start
- Added auto-save when API keys are updated

### 2. **ModelsLab model selection missing** ✅
**Problem**: The settings UI explicitly hid the model selection dropdown for ModelsLab.

**Root Cause**: 
```kotlin
// This condition excluded ModelsLab from showing models
if (uiState.llmProvider != "ModelsLab" && uiState.availableModels.isNotEmpty())
```

**Solution**:
- Removed the ModelsLab exclusion condition
- Now all providers show model selection when models are available
- ModelsLab users can now select from fetched models

## Code Changes

### SettingsViewModel.kt
```kotlin
// Added DataStore dependency
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val chatLLMService: ChatLLMService,
    private val dataStore: DataStore<Preferences>
) : ViewModel()

// Added settings loading on init
init {
    loadSettings()
}

// Implemented proper DataStore saving
fun saveLLMSettings() {
    viewModelScope.launch {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("together_ai_api_key")] = currentState.togetherAiApiKey
            preferences[stringPreferencesKey("modelslab_api_key")] = currentState.modelsLabApiKey
            // ... all other API keys and settings
        }
    }
}

// Added auto-save to API key update methods
fun updateTogetherAiApiKey(key: String) { 
    _uiState.value = _uiState.value.copy(togetherAiApiKey = key)
    saveLLMSettings() // Auto-save
}
```

### SettingsScreen.kt
```kotlin
// Removed ModelsLab exclusion from model selection
// Before:
if (uiState.llmProvider != "ModelsLab" && uiState.availableModels.isNotEmpty())

// After:
if (uiState.availableModels.isNotEmpty())
```

## How to Test

### 1. **Test Chat Connection**
1. Enter API key for any provider (Together AI, ModelsLab, etc.)
2. The key should auto-save immediately
3. Start a chat conversation
4. Should now connect properly instead of showing "couldn't connect to ai endpoint"

### 2. **Test ModelsLab Model Selection**
1. Go to Settings → LLM
2. Select "ModelsLab" as provider
3. Enter your ModelsLab API key
4. Click "Fetch Available Models"
5. Should now see a dropdown with available models
6. Select a model from the dropdown
7. Model selection should be saved

### 3. **Test Settings Persistence**
1. Enter API keys and select models
2. Close and reopen the app
3. Settings should be preserved
4. Chat should work with saved settings

## Expected Behavior

### API Key Flow:
1. **Enter API Key** → Auto-saved to DataStore
2. **Fetch Models** → Uses saved API key
3. **Start Chat** → Uses saved API key from DataStore
4. **Chat Generation** → Connects successfully

### Model Selection Flow:
1. **Select Provider** → Shows appropriate UI
2. **Fetch Models** → Populates dropdown for ALL providers
3. **Select Model** → Saves selection to DataStore
4. **Chat Generation** → Uses selected model

## Troubleshooting

If chat still shows connection errors:

1. **Check API Key Format**:
   - Together AI: Long alphanumeric string
   - ModelsLab: Alphanumeric string
   - OpenRouter: Starts with `sk-or-`

2. **Verify Settings Saved**:
   - Close and reopen app
   - Check if API keys are still there
   - If not, there might be a DataStore issue

3. **Check Network**:
   - Ensure internet connection
   - Try different provider to isolate issue

4. **Check Logs**:
   - Look for specific error messages
   - Check for HTTP status codes (401, 403, etc.)

The chat connection and model selection should now work properly for all providers including ModelsLab! 🚀
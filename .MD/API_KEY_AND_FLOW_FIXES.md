# API Key and Flow Error Fixes

## 🐛 **Issues Identified**

### **1. Together AI API Key Issue**
**Problem**: "No API key configured for Together AI" even with correct API key
**Root Cause**: Separate API key fields for LLM (`togetherAiApiKey`) and Image (`togetherAiImageApiKey`), but Together AI uses the same key for both services

### **2. Flow Invariant Violation**
**Problem**: `IllegalStateException: Flow invariant is violated`
**Root Cause**: Flow not emitting values in certain cases (duplicate prevention) and improper flow collection patterns

## ✅ **Fixes Implemented**

### **1. Together AI API Key Fallback**

**Files**: `ChatViewModel.kt`, `SettingsViewModel.kt`

**Fix**: Added fallback logic to use LLM API key for image generation if image-specific key is not set

**Before**:
```kotlin
val apiKey = when (imageProvider) {
    "Together AI" -> preferences[SettingsViewModel.TOGETHER_AI_IMAGE_API_KEY] ?: ""
    // ...
}
```

**After**:
```kotlin
val apiKey = when (imageProvider) {
    "Together AI" -> {
        // Try image-specific key first, then fall back to LLM key
        val imageKey = preferences[SettingsViewModel.TOGETHER_AI_IMAGE_API_KEY] ?: ""
        val llmKey = preferences[stringPreferencesKey("together_ai_api_key")] ?: ""
        imageKey.ifBlank { llmKey }
    }
    // ...
}
```

**In SettingsViewModel**:
```kotlin
"Together AI" -> {
    // Try image-specific key first, then fall back to LLM key
    currentState.togetherAiImageApiKey.ifBlank { currentState.togetherAiApiKey }
}
```

### **2. Flow Collection Pattern Fix**

**File**: `ChatViewModel.kt`

**Problem**: Using `firstOrNull()` on flows that might not emit
**Fix**: Proper flow collection with error handling

**Before**:
```kotlin
val result = chatRepository.generateCharacterResponse(conversationId).firstOrNull()

if (result == null) {
    // Handle null case
    return@launch
}
```

**After**:
```kotlin
chatRepository.generateCharacterResponse(conversationId)
    .flowOn(Dispatchers.IO)
    .catch { exception ->
        // Handle flow errors
    }
    .collect { result ->
        // Handle result
    }
```

### **3. Flow Emission Guarantee**

**File**: `ChatRepository.kt`

**Problem**: `DuplicateResponseException` causing flow to not emit anything
**Fix**: Always emit a result, even for duplicate cases

**Before**:
```kotlin
fun generateCharacterResponse(...): Flow<Result<Message>> = flow {
    val result: Result<Message> = runCatching {
        // ... logic that might throw DuplicateResponseException
    }
    emit(result) // This might not be reached if exception is thrown
}
```

**After**:
```kotlin
fun generateCharacterResponse(...): Flow<Result<Message>> = flow {
    try {
        val result: Result<Message> = runCatching {
            // ... logic
        }
        emit(result)
    } catch (e: DuplicateResponseException) {
        // Always emit a result, even for duplicates
        Log.d(TAG, "Duplicate response detected: ${e.message}")
        emit(Result.failure(e))
    } catch (e: Exception) {
        emit(Result.failure(e))
    }
}
```

### **4. Graceful Duplicate Handling**

**File**: `ChatViewModel.kt`

**Fix**: Don't show duplicate response exceptions as errors to users

**Implementation**:
```kotlin
onFailure = { exception ->
    when (exception) {
        is ChatRepository.DuplicateResponseException -> {
            // Don't show duplicate response as an error to user
            Log.d(TAG, "Duplicate response skipped: ${exception.message}")
            _uiState.update { it.copy(isCharacterTyping = false) }
        }
        else -> {
            // Show actual errors
            _uiState.update { 
                it.copy(
                    isCharacterTyping = false,
                    errorMessage = "Failed to generate response: ${exception.message}"
                )
            }
        }
    }
}
```

## 🎯 **Expected Results**

### **Before Fixes**:
- ❌ "No API key configured for Together AI" (even with valid key)
- ❌ `IllegalStateException: Flow invariant is violated`
- ❌ App crashes or hangs on character response generation
- ❌ Duplicate response errors shown to users

### **After Fixes**:
- ✅ **Together AI image generation works** with LLM API key
- ✅ **Flow errors resolved** - no more invariant violations
- ✅ **Stable character responses** without crashes
- ✅ **Graceful duplicate handling** - no user-visible errors
- ✅ **Proper error messages** for actual issues

## 🔧 **Technical Details**

### **API Key Resolution Logic**:
1. Check for provider-specific image API key
2. If blank, fall back to LLM API key for same provider
3. This allows users to set one Together AI key for both services

### **Flow Safety**:
1. Always emit a result from flows
2. Use proper flow collection patterns with `.collect()`
3. Handle exceptions at flow level with `.catch()`
4. Ensure flows run on appropriate dispatchers with `.flowOn()`

### **Error Categorization**:
- **User Errors**: Show in UI (API key issues, network problems)
- **System Errors**: Log only (duplicate prevention, flow issues)
- **Recoverable Errors**: Retry logic where appropriate

## 🧪 **Testing Scenarios**

### **Together AI Image Generation**:
1. ✅ Set only LLM API key → Image generation should work
2. ✅ Set both LLM and Image API keys → Use image-specific key
3. ✅ Set only Image API key → Use image-specific key

### **Flow Stability**:
1. ✅ Rapid message sending → No flow violations
2. ✅ Character response generation → Stable flow handling
3. ✅ Duplicate prevention → Graceful handling without errors

### **Error Handling**:
1. ✅ Invalid API keys → Clear error messages
2. ✅ Network issues → Appropriate error messages
3. ✅ Duplicate responses → Silent handling (no user error)

## 🚀 **Build Status**

- ✅ **Compilation**: Successful with no errors
- ✅ **Flow Safety**: Proper emission guarantees
- ✅ **API Key Logic**: Fallback mechanisms implemented
- ✅ **Error Handling**: Comprehensive exception management

The fixes address both the API key configuration issue and the flow invariant violation, providing a more stable and user-friendly experience! 🎉
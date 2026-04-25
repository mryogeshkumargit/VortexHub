# Provider Endpoint Fix - Together AI, OpenRouter, Gemini

## Issues Fixed

### **Root Cause** 🐛
The chat generation was failing because the LLM providers were incorrectly mapped:

1. **OpenRouter** → Was using `OpenAIProvider` with OpenAI's URL (`https://api.openai.com/v1/`)
2. **Gemini** → Was using `AnthropicProvider` instead of proper Gemini API
3. **Missing proper provider implementations** for OpenRouter and Gemini

## **Solutions Implemented** ✅

### 1. **Created OpenRouterProvider**
- **File**: `app/src/main/java/com/vortexai/android/domain/service/llm/OpenRouterProvider.kt`
- **Base URL**: `https://openrouter.ai/api/v1/`
- **Features**:
  - Proper OpenRouter API endpoint
  - Required headers (`HTTP-Referer`, `X-Title`)
  - OpenAI-compatible request format
  - Default model: `openai/gpt-4o-mini`

### 2. **Created GeminiProvider**
- **File**: `app/src/main/java/com/vortexai/android/domain/service/llm/GeminiProvider.kt`
- **Base URL**: `https://generativelanguage.googleapis.com/v1beta/`
- **Features**:
  - Proper Gemini API format (different from OpenAI)
  - Uses `generateContent` endpoint
  - API key in URL parameter
  - Default model: `gemini-1.5-flash`

### 3. **Updated ChatLLMService Provider Mapping**
```kotlin
// Before (WRONG):
"Open Router" -> OpenAIProvider().apply { setApiKey(apiKey) }  // Used OpenAI URL!
"Gemini API" -> AnthropicProvider().apply { setApiKey(apiKey) } // Wrong provider!

// After (CORRECT):
"Open Router" -> OpenRouterProvider().apply { setApiKey(apiKey) }
"Gemini API" -> GeminiProvider().apply { setApiKey(apiKey) }
```

## **API Endpoint Details**

### **Together AI** ✅
- **URL**: `https://api.together.xyz/v1/chat/completions`
- **Format**: OpenAI-compatible
- **Headers**: `Authorization: Bearer {API_KEY}`

### **OpenRouter** ✅
- **URL**: `https://openrouter.ai/api/v1/chat/completions`
- **Format**: OpenAI-compatible
- **Headers**: 
  - `Authorization: Bearer {API_KEY}`
  - `HTTP-Referer: https://vortexai.app`
  - `X-Title: VortexAI Android`

### **Gemini** ✅
- **URL**: `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={API_KEY}`
- **Format**: Google's custom format
- **Headers**: `Content-Type: application/json`

## **Testing Your API Keys**

### **Together AI Key**: `31980f30c8041ce661665b782482f885d89a2296abf856111bafee8507c64d5c`
```bash
curl -X POST "https://api.together.xyz/v1/chat/completions" \
  -H "Authorization: Bearer 31980f30c8041ce661665b782482f885d89a2296abf856111bafee8507c64d5c" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3.2-3B-Instruct-Turbo",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 50
  }'
```

### **OpenRouter Key**: `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`
```bash
curl -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer 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" \
  -H "Content-Type: application/json" \
  -H "HTTP-Referer: https://vortexai.app" \
  -d '{
    "model": "openai/gpt-4o-mini",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 50
  }'
```

## **How to Test in App**

### 1. **Enter API Keys**
1. Open Settings → LLM
2. Select "Together AI" → Enter your Together AI key
3. Select "Open Router" → Enter your OpenRouter key
4. Select "Gemini API" → Enter your Gemini key (if you have one)

### 2. **Test Chat Generation**
1. After entering keys, start a chat
2. Should now connect properly instead of "couldn't connect to ai endpoint"
3. Each provider should use its correct API endpoint

### 3. **Test Model Fetching**
1. After entering API key, click "Fetch Available Models"
2. Should populate with real models from each provider
3. Select a model and test chat generation

## **Expected Results**

### **Together AI**:
- ✅ Should connect to `api.together.xyz`
- ✅ Should fetch models like Llama 3.2, Mixtral
- ✅ Should generate responses successfully

### **OpenRouter**:
- ✅ Should connect to `openrouter.ai`
- ✅ Should fetch models like GPT-4o, Claude 3.5
- ✅ Should generate responses successfully

### **Gemini**:
- ✅ Should connect to `generativelanguage.googleapis.com`
- ✅ Should use Gemini 1.5 Flash model
- ✅ Should generate responses successfully

## **Troubleshooting**

If you still get "couldn't connect to ai endpoint":

### 1. **Check API Key Format**
- **Together AI**: Long alphanumeric (your key looks correct)
- **OpenRouter**: Must start with `sk-or-` (your key looks correct)
- **Gemini**: Alphanumeric string from Google AI Studio

### 2. **Check Network**
- Ensure internet connection
- Try different provider to isolate issue
- Check if corporate firewall blocks API endpoints

### 3. **Check Logs**
- Look for specific HTTP error codes:
  - `401`: Invalid API key
  - `403`: API key lacks permissions
  - `429`: Rate limit exceeded
  - `500`: Server error

### 4. **Verify API Key Status**
- **Together AI**: Check at https://api.together.xyz/
- **OpenRouter**: Check at https://openrouter.ai/
- **Gemini**: Check at https://makersuite.google.com/

The chat should now work properly with all three providers using their correct API endpoints! 🚀

## **Files Modified**
- ✅ Created `OpenRouterProvider.kt`
- ✅ Created `GeminiProvider.kt` 
- ✅ Updated `ChatLLMService.kt` provider mapping
- ✅ Fixed API endpoint URLs and request formats
# Debug API Endpoints - Together AI & OpenRouter

## Changes Made for Better Debugging

### 1. **Enhanced Error Messages** ✅
- **Before**: Generic "couldn't connect to ai endpoint" 
- **After**: Specific error messages like "API Error (Together AI): HTTP 401 - Invalid API key"

### 2. **Added Detailed Logging** ✅
- Provider creation logging
- API request details (URL, headers, body)
- Response codes and error details
- Model selection logging

### 3. **Fixed Model Selection** ✅
- Providers now use the selected model instead of hardcoded defaults
- Added `setModel()` method to TogetherProvider and OpenRouterProvider

## How to Debug

### 1. **Check Android Logs**
After trying to chat, check the Android logs for detailed error messages:

```bash
adb logcat | grep -E "(ChatLLMService|TogetherProvider|OpenRouterProvider)"
```

Look for messages like:
- `Creating provider for: Together AI with API key length: 64`
- `Together AI: Starting request with model: meta-llama/Llama-3.2-3B-Instruct-Turbo`
- `Together AI: Response code: 401` (or other error codes)

### 2. **Test API Keys Manually**

#### Together AI Test:
```bash
curl -X POST "https://api.together.xyz/v1/chat/completions" \
  -H "Authorization: Bearer 31980f30c8041ce661665b782482f885d89a2296abf856111bafee8507c64d5c" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3.2-3B-Instruct-Turbo",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 20
  }'
```

#### OpenRouter Test:
```bash
curl -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer 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" \
  -H "Content-Type: application/json" \
  -H "HTTP-Referer: https://vortexai.app" \
  -d '{
    "model": "openai/gpt-4o-mini",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 20
  }'
```

### 3. **Common Error Codes**

#### HTTP 401 - Unauthorized
- **Cause**: Invalid API key
- **Solution**: Check API key format and validity
- **Together AI**: Should be 64-character alphanumeric
- **OpenRouter**: Should start with `sk-or-`

#### HTTP 403 - Forbidden
- **Cause**: API key lacks permissions or account suspended
- **Solution**: Check account status on provider website

#### HTTP 429 - Rate Limited
- **Cause**: Too many requests
- **Solution**: Wait and try again

#### HTTP 400 - Bad Request
- **Cause**: Invalid request format or model name
- **Solution**: Check model name exists and request format

#### Connection Timeout
- **Cause**: Network issues or server down
- **Solution**: Check internet connection and try different provider

### 4. **Debugging Steps**

1. **Enter API Key**: Make sure the key is saved properly
2. **Check Logs**: Look for specific error messages in Android logs
3. **Test Manually**: Use curl commands to test API keys directly
4. **Try Different Model**: Some models might not be available
5. **Check Network**: Ensure internet connection works
6. **Try Different Provider**: Test with ModelsLab to isolate issue

### 5. **Expected Log Output**

#### Successful Request:
```
ChatLLMService: Creating provider for: Together AI with API key length: 64
ChatLLMService: Provider created: TogetherProvider, ready: true
TogetherProvider: Starting request with model: meta-llama/Llama-3.2-3B-Instruct-Turbo
TogetherProvider: Making request to: https://api.together.xyz/v1/chat/completions
TogetherProvider: Response code: 200
ChatLLMService: LLM response received: Hello! How can I help you today?...
```

#### Failed Request:
```
ChatLLMService: Creating provider for: Together AI with API key length: 64
ChatLLMService: Provider created: TogetherProvider, ready: true
TogetherProvider: Starting request with model: meta-llama/Llama-3.2-3B-Instruct-Turbo
TogetherProvider: Making request to: https://api.together.xyz/v1/chat/completions
TogetherProvider: Response code: 401
TogetherProvider: Together API error: 401 - {"error": "Invalid API key"}
ChatLLMService: LLM API call failed for Together AI: Together API error: 401 - {"error": "Invalid API key"}
```

### 6. **Next Steps**

1. **Install the updated app** with enhanced error messages
2. **Try chatting** with Together AI or OpenRouter
3. **Check the error message** - it should now show the specific error instead of generic message
4. **Share the specific error** so we can fix the exact issue

The app will now show you exactly what's wrong instead of the generic "couldn't connect" message! 🔍
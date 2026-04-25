# API Setup Guide for VortexAI Android App

This guide will help you configure and fix the various API endpoints used in the VortexAI Android application.

## Overview

The app integrates with multiple AI service providers:
- **LLM APIs**: Together AI, OpenAI, ModelsLab, Anthropic, Gemini
- **Image Generation**: ModelsLab Image API
- **Text-to-Speech**: ModelsLab TTS API
- **Local Services**: Ollama, Kobold AI

## 1. LLM API Configuration

### Together AI
1. **Get API Key**: Visit [Together AI](https://api.together.xyz/) and create an account
2. **API Key Format**: Long alphanumeric string (32+ characters)
3. **Endpoint**: `https://api.together.xyz/v1/chat/completions`
4. **Models**: Llama 2, Mixtral, Nous Hermes, etc.

**Test Command**:
```bash
curl -X POST "https://api.together.xyz/v1/chat/completions" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3.2-3B-Instruct-Turbo",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 50
  }'
```

### OpenAI
1. **Get API Key**: Visit [OpenAI Platform](https://platform.openai.com/api-keys)
2. **API Key Format**: Starts with `sk-` (e.g., `sk-1234567890abcdef...`)
3. **Endpoint**: `https://api.openai.com/v1/chat/completions`
4. **Models**: GPT-4, GPT-3.5-turbo, etc.

**Test Command**:
```bash
curl -X POST "https://api.openai.com/v1/chat/completions" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 50
  }'
```

### ModelsLab LLM
1. **Get API Key**: Visit [ModelsLab](https://modelslab.com/) and create an account
2. **API Key Format**: Alphanumeric string (20+ characters)
3. **Endpoint**: `https://modelslab.com/api/uncensored-chat/v1/chat/completions`
4. **Model**: `ModelsLab/Llama-3.1-8b-Uncensored-Dare`

**Test Command**:
```bash
curl -X POST "https://modelslab.com/api/uncensored-chat/v1/chat/completions" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 50,
    "model": "ModelsLab/Llama-3.1-8b-Uncensored-Dare"
  }'
```

## 2. Image Generation API Configuration

### ModelsLab Image API
1. **Same API Key**: Use the same ModelsLab API key from above
2. **Endpoint**: `https://modelslab.com/api/v6/images/text2img`
3. **Available Models**: See `image_models.csv` for full list

**Test Command**:
```bash
curl -X POST "https://modelslab.com/api/v6/images/text2img" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "YOUR_API_KEY",
    "model_id": "realistic-vision-v6.0",
    "prompt": "a beautiful landscape",
    "width": "512",
    "height": "512",
    "samples": "1",
    "num_inference_steps": "31",
    "safety_checker": "no",
    "enhance_prompt": "yes",
    "guidance_scale": 7.5
  }'
```

**Popular Models**:
- `realistic-vision-v6.0` - Photorealistic images
- `anime-diffusion` - Anime style
- `midjourney` - Midjourney style
- `flux` - FLUX model for high quality

## 3. Text-to-Speech API Configuration

### ModelsLab TTS API
1. **Same API Key**: Use the same ModelsLab API key
2. **Endpoint**: `https://modelslab.com/api/v6/voice/text_to_audio`
3. **Languages**: English, Hindi

**Test Command**:
```bash
curl -X POST "https://modelslab.com/api/v6/voice/text_to_audio" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "YOUR_API_KEY",
    "prompt": "Hello, this is a test of text to speech.",
    "language": "english",
    "speed": 1
  }'
```

## 4. Local Service Configuration

### Ollama
1. **Installation**: Download from [Ollama.ai](https://ollama.ai/)
2. **Default Endpoint**: `http://localhost:11434` (or `http://10.0.2.2:11434` for Android emulator)
3. **No API Key Required**
4. **Models**: Pull models using `ollama pull llama2`

**Test Command**:
```bash
curl -X POST "http://localhost:11434/api/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2",
    "prompt": "Hello",
    "stream": false
  }'
```

### Kobold AI
1. **Installation**: Follow [KoboldAI GitHub](https://github.com/KoboldAI/KoboldAI-Client)
2. **Default Endpoint**: `http://localhost:5000`
3. **No API Key Required**

## 5. Common Issues and Solutions

### Issue 1: "API key not configured"
**Solution**: 
1. Open app settings
2. Navigate to API Configuration
3. Enter your API key for the respective service
4. Validate the key format matches the provider requirements

### Issue 2: "Connection failed"
**Solution**:
1. Check internet connectivity
2. Verify the API endpoint URL is correct
3. For local services (Ollama/Kobold), ensure they're running
4. Check firewall settings

### Issue 3: "Authentication failed (401)"
**Solution**:
1. Verify API key is correct and not expired
2. Check if the API key has the required permissions
3. Ensure the API key format is correct for the provider

### Issue 4: "Rate limit exceeded (429)"
**Solution**:
1. Wait before making another request
2. Consider upgrading your API plan
3. Implement request throttling in the app

### Issue 5: "Model not found"
**Solution**:
1. Check if the model ID is correct
2. Verify the model is available for your API tier
3. Use the model list endpoints to see available models

## 6. Testing Your Configuration

The app includes built-in API testing functionality:

1. **Open Settings** → **API Testing**
2. **Run Full Test Suite** - Tests all configured APIs
3. **Individual Tests** - Test specific providers
4. **Connectivity Test** - Check network connectivity to endpoints

### Manual Testing

You can also test APIs manually using the provided curl commands above. Replace `YOUR_API_KEY` with your actual API key.

## 7. Troubleshooting Steps

1. **Check API Key Validity**:
   - Ensure the key is not expired
   - Verify it has the correct permissions
   - Test with the provider's official documentation

2. **Network Diagnostics**:
   - Use the app's built-in network diagnostics
   - Check DNS resolution for API endpoints
   - Verify no proxy/firewall blocking

3. **Log Analysis**:
   - Enable debug logging in the app
   - Check Android logs for detailed error messages
   - Look for specific HTTP error codes

4. **Provider Status**:
   - Check the provider's status page
   - Look for maintenance announcements
   - Verify service availability in your region

## 8. Best Practices

1. **API Key Security**:
   - Never commit API keys to version control
   - Use environment variables or secure storage
   - Rotate keys regularly

2. **Error Handling**:
   - Implement proper retry logic
   - Show user-friendly error messages
   - Log technical details for debugging

3. **Rate Limiting**:
   - Respect provider rate limits
   - Implement exponential backoff
   - Cache responses when appropriate

4. **Monitoring**:
   - Monitor API usage and costs
   - Set up alerts for failures
   - Track response times and success rates

## 9. Support and Resources

- **ModelsLab Documentation**: [https://modelslab.com/docs](https://modelslab.com/docs)
- **Together AI Documentation**: [https://docs.together.ai](https://docs.together.ai)
- **OpenAI Documentation**: [https://platform.openai.com/docs](https://platform.openai.com/docs)
- **Ollama Documentation**: [https://github.com/ollama/ollama](https://github.com/ollama/ollama)

For additional support, check the app's built-in help system or contact the development team.
# Custom API Configuration Guide

## Overview
VortexAndroid now supports custom API configurations for:
- **Text Generation** (LLM/Chat)
- **Image Generation**
- **Image Editing**

Each API type has its own independent configuration with custom models.

## Configuration Steps

### 1. Add API Configuration

Navigate to: **Settings → Custom APIs → [Select Type]**

Required fields:
- **Name**: Friendly name for your API (e.g., "My OpenAI API")
- **Base URL**: API endpoint (e.g., `https://api.openai.com/v1`)
- **API Key**: Your authentication key

Optional:
- **Custom Headers**: Additional headers (e.g., `Organization: org-xxx`)

### 2. Add Models

After creating an API configuration, add models:

Required fields:
- **Model ID**: Exact model identifier (e.g., `gpt-4`, `flux-dev`)
- **Display Name**: User-friendly name

Optional fields:
- **Context Length**: Maximum context window
- **Max Tokens**: Maximum output tokens
- **Supports Streaming**: Enable for streaming responses
- **Additional Parameters**: Custom parameters as key-value pairs

## API Format Examples

### Text Generation (OpenAI-Compatible)

**Configuration:**
```
Name: OpenAI
Base URL: https://api.openai.com/v1
API Key: sk-...
Headers: 
  Authorization: Bearer {API_KEY}
```

**Request Format:**
```json
POST /chat/completions
{
  "model": "gpt-4",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "max_tokens": 1000,
  "temperature": 0.7
}
```

**Expected Response:**
```json
{
  "choices": [
    {
      "message": {
        "content": "Response text"
      }
    }
  ]
}
```

### Image Generation

**Configuration:**
```
Name: Replicate
Base URL: https://api.replicate.com/v1
API Key: r8_...
Headers:
  Authorization: Token {API_KEY}
```

**Request Format:**
```json
POST /predictions
{
  "version": "model-version-id",
  "input": {
    "prompt": "A beautiful landscape",
    "width": 1024,
    "height": 1024
  }
}
```

**Expected Response:**
```json
{
  "id": "prediction-id",
  "status": "succeeded",
  "output": ["https://image-url.png"]
}
```

### Image Editing

**Configuration:**
```
Name: Together AI
Base URL: https://api.together.xyz/v1
API Key: ...
```

**Request Format:**
```json
POST /images/edits
{
  "model": "qwen-vl",
  "prompt": "Edit description",
  "image": "base64-or-url",
  "width": 1024,
  "height": 1024
}
```

## Supported Providers

### Text Generation
- OpenAI
- Anthropic
- Together AI
- OpenRouter
- Any OpenAI-compatible API

### Image Generation
- Replicate
- ModelsLab
- Stability AI
- Together AI
- Any compatible API

### Image Editing
- Together AI (Qwen-VL)
- Replicate
- Any compatible API

## Custom Headers

Common use cases:
- `Authorization: Bearer {token}`
- `Organization: org-id`
- `X-API-Version: v1`
- `Content-Type: application/json`

## Model Parameters

### Text Generation
- `temperature`: Creativity (0.0-2.0)
- `top_p`: Nucleus sampling
- `frequency_penalty`: Repetition penalty
- `presence_penalty`: Topic diversity

### Image Generation
- `width`: Image width
- `height`: Image height
- `steps`: Generation steps
- `guidance_scale`: Prompt adherence

### Image Editing
- `strength`: Edit intensity (0.0-1.0)
- `guidance_scale`: Prompt adherence

## Troubleshooting

### API Not Working
1. Verify Base URL is correct
2. Check API Key is valid
3. Ensure headers are properly formatted
4. Test with API provider's documentation

### Model Not Found
1. Verify Model ID matches provider's documentation
2. Check if model requires specific version/format
3. Ensure API key has access to the model

### Response Parsing Errors
1. Check API response format matches expected structure
2. Verify JSON structure in provider's documentation
3. Check for rate limiting or quota issues

## Security Notes

- API keys are stored encrypted in local database
- Never share your API keys
- Use environment-specific keys (dev/prod)
- Rotate keys regularly
- Monitor API usage and costs

## Examples

### OpenRouter Configuration
```
Name: OpenRouter
Base URL: https://openrouter.ai/api/v1
API Key: sk-or-v1-...
Headers:
  HTTP-Referer: https://vortexai.app
  X-Title: VortexAI
```

### ModelsLab Configuration
```
Name: ModelsLab
Base URL: https://modelslab.com/api/v6
API Key: ...
```

### Together AI Configuration
```
Name: Together AI
Base URL: https://api.together.xyz/v1
API Key: ...
```

## API Response Requirements

### Text Generation
Must return JSON with:
- `choices[0].message.content` OR
- `choices[0].text` OR
- `content` field

### Image Generation
Must return JSON with:
- `output` array with image URLs OR
- `images` array with URLs OR
- `url` field

### Image Editing
Must return JSON with:
- `output` array with edited image URLs OR
- `images` array with URLs OR
- `url` field

## Rate Limiting

Configure rate limits in Additional Parameters:
```
rate_limit: 10
rate_limit_period: 60
```

## Cost Tracking

Add cost parameters to models:
```
cost_per_1k_tokens: 0.03
cost_per_image: 0.05
```

## Support

For issues or questions:
1. Check provider's API documentation
2. Verify configuration matches examples
3. Test with provider's official tools first
4. Check app logs for detailed error messages

# Custom API System - Complete Guide

## Overview
The Custom API System allows you to add, configure, and manage any API provider for text generation, image generation, and image editing directly from the app.

## Features
- ✅ Add unlimited custom API providers
- ✅ Configure request/response schemas
- ✅ Manage multiple models per provider
- ✅ Define custom parameters with validation
- ✅ Test connections before use
- ✅ Template-based setup for common providers
- ✅ Secure API key encryption
- ✅ Enable/disable providers on the fly

## Getting Started

### Step 1: Navigate to Custom APIs
1. Open **Settings**
2. Go to **LLM Config**, **Image Gen**, or **Image Edit** tab
3. Click **"Configure Custom APIs"** button

### Step 2: Add a Provider
1. Click the **+** button in the top right
2. Fill in:
   - **Provider Name**: Friendly name (e.g., "My OpenAI")
   - **Base URL**: API endpoint (e.g., `https://api.openai.com`)
   - **API Key**: Your authentication key
3. Click **Save**

### Step 3: Configure Endpoint
1. Select your provider from the list
2. Click **Add Endpoint**
3. Choose a template or configure manually:
   - **Template**: Select from pre-built templates
   - **Endpoint Path**: API path (e.g., `/v1/chat/completions`)
   - **HTTP Method**: GET, POST, PUT, DELETE, PATCH
   - **Request Schema**: JSON schema for requests
   - **Response Schema**: JSON schema for responses
4. Click **Save**

### Step 4: Add Models
1. With provider selected, click **Add Model**
2. Fill in:
   - **Model ID**: Exact model identifier (e.g., `gpt-4`)
   - **Display Name**: User-friendly name
   - **Capabilities**: Enable streaming, vision, etc.
3. Click **Add**

### Step 5: Configure Parameters (Optional)
1. Select a model
2. Click the settings icon
3. Add parameters:
   - **Parameter Name**: e.g., `temperature`
   - **Type**: STRING, INTEGER, FLOAT, BOOLEAN, ARRAY, OBJECT
   - **Default Value**: Optional default
   - **Min/Max**: For numeric types
   - **Required**: Toggle if mandatory
   - **Description**: Help text
4. Click **Add**

### Step 6: Test Connection
1. With provider, endpoint, and model configured
2. Click **Test Connection**
3. Verify success message

## Schema Format

### Request Schema
```json
{
  "headers": {
    "Authorization": "Bearer {apiKey}",
    "Content-Type": "application/json"
  },
  "body": {
    "model": "{modelId}",
    "messages": "{messages}",
    "temperature": "{temperature}",
    "max_tokens": "{maxTokens}"
  },
  "parameterMapping": {
    "messages": "array",
    "temperature": "float",
    "maxTokens": "integer"
  }
}
```

**Placeholders:**
- `{apiKey}` - Replaced with provider's API key
- `{modelId}` - Replaced with selected model ID
- `{paramName}` - Replaced with parameter value

### Response Schema
```json
{
  "dataPath": "choices[0].message.content",
  "streamingPath": "choices[0].delta.content",
  "errorPath": "error.message",
  "imageUrlPath": "data[0].url",
  "statusPath": "status"
}
```

**Path Format:**
- Use dot notation: `object.field`
- Use brackets for arrays: `array[0]`
- Combine: `choices[0].message.content`

## Templates

### Text Generation

#### OpenAI Compatible
- **Endpoint**: `/v1/chat/completions`
- **Method**: POST
- **Use for**: OpenAI, Azure OpenAI, LocalAI, LM Studio

#### Anthropic Claude
- **Endpoint**: `/v1/messages`
- **Method**: POST
- **Use for**: Claude API

### Image Generation

#### Stability AI
- **Endpoint**: `/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image`
- **Method**: POST
- **Use for**: Stability AI API

#### Replicate
- **Endpoint**: `/v1/predictions`
- **Method**: POST
- **Use for**: Replicate models

### Image Editing

#### Stability AI Inpainting
- **Endpoint**: `/v1/generation/stable-diffusion-xl-1024-v1-0/image-to-image`
- **Method**: POST

#### Replicate Image Edit
- **Endpoint**: `/v1/predictions`
- **Method**: POST

## Common Providers

### OpenAI
```
Name: OpenAI
Base URL: https://api.openai.com
API Key: sk-...
Template: OpenAI Compatible
Models: gpt-4, gpt-3.5-turbo, gpt-4-turbo
```

### Anthropic
```
Name: Anthropic
Base URL: https://api.anthropic.com
API Key: sk-ant-...
Template: Anthropic Claude
Models: claude-3-opus, claude-3-sonnet
```

### Together AI
```
Name: Together AI
Base URL: https://api.together.xyz
API Key: ...
Template: OpenAI Compatible
Models: meta-llama/Llama-3-70b-chat-hf
```

### Replicate
```
Name: Replicate
Base URL: https://api.replicate.com
API Key: r8_...
Template: Replicate
Models: stability-ai/sdxl, black-forest-labs/flux-dev
```

### Stability AI
```
Name: Stability AI
Base URL: https://api.stability.ai
API Key: sk-...
Template: Stability AI
Models: stable-diffusion-xl-1024-v1-0
```

## Parameter Types

### STRING
- Text values
- No validation
- Example: `"hello world"`

### INTEGER
- Whole numbers
- Supports min/max validation
- Example: `100`, `512`, `1024`

### FLOAT
- Decimal numbers
- Supports min/max validation
- Example: `0.7`, `1.5`, `7.5`

### BOOLEAN
- True/false values
- Example: `true`, `false`

### ARRAY
- List of values
- JSON array format
- Example: `[1, 2, 3]`, `["a", "b"]`

### OBJECT
- Complex nested data
- JSON object format
- Example: `{"key": "value"}`

## Common Parameters

### Text Generation
- `temperature` (FLOAT, 0.0-2.0): Creativity level
- `max_tokens` (INTEGER, 1-8192): Response length
- `top_p` (FLOAT, 0.0-1.0): Nucleus sampling
- `frequency_penalty` (FLOAT, 0.0-2.0): Repetition penalty
- `presence_penalty` (FLOAT, 0.0-2.0): Topic diversity

### Image Generation
- `width` (INTEGER): Image width in pixels
- `height` (INTEGER): Image height in pixels
- `steps` (INTEGER, 10-100): Generation steps
- `guidance_scale` (FLOAT, 1.0-20.0): Prompt adherence
- `negative_prompt` (STRING): What to avoid

### Image Editing
- `strength` (FLOAT, 0.0-1.0): Edit intensity
- `guidance_scale` (FLOAT, 1.0-20.0): Prompt adherence
- `image` (STRING): Base64 or URL

## Troubleshooting

### Connection Failed
1. Verify Base URL is correct (no trailing slash)
2. Check API Key is valid
3. Ensure endpoint path is correct
4. Test with provider's official documentation

### Invalid Schema
1. Validate JSON syntax
2. Check placeholder names match parameters
3. Verify path format (dot notation, brackets)

### Response Parsing Failed
1. Check response schema paths
2. Test with actual API response
3. Verify data path exists in response

### Parameter Validation Error
1. Check parameter type matches value
2. Verify min/max ranges
3. Ensure required parameters are provided

## Best Practices

### Security
- Never share API keys
- Use environment-specific keys
- Rotate keys regularly
- Monitor API usage

### Organization
- Use descriptive provider names
- Group related models
- Document custom parameters
- Test before production use

### Performance
- Set appropriate timeouts
- Use streaming when available
- Cache responses when possible
- Monitor rate limits

## Advanced Features

### Custom Headers
Add custom headers in request schema:
```json
{
  "headers": {
    "Authorization": "Bearer {apiKey}",
    "X-Custom-Header": "value",
    "Organization": "org-id"
  }
}
```

### Dynamic Values
Use parameters in any field:
```json
{
  "body": {
    "model": "{modelId}",
    "custom_field": "{customParam}"
  }
}
```

### Nested Objects
Handle complex structures:
```json
{
  "body": {
    "input": {
      "prompt": "{prompt}",
      "settings": {
        "quality": "{quality}"
      }
    }
  }
}
```

### Array Responses
Extract from arrays:
```json
{
  "imageUrlPath": "output[0]",
  "dataPath": "results[0].text"
}
```

## Support

### Getting Help
1. Check this documentation
2. Verify provider's API documentation
3. Test with provider's official tools
4. Check app logs for detailed errors

### Reporting Issues
Include:
- Provider name and type
- Request/response schemas
- Error messages
- Steps to reproduce

## Updates

### Version 1.0
- Initial release
- Basic CRUD operations
- Template support
- Parameter management
- Connection testing

### Planned Features
- Import/export providers
- Batch model import
- Advanced parameter types
- Response transformations
- Rate limit handling

# Custom API Configuration Guide

## Overview

The Custom API system in Vortex Android allows you to connect to any OpenAI-compatible API endpoint for text generation, image generation, and image editing. This guide will help you set up and use custom APIs.

## Accessing Custom API Configuration

1. Open **Settings** from the main menu
2. Navigate to the appropriate tab:
   - **LLM Configuration** for text generation
   - **Image Generation** for image creation
   - **Image Editing** for image modification
3. Click the **"Configure Custom APIs"** button at the top of the tab

## Setting Up a Custom API Provider

### Step 1: Add a Provider

1. Click the **"+"** button in the top-right corner
2. Fill in the provider details:
   - **Provider Name**: A friendly name (e.g., "My OpenAI Compatible API")
   - **Base URL**: Your API endpoint (e.g., `https://api.example.com`)
   - **API Key**: Your authentication key
3. Click **"Save"**

### Step 2: Add Endpoints

After creating a provider, you need to define endpoints:

1. Select your provider from the list
2. Click **"Add Endpoint"** in the Endpoints section
3. Choose a template or configure manually:
   - **Template**: Pre-configured schemas for common APIs
   - **Endpoint Path**: The API path (e.g., `/v1/chat/completions`)
   - **HTTP Method**: Usually POST
   - **Request Schema**: JSON structure for requests
   - **Response Schema**: JSON structure for parsing responses
4. Click **"Save"**

### Step 3: Add Models

Define which models are available:

1. In the Models section, click **"Add Model"**
2. Configure the model:
   - **Model ID**: The model identifier (e.g., `gpt-4`, `flux-dev`)
   - **Display Name**: User-friendly name
   - **Capabilities**: Enable streaming, vision, etc.
3. Click **"Add"**

### Step 4: Test Connection

1. Click **"Test Connection"** to verify your configuration
2. If successful, you'll see a ✅ success message
3. If it fails, check your endpoint URL, API key, and schemas

## Using Custom APIs

### For Text Generation (LLM)

1. Go to **Settings → LLM Configuration**
2. Click **"Configure Custom Text Generation APIs"**
3. Set up your provider, endpoints, and models
4. The custom API will now be available in the LLM provider dropdown

### For Image Generation

1. Go to **Settings → Image Generation**
2. Click **"Configure Custom Image Generation APIs"**
3. Set up your provider, endpoints, and models
4. The custom API will now be available in the Image Provider dropdown

### For Image Editing

1. Go to **Settings → Image Editing**
2. Click **"Configure Custom Image Editing APIs"**
3. Set up your provider, endpoints, and models
4. The custom API will now be available in the Image Editing Provider dropdown

## Schema Configuration

### Request Schema

The request schema defines how to structure API requests. Example for OpenAI-compatible chat:

```json
{
  "model": "{{model}}",
  "messages": "{{messages}}",
  "temperature": "{{temperature}}",
  "max_tokens": "{{max_tokens}}"
}
```

### Response Schema

The response schema defines how to parse API responses. Example:

```json
{
  "dataPath": "choices[0].message.content",
  "errorPath": "error.message",
  "statusPath": "status"
}
```

## Templates

The system includes pre-configured templates for:

### Text Generation
- **OpenAI Compatible**: Standard chat completions format
- **Anthropic Claude**: Claude API format
- **Custom Format**: Flexible custom structure

### Image Generation
- **OpenAI DALL-E**: DALL-E image generation
- **Stability AI**: Stable Diffusion format
- **Custom Format**: Flexible custom structure

### Image Editing
- **OpenAI Edit**: Image editing format
- **Stability AI**: Stable Diffusion editing
- **Custom Format**: Flexible custom structure

## Troubleshooting

### Connection Failed

1. **Check Base URL**: Ensure it's correct and accessible
2. **Verify API Key**: Make sure your key is valid
3. **Test Endpoint**: Try the endpoint in a tool like Postman
4. **Check Schemas**: Ensure request/response schemas match your API

### Invalid Response

1. **Review Response Schema**: Make sure paths are correct
2. **Check API Documentation**: Verify the response format
3. **Test with Simple Request**: Try a minimal request first

### Model Not Available

1. **Add Model Manually**: Use the "Add Model" button
2. **Check Model ID**: Ensure it matches your API's model identifier
3. **Verify Capabilities**: Enable appropriate capabilities (streaming, vision)

## Best Practices

1. **Use Templates**: Start with a template and modify as needed
2. **Test Incrementally**: Test after each configuration step
3. **Document Your Setup**: Keep notes on your custom configurations
4. **Use Descriptive Names**: Make providers and models easy to identify
5. **Secure Your Keys**: Never share your API keys

## Examples

### Example 1: OpenAI Compatible API

```
Provider Name: My OpenAI API
Base URL: https://api.openai.com
API Key: sk-...

Endpoint Path: /v1/chat/completions
HTTP Method: POST
Purpose: chat

Request Schema:
{
  "model": "{{model}}",
  "messages": "{{messages}}",
  "temperature": "{{temperature}}"
}

Response Schema:
{
  "dataPath": "choices[0].message.content"
}

Model ID: gpt-4
Display Name: GPT-4
```

### Example 2: Custom Image Generation API

```
Provider Name: My Image API
Base URL: https://image-api.example.com
API Key: custom-key-...

Endpoint Path: /generate
HTTP Method: POST
Purpose: image_gen

Request Schema:
{
  "prompt": "{{prompt}}",
  "model": "{{model}}",
  "size": "{{size}}"
}

Response Schema:
{
  "imageUrlPath": "data.url"
}

Model ID: stable-diffusion-xl
Display Name: SDXL
```

## Advanced Features

### Multiple Providers

You can configure multiple custom API providers and switch between them:

1. Add multiple providers in the Custom API screen
2. Enable/disable providers using the toggle switch
3. Select the active provider from the dropdown in settings

### Parameter Customization

For advanced users, you can define custom parameters:

1. Select a model
2. Click "Add Parameter"
3. Define parameter name, type, range, and default value
4. Parameters will be available when using the model

## Support

If you encounter issues:

1. Check the app logs for detailed error messages
2. Verify your API endpoint is working with external tools
3. Review the API provider's documentation
4. Ensure your API key has the necessary permissions

## Migration from Old System

If you were using the old custom endpoint fields:

1. Your existing settings are preserved
2. Configure the new Custom API system
3. Test the new configuration
4. The old fields have been removed to avoid confusion

## Security Notes

- API keys are stored securely in the app's encrypted database
- Never share your API keys or configuration files
- Use environment-specific keys (dev/prod)
- Regularly rotate your API keys
- Monitor API usage for unauthorized access

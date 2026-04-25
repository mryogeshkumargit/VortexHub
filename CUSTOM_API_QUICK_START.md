# Custom API Quick Start Guide

## 🚀 Import Fal AI in 30 Seconds

1. **Open Settings** → Image Generation
2. **Click** "Configure Custom Image Generation APIs"
3. **Click** Import button (📥 icon in top bar)
4. **Click** "Use Template" button
5. **Replace** empty `"apiKey": ""` with your Fal AI key
6. **Click** "Import"
7. **Done!** Select "Custom API" → "Fal AI" in Image Generation settings

## 📋 What Gets Imported

When you import the Fal AI template:
- ✅ Provider: Fal AI with your API key
- ✅ Endpoint: /fal-ai/flux-pro (POST)
- ✅ Model: FLUX Pro
- ✅ Parameters: prompt, image_size, steps, guidance_scale, etc.
- ✅ Authentication: Properly configured "Key {apiKey}" header

## 🎯 Using Custom APIs

### For Image Generation
1. Settings → Image Generation
2. Provider: Select "Custom API"
3. Custom Provider: Select your imported provider
4. All settings managed in Custom API configuration

### For Text Generation (LLM)
1. Settings → LLM Configuration
2. Provider: Select "Custom API"
3. Custom Provider: Select your imported provider
4. Model configuration in Custom API screen

### For Image Editing
1. Settings → Image Editing
2. Provider: Select "Custom API"
3. Custom Provider: Select your imported provider

## 🆘 Need Help?

Click the **Help button (❓)** in Custom API screen for:
- Step-by-step configuration guide
- Request/Response schema examples
- Available placeholders
- Import/Export instructions

## 📝 Example Configurations Included

### Fal AI (Image Generation)
- File: `EXAMPLE_FAL_AI_CONFIG.json`
- Models: FLUX Pro
- Features: High-quality image generation

### OpenAI Compatible (Text Generation)
- File: `EXAMPLE_OPENAI_COMPATIBLE_CONFIG.json`
- Models: GPT-4o
- Features: Chat, streaming, vision support

## 🔧 Manual Configuration

If you prefer manual setup:
1. Click **Add Provider** (+)
2. Enter name, base URL, API key
3. Click **Add Endpoint**
4. Configure request/response schemas
5. Click **Add Model**
6. Add parameters (optional)

## 💡 Pro Tips

- **Templates**: Click "Show Template" in Import dialog to see structure
- **Edit Anytime**: Click edit icon (✏️) next to any item
- **Test Connection**: Use test button after configuration
- **Share Configs**: Export as JSON and share with others
- **Multiple Providers**: Add unlimited custom API providers

## 🔐 Authentication Formats

### Fal AI
```json
"headers": {
  "Authorization": "Key {apiKey}"
}
```

### OpenAI / Most APIs
```json
"headers": {
  "Authorization": "Bearer {apiKey}"
}
```

### Custom Header
```json
"headers": {
  "X-API-Key": "{apiKey}"
}
```

## 🎨 Supported Placeholders

### All Types
- `{apiKey}` - Your API key
- `{modelId}` - Selected model

### Text Generation
- `{messages}` - Chat messages
- `{temperature}` - Randomness (0.0-2.0)
- `{maxTokens}` - Response length
- `{topP}` - Nucleus sampling
- `{frequencyPenalty}` - Repetition control

### Image Generation
- `{prompt}` - Image description
- `{width}` - Image width
- `{height}` - Image height
- `{steps}` - Quality (more = better)
- `{guidanceScale}` - Prompt adherence
- `{negativePrompt}` - What to avoid

### Image Editing
- `{image}` - Source image
- `{prompt}` - Edit instruction
- `{strength}` - Edit intensity

## ⚠️ Troubleshooting

### "401 Unauthorized"
- Check API key is correct
- Verify authentication header format
- Ensure API key has proper permissions

### "Import Failed"
- Validate JSON syntax
- Check all required fields present
- Use template as reference

### "No Response"
- Verify base URL is correct
- Check endpoint path
- Test with provider's documentation

## 🌟 Popular APIs You Can Add

- Fal AI (FLUX, Stable Diffusion)
- OpenAI (GPT-4, DALL-E)
- Anthropic (Claude)
- Google (Gemini)
- Mistral AI
- Cohere
- Replicate
- Stability AI
- Any OpenAPI-compatible service

## 📱 App Features

- ✅ Persistent storage (survives app restart)
- ✅ Encrypted API keys
- ✅ Multiple providers per type
- ✅ Edit any configuration
- ✅ Import/Export JSON
- ✅ In-app help system
- ✅ Connection testing
- ✅ Parameter validation

## 🎓 Learning Path

1. **Start**: Import Fal AI template
2. **Explore**: Click Help button to learn
3. **Customize**: Edit imported configuration
4. **Expand**: Add more providers
5. **Share**: Export and share configs

---

**Questions?** Click the Help button (❓) in any Custom API screen for detailed guidance!

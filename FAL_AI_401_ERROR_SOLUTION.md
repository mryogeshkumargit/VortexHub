# Fal AI 401 Authentication Error - Solution

## Problem
After importing `EXAMPLE_FAL_AI_CONFIG.json`, you're getting HTTP 401 authentication error.

## Root Cause
The imported configuration has an **empty API key**:
```json
"apiKey": ""
```

The placeholder `{apiKey}` in the Authorization header gets replaced with an empty string, resulting in:
```
Authorization: Key 
```

This causes Fal AI to reject the request with 401 Unauthorized.

## Solution

### Option 1: Edit Provider After Import (Recommended)
1. Import the JSON configuration
2. Go to Custom API screen
3. Click the **Edit icon** (pencil) on the "Fal AI" provider card
4. Enter your actual Fal AI API key in the "API Key" field
5. Click "Save"

### Option 2: Edit JSON Before Import
1. Open `EXAMPLE_FAL_AI_CONFIG.json`
2. Replace the empty apiKey:
   ```json
   "apiKey": "YOUR_ACTUAL_FAL_AI_KEY_HERE"
   ```
3. Save the file
4. Import the modified JSON

## How to Get Fal AI API Key
1. Go to https://fal.ai/
2. Sign up or log in
3. Navigate to API Keys section
4. Generate a new API key
5. Copy the key (starts with `fal_`)

## Verification
After adding your API key:
1. Go to Custom API → Image Generation
2. Select "Fal AI" provider
3. Click "Test Connection" button
4. You should see "✅ Connection successful!"

## Technical Details

### Request Flow
1. User enters API key in provider settings
2. API key is encrypted and stored in database
3. When making request, key is decrypted
4. Placeholder `{apiKey}` in headers is replaced with actual key
5. Final header sent to Fal AI:
   ```
   Authorization: Key fal_your_actual_key_here
   ```

### Why "Key" prefix?
Fal AI uses `Key` prefix instead of `Bearer`:
- ❌ Wrong: `Authorization: Bearer fal_key`
- ✅ Correct: `Authorization: Key fal_key`

This is already configured correctly in the JSON template.

## Server Import File List Feature

### New Feature
The server import dialog now automatically fetches and lists all JSON files from the server.

### How It Works
1. Enter server URL (or scan QR code)
2. Click **Refresh** button (circular arrow icon)
3. App calls `http://server:port/list` endpoint
4. Server returns JSON: `{"files": ["file1.json", "file2.json"]}`
5. Files appear in dropdown menu
6. Select file and click "Download & Import"

### Server Requirements
Your `file_transfer_server.py` must have a `/list` endpoint that returns:
```json
{
  "files": [
    "EXAMPLE_FAL_AI_CONFIG.json",
    "EXAMPLE_OPENAI_COMPATIBLE_CONFIG.json",
    "custom_config.json"
  ]
}
```

### User Flow
```
1. Click "Import from Server" (cloud icon)
2. Scan QR code OR enter URL manually
3. Click Refresh icon next to URL field
4. Select file from dropdown
5. Click "Download & Import"
6. Edit provider to add API key
7. Test connection
```

## Summary
- **401 Error**: Empty API key in imported configuration
- **Fix**: Edit provider after import and add your actual Fal AI API key
- **New Feature**: Server import now lists available files automatically
- **Test**: Use "Test Connection" button to verify setup

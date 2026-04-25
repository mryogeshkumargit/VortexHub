# 🔍 Together AI Debug Instructions

## 🚨 COMPREHENSIVE LOGGING ADDED

Since no errors are shown but only 8 models appear, I've added **detailed logging** to see exactly what's happening with the API response and filtering.

## 📱 **Steps to Debug**

### **1. Install Updated APK**
The new build has comprehensive logging to trace every step.

### **2. Enable Android Studio Logcat**
```bash
# Option 1: Android Studio
- Open Logcat tab
- Filter by "ChatLLMService" or "ImageGenerationService"

# Option 2: ADB Command Line
adb logcat | grep -E "(ChatLLMService|ImageGenerationService)"
```

### **3. Test Model Fetching**
1. Open **Settings > LLM Config**
2. Select **"Together AI"**
3. Enter your API key
4. Click **"Clear Cache"**
5. Click **"Fetch Models"**
6. **Watch the logs carefully!**

## 🔍 **What to Look For in Logs**

### **LLM Models Debug Output:**
```
D/ChatLLMService: Fetching Together AI models with API key: sk-xxxxx...
D/ChatLLMService: Together AI API response code: 200
D/ChatLLMService: Together AI API response body length: XXXX
D/ChatLLMService: Together AI API response preview: {"data":[...
D/ChatLLMService: Together AI models array length: XX
D/ChatLLMService: Model 0: {"id":"model-name","type":"..."}
D/ChatLLMService: Model 1: {"id":"model-name","type":"..."}
D/ChatLLMService: ✅ Included LLM model: model-name-1
D/ChatLLMService: ❌ Excluded model: model-name-2 (flux model)
D/ChatLLMService: Together AI filtering: XX total, X included, XX excluded
```

### **Image Models Debug Output:**
```
D/ImageGenerationService: Together AI image models response body length: XXXX
D/ImageGenerationService: Together AI image models response preview: {"data":[...
D/ImageGenerationService: Together AI image models array length: XX
D/ImageGenerationService: ✅ Included image model: flux-model (type: image)
D/ImageGenerationService: ❌ Excluded image model: chat-model (non-image model)
D/ImageGenerationService: Together AI image filtering complete: X models included
```

## 🔍 **Key Questions the Logs Will Answer**

### **1. Is the API call succeeding?**
- Look for: `Together AI API response code: 200`
- If not 200, we'll see the error code and body

### **2. How many models is the API returning?**
- Look for: `Together AI models array length: XX`
- Should be 50+ if API is working correctly

### **3. What does the response structure look like?**
- Look for: `Model 0: {"id":"...","type":"..."}`
- This shows the actual JSON structure from Together AI

### **4. What models are being excluded and why?**
- Look for: `❌ Excluded model: model-name (reason)`
- This will show us if our filtering is too aggressive

### **5. Are we falling back to defaults?**
- If you see only 8 models with no API logs, we're using cached defaults
- If API succeeds but only 8 included, filtering is wrong

## 🎯 **Most Likely Scenarios**

### **Scenario A: API Returns Many Models, Filtering Excludes Most**
**Logs show:** `Together AI models array length: 60`, but `Together AI filtering: 60 total, 8 included, 52 excluded`
**Problem:** Our filtering logic is too restrictive
**Solution:** Adjust filtering criteria

### **Scenario B: API Returns Only 8 Models**
**Logs show:** `Together AI models array length: 8`
**Problem:** Together AI is only returning 8 models (API rate limiting, different endpoint, etc.)
**Solution:** Check API key permissions or Together AI service status

### **Scenario C: Different Response Format**
**Logs show:** Unexpected JSON structure in model previews
**Problem:** Together AI changed their API response format
**Solution:** Update parsing logic

### **Scenario D: Silent Caching**
**Logs show:** No API call logs at all
**Problem:** Still using cached data despite clearing cache
**Solution:** Check cache clearing logic

## 📝 **Report Back With:**

Please copy/paste the **exact log output** showing:

1. **API Response Code:** `Together AI API response code: XXX`
2. **Models Array Length:** `Together AI models array length: XX`  
3. **First few models:** `Model 0: {...}`
4. **Filtering Summary:** `Together AI filtering: XX total, X included, XX excluded`
5. **Any excluded model examples:** `❌ Excluded model: model-name (reason)`

This will tell us exactly what's happening! 🚀

## 🔧 **If No Logs Appear**

If you don't see any debug logs:
1. Make sure Logcat is filtering correctly
2. Try filtering by just "Together" 
3. Check if the app is actually making the API calls
4. Verify the updated APK was installed

The comprehensive logging will pinpoint the exact issue! 🎯
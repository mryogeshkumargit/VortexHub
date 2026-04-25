#!/usr/bin/env python3
"""
Debug script for ModelsLab API to identify why models are not fetching.
Tests the exact endpoint and parameters used in the Android app.
"""
import json
import requests
import sys

# ModelsLab API key provided by user
API_KEY = "DHcpakbboJ6bVeMmOA73Kt9yKJuK7HoiaM61rZlngmFKOSbmj2cazyKF9Duo"

print(f"🔑 Testing with API key: {API_KEY[:8]}...")
print(f"🔑 API key length: {len(API_KEY)}")

# Test 1: ModelsLab model list endpoint (exact same as Android app)
print("\n=== Testing ModelsLab Model List Endpoint ===")
print("URL: https://modelslab.com/api/v4/dreambooth/model_list")
print("Method: POST")
print("Headers: Content-Type: application/json")

headers = {"Content-Type": "application/json"}
payload = {"key": API_KEY}

try:
    print(f"📡 Sending request...")
    response = requests.post(
        "https://modelslab.com/api/v4/dreambooth/model_list",
        headers=headers,
        json=payload,
        timeout=30
    )
    
    print(f"📊 Status Code: {response.status_code}")
    print(f"📊 Response Headers: {dict(response.headers)}")
    
    if response.status_code == 200:
        try:
            data = response.json()
            print(f"✅ SUCCESS: Got JSON response")
            print(f"📝 Response type: {type(data)}")
            
            if isinstance(data, list):
                print(f"📊 Found {len(data)} models in array")
                
                # Count models by category
                categories = {}
                llm_models = []
                
                for i, model in enumerate(data[:10]):  # Show first 10 models
                    category = model.get("model_category", "unknown")
                    categories[category] = categories.get(category, 0) + 1
                    
                    if category == "llm":
                        llm_models.append({
                            "model_id": model.get("model_id", ""),
                            "model_name": model.get("model_name", ""),
                            "description": model.get("description", "")
                        })
                    
                    print(f"  Model {i+1}: {model.get('model_id', 'N/A')} (category: {category})")
                
                print(f"\n📊 Category breakdown:")
                for category, count in categories.items():
                    print(f"  {category}: {count} models")
                
                print(f"\n📊 LLM models found: {len(llm_models)}")
                for model in llm_models:
                    print(f"  - {model['model_id']}: {model['model_name']}")
                
                if len(llm_models) == 0:
                    print("❌ NO LLM MODELS FOUND - This explains why the app shows no models!")
                else:
                    print(f"✅ Found {len(llm_models)} LLM models - should work in app")
                    
            elif isinstance(data, dict):
                print(f"📊 Got object response with keys: {list(data.keys())}")
                if "data" in data and isinstance(data["data"], list):
                    print(f"📊 Found {len(data['data'])} models in data array")
                else:
                    print(f"❌ Unexpected response structure: {data}")
            else:
                print(f"❌ Unexpected response type: {type(data)}")
                
        except json.JSONDecodeError as e:
            print(f"❌ JSON decode error: {e}")
            print(f"📝 Raw response: {response.text[:500]}...")
    else:
        print(f"❌ HTTP ERROR: {response.status_code}")
        print(f"📝 Error response: {response.text}")
        
except Exception as e:
    print(f"❌ Request failed: {e}")

# Test 2: Check if API key is valid with a simple chat request
print("\n=== Testing ModelsLab Chat Endpoint ===")
print("URL: https://modelslab.com/api/uncensored-chat/v1/chat/completions")

chat_headers = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}

chat_payload = {
    "messages": [
        {
            "role": "system",
            "content": "You are a helpful assistant."
        },
        {
            "role": "user",
            "content": "Say hello in exactly 3 words."
        }
    ],
    "max_tokens": 20,
    "model": "ModelsLab/Llama-3.1-8b-Uncensored-Dare",
    "temperature": 0.7
}

try:
    print(f"📡 Testing chat endpoint...")
    chat_response = requests.post(
        "https://modelslab.com/api/uncensored-chat/v1/chat/completions",
        headers=chat_headers,
        json=chat_payload,
        timeout=30
    )
    
    print(f"📊 Chat Status: {chat_response.status_code}")
    
    if chat_response.status_code == 200:
        chat_data = chat_response.json()
        if "choices" in chat_data and len(chat_data["choices"]) > 0:
            content = chat_data["choices"][0]["message"]["content"]
            print(f"✅ Chat SUCCESS: {content}")
            print(f"✅ API key is valid and working!")
        else:
            print(f"⚠️ Chat response has no choices: {chat_data}")
    else:
        print(f"❌ Chat ERROR: {chat_response.status_code} - {chat_response.text}")
        if chat_response.status_code == 401:
            print(f"❌ API key appears to be invalid!")
        elif chat_response.status_code == 403:
            print(f"❌ API key doesn't have chat permissions!")
        
except Exception as e:
    print(f"❌ Chat test failed: {e}")

print("\n=== SUMMARY ===")
print("If the model list endpoint returns 0 LLM models but the chat endpoint works,")
print("then the issue is with the model filtering logic in the Android app.")
print("If both fail, then there's an issue with the API key or permissions.") 
#!/usr/bin/env python3
"""
Test alternative ModelsLab endpoints to find where LLM models are listed.
"""
import json
import requests
import sys

API_KEY = "DHcpakbboJ6bVeMmOA73Kt9yKJuK7HoiaM61rZlngmFKOSbmj2cazyKF9Duo"

print(f"🔑 Testing with API key: {API_KEY[:8]}...")

# Test different endpoints that might contain LLM models
endpoints_to_test = [
    {
        "name": "v4 dreambooth model_list (current)",
        "url": "https://modelslab.com/api/v4/dreambooth/model_list",
        "method": "POST",
        "headers": {"Content-Type": "application/json"},
        "data": {"key": API_KEY}
    },
    {
        "name": "v4 dreambooth model_list with different payload",
        "url": "https://modelslab.com/api/v4/dreambooth/model_list",
        "method": "POST",
        "headers": {"Content-Type": "application/json"},
        "data": {"api_key": API_KEY}
    },
    {
        "name": "v4 dreambooth model_list with auth header",
        "url": "https://modelslab.com/api/v4/dreambooth/model_list",
        "method": "POST",
        "headers": {"Content-Type": "application/json", "Authorization": f"Bearer {API_KEY}"},
        "data": {}
    },
    {
        "name": "v3 models endpoint",
        "url": "https://modelslab.com/api/v3/models",
        "method": "GET",
        "headers": {"Authorization": f"Bearer {API_KEY}"},
        "data": None
    },
    {
        "name": "v1 models endpoint",
        "url": "https://modelslab.com/api/v1/models",
        "method": "GET",
        "headers": {"Authorization": f"Bearer {API_KEY}"},
        "data": None
    },
    {
        "name": "v6 models endpoint",
        "url": "https://modelslab.com/api/v6/models",
        "method": "GET",
        "headers": {"Authorization": f"Bearer {API_KEY}"},
        "data": None
    },
    {
        "name": "v4 models endpoint",
        "url": "https://modelslab.com/api/v4/models",
        "method": "GET",
        "headers": {"Authorization": f"Bearer {API_KEY}"},
        "data": None
    }
]

for endpoint in endpoints_to_test:
    print(f"\n=== Testing: {endpoint['name']} ===")
    print(f"URL: {endpoint['url']}")
    print(f"Method: {endpoint['method']}")
    
    try:
        if endpoint['method'] == 'GET':
            response = requests.get(
                endpoint['url'],
                headers=endpoint['headers'],
                timeout=30
            )
        else:
            response = requests.post(
                endpoint['url'],
                headers=endpoint['headers'],
                json=endpoint['data'] if endpoint['data'] else {},
                timeout=30
            )
        
        print(f"📊 Status: {response.status_code}")
        
        if response.status_code == 200:
            try:
                data = response.json()
                print(f"✅ Got JSON response")
                
                if isinstance(data, list):
                    print(f"📊 Found {len(data)} items in array")
                    
                    # Look for LLM models
                    llm_models = []
                    categories = {}
                    
                    for i, item in enumerate(data[:20]):  # Check first 20 items
                        if isinstance(item, dict):
                            category = item.get("model_category", item.get("category", "unknown"))
                            categories[category] = categories.get(category, 0) + 1
                            
                            if category == "llm" or category == "LLMaster":
                                llm_models.append({
                                    "id": item.get("model_id", item.get("id", "")),
                                    "name": item.get("model_name", item.get("name", "")),
                                    "category": category
                                })
                    
                    print(f"📊 Categories found: {list(categories.keys())}")
                    print(f"📊 LLM models: {len(llm_models)}")
                    
                    if llm_models:
                        print("✅ FOUND LLM MODELS!")
                        for model in llm_models:
                            print(f"  - {model['id']}: {model['name']} ({model['category']})")
                    else:
                        print("❌ No LLM models found")
                        
                elif isinstance(data, dict):
                    print(f"📊 Got object with keys: {list(data.keys())}")
                    if "models" in data and isinstance(data["models"], list):
                        print(f"📊 Found {len(data['models'])} models in 'models' key")
                    elif "data" in data and isinstance(data["data"], list):
                        print(f"📊 Found {len(data['data'])} models in 'data' key")
                else:
                    print(f"📊 Unexpected response type: {type(data)}")
                    
            except json.JSONDecodeError:
                print(f"❌ Not JSON response: {response.text[:200]}...")
        else:
            print(f"❌ HTTP Error: {response.text[:200]}...")
            
    except Exception as e:
        print(f"❌ Request failed: {e}")

print("\n=== SUMMARY ===")
print("If none of these endpoints return LLM models, then ModelsLab may have:")
print("1. Moved LLM models to a different endpoint")
print("2. Changed the API structure")
print("3. Removed LLM models from the public API")
print("4. Require different authentication or parameters") 
#!/usr/bin/env python3
"""
Comprehensive test script for ModelsLab integration.
Tests both chat and image generation endpoints.
"""
import os
import json
import requests
import time

# Get API key from environment
API_KEY = os.getenv("MODELSLAB_KEY")
if not API_KEY:
    print("❌ Set MODELSLAB_KEY environment variable")
    exit(1)

print(f"🔑 Using API key: {API_KEY[:8]}...")

# Test 1: Chat endpoint
print("\n=== Testing ModelsLab Chat Endpoint ===")
chat_url = "https://modelslab.com/api/uncensored-chat/v1/chat/completions"
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
            "content": "Say hello in exactly 5 words."
        }
    ],
    "max_tokens": 50,
    "model": "ModelsLab/Llama-3.1-8b-Uncensored-Dare",
    "temperature": 0.7,
    "top_p": 0.9
}

try:
    print(f"📡 Sending chat request...")
    chat_response = requests.post(chat_url, headers=chat_headers, json=chat_payload, timeout=30)
    print(f"📊 Chat Status: {chat_response.status_code}")
    
    if chat_response.status_code == 200:
        chat_data = chat_response.json()
        print(f"📝 Chat Response: {json.dumps(chat_data, indent=2)}")
        
        if "choices" in chat_data and len(chat_data["choices"]) > 0:
            content = chat_data["choices"][0]["message"]["content"]
            print(f"✅ Chat SUCCESS: {content}")
        else:
            print(f"⚠️ Chat response has no choices: {chat_data}")
    else:
        print(f"❌ Chat ERROR: {chat_response.status_code} - {chat_response.text}")
        
except Exception as e:
    print(f"❌ Chat Exception: {e}")

# Test 2: Image generation endpoint
print("\n=== Testing ModelsLab Image Generation ===")
image_url = "https://modelslab.com/api/v6/images/text2img"
image_headers = {
    "Content-Type": "application/json"
}

image_payload = {
    "key": API_KEY,
    "model_id": "stable-diffusion-v1-5",
    "prompt": "a cute cat",
    "negative_prompt": "blurry, low quality",
    "width": "512",
    "height": "512",
    "samples": "1",
    "num_inference_steps": "20",
    "guidance_scale": 7.5,
    "seed": None,
    "scheduler": "UniPCMultistepScheduler",
    "safety_checker": "no",
    "enhance_prompt": "yes",
    "webhook": None,
    "track_id": None
}

try:
    print(f"🖼️ Sending image generation request...")
    image_response = requests.post(image_url, headers=image_headers, json=image_payload, timeout=60)
    print(f"📊 Image Status: {image_response.status_code}")
    
    if image_response.status_code == 200:
        image_data = image_response.json()
        print(f"📝 Image Response: {json.dumps(image_data, indent=2)}")
        
        if image_data.get("status") == "success":
            output = image_data.get("output", [])
            if output:
                print(f"✅ Image SUCCESS: Generated {len(output)} image(s)")
                for i, img_url in enumerate(output):
                    print(f"   Image {i+1}: {img_url}")
            else:
                print(f"⚠️ Image success but no output: {image_data}")
        else:
            print(f"❌ Image generation failed: {image_data}")
    else:
        print(f"❌ Image ERROR: {image_response.status_code} - {image_response.text}")
        
except Exception as e:
    print(f"❌ Image Exception: {e}")

# Test 3: Model list endpoints
print("\n=== Testing ModelsLab Model List Endpoints ===")

# Test dreambooth models
print("\n--- Testing Dreambooth Models ---")
dreambooth_url = "https://modelslab.com/api/v4/dreambooth/model_list"
dreambooth_payload = {"key": API_KEY}

try:
    dreambooth_response = requests.post(dreambooth_url, json=dreambooth_payload, timeout=30)
    print(f"📊 Dreambooth Status: {dreambooth_response.status_code}")
    
    if dreambooth_response.status_code == 200:
        dreambooth_data = dreambooth_response.json()
        if dreambooth_data.get("status") == "success":
            models = dreambooth_data.get("data", [])
            print(f"✅ Dreambooth SUCCESS: Found {len(models)} models")
            if models:
                for i, model in enumerate(models[:5]):  # Show first 5
                    model_id = model.get("model_id", model.get("id", "unknown"))
                    print(f"   Model {i+1}: {model_id}")
        else:
            print(f"❌ Dreambooth failed: {dreambooth_data}")
    else:
        print(f"❌ Dreambooth ERROR: {dreambooth_response.status_code} - {dreambooth_response.text}")
        
except Exception as e:
    print(f"❌ Dreambooth Exception: {e}")

# Test public models
print("\n--- Testing Public Models ---")
public_url = "https://modelslab.com/api/v3/model_list"
public_payload = {"key": API_KEY}

try:
    public_response = requests.post(public_url, json=public_payload, timeout=30)
    print(f"📊 Public Status: {public_response.status_code}")
    
    if public_response.status_code == 200:
        public_data = public_response.json()
        if public_data.get("status") == "success":
            models = public_data.get("data", [])
            print(f"✅ Public SUCCESS: Found {len(models)} models")
            if models:
                for i, model in enumerate(models[:5]):  # Show first 5
                    if isinstance(model, dict):
                        model_id = model.get("id", model.get("name", "unknown"))
                    else:
                        model_id = str(model)
                    print(f"   Model {i+1}: {model_id}")
        else:
            print(f"❌ Public failed: {public_data}")
    else:
        print(f"❌ Public ERROR: {public_response.status_code} - {public_response.text}")
        
except Exception as e:
    print(f"❌ Public Exception: {e}")

print("\n=== Test Summary ===")
print("✅ = Success, ❌ = Error, ⚠️ = Warning")
print("If chat is working but Android app isn't, check the response parsing logic.")
print("If image generation is working, the default models should be available.") 
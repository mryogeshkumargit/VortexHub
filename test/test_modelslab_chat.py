#!/usr/bin/env python3
"""
Test script for ModelsLab uncensored chat endpoint.
"""
import os
import json
import requests

API_KEY = os.getenv("MODELSLAB_KEY")
if not API_KEY:
    print("Set MODELSLAB_KEY env var")
    exit(1)

URL = "https://modelslab.com/api/uncensored-chat/v1/chat/completions"
HEADERS = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}

payload = {
    "messages": [
        {
            "role": "system",
            "content": "You are a helpful assistant."
        },
        {
            "role": "user",
            "content": "Say hello"
        }
    ],
    "max_tokens": 100,
    "model": "ModelsLab/Llama-3.1-8b-Uncensored-Dare"
}

try:
    print(f"Testing ModelsLab chat endpoint...")
    print(f"Payload: {json.dumps(payload, indent=2)}")
    
    response = requests.post(URL, headers=HEADERS, json=payload, timeout=30)
    print(f"Status: {response.status_code}")
    print(f"Response: {response.text}")
    
    if response.status_code == 200:
        data = response.json()
        if "choices" in data and len(data["choices"]) > 0:
            content = data["choices"][0]["message"]["content"]
            print(f"SUCCESS: {content}")
        else:
            print(f"No choices in response: {data}")
    else:
        print(f"ERROR: {response.status_code} - {response.text}")
        
except Exception as e:
    print(f"Exception: {e}") 
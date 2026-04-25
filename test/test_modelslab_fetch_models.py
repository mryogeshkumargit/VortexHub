#!/usr/bin/env python3
"""
Quick utility to verify which ModelsLab model-list endpoint works with your API key.

Usage:
  $ export MODELSLAB_KEY=YOUR_KEY_HERE
  $ python test_modelslab_fetch_models.py
"""
import os
import json
import sys
import requests

API_KEY = os.getenv("MODELSLAB_KEY")
if not API_KEY:
    print("Please set environment variable MODELSLAB_KEY")
    sys.exit(1)

HEADERS = {"Content-Type": "application/json"}
PAYLOAD = json.dumps({"key": API_KEY})

def hit(url: str):
    try:
        resp = requests.post(url, headers=HEADERS, data=PAYLOAD, timeout=20)
        print(f"--- {url} -> {resp.status_code}")
        print(resp.text[:400] + ("..." if len(resp.text) > 400 else ""))
    except Exception as e:
        print(f"Error calling {url}: {e}")

print("Testing ModelsLab model endpoints...\n")

endpoints = [
    "https://modelslab.com/api/v4/dreambooth/model_list",
    "https://modelslab.com/api/v1/enterprise/get_all_models",
    "https://modelslab.com/api/v3/model_list",
    "https://modelslab.com/api/v3/finetune_list",
]

for ep in endpoints:
    hit(ep) 
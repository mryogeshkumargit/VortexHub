#!/usr/bin/env python3
"""Quick script to iterate through candidate ModelsLab model_ids and verify that
a successful generation request returns status:"success".

It sends very lightweight settings (512×512, 8 steps, 1 sample) and prints
OK or FAILED per model.

Set env var MODELSLAB_KEY with your API key.
"""
import os, json, sys, time, requests

API_KEY = os.getenv("MODELSLAB_KEY")
if not API_KEY:
    print("Set MODELSLAB_KEY env var")
    sys.exit(1)

# Helper: discover models dynamically

def fetch_model_ids(api_key: str):
    """Query several ModelsLab model-list endpoints and return a list of model_id strings.
    Stops on first endpoint that yields results. Returns [] if all fail."""
    endpoints = [
        "https://modelslab.com/api/v4/dreambooth/model_list",
        "https://modelslab.com/api/v1/enterprise/get_all_models",
        "https://modelslab.com/api/v3/model_list",
        "https://modelslab.com/api/v3/finetune_list",
    ]
    payload = json.dumps({"key": api_key})
    headers = {"Content-Type": "application/json"}
    for ep in endpoints:
        try:
            resp = requests.post(ep, headers=headers, data=payload, timeout=20)
            if resp.status_code == 200:
                data = resp.json()
                models = []
                # Common patterns – adapt to whatever structure we find
                if isinstance(data, dict):
                    # v4 dreambooth: maybe { "status": "success", "models": [ {"model_id": "..."}, ... ] }
                    if "models" in data and isinstance(data["models"], list):
                        models = [m.get("model_id") or m.get("id") or m.get("name") for m in data["models"]]
                    # enterprise/v3 may return list directly under "data"
                    if not models and "data" in data and isinstance(data["data"], list):
                        models = [m.get("model_id") or m.get("id") or m.get("name") for m in data["data"]]
                elif isinstance(data, list):
                    models = [m.get("model_id") if isinstance(m, dict) else None for m in data]
                # Filter out Nones / empties
                models = [m for m in models if m]
                if models:
                    print(f"Fetched {len(models)} model IDs from {ep}")
                    return models
        except Exception as e:
            print(f"Endpoint {ep} failed: {e}")
    return []

# Retrieve dynamic list, else fallback
CANDIDATE_MODELS = fetch_model_ids(API_KEY) or [
    "sdxl",
    "stable-diffusion-v1-5",
    "realistic-vision-v2",
    "analog-diffusion",
    "openjourney-v4",
    "flux-dev",
    "kandinsky-2-2",
    "playground-v2",
    "anything-v3",
    "dreamlike-diffusion",
    "sdxl-turbo",
    "dreamshaper-8",
    "meinamix",
    "rev-animated",
    "realistic-vision-v1-4",
]

print("Testing image generation for", len(CANDIDATE_MODELS), "models…")

URL = "https://modelslab.com/api/v6/images/text2img"
HEADERS = {"Content-Type": "application/json"}

prompt = "a red apple on white background"

for model_id in CANDIDATE_MODELS:
    payload = {
        "key": API_KEY,
        "model_id": model_id,
        "prompt": prompt,
        "width": "512",
        "height": "512",
        "samples": "1",
        "num_inference_steps": "8",
    }
    try:
        start = time.time()
        resp = requests.post(URL, headers=HEADERS, data=json.dumps(payload), timeout=30)
        ms = int((time.time() - start)*1000)
        if resp.status_code == 200:
            j = resp.json()
            status = j.get("status")
            if status == "success":
                print(f"{model_id:<25} OK   ({ms} ms)")
            else:
                print(f"{model_id:<25} FAIL status={status} msg={j.get('message')}")
        else:
            print(f"{model_id:<25} HTTP {resp.status_code}")
    except Exception as e:
        print(f"{model_id:<25} ERROR {e}") 
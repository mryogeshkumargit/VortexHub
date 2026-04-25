import requests
import json
import time
import sys

API_KEY = input("Enter your Replicate API key: ").strip()

def fetch_model_version(model_id):
    """Step 1: Fetch model version hash"""
    print(f"\n[Step 1] Fetching version for model: {model_id}")
    url = f"https://api.replicate.com/v1/models/{model_id}"
    headers = {"Authorization": f"Bearer {API_KEY}"}
    
    response = requests.get(url, headers=headers)
    print(f"Status: {response.status_code}")
    
    if response.status_code != 200:
        print(f"Error: {response.text}")
        return None
    
    data = response.json()
    version = data.get("latest_version", {}).get("id")
    print(f"Version hash: {version}")
    return version

def create_prediction(version_hash, prompt, disable_safety_checker=True):
    """Step 2-4: Create prediction with correct format"""
    print(f"\n[Step 2-4] Creating prediction")
    url = "https://api.replicate.com/v1/predictions"
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "version": version_hash,
        "input": {
            "prompt": prompt,
            "width": 1024,
            "height": 1024,
            "num_inference_steps": 20,
            "guidance_scale": 7.5,
            "disable_safety_checker": disable_safety_checker
        }
    }
    
    print(f"Request payload: {json.dumps(payload, indent=2)}")
    response = requests.post(url, headers=headers, json=payload)
    print(f"Status: {response.status_code}")
    
    if response.status_code not in [200, 201]:
        print(f"Error: {response.text}")
        return None
    
    data = response.json()
    prediction_id = data.get("id")
    print(f"Prediction ID: {prediction_id}")
    return prediction_id

def poll_prediction(prediction_id):
    """Step 5: Poll for completion"""
    print(f"\n[Step 5] Polling prediction: {prediction_id}")
    url = f"https://api.replicate.com/v1/predictions/{prediction_id}"
    headers = {"Authorization": f"Bearer {API_KEY}"}
    
    max_attempts = 60
    for attempt in range(max_attempts):
        response = requests.get(url, headers=headers)
        
        if response.status_code != 200:
            print(f"Poll error: {response.status_code}")
            return None
        
        data = response.json()
        status = data.get("status")
        print(f"Attempt {attempt + 1}/{max_attempts} - Status: {status}")
        
        if status == "succeeded":
            output = data.get("output")
            if isinstance(output, list) and len(output) > 0:
                image_url = output[0]
            elif isinstance(output, str):
                image_url = output
            else:
                image_url = None
            
            print(f"✓ Success! Image URL: {image_url}")
            return image_url
        elif status == "failed":
            error = data.get("error", "Unknown error")
            print(f"✗ Failed: {error}")
            return None
        elif status == "canceled":
            print(f"✗ Canceled")
            return None
        
        time.sleep(5)
    
    print("✗ Timeout")
    return None

def test_model(model_id, prompt="a beautiful sunset over mountains"):
    print(f"\n{'='*60}")
    print(f"Testing model: {model_id}")
    print(f"{'='*60}")
    
    # Step 1: Fetch version
    version = fetch_model_version(model_id)
    if not version:
        print("✗ Failed to fetch version")
        return False
    
    # Step 2-4: Create prediction
    prediction_id = create_prediction(version, prompt)
    if not prediction_id:
        print("✗ Failed to create prediction")
        return False
    
    # Step 5: Poll for result
    image_url = poll_prediction(prediction_id)
    if not image_url:
        print("✗ Failed to get result")
        return False
    
    print(f"\n✓ Test PASSED for {model_id}")
    return True

if __name__ == "__main__":
    if not API_KEY:
        print("Error: API key required")
        sys.exit(1)
    
    # Test a few models
    test_models = [
        "stability-ai/sdxl",
        "lucataco/ssd-1b",
        "black-forest-labs/flux-dev"
    ]
    
    results = {}
    for model in test_models:
        try:
            results[model] = test_model(model)
        except Exception as e:
            print(f"✗ Exception: {e}")
            results[model] = False
    
    print(f"\n{'='*60}")
    print("SUMMARY")
    print(f"{'='*60}")
    for model, passed in results.items():
        status = "✓ PASSED" if passed else "✗ FAILED"
        print(f"{model}: {status}")

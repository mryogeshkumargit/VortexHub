#!/usr/bin/env python3
"""
Replicate API Debug Script
Test your Replicate API key and model access directly
"""

import requests
import json
import time
import sys

def test_replicate_api(api_key, model="flux.1-schnell", prompt="a beautiful sunset over mountains"):
    """Test Replicate API with the given parameters"""
    
    print(f"🔍 Testing Replicate API...")
    print(f"📝 Model: {model}")
    print(f"💬 Prompt: {prompt}")
    print(f"🔑 API Key: {api_key[:10]}..." if api_key else "❌ No API key provided")
    print("-" * 50)
    
    if not api_key:
        print("❌ ERROR: No API key provided")
        return False
    
    # Step 1: Create prediction
    print("📤 Step 1: Creating prediction...")
    
    headers = {
        "Authorization": f"Token {api_key}",
        "Content-Type": "application/json"
    }
    
    # Different payload formats for different models
    if model in ["flux.1-dev", "flux.1-schnell"]:
        payload = {
            "model": f"black-forest-labs/{model.replace('.', '-')}",
            "input": {
                "prompt": prompt,
                "width": 1024,
                "height": 1024,
                "num_inference_steps": 20,
                "guidance_scale": 7.5
            }
        }
    elif model == "qwen-image-edit":
        payload = {
            "version": "f1d0e682b391956e6e8399320775082e4511adf1f2f0f2250d823dae5fa5ff42",
            "input": {
                "prompt": prompt
            }
        }
    elif model == "nano-banana":
        payload = {
            "version": "f0a9d34b12ad1c1cd76269a844b218ff4e64e128ddaba93e15891f47368958a0",
            "input": {
                "prompt": prompt
            }
        }
    else:
        print(f"❌ Unknown model: {model}")
        return False
    
    print(f"📋 Payload: {json.dumps(payload, indent=2)}")
    
    try:
        response = requests.post(
            "https://api.replicate.com/v1/predictions",
            headers=headers,
            json=payload,
            timeout=30
        )
        
        print(f"📊 Response Status: {response.status_code}")
        
        if response.status_code != 201:
            print(f"❌ ERROR: {response.status_code}")
            print(f"📄 Response: {response.text}")
            
            # Common error explanations
            if response.status_code == 401:
                print("🔍 DIAGNOSIS: Invalid API key")
                print("💡 SOLUTION: Check your Replicate API key in settings")
            elif response.status_code == 402:
                print("🔍 DIAGNOSIS: Insufficient credits")
                print("💡 SOLUTION: Add credits to your Replicate account")
            elif response.status_code == 404:
                print("🔍 DIAGNOSIS: Model not found")
                print("💡 SOLUTION: Check if the model exists and is accessible")
            elif response.status_code == 422:
                print("🔍 DIAGNOSIS: Invalid input parameters")
                print("💡 SOLUTION: Check the model's required input format")
            
            return False
        
        result = response.json()
        prediction_id = result.get("id")
        
        if not prediction_id:
            print("❌ ERROR: No prediction ID returned")
            print(f"📄 Response: {response.text}")
            return False
        
        print(f"✅ Prediction created: {prediction_id}")
        
        # Step 2: Poll for completion
        print("⏳ Step 2: Polling for completion...")
        
        max_attempts = 60  # 5 minutes
        attempt = 0
        
        while attempt < max_attempts:
            attempt += 1
            print(f"🔄 Poll attempt {attempt}/{max_attempts}")
            
            try:
                poll_response = requests.get(
                    f"https://api.replicate.com/v1/predictions/{prediction_id}",
                    headers=headers,
                    timeout=10
                )
                
                if poll_response.status_code != 200:
                    print(f"❌ Poll error: {poll_response.status_code}")
                    print(f"📄 Response: {poll_response.text}")
                    return False
                
                poll_result = poll_response.json()
                status = poll_result.get("status")
                
                print(f"📊 Status: {status}")
                
                if status == "succeeded":
                    output = poll_result.get("output")
                    print(f"✅ SUCCESS!")
                    print(f"🖼️ Output: {output}")
                    
                    # Extract image URL
                    if isinstance(output, list) and len(output) > 0:
                        image_url = output[0]
                        print(f"🔗 Image URL: {image_url}")
                    elif isinstance(output, str):
                        print(f"🔗 Image URL: {output}")
                    else:
                        print(f"⚠️ Unexpected output format: {type(output)}")
                    
                    return True
                
                elif status == "failed":
                    error = poll_result.get("error", "Unknown error")
                    logs = poll_result.get("logs", "")
                    print(f"❌ FAILED: {error}")
                    if logs:
                        print(f"📋 Logs: {logs}")
                    return False
                
                elif status == "canceled":
                    print("❌ CANCELED")
                    return False
                
                elif status in ["starting", "processing"]:
                    print(f"⏳ {status.upper()}... waiting 5 seconds")
                    time.sleep(5)
                
                else:
                    print(f"❓ Unknown status: {status}")
                    time.sleep(5)
                
            except requests.exceptions.RequestException as e:
                print(f"❌ Poll request error: {e}")
                time.sleep(5)
        
        print("⏰ TIMEOUT: Prediction took too long")
        return False
        
    except requests.exceptions.RequestException as e:
        print(f"❌ Request error: {e}")
        return False

def main():
    print("🚀 Replicate API Debug Tool")
    print("=" * 50)
    
    # Get API key
    api_key = input("🔑 Enter your Replicate API key: ").strip()
    
    if not api_key:
        print("❌ No API key provided. Exiting.")
        return
    
    # Test different models
    models_to_test = [
        "flux.1-schnell",  # Fast FLUX model
        "flux.1-dev",      # High-quality FLUX model
        "nano-banana",     # Simple test model
    ]
    
    print(f"\n🧪 Testing {len(models_to_test)} models...")
    
    results = {}
    
    for model in models_to_test:
        print(f"\n{'='*20} {model.upper()} {'='*20}")
        success = test_replicate_api(api_key, model)
        results[model] = success
        
        if not success:
            print(f"❌ {model} failed")
        else:
            print(f"✅ {model} succeeded")
    
    # Summary
    print(f"\n{'='*50}")
    print("📊 SUMMARY")
    print(f"{'='*50}")
    
    for model, success in results.items():
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{model:20} {status}")
    
    successful_models = [model for model, success in results.items() if success]
    
    if successful_models:
        print(f"\n🎉 SUCCESS! Working models: {', '.join(successful_models)}")
        print("💡 Use any of these models in your Android app")
    else:
        print(f"\n❌ NO MODELS WORKING")
        print("🔍 Check your API key and account status")
        print("💡 Visit https://replicate.com/account to verify your account")

if __name__ == "__main__":
    main()
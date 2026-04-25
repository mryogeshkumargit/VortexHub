#!/usr/bin/env python3
"""
Test script to verify Replicate API fixes for qwen-image-edit
"""

import requests
import json
import time
import base64

# Configuration
REPLICATE_API_TOKEN = "your-replicate-api-token-here"  # Replace with your actual token
BASE_URL = "https://api.replicate.com/v1"

def test_qwen_image_edit_fixed():
    """Test the fixed qwen-image-edit implementation"""
    
    if REPLICATE_API_TOKEN == "your-replicate-api-token-here":
        print("❌ Please set your REPLICATE_API_TOKEN in the script")
        return False
    
    print("🧪 Testing Fixed Qwen Image Edit Implementation...")
    
    # Test 1: Correct API format with version hash
    print("\n📋 Test 1: Correct API format")
    
    # Sample image URL
    sample_image_url = "https://replicate.delivery/pbxt/NYfZuQXicUlwvUfeNmX2IgEfCz7vzuVrXitm9pgXVm1RBIJO/image.png"
    
    input_data = {
        "image": sample_image_url,
        "prompt": "Change the sweater to be blue with white text",
        "go_fast": True,
        "output_format": "webp",
        "enhance_prompt": False,
        "output_quality": 80
    }
    
    payload = {
        "version": "f1d0e682b391956e6e8399320775082e4511adf1f2f2250d823dae5fa5ff42",
        "input": input_data
    }
    
    headers = {
        "Authorization": f"Token {REPLICATE_API_TOKEN}",
        "Content-Type": "application/json"
    }
    
    print(f"📤 Request URL: {BASE_URL}/predictions")
    print(f"📋 Payload: {json.dumps(payload, indent=2)}")
    
    try:
        response = requests.post(
            f"{BASE_URL}/predictions",
            headers=headers,
            json=payload,
            timeout=30
        )
        
        print(f"📊 Response Code: {response.status_code}")
        
        if response.status_code == 201:
            prediction_data = response.json()
            prediction_id = prediction_data.get("id")
            print(f"✅ Success! Prediction ID: {prediction_id}")
            
            # Test polling
            print("\n⏳ Testing prediction polling...")
            success = poll_prediction(prediction_id, headers)
            return success
        else:
            print(f"❌ Failed: {response.status_code}")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def test_base64_image():
    """Test with base64 encoded image"""
    print("\n📋 Test 2: Base64 image format")
    
    # Create a simple test image (1x1 pixel PNG)
    test_image_base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChAI9jU77zgAAAABJRU5ErkJggg=="
    
    input_data = {
        "image": f"data:image/png;base64,{test_image_base64}",
        "prompt": "Make this a red pixel",
        "go_fast": True,
        "output_format": "webp",
        "enhance_prompt": False,
        "output_quality": 80
    }
    
    payload = {
        "version": "f1d0e682b391956e6e8399320775082e4511adf1f2f2250d823dae5fa5ff42",
        "input": input_data
    }
    
    headers = {
        "Authorization": f"Token {REPLICATE_API_TOKEN}",
        "Content-Type": "application/json"
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/predictions",
            headers=headers,
            json=payload,
            timeout=30
        )
        
        print(f"📊 Base64 Response: {response.status_code}")
        
        if response.status_code == 201:
            prediction_data = response.json()
            print(f"✅ Base64 accepted: {prediction_data.get('id')}")
            return True
        else:
            print(f"❌ Base64 rejected: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Base64 error: {e}")
        return False

def poll_prediction(prediction_id, headers):
    """Poll prediction for completion"""
    max_attempts = 30
    attempt = 0
    
    while attempt < max_attempts:
        time.sleep(5)
        attempt += 1
        
        try:
            response = requests.get(
                f"{BASE_URL}/predictions/{prediction_id}",
                headers=headers,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                status = data.get("status", "unknown")
                
                print(f"📊 Attempt {attempt}/{max_attempts} - Status: {status}")
                
                if status == "succeeded":
                    output = data.get("output")
                    if output:
                        if isinstance(output, list) and len(output) > 0:
                            image_url = output[0]
                        elif isinstance(output, str):
                            image_url = output
                        else:
                            print("❌ Unexpected output format")
                            return False
                        
                        print(f"✅ Image editing completed!")
                        print(f"🖼️  Result: {image_url}")
                        return True
                
                elif status == "failed":
                    error = data.get("error", "Unknown error")
                    print(f"❌ Failed: {error}")
                    return False
                
                elif status in ["starting", "processing"]:
                    continue
                
                else:
                    print(f"❓ Unknown status: {status}")
            
            else:
                print(f"❌ Poll failed: {response.status_code}")
                
        except Exception as e:
            print(f"❌ Poll error: {e}")
    
    print("⏰ Polling timed out")
    return False

def main():
    """Main test function"""
    print("🚀 Replicate Qwen Image Edit Fix Verification")
    print("=" * 60)
    
    # Test 1: Fixed API format
    success1 = test_qwen_image_edit_fixed()
    
    # Test 2: Base64 image
    success2 = test_base64_image()
    
    print("\n" + "=" * 60)
    print("📊 Test Results:")
    print(f"Fixed API format: {'✅ PASS' if success1 else '❌ FAIL'}")
    print(f"Base64 image: {'✅ PASS' if success2 else '❌ FAIL'}")
    
    if success1 and success2:
        print("\n🎉 All tests passed! The Replicate API fixes are working correctly.")
        print("💡 Your Android app should now work properly with qwen-image-edit.")
    else:
        print("\n❌ Some tests failed. Check the error messages above.")
        print("🔧 Verify your API key and network connection.")

if __name__ == "__main__":
    main()

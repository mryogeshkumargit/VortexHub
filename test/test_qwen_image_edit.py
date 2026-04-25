#!/usr/bin/env python3
"""
Test script for Qwen/Qwen-Image-Edit functionality via Replicate API
"""

import requests
import json
import time
import base64
import os

# Configuration
REPLICATE_API_TOKEN = "your-replicate-api-token-here"  # Replace with your actual token
BASE_URL = "https://api.replicate.com/v1"

def test_qwen_image_edit():
    """Test Qwen image editing functionality"""
    
    # Check if API token is set
    if REPLICATE_API_TOKEN == "your-replicate-api-token-here":
        print("❌ Please set your REPLICATE_API_TOKEN in the script")
        return False
    
    print("🧪 Testing Qwen/Qwen-Image-Edit functionality...")
    
    # Sample image URL (you can replace with your own)
    sample_image_url = "https://replicate.delivery/pbxt/NYfZuQXicUlwvUfeNmX2IgEfCz7vzuVrXitm9pgXVm1RBIJO/image.png"
    
    # Test input
    input_data = {
        "image": sample_image_url,
        "prompt": "Change the sweater to be blue with white text",
        "go_fast": True,
        "output_format": "webp",
        "enhance_prompt": False,
        "output_quality": 80
    }
    
    # Create prediction
    print("📤 Creating Qwen image editing prediction...")
    
    headers = {
        "Authorization": f"Token {REPLICATE_API_TOKEN}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "version": "f1d0e682b391956e6e8399320775082e4511adf1f2f2250d823dae5fa5ff42",
        "input": input_data
    }
    
    try:
        # Step 1: Create prediction
        response = requests.post(
            f"{BASE_URL}/predictions",
            headers=headers,
            json=payload,
            timeout=30
        )
        
        print(f"📊 Create prediction response: {response.status_code}")
        
        if response.status_code != 201:
            print(f"❌ Failed to create prediction: {response.status_code}")
            print(f"Response: {response.text}")
            return False
        
        prediction_data = response.json()
        prediction_id = prediction_data.get("id")
        
        if not prediction_id:
            print("❌ No prediction ID returned")
            return False
        
        print(f"✅ Prediction created: {prediction_id}")
        print(f"📋 Initial status: {prediction_data.get('status', 'unknown')}")
        
        # Step 2: Poll for completion
        print("⏳ Polling for completion...")
        
        max_attempts = 60  # 5 minutes max
        attempt = 0
        
        while attempt < max_attempts:
            time.sleep(5)  # Wait 5 seconds between polls
            attempt += 1
            
            # Get prediction status
            poll_response = requests.get(
                f"{BASE_URL}/predictions/{prediction_id}",
                headers=headers,
                timeout=30
            )
            
            if poll_response.status_code != 200:
                print(f"❌ Failed to poll prediction: {poll_response.status_code}")
                continue
            
            poll_data = poll_response.json()
            status = poll_data.get("status", "unknown")
            
            print(f"📊 Attempt {attempt}/{max_attempts} - Status: {status}")
            
            if status == "succeeded":
                output = poll_data.get("output")
                if output:
                    if isinstance(output, list) and len(output) > 0:
                        image_url = output[0]
                    elif isinstance(output, str):
                        image_url = output
                    else:
                        print("❌ Unexpected output format")
                        return False
                    
                    print(f"✅ Image editing completed successfully!")
                    print(f"🖼️  Edited image URL: {image_url}")
                    
                    # Get generation info
                    generation_time = poll_data.get("metrics", {}).get("predict_time", 0)
                    print(f"⏱️  Generation time: {generation_time:.2f} seconds")
                    
                    return True
                else:
                    print("❌ No output in successful prediction")
                    return False
            
            elif status == "failed":
                error = poll_data.get("error", "Unknown error")
                logs = poll_data.get("logs", "")
                print(f"❌ Prediction failed: {error}")
                if logs:
                    print(f"📝 Logs: {logs}")
                return False
            
            elif status == "canceled":
                print("❌ Prediction was canceled")
                return False
            
            elif status in ["starting", "processing"]:
                continue  # Keep polling
            
            else:
                print(f"❓ Unknown status: {status}")
        
        print("⏰ Prediction timed out")
        return False
        
    except requests.exceptions.RequestException as e:
        print(f"❌ Network error: {e}")
        return False
    except json.JSONDecodeError as e:
        print(f"❌ JSON decode error: {e}")
        return False
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        return False

def test_with_base64_image():
    """Test with base64 encoded image"""
    
    print("\n🧪 Testing with base64 image...")
    
    # Create a simple test image (1x1 pixel PNG)
    # This is a minimal PNG file in base64
    test_image_base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChAI9jU77zgAAAABJRU5ErkJggg=="
    
    input_data = {
        "image": f"data:image/png;base64,{test_image_base64}",
        "prompt": "Make this a red pixel",
        "go_fast": True,
        "output_format": "webp",
        "enhance_prompt": False,
        "output_quality": 80
    }
    
    headers = {
        "Authorization": f"Token {REPLICATE_API_TOKEN}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "version": "f1d0e682b391956e6e8399320775082e4511adf1f2f2250d823dae5fa5ff42",
        "input": input_data
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/predictions",
            headers=headers,
            json=payload,
            timeout=30
        )
        
        print(f"📊 Base64 test response: {response.status_code}")
        
        if response.status_code == 201:
            prediction_data = response.json()
            print(f"✅ Base64 image accepted: {prediction_data.get('id')}")
            return True
        else:
            print(f"❌ Base64 image rejected: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Base64 test error: {e}")
        return False

def main():
    """Main test function"""
    print("🚀 Qwen Image Edit API Test")
    print("=" * 50)
    
    # Test 1: URL-based image editing
    success1 = test_qwen_image_edit()
    
    # Test 2: Base64 image editing
    success2 = test_with_base64_image()
    
    print("\n" + "=" * 50)
    print("📊 Test Results:")
    print(f"URL-based editing: {'✅ PASS' if success1 else '❌ FAIL'}")
    print(f"Base64 editing: {'✅ PASS' if success2 else '❌ FAIL'}")
    
    if success1 or success2:
        print("\n🎉 Qwen image editing is working!")
        print("💡 You can now use image editing in your Android app")
    else:
        print("\n❌ Qwen image editing tests failed")
        print("🔧 Check your API key and network connection")

if __name__ == "__main__":
    main()
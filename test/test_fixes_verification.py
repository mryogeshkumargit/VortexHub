#!/usr/bin/env python3
"""
Test script to verify Replicate API and Image Settings fixes
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
    
    # Sample image URL for testing
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
            return True
        else:
            print(f"❌ Failed: {response.status_code}")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    """Main test function"""
    print("🚀 Replicate API & Image Settings Fix Verification")
    print("=" * 60)
    
    # Test fixed API format
    success = test_qwen_image_edit_fixed()
    
    print("\n" + "=" * 60)
    print("📊 Test Results:")
    print(f"Fixed API format: {'✅ PASS' if success else '❌ FAIL'}")
    
    if success:
        print("\n🎉 Replicate API fixes are working correctly!")
        print("💡 Your Android app should now work properly with qwen-image-edit.")
    else:
        print("\n❌ Test failed. Check the error messages above.")
        print("🔧 Verify your API key and network connection.")

if __name__ == "__main__":
    main()
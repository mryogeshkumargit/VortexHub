#!/usr/bin/env python3
"""
Test Together AI image-to-image functionality with FLUX kontext models
"""
import requests
import json
import base64

def test_together_ai_image_edit():
    """Test Together AI image-to-image API with FLUX kontext models"""
    
    # API configuration
    api_key = ""  # Replace with actual key
    base_url = "https://api.together.xyz/v1"
    
    # Test models
    models = [
        "black-forest-labs/FLUX.1-kontext-dev",
        "black-forest-labs/FLUX.1-kontext-pro", 
        "black-forest-labs/FLUX.1-kontext-max"
    ]
    
    # Create a simple test image (1x1 pixel base64 encoded)
    test_image_base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
    test_image_data_url = f"data:image/png;base64,{test_image_base64}"
    
    print("Testing Together AI Image-to-Image API")
    print(f"Base URL: {base_url}")
    print(f"API Key: {api_key[:12]}...")
    print()
    
    for model in models:
        print(f"Testing model: {model}")
        
        # Request payload for image-to-image
        payload = {
            "model": model,
            "prompt": "A beautiful sunset over mountains",
            "image_url": test_image_data_url,
            "strength": 0.5,
            "width": 1024,
            "height": 1024,
            "steps": 20,
            "n": 1
        }
        
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        
        try:
            print(f"  Sending request...")
            response = requests.post(
                f"{base_url}/images/generations",
                headers=headers,
                json=payload,
                timeout=30
            )
            
            print(f"  Response status: {response.status_code}")
            
            if response.status_code == 200:
                data = response.json()
                print(f"  SUCCESS! Response keys: {list(data.keys())}")
                
                if "data" in data and len(data["data"]) > 0:
                    image_url = data["data"][0].get("url", "No URL")
                    print(f"  Generated image URL: {image_url[:50]}...")
                else:
                    print(f"  WARNING: No image data in response")
                    
            else:
                error_text = response.text[:200]
                print(f"  ERROR {response.status_code}: {error_text}...")
                
                # Check for specific error types
                if response.status_code == 401:
                    print(f"  Invalid API key")
                elif response.status_code == 422:
                    print(f"  Invalid parameters - model might not support image-to-image")
                elif response.status_code == 404:
                    print(f"  Model not found or not available")
                    
        except requests.exceptions.Timeout:
            print(f"  Request timed out")
        except requests.exceptions.RequestException as e:
            print(f"  Request failed: {e}")
        except Exception as e:
            print(f"  Unexpected error: {e}")
            
        print()

def test_together_ai_models_list():
    """Test fetching available models from Together AI"""
    
    api_key = ""
    
    print("Fetching Together AI models...")
    
    headers = {
        "Authorization": f"Bearer {api_key}"
    }
    
    try:
        response = requests.get(
            "https://api.together.xyz/v1/models",
            headers=headers,
            timeout=10
        )
        
        if response.status_code == 200:
            data = response.json()
            models = data.get("data", [])
            
            # Filter for FLUX kontext models
            flux_kontext_models = [
                model for model in models 
                if "flux" in model.get("id", "").lower() and "kontext" in model.get("id", "").lower()
            ]
            
            print(f"Found {len(flux_kontext_models)} FLUX kontext models:")
            for model in flux_kontext_models:
                model_id = model.get("id", "")
                model_type = model.get("type", "")
                print(f"  - {model_id} (type: {model_type})")
                
            # Also check for any FLUX models
            flux_models = [
                model for model in models 
                if "flux" in model.get("id", "").lower()
            ]
            
            print(f"\nAll FLUX models found ({len(flux_models)}):")
            for model in flux_models[:10]:  # Show first 10
                model_id = model.get("id", "")
                print(f"  - {model_id}")
                
        else:
            print(f"Failed to fetch models: {response.status_code}")
            print(f"Response: {response.text[:200]}...")
            
    except Exception as e:
        print(f"Error fetching models: {e}")

if __name__ == "__main__":
    test_together_ai_models_list()
    print("\n" + "="*50 + "\n")
    test_together_ai_image_edit()

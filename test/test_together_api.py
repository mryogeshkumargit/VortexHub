#!/usr/bin/env python3
"""
Quick test to verify Together AI API is working
Run this with your API key to see what the API actually returns
"""
import requests
import json

def test_together_ai_api(api_key):
    """Test the Together AI models API endpoint"""
    
    if not api_key or api_key.startswith('your-api-key'):
        print("❌ Please set a valid Together AI API key")
        return
    
    url = "https://api.together.xyz/v1/models"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    print(f"🔍 Testing Together AI API...")
    print(f"URL: {url}")
    print(f"API Key: {api_key[:8]}...")
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            models = data.get('data', [])
            print(f"✅ API SUCCESS - Total models returned: {len(models)}")
            
            # Count different model types
            llm_models = []
            image_models = []
            other_models = []
            
            for model in models:
                model_id = model.get('id', '')
                model_type = model.get('type', '')
                
                # LLM filtering logic (from your Android code)
                is_llm = not any(keyword in model_id.lower() for keyword in [
                    'embedding', 'rerank', 'stable-diffusion', 'flux', 
                    'whisper', 'tts', 'speech'
                ])
                
                # Image filtering logic  
                is_image = any(keyword in model_id.lower() for keyword in [
                    'flux', 'stable-diffusion', 'sdxl', 'diffusion',
                    'dall-e', 'midjourney', 'playground'
                ]) or 'image' in model_type.lower()
                
                if is_llm and model_id:
                    llm_models.append(model_id)
                elif is_image and model_id:
                    image_models.append(model_id)
                else:
                    other_models.append(model_id)
            
            print(f"📊 Model breakdown:")
            print(f"   LLM Models: {len(llm_models)}")
            print(f"   Image Models: {len(image_models)}")
            print(f"   Other Models: {len(other_models)}")
            
            print(f"\n🤖 First 10 LLM models:")
            for i, model in enumerate(llm_models[:10]):
                print(f"   {i+1}. {model}")
            
            print(f"\n🎨 First 10 Image models:")
            for i, model in enumerate(image_models[:10]):
                print(f"   {i+1}. {model}")
                
            # Check if this matches what your app should see
            if len(llm_models) > 8:
                print(f"\n✅ LLM API working - should see {len(llm_models)} models, not 8!")
            else:
                print(f"\n⚠️  Only {len(llm_models)} LLM models found")
                
            if len(image_models) > 8:
                print(f"✅ Image API working - should see {len(image_models)} models, not 8!")
            else:
                print(f"⚠️  Only {len(image_models)} image models found")
                
        elif response.status_code == 401:
            print("❌ Authentication failed - Invalid API key")
        elif response.status_code == 403:
            print("❌ Access denied - API key doesn't have permissions")
        elif response.status_code == 429:
            print("❌ Rate limit exceeded")
        else:
            print(f"❌ API Error: {response.status_code}")
            print(f"Response: {response.text[:200]}...")
            
    except requests.exceptions.Timeout:
        print("❌ API call timed out")
    except requests.exceptions.ConnectionError:
        print("❌ Connection error - check internet connection")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    # Replace with your actual API key
    api_key = "your-api-key-here"
    
    print("🚀 Together AI API Tester")
    print("=" * 50)
    
    if api_key == "your-api-key-here":
        print("Please edit this file and set your Together AI API key")
        print("Get it from: https://api.together.xyz/settings/api-keys")
    else:
        test_together_ai_api(api_key)
#!/usr/bin/env python3
import requests
import json

def test_together_ai_api():
    api_key = '89c6de4fade1bb8754bf040204388c6df586aff83d48fe9385280dd7c576df50'
    url = 'https://api.together.xyz/v1/models'
    headers = {'Authorization': f'Bearer {api_key}'}

    print('🔍 Testing Together AI API...')
    print(f'URL: {url}')
    print(f'API Key: {api_key[:12]}...')

    try:
        response = requests.get(url, headers=headers, timeout=10)
        print(f'Status Code: {response.status_code}')
        
        if response.status_code == 200:
            data = response.json()
            print(f'Raw response keys: {list(data.keys()) if isinstance(data, dict) else "Not a dict"}')
            print(f'Raw response preview: {str(data)[:300]}...')
            
            models = data.get('data', []) if isinstance(data, dict) else []
            if not models:
                models = data if isinstance(data, list) else []
                
            print(f'✅ Total models returned: {len(models)}')
            
            # Analyze model types
            llm_models = []
            image_models = []
            audio_models = []
            
            print(f'\n📋 First 10 models structure:')
            for i, model in enumerate(models[:10]):
                model_id = model.get('id', '')
                model_type = model.get('type', '')
                display_name = model.get('display_name', '')
                
                print(f'{i+1}. ID: {model_id}')
                print(f'   Type: {model_type}')
                print(f'   Display Name: {display_name}')
                print(f'   Full JSON: {json.dumps(model, indent=2)[:200]}...')
                print()
                
                # Categorize models using app's logic
                is_llm = not any(keyword in model_id.lower() for keyword in [
                    'embedding', 'rerank', 'stable-diffusion', 'flux', 
                    'whisper', 'tts', 'speech'
                ])
                
                is_image = any(keyword in model_id.lower() for keyword in [
                    'flux', 'stable-diffusion', 'sdxl', 'diffusion',
                    'dall-e', 'midjourney', 'playground'
                ]) or 'image' in model_type.lower()
                
                is_audio = any(keyword in model_id.lower() for keyword in [
                    'whisper', 'tts', 'speech', 'audio'
                ])
                
                if is_llm and model_id:
                    llm_models.append(model_id)
                if is_image and model_id:
                    image_models.append(model_id)
                if is_audio and model_id:
                    audio_models.append(model_id)
            
            print(f'\n📊 Model Analysis (using app filtering logic):')
            print(f'Total models: {len(models)}')
            print(f'LLM models: {len(llm_models)}')
            print(f'Image models: {len(image_models)}')
            print(f'Audio models: {len(audio_models)}')
            
            print(f'\n🤖 LLM models found:')
            for model in llm_models[:15]:  # Show first 15
                print(f'  - {model}')
            
            print(f'\n🎨 Image models found:')
            for model in image_models[:15]:  # Show first 15
                print(f'  - {model}')
                
            print(f'\n🎵 Audio models found:')
            for model in audio_models[:15]:  # Show first 15
                print(f'  - {model}')
                
        else:
            print(f'❌ Error: {response.status_code}')
            print(f'Response: {response.text[:500]}')
            
    except Exception as e:
        print(f'❌ Exception: {e}')

if __name__ == '__main__':
    test_together_ai_api()
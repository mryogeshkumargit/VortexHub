#!/usr/bin/env python3
import requests
import json

def simulate_android_app_logic():
    """Simulate exactly what the Android app should now do"""
    
    api_key = ''
    response = requests.get('https://api.together.xyz/v1/models', headers={'Authorization': f'Bearer {api_key}'})
    
    print('🔍 SIMULATING ANDROID APP BEHAVIOR (AFTER FIX)')
    print('=' * 60)
    
    # Parse as direct array (like the fixed Android code)
    try:
        models_array = response.json()  # Direct array parsing
        print(f'✅ Parsed as direct array: {len(models_array)} models')
    except:
        print('❌ Failed to parse as direct array')
        return
    
    # LLM filtering (like the fixed Android code)
    llm_models = []
    image_models = []
    audio_models = []
    
    for model in models_array:
        model_id = model.get('id', '')
        model_type = model.get('type', '')
        display_name = model.get('display_name', model_id)
        
        # LLM filtering: type == "chat" or "language"
        if model_type.lower() in ['chat', 'language'] and model_id:
            llm_models.append({
                'id': model_id,
                'name': display_name,
                'type': model_type
            })
            
        # Image filtering: type == "image"
        elif model_type.lower() == 'image' and model_id:
            image_models.append({
                'id': model_id,
                'name': display_name,
                'type': model_type
            })
            
        # Audio filtering: type == "audio"
        elif model_type.lower() == 'audio' and model_id:
            audio_models.append({
                'id': model_id,
                'name': display_name,
                'type': model_type
            })
    
    print(f'\n📊 RESULTS (what Android app should now show):')
    print(f'LLM Models: {len(llm_models)} (was 8, should be ~62)')
    print(f'Image Models: {len(image_models)} (was 8, should be ~13)')
    print(f'Audio Models: {len(audio_models)} (should be ~2)')
    
    print(f'\n🤖 First 10 LLM models:')
    for i, model in enumerate(llm_models[:10]):
        print(f'  {i+1:2d}. {model["id"]} ({model["name"]})')
        
    print(f'\n🎨 All Image models:')
    for i, model in enumerate(image_models):
        print(f'  {i+1:2d}. {model["id"]} ({model["name"]})')
        
    print(f'\n🎵 All Audio models:')
    for i, model in enumerate(audio_models):
        print(f'  {i+1:2d}. {model["id"]} ({model["name"]})')
        
    print(f'\n✅ SUCCESS INDICATORS:')
    if len(llm_models) >= 60:
        print(f'  ✅ LLM models: {len(llm_models)} (FIXED - was only 8)')
    else:
        print(f'  ❌ LLM models: {len(llm_models)} (still wrong)')
        
    if len(image_models) >= 10:
        print(f'  ✅ Image models: {len(image_models)} (FIXED - was only 8)')
    else:
        print(f'  ❌ Image models: {len(image_models)} (still wrong)')
        
    if len(audio_models) >= 2:
        print(f'  ✅ Audio models: {len(audio_models)} (GOOD)')
    else:
        print(f'  ❌ Audio models: {len(audio_models)} (wrong)')

if __name__ == '__main__':
    simulate_android_app_logic()

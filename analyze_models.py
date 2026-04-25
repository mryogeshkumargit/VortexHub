#!/usr/bin/env python3
import requests

api_key = ''
response = requests.get('https://api.together.xyz/v1/models', headers={'Authorization': f'Bearer {api_key}'})
models = response.json()

print('🔍 DETAILED ANALYSIS OF TOGETHER AI MODELS')
print('=' * 60)

# Count by type
types = {}
for model in models:
    model_type = model.get('type', 'unknown')
    types[model_type] = types.get(model_type, 0) + 1

print('📊 Models by type:')
for t, count in sorted(types.items()):
    print(f'  {t}: {count} models')

print('\n🤖 Chat/LLM models:')
chat_models = [m for m in models if m.get('type') == 'chat']
for i, model in enumerate(chat_models[:25]):  # First 25
    print(f'  {i+1:2d}. {model.get("id", "N/A")}')

print(f'\n🎨 Image models:')
image_models = [m for m in models if m.get('type') == 'image']
for i, model in enumerate(image_models[:25]):  # First 25
    print(f'  {i+1:2d}. {model.get("id", "N/A")}')

print(f'\n🎵 Audio models:')
audio_models = [m for m in models if m.get('type') == 'audio']
for i, model in enumerate(audio_models[:25]):  # First 25
    print(f'  {i+1:2d}. {model.get("id", "N/A")}')

print(f'\n📈 SUMMARY:')
print(f'Total models: {len(models)}')
print(f'Chat models: {len(chat_models)}')
print(f'Image models: {len(image_models)}')
print(f'Audio models: {len(audio_models)}')

# Test the app's filtering logic
print(f'\n🔍 TESTING APP\'S FILTERING LOGIC:')
print('-' * 40)

llm_count_app_logic = 0
for model in models:
    model_id = model.get('id', '')
    
    # App's LLM filtering logic
    is_llm = not any(keyword in model_id.lower() for keyword in [
        'embedding', 'rerank', 'stable-diffusion', 'flux', 
        'whisper', 'tts', 'speech'
    ])
    
    if is_llm and model_id:
        llm_count_app_logic += 1

print(f'LLM models using app logic: {llm_count_app_logic}')
print(f'Chat models by type field: {len(chat_models)}')
print(f'Difference: {len(chat_models) - llm_count_app_logic}')

print(f'\n❌ Models excluded by app logic but are actually chat:')
for model in chat_models:
    model_id = model.get('id', '')
    
    # App's filtering logic
    is_llm = not any(keyword in model_id.lower() for keyword in [
        'embedding', 'rerank', 'stable-diffusion', 'flux', 
        'whisper', 'tts', 'speech'
    ])
    
    if not is_llm:
        excluded_reason = 'unknown'
        if 'embedding' in model_id.lower():
            excluded_reason = 'embedding'
        elif 'rerank' in model_id.lower():
            excluded_reason = 'rerank'
        elif 'stable-diffusion' in model_id.lower():
            excluded_reason = 'stable-diffusion'
        elif 'flux' in model_id.lower():
            excluded_reason = 'flux'
        elif 'whisper' in model_id.lower():
            excluded_reason = 'whisper'
        elif 'tts' in model_id.lower():
            excluded_reason = 'tts'
        elif 'speech' in model_id.lower():
            excluded_reason = 'speech'
            
        print(f'  - {model_id} (excluded as: {excluded_reason})')

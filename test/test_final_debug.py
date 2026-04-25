#!/usr/bin/env python3
import requests
import json

def test_android_exact_logic():
    """Test the exact logic the Android app should be using"""
    
    api_key = '89c6de4fade1bb8754bf040204388c6df586aff83d48fe9385280dd7c576df50'
    
    print('🔍 TESTING EXACT ANDROID APP LOGIC')
    print('=' * 60)
    
    # Test 1: LLM Models
    print('\n1️⃣ TESTING LLM MODELS FETCHING...')
    try:
        response = requests.get('https://api.together.xyz/v1/models', 
                              headers={'Authorization': f'Bearer {api_key}'})
        print(f'Status Code: {response.status_code}')
        
        if response.status_code == 200:
            # Try parsing as direct array (like fixed Android code)
            try:
                models_array = response.json()
                print(f'✅ Parsed as direct array: {len(models_array)} models')
                
                # Simulate Android filtering logic
                llm_models = []
                total_models = 0
                excluded_models = 0
                
                for model in models_array:
                    model_id = model.get('id', '')
                    model_type = model.get('type', '')
                    display_name = model.get('display_name', model_id)
                    total_models += 1
                    
                    # Use the type field for filtering (like fixed Android code)
                    is_llm_model = model_type.lower() in ['chat', 'language']
                    
                    if is_llm_model and model_id:
                        llm_models.append({
                            'id': model_id,
                            'name': display_name,
                            'type': model_type
                        })
                        print(f'✅ Included: {model_id} (type: {model_type})')
                    else:
                        excluded_models += 1
                        if total_models <= 10:  # Only show first 10 exclusions
                            reason = f'not chat/language type (type: {model_type})' if model_type else 'unknown type'
                            print(f'❌ Excluded: {model_id} ({reason})')
                
                print(f'\n📊 LLM FILTERING RESULTS:')
                print(f'Total models: {total_models}')
                print(f'Included: {len(llm_models)}')
                print(f'Excluded: {excluded_models}')
                
                if len(llm_models) >= 60:
                    print(f'✅ LLM SUCCESS: {len(llm_models)} models (should NOT be 8)')
                else:
                    print(f'❌ LLM PROBLEM: Only {len(llm_models)} models')
                
            except json.JSONDecodeError as e:
                print(f'❌ JSON parsing failed: {e}')
                
        else:
            print(f'❌ API call failed: {response.status_code}')
            print(f'Response: {response.text[:200]}')
            
    except Exception as e:
        print(f'❌ Exception in LLM test: {e}')
    
    # Test 2: Image Models  
    print('\n2️⃣ TESTING IMAGE MODELS FETCHING...')
    try:
        response = requests.get('https://api.together.xyz/v1/models', 
                              headers={'Authorization': f'Bearer {api_key}'})
        
        if response.status_code == 200:
            models_array = response.json()
            
            # Simulate Android image filtering logic
            image_models = []
            
            for model in models_array:
                model_id = model.get('id', '')
                model_type = model.get('type', '')
                
                # Use the type field for filtering (like fixed Android code)
                is_image_model = model_type.lower() == 'image'
                
                if is_image_model and model_id:
                    image_models.append(model_id)
                    print(f'✅ Included image: {model_id} (type: {model_type})')
            
            print(f'\n📊 IMAGE FILTERING RESULTS:')
            print(f'Image models found: {len(image_models)}')
            
            if len(image_models) >= 10:
                print(f'✅ IMAGE SUCCESS: {len(image_models)} models (should NOT be 8)')
            else:
                print(f'❌ IMAGE PROBLEM: Only {len(image_models)} models')
                
    except Exception as e:
        print(f'❌ Exception in Image test: {e}')
    
    # Test 3: Check if getting defaults
    print('\n3️⃣ CHECKING DEFAULT MODEL COUNTS...')
    print('If Android shows exactly these counts, it means API calls are failing:')
    print('  - LLM models: 8 (default hardcoded)')
    print('  - Image models: 8 (default hardcoded)')
    print('  - Expected real counts: 63 LLM, 13 Image')

if __name__ == '__main__':
    test_android_exact_logic()
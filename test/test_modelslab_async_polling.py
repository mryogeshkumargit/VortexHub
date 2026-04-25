#!/usr/bin/env python3
"""
Automated test for ModelsLab Image Editing API async polling functionality
Tests all 4 models to verify async detection and polling works correctly
"""

import requests
import json
import time
import base64
import io
from PIL import Image
import os

class ModelsLabAsyncTester:
    def __init__(self):
        self.api_key = ""
        self.imgbb_api_key = ""
        self.load_env()
        
        # Test models
        self.models = [
            "flux-kontext-dev",    # v6 API
            "flux-kontext-pro",    # v7 API
            "seedream-4",          # v7 API
            "nano-banana"          # v7 API
        ]
        
        # Test image (create a simple test image)
        self.test_image_url = self.create_test_image()
        
    def load_env(self):
        """Load API keys from .env file"""
        env_path = os.path.join("Modelslab Image Editing Tester", ".env")
        if os.path.exists(env_path):
            with open(env_path, 'r') as f:
                for line in f:
                    if line.startswith('MODELSLAB_API_KEY='):
                        self.api_key = line.split('=', 1)[1].strip()
                    elif line.startswith('IMGBB_API_KEY='):
                        self.imgbb_api_key = line.split('=', 1)[1].strip()
    
    def create_test_image(self):
        """Create a simple test image and upload to imgbb"""
        try:
            # Create a simple 512x512 test image
            img = Image.new('RGB', (512, 512), color='lightblue')
            
            # Add some simple content
            from PIL import ImageDraw, ImageFont
            draw = ImageDraw.Draw(img)
            try:
                # Try to use default font
                font = ImageFont.load_default()
            except:
                font = None
            
            draw.rectangle([100, 100, 400, 400], fill='white', outline='black', width=3)
            draw.text((200, 250), "TEST IMAGE", fill='black', font=font)
            
            # Convert to bytes
            buffer = io.BytesIO()
            img.save(buffer, format='PNG')
            image_data = buffer.getvalue()
            
            # Upload to imgbb
            if self.imgbb_api_key:
                base64_data = base64.b64encode(image_data).decode('utf-8')
                
                payload = {
                    'key': self.imgbb_api_key,
                    'image': base64_data,
                    'expiration': 3600  # 1 hour
                }
                
                response = requests.post("https://api.imgbb.com/1/upload", data=payload, timeout=30)
                
                if response.status_code == 200:
                    result = response.json()
                    if result.get('success'):
                        url = result['data']['url']
                        print(f"[SUCCESS] Test image uploaded: {url}")
                        return url
            
            print("[ERROR] Failed to upload test image, using fallback URL")
            return "https://i.pinimg.com/736x/20/ab/3d/20ab3df5c180e1cae812020bcfeb3093.jpg"
            
        except Exception as e:
            print(f"[ERROR] Error creating test image: {e}")
            return "https://i.pinimg.com/736x/20/ab/3d/20ab3df5c180e1cae812020bcfeb3093.jpg"
    
    def poll_modelslab_result(self, initial_response, max_attempts=60):
        """Poll ModelsLab API for async result"""
        try:
            task_id = initial_response.get('id')
            fetch_url = initial_response.get('fetch_result')
            estimated_time = initial_response.get('estimated_time', 10)
            
            if not task_id or not fetch_url:
                return None
            
            print(f"[POLLING] Starting polling for task: {task_id}")
            print(f"[POLLING] Fetch URL: {fetch_url}")
            print(f"[POLLING] Estimated time: {estimated_time}s")
            
            # Wait initial estimated time
            time.sleep(min(estimated_time, 30))
            
            attempts = 0
            while attempts < max_attempts:
                try:
                    # Poll the fetch endpoint
                    poll_data = {"key": self.api_key}
                    poll_response = requests.post(fetch_url, json=poll_data, timeout=30)
                    
                    print(f"[POLL] Attempt {attempts+1}/{max_attempts}, status: {poll_response.status_code}")
                    
                    if poll_response.status_code == 200:
                        result = poll_response.json()
                        status = result.get('status', '')
                        
                        if status == 'success':
                            print(f"[SUCCESS] Task completed successfully!")
                            return result
                        elif status in ['processing', 'queued']:
                            print(f"[WAIT] Task status: {status}, continuing...")
                        elif status in ['failed', 'error']:
                            error_msg = result.get('message', 'Task failed')
                            print(f"[FAILED] Task failed: {error_msg}")
                            return None
                    else:
                        print(f"[WARNING] Poll request failed with status: {poll_response.status_code}")
                    
                    attempts += 1
                    if attempts < max_attempts:
                        time.sleep(5)
                        
                except Exception as e:
                    print(f"[WARNING] Poll attempt error: {e}")
                    attempts += 1
                    if attempts < max_attempts:
                        time.sleep(5)
            
            print(f"[TIMEOUT] Polling timed out after {max_attempts * 5} seconds")
            return None
            
        except Exception as e:
            print(f"[ERROR] Polling error: {e}")
            return None
    
    def test_model(self, model_id):
        """Test a specific model for async behavior"""
        print(f"\n[TEST] Testing model: {model_id}")
        print("=" * 50)
        
        try:
            # Prepare request based on model
            if model_id == "flux-kontext-pro":
                url = "https://modelslab.com/api/v7/images/image-to-image"
                data = {
                    "init_image": self.test_image_url,
                    "prompt": "a beautiful landscape with mountains and lakes",
                    "model_id": model_id,
                    "aspect_ratio": "1:1",
                    "key": self.api_key
                }
            elif model_id == "seedream-4":
                url = "https://modelslab.com/api/v7/images/image-to-image"
                data = {
                    "init_image": [self.test_image_url],
                    "prompt": "a beautiful landscape with mountains and lakes",
                    "model_id": model_id,
                    "key": self.api_key
                }
            elif model_id == "nano-banana":
                url = "https://modelslab.com/api/v7/images/image-to-image"
                data = {
                    "prompt": "a beautiful landscape with mountains and lakes",
                    "model_id": model_id,
                    "init_image": self.test_image_url,
                    "key": self.api_key
                }
            else:  # flux-kontext-dev
                url = "https://modelslab.com/api/v6/images/img2img"
                data = {
                    "init_image": self.test_image_url,
                    "init_image_2": "",
                    "prompt": "a beautiful landscape with mountains and lakes",
                    "negative_prompt": "bad quality, blurry",
                    "model_id": model_id,
                    "num_inference_steps": "28",
                    "strength": "0.7",
                    "scheduler": "DPMSolverMultistepScheduler",
                    "guidance": "2.5",
                    "enhance_prompt": False,
                    "base64": "no",
                    "key": self.api_key
                }
            
            headers = {"Content-Type": "application/json"}
            
            print(f"[REQUEST] Making request to: {url}")
            print(f"[REQUEST] Request data: {json.dumps({k: v for k, v in data.items() if k != 'key'}, indent=2)}")
            
            # Make API request
            response = requests.post(url, json=data, headers=headers, timeout=120)
            
            print(f"[RESPONSE] Response status: {response.status_code}")
            
            if response.status_code == 200:
                response_json = response.json()
                print(f"[RESPONSE] Response: {json.dumps(response_json, indent=2)}")
                
                # Check if response is async
                is_async = (response_json.get('status') != 'success' and 
                           ("fetch_result" in response_json or "id" in response_json))
                
                if is_async:
                    print("[ASYNC] ASYNC response detected - starting polling...")
                    poll_result = self.poll_modelslab_result(response_json)
                    
                    if poll_result and poll_result.get('status') == 'success':
                        output = poll_result.get("output")
                        if isinstance(output, list) and len(output) > 0:
                            image_url = output[0]
                        elif isinstance(output, str):
                            image_url = output
                        else:
                            image_url = None
                        
                        if image_url:
                            print(f"[SUCCESS] Final image URL: {image_url}")
                            return True, "async", image_url
                        else:
                            print("[FAILED] No image URL in poll result")
                            return False, "async", None
                    else:
                        print("[FAILED] Polling failed or timed out")
                        return False, "async", None
                else:
                    print("[SYNC] SYNC response detected - immediate result")
                    output = response_json.get("output")
                    if isinstance(output, list) and len(output) > 0:
                        image_url = output[0]
                    elif isinstance(output, str):
                        image_url = output
                    else:
                        image_url = None
                    
                    if image_url:
                        print(f"[SUCCESS] Immediate image URL: {image_url}")
                        return True, "sync", image_url
                    else:
                        print("[FAILED] No image URL in immediate response")
                        return False, "sync", None
            else:
                error_text = response.text
                print(f"[FAILED] HTTP {response.status_code} - {error_text}")
                return False, "error", None
                
        except Exception as e:
            print(f"[EXCEPTION] {e}")
            return False, "exception", None
    
    def run_all_tests(self):
        """Run tests for all models"""
        print("[START] Starting ModelsLab Async Polling Tests")
        print("=" * 60)
        
        if not self.api_key:
            print("[ERROR] MODELSLAB_API_KEY not found in .env file")
            return
        
        print(f"[INFO] Using API key: {self.api_key[:10]}...")
        print(f"[INFO] Test image URL: {self.test_image_url}")
        
        results = {}
        
        for model in self.models:
            success, response_type, image_url = self.test_model(model)
            results[model] = {
                'success': success,
                'type': response_type,
                'image_url': image_url
            }
            
            # Wait between tests
            time.sleep(2)
        
        # Print summary
        print("\n[SUMMARY] TEST RESULTS SUMMARY")
        print("=" * 60)
        
        async_models = []
        sync_models = []
        failed_models = []
        
        for model, result in results.items():
            status = "[PASS]" if result['success'] else "[FAIL]"
            response_type = result['type'].upper()
            print(f"{model:20} | {status} | {response_type:10}")
            
            if result['success']:
                if result['type'] == 'async':
                    async_models.append(model)
                elif result['type'] == 'sync':
                    sync_models.append(model)
            else:
                failed_models.append(model)
        
        print(f"\n[FINAL] SUMMARY:")
        print(f"[ASYNC] Async models: {len(async_models)} - {async_models}")
        print(f"[SYNC] Sync models: {len(sync_models)} - {sync_models}")
        print(f"[FAILED] Failed models: {len(failed_models)} - {failed_models}")
        
        # Verify async functionality
        if async_models:
            print(f"\n[SUCCESS] ASYNC POLLING VERIFIED: {len(async_models)} models use async responses")
            print("[READY] Ready to implement in Android app!")
        else:
            print(f"\n[WARNING] NO ASYNC RESPONSES: All models returned immediate results")
            print("[INFO] May need to test with larger images or different prompts")

if __name__ == "__main__":
    tester = ModelsLabAsyncTester()
    tester.run_all_tests()
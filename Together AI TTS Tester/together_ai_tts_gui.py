#!/usr/bin/env python3
"""
Together AI TTS GUI Testing Tool
"""

import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox, filedialog
import requests
import json
import threading
import os
from datetime import datetime

class TTSTestGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Together AI TTS Testing Tool")
        self.root.geometry("800x600")
        
        # Variables
        self.api_key = tk.StringVar()
        self.test_text = tk.StringVar(value="Hello! This is a test of the Together AI text-to-speech system.")
        self.is_testing = False
        
        # Load API key from .env if exists
        self.load_api_key()
        
        # Models and voices
        self.models = ["cartesia/sonic", "cartesia/sonic-2"]
        self.voices = {
            "79a125e8-cd45-4c13-8a67-188112f4dd22": "Barbershop Man",
            "a0e99841-438c-4a64-b679-ae501e7d6091": "Conversational Woman", 
            "2ee87190-8f84-4925-97da-e52547f9462c": "Customer Service Woman",
            "820a3788-2b37-4d21-847a-b65d8a68c99a": "Newscaster Man",
            "fb26447f-308b-471e-8b00-8e9f04284eb5": "Newscaster Woman",
            "726d5ae5-055f-4c3d-8355-d9677de68937": "Classy British Man",
            "700d49a5-bbc4-4e9b-9f31-a455a2ac7473": "Calm Woman",
            "248be419-c632-4f23-adf1-5324ed7dbf1d": "Excited Man",
            "156fb8d2-335b-4950-9cb3-a2d33befec77": "Friendly Woman",
            "c45bc5ec-dc68-4feb-8829-6e6b2748095d": "Authoritative Man",
            "50d6beb4-80ea-4802-8387-6c948fe84208": "Young Woman",
            "638efaaa-1a65-4c63-9b04-86c4ad8a3c2b": "Wise Man",
            "b7d50908-b17c-442d-ad8d-810c63997ed9": "Cheerful Woman",
            "87748186-23bb-4158-a1eb-332911b0b708": "Professional Man",
            "21b81c14-f85b-436d-aff5-43f2e788ecf8": "Gentle Woman"
        }
        
        self.setup_ui()
        
    def load_api_key(self):
        """Load API key from .env file"""
        try:
            if os.path.exists('.env'):
                with open('.env', 'r') as f:
                    for line in f:
                        if line.startswith('TOGETHER_AI_API_KEY='):
                            key = line.split('=', 1)[1].strip()
                            self.api_key.set(key)
                            break
        except Exception:
            pass
    
    def save_api_key(self):
        """Save API key to .env file"""
        try:
            key = self.api_key.get().strip()
            if key:
                with open('.env', 'w') as f:
                    f.write(f'TOGETHER_AI_API_KEY={key}\n')
        except Exception:
            pass
        
    def setup_ui(self):
        # Main frame
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # API Key
        ttk.Label(main_frame, text="Together AI API Key:").grid(row=0, column=0, sticky=tk.W, pady=5)
        ttk.Entry(main_frame, textvariable=self.api_key, width=50, show="*").grid(row=0, column=1, sticky=(tk.W, tk.E), pady=5)
        
        # Test Text
        ttk.Label(main_frame, text="Test Text:").grid(row=1, column=0, sticky=tk.W, pady=5)
        ttk.Entry(main_frame, textvariable=self.test_text, width=50).grid(row=1, column=1, sticky=(tk.W, tk.E), pady=5)
        
        # Model Selection
        ttk.Label(main_frame, text="Models:").grid(row=2, column=0, sticky=tk.W, pady=5)
        model_frame = ttk.Frame(main_frame)
        model_frame.grid(row=2, column=1, sticky=(tk.W, tk.E), pady=5)
        
        self.model_vars = {}
        for i, model in enumerate(self.models):
            var = tk.BooleanVar(value=True)
            self.model_vars[model] = var
            ttk.Checkbutton(model_frame, text=model, variable=var).grid(row=0, column=i, sticky=tk.W, padx=5)
        
        # Voice Selection
        ttk.Label(main_frame, text="Voices:").grid(row=3, column=0, sticky=tk.W, pady=5)
        voice_frame = ttk.Frame(main_frame)
        voice_frame.grid(row=3, column=1, sticky=(tk.W, tk.E), pady=5)
        
        self.voice_vars = {}
        for i, (voice_id, voice_name) in enumerate(self.voices.items()):
            var = tk.BooleanVar(value=True)
            self.voice_vars[voice_id] = var
            ttk.Checkbutton(voice_frame, text=voice_name, variable=var).grid(row=i//3, column=i%3, sticky=tk.W, padx=5)
        
        # Buttons
        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=4, column=0, columnspan=2, pady=10)
        
        self.test_btn = ttk.Button(button_frame, text="Start Testing", command=self.start_testing)
        self.test_btn.pack(side=tk.LEFT, padx=5)
        
        ttk.Button(button_frame, text="Clear Log", command=self.clear_log).pack(side=tk.LEFT, padx=5)
        ttk.Button(button_frame, text="Save Log", command=self.save_log).pack(side=tk.LEFT, padx=5)
        
        # Progress
        self.progress = ttk.Progressbar(main_frame, mode='determinate')
        self.progress.grid(row=5, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=5)
        
        # Log view
        ttk.Label(main_frame, text="Test Log:").grid(row=6, column=0, sticky=tk.W, pady=(10,0))
        
        self.log_text = scrolledtext.ScrolledText(main_frame, height=20, width=80)
        self.log_text.grid(row=7, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=5)
        
        # Configure grid weights
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        main_frame.rowconfigure(7, weight=1)
        
    def log(self, message):
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.insert(tk.END, f"[{timestamp}] {message}\n")
        self.log_text.see(tk.END)
        self.root.update()
        
    def clear_log(self):
        self.log_text.delete(1.0, tk.END)
        
    def save_log(self):
        filename = filedialog.asksaveasfilename(
            defaultextension=".txt",
            filetypes=[("Text files", "*.txt"), ("All files", "*.*")]
        )
        if filename:
            with open(filename, 'w') as f:
                f.write(self.log_text.get(1.0, tk.END))
            messagebox.showinfo("Saved", f"Log saved to {filename}")
    
    def start_testing(self):
        if self.is_testing:
            return
            
        if not self.api_key.get().strip():
            messagebox.showerror("Error", "Please enter your Together AI API key")
            return
            
        # Save API key to .env for future use
        self.save_api_key()
            
        self.is_testing = True
        self.test_btn.config(text="Testing...", state="disabled")
        selected_models = [m for m, var in self.model_vars.items() if var.get()]
        selected_voices = [v for v, var in self.voice_vars.items() if var.get()]
        
        if not selected_models or not selected_voices:
            messagebox.showerror("Error", "Please select at least one model and one voice")
            self.is_testing = False
            self.test_btn.config(text="Start Testing", state="normal")
            return
            
        self.progress['maximum'] = len(selected_models) * len(selected_voices)
        self.progress['value'] = 0
        
        # Start testing in separate thread
        threading.Thread(target=self.run_tests, daemon=True).start()
        
    def run_tests(self):
        try:
            self.log("🚀 Starting Together AI TTS Testing")
            self.log(f"📝 Test text: {self.test_text.get()}")
            self.log(f"🔑 API key: {'*' * (len(self.api_key.get()) - 8) + self.api_key.get()[-8:]}")
            self.log("=" * 60)
            
            selected_models = [m for m, var in self.model_vars.items() if var.get()]
            selected_voices = [(v, self.voices[v]) for v, var in self.voice_vars.items() if var.get()]
            
            results = []
            test_count = 0
            total_tests = len(selected_models) * len(selected_voices)
            
            for model in selected_models:
                for voice_id, voice_name in selected_voices:
                    test_count += 1
                    self.log(f"\n[{test_count}/{total_tests}] 🎵 Testing {model} + {voice_name}")
                    
                    success, response_data = self.test_tts(model, voice_id, voice_name)
                    results.append({
                        "model": model,
                        "voice_id": voice_id,
                        "voice_name": voice_name,
                        "success": success,
                        "response": response_data
                    })
                    
                    self.progress['value'] = test_count
                    
            # Summary
            successful = sum(1 for r in results if r["success"])
            self.log("\n" + "=" * 60)
            self.log("📊 TEST RESULTS SUMMARY")
            self.log("=" * 60)
            self.log(f"✅ Successful: {successful}/{total_tests}")
            self.log(f"❌ Failed: {total_tests - successful}/{total_tests}")
            
            self.log("\n📋 DETAILED RESULTS:")
            for result in results:
                status = "✅" if result["success"] else "❌"
                self.log(f"{status} {result['model']} + {result['voice_name']}")
            
            # Save results
            with open("tts_gui_results.json", "w") as f:
                json.dump(results, f, indent=2)
            self.log(f"\n💾 Results saved to tts_gui_results.json")
            
        except Exception as e:
            self.log(f"❌ CRITICAL ERROR: {str(e)}")
        finally:
            self.is_testing = False
            self.test_btn.config(text="Start Testing", state="normal")
            
    def test_tts(self, model, voice_id, voice_name):
        headers = {
            "Authorization": f"Bearer {self.api_key.get()}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "model": model,
            "input": self.test_text.get(),
            "voice": voice_id,
            "response_format": "mp3",
            "speed": 1.0
        }
        
        self.log(f"📤 REQUEST: {json.dumps(payload, indent=2)}")
        
        try:
            response = requests.post(
                "https://api.together.xyz/v1/audio/speech",
                headers=headers,
                json=payload,
                timeout=30
            )
            
            self.log(f"📥 RESPONSE CODE: {response.status_code}")
            self.log(f"📥 RESPONSE HEADERS: {dict(response.headers)}")
            
            if response.status_code == 200:
                # Save audio file
                filename = f"test_{model.replace('/', '_')}_{voice_name.replace(' ', '_')}.mp3"
                with open(filename, 'wb') as f:
                    f.write(response.content)
                
                self.log(f"✅ SUCCESS - Saved {filename} ({len(response.content)} bytes)")
                return True, {"status": "success", "file": filename, "size": len(response.content)}
            else:
                error_text = response.text
                self.log(f"❌ FAILED - {response.status_code}")
                self.log(f"📥 ERROR RESPONSE: {error_text}")
                return False, {"status": "error", "code": response.status_code, "error": error_text}
                
        except Exception as e:
            self.log(f"❌ EXCEPTION: {str(e)}")
            return False, {"status": "exception", "error": str(e)}

def main():
    root = tk.Tk()
    app = TTSTestGUI(root)
    root.mainloop()

if __name__ == "__main__":
    main()
#!/usr/bin/env python3
"""
ModelsLab TTS Testing App
Test both inworld-tts-1 and eleven_multilingual_v2 models with their respective voices
"""

import requests
import json
import time
import os
import webbrowser
from typing import Dict, List, Optional
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
import threading
import pygame

class ModelsLabTTSTester:
    def __init__(self):
        self.api_key = ""
        self.base_url = "https://modelslab.com/api/v6/voice/text_to_audio"
        
        # Voice configurations
        self.models = {
            "inworld-tts-1": [
                "Alex", "Ashley", "Craig", "Deborah", "Dennis", "Edward", 
                "Elizabeth", "Hades", "Julia", "Pixie", "Mark", "Olivia", 
                "Priya", "Ronald", "Sarah", "Shaun", "Theodore", "Timothy", "Wendy"
            ],
            "eleven_multilingual_v2": [
                "JBFqnCBsd6RMkjVDRZzb", "Olivia", "CrSantos", "Egor", 
                "Le Thanh Sang", "Lucy Fennek", "Deshna Sogam", "Bunny", 
                "Roy Neace", "Sead", "Joseph", "Victoria", "Axel", "Arka", 
                "Jia Wei", "Jordan Rapp", "Ozzie", "Zoe", "Eli", "Oscar Bardem",
                "egBwWoDtXGCSMgwiS3vn","duDBJHU6G1oq7ZdK4Kxf","6HyctQJo1Ipy39bEzCZ9",
                "M7baJQBjzMsrxxZ796H6","I1HmD60D49GZSRek3im0","PPybRLZHRicSB057bHyI",
                "ynPDxnl9LkoyPcG6LoYS","nQWxDqNbCfuV85X9I7qH"
            ]
        }
        
        # Initialize pygame mixer for audio playback
        pygame.mixer.init()
        
        self.setup_gui()
    
    def setup_gui(self):
        self.root = tk.Tk()
        self.root.title("ModelsLab TTS Tester")
        self.root.geometry("800x700")
        
        # API Key Frame
        api_frame = ttk.Frame(self.root)
        api_frame.pack(fill="x", padx=10, pady=5)
        
        ttk.Label(api_frame, text="API Key:").pack(side="left")
        self.api_key_var = tk.StringVar()
        ttk.Entry(api_frame, textvariable=self.api_key_var, show="*", width=50).pack(side="left", padx=5)
        ttk.Button(api_frame, text="Set", command=self.set_api_key).pack(side="left")
        
        # Model Selection Frame
        model_frame = ttk.Frame(self.root)
        model_frame.pack(fill="x", padx=10, pady=5)
        
        ttk.Label(model_frame, text="Model:").pack(side="left")
        self.model_var = tk.StringVar(value="inworld-tts-1")
        model_combo = ttk.Combobox(model_frame, textvariable=self.model_var, 
                                  values=list(self.models.keys()), state="readonly")
        model_combo.pack(side="left", padx=5)
        model_combo.bind("<<ComboboxSelected>>", self.on_model_change)
        
        # Voice Selection Frame
        voice_frame = ttk.Frame(self.root)
        voice_frame.pack(fill="x", padx=10, pady=5)
        
        ttk.Label(voice_frame, text="Voice:").pack(side="left")
        self.voice_var = tk.StringVar()
        self.voice_combo = ttk.Combobox(voice_frame, textvariable=self.voice_var, state="readonly")
        self.voice_combo.pack(side="left", padx=5, fill="x", expand=True)
        
        # Text Input Frame
        text_frame = ttk.Frame(self.root)
        text_frame.pack(fill="both", padx=10, pady=5, expand=True)
        
        ttk.Label(text_frame, text="Text to Speech:").pack(anchor="w")
        self.text_input = scrolledtext.ScrolledText(text_frame, height=4)
        self.text_input.pack(fill="both", expand=True)
        self.text_input.insert("1.0", "Hello! This is a test of the selected voice.")
        
        # Control Buttons Frame
        control_frame = ttk.Frame(self.root)
        control_frame.pack(fill="x", padx=10, pady=5)
        
        ttk.Button(control_frame, text="Generate & Play", command=self.generate_and_play).pack(side="left", padx=5)
        ttk.Button(control_frame, text="Test All Voices", command=self.test_all_voices).pack(side="left", padx=5)
        ttk.Button(control_frame, text="Stop Audio", command=self.stop_audio).pack(side="left", padx=5)
        
        # Results Frame
        results_frame = ttk.Frame(self.root)
        results_frame.pack(fill="both", padx=10, pady=5, expand=True)
        
        ttk.Label(results_frame, text="Results:").pack(anchor="w")
        self.results_text = scrolledtext.ScrolledText(results_frame, height=10)
        self.results_text.pack(fill="both", expand=True)
        
        # Progress Bar
        self.progress = ttk.Progressbar(self.root, mode='indeterminate')
        self.progress.pack(fill="x", padx=10, pady=5)
        
        # Initialize voice list
        self.on_model_change()
    
    def set_api_key(self):
        self.api_key = self.api_key_var.get().strip()
        if self.api_key:
            self.log("API Key set successfully")
        else:
            self.log("Please enter a valid API key")
    
    def on_model_change(self, event=None):
        model = self.model_var.get()
        voices = self.models.get(model, [])
        self.voice_combo['values'] = voices
        if voices:
            self.voice_var.set(voices[0])
    
    def log(self, message: str):
        timestamp = time.strftime("%H:%M:%S")
        self.results_text.insert(tk.END, f"[{timestamp}] {message}\n")
        self.results_text.see(tk.END)
        self.root.update()
    
    def generate_tts(self, text: str, model: str, voice: str) -> Optional[str]:
        if not self.api_key:
            self.log("❌ API key not set")
            return None
        
        payload = {
            "key": self.api_key,
            "model_id": model,
            "prompt": text,
            "voice_id": voice,
            "language": "english"
        }
        
        try:
            self.log(f"🔄 Generating TTS for {model} - {voice}")
            response = requests.post(self.base_url, json=payload, timeout=60)
            
            if response.status_code != 200:
                self.log(f"❌ HTTP {response.status_code}")
                self.log(f"📋 Response Body: {response.text}")
                return None
            
            data = response.json()
            
            if data.get("status") != "success":
                error_msg = data.get('message', 'Unknown error')
                self.log(f"❌ API Error: {error_msg}")
                self.log(f"📋 Full API Response: {data}")
                return None
            
            # Extract audio URL
            audio_url = None
            if "output" in data:
                if isinstance(data["output"], list) and data["output"]:
                    audio_url = data["output"][0]
                elif isinstance(data["output"], str):
                    audio_url = data["output"]
            
            if not audio_url:
                self.log(f"❌ No audio URL in response: {data}")
                return None
            
            self.log(f"✅ Generated: {audio_url}")
            return audio_url
            
        except requests.exceptions.Timeout:
            self.log("❌ Request timeout")
            return None
        except Exception as e:
            self.log(f"❌ Error: {str(e)}")
            return None
    
    def play_audio_url(self, url: str) -> bool:
        temp_file = None
        try:
            self.log(f"🔊 Playing audio from: {url}")
            
            # Download audio file
            response = requests.get(url, timeout=30)
            if response.status_code != 200:
                self.log(f"❌ Failed to download audio: HTTP {response.status_code}")
                return False
            
            # Save temporary file with unique name
            import tempfile
            temp_file = tempfile.mktemp(suffix=".wav")
            with open(temp_file, "wb") as f:
                f.write(response.content)
            
            # Play audio
            pygame.mixer.music.load(temp_file)
            pygame.mixer.music.play()
            
            # Wait for playback to complete
            while pygame.mixer.music.get_busy():
                time.sleep(0.1)
            
            # Unload music before cleanup
            pygame.mixer.music.unload()
            
            self.log("✅ Audio playback completed")
            return True
            
        except Exception as e:
            self.log(f"❌ Playback error: {str(e)}")
            return False
        finally:
            # Clean up temp file
            if temp_file and os.path.exists(temp_file):
                try:
                    os.remove(temp_file)
                except:
                    pass  # Ignore cleanup errors
    
    def stop_audio(self):
        pygame.mixer.music.stop()
        self.log("🛑 Audio stopped")
    
    def generate_and_play(self):
        def task():
            self.progress.start()
            try:
                text = self.text_input.get("1.0", tk.END).strip()
                model = self.model_var.get()
                voice = self.voice_var.get()
                
                if not text:
                    self.log("❌ Please enter text to convert")
                    return
                
                audio_url = self.generate_tts(text, model, voice)
                if audio_url:
                    self.play_audio_url(audio_url)
            finally:
                self.progress.stop()
        
        threading.Thread(target=task, daemon=True).start()
    
    def test_all_voices(self):
        def task():
            self.progress.start()
            try:
                model = self.model_var.get()
                voices = self.models[model]
                text = "Testing voice"
                
                self.log(f"🧪 Testing all voices for {model}")
                
                results = {}
                for voice in voices:
                    self.log(f"Testing {voice}...")
                    audio_url = self.generate_tts(text, model, voice)
                    results[voice] = "✅ Success" if audio_url else "❌ Failed"
                    time.sleep(1)  # Rate limiting
                
                self.log("\n📊 Test Results Summary:")
                for voice, result in results.items():
                    self.log(f"  {voice}: {result}")
                
            finally:
                self.progress.stop()
        
        threading.Thread(target=task, daemon=True).start()
    
    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    # Install required packages if not available
    try:
        import pygame
        import requests
    except ImportError:
        print("Installing required packages...")
        os.system("pip install pygame requests")
        import pygame
        import requests
    
    app = ModelsLabTTSTester()
    app.run()
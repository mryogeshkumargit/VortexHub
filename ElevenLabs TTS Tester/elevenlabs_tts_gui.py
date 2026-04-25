import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox
import requests
import json
import os
from datetime import datetime
import threading
import pygame
import tempfile
import time

class ElevenLabsTTSGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("ElevenLabs TTS Tester")
        self.root.geometry("800x700")
        
        # Load environment variables
        self.load_env()
        
        # TTS Models and Voices
        self.tts_models = [
            "eleven_multilingual_v2",
            "eleven_turbo_v2_5", 
            "eleven_turbo_v2",
            "eleven_monolingual_v1",
            "eleven_multilingual_v1"
        ]
        
        # English voices with actual ElevenLabs voice IDs
        self.english_voices = {
            "Rachel": "21m00Tcm4TlvDq8ikWAM",
            "Drew": "29vD33N1CtxCmqQRPOHJ", 
            "Clyde": "2EiwWnXFnvU5JabPnv8n",
            "Paul": "5Q0t7uMcjvnagumLfvZi",
            "Domi": "AZnzlk1XvdvUeBnXmlld",
            "Dave": "CYw3kZ02Hs0563khs1Fj",
            "Fin": "D38z5RcWu1voky8WS1ja",
            "Sarah": "EXAVITQu4vr4xnSDxMaL",
            "Antoni": "ErXwobaYiN019PkySvjV",
            "Thomas": "GBv7mTt0atIp3Br8iCZE"
        }
        
        # Hindi voices with actual ElevenLabs voice IDs  
        self.hindi_voices = {
            "Prabhat": "pNInz6obpgDQGcFmaJgB",
            "Abhishek": "JBFqnCBsd6RMkjVDRZzb",
            "Aditi": "Xb7hH8MSUJpSbSDYk0k2",
            "Arjun": "bVMeCyTHy58xNoL34h3p",
            "Kavya": "YoX06hSQ4eaAhyEXGBJO"
        }
        
        # Initialize pygame mixer for audio playback
        pygame.mixer.init()
        
        self.setup_ui()
        
    def load_env(self):
        """Load API key from .env file"""
        self.api_key = ""
        env_path = os.path.join(os.path.dirname(__file__), '.env')
        if os.path.exists(env_path):
            with open(env_path, 'r') as f:
                for line in f:
                    if line.startswith('ELEVENLABS_API_KEY='):
                        self.api_key = line.split('=', 1)[1].strip()
                        break
    
    def save_env(self):
        """Save API key to .env file"""
        env_path = os.path.join(os.path.dirname(__file__), '.env')
        with open(env_path, 'w') as f:
            f.write(f'ELEVENLABS_API_KEY={self.api_key}\n')
    
    def setup_ui(self):
        # Main frame
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # API Key section
        ttk.Label(main_frame, text="ElevenLabs API Key:").grid(row=0, column=0, sticky=tk.W, pady=5)
        self.api_key_var = tk.StringVar(value=self.api_key)
        api_key_entry = ttk.Entry(main_frame, textvariable=self.api_key_var, width=50, show="*")
        api_key_entry.grid(row=0, column=1, columnspan=2, sticky=(tk.W, tk.E), pady=5)
        
        ttk.Button(main_frame, text="Save API Key", command=self.save_api_key).grid(row=0, column=3, padx=5, pady=5)
        
        # Model selection
        ttk.Label(main_frame, text="TTS Model:").grid(row=1, column=0, sticky=tk.W, pady=5)
        self.model_var = tk.StringVar(value=self.tts_models[0])
        model_combo = ttk.Combobox(main_frame, textvariable=self.model_var, values=self.tts_models, state="readonly")
        model_combo.grid(row=1, column=1, sticky=(tk.W, tk.E), pady=5)
        
        # Language selection
        ttk.Label(main_frame, text="Language:").grid(row=2, column=0, sticky=tk.W, pady=5)
        self.language_var = tk.StringVar(value="English")
        language_combo = ttk.Combobox(main_frame, textvariable=self.language_var, values=["English", "Hindi"], state="readonly")
        language_combo.grid(row=2, column=1, sticky=(tk.W, tk.E), pady=5)
        language_combo.bind('<<ComboboxSelected>>', self.on_language_change)
        
        # Voice selection
        ttk.Label(main_frame, text="Voice:").grid(row=3, column=0, sticky=tk.W, pady=5)
        self.voice_var = tk.StringVar(value=list(self.english_voices.keys())[0])
        self.voice_combo = ttk.Combobox(main_frame, textvariable=self.voice_var, values=list(self.english_voices.keys()), state="readonly")
        self.voice_combo.grid(row=3, column=1, sticky=(tk.W, tk.E), pady=5)
        
        # Text input
        ttk.Label(main_frame, text="Text to Speech:").grid(row=4, column=0, sticky=(tk.W, tk.N), pady=5)
        self.text_input = scrolledtext.ScrolledText(main_frame, width=60, height=5)
        self.text_input.grid(row=4, column=1, columnspan=3, sticky=(tk.W, tk.E), pady=5)
        self.text_input.insert("1.0", "Hello! This is a test of ElevenLabs text-to-speech.")
        
        # Control buttons
        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=5, column=0, columnspan=4, pady=10)
        
        self.generate_btn = ttk.Button(button_frame, text="Generate Speech", command=self.generate_speech)
        self.generate_btn.pack(side=tk.LEFT, padx=5)
        
        self.play_btn = ttk.Button(button_frame, text="Play Audio", command=self.play_audio, state="disabled")
        self.play_btn.pack(side=tk.LEFT, padx=5)
        
        ttk.Button(button_frame, text="Clear Logs", command=self.clear_logs).pack(side=tk.LEFT, padx=5)
        
        # Progress bar
        self.progress = ttk.Progressbar(main_frame, mode='indeterminate')
        self.progress.grid(row=6, column=0, columnspan=4, sticky=(tk.W, tk.E), pady=5)
        
        # Logs section
        ttk.Label(main_frame, text="API Response Log:").grid(row=7, column=0, sticky=tk.W, pady=(10, 5))
        self.response_log = scrolledtext.ScrolledText(main_frame, width=80, height=8)
        self.response_log.grid(row=8, column=0, columnspan=4, sticky=(tk.W, tk.E), pady=5)
        
        ttk.Label(main_frame, text="Request Log:").grid(row=9, column=0, sticky=tk.W, pady=(10, 5))
        self.request_log = scrolledtext.ScrolledText(main_frame, width=80, height=8)
        self.request_log.grid(row=10, column=0, columnspan=4, sticky=(tk.W, tk.E), pady=5)
        
        # Configure grid weights
        main_frame.columnconfigure(1, weight=1)
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        
        self.audio_file = None
    
    def on_language_change(self, event=None):
        """Update voice options based on selected language"""
        language = self.language_var.get()
        if language == "English":
            voices = list(self.english_voices.keys())
        else:
            voices = list(self.hindi_voices.keys())
        
        self.voice_combo['values'] = voices
        self.voice_var.set(voices[0])
    
    def save_api_key(self):
        """Save API key to environment and .env file"""
        self.api_key = self.api_key_var.get()
        self.save_env()
        messagebox.showinfo("Success", "API key saved successfully!")
    
    def log_request(self, request_data):
        """Log API request details"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] REQUEST:\n{json.dumps(request_data, indent=2)}\n\n"
        self.request_log.insert(tk.END, log_entry)
        self.request_log.see(tk.END)
    
    def log_response(self, response_data):
        """Log API response details"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] RESPONSE:\n{response_data}\n\n"
        self.response_log.insert(tk.END, log_entry)
        self.response_log.see(tk.END)
    
    def clear_logs(self):
        """Clear both log windows"""
        self.request_log.delete(1.0, tk.END)
        self.response_log.delete(1.0, tk.END)
    
    def generate_speech(self):
        """Generate speech using ElevenLabs API"""
        if not self.api_key_var.get():
            messagebox.showerror("Error", "Please enter your ElevenLabs API key!")
            return
        
        text = self.text_input.get("1.0", tk.END).strip()
        if not text:
            messagebox.showerror("Error", "Please enter text to convert to speech!")
            return
        
        # Start generation in separate thread
        threading.Thread(target=self._generate_speech_thread, args=(text,), daemon=True).start()
    
    def _generate_speech_thread(self, text):
        """Generate speech in separate thread"""
        try:
            self.root.after(0, lambda: self.progress.start())
            self.root.after(0, lambda: self.generate_btn.config(state="disabled"))
            
            # Get actual voice ID from selected voice name
            voice_name = self.voice_var.get()
            language = self.language_var.get()
            
            if language == "English":
                voice_id = self.english_voices.get(voice_name)
            else:
                voice_id = self.hindi_voices.get(voice_name)
            
            if not voice_id:
                raise ValueError(f"Voice ID not found for {voice_name}")
            
            # Prepare request
            url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
            
            headers = {
                "Accept": "audio/mpeg",
                "Content-Type": "application/json",
                "xi-api-key": self.api_key_var.get()
            }
            
            data = {
                "text": text,
                "model_id": self.model_var.get(),
                "voice_settings": {
                    "stability": 0.5,
                    "similarity_boost": 0.5
                }
            }
            
            # Log request
            request_info = {
                "url": url,
                "headers": {k: v if k != "xi-api-key" else "***HIDDEN***" for k, v in headers.items()},
                "data": data
            }
            self.root.after(0, lambda: self.log_request(request_info))
            
            # Make API request with retry logic
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    response = requests.post(url, json=data, headers=headers, timeout=30)
                    break
                except requests.exceptions.ConnectionError as e:
                    if attempt < max_retries - 1:
                        self.root.after(0, lambda: self.log_response(f"Connection attempt {attempt + 1} failed, retrying in 2 seconds..."))
                        time.sleep(2)
                        continue
                    else:
                        raise requests.exceptions.ConnectionError(f"Failed to connect after {max_retries} attempts. Check your internet connection and firewall settings.") from e
            
            # Log response
            response_info = f"Status Code: {response.status_code}\n"
            response_info += f"Headers: {dict(response.headers)}\n"
            
            if response.status_code == 200:
                # Save audio file in app directory
                audio_filename = f"elevenlabs_audio_{datetime.now().strftime('%Y%m%d_%H%M%S')}.mp3"
                audio_path = os.path.join(os.path.dirname(__file__), audio_filename)
                
                with open(audio_path, 'wb') as f:
                    f.write(response.content)
                
                self.audio_file = type('obj', (object,), {'name': audio_path})()
                
                response_info += f"Audio saved to: {audio_path}\n"
                response_info += f"Audio size: {len(response.content)} bytes"
                
                self.root.after(0, lambda: self.play_btn.config(state="normal"))
                self.root.after(0, lambda: messagebox.showinfo("Success", "Speech generated successfully!"))
            else:
                response_info += f"Error: {response.text}"
                self.root.after(0, lambda: messagebox.showerror("Error", f"API Error: {response.text}"))
            
            self.root.after(0, lambda: self.log_response(response_info))
            
        except requests.exceptions.ConnectionError as e:
            error_msg = f"Network connection error: {str(e)}\n\nTroubleshooting steps:\n1. Check your internet connection\n2. Try running 'ipconfig /flushdns' in Command Prompt\n3. Check firewall/antivirus settings\n4. Try using a VPN if behind corporate firewall"
            self.root.after(0, lambda: self.log_response(error_msg))
            self.root.after(0, lambda: messagebox.showerror("Network Error", error_msg))
        except Exception as e:
            error_msg = f"Error generating speech: {str(e)}"
            self.root.after(0, lambda: self.log_response(error_msg))
            self.root.after(0, lambda: messagebox.showerror("Error", error_msg))
        
        finally:
            self.root.after(0, lambda: self.progress.stop())
            self.root.after(0, lambda: self.generate_btn.config(state="normal"))
    
    def play_audio(self):
        """Play the generated audio"""
        if self.audio_file and os.path.exists(self.audio_file.name):
            try:
                pygame.mixer.music.load(self.audio_file.name)
                pygame.mixer.music.play()
                messagebox.showinfo("Playing", "Audio is now playing!")
            except Exception as e:
                messagebox.showerror("Error", f"Error playing audio: {str(e)}")
        else:
            messagebox.showerror("Error", "No audio file to play!")

def main():
    root = tk.Tk()
    app = ElevenLabsTTSGUI(root)
    root.mainloop()

if __name__ == "__main__":
    main()
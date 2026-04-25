import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox
import requests
import json
import time
import threading
import os
from PIL import Image, ImageTk
from io import BytesIO

CONFIG_FILE = "replicate_config.json"
IMAGES_DIR = "replicate_test_images"

class ReplicateTestGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Replicate API Tester")
        self.root.geometry("1200x700")
        
        self.api_key = tk.StringVar()
        self.grok_api_key = tk.StringVar()
        self.current_image = None
        self.generated_images = []
        
        if not os.path.exists(IMAGES_DIR):
            os.makedirs(IMAGES_DIR)
        
        self.load_config()
        self.load_saved_images()
        
        self.create_widgets()
        
    def load_config(self):
        if os.path.exists(CONFIG_FILE):
            try:
                with open(CONFIG_FILE, 'r') as f:
                    config = json.load(f)
                    self.api_key.set(config.get('api_key', ''))
                    self.grok_api_key.set(config.get('grok_api_key', ''))
            except:
                pass
    
    def save_config(self):
        with open(CONFIG_FILE, 'w') as f:
            json.dump({
                'api_key': self.api_key.get(),
                'grok_api_key': self.grok_api_key.get()
            }, f)
    
    def create_widgets(self):
        # Main canvas with scrollbar
        main_canvas = tk.Canvas(self.root)
        scrollbar = ttk.Scrollbar(self.root, orient="vertical", command=main_canvas.yview)
        scrollable_frame = ttk.Frame(main_canvas)
        
        scrollable_frame.bind(
            "<Configure>",
            lambda e: main_canvas.configure(scrollregion=main_canvas.bbox("all"))
        )
        
        main_canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        main_canvas.configure(yscrollcommand=scrollbar.set)
        
        main_canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        
        # Bind mousewheel
        def _on_mousewheel(event):
            main_canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        main_canvas.bind_all("<MouseWheel>", _on_mousewheel)
        
        # API Key Frame
        key_frame = ttk.LabelFrame(scrollable_frame, text="API Configuration", padding=10)
        key_frame.pack(fill="x", padx=10, pady=5)
        
        replicate_key_frame = ttk.Frame(key_frame)
        replicate_key_frame.pack(fill="x", pady=2)
        ttk.Label(replicate_key_frame, text="Replicate API Key:").pack(side="left")
        ttk.Entry(replicate_key_frame, textvariable=self.api_key, width=50, show="*").pack(side="left", padx=5)
        
        grok_key_frame = ttk.Frame(key_frame)
        grok_key_frame.pack(fill="x", pady=2)
        ttk.Label(grok_key_frame, text="Grok API Key:      ").pack(side="left")
        ttk.Entry(grok_key_frame, textvariable=self.grok_api_key, width=50, show="*").pack(side="left", padx=5)
        
        ttk.Button(key_frame, text="Save Keys", command=self.save_config).pack(pady=5)
        
        # Model Selection Frame
        model_frame = ttk.LabelFrame(scrollable_frame, text="Model Selection", padding=10)
        model_frame.pack(fill="x", padx=10, pady=5)
        
        # Provider selection
        provider_frame = ttk.Frame(model_frame)
        provider_frame.pack(fill="x", pady=5)
        
        ttk.Label(provider_frame, text="Provider:").pack(side="left")
        self.provider_var = tk.StringVar(value="Replicate")
        ttk.Combobox(provider_frame, textvariable=self.provider_var, values=["Replicate", "Grok"], width=20, state="readonly").pack(side="left", padx=5)
        self.provider_var.trace('w', self.on_provider_change)
        
        self.model_var = tk.StringVar(value="stability-ai/sdxl")
        self.replicate_models = [
            "stability-ai/sdxl",
            "lucataco/ssd-1b",
            "lucataco/dreamshaper-xl-turbo",
            "adirik/realvisxl-v3.0-turbo",
            "playgroundai/playground-v2.5-1024px-aesthetic",
            "black-forest-labs/flux-dev",
            "black-forest-labs/flux-dev-lora",
            "tencent/hunyuan-image-3"
        ]
        self.grok_models = ["grok-2-image-1212"]
        models = self.replicate_models
        
        ttk.Label(model_frame, text="Model:").pack(side="left")
        self.model_combo = ttk.Combobox(model_frame, textvariable=self.model_var, values=models, width=50)
        self.model_combo.pack(side="left", padx=5)
        
        # Prompt Frame
        prompt_frame = ttk.LabelFrame(scrollable_frame, text="Prompt", padding=10)
        prompt_frame.pack(fill="x", padx=10, pady=5)
        
        self.prompt_var = tk.StringVar(value="a beautiful sunset over mountains")
        ttk.Entry(prompt_frame, textvariable=self.prompt_var, width=70).pack(fill="x")
        
        # Options Frame
        options_frame = ttk.LabelFrame(scrollable_frame, text="Options", padding=10)
        options_frame.pack(fill="x", padx=10, pady=5)
        
        self.disable_safety = tk.BooleanVar(value=True)
        ttk.Checkbutton(options_frame, text="Disable Safety Checker", variable=self.disable_safety).pack(side="left")
        
        # Control Frame
        control_frame = ttk.Frame(scrollable_frame)
        control_frame.pack(fill="x", padx=10, pady=5)
        
        self.test_btn = ttk.Button(control_frame, text="Test Model", command=self.start_test)
        self.test_btn.pack(side="left", padx=5)
        
        ttk.Button(control_frame, text="Clear Log", command=self.clear_log).pack(side="left")
        
        # Main content frame
        content_frame = ttk.Frame(scrollable_frame)
        content_frame.pack(fill="both", expand=True, padx=10, pady=5)
        
        # Left side - Log
        log_frame = ttk.LabelFrame(content_frame, text="Test Log", padding=10)
        log_frame.pack(side="left", fill="both", expand=True, padx=(0, 5))
        
        self.log_text = scrolledtext.ScrolledText(log_frame, height=20, wrap=tk.WORD)
        self.log_text.pack(fill="both", expand=True)
        
        # Right side - Images
        image_frame = ttk.LabelFrame(content_frame, text="Generated Images", padding=10)
        image_frame.pack(side="right", fill="both", expand=True, padx=(5, 0))
        
        # Image display
        self.image_label = ttk.Label(image_frame, text="No image generated yet", anchor="center")
        self.image_label.pack(fill="both", expand=True, pady=5)
        
        # Image list
        list_frame = ttk.Frame(image_frame)
        list_frame.pack(fill="x", pady=5)
        
        ttk.Label(list_frame, text="Saved Images:").pack(anchor="w")
        
        list_scroll_frame = ttk.Frame(list_frame)
        list_scroll_frame.pack(fill="x", pady=5)
        
        scrollbar = ttk.Scrollbar(list_scroll_frame)
        scrollbar.pack(side="right", fill="y")
        
        self.image_listbox = tk.Listbox(list_scroll_frame, height=6, yscrollcommand=scrollbar.set)
        self.image_listbox.pack(side="left", fill="x", expand=True)
        self.image_listbox.bind('<<ListboxSelect>>', self.on_image_select)
        
        scrollbar.config(command=self.image_listbox.yview)
        
        btn_frame = ttk.Frame(list_frame)
        btn_frame.pack(fill="x")
        ttk.Button(btn_frame, text="Delete Selected", command=self.delete_selected_image).pack(side="left", padx=2)
        ttk.Button(btn_frame, text="Clear All", command=self.clear_all_images).pack(side="left", padx=2)
    
    def log(self, message):
        self.log_text.insert(tk.END, message + "\n")
        self.log_text.see(tk.END)
        self.root.update()
    
    def clear_log(self):
        self.log_text.delete(1.0, tk.END)
    
    def start_test(self):
        provider = self.provider_var.get()
        if provider == "Grok" and not self.grok_api_key.get():
            messagebox.showerror("Error", "Please enter Grok API key")
            return
        if provider == "Replicate" and not self.api_key.get():
            messagebox.showerror("Error", "Please enter Replicate API key")
            return
        
        self.test_btn.config(state="disabled")
        thread = threading.Thread(target=self.run_test)
        thread.daemon = True
        thread.start()
    
    def on_provider_change(self, *args):
        provider = self.provider_var.get()
        if provider == "Grok":
            self.model_combo['values'] = self.grok_models
            self.model_var.set("grok-2-image-1212")
        else:
            self.model_combo['values'] = self.replicate_models
            self.model_var.set("stability-ai/sdxl")
    
    def run_test(self):
        try:
            provider = self.provider_var.get()
            model = self.model_var.get()
            prompt = self.prompt_var.get()
            
            self.log(f"{'='*60}")
            self.log(f"Testing {provider} - Model: {model}")
            self.log(f"{'='*60}")
            
            if provider == "Grok":
                image_url = self.test_grok(model, prompt)
            else:
                image_url = self.test_replicate(model, prompt)
            
            if not image_url:
                return
            
            self.log(f"\n✓ TEST PASSED!")
            self.log(f"Image URL: {image_url}")
            
            # Download and save image
            self.log("\nDownloading image...")
            self.download_and_save_image(image_url, f"{provider}/{model}", prompt)
            
        except Exception as e:
            self.log(f"\n✗ Exception: {str(e)}")
        finally:
            self.test_btn.config(state="normal")
    
    def test_replicate(self, model, prompt):
        # Step 1: Fetch version
        self.log("\n[Step 1] Fetching model version...")
        version = self.fetch_version(model)
        if not version:
            self.log("✗ Failed to fetch version")
            return None
        
        # Step 2-4: Create prediction
        self.log("\n[Step 2-4] Creating prediction...")
        prediction_id = self.create_prediction(version, prompt)
        if not prediction_id:
            self.log("✗ Failed to create prediction")
            return None
        
        # Step 5: Poll for result
        self.log("\n[Step 5] Polling for result...")
        image_url = self.poll_prediction(prediction_id)
        return image_url
    
    def test_grok(self, model, prompt):
        self.log("\n[Grok] Generating image...")
        url = "https://api.x.ai/v1/images/generations"
        headers = {
            "Authorization": f"Bearer {self.grok_api_key.get()}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "model": model,
            "prompt": prompt,
            "width": 1024,
            "height": 1024,
            "n": 1
        }
        
        try:
            self.log(f"Request: {json.dumps(payload, indent=2)}")
            response = requests.post(url, headers=headers, json=payload)
            self.log(f"Status: {response.status_code}")
            
            if response.status_code != 200:
                self.log(f"Error: {response.text}")
                return None
            
            data = response.json()
            if 'data' in data and len(data['data']) > 0:
                image_url = data['data'][0].get('url')
                if not image_url:
                    image_url = data['data'][0].get('b64_json')
                return image_url
            else:
                self.log("No image data in response")
                return None
        except Exception as e:
            self.log(f"Exception: {str(e)}")
            return None
    
    def fetch_version(self, model_id):
        url = f"https://api.replicate.com/v1/models/{model_id}"
        headers = {"Authorization": f"Bearer {self.api_key.get()}"}
        
        try:
            response = requests.get(url, headers=headers)
            self.log(f"Status: {response.status_code}")
            
            if response.status_code != 200:
                self.log(f"Error: {response.text}")
                return None
            
            data = response.json()
            version = data.get("latest_version", {}).get("id")
            self.log(f"Version hash: {version}")
            return version
        except Exception as e:
            self.log(f"Exception: {str(e)}")
            return None
    
    def create_prediction(self, version_hash, prompt):
        url = "https://api.replicate.com/v1/predictions"
        headers = {
            "Authorization": f"Bearer {self.api_key.get()}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "version": version_hash,
            "input": {
                "prompt": prompt,
                "width": 1024,
                "height": 1024,
                "num_inference_steps": 20,
                "guidance_scale": 7.5,
                "disable_safety_checker": self.disable_safety.get()
            }
        }
        
        try:
            self.log(f"Payload: {json.dumps(payload, indent=2)}")
            response = requests.post(url, headers=headers, json=payload)
            self.log(f"Status: {response.status_code}")
            
            if response.status_code not in [200, 201]:
                self.log(f"Error: {response.text}")
                return None
            
            data = response.json()
            prediction_id = data.get("id")
            self.log(f"Prediction ID: {prediction_id}")
            return prediction_id
        except Exception as e:
            self.log(f"Exception: {str(e)}")
            return None
    
    def poll_prediction(self, prediction_id):
        url = f"https://api.replicate.com/v1/predictions/{prediction_id}"
        headers = {"Authorization": f"Bearer {self.api_key.get()}"}
        
        max_attempts = 60
        for attempt in range(max_attempts):
            try:
                response = requests.get(url, headers=headers)
                
                if response.status_code != 200:
                    self.log(f"Poll error: {response.status_code}")
                    return None
                
                data = response.json()
                status = data.get("status")
                self.log(f"Attempt {attempt + 1}/{max_attempts} - Status: {status}")
                
                if status == "succeeded":
                    output = data.get("output")
                    if isinstance(output, list) and len(output) > 0:
                        image_url = output[0]
                    elif isinstance(output, str):
                        image_url = output
                    else:
                        image_url = None
                    return image_url
                elif status == "failed":
                    error = data.get("error", "Unknown error")
                    self.log(f"Failed: {error}")
                    return None
                elif status == "canceled":
                    self.log("Canceled")
                    return None
                
                time.sleep(5)
            except Exception as e:
                self.log(f"Poll exception: {str(e)}")
                return None
        
        self.log("Timeout")
        return None
    
    def download_and_save_image(self, image_url, model, prompt):
        try:
            response = requests.get(image_url)
            if response.status_code == 200:
                # Save to disk
                timestamp = int(time.time())
                filename = f"{timestamp}_{model.replace('/', '_')}.png"
                filepath = os.path.join(IMAGES_DIR, filename)
                
                with open(filepath, 'wb') as f:
                    f.write(response.content)
                
                # Save metadata
                metadata = {
                    'filename': filename,
                    'model': model,
                    'prompt': prompt,
                    'timestamp': timestamp,
                    'url': image_url
                }
                
                metadata_file = filepath.replace('.png', '.json')
                with open(metadata_file, 'w') as f:
                    json.dump(metadata, f, indent=2)
                
                self.log(f"✓ Image saved: {filename}")
                
                # Add to list and display
                self.generated_images.append(metadata)
                self.image_listbox.insert(tk.END, f"{model} - {prompt[:30]}...")
                self.display_image(filepath)
            else:
                self.log(f"✗ Failed to download image: {response.status_code}")
        except Exception as e:
            self.log(f"✗ Error saving image: {str(e)}")
    
    def load_saved_images(self):
        if not os.path.exists(IMAGES_DIR):
            return
        
        for filename in os.listdir(IMAGES_DIR):
            if filename.endswith('.json'):
                try:
                    with open(os.path.join(IMAGES_DIR, filename), 'r') as f:
                        metadata = json.load(f)
                        self.generated_images.append(metadata)
                        model = metadata.get('model', 'Unknown')
                        prompt = metadata.get('prompt', 'No prompt')[:30]
                        self.image_listbox.insert(tk.END, f"{model} - {prompt}...")
                except:
                    pass
    
    def on_image_select(self, event):
        selection = self.image_listbox.curselection()
        if selection:
            index = selection[0]
            metadata = self.generated_images[index]
            filepath = os.path.join(IMAGES_DIR, metadata['filename'])
            if os.path.exists(filepath):
                self.display_image(filepath)
    
    def display_image(self, filepath):
        try:
            image = Image.open(filepath)
            # Resize to fit display area (max 400x400)
            image.thumbnail((400, 400), Image.Resampling.LANCZOS)
            photo = ImageTk.PhotoImage(image)
            
            self.image_label.config(image=photo, text="")
            self.image_label.image = photo  # Keep reference
        except Exception as e:
            self.log(f"Error displaying image: {str(e)}")
    
    def delete_selected_image(self):
        selection = self.image_listbox.curselection()
        if not selection:
            return
        
        if messagebox.askyesno("Confirm", "Delete selected image?"):
            index = selection[0]
            metadata = self.generated_images[index]
            
            # Delete files
            filepath = os.path.join(IMAGES_DIR, metadata['filename'])
            metadata_file = filepath.replace('.png', '.json')
            
            try:
                if os.path.exists(filepath):
                    os.remove(filepath)
                if os.path.exists(metadata_file):
                    os.remove(metadata_file)
                
                # Remove from list
                self.generated_images.pop(index)
                self.image_listbox.delete(index)
                self.image_label.config(image='', text="No image selected")
                self.log(f"✓ Deleted image: {metadata['filename']}")
            except Exception as e:
                self.log(f"✗ Error deleting image: {str(e)}")
    
    def clear_all_images(self):
        if not self.generated_images:
            return
        
        if messagebox.askyesno("Confirm", "Delete all saved images?"):
            try:
                for metadata in self.generated_images:
                    filepath = os.path.join(IMAGES_DIR, metadata['filename'])
                    metadata_file = filepath.replace('.png', '.json')
                    
                    if os.path.exists(filepath):
                        os.remove(filepath)
                    if os.path.exists(metadata_file):
                        os.remove(metadata_file)
                
                self.generated_images.clear()
                self.image_listbox.delete(0, tk.END)
                self.image_label.config(image='', text="No images")
                self.log("✓ All images deleted")
            except Exception as e:
                self.log(f"✗ Error clearing images: {str(e)}")

if __name__ == "__main__":
    root = tk.Tk()
    app = ReplicateTestGUI(root)
    root.mainloop()

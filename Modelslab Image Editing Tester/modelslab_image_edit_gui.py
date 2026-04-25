import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox, filedialog
import requests
import json
import os
from datetime import datetime
import threading
from PIL import Image, ImageTk
import base64
import io

class ModelslabImageEditGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Modelslab Image Editing API Tester")
        self.root.geometry("1000x800")
        
        # Available models
        self.models = [
            "flux-kontext-dev",    # v6 API
            "flux-kontext-pro",    # v7 API
            "seedream-4",          # v7 API
            "nano-banana"          # v7 API
        ]
        
        # Schedulers
        self.schedulers = [
            "DPMSolverMultistepScheduler",
            "EulerAncestralDiscreteScheduler",
            "EulerDiscreteScheduler",
            "DDIMScheduler",
            "PNDMScheduler"
        ]
        
        # Load environment variables
        self.load_env()
        
        self.init_image_path = None
        self.init_image_2_path = None
        self.result_image = None
        
        self.setup_ui()
        
    def load_env(self):
        """Load API keys and settings from .env file"""
        self.api_key = ""
        self.imgbb_api_key = ""
        self.last_prompt = "a girl from image one holding the can from image two"
        self.last_negative_prompt = "(worst quality:2), (low quality:2), (normal quality:2), (jpeg artifacts), (blurry), (duplicate), (morbid), (mutilated), (out of frame), (extra limbs), (bad anatomy), (disfigured), (deformed), (cross-eye), (glitch), (oversaturated), (overexposed), (underexposed), (bad proportions), (bad hands), (bad feet), (cloned face), (long neck), (missing arms), (missing legs), (extra fingers), (fused fingers), (poorly drawn hands), (poorly drawn face), (mutation), (deformed eyes), watermark, text, logo, signature, grainy, tiling, censored, nsfw, ugly, blurry eyes, noisy image, bad lighting, unnatural skin, asymmetry"
        self.last_model = self.models[0]
        self.last_scheduler = self.schedulers[0]
        self.last_steps = "28"
        self.last_strength = "0.7"
        self.last_guidance = "2.5"
        self.last_enhance_prompt = False
        
        env_path = os.path.join(os.path.dirname(__file__), '.env')
        if os.path.exists(env_path):
            with open(env_path, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.startswith('MODELSLAB_API_KEY='):
                        self.api_key = line.split('=', 1)[1].strip()
                    elif line.startswith('IMGBB_API_KEY='):
                        self.imgbb_api_key = line.split('=', 1)[1].strip()
                    elif line.startswith('LAST_PROMPT='):
                        self.last_prompt = line.split('=', 1)[1].strip()
                    elif line.startswith('LAST_NEGATIVE_PROMPT='):
                        self.last_negative_prompt = line.split('=', 1)[1].strip()
                    elif line.startswith('LAST_MODEL='):
                        model = line.split('=', 1)[1].strip()
                        if model in self.models:
                            self.last_model = model
                    elif line.startswith('LAST_SCHEDULER='):
                        scheduler = line.split('=', 1)[1].strip()
                        if scheduler in self.schedulers:
                            self.last_scheduler = scheduler
                    elif line.startswith('LAST_STEPS='):
                        self.last_steps = line.split('=', 1)[1].strip()
                    elif line.startswith('LAST_STRENGTH='):
                        self.last_strength = line.split('=', 1)[1].strip()
                    elif line.startswith('LAST_GUIDANCE='):
                        self.last_guidance = line.split('=', 1)[1].strip()
                    elif line.startswith('LAST_ENHANCE_PROMPT='):
                        self.last_enhance_prompt = line.split('=', 1)[1].strip().lower() == 'true'
    
    def save_env(self):
        """Save API keys and settings to .env file"""
        env_path = os.path.join(os.path.dirname(__file__), '.env')
        with open(env_path, 'w', encoding='utf-8') as f:
            f.write(f'MODELSLAB_API_KEY={self.api_key}\n')
            f.write(f'IMGBB_API_KEY={self.imgbb_api_key}\n')
            f.write(f'LAST_PROMPT={self.last_prompt}\n')
            f.write(f'LAST_NEGATIVE_PROMPT={self.last_negative_prompt}\n')
            f.write(f'LAST_MODEL={self.last_model}\n')
            f.write(f'LAST_SCHEDULER={self.last_scheduler}\n')
            f.write(f'LAST_STEPS={self.last_steps}\n')
            f.write(f'LAST_STRENGTH={self.last_strength}\n')
            f.write(f'LAST_GUIDANCE={self.last_guidance}\n')
            f.write(f'LAST_ENHANCE_PROMPT={self.last_enhance_prompt}\n')
    
    def setup_ui(self):
        # Create notebook for tabs
        notebook = ttk.Notebook(self.root)
        notebook.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Main tab
        main_frame = ttk.Frame(notebook)
        notebook.add(main_frame, text="Image Editing")
        
        # Logs tab
        logs_frame = ttk.Frame(notebook)
        notebook.add(logs_frame, text="Logs")
        
        self.setup_main_tab(main_frame)
        self.setup_logs_tab(logs_frame)
    
    def setup_main_tab(self, parent):
        # Create scrollable frame
        canvas = tk.Canvas(parent)
        scrollbar = ttk.Scrollbar(parent, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)
        
        scrollable_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        
        main_frame = scrollable_frame
        
        # API Key section
        api_frame = ttk.LabelFrame(main_frame, text="API Configuration", padding="10")
        api_frame.pack(fill=tk.X, pady=5)
        
        ttk.Label(api_frame, text="Modelslab API Key:").grid(row=0, column=0, sticky=tk.W, pady=5)
        self.api_key_var = tk.StringVar(value=self.api_key)
        api_key_entry = ttk.Entry(api_frame, textvariable=self.api_key_var, width=50, show="*")
        api_key_entry.grid(row=0, column=1, columnspan=2, sticky=(tk.W, tk.E), pady=5, padx=5)
        
        ttk.Label(api_frame, text="imgbb API Key:").grid(row=1, column=0, sticky=tk.W, pady=5)
        self.imgbb_api_key_var = tk.StringVar(value=self.imgbb_api_key)
        imgbb_key_entry = ttk.Entry(api_frame, textvariable=self.imgbb_api_key_var, width=50, show="*")
        imgbb_key_entry.grid(row=1, column=1, columnspan=2, sticky=(tk.W, tk.E), pady=5, padx=5)
        
        ttk.Button(api_frame, text="Save API Keys", command=self.save_api_keys).grid(row=0, column=3, rowspan=2, padx=5, pady=5)
        
        # Image Upload section
        image_frame = ttk.LabelFrame(main_frame, text="Image Upload", padding="10")
        image_frame.pack(fill=tk.X, pady=5)
        
        # Init Image 1 (Required)
        ttk.Label(image_frame, text="Init Image 1 (Required):").grid(row=0, column=0, sticky=tk.W, pady=5)
        self.init_image_label = ttk.Label(image_frame, text="No image selected")
        self.init_image_label.grid(row=0, column=1, sticky=tk.W, padx=5)
        ttk.Button(image_frame, text="Browse", command=self.browse_init_image).grid(row=0, column=2, padx=5)
        
        # Init Image 2 (Optional)
        ttk.Label(image_frame, text="Init Image 2 (Optional):").grid(row=1, column=0, sticky=tk.W, pady=5)
        self.init_image_2_label = ttk.Label(image_frame, text="No image selected")
        self.init_image_2_label.grid(row=1, column=1, sticky=tk.W, padx=5)
        ttk.Button(image_frame, text="Browse", command=self.browse_init_image_2).grid(row=1, column=2, padx=5)
        ttk.Button(image_frame, text="Clear", command=self.clear_init_image_2).grid(row=1, column=3, padx=5)
        
        # Parameters section
        params_frame = ttk.LabelFrame(main_frame, text="Generation Parameters", padding="10")
        params_frame.pack(fill=tk.X, pady=5)
        
        # Prompt
        ttk.Label(params_frame, text="Prompt:").grid(row=0, column=0, sticky=(tk.W, tk.N), pady=5)
        self.prompt_text = scrolledtext.ScrolledText(params_frame, width=60, height=3)
        self.prompt_text.grid(row=0, column=1, columnspan=3, sticky=(tk.W, tk.E), pady=5, padx=5)
        self.prompt_text.insert("1.0", self.last_prompt)
        
        # Negative Prompt
        ttk.Label(params_frame, text="Negative Prompt:").grid(row=1, column=0, sticky=(tk.W, tk.N), pady=5)
        self.negative_prompt_text = scrolledtext.ScrolledText(params_frame, width=60, height=3)
        self.negative_prompt_text.grid(row=1, column=1, columnspan=3, sticky=(tk.W, tk.E), pady=5, padx=5)
        self.negative_prompt_text.insert("1.0", self.last_negative_prompt)
        
        # Model selection
        ttk.Label(params_frame, text="Model:").grid(row=2, column=0, sticky=tk.W, pady=5)
        self.model_var = tk.StringVar(value=self.last_model)
        model_combo = ttk.Combobox(params_frame, textvariable=self.model_var, values=self.models, state="readonly")
        model_combo.grid(row=2, column=1, sticky=(tk.W, tk.E), pady=5, padx=5)
        
        # Scheduler
        ttk.Label(params_frame, text="Scheduler:").grid(row=2, column=2, sticky=tk.W, pady=5, padx=(20, 0))
        self.scheduler_var = tk.StringVar(value=self.last_scheduler)
        scheduler_combo = ttk.Combobox(params_frame, textvariable=self.scheduler_var, values=self.schedulers, state="readonly")
        scheduler_combo.grid(row=2, column=3, sticky=(tk.W, tk.E), pady=5, padx=5)
        
        # Inference Steps
        ttk.Label(params_frame, text="Inference Steps:").grid(row=3, column=0, sticky=tk.W, pady=5)
        self.steps_var = tk.StringVar(value=self.last_steps)
        steps_entry = ttk.Entry(params_frame, textvariable=self.steps_var, width=10)
        steps_entry.grid(row=3, column=1, sticky=tk.W, pady=5, padx=5)
        
        # Strength
        ttk.Label(params_frame, text="Strength:").grid(row=3, column=2, sticky=tk.W, pady=5, padx=(20, 0))
        self.strength_var = tk.StringVar(value=self.last_strength)
        strength_entry = ttk.Entry(params_frame, textvariable=self.strength_var, width=10)
        strength_entry.grid(row=3, column=3, sticky=tk.W, pady=5, padx=5)
        
        # Guidance
        ttk.Label(params_frame, text="Guidance:").grid(row=4, column=0, sticky=tk.W, pady=5)
        self.guidance_var = tk.StringVar(value=self.last_guidance)
        guidance_entry = ttk.Entry(params_frame, textvariable=self.guidance_var, width=10)
        guidance_entry.grid(row=4, column=1, sticky=tk.W, pady=5, padx=5)
        
        # Enhance Prompt
        self.enhance_prompt_var = tk.BooleanVar(value=self.last_enhance_prompt)
        enhance_check = ttk.Checkbutton(params_frame, text="Enhance Prompt", variable=self.enhance_prompt_var)
        enhance_check.grid(row=4, column=2, sticky=tk.W, pady=5, padx=(20, 0))
        
        # Model Info
        ttk.Label(params_frame, text="Note: Different models use different API versions").grid(row=5, column=0, columnspan=2, sticky=tk.W, pady=5)
        ttk.Label(params_frame, text="v6: flux-kontext-dev | v7: flux-kontext-pro, seedream-4, nano-banana").grid(row=6, column=0, columnspan=4, sticky=tk.W, pady=2)
        
        # Control buttons
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill=tk.X, pady=10)
        
        self.generate_btn = ttk.Button(button_frame, text="Generate Image", command=self.generate_image)
        self.generate_btn.pack(side=tk.LEFT, padx=5)
        
        self.save_btn = ttk.Button(button_frame, text="Save Result", command=self.save_result, state="disabled")
        self.save_btn.pack(side=tk.LEFT, padx=5)
        
        ttk.Button(button_frame, text="Clear Logs", command=self.clear_logs).pack(side=tk.LEFT, padx=5)
        
        # Progress bar
        self.progress = ttk.Progressbar(main_frame, mode='indeterminate')
        self.progress.pack(fill=tk.X, pady=5)
        
        # Result display
        result_frame = ttk.LabelFrame(main_frame, text="Result", padding="10")
        result_frame.pack(fill=tk.BOTH, expand=True, pady=5)
        
        self.result_label = ttk.Label(result_frame, text="Generated image will appear here")
        self.result_label.pack(pady=20)
        
        # Configure grid weights
        params_frame.columnconfigure(1, weight=1)
        params_frame.columnconfigure(3, weight=1)
    
    def setup_logs_tab(self, parent):
        # Request log
        ttk.Label(parent, text="Request Log:").pack(anchor=tk.W, pady=(10, 5))
        self.request_log = scrolledtext.ScrolledText(parent, height=15)
        self.request_log.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)
        
        # Response log
        ttk.Label(parent, text="Response Log:").pack(anchor=tk.W, pady=(10, 5))
        self.response_log = scrolledtext.ScrolledText(parent, height=15)
        self.response_log.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)
    
    def save_api_keys(self):
        """Save API keys to environment and .env file"""
        self.api_key = self.api_key_var.get()
        self.imgbb_api_key = self.imgbb_api_key_var.get()
        self.save_env()
        messagebox.showinfo("Success", "API keys saved successfully!")
    
    def save_current_settings(self):
        """Save current UI settings"""
        self.last_prompt = self.prompt_text.get("1.0", tk.END).strip()
        self.last_negative_prompt = self.negative_prompt_text.get("1.0", tk.END).strip()
        self.last_model = self.model_var.get()
        self.last_scheduler = self.scheduler_var.get()
        self.last_steps = self.steps_var.get()
        self.last_strength = self.strength_var.get()
        self.last_guidance = self.guidance_var.get()
        self.last_enhance_prompt = self.enhance_prompt_var.get()
        self.save_env()
    
    def browse_init_image(self):
        """Browse for init image 1"""
        file_path = filedialog.askopenfilename(
            title="Select Init Image 1",
            filetypes=[("Image files", "*.jpg *.jpeg *.png *.bmp *.gif *.webp")]
        )
        if file_path:
            self.init_image_path = file_path
            self.init_image_label.config(text=os.path.basename(file_path))
    
    def browse_init_image_2(self):
        """Browse for init image 2"""
        file_path = filedialog.askopenfilename(
            title="Select Init Image 2 (Optional)",
            filetypes=[("Image files", "*.jpg *.jpeg *.png *.bmp *.gif *.webp")]
        )
        if file_path:
            self.init_image_2_path = file_path
            self.init_image_2_label.config(text=os.path.basename(file_path))
    
    def clear_init_image_2(self):
        """Clear init image 2"""
        self.init_image_2_path = None
        self.init_image_2_label.config(text="No image selected")
    
    def upload_image_to_cloud(self, image_path):
        """Upload image to cloud and return URL"""
        try:
            # Process image to meet Modelslab requirements
            with Image.open(image_path) as img:
                # Convert to RGB if necessary
                if img.mode != 'RGB':
                    img = img.convert('RGB')
                
                # Resize if too large (max 1500x1500 or 2.25M pixels)
                width, height = img.size
                max_pixels = 2250000
                max_dimension = 1500
                
                if width * height > max_pixels or width > max_dimension or height > max_dimension:
                    # Calculate new size maintaining aspect ratio
                    ratio = min(max_dimension / width, max_dimension / height)
                    new_width = int(width * ratio)
                    new_height = int(height * ratio)
                    
                    # Ensure total pixels don't exceed limit
                    if new_width * new_height > max_pixels:
                        pixel_ratio = (max_pixels / (new_width * new_height)) ** 0.5
                        new_width = int(new_width * pixel_ratio)
                        new_height = int(new_height * pixel_ratio)
                    
                    img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
                
                # Save as PNG
                buffer = io.BytesIO()
                img.save(buffer, format='PNG')
                image_data = buffer.getvalue()
            
            # Upload to imgbb (free image hosting)
            upload_url = "https://api.imgbb.com/1/upload"
            
            # Use imgbb API key from .env file
            if not self.imgbb_api_key:
                self.log_response("Error: imgbb API key not found. Please add IMGBB_API_KEY to .env file")
                return None
            
            api_key = self.imgbb_api_key
            
            # Convert to base64 for upload
            base64_data = base64.b64encode(image_data).decode('utf-8')
            
            payload = {
                'key': api_key,
                'image': base64_data,
                'expiration': 600  # 10 minutes
            }
            
            response = requests.post(upload_url, data=payload, timeout=30)
            
            if response.status_code == 200:
                result = response.json()
                if result.get('success'):
                    return result['data']['url']
                else:
                    self.log_response(f"Upload failed: {result.get('error', {}).get('message', 'Unknown error')}")
                    return None
            else:
                self.log_response(f"Upload failed with status: {response.status_code}")
                return None
                
        except Exception as e:
            self.log_response(f"Error uploading image: {str(e)}")
            return None
    
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
    
    def poll_modelslab_result(self, initial_response, max_attempts=60):
        """Poll Modelslab API for async result"""
        try:
            # Extract polling information
            task_id = initial_response.get('id')
            fetch_url = initial_response.get('fetch_result')
            estimated_time = initial_response.get('estimated_time', 10)
            
            if not task_id or not fetch_url:
                return None
            
            self.root.after(0, lambda: self.log_response(f"Starting polling for task: {task_id}, estimated time: {estimated_time}s"))
            
            # Wait initial estimated time
            import time
            time.sleep(min(estimated_time, 30))  # Cap at 30 seconds
            
            attempts = 0
            while attempts < max_attempts:
                try:
                    # Poll the fetch endpoint (POST method required with API key)
                    poll_data = {"key": self.api_key_var.get()}
                    poll_response = requests.post(fetch_url, json=poll_data, timeout=30)
                    
                    self.root.after(0, lambda a=attempts+1: self.log_response(f"Poll attempt {a}/{max_attempts}, status: {poll_response.status_code}"))
                    
                    if poll_response.status_code == 200:
                        result = poll_response.json()
                        status = result.get('status', '')
                        
                        if status == 'success':
                            self.root.after(0, lambda: self.log_response(f"Task completed successfully: {json.dumps(result, indent=2)}"))
                            return result
                        elif status == 'processing' or status == 'queued':
                            self.root.after(0, lambda s=status: self.log_response(f"Task status: {s}, continuing to poll..."))
                        elif status == 'failed' or status == 'error':
                            error_msg = result.get('message', 'Task failed')
                            self.root.after(0, lambda e=error_msg: self.log_response(f"Task failed: {e}"))
                            return None
                    else:
                        self.root.after(0, lambda c=poll_response.status_code: self.log_response(f"Poll request failed with status: {c}"))
                    
                    attempts += 1
                    if attempts < max_attempts:
                        time.sleep(5)  # Wait 5 seconds between polls
                        
                except Exception as e:
                    self.root.after(0, lambda err=str(e): self.log_response(f"Poll attempt error: {err}"))
                    attempts += 1
                    if attempts < max_attempts:
                        time.sleep(5)
            
            self.root.after(0, lambda: self.log_response(f"Polling timed out after {max_attempts * 5} seconds"))
            return None
            
        except Exception as e:
            self.root.after(0, lambda err=str(e): self.log_response(f"Polling error: {err}"))
            return None
    
    def clear_logs(self):
        """Clear both log windows"""
        self.request_log.delete(1.0, tk.END)
        self.response_log.delete(1.0, tk.END)
    
    def generate_image(self):
        """Generate image using Modelslab API"""
        if not self.api_key_var.get():
            messagebox.showerror("Error", "Please enter your Modelslab API key!")
            return
        
        if not self.init_image_path:
            messagebox.showerror("Error", "Please select an init image!")
            return
        
        prompt = self.prompt_text.get("1.0", tk.END).strip()
        if not prompt:
            messagebox.showerror("Error", "Please enter a prompt!")
            return
        
        # Save current settings before generation
        self.save_current_settings()
        
        # Start generation in separate thread
        threading.Thread(target=self._generate_image_thread, daemon=True).start()
    
    def _generate_image_thread(self):
        """Generate image in separate thread"""
        try:
            self.root.after(0, lambda: self.progress.start())
            self.root.after(0, lambda: self.generate_btn.config(state="disabled"))
            
            # Upload images to cloud and get URLs
            self.root.after(0, lambda: self.log_response("Uploading init image to cloud..."))
            init_image_url = self.upload_image_to_cloud(self.init_image_path)
            if not init_image_url:
                self.root.after(0, lambda: messagebox.showerror("Error", "Failed to upload init image"))
                return
            
            init_image_2_url = ""
            if self.init_image_2_path:
                self.root.after(0, lambda: self.log_response("Uploading second image to cloud..."))
                init_image_2_url = self.upload_image_to_cloud(self.init_image_2_path)
                if not init_image_2_url:
                    self.root.after(0, lambda: messagebox.showerror("Error", "Failed to upload second image"))
                    return
            
            # Prepare model-specific request data
            model_id = self.model_var.get()
            prompt = self.prompt_text.get("1.0", tk.END).strip()
            api_key = self.api_key_var.get()
            
            if model_id == "flux-kontext-pro":
                url = "https://modelslab.com/api/v7/images/image-to-image"
                # Calculate aspect ratio from init image
                aspect_ratio = self.calculate_aspect_ratio(init_image_url)
                data = {
                    "init_image": init_image_url,
                    "prompt": prompt,
                    "model_id": model_id,
                    "aspect_ratio": aspect_ratio,
                    "key": api_key
                }
            elif model_id == "seedream-4":
                url = "https://modelslab.com/api/v7/images/image-to-image"
                data = {
                    "init_image": [init_image_url],  # Array format as per Android implementation
                    "prompt": prompt,
                    "model_id": model_id,
                    "key": api_key
                }
            elif model_id == "nano-banana":
                url = "https://modelslab.com/api/v7/images/image-to-image"
                data = {
                    "prompt": prompt,
                    "model_id": model_id,
                    "init_image": init_image_url,
                    "key": api_key
                }
            else:  # flux-kontext-dev and others
                url = "https://modelslab.com/api/v6/images/img2img"
                data = {
                    "init_image": init_image_url,
                    "init_image_2": init_image_2_url,
                    "prompt": prompt,
                    "negative_prompt": self.negative_prompt_text.get("1.0", tk.END).strip(),
                    "model_id": model_id,
                    "num_inference_steps": self.steps_var.get(),
                    "strength": self.strength_var.get(),
                    "scheduler": self.scheduler_var.get(),
                    "guidance": self.guidance_var.get(),
                    "enhance_prompt": self.enhance_prompt_var.get(),
                    "base64": "no",
                    "key": api_key
                }
            
            headers = {
                "Content-Type": "application/json"
            }
            
            # Log request (hide API key)
            request_info = data.copy()
            request_info["key"] = "***HIDDEN***"
            
            request_log_data = {
                "url": url,
                "headers": headers,
                "data": request_info
            }
            self.root.after(0, lambda: self.log_request(request_log_data))
            
            # Make API request
            response = requests.post(url, json=data, headers=headers, timeout=120)
            
            # Log response
            response_info = f"Status Code: {response.status_code}\n"
            response_info += f"Headers: {dict(response.headers)}\n"
            
            if response.status_code == 200:
                response_json = response.json()
                response_info += f"Response: {json.dumps(response_json, indent=2)}"
                
                # Universal async/sync handling for all models
                self.root.after(0, lambda: self.handle_modelslab_response(response_json))
                    
            else:
                response_info += f"Error: {response.text}"
                self.root.after(0, lambda: messagebox.showerror("Error", f"API Error ({response.status_code}): {response.text}"))
            
            self.root.after(0, lambda: self.log_response(response_info))
            
        except Exception as e:
            error_msg = f"Error generating image: {str(e)}"
            self.root.after(0, lambda: self.log_response(error_msg))
            self.root.after(0, lambda: messagebox.showerror("Error", error_msg))
        
        finally:
            self.root.after(0, lambda: self.progress.stop())
            self.root.after(0, lambda: self.generate_btn.config(state="normal"))
    
    def download_and_display_image(self, image_url):
        """Download and display the generated image from URL"""
        try:
            # Download image from URL
            response = requests.get(image_url, timeout=30)
            response.raise_for_status()
            image_data = response.content
            
            # Open and display image
            image = Image.open(io.BytesIO(image_data))
            
            # Resize for display (max 400x400)
            display_size = (400, 400)
            image.thumbnail(display_size, Image.Resampling.LANCZOS)
            
            # Convert to PhotoImage
            photo = ImageTk.PhotoImage(image)
            
            # Store references
            self.result_image = image_data
            
            # Update UI
            self.root.after(0, lambda: self.result_label.config(image=photo, text=""))
            self.root.after(0, lambda: setattr(self.result_label, 'image', photo))  # Keep reference
            self.root.after(0, lambda: self.save_btn.config(state="normal"))
            self.root.after(0, lambda: messagebox.showinfo("Success", "Image generated successfully!"))
            
        except Exception as e:
            error_msg = f"Error downloading/displaying image: {str(e)}"
            self.root.after(0, lambda: self.log_response(error_msg))
            self.root.after(0, lambda: messagebox.showerror("Error", error_msg))
    
    def handle_modelslab_response(self, response_json):
        """Handle both immediate and async responses for all ModelsLab models"""
        try:
            status = response_json.get('status', '')
            
            # Check if response is async (requires polling)
            is_async = (status != 'success' and ("fetch_result" in response_json or "id" in response_json))
            
            if is_async:
                self.log_response(f"Async response detected (status: {status}), starting polling...")
                
                # Poll for result
                poll_result = self.poll_modelslab_result(response_json)
                
                if poll_result and poll_result.get('status') == 'success':
                    # Extract image URL from poll result
                    image_url = self.extract_image_url(poll_result)
                    if image_url:
                        self.download_and_display_image(image_url)
                    else:
                        messagebox.showerror("Error", "No image URL in poll result")
                else:
                    messagebox.showerror("Error", "Polling failed or timed out")
            else:
                # Handle immediate response (sync)
                self.log_response(f"Immediate response detected (status: {status})")
                image_url = self.extract_image_url(response_json)
                if image_url:
                    self.download_and_display_image(image_url)
                else:
                    messagebox.showerror("Error", "No image URL in immediate response")
                    
        except Exception as e:
            self.log_response(f"Error handling response: {str(e)}")
            messagebox.showerror("Error", f"Response handling error: {str(e)}")
    
    def extract_image_url(self, response_json):
        """Extract image URL from response JSON (works for both sync and async responses)"""
        try:
            # Try different response formats
            if "output" in response_json:
                output = response_json["output"]
                if isinstance(output, list) and len(output) > 0:
                    return output[0]
                elif isinstance(output, str) and output:
                    return output
            
            # Fallback for other formats
            if "image" in response_json:
                return response_json["image"]
            
            # Check future_links as backup
            if "future_links" in response_json:
                future_links = response_json["future_links"]
                if isinstance(future_links, list) and len(future_links) > 0:
                    return future_links[0]
            
            return None
            
        except Exception as e:
            self.log_response(f"Error extracting image URL: {str(e)}")
            return None
    
    def calculate_aspect_ratio(self, image_url):
        """Calculate aspect ratio from image URL for flux-kontext-pro"""
        try:
            # Download image to get dimensions
            response = requests.get(image_url, timeout=30)
            if response.status_code == 200:
                image = Image.open(io.BytesIO(response.content))
                width, height = image.size
                
                # Calculate GCD for aspect ratio
                def gcd(a, b):
                    while b:
                        a, b = b, a % b
                    return a
                
                divisor = gcd(width, height)
                ratio_w = width // divisor
                ratio_h = height // divisor
                
                return f"{ratio_w}:{ratio_h}"
        except Exception as e:
            self.log_response(f"Error calculating aspect ratio: {str(e)}")
        
        return "1:1"  # Default aspect ratio
    
    def save_result(self):
        """Save the generated image"""
        if not self.result_image:
            messagebox.showerror("Error", "No image to save!")
            return
        
        file_path = filedialog.asksaveasfilename(
            title="Save Generated Image",
            defaultextension=".png",
            filetypes=[
                ("PNG files", "*.png"),
                ("JPEG files", "*.jpg"),
                ("All files", "*.*")
            ]
        )
        
        if file_path:
            try:
                with open(file_path, 'wb') as f:
                    f.write(self.result_image)
                messagebox.showinfo("Success", f"Image saved to {file_path}")
            except Exception as e:
                messagebox.showerror("Error", f"Error saving image: {str(e)}")

def main():
    root = tk.Tk()
    app = ModelslabImageEditGUI(root)
    root.mainloop()

if __name__ == "__main__":
    main()
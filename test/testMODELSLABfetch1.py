import requests
import json
import tkinter as tk
from tkinter import ttk, messagebox, TclError
import webbrowser
import logging
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import time
import csv
import os
import re
from PIL import Image, ImageTk, ImageDraw, ImageFont
import io
import threading
import pygame
import urllib.request

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("modelslab_gui.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class DataManager:
    """Handles saving and loading of data like models, settings, and generated content."""
    def __init__(self, db_file="models_database.json", settings_file="settings.json", content_file="generated_content.json"):
        self.db_file = db_file
        self.settings_file = settings_file
        self.content_file = content_file

    def load_json(self, file_path):
        if not os.path.exists(file_path):
            return {}
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except (json.JSONDecodeError, IOError) as e:
            logger.error(f"Error loading {file_path}: {e}")
            return {}

    def save_json(self, data, file_path):
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=4)
        except IOError as e:
            logger.error(f"Error saving to {file_path}: {e}")

    def load_models(self):
        data = self.load_json(self.db_file)
        return data.get("models", [])

    def save_models(self, models):
        self.save_json({"models": models}, self.db_file)

    def load_settings(self):
        return self.load_json(self.settings_file)

    def save_settings(self, settings):
        self.save_json(settings, self.settings_file)

    def load_generated_content(self):
        return self.load_json(self.content_file)

    def save_generated_content(self, content):
        self.save_json(content, self.content_file)

class ApiHandler:
    """Handles all API interactions with modelslab.com."""
    def __init__(self, api_key):
        self.API_KEY = api_key
        self.headers = {"Authorization": f"Bearer {self.API_KEY}", "Content-Type": "application/json"}
        self.session = self._create_session()
        self.ENDPOINTS = {
            "model_list": "https://modelslab.com/api/v4/dreambooth/model_list",
            "text2img": "https://modelslab.com/api/v6/images/text2img",
            "chat": "https://modelslab.com/api/v6/llm/uncensored_chat",
            "text_to_audio": "https://modelslab.com/api/v6/voice/text_to_audio",
            "text_to_speech": "https://modelslab.com/api/v6/voice/text_to_speech",
            "voice_to_voice": "https://modelslab.com/api/v6/voice/voice_to_voice",
            "text2video": "https://modelslab.com/api/v6/video/text2video",
            "img2video": "https://modelslab.com/api/v6/video/img2video",
            "voice_list": "https://modelslab.com/api/v6/voice/voice_list",
            "music_gen": "https://modelslab.com/api/v6/voice/music_gen",
            "song_generator": "https://modelslab.com/api/v6/voice/song_generator"
        }

    def _create_session(self):
        session = requests.Session()
        retries = Retry(total=3, backoff_factor=1, status_forcelist=[429, 500, 502, 503, 504])
        session.mount("https://", HTTPAdapter(max_retries=retries))
        return session

    def _post(self, endpoint_key, payload):
        url = self.ENDPOINTS.get(endpoint_key, endpoint_key)
        payload['key'] = self.API_KEY
        response = self.session.post(url, headers=self.headers, data=json.dumps(payload), timeout=30)
        response.raise_for_status()
        return response.json()

    def fetch_models_from_api(self):
        payload = {"limit": 500}
        return self._post("model_list", payload)

    def fetch_voice_list(self):  # Line ~89
        """Fetch the list of available voice IDs from the API."""
        payload = {"key": self.API_KEY}
        return self._post("voice_list", payload)

    def generate_image(self, payload):
        return self._post("text2img", payload)
    
    def send_chat(self, payload):
        return self._post("chat", payload)

    def generate_audio(self, audio_type, payload):
        return self._post(audio_type, payload)

    def generate_video(self, route, payload):
        return self._post(route, payload)

class ModelsLabGUI:
    def __init__(self, root):
        self.root = root
        
        # Core Components
        self.data_manager = DataManager()
        self.api_handler = ApiHandler("RpcCfnn8RHAROrjkq9SBFHvwqxqCgzEtNb2NFg6gZCeJSxgGRiXDonfGmoun")

        # Load Data and Settings
        self.settings = self.data_manager.load_settings()
        self.generated_content = self.data_manager.load_generated_content()

        # GUI Configuration
        self.root.title("ModelsLab Enhanced GUI")
        self.root.geometry(self.settings.get("geometry", "1200x900"))
        self.root.resizable(True, True)
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)

        # State Variables
        self.is_processing = False
        self.models = []
        self.image_models = []
        self.lora_models = []
        self.audiogen_models = []
        self.llmaster_models = []
        self.videofusion_models = []
        self.voices = []  # Store available voice IDs
        self.sort_order = {}
        self.voice_id_var = tk.StringVar(value="default")  # Default voice_id

        # CSV and filter settings
        self.csv_fields = {
            "Model ID": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Model ID", True)),
            "Model Name": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Model Name", True)),
            "Model Category": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Model Category", True)),
            "Description": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Description", True)),
            "Instance Prompt": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Instance Prompt", True)),
            "Screenshot URL": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Screenshot URL", True)),
            "Is NSFW": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Is NSFW", True)),
            "Feature": tk.BooleanVar(value=self.settings.get("csv_fields", {}).get("Feature", True))
        }
        self.nsfw_filter = {
            "image": tk.StringVar(value=self.settings.get("nsfw_filter", {}).get("image", "All")),
            "lora": tk.StringVar(value=self.settings.get("nsfw_filter", {}).get("lora", "All"))
        }
        self.show_image_var = tk.BooleanVar(value=self.settings.get("show_image", True))

        # Image and media handling
        self.placeholder_image = self.create_placeholder_image()
        pygame.mixer.init()
        
        # Initialize GUI
        self.create_gui()
        self.load_models_from_db()
        self.load_persistent_content()

    def on_closing(self):
        """Handle saving settings and content before closing the app."""
        logger.info("Closing application and saving settings.")
        # Save settings
        self.settings["geometry"] = self.root.geometry()
        self.settings["show_image"] = self.show_image_var.get()
        self.settings["csv_fields"] = {field: var.get() for field, var in self.csv_fields.items()}
        self.settings["nsfw_filter"] = {ftype: var.get() for ftype, var in self.nsfw_filter.items()}
        self.data_manager.save_settings(self.settings)

        # Save generated content
        self.data_manager.save_generated_content(self.generated_content)
        
        self.root.destroy()

    def create_placeholder_image(self):
        try:
            img = Image.new('RGB', (300, 150), color='gray')
            draw = ImageDraw.Draw(img)
            try:
                font = ImageFont.load_default()
                text = "Image unavailable"
                text_bbox = draw.textbbox((0, 0), text, font=font)
                text_width = text_bbox[2] - text_bbox[0]
                text_height = text_bbox[3] - text_bbox[1]
                x = (300 - text_width) / 2
                y = (150 - text_height) / 2
                draw.text((x, y), text, fill='white', font=font)
            except Exception:
                draw.text((10, 75), "Image unavailable", fill='white')
            return ImageTk.PhotoImage(img)
        except Exception as e:
            logger.error(f"Failed to create placeholder image: {e}")
            return None
            
    def load_image_from_url(self, url, label_to_update):
        def task():
            try:
                if not url or not url.startswith(("http://", "https://"))):
                    image = self.placeholder_image
                else:
                    response = self.api_handler.session.get(url, timeout=10)
                    response.raise_for_status()
                    img_data = response.content
                    img = Image.open(io.BytesIO(img_data))
                    ratio = img.height / img.width
                    new_height = int(300 * ratio)
                    img = img.resize((300, new_height), Image.Resampling.LANCZOS)
                    image = ImageTk.PhotoImage(img)
                
                label_to_update.configure(image=image)
                label_to_update.image = image
            except Exception as e:
                logger.error(f"Failed to load image from {url}: {e}")
                label_to_update.configure(image=self.placeholder_image)
                label_to_update.image = self.placeholder_image
        
        threading.Thread(target=task, daemon=True).start()

    def create_gui(self):
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.pack(fill="both", expand=True)

        # Top controls
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill="x", pady=5)
        self.fetch_button = ttk.Button(button_frame, text="Fetch Models", command=self.start_fetch_models)
        self.fetch_button.pack(side="left", padx=5)
        ttk.Checkbutton(button_frame, text="Show Image", variable=self.show_image_var).pack(side="left", padx=5)

        # CSV field selection
        csv_frame = ttk.LabelFrame(main_frame, text="Select CSV Fields", padding="5")
        csv_frame.pack(fill="x", pady=5, padx=5)
        for i, (field, var) in enumerate(self.csv_fields.items()):
            ttk.Checkbutton(csv_frame, text=field, variable=var).pack(side="left", padx=5, pady=2)

        self.notebook = ttk.Notebook(main_frame)
        self.notebook.pack(fill="both", expand=True, pady=5)
        
        # Create all tabs
        self.create_model_tab("Image", "image")
        self.create_model_tab("LoRA", "lora")
        self.create_model_tab("Audiogen", "audiogen")
        self.create_model_tab("LLMaster", "llmaster")
        self.create_model_tab("VideoFusion", "videofusion")
        self.create_generation_tab()
        self.create_chat_tab()
        self.create_audio_tab()
        self.create_video_tab()

        # Status Bar
        self.status_var = tk.StringVar(value="Ready")
        ttk.Label(main_frame, textvariable=self.status_var).pack(side="bottom", fill="x", pady=2)
        logger.info("GUI initialized")

    def create_model_tab(self, title, tab_key):
        frame = ttk.Frame(self.notebook, padding="5")
        self.notebook.add(frame, text=f"{title} Models")

        # Main content area (Treeview + Scrollbars)
        inner_frame = ttk.Frame(frame)
        inner_frame.pack(side="left", fill="both", expand=True)

        columns = ("Model ID", "Model Name", "Description", "Instance Prompt", "Is NSFW", "Feature")
        tree = ttk.Treeview(inner_frame, columns=columns, show="headings", selectmode="browse")

        for col in columns:
            tree.heading(col, text=col, command=lambda c=col, t=tree: self.sort_column(t, c, not self.sort_order.get((t, c), False)))
            tree.column(col, width=150, anchor="w")
        
        y_scroll = ttk.Scrollbar(inner_frame, orient="vertical", command=tree.yview)
        x_scroll = ttk.Scrollbar(inner_frame, orient="horizontal", command=tree.xview)
        tree.configure(yscrollcommand=y_scroll.set, xscrollcommand=x_scroll.set)

        y_scroll.pack(side="right", fill="y")
        x_scroll.pack(side="bottom", fill="x")
        tree.pack(side="left", fill="both", expand=True)

        tree.bind("<Double-1>", self.open_screenshot_url)
        
        setattr(self, f"{tab_key}_tree", tree)
        
        # Right-side panel (Filters, Export, Image)
        right_panel = ttk.Frame(frame, width=320)
        right_panel.pack(side="right", fill="y", padx=10)
        right_panel.pack_propagate(False)

        if tab_key in ["image", "lora"]:
            tree.bind("<<TreeviewSelect>>", lambda e, tk=tab_key: self.on_model_select(e, tk))
            nsfw_combo = ttk.Combobox(right_panel, textvariable=self.nsfw_filter[tab_key], values=["All", "True", "False"], state="readonly")
            nsfw_combo.pack(side="top", pady=5, fill="x")
            nsfw_combo.bind("<<ComboboxSelected>>", lambda e, tk=tab_key: self.filter_and_update_treeview(tk))
        else:
            tree.bind("<<TreeviewSelect>>", lambda e, tk=tab_key: self.update_image_preview(e, tk))

        export_button = ttk.Button(right_panel, text=f"Export to CSV", command=lambda tk=tab_key: self.export_to_csv(tk), state="disabled")
        export_button.pack(side="top", pady=5, fill="x")
        setattr(self, f"{tab_key}_export_button", export_button)
        
        image_label = ttk.Label(right_panel, image=self.placeholder_image)
        image_label.pack(side="top", pady=10)
        setattr(self, f"{tab_key}_label", image_label)
    
    def create_generation_tab(self):
        frame = ttk.Frame(self.notebook, padding="10")
        self.notebook.add(frame, text="Image Generation")

        left_panel = ttk.Frame(frame)
        left_panel.pack(side="left", fill="both", expand=True, padx=5)

        ttk.Label(left_panel, text="Prompt:").pack(anchor="w")
        self.gen_prompt = tk.Text(left_panel, height=4)
        self.gen_prompt.pack(fill="x", pady=2)
        
        ttk.Label(left_panel, text="Negative Prompt:").pack(anchor="w", pady=(5,0))
        self.gen_neg_prompt = tk.Text(left_panel, height=4)
        self.gen_neg_prompt.pack(fill="x", pady=2)

        # Model selection display
        self.gen_image_model_var = tk.StringVar(value="Select model from Image Models tab")
        ttk.Label(left_panel, text="Image Model:").pack(anchor="w", pady=(5,0))
        ttk.Label(left_panel, textvariable=self.gen_image_model_var, relief="sunken").pack(fill="x", pady=2)
        
        # LoRA controls
        lora_frame = ttk.Frame(left_panel)
        lora_frame.pack(fill="x", pady=5)
        self.use_lora_var = tk.BooleanVar(value=True)
        self.use_lora_check = ttk.Checkbutton(lora_frame, text="Use LoRA Model", variable=self.use_lora_var, command=self.toggle_lora_controls)
        self.use_lora_check.pack(side="left")
        
        self.gen_lora_model_var = tk.StringVar(value="Select model from LoRA Models tab")
        ttk.Label(left_panel, text="LoRA Model:").pack(anchor="w")
        self.lora_model_label = ttk.Label(left_panel, textvariable=self.gen_lora_model_var, relief="sunken")
        self.lora_model_label.pack(fill="x", pady=2)
        
        lora_strength_frame = ttk.Frame(left_panel)
        lora_strength_frame.pack(fill="x", pady=5)
        ttk.Label(lora_strength_frame, text="LoRA Strength:").pack(side="left")
        self.lora_strength_var = tk.StringVar(value="0.5")
        self.lora_strength_entry = ttk.Entry(lora_strength_frame, textvariable=self.lora_strength_var, width=10)
        self.lora_strength_entry.pack(side="left", padx=5)

        self.gen_button = ttk.Button(left_panel, text="Generate Image", command=self.start_generate_image)
        self.gen_button.pack(pady=10)

        right_panel = ttk.Frame(frame, width=320)
        right_panel.pack(side="right", fill="y", padx=10)
        right_panel.pack_propagate(False)

        self.gen_image_label = ttk.Label(right_panel, image=self.placeholder_image)
        self.gen_image_label.pack(pady=5)
        
        self.toggle_lora_controls()
    
    def create_chat_tab(self):
        frame = ttk.Frame(self.notebook, padding="10")
        self.notebook.add(frame, text="LLM Chat")

        ttk.Label(frame, text="LLMaster Model:").pack(anchor="w")
        self.chat_model = ttk.Combobox(frame, values=[], state="readonly")
        self.chat_model.pack(fill="x", pady=2)

        ttk.Label(frame, text="Message:").pack(anchor="w", pady=(5,0))
        self.chat_input = tk.Text(frame, height=3)
        self.chat_input.pack(fill="x", pady=2)
        
        self.chat_button = ttk.Button(frame, text="Send", command=self.start_send_chat)
        self.chat_button.pack(pady=5)
        
        ttk.Label(frame, text="Response:").pack(anchor="w", pady=(5,0))
        self.chat_output = tk.Text(frame, height=10, state="disabled", wrap="word")
        self.chat_output.pack(fill="both", expand=True, pady=2)

    def create_audio_tab(self):
        frame = ttk.Frame(self.notebook, padding="10")
        self.notebook.add(frame, text="Audio Generation")

        ttk.Label(frame, text="Audiogen Model:").pack(anchor="w")
        self.audio_model = ttk.Combobox(frame, values=[], state="readonly")
        self.audio_model.pack(fill="x", pady=2)

        ttk.Label(frame, text="Text:").pack(anchor="w", pady=(5,0))
        self.audio_input = tk.Text(frame, height=4)
        self.audio_input.pack(fill="x", pady=2)
        
        ttk.Label(frame, text="Initial Audio URL (optional):").pack(anchor="w", pady=(5,0))
        self.audio_init_input = tk.Text(frame, height=2)
        self.audio_init_input.pack(fill="x", pady=2)
        
        ttk.Label(frame, text="Voice ID:").pack(anchor="w", pady=(5,0))
        self.voice_id_combo = ttk.Combobox(frame, textvariable=self.voice_id_var, values=["default"], state="readonly")
        self.voice_id_combo.pack(fill="x", pady=2)
        
        ttk.Label(frame, text="Audio Type:").pack(anchor="w", pady=(5,0))
        self.audio_type = tk.StringVar(value="text_to_audio")
        audio_types = ["text_to_audio", "text_to_speech", "voice_to_voice", "music_gen", "song_generator"]
        ttk.Combobox(frame, textvariable=self.audio_type, values=audio_types, state="readonly").pack(fill="x", pady=2)

        self.audio_button = ttk.Button(frame, text="Generate and Play Audio", command=self.start_generate_audio)
        self.audio_button.pack(pady=10)
        
        self.audio_status_label = ttk.Label(frame, text="Ready")
        self.audio_status_label.pack(pady=5)

        # Fetch voice list on initialization
        self.start_fetch_voice_list()

    def create_video_tab(self):
        frame = ttk.Frame(self.notebook, padding="10")
        self.notebook.add(frame, text="Video Generation")

        ttk.Label(frame, text="VideoFusion Model:").pack(anchor="w")
        self.video_model = ttk.Combobox(frame, values=[], state="readonly")
        self.video_model.pack(fill="x", pady=2)

        ttk.Label(frame, text="Prompt or Image URL:").pack(anchor="w", pady=(5,0))
        self.video_input = tk.Text(frame, height=4)
        self.video_input.pack(fill="x", pady=2)
        
        self.video_button = ttk.Button(frame, text="Generate Video", command=self.start_generate_video)
        self.video_button.pack(pady=10)

        self.video_url_label = ttk.Label(frame, text="Video URL will appear here.", wraplength=400, justify="center")
        self.video_url_label.pack(pady=5)

    def load_models_from_db(self):
        """Loads models from the local JSON database and populates the GUI."""
        self.models = self.data_manager.load_models()
        if not self.models:
            self.status_var.set("No local models found. Please fetch from API.")
            return

        self.image_models = [m for m in self.models if m.get("model_category") == "Image"]
        self.lora_models = [m for m in self.models if m.get("model_category") == "LoRA"]
        self.audiogen_models = [m for m in self.models if m.get("model_category") == "Audiogen"]
        self.llmaster_models = [m for m in self.models if m.get("model_category") == "LLMaster"]
        self.videofusion_models = [m for m in self.models if m.get("model_category") == "VideoFusion"]
        
        logger.info(f"Loaded {len(self.models)} models from database.")
        self.status_var.set(f"Loaded {len(self.models)} models from local database.")
        
        self.update_all_treeviews()
        self.update_all_comboboxes()

    def fetch_models(self):
        """API call to fetch models, executed in a separate thread."""
        try:
            self.status_var.set("Fetching models from API...")
            
            api_data = self.api_handler.fetch_models_from_api()

            if not isinstance(api_data, list):
                logger.error(f"API returned unexpected data type: {type(api_data)}")
                messagebox.showerror("API Error", "Received invalid data from API.")
                return

            self.models = api_data
            self.categorize_models()
            self.data_manager.save_models(self.models)
            
            self.root.after(0, self.update_after_fetch)

        except Exception as e:
            logger.error(f"Failed to fetch models: {e}", exc_info=True)
            self.root.after(0, lambda: messagebox.showerror("Error", f"Failed to fetch models: {e}"))
            self.status_var.set("Failed to fetch models.")
        finally:
            self.is_processing = False
            self.root.after(0, lambda: self.fetch_button.config(state="normal"))
            self.root.after(0, lambda: self.root.config(cursor=""))

    def fetch_voice_list(self):
        """Fetch voice list from API in a separate thread."""
        try:
            self.status_var.set("Fetching voice list...")
            response = self.api_handler.fetch_voice_list()
            
            if response.get("status") == "success" and response.get("voices"):
                self.voices = response["voices"]
                voice_names = [voice.get("name", voice.get("voice_id", "Unknown")) for voice in self.voices]
                self.root.after(0, lambda: self.voice_id_combo.config(values=voice_names))
                if voice_names:
                    self.root.after(0, lambda: self.voice_id_var.set(voice_names[0]))
                self.status_var.set("Voice list fetched successfully.")
            else:
                logger.error(f"Failed to fetch voice list: {response.get('message', 'Unknown error')}")
                self.root.after(0, lambda: messagebox.showerror("API Error", f"Failed to fetch voice list: {response.get('message', 'Unknown error')}"))
                self.status_var.set("Failed to fetch voice list.")

        except Exception as e:
            logger.error(f"Voice list fetch error: {e}", exc_info=True)
            self.root.after(0, lambda: messagebox.showerror("Error", f"Failed to fetch voice list: {e}"))
            self.status_var.set("Failed to fetch voice list.")

    def start_fetch_voice_list(self):
        """Start fetching voice list in a separate thread."""
        self._start_threaded_task(self.fetch_voice_list, self.audio_button)

    def categorize_models(self):
        """Categorize models into different types (Image, LoRA, etc.)."""
        self.image_models.clear()
        self.lora_models.clear()
        self.audiogen_models.clear()
        self.llmaster_models.clear()
        self.videofusion_models.clear()

        for model in self.models:
            if not isinstance(model, dict):
                continue
            name = str(model.get("model_name") or model.get("name") or "").lower()
            desc = str(model.get("description") or "").lower()
            feature = str(model.get("feature") or model.get("features") or "").lower()
            sub_cat = str(model.get("model_subcategory") or "").lower()

            if "lora" in sub_cat or "lora" in name:
                model["model_category"] = "LoRA"
                self.lora_models.append(model)
            elif feature == "audiogen":
                model["model_category"] = "Audiogen"
                self.audiogen_models.append(model)
            elif feature == "llmaster":
                model["model_category"] = "LLMaster"
                self.llmaster_models.append(model)
            elif feature == "videofusion":
                model["model_category"] = "VideoFusion"
                self.videofusion_models.append(model)
            else:
                model["model_category"] = "Image"
                self.image_models.append(model)

    def update_after_fetch(self):
        """Update GUI elements after models have been fetched."""
        self.update_all_treeviews()
        self.update_all_comboboxes()
        messagebox.showinfo("Success", f"Successfully fetched and categorized {len(self.models)} models.")
        self.status_var.set(f"Fetched {len(self.models)} models.")

    def update_all_treeviews(self):
        self.filter_and_update_treeview("image")
        self.filter_and_update_treeview("lora")
        self.filter_and_update_treeview("audiogen")
        self.filter_and_update_treeview("llmaster")
        self.filter_and_update_treeview("videofusion")

    def filter_and_update_treeview(self, tab_key):
        """Populates a treeview based on model type and active filters, ensuring unique item IDs."""
        tree = getattr(self, f"{tab_key}_tree")
        models = getattr(self, f"{tab_key}_models")
        
        tree.delete(*tree.get_children())
        
        used_iids = set()
        nsfw_filter = self.nsfw_filter.get(tab_key, None)
        
        for model in models:
            is_nsfw = str(model.get("is_nsfw", "False"))
            
            if nsfw_filter:
                filter_val = nsfw_filter.get()
                if filter_val != "All" and filter_val != is_nsfw:
                    continue
            
            item_id = model.get('model_id')
            
            if not item_id or item_id in used_iids:
                item_id = f"gen_id_{id(model)}"
            
            if item_id in used_iids:
                continue 
            
            used_iids.add(item_id)
            
            values = (
                model.get("model_id") or model.get("id", "N/A"),
                model.get("model_name") or model.get("name", "N/A"),
                model.get("description", "N/A"),
                model.get("instance_prompt", "N/A"),
                is_nsfw,
                model.get("feature") or model.get("features", "N/A")
            )
            
            tree.insert("", "end", values=values, iid=item_id)
        
        export_button = getattr(self, f"{tab_key}_export_button")
        export_button.config(state="normal" if models else "disabled")
        
    def update_all_comboboxes(self):
        """Updates all comboboxes with the latest model names."""
        def get_name(m): return m.get('model_name') or m.get('name', 'Unknown')
        
        self.chat_model['values'] = [get_name(m) for m in self.llmaster_models]
        if self.llmaster_models: self.chat_model.set(get_name(self.llmaster_models[0]))

        self.audio_model['values'] = [get_name(m) for m in self.audiogen_models]
        if self.audiogen_models: self.audio_model.set(get_name(self.audiogen_models[0]))
        
        self.video_model['values'] = [get_name(m) for m in self.videofusion_models]
        if self.videofusion_models: self.video_model.set(get_name(self.videofusion_models[0]))
    
    def sort_column(self, tree, col, reverse):
        try:
            items = [(tree.set(item, col), item) for item in tree.get_children('')]
            items.sort(reverse=reverse)
            for index, (val, item) in enumerate(items):
                tree.move(item, '', index)
            self.sort_order[(tree, col)] = not reverse
        except Exception as e:
            logger.error(f"Error sorting column {col}: {e}")

    def open_screenshot_url(self, event):
        tree = event.widget
        if not tree.selection():
            return
        item_id = tree.selection()[0]
        model = None
        for m in self.models:
            original_id = m.get('model_id')
            generated_id = f"gen_id_{id(m)}"
            if original_id == item_id or generated_id == item_id:
                model = m
                break

        if model:
            url = model.get("screenshots") or model.get("screenshot_url") or model.get("image_url")
            if url and url != "N/A":
                webbrowser.open(url)
            else:
                messagebox.showwarning("No URL", "No valid screenshot URL available for this model.")

    def on_model_select(self, event, tab_key):
        """Handle selection in Image or LoRA model lists."""
        tree = event.widget
        if not tree.selection():
            return
        
        item_id = tree.selection()[0]
        model_name = tree.item(item_id, "values")[1]

        if tab_key == "image":
            self.gen_image_model_var.set(model_name)
        elif tab_key == "lora":
            self.gen_lora_model_var.set(model_name)
            if not self.use_lora_var.get():
                self.use_lora_var.set(True)
                self.toggle_lora_controls()

        self.update_image_preview(event, tab_key)
        
    def update_image_preview(self, event, tab_key):
        """Update the image preview panel for the selected model."""
        if not self.show_image_var.get():
            return
            
        tree = event.widget
        if not tree.selection():
            return
        
        item_id = tree.selection()[0]
        model = None
        for m in self.models:
            original_id = m.get('model_id')
            generated_id = f"gen_id_{id(m)}"
            if original_id == item_id or generated_id == item_id:
                model = m
                break
        
        if model:
            url = model.get("screenshots") or model.get("screenshot_url") or model.get("image_url")
            label = getattr(self, f"{tab_key}_label")
            self.load_image_from_url(url, label)

    def toggle_lora_controls(self):
        """Enable or disable LoRA-related controls based on the checkbox."""
        state = "normal" if self.use_lora_var.get() else "disabled"
        self.lora_model_label.config(state=state)
        self.lora_strength_entry.config(state=state)

    def export_to_csv(self, model_type):
        models = getattr(self, f"{model_type}_models", [])
        if not models:
            messagebox.showwarning("Export Empty", f"No {model_type} models to export.")
            return

        selected_fields = [field for field, var in self.csv_fields.items() if var.get()]
        if not selected_fields:
            messagebox.showwarning("Export Empty", "No CSV fields selected for export.")
            return
        
        filename = f"{model_type}_models_{time.strftime('%Y%m%d')}.csv"
        try:
            with open(filename, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow(selected_fields)
                for model in models:
                    row_data = {
                        "Model ID": model.get("model_id") or model.get("id", "N/A"),
                        "Model Name": model.get("model_name") or model.get("name", "N/A"),
                        "Model Category": model.get("model_category", "N/A"),
                        "Description": model.get("description", "N/A"),
                        "Instance Prompt": model.get("instance_prompt", "N/A"),
                        "Screenshot URL": model.get("screenshots") or model.get("screenshot_url") or model.get("image_url", "N/A"),
                        "Is NSFW": str(model.get("is_nsfw", "False")),
                        "Feature": model.get("feature") or model.get("features", "N/A")
                    }
                    writer.writerow([row_data.get(field, "") for field in selected_fields])
            messagebox.showinfo("Export Success", f"Exported {len(models)} models to {filename}")
        except IOError as e:
            messagebox.showerror("Export Error", f"Failed to write to {filename}: {e}")
            logger.error(f"CSV export failed: {e}")

    def load_persistent_content(self):
        """Load and display content saved from the previous session."""
        if self.generated_content.get("image_url"):
            self.load_image_from_url(self.generated_content["image_url"], self.gen_image_label)
        
        if self.generated_content.get("chat_history"):
            self.chat_output.config(state="normal")
            self.chat_output.insert("1.0", self.generated_content["chat_history"])
            self.chat_output.config(state="disabled")
            
        if self.generated_content.get("audio_url"):
            url = self.generated_content["audio_url"]
            self.audio_status_label.config(text=f"Last audio: {url}")

        if self.generated_content.get("video_url"):
            url = self.generated_content["video_url"]
            self.video_url_label.config(text=f"Last video: {url}")
            
    def _start_threaded_task(self, task_func, button):
        if self.is_processing:
            messagebox.showwarning("Busy", "Another process is already running.")
            return
        self.is_processing = True
        button.config(state="disabled")
        self.root.config(cursor="wait")
        threading.Thread(target=task_func, daemon=True).start()

    def start_fetch_models(self):
        self._start_threaded_task(self.fetch_models, self.fetch_button)
        
    def start_generate_image(self):
        self._start_threaded_task(self.generate_image_task, self.gen_button)

    def start_send_chat(self):
        self._start_threaded_task(self.send_chat_task, self.chat_button)

    def start_generate_audio(self):
        self._start_threaded_task(self.generate_audio_task, self.audio_button)

    def start_generate_video(self):
        self._start_threaded_task(self.generate_video_task, self.video_button)

    def _task_cleanup(self, button, status_message=""):
        """Reset GUI state after a task is finished."""
        def run_in_main_thread():
            self.is_processing = False
            button.config(state="normal")
            self.root.config(cursor="")
            if status_message:
                self.status_var.set(status_message)
        self.root.after(0, run_in_main_thread)

    def generate_image_task(self):
        try:
            image_model_name = self.gen_image_model_var.get()
            image_model = next((m for m in self.image_models if (m.get('model_name') or m.get('name')) == image_model_name), None)
            
            if not image_model:
                messagebox.showerror("Error", "Please select a valid Image Model from the list.")
                self._task_cleanup(self.gen_button, "Image generation failed.")
                return

            payload = {
                "model_id": image_model.get('model_id'),
                "prompt": self.gen_prompt.get("1.0", tk.END).strip(),
                "negative_prompt": self.gen_neg_prompt.get("1.0", tk.END).strip(),
                "width": "512",
                "height": "512",
                "samples": "1",
                "num_inference_steps": "31",
                "safety_checker": "no",
                "enhance_prompt": "yes",
                "seed": None,
                "guidance_scale": 7.5,
                "scheduler": "UniPCMultistepScheduler",
            }
            
            if self.use_lora_var.get():
                lora_model_name = self.gen_lora_model_var.get()
                lora_model = next((m for m in self.lora_models if (m.get('model_name') or m.get('name')) == lora_model_name), None)
                if lora_model:
                    payload["lora_model"] = lora_model.get('model_id')
                    payload["lora_strength"] = self.lora_strength_var.get()
                else:
                    messagebox.showwarning("LoRA Warning", "LoRA is enabled, but no valid LoRA model is selected. Proceeding without LoRA.")

            self.status_var.set("Generating image...")
            response = self.api_handler.generate_image(payload)
            
            if response.get("status") == "success" and response.get("output"):
                img_url = response["output"][0]
                self.generated_content['image_url'] = img_url
                self.load_image_from_url(img_url, self.gen_image_label)
                self._task_cleanup(self.gen_button, "Image generated successfully.")
            else:
                messagebox.showerror("API Error", f"Image generation failed: {response.get('message', 'Unknown error')}")
                self._task_cleanup(self.gen_button, "Image generation failed.")

        except Exception as e:
            logger.error(f"Image generation error: {e}", exc_info=True)
            messagebox.showerror("Error", f"An error occurred: {e}")
            self._task_cleanup(self.gen_button, "Image generation failed.")

    def send_chat_task(self):
        try:
            model_name = self.chat_model.get()
            model = next((m for m in self.llmaster_models if (m.get('model_name') or m.get('name')) == model_name), None)
            if not model:
                messagebox.showerror("Error", "Please select a valid LLM model.")
                return

            message = self.chat_input.get("1.0", tk.END).strip()
            payload = {"model_id": model.get('model_id'), "messages": [{"role": "user", "content": message}]}
            
            self.status_var.set("Sending message...")
            response = self.api_handler.send_chat(payload)

            if response.get("status") == "success" and response.get("output"):
                content = response['output'][0]['content']
                def update_chat():
                    self.chat_output.config(state="normal")
                    self.chat_output.delete("1.0", tk.END)
                    self.chat_output.insert("1.0", content)
                    self.chat_output.config(state="disabled")
                    self.generated_content['chat_history'] = content
                self.root.after(0, update_chat)
                self._task_cleanup(self.chat_button, "Chat response received.")
            else:
                messagebox.showerror("API Error", f"Chat failed: {response.get('message', 'Unknown error')}")
                self._task_cleanup(self.chat_button, "Chat failed.")

        except Exception as e:
            logger.error(f"Chat error: {e}", exc_info=True)
            messagebox.showerror("Error", f"An error occurred: {e}")
            self._task_cleanup(self.chat_button, "Chat failed.")

    def generate_audio_task(self):
        try:
            model_name = self.audio_model.get()
            model = next((m for m in self.audiogen_models if (m.get('model_name') or m.get('name')) == model_name), None)
            if not model:
                messagebox.showerror("Error", "Please select a valid Audiogen model.")
                return

            audio_type = self.audio_type.get()
            prompt_text = self.audio_input.get("1.0", tk.END).strip()
            if not prompt_text and audio_type not in ["voice_to_voice"]:
                messagebox.showerror("Error", "Prompt field cannot be empty for this audio type.")
                return

            init_audio = self.audio_init_input.get("1.0", tk.END).strip()
            selected_voice_name = self.voice_id_var.get()
            voice_id = next((voice["voice_id"] for voice in self.voices if voice.get("name", voice["voice_id"]) == selected_voice_name), "default")

            payload = {
                "model_id": model.get('model_id'),
                "language": "english" if audio_type == "text_to_audio" else "american english",
                "webhook": None,
                "track_id": None
            }

            if audio_type in ["text_to_audio", "text_to_speech"]:
                if not prompt_text:
                    messagebox.showerror("Error", "Prompt is required for text_to_audio or text_to_speech.")
                    return
                payload["prompt"] = prompt_text
                if init_audio and init_audio.startswith(("http://", "https://")):
                    payload["init_audio"] = init_audio
                else:
                    payload["voice_id"] = voice_id
                if audio_type == "text_to_speech":
                    payload["speed"] = 1
                    payload["emotion"] = False
            elif audio_type == "voice_to_voice":
                if not init_audio or not init_audio.startswith(("http://", "https://")):
                    messagebox.showerror("Error", "Valid init_audio URL is required for voice_to_voice.")
                    return
                target_audio = init_audio  # For simplicity, reuse init_audio as target_audio
                payload["init_audio"] = init_audio
                payload["target_audio"] = target_audio
                payload["temp"] = False
                payload["base64"] = False
            elif audio_type == "music_gen":
                if not prompt_text:
                    messagebox.showerror("Error", "Prompt is required for music_gen.")
                    return
                payload["prompt"] = prompt_text
                payload["sampling_rate"] = 32000
                payload["temp"] = False
                payload["base64"] = False
                if init_audio and init_audio.startswith(("http://", "https://")):
                    payload["init_audio"] = init_audio
            elif audio_type == "song_generator":
                if not init_audio or not init_audio.startswith(("http://", "https://")):
                    messagebox.showerror("Error", "Valid init_audio URL is required for song_generator.")
                    return
                payload["init_audio"] = init_audio
                payload["lyrics"] = prompt_text if prompt_text else ""
                payload["lyrics_generation"] = False
                payload["base64"] = False
                payload["temp"] = False

            logger.info(f"Sending payload to {audio_type} API: {payload}")
            self.status_var.set(f"Generating {audio_type}...")
            response = self.api_handler.generate_audio(audio_type, payload)
            logger.info(f"API response: {response}")

            if response.get("status") == "success" and response.get("output"):
                audio_url = response["output"][0]
                self.generated_content['audio_url'] = audio_url
                self.root.after(0, lambda: self.audio_status_label.config(text=f"Playing audio: {audio_url}"))
                
                # Download and play
                urllib.request.urlretrieve(audio_url, "temp_audio.mp3")
                pygame.mixer.music.load("temp_audio.mp3")
                pygame.mixer.music.play()
                
                self._task_cleanup(self.audio_button, "Audio generated successfully.")
            else:
                messagebox.showerror("API Error", f"Audio generation failed: {response.get('message', 'Unknown error')}")
                self._task_cleanup(self.audio_button, "Audio generation failed.")

        except Exception as e:
            logger.error(f"Audio generation error: {e}", exc_info=True)
            messagebox.showerror("Error", f"An error occurred: {e}")
            self._task_cleanup(self.audio_button, "Audio generation failed.")

    def generate_video_task(self):
        try:
            model_name = self.video_model.get()
            model = next((m for m in self.videofusion_models if (m.get('model_name') or m.get('name')) == model_name), None)
            if not model:
                messagebox.showerror("Error", "Please select a valid VideoFusion model.")
                return

            input_data = self.video_input.get("1.0", tk.END).strip()
            payload = {"model_id": model.get('model_id'), "duration": 5}
            
            if input_data.startswith(("http://", "https://")):
                route = "img2video"
                payload["image_url"] = input_data
            else:
                route = "text2video"
                payload["prompt"] = input_data

            self.status_var.set(f"Generating video from {route.split('2')[0]}...")
            response = self.api_handler.generate_video(route, payload)

            if response.get("status") == "success" and response.get("video_url"):
                video_url = response["video_url"]
                self.generated_content['video_url'] = video_url
                self.root.after(0, lambda: self.video_url_label.config(text=f"Video URL: {video_url}"))
                self._task_cleanup(self.video_button, "Video generated successfully.")
            else:
                messagebox.showerror("API Error", f"Video generation failed: {response.get('message', 'Unknown error')}")
                self._task_cleanup(self.video_button, "Video generation failed.")

        except Exception as e:
            logger.error(f"Video generation error: {e}", exc_info=True)
            messagebox.showerror("Error", f"An error occurred: {e}")
            self._task_cleanup(self.video_button, "Video generation failed.")

if __name__ == "__main__":
    root = tk.Tk()
    app = ModelsLabGUI(root)
    root.mainloop()
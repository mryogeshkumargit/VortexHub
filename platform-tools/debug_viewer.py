#!/usr/bin/env python3
"""
VortexAI Debug Viewer
Captures Android debug logs via WiFi and displays them in a GUI
"""

import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox, filedialog
import subprocess
import threading
import queue
import re
import os
import qrcode
from PIL import Image, ImageTk
from datetime import datetime

class DebugViewer:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("VortexAI Debug Viewer")
        self.root.geometry("1200x800")
        
        self.log_queue = queue.Queue()
        self.is_connected = False
        self.adb_process = None
        self.adb_path = "adb"  # Default, will be updated by check_adb()
        # connection_method will be initialized in setup_ui()
        
        self.setup_ui()
        self.check_adb()
        
    def setup_ui(self):
        # Main frame
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Connection frame
        conn_frame = ttk.LabelFrame(main_frame, text="Connection", padding="5")
        conn_frame.grid(row=0, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # Connection method selection
        ttk.Label(conn_frame, text="Method:").grid(row=0, column=0, padx=(0, 5))
        self.connection_method = tk.StringVar(value="wifi")
        
        usb_radio = ttk.Radiobutton(conn_frame, text="USB", variable=self.connection_method, 
                                   value="usb", command=self.on_method_change)
        usb_radio.grid(row=0, column=1, padx=(0, 10))
        
        wifi_radio = ttk.Radiobutton(conn_frame, text="WiFi", variable=self.connection_method, 
                                    value="wifi", command=self.on_method_change)
        wifi_radio.grid(row=0, column=2, padx=(0, 10))
        
        # Connection state tracking
        self.connection_state = "disconnected"  # disconnected, need_pairing, paired, connected
        
        # WiFi connection fields
        self.wifi_frame = ttk.Frame(conn_frame)
        self.wifi_frame.grid(row=1, column=0, columnspan=6, sticky=(tk.W, tk.E), pady=(5, 0))
        
        ttk.Label(self.wifi_frame, text="Phone IP:").grid(row=0, column=0, padx=(0, 5))
        self.ip_entry = ttk.Entry(self.wifi_frame, width=15)
        self.ip_entry.grid(row=0, column=1, padx=(0, 10))
        self.ip_entry.insert(0, "192.168.1.2")
        
        ttk.Label(self.wifi_frame, text="Port:").grid(row=0, column=2, padx=(0, 5))
        self.port_entry = ttk.Entry(self.wifi_frame, width=8)
        self.port_entry.grid(row=0, column=3, padx=(0, 10))
        self.port_entry.insert(0, "43359")
        
        self.detect_btn = ttk.Button(self.wifi_frame, text="Auto-Detect", command=self.auto_detect_phone)
        self.detect_btn.grid(row=0, column=4, padx=(0, 10))
        
        # WiFi pairing frame (initially hidden)
        self.pair_frame = ttk.Frame(conn_frame)
        
        ttk.Label(self.pair_frame, text="Pairing Code:").grid(row=0, column=0, padx=(0, 5))
        self.pair_code_entry = ttk.Entry(self.pair_frame, width=10)
        self.pair_code_entry.grid(row=0, column=1, padx=(0, 10))
        
        ttk.Label(self.pair_frame, text="Pair IP:").grid(row=0, column=2, padx=(0, 5))
        self.pair_ip_entry = ttk.Entry(self.pair_frame, width=15)
        self.pair_ip_entry.grid(row=0, column=3, padx=(0, 10))
        
        ttk.Label(self.pair_frame, text="Pair Port:").grid(row=0, column=4, padx=(0, 5))
        self.pair_port_entry = ttk.Entry(self.pair_frame, width=8)
        self.pair_port_entry.grid(row=0, column=5, padx=(0, 10))
        
        self.pair_btn = ttk.Button(self.pair_frame, text="Pair Device", command=self.pair_device)
        self.pair_btn.grid(row=0, column=6, padx=(0, 5))
        
        self.qr_btn = ttk.Button(self.pair_frame, text="QR Pair", command=self.show_qr_pair)
        self.qr_btn.grid(row=0, column=7)
        
        # Connection buttons
        button_frame = ttk.Frame(conn_frame)
        button_frame.grid(row=3, column=0, columnspan=6, pady=(5, 0))
        
        self.connect_btn = ttk.Button(button_frame, text="Connect", command=self.connect_device)
        self.connect_btn.grid(row=0, column=0, padx=(0, 5))
        
        self.test_network_btn = ttk.Button(button_frame, text="Test Network", command=self.test_network)
        self.test_network_btn.grid(row=0, column=1, padx=(0, 5))
        
        self.install_btn = ttk.Button(button_frame, text="Install APK", command=self.install_apk)
        self.install_btn.grid(row=0, column=2, padx=(0, 5))
        
        self.debug_help_btn = ttk.Button(button_frame, text="Enable Debug Help", command=self.show_debug_help)
        self.debug_help_btn.grid(row=0, column=4, padx=(5, 0))
        
        self.status_label = ttk.Label(button_frame, text="Not connected", foreground="red")
        self.status_label.grid(row=0, column=3, padx=(10, 0))
        
        # Update visibility based on connection method
        self.update_ui_state()
        
        # Filter frame
        filter_frame = ttk.LabelFrame(main_frame, text="Filters", padding="5")
        filter_frame.grid(row=1, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        ttk.Label(filter_frame, text="Filter:").grid(row=0, column=0, padx=(0, 5))
        self.filter_entry = ttk.Entry(filter_frame, width=30)
        self.filter_entry.grid(row=0, column=1, padx=(0, 10))
        self.filter_entry.insert(0, "")
        
        # Add show all checkbox
        self.show_all_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(filter_frame, text="Show All", variable=self.show_all_var).grid(row=0, column=5, padx=(10, 0))
        
        self.start_btn = ttk.Button(filter_frame, text="Start Logging", command=self.start_logging)
        self.start_btn.grid(row=0, column=2, padx=(0, 5))
        
        self.stop_btn = ttk.Button(filter_frame, text="Stop", command=self.stop_logging, state="disabled")
        self.stop_btn.grid(row=0, column=3, padx=(0, 5))
        
        self.clear_btn = ttk.Button(filter_frame, text="Clear", command=self.clear_logs)
        self.clear_btn.grid(row=0, column=4)
        
        self.quick_capture_btn = ttk.Button(filter_frame, text="📸 Quick Capture", command=self.quick_capture)
        self.quick_capture_btn.grid(row=0, column=6, padx=(10, 0))
        
        # Log display with performance controls
        log_frame = ttk.LabelFrame(main_frame, text="Debug Logs", padding="5")
        log_frame.grid(row=2, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(0, 10))
        
        # Add performance controls
        perf_frame = ttk.Frame(log_frame)
        perf_frame.grid(row=0, column=0, sticky=(tk.W, tk.E), pady=(0, 5))
        
        self.auto_scroll_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(perf_frame, text="Auto-scroll", variable=self.auto_scroll_var).grid(row=0, column=0, padx=(0, 10))
        
        self.rate_limit_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(perf_frame, text="Rate limiting", variable=self.rate_limit_var).grid(row=0, column=1, padx=(0, 10))
        
        ttk.Label(perf_frame, text="Lines:").grid(row=0, column=2, padx=(10, 5))
        self.line_count_label = ttk.Label(perf_frame, text="0")
        self.line_count_label.grid(row=0, column=3)
        
        self.log_text = scrolledtext.ScrolledText(log_frame, wrap=tk.WORD, height=25, font=("Consolas", 9))
        self.log_text.grid(row=1, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Update grid configuration
        log_frame.columnconfigure(0, weight=1)
        log_frame.rowconfigure(1, weight=1)
        
        # Summary frame
        summary_frame = ttk.LabelFrame(main_frame, text="Quick Summary", padding="5")
        summary_frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E))
        
        self.summary_text = tk.Text(summary_frame, height=6, font=("Consolas", 9), bg="#f0f0f0")
        self.summary_text.grid(row=0, column=0, sticky=(tk.W, tk.E))
        
        # Configure grid weights
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(0, weight=1)
        main_frame.rowconfigure(2, weight=1)
        summary_frame.columnconfigure(0, weight=1)
        
        # Initialize line counter
        self.total_lines = 0
        
        # Start queue processing
        self.process_queue()
        
    def check_adb(self):
        """Check if ADB is available"""
        # Common ADB locations
        adb_paths = [
            "adb",  # If in PATH
            os.path.join(os.path.dirname(__file__), "adb.exe"),  # Same directory as script
            r"C:\Users\{}\AppData\Local\Android\Sdk\platform-tools\adb.exe".format(os.getenv('USERNAME', '')),
            r"C:\Android\sdk\platform-tools\adb.exe",
            r"C:\Program Files\Android\sdk\platform-tools\adb.exe",
            r"C:\Android\platform-tools\adb.exe"
        ]
        
        self.adb_path = None
        
        for path in adb_paths:
            try:
                result = subprocess.run([path, "version"], capture_output=True, text=True, timeout=5)
                if result.returncode == 0:
                    self.adb_path = path
                    self.log_message(f"✅ ADB found at: {path}")
                    return
            except (FileNotFoundError, OSError):
                continue
            except (subprocess.SubprocessError, OSError, FileNotFoundError) as e:
                self.log_message(f"ADB check failed for {path}: {e}")
                continue
        
        # If no ADB found, show error with instructions
        error_msg = """ADB not found! 
        
Please either:
1. Add ADB to your PATH, or
2. Place adb.exe in the same folder as this script
        
ADB is part of Android SDK Platform Tools:
https://developer.android.com/studio/releases/platform-tools"""
        
        messagebox.showerror("ADB Not Found", error_msg)
        self.log_message("❌ ADB not found in common locations")
    
    def on_method_change(self):
        """Handle connection method change"""
        self.connection_state = "disconnected"
        self.update_ui_state()
    
    def update_ui_state(self):
        """Update UI visibility based on connection method and state"""
        method = self.connection_method.get()
        
        if method == "usb":
            # USB mode - hide all WiFi elements
            self.wifi_frame.grid_remove()
            self.pair_frame.grid_remove()
        else:
            # WiFi mode - show connection fields
            self.wifi_frame.grid(row=1, column=0, columnspan=6, sticky=(tk.W, tk.E), pady=(5, 0))
            
            # Show pairing fields only when needed
            if self.connection_state in ["disconnected", "need_pairing"]:
                self.pair_frame.grid(row=2, column=0, columnspan=6, sticky=(tk.W, tk.E), pady=(5, 0))
            else:
                self.pair_frame.grid_remove()
    
    def auto_detect_phone(self):
        """Auto-detect phone IP on network (non-blocking)"""
        self.detect_btn.config(state="disabled", text="Scanning...")
        self.log_message("🔍 Scanning network for Android devices...")
        
        def scan_network():
            try:
                # Get PC's IP to determine network range
                import socket
                s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                s.connect(("8.8.8.8", 80))
                pc_ip = s.getsockname()[0]
                s.close()
                
                network = ".".join(pc_ip.split(".")[:-1])
                self.root.after(0, lambda: self.log_message(f"📡 Scanning network: {network}.1-254"))
                
                # Quick scan of common ranges first
                found_devices = []
                common_ranges = [range(100, 120), range(1, 20), range(200, 255)]
                
                for ip_range in common_ranges:
                    for i in ip_range:
                        test_ip = f"{network}.{i}"
                        if test_ip != pc_ip:
                            # Quick port check for common ADB ports
                            for port in [5555, 5556, 37045, 35647]:
                                try:
                                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                                        sock.settimeout(0.05)  # Very quick timeout
                                        result = sock.connect_ex((test_ip, port))
                                        if result == 0:
                                            found_devices.append((test_ip, port))
                                            break
                                except (socket.error, OSError):
                                    continue
                        
                        # Break early if device found
                        if found_devices:
                            break
                    if found_devices:
                        break
                
                # Update UI in main thread
                def update_ui():
                    if found_devices:
                        ip, port = found_devices[0]
                        self.ip_entry.delete(0, tk.END)
                        self.ip_entry.insert(0, ip)
                        self.port_entry.delete(0, tk.END)
                        self.port_entry.insert(0, str(port))
                        self.log_message(f"📱 Found device at {ip}:{port}")
                    else:
                        self.log_message("❌ No Android devices found on network")
                    
                    self.detect_btn.config(state="normal", text="Auto-Detect")
                
                self.root.after(0, update_ui)
                
            except Exception as e:
                self.root.after(0, lambda: self.log_message(f"❌ Auto-detect failed: {e}"))
                self.root.after(0, lambda: self.detect_btn.config(state="normal", text="Auto-Detect"))
        
        # Run scan in background thread
        threading.Thread(target=scan_network, daemon=True).start()
    
    def test_network(self):
        """Test network connectivity"""
        method = self.connection_method.get()
        
        if method == "wifi":
            ip = self.ip_entry.get().strip()
            port = self.port_entry.get().strip()
            
            if not ip or not port:
                messagebox.showerror("Error", "Please enter IP and port")
                return
            
            self.log_message(f"🧪 Testing network connectivity to {ip}:{port}...")
            
            # Test ping
            try:
                import socket
                
                # Test if IP is reachable
                with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                    sock.settimeout(3)
                    result = sock.connect_ex((ip, int(port)))
                
                if result == 0:
                    self.log_message(f"✅ Port {port} is open on {ip}")
                else:
                    self.log_message(f"❌ Port {port} is closed on {ip}")
                    
                # Test ping
                ping_result = subprocess.run(["ping", "-n", "1", "-w", "1000", ip], 
                                           capture_output=True, text=True)
                if ping_result.returncode == 0:
                    self.log_message(f"✅ {ip} is reachable (ping successful)")
                else:
                    self.log_message(f"❌ {ip} is not reachable (ping failed)")
                    
            except Exception as e:
                self.log_message(f"❌ Network test failed: {e}")
        else:
            # USB test
            if not self.adb_path:
                self.log_message("❌ ADB not found")
                return
                
            self.log_message("🧪 Testing USB connection...")
            try:
                result = subprocess.run([self.adb_path, "devices"], capture_output=True, text=True, timeout=5)
                devices = [line for line in result.stdout.split('\n') if '\tdevice' in line]
                if devices:
                    self.log_message(f"✅ Found {len(devices)} USB device(s)")
                else:
                    self.log_message("❌ No USB devices found")
            except (subprocess.SubprocessError, OSError) as e:
                self.log_message(f"❌ USB test failed: {e}")
    
    def connect_device(self):
        """Connect to Android device via USB or WiFi"""
        method = self.connection_method.get()
        
        if method == "usb":
            self.connect_usb()
        else:
            self.connect_wifi()
    
    def connect_usb(self):
        """Connect via USB"""
        if not self.adb_path:
            messagebox.showerror("Error", "ADB not found. Please install Android SDK Platform Tools.")
            return
            
        try:
            self.log_message("🔄 Connecting via USB...")
            
            # Check for USB devices
            result = subprocess.run([self.adb_path, "devices"], capture_output=True, text=True, timeout=5)
            devices = [line for line in result.stdout.split('\n') if '\tdevice' in line]
            
            if not devices:
                messagebox.showerror("USB Error", "No USB devices found. Please:\n1. Connect USB cable\n2. Enable USB Debugging\n3. Accept debugging prompt on phone")
                return
            
            self.is_connected = True
            self.status_label.config(text=f"Connected via USB ({len(devices)} device(s))", foreground="green")
            self.log_message(f"✅ Connected via USB - {len(devices)} device(s) found")
            self.connect_btn.config(text="Disconnect", command=self.disconnect_device)
            self.check_debug_status()
            
        except Exception as e:
            self.log_message(f"❌ USB connection error: {e}")
            messagebox.showerror("Error", f"USB connection error: {e}")
    
    def connect_wifi(self):
        """Connect via WiFi with robust error handling"""
        ip = self.ip_entry.get().strip()
        port = self.port_entry.get().strip()
        
        if not ip or not port:
            messagebox.showerror("Error", "Please enter IP and port")
            return
        
        try:
            self.log_message(f"🔄 Connecting to {ip}:{port}...")
            
            # Skip port check - directly try ADB connect as Android WiFi debugging
            # may not respond to TCP socket probes but ADB can still connect
            self.log_message(f"📡 Attempting ADB connection...")
            
            # Try ADB connect with longer timeout for WiFi
            result = subprocess.run([self.adb_path, "connect", f"{ip}:{port}"], 
                                  capture_output=True, text=True, timeout=30)
            
            output_combined = (result.stdout + result.stderr).lower()
            self.log_message(f"ADB output: {result.stdout}")
            
            if "connected" in output_combined or "already connected" in output_combined:
                self.is_connected = True
                self.connection_state = "connected"
                self.status_label.config(text=f"Connected to {ip}:{port}", foreground="green")
                self.log_message(f"✅ Connected to {ip}:{port}")
                self.connect_btn.config(text="Disconnect", command=self.disconnect_device)
                self.update_ui_state()
                self.check_debug_status()
            elif "cannot connect" in output_combined or "failed to connect" in output_combined:
                error_details = result.stdout + result.stderr
                self.log_message(f"❌ ADB connection failed: {error_details}")
                
                if "authentication" in error_details.lower() or "not paired" in error_details.lower():
                    self.connection_state = "need_pairing"
                    self.update_ui_state()
                    messagebox.showwarning("Pairing Required", "Device needs to be paired first. Use the pairing fields below.")
                elif "offline" in error_details.lower():
                    messagebox.showerror("Device Offline", "Device is offline. Try reconnecting WiFi debugging")
                else:
                    error_msg = f"""Connection failed to {ip}:{port}

Common issues:
1. Wrong port - Check if you're using the connection port (e.g. 43359) not pairing port (e.g. 37045)
2. Device not on same WiFi network
3. WiFi debugging disabled on phone
4. Firewall blocking connection
5. Try pairing first if you haven't

ADB Output:
{error_details}"""
                    messagebox.showerror("Connection Failed", error_msg)
            else:
                # Unexpected output - treat as potential success
                self.log_message(f"⚠️ Unexpected ADB output - verifying connection...")
                verify_result = subprocess.run([self.adb_path, "devices"], 
                                             capture_output=True, text=True, timeout=5)
                if f"{ip}:{port}" in verify_result.stdout and "device" in verify_result.stdout:
                    self.is_connected = True
                    self.connection_state = "connected"
                    self.status_label.config(text=f"Connected to {ip}:{port}", foreground="green")
                    self.log_message(f"✅ Connection verified via adb devices")
                    self.connect_btn.config(text="Disconnect", command=self.disconnect_device)
                    self.update_ui_state()
                else:
                    messagebox.showerror("Connection Failed", f"Could not verify connection.\n\nADB Output:\n{result.stdout}")
                
        except subprocess.TimeoutExpired:
            self.log_message(f"❌ Connection timeout to {ip}:{port}")
            messagebox.showerror("Timeout", f"Connection timed out. Check if {ip}:{port} is correct and device is on same WiFi network.")
        except Exception as e:
            self.log_message(f"❌ WiFi connection error: {e}")
            messagebox.showerror("Error", f"WiFi connection error: {e}")
    
    def disconnect_device(self):
        """Disconnect from device"""
        try:
            if self.adb_process:
                self.stop_logging()
            
            if self.connection_method.get() == "wifi":
                subprocess.run([self.adb_path, "disconnect"], capture_output=True, timeout=5)
            self.is_connected = False
            self.connection_state = "disconnected"
            self.status_label.config(text="Not connected", foreground="red")
            self.log_message("🔌 Disconnected")
            self.connect_btn.config(text="Connect", command=self.connect_device)
            self.update_ui_state()
            
        except Exception as e:
            self.log_message(f"❌ Disconnect error: {e}")
    
    def start_logging(self):
        """Start capturing logs"""
        if not self.is_connected:
            messagebox.showerror("Error", "Please connect to device first")
            return
            
        if not self.adb_path:
            messagebox.showerror("Error", "ADB not found. Please install Android SDK Platform Tools.")
            return
        
        # Verify connection is still active
        try:
            result = subprocess.run([self.adb_path, "devices"], capture_output=True, text=True, timeout=5)
            if "device" not in result.stdout:
                messagebox.showerror("Connection Lost", "Device connection lost. Please reconnect.")
                self.disconnect_device()
                return
        except Exception as e:
            self.log_message(f"❌ Connection check failed: {e}")
            return
            
        filter_text = self.filter_entry.get().strip()
        if self.show_all_var.get():
            filter_text = ""  # Show everything if checkbox is checked
        
        try:
            # Clear logcat buffer first
            clear_cmd = [self.adb_path, "logcat", "-c"]
            subprocess.run(clear_cmd, capture_output=True, timeout=5)
            
            # Build logcat command with tag filtering for better performance
            cmd = [self.adb_path, "logcat"]
            
            # If user specified a filter, apply it at logcat level for better performance
            if filter_text:
                # For common use cases, filter by tag at logcat level
                if "ImageGeneration" in filter_text:
                    # Show ImageGenerationService, ImageGenerationViewModel, and related tags
                    cmd.extend(["ImageGenerationService:V", "ImageGenerationViewModel:V", 
                               "CustomApiExecutor:V", "CustomApiProvider:V", "*:S"])
                elif "Chat" in filter_text:
                    cmd.extend(["ChatViewModel:V", "ChatLLMService:V", "*:S"])
                elif "vortex" in filter_text.lower():
                    # Show all vortex app logs
                    cmd.extend(["*:V"])
                else:
                    # Generic filter - show all but will be filtered in read_logs
                    cmd.extend(["*:V"])
            else:
                # No filter - show all logs
                cmd.extend(["*:V"])
            
            self.log_message(f"🔧 Running: {' '.join(cmd)}")
            
            self.adb_process = subprocess.Popen(cmd, stdout=subprocess.PIPE, 
                                             stderr=subprocess.PIPE, text=True, 
                                             bufsize=0, encoding='utf-8', errors='replace')
            
            # Start log reading thread
            self.log_thread = threading.Thread(target=self.read_logs, args=(filter_text,))
            self.log_thread.daemon = True
            self.log_thread.start()
            
            self.start_btn.config(state="disabled")
            self.stop_btn.config(state="normal")
            if filter_text:
                self.log_message(f"🚀 Started logging with filter: '{filter_text}'")
            else:
                self.log_message("🚀 Started logging (showing all logs)")
            
            # Test if logcat is working
            def test_logcat():
                import time
                time.sleep(2)
                test_result = subprocess.run([self.adb_path, "shell", "log", "TEST_LOG_MESSAGE"], 
                                           capture_output=True, timeout=5)
                if test_result.returncode == 0:
                    self.log_message("📝 Sent test log message")
            
            threading.Thread(target=test_logcat, daemon=True).start()
            
        except Exception as e:
            self.log_message(f"❌ Failed to start logging: {e}")
            messagebox.showerror("Error", f"Failed to start logging: {e}")
    
    def stop_logging(self):
        """Stop capturing logs"""
        try:
            if self.adb_process:
                self.adb_process.terminate()
                self.adb_process = None
                
            self.start_btn.config(state="normal")
            self.stop_btn.config(state="disabled")
            self.log_message("⏹️ Stopped logging")
            
        except Exception as e:
            self.log_message(f"❌ Error stopping logging: {e}")
    
    def read_logs(self, filter_text):
        """Read logs from ADB process with improved filtering"""
        line_count = 0
        filter_lower = filter_text.lower() if filter_text else ""
        
        try:
            if filter_text:
                self.log_queue.put(("log", f"📡 Starting to read logs with filter: '{filter_text}'..."))
            else:
                self.log_queue.put(("log", "📡 Starting to read all logs..."))
            
            while self.adb_process and self.adb_process.poll() is None:
                try:
                    line = self.adb_process.stdout.readline()
                    if line:
                        clean_line = line.strip()
                        line_count += 1
                        
                        # Debug: show first few lines regardless of filter
                        if line_count <= 3:
                            self.log_queue.put(("log", f"[DEBUG {line_count}] {clean_line}"))
                            continue
                        
                        # Improved filtering logic
                        should_show = False
                        
                        if not filter_text:
                            # No filter - show everything
                            should_show = True
                        else:
                            # Apply filter (case-insensitive)
                            line_lower = clean_line.lower()
                            
                            # Check if filter text appears in the line
                            if filter_lower in line_lower:
                                should_show = True
                            # Also show critical errors regardless of filter
                            elif any(keyword in clean_line for keyword in ["FATAL", "AndroidRuntime"]):
                                should_show = True
                        
                        if should_show:
                            if self.log_queue.qsize() < 1000:
                                self.log_queue.put(("log", clean_line))
                            
                            # Check for app-specific patterns for summary
                            line_lower = clean_line.lower()
                            if any(pattern in line_lower for pattern in 
                                   ["imagegenerationservice", "imagegenerationviewmodel", 
                                    "customapiexecutor", "vortex", "exception", "error", "fatal"]):
                                if self.log_queue.qsize() < 200:
                                    self.log_queue.put(("summary", clean_line))
                        
                        # Show progress less frequently for better performance
                        if line_count % 500 == 0:
                            self.log_queue.put(("log", f"📊 Processed {line_count} lines..."))
                                
                except UnicodeDecodeError:
                    self.log_queue.put(("log", "[Encoding Error - Skipped Line]"))
                    continue
            
            # Process ended
            self.log_queue.put(("log", f"📊 Log reading finished. Processed {line_count} total lines."))
            
            if self.adb_process and self.adb_process.poll() is not None:
                stderr_output = self.adb_process.stderr.read()
                if stderr_output:
                    self.log_queue.put(("error", f"ADB stderr: {stderr_output}"))
                            
        except Exception as e:
            self.log_queue.put(("error", f"Log reading error: {e}"))
    
    def process_queue(self):
        """Process log queue and update UI with batching for performance"""
        batch_size = 0
        max_batch = 20  # Process max 20 messages per cycle
        
        try:
            while batch_size < max_batch:
                msg_type, message = self.log_queue.get_nowait()
                batch_size += 1
                
                if msg_type == "log":
                    self.log_text.insert(tk.END, message + "\n")
                    self.total_lines += 1
                    
                    # Update line counter every 10 lines
                    if self.total_lines % 10 == 0:
                        self.line_count_label.config(text=str(self.total_lines))
                    
                    # Limit text widget size to prevent memory issues
                    line_count = int(self.log_text.index('end-1c').split('.')[0])
                    if line_count > 5000:  # Keep only last 5000 lines
                        self.log_text.delete('1.0', '1000.0')  # Delete first 1000 lines
                        self.total_lines -= 1000  # Fix line counter accuracy
                    
                    # Color coding for important messages (simplified)
                    if "🔥 DEEP DEBUG" in message:
                        try:
                            start = self.log_text.index("end-2c linestart")
                            end = self.log_text.index("end-2c lineend")
                            
                            if "ERROR" in message or "FAILED" in message:
                                self.log_text.tag_add("error", start, end)
                                self.log_text.tag_config("error", foreground="red")
                            elif "SUCCESS" in message:
                                self.log_text.tag_add("success", start, end)
                                self.log_text.tag_config("success", foreground="green")
                        except tk.TclError:
                            pass  # Ignore tagging errors
                
                elif msg_type == "summary":
                    self.update_summary(message)
                    
                elif msg_type == "error":
                    self.log_message(message)
        
        except queue.Empty:
            pass
        
        # Auto-scroll only if enabled and near bottom (performance optimization)
        try:
            if batch_size > 0 and self.auto_scroll_var.get():
                # Check if scrollbar is near bottom
                scrollbar_pos = self.log_text.yview()[1]
                if scrollbar_pos > 0.9:  # Only auto-scroll if near bottom
                    self.log_text.see(tk.END)
        except tk.TclError:
            pass
        
        # Schedule next check with adaptive timing
        next_delay = 50 if batch_size >= max_batch else 100
        self.root.after(next_delay, self.process_queue)
    
    def update_summary(self, message):
        """Update summary with key information (rate limited)"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        
        # Limit summary text size
        line_count = int(self.summary_text.index('end-1c').split('.')[0])
        if line_count > 100:  # Keep only last 100 lines
            lines_to_delete = line_count - 80
            self.summary_text.delete('1.0', f'{lines_to_delete}.0')
        
        # Extract key information
        if "Provider:" in message:
            self.summary_text.insert(tk.END, f"[{timestamp}] {message}\n")
        elif "API Key" in message:
            self.summary_text.insert(tk.END, f"[{timestamp}] {message}\n")
        elif "Exception" in message or "ERROR" in message:
            self.summary_text.insert(tk.END, f"[{timestamp}] ❌ {message}\n")
        elif "SUCCESS" in message:
            self.summary_text.insert(tk.END, f"[{timestamp}] ✅ {message}\n")
            
        self.summary_text.see(tk.END)
    
    def clear_logs(self):
        """Clear log display"""
        self.log_text.delete(1.0, tk.END)
        self.summary_text.delete(1.0, tk.END)
        self.total_lines = 0
        self.line_count_label.config(text="0")
        
        # Clear the queue to prevent old messages from appearing
        for _ in range(1000):  # Prevent infinite loop
            try:
                self.log_queue.get_nowait()
            except queue.Empty:
                break
        
        self.log_message("🗑️ Logs cleared")
    
    def quick_capture(self):
        """Quick capture workflow: Clear logs, wait for action, capture snapshot"""
        if not self.is_connected:
            messagebox.showerror("Error", "Please connect to device first")
            return
        
        filter_text = self.filter_entry.get().strip()
        
        if not filter_text:
            response = messagebox.askyesno(
                "No Filter Set",
                "No filter is set. This will capture ALL logs which can be overwhelming.\n\n" +
                "Recommended filters:\n" +
                "- ImageGeneration (for image generation issues)\n" +
                "- Chat (for chat issues)\n" +
                "- vortex (for all app logs)\n\n" +
                "Continue without filter?"
            )
            if not response:
                return
        
        try:
            # Step 1: Clear logcat buffer
            self.log_message("📸 Quick Capture: Clearing old logs...")
            subprocess.run([self.adb_path, "logcat", "-c"], capture_output=True, timeout=5)
            
            # Step 2: Prompt user
            self.log_message("✋ Waiting for action...")
            response = messagebox.showinfo(
                "Quick Capture Ready",
                f"Logs cleared! Now:\n\n" +
                f"1. Perform the action in your app (e.g., generate image)\n" +
                f"2. Wait 2-3 seconds after action completes\n" +
                f"3. Click OK to capture logs\n\n" +
                (f"Filter: '{filter_text}'" if filter_text else "Capturing ALL logs")
            )
            
            # Step 3: Capture logs
            self.log_message("📡 Capturing logs...")
            
            # Build capture command with tag filtering
            cmd = [self.adb_path, "logcat", "-d"]  # -d dumps and exits
            
            if filter_text:
                if "ImageGeneration" in filter_text:
                    cmd.extend(["ImageGenerationService:V", "ImageGenerationViewModel:V", 
                               "CustomApiExecutor:V", "CustomApiProvider:V", "*:S"])
                elif "Chat" in filter_text:
                    cmd.extend(["ChatViewModel:V", "ChatLLMService:V", "*:S"])
                elif "vortex" in filter_text.lower():
                    cmd.extend(["*:V"])
                else:
                    # Generic - will filter in display
                    pass
            
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=15)
            
            if result.returncode == 0:
                output_lines = result.stdout.strip().split('\n')
                
                # Apply text-level filter if specified
                if filter_text:
                    filter_lower = filter_text.lower()
                    filtered_lines = [line for line in output_lines 
                                    if filter_lower in line.lower() or 
                                    any(kw in line for kw in ["FATAL", "AndroidRuntime"])]
                else:
                    filtered_lines = output_lines
                
                # Clear display and show captured logs
                self.log_text.delete(1.0, tk.END)
                self.summary_text.delete(1.0, tk.END)
                
                if filtered_lines:
                    self.log_message(f"📸 === Quick Capture Results ({len(filtered_lines)} lines) ===")
                    self.log_message(f"Filter: {filter_text if filter_text else 'None (all logs)'}")
                    self.log_message("=" * 80)
                    
                    for line in filtered_lines:
                        self.log_text.insert(tk.END, line + "\n")
                        
                        # Add to summary if relevant
                        line_lower = line.lower()
                        if any(pattern in line_lower for pattern in 
                               ["imagegenerationservice", "imagegenerationviewmodel", 
                                "customapiexecutor", "exception", "error", "fatal"]):
                            self.update_summary(line)
                    
                    self.log_message("=" * 80)
                    self.log_message(f"✅ Capture complete! {len(filtered_lines)} lines captured.")
                    
                    # Inform user they can copy/paste to save
                    if len(filtered_lines) > 20:
                        self.log_message("💡 Tip: Select text and Ctrl+C to copy, or use 'Save Logs' menu option")
                else:
                    self.log_message("⚠️ No logs captured matching the filter.")
                    self.log_message("Try:")
                    self.log_message("1. Check if the action actually ran in the app")
                    self.log_message("2. Try with 'Show All' checkbox enabled")
                    self.log_message("3. Use a broader filter (e.g., just 'Image' instead of 'ImageGeneration')")
            else:
                self.log_message(f"❌ Capture failed: {result.stderr}")
                
        except Exception as e:
            self.log_message(f"❌ Quick capture error: {e}")
            messagebox.showerror("Error", f"Quick capture failed: {e}")
    
    def log_message(self, message):
        """Add message to log display"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.insert(tk.END, f"[{timestamp}] {message}\n")
        self.log_text.see(tk.END)
    
    def run(self):
        """Start the application"""
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)
        self.root.mainloop()
    
    def pair_device(self):
        """Pair device using pairing code"""
        pair_code = self.pair_code_entry.get().strip()
        pair_ip = self.pair_ip_entry.get().strip()
        pair_port = self.pair_port_entry.get().strip()
        
        if not all([pair_code, pair_ip, pair_port]):
            messagebox.showerror("Error", "Please enter pairing code, IP and port")
            return
            
        # Validate pairing code format (typically 6 digits)
        if not pair_code.isdigit() or len(pair_code) != 6:
            messagebox.showerror("Error", "Pairing code must be 6 digits")
            return
            
        # Validate port range
        try:
            port_num = int(pair_port)
            if not (1 <= port_num <= 65535):
                messagebox.showerror("Error", "Port must be between 1 and 65535")
                return
        except ValueError:
            messagebox.showerror("Error", "Port must be a valid number")
            return
            
        if not self.adb_path:
            messagebox.showerror("Error", "ADB not found. Please install Android SDK Platform Tools.")
            return
        
        try:
            self.log_message(f"🔗 Pairing with {pair_ip}:{pair_port} using code {pair_code}...")
            
            result = subprocess.run([self.adb_path, "pair", f"{pair_ip}:{pair_port}", pair_code], 
                                  capture_output=True, text=True, timeout=30, input="\n")
            
            if "successfully paired" in result.stdout.lower() or "paired" in result.stdout.lower():
                self.connection_state = "paired"
                self.log_message(f"✅ Successfully paired with {pair_ip}:{pair_port}")
                self.update_ui_state()
                messagebox.showinfo("Success", f"Device paired successfully!\nNow use the connection IP and port to connect.")
            else:
                error_msg = result.stdout + result.stderr
                self.log_message(f"❌ Pairing failed: {error_msg}")
                messagebox.showerror("Pairing Failed", f"Failed to pair device:\n{error_msg}")
                
        except Exception as e:
            self.log_message(f"❌ Pairing error: {e}")
            messagebox.showerror("Error", f"Pairing error: {e}")
    
    def show_qr_pair(self):
        """Show QR code pairing dialog"""
        qr_window = tk.Toplevel(self.root)
        qr_window.title("QR Code Pairing")
        qr_window.geometry("400x500")
        qr_window.resizable(False, False)
        
        # Instructions
        instructions = tk.Text(qr_window, height=8, wrap=tk.WORD, font=("Arial", 10))
        instructions.pack(padx=20, pady=10, fill=tk.X)
        
        instructions.insert(tk.END, "QR Code Pairing Instructions:\n\n")
        instructions.insert(tk.END, "1. On your Android device, go to:\n")
        instructions.insert(tk.END, "   Settings → Developer Options → Wireless Debugging\n\n")
        instructions.insert(tk.END, "2. Tap 'Pair device with QR code'\n\n")
        instructions.insert(tk.END, "3. Scan the QR code below\n\n")
        instructions.insert(tk.END, "4. Once paired, use the connection IP:Port to connect")
        instructions.config(state=tk.DISABLED)
        
        # Generate QR code with pairing info
        try:
            # Generate proper ADB pairing data
            pair_ip = self.pair_ip_entry.get().strip() or "192.168.1.100"
            pair_port = self.pair_port_entry.get().strip() or "37045"
            pair_data = f"adb-tls-connect:{pair_ip}:{pair_port}"
            
            qr = qrcode.QRCode(version=1, box_size=8, border=5)
            qr.add_data(pair_data)
            qr.make(fit=True)
            
            qr_img = qr.make_image(fill_color="black", back_color="white")
            qr_photo = ImageTk.PhotoImage(qr_img)
            
            qr_label = tk.Label(qr_window, image=qr_photo)
            qr_label.image = qr_photo  # Keep a reference
            qr_label.pack(pady=10)
            
        except Exception as e:
            tk.Label(qr_window, text=f"QR Code generation failed: {e}", fg="red").pack(pady=20)
        
        # Note about manual pairing
        note_frame = tk.Frame(qr_window)
        note_frame.pack(pady=10, padx=20, fill=tk.X)
        
        tk.Label(note_frame, text="Alternative: Use manual pairing with code", font=("Arial", 10, "bold")).pack()
        tk.Label(note_frame, text="Enter the pairing code and IP:Port shown on your device", font=("Arial", 9)).pack()
        
        tk.Button(qr_window, text="Close", command=qr_window.destroy).pack(pady=10)
    
    def install_apk(self):
        """Install APK file to connected device"""
        if not self.is_connected:
            messagebox.showerror("Error", "Please connect to device first")
            return
        
        # Browse for APK file
        apk_path = filedialog.askopenfilename(
            title="Select APK file to install",
            filetypes=[("APK files", "*.apk"), ("All files", "*.*")],
            initialdir=os.path.dirname(os.path.abspath(__file__))
        )
        
        if not apk_path:
            return
        
        # Confirm installation
        apk_name = os.path.basename(apk_path)
        if not messagebox.askyesno("Confirm Installation", f"Install {apk_name} to connected device?"):
            return
        
        try:
            self.log_message(f"📱 Installing {apk_name}...")
            
            # Show progress dialog
            progress_window = tk.Toplevel(self.root)
            progress_window.title("Installing APK")
            progress_window.geometry("300x100")
            progress_window.resizable(False, False)
            
            tk.Label(progress_window, text=f"Installing {apk_name}...").pack(pady=10)
            progress_bar = ttk.Progressbar(progress_window, mode='indeterminate')
            progress_bar.pack(pady=10, padx=20, fill=tk.X)
            progress_bar.start()
            
            # Install APK in separate thread
            def install_thread():
                try:
                    # Get target device ID
                    device_id = self.get_target_device()
                    if device_id:
                        cmd = [self.adb_path, "-s", device_id, "install", "-r", apk_path]
                    else:
                        cmd = [self.adb_path, "install", "-r", apk_path]
                    
                    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
                    
                except subprocess.TimeoutExpired:
                    progress_window.after(0, lambda: progress_bar.stop())
                    progress_window.after(0, lambda: progress_window.destroy())
                    self.log_message(f"❌ Installation timed out after 120 seconds")
                    messagebox.showerror("Installation Timeout", "Installation timed out. Please try again.")
                    return
                except Exception as e:
                    progress_window.after(0, lambda: progress_bar.stop())
                    progress_window.after(0, lambda: progress_window.destroy())
                    self.log_message(f"❌ Installation error: {e}")
                    messagebox.showerror("Error", f"Installation error: {e}")
                    return
                
                try:
                    
                    progress_window.after(0, lambda: progress_bar.stop())
                    progress_window.after(0, lambda: progress_window.destroy())
                    
                    if "Success" in result.stdout or result.returncode == 0:
                        self.log_message(f"✅ Successfully installed {apk_name}")
                        messagebox.showinfo("Success", f"Successfully installed {apk_name}")
                    else:
                        error_msg = result.stdout + result.stderr
                        self.log_message(f"❌ Installation failed: {error_msg}")
                        messagebox.showerror("Installation Failed", f"Failed to install APK:\n{error_msg}")
                        
                except Exception as e:
                    progress_window.after(0, lambda: progress_bar.stop())
                    progress_window.after(0, lambda: progress_window.destroy())
                    self.log_message(f"❌ Installation error: {e}")
                    messagebox.showerror("Error", f"Installation error: {e}")
            
            threading.Thread(target=install_thread, daemon=True).start()
            
        except Exception as e:
            self.log_message(f"❌ APK installation error: {e}")
            messagebox.showerror("Error", f"APK installation error: {e}")
    
    def get_target_device(self):
        """Get the target device ID for ADB commands"""
        try:
            result = subprocess.run([self.adb_path, "devices"], capture_output=True, text=True, timeout=5)
            devices = []
            for line in result.stdout.split('\n'):
                if '\tdevice' in line:
                    device_id = line.split('\t')[0]
                    devices.append(device_id)
            
            if len(devices) == 1:
                return devices[0]
            elif len(devices) > 1:
                # If WiFi connected, prefer WiFi device
                if self.connection_method.get() == "wifi":
                    ip = self.ip_entry.get().strip()
                    port = self.port_entry.get().strip()
                    wifi_device = f"{ip}:{port}"
                    if wifi_device in devices:
                        return wifi_device
                # Return first device as fallback
                return devices[0]
            return None
        except (subprocess.SubprocessError, OSError):
            return None
    
    def show_debug_help(self):
        """Show instructions for enabling debugging"""
        help_window = tk.Toplevel(self.root)
        help_window.title("Enable Android Debugging")
        help_window.geometry("500x400")
        help_window.resizable(False, False)
        
        text_widget = scrolledtext.ScrolledText(help_window, wrap=tk.WORD, font=("Arial", 10))
        text_widget.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        instructions = """📱 ENABLE USB DEBUGGING:

1. Go to Settings → About Phone
2. Tap "Build Number" 7 times to enable Developer Options
3. Go back to Settings → Developer Options
4. Enable "USB Debugging"
5. Connect USB cable and accept debugging prompt

📶 ENABLE WIFI DEBUGGING (Android 11+):

1. Enable USB Debugging first (steps above)
2. Connect via USB once
3. In Developer Options, enable "Wireless Debugging"
4. Tap "Wireless Debugging" → "Pair device with pairing code"
5. Note the IP:Port and pairing code
6. Use the pairing fields in this app

⚠️ SECURITY NOTE:
Only enable debugging on trusted networks. Disable when not needed.

🔍 TROUBLESHOOTING:
• Ensure phone and PC are on same WiFi network
• Try different USB cables/ports
• Restart ADB: adb kill-server && adb start-server
• Check Windows firewall settings"""
        
        text_widget.insert(tk.END, instructions)
        text_widget.config(state=tk.DISABLED)
        
        ttk.Button(help_window, text="Close", command=help_window.destroy).pack(pady=10)
    
    def check_debug_status(self):
        """Check if debugging is enabled on connected device"""
        if not self.adb_path or not self.is_connected:
            return False
            
        try:
            # Check if device is in developer mode
            result = subprocess.run([self.adb_path, "shell", "getprop", "ro.debuggable"], 
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0 and "1" in result.stdout:
                self.log_message("✅ USB Debugging is enabled")
                return True
            else:
                self.log_message("❌ USB Debugging not detected")
                return False
        except (subprocess.SubprocessError, OSError):
            return False
    
    def on_closing(self):
        """Handle application closing"""
        if self.adb_process:
            self.stop_logging()
        self.root.destroy()

if __name__ == "__main__":
    try:
        app = DebugViewer()
        app.run()
    except Exception as e:
        import tkinter.messagebox as mb
        mb.showerror("Startup Error", f"Failed to start application: {e}")
# Vortex AI Android Client

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white) 
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)

Vortex AI is a highly advanced, multi-modal AI client for Android powered by Jetpack Compose. It serves as a centralized hub for interacting with the world's leading artificial intelligence models across multiple domains, including state-of-the-art Large Language Models (LLMs), Image Generation and Editing, and Text-to-Speech (TTS) synthesis.

Designed with robust background processes and seamless user customization, it integrates APIs dynamically and provides an unparalleled AI role-playing and chat experience directly on your mobile device.

---

## 🌟 Core Features

### 💬 Comprehensive LLM Hub (Chat & Roleplay)
- **Multi-Provider Support**: Switch seamlessly between top-tier AI providers including **OpenRouter, Together AI, Gemini, Grok**, and **ModelsLab**.
- **Character Card System**: Full support for importing standard JSON Character Cards (V2) used for AI roleplaying. Features include bulk imports, avatar extractions, and persistent memory.
- **Vortex Mode**: Advanced autonomous interactions utilizing optimized context-handling.
- **Dynamic Context Management**: Adjust maximum tokens, context sizes, and sampling parameters automatically or manually through the Settings UI.

### 🎨 Image Generation & Editing
Vortex AI includes a suite of powerful image generators and manipulators natively integrated.
- **Providers**: Supports **Replicate**, **Together AI (FLUX)**, **ModelsLab**, **ComfyUI**, **Grok**, and **Qwen-Image-Edit**.
- **Capabilities**:
  - Direct Text-to-Image Generation (with support for LoRAs and aspect ratio controls).
  - Image-To-Image Editing (masking, prompt-based enhancement, background alteration).
  - Automatic base64 processing for local image uploads.
- **Asynchronous Persistence**: Generations are handled by Background Services. The app can be fully minimized, and your images will continue generating and automatically persist to your internal database once completed.

### 🗣️ Text-To-Speech (TTS) Engine
Give your characters and AI assistants a voice!
- **Providers**: **ElevenLabs** and **Together AI TTS**.
- **Customization**: Select specific voice profiles, toggle autoplay on message generation, and control speaking rates directly from the chat interface.

### 🔗 Custom API Architecture
Vortex AI features an unprecedented **Custom API Infrastructure** that allows users or developers to link their own unique backend servers dynamically.
- **Schema Mapping**: Import Swagger files or structure your own endpoints for direct compatibility.
- **Database Driven**: Custom endpoints and configurations are stored securely in a local Room Database.
- **Zero-Code Integration**: Set `Base URL` and `API keys` natively in the app's dynamic Settings UI.

---

## 🏗️ Architecture & Technologies

Vortex AI is built following modern Android Development best practices utilizing clean architecture (MVVM) to ensure scalability, offline availability, and performance.

### Tech Stack
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) ensuring fluid, responsive, and dynamic user interfaces.
- **Architecture**: Model-View-ViewModel (MVVM) ensuring clear separation of concerns.
- **Dependency Injection**: [Hilt / Dagger](https://dagger.dev/hilt/) for robust modular DI.
- **Persistence & Storage**: 
  - **Room Database**: Saves Chat histories, Generated Images, Character Cards, and Custom API providers locally.
  - **DataStore**: Securely stores configurations, user preferences, and encrypted API strings decoupled from code execution.
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & OkHttp3 handles resilient API connections and timeout policies for large generations.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) optimized for lazy loading large grids of AI-generated inputs.

---

## 📂 Project Structure

```text
VortexAndroid/
├── app/
│   ├── src/main/java/com/vortexai/android/
│   │   ├── core/         # Core application rules, Room DB Entities & DAOs
│   │   ├── data/         # Repositories and Data Sources
│   │   ├── di/           # Dependency Injection modules (Hilt)
│   │   ├── domain/       # Service layer handling specific API Provider Logic (LLM, Image, Audio)
│   │   ├── ui/           # Jetpack Compose Screens, ViewModels, and Composables (MVVM presentation)
│   │   └── utils/        # Cryptography, API Schema Parsers, Diagnostics, etc.
│   └── src/main/res/     # Static graphics, App icons, manifest bindings
├── test/                 # Python scripts for validating API connections off-device
├── Other Files/          # JVM standalone script configurations
└── build.gradle          # Application-level Gradle build configuration
```

---

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Iguana (2023.2.1) or higher.
- Java 17+
- Android SDK Tool minimum version 24+

### Compilation
1. Clone the repository: 
   ```bash
   git clone https://github.com/mryogeshkumargit/VortexHub.git
   ```
2. Open the project inside **Android Studio**.
3. Let Gradle sync and resolve all dependencies.
4. Hit **Run** (`Shift + F10`) to compile and deploy to a connected physical device or Android Emulator.

### Setting Up API Keys
**Security Note:** Vortex AI guarantees secure implementation by avoiding hard-coded keys in the source tree to prevent GitHub Secret Scanning alerts and credential leaks.

To use the AI generation features in the app:
1. Open the App and navigate to the **Settings** Tab on the Bottom Navigation Bar.
2. Tap into specific provider sub-sections (e.g., `Image Generation`, `LLM Configuration`).
3. Enter your private API Keys strings into the Secure Fields provided. 
4. Hit **Save Settings**. This persists the API keys locally inside `SettingsDataStore`. 

You're ready to generate! 

---

## 🧪 Testing Environment

If you are developing new endpoints, assessing latency, or verifying your API quotas, the repository includes a suite of standalone test scripts:
- Located under `/test/` and `/Modelslab Image Editing Tester/`
- Contains multiple Python GUIs built with `Tkinter` to visually test image edits, generations, and voice generations prior to hooking them up onto the Android Kotlin architecture.

**Running a test script**:
Make sure you replace the empty initialization string or provide your token when prompted.
```bash
python test/test_replicate_gui.py
```

---

## 🛠️ Contribution & Development

When contributing or adding new tools to Vortex Hub:
1. Ensure your network calls are defined under `domain/service/`.
2. Provide backwards compatibility mapping if modifying the `Room` Schemas.
3. Keep the presentation logic wrapped within `ViewModel` to ensure `Jetpack Compose` correctly reacts to State Flow mutations without locking the UI thread.
4. Never hardcode sensitive API keys within `.kt` files. Use `SettingsDataStore` hooks.

## 📄 License

Vortex AI is a proprietary hub intended for development, modular AI-roleplay, and creative generation management. Review project specifics regarding replication limits and external API liabilities based on integrated providers.

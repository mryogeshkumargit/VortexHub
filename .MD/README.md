# VortexAndroid - Native Android App

## 📱 Overview

VortexAndroid is a native Android application that brings the full power of the VortexAI Chatbot Platform to mobile devices. Built with modern Android development practices using Jetpack Compose, it provides a seamless, native mobile experience while maintaining full compatibility with the existing VortexAI ecosystem.

## ✨ Features

### Core Functionality
- **Real-time Chat** - Seamless messaging with AI characters using WebSocket
- **Character Management** - Full CRUD operations with Character Card V2 support
- **Image Generation** - FLUX and SDXL integration with face-consistent avatars
- **Voice Processing** - Speech-to-text and text-to-speech capabilities
- **Lorebook System** - Advanced world-building with 20,000 token budget
- **Multi-Provider LLM** - Support for Local API, OpenAI, Anthropic, Together, Groq, Gemini

### Mobile-Optimized Features
- **Offline Functionality** - Local data storage with cloud synchronization
- **Background Services** - Image generation and sync in the background
- **Push Notifications** - Real-time alerts for messages and completions
- **Material Design 3** - Modern, adaptive UI following Android design guidelines
- **Dark/Light Theme** - Automatic theme switching based on system preferences
- **Edge-to-Edge Display** - Full screen experience on modern Android devices

## 🏗️ Architecture

The app follows **Clean Architecture** principles with **MVVM** pattern:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Presentation  │    │     Domain      │    │      Data       │
│                 │    │                 │    │                 │
│ • UI (Compose)  │◄──►│ • Use Cases     │◄──►│ • Repository    │
│ • ViewModels    │    │ • Models        │    │ • API Services  │
│ • Navigation    │    │ • Interfaces    │    │ • Local DB      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Technology Stack

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture + MVVM
- **Dependency Injection**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Async Operations**: Coroutines + Flow
- **Navigation**: Navigation Compose
- **Background Work**: WorkManager

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Arctic Fox or later
- **Minimum SDK**: 24 (Android 7.0) - 87% market coverage
- **Target SDK**: 34 (Android 14)
- **Java Version**: 17
- **Kotlin Version**: 1.9.22

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd VortexAndroid
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the VortexAndroid folder

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

### Configuration

1. **API Configuration**
   - Set your local API URL in `BuildConfig.BASE_URL`
   - Configure API keys in the Settings screen
   - Enable desired LLM providers

2. **Debug vs Release**
   - Debug builds use `http://localhost:5000/` by default
   - Release builds use production API URL
   - Modify in `app/build.gradle` as needed

## 📁 Project Structure

```
app/src/main/java/com/vortexai/android/
├── ui/                              # Presentation Layer
│   ├── screens/                     # Screen Composables
│   │   ├── home/                    # Home screen
│   │   ├── chat/                    # Chat interface
│   │   ├── characters/              # Character management
│   │   ├── image/                   # Image generation
│   │   └── settings/                # App settings
│   ├── components/                  # Reusable UI components
│   ├── theme/                       # Material 3 theme system
│   └── navigation/                  # Navigation setup
├── data/                            # Data Layer
│   ├── local/                       # Room database
│   ├── remote/                      # API services
│   └── repository/                  # Repository implementations
├── domain/                          # Business Logic Layer
│   ├── models/                      # Domain models
│   ├── usecases/                    # Use cases
│   └── repository/                  # Repository interfaces
├── di/                              # Dependency Injection
├── utils/                           # Utility classes
└── services/                        # Background services
```

## 🎨 UI/UX Design

### Material Design 3
- **Dynamic Colors**: Adaptive color schemes (can be disabled for brand consistency)
- **Typography**: Custom typography scale optimized for chat interfaces
- **Shapes**: Rounded corners with varying radii for different components
- **Motion**: Smooth transitions and animations

### Custom Components
- **Chat Bubbles**: Asymmetric bubbles for user/bot messages
- **Character Cards**: Rich character display with avatars and metadata
- **Image Gallery**: Grid layout for generated images
- **Voice Controls**: Recording and playback interfaces

### Responsive Design
- **Adaptive Layouts**: Optimized for different screen sizes
- **Orientation Support**: Portrait and landscape modes
- **Accessibility**: Full support for screen readers and accessibility services

## 🔧 Development

### Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Testing

The project includes comprehensive testing:

- **Unit Tests**: Business logic and repository tests
- **Integration Tests**: API and database integration
- **UI Tests**: Compose UI testing with semantics
- **Performance Tests**: Memory and battery usage

### Code Style

- **Kotlin Coding Conventions**: Following official Kotlin style guide
- **Compose Best Practices**: Stateless composables, proper state hoisting
- **Clean Architecture**: Clear separation of concerns
- **SOLID Principles**: Maintainable and testable code

## 📦 Dependencies

### Core Dependencies
```kotlin
// Jetpack Compose
implementation platform('androidx.compose:compose-bom:2024.02.00')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.material3:material3'

// Hilt Dependency Injection
implementation 'com.google.dagger:hilt-android:2.48'

// Room Database
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.room:room-ktx:2.6.1'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'

// Image Loading
implementation 'io.coil-kt:coil-compose:2.5.0'
```

### Full dependency list available in `app/build.gradle`

## 🔒 Security

- **API Key Encryption**: Secure storage of API keys using Android Keystore
- **Network Security**: Certificate pinning and secure communication
- **Data Protection**: Encrypted local database for sensitive data
- **Permissions**: Minimal required permissions with runtime requests
- **Biometric Auth**: Optional biometric authentication for app access

## 📱 Permissions

```xml
<!-- Required -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Optional -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 🚀 Performance

### Optimization Strategies
- **Image Caching**: Intelligent caching with Coil
- **Database Optimization**: Efficient queries and indexing
- **Memory Management**: Proper lifecycle handling
- **Background Processing**: WorkManager for heavy operations
- **Network Optimization**: Request batching and caching

### Performance Targets
- **App Startup**: < 3 seconds cold start
- **Chat Response**: < 2 seconds for local API
- **Memory Usage**: < 512MB average
- **Battery Usage**: < 5% per hour active use

## 🔄 Development Roadmap

### Phase 1: Core Foundation ✅
- [x] Project setup and architecture
- [x] Navigation and basic UI
- [x] Theme system and Material 3
- [x] Basic screen layouts

### Phase 2: Data Layer (In Progress)
- [ ] Room database implementation
- [ ] Repository pattern setup
- [ ] API service integration
- [ ] Local data management

### Phase 3: Network Integration
- [ ] Multi-provider LLM support
- [ ] WebSocket real-time chat
- [ ] Image generation APIs
- [ ] Voice processing integration

### Phase 4: Advanced Features
- [ ] Character Card V2 support
- [ ] Lorebook system
- [ ] Background services
- [ ] Push notifications

### Phase 5: Polish & Testing
- [ ] Comprehensive testing
- [ ] Performance optimization
- [ ] Security hardening
- [ ] Production deployment

## 🤝 Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit changes**: `git commit -m 'Add amazing feature'`
4. **Push to branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Development Guidelines
- Follow Kotlin coding conventions
- Write comprehensive tests
- Update documentation
- Ensure accessibility compliance
- Test on multiple devices and API levels

## 📄 License

This project is part of the VortexAI ecosystem. See the main project for licensing information.

## 🆘 Support

- **Issues**: Report bugs and feature requests on GitHub
- **Documentation**: Comprehensive guides in `/docs`
- **Community**: Join our development discussions

## 🖼️ ModelsLab Image Generation (v6)

The app now supports **ModelsLab** for advanced text-to-image workflows.

### Supported Endpoints
| Flow | Endpoint | Notes |
|------|----------|-------|
| Text → Image | `/api/v6/images/text2img` | Standard community models (requires `model_id`) |
| Image → Image | `/api/v6/images/img2img` | Provide `init_image` (base64/URL) & `strength` |
| LoRA Text → Image | `/api/v6/images/lora/text2img` | `lora_model` & `lora_strength` |
| Realtime Stable-Diffusion | `/api/v6/realtime/text2img` | No `model_id` needed |
| FLUX | `/api/v6/flux/fluxtext2img` | Fast diffusion backend |

### Settings → Image → Provider = "ModelsLab"
1. Enter your **API key**.
2. Select **Workflow**: `default`, `realtime`, or `flux`.
3. (Optional) Toggle **Use character image as source** to enable img-to-img using the active character portrait.
4. (Optional) Specify a **LoRA model** and adjust **LoRA strength**.
5. Pick an **Image Model** (list fetched from ModelsLab public-model endpoint) or keep the default.

> ⚠️  Realtime & FLUX workflows ignore the model field.

### Error Handling
All ModelsLab responses map to structured `AppError`s. Common cases:
* `RATE_LIMIT` – Too many requests (HTTP 429)
* `AUTH_FAILED` – Invalid API key (HTTP 401)
* `NETWORK_ERROR` – Device offline / DNS issues

The UI surfaces a user-friendly message while logging the technical code via Timber.

### Testing
`ModelsLabImageApiTest` covers success and failure scenarios using `MockWebServer`.

Run:
```bash
./gradlew testDebugUnitTest --tests "*ModelsLabImageApiTest*"
```

---

**VortexAndroid** - Bringing AI conversations to your pocket with native Android performance and modern design principles. 
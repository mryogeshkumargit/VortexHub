# Custom API System - Visual Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │   Settings   │  │   Settings   │  │   Settings   │        │
│  │     LLM      │  │    Image     │  │    Image     │        │
│  │ Configuration│  │  Generation  │  │   Editing    │        │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘        │
│         │                 │                 │                  │
│         └─────────────────┴─────────────────┘                  │
│                           │                                     │
│                           ▼                                     │
│         ┌─────────────────────────────────────┐               │
│         │  "Configure Custom APIs" Button     │               │
│         └─────────────────┬───────────────────┘               │
│                           │                                     │
└───────────────────────────┼─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  CUSTOM API PROVIDER SCREEN                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Provider List                                          │  │
│  │  ┌────────────────────────────────────────────────┐    │  │
│  │  │  ✓ My OpenAI API          [Enabled] [Delete]  │    │  │
│  │  │    https://api.openai.com                      │    │  │
│  │  │                                                 │    │  │
│  │  │    Endpoints:                                   │    │  │
│  │  │    • /v1/chat/completions (POST)               │    │  │
│  │  │                                                 │    │  │
│  │  │    Models:                                      │    │  │
│  │  │    • gpt-4 (GPT-4)                             │    │  │
│  │  │    • gpt-3.5-turbo (GPT-3.5 Turbo)            │    │  │
│  │  │                                                 │    │  │
│  │  │    [Test Connection]                           │    │  │
│  │  └────────────────────────────────────────────────┘    │  │
│  │                                                          │  │
│  │  [+ Add Provider]                                       │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      VIEW MODEL LAYER                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CustomApiProviderViewModel                                     │
│  ├─ loadProviders()                                            │
│  ├─ saveProvider()                                             │
│  ├─ saveEndpoint()                                             │
│  ├─ saveModel()                                                │
│  ├─ testConnection()                                           │
│  └─ deleteProvider()                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     REPOSITORY LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CustomApiProviderRepository                                    │
│  ├─ getProvidersByType()                                       │
│  ├─ getEndpointsByProvider()                                   │
│  ├─ getModelsByProvider()                                      │
│  ├─ saveProvider()                                             │
│  ├─ updateProvider()                                           │
│  └─ deleteProvider()                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Room Database (VortexDatabase)                                │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  custom_api_providers                                   │  │
│  │  ├─ id (PK)                                             │  │
│  │  ├─ name                                                │  │
│  │  ├─ type (TEXT_GENERATION, IMAGE_GENERATION, etc.)     │  │
│  │  ├─ baseUrl                                             │  │
│  │  ├─ apiKey (encrypted)                                  │  │
│  │  └─ isEnabled                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  custom_api_endpoints                                   │  │
│  │  ├─ id (PK)                                             │  │
│  │  ├─ providerId (FK)                                     │  │
│  │  ├─ endpointPath                                        │  │
│  │  ├─ httpMethod                                          │  │
│  │  ├─ requestSchemaJson                                   │  │
│  │  ├─ responseSchemaJson                                  │  │
│  │  └─ purpose                                             │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  custom_api_models                                      │  │
│  │  ├─ id (PK)                                             │  │
│  │  ├─ providerId (FK)                                     │  │
│  │  ├─ modelId                                             │  │
│  │  ├─ displayName                                         │  │
│  │  ├─ capabilitiesJson                                    │  │
│  │  └─ isActive                                            │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     EXECUTION LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CustomApiExecutor                                              │
│  ├─ executeRequest()                                           │
│  ├─ parseResponse()                                            │
│  ├─ buildRequest()                                             │
│  └─ replacePlaceholders()                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      NETWORK LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  OkHttpClient                                                   │
│  ├─ Connection pooling                                         │
│  ├─ Timeout configuration                                      │
│  ├─ TLS/SSL                                                    │
│  └─ Interceptors                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      EXTERNAL API                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  https://api.example.com/v1/chat/completions                   │
│                                                                 │
│  Request:                                                       │
│  {                                                              │
│    "model": "gpt-4",                                           │
│    "messages": [...],                                          │
│    "temperature": 0.7                                          │
│  }                                                              │
│                                                                 │
│  Response:                                                      │
│  {                                                              │
│    "choices": [                                                │
│      {                                                          │
│        "message": {                                            │
│          "content": "AI response..."                           │
│        }                                                        │
│      }                                                          │
│    ]                                                            │
│  }                                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagram

```
┌──────────┐
│   User   │
└────┬─────┘
     │ 1. Clicks "Configure Custom APIs"
     ▼
┌─────────────────────┐
│  Settings Screen    │
└────┬────────────────┘
     │ 2. Navigates to
     ▼
┌─────────────────────────────┐
│  CustomApiProviderScreen    │
│  - Shows provider list      │
│  - Add/Edit/Delete buttons  │
└────┬────────────────────────┘
     │ 3. User adds provider
     ▼
┌─────────────────────────────┐
│  AddProviderDialog          │
│  - Name: "My API"           │
│  - URL: "https://..."       │
│  - API Key: "sk-..."        │
└────┬────────────────────────┘
     │ 4. Saves provider
     ▼
┌─────────────────────────────┐
│  ViewModel                  │
│  - Validates input          │
│  - Calls repository         │
└────┬────────────────────────┘
     │ 5. Persists data
     ▼
┌─────────────────────────────┐
│  Repository                 │
│  - Encrypts API key         │
│  - Saves to database        │
└────┬────────────────────────┘
     │ 6. Stores in Room
     ▼
┌─────────────────────────────┐
│  Room Database              │
│  - custom_api_providers     │
│  - custom_api_endpoints     │
│  - custom_api_models        │
└────┬────────────────────────┘
     │ 7. Returns success
     ▼
┌─────────────────────────────┐
│  UI Updates                 │
│  - Shows new provider       │
│  - Enables configuration    │
└─────────────────────────────┘
```

## Request Execution Flow

```
┌──────────┐
│   User   │
└────┬─────┘
     │ 1. Makes API call (e.g., sends chat message)
     ▼
┌─────────────────────────────┐
│  ChatViewModel              │
│  - Prepares request         │
└────┬────────────────────────┘
     │ 2. Calls service
     ▼
┌─────────────────────────────┐
│  ChatLLMService             │
│  - Gets provider config     │
└────┬────────────────────────┘
     │ 3. Loads from database
     ▼
┌─────────────────────────────┐
│  Repository                 │
│  - Fetches provider         │
│  - Fetches endpoint         │
│  - Fetches model            │
└────┬────────────────────────┘
     │ 4. Returns config
     ▼
┌─────────────────────────────┐
│  CustomApiExecutor          │
│  - Builds request           │
│  - Replaces placeholders    │
└────┬────────────────────────┘
     │ 5. Executes HTTP request
     ▼
┌─────────────────────────────┐
│  OkHttpClient               │
│  - Sends to external API    │
└────┬────────────────────────┘
     │ 6. Receives response
     ▼
┌─────────────────────────────┐
│  CustomApiExecutor          │
│  - Parses response          │
│  - Extracts data            │
└────┬────────────────────────┘
     │ 7. Returns result
     ▼
┌─────────────────────────────┐
│  ChatLLMService             │
│  - Processes response       │
└────┬────────────────────────┘
     │ 8. Updates UI
     ▼
┌─────────────────────────────┐
│  ChatViewModel              │
│  - Displays message         │
└─────────────────────────────┘
```

## Schema Processing

```
Request Schema:
┌─────────────────────────────────────────┐
│ {                                       │
│   "headers": {                          │
│     "Authorization": "Bearer {{apiKey}}"│
│   },                                    │
│   "body": {                             │
│     "model": "{{model}}",               │
│     "messages": "{{messages}}",         │
│     "temperature": "{{temperature}}"    │
│   }                                     │
│ }                                       │
└─────────────────────────────────────────┘
         │
         │ Placeholder Replacement
         ▼
┌─────────────────────────────────────────┐
│ {                                       │
│   "headers": {                          │
│     "Authorization": "Bearer sk-abc123" │
│   },                                    │
│   "body": {                             │
│     "model": "gpt-4",                   │
│     "messages": [...],                  │
│     "temperature": 0.7                  │
│   }                                     │
│ }                                       │
└─────────────────────────────────────────┘
         │
         │ HTTP Request
         ▼
┌─────────────────────────────────────────┐
│  External API                           │
└─────────────────────────────────────────┘
         │
         │ HTTP Response
         ▼
┌─────────────────────────────────────────┐
│ {                                       │
│   "choices": [                          │
│     {                                   │
│       "message": {                      │
│         "content": "AI response..."     │
│       }                                 │
│     }                                   │
│   ]                                     │
│ }                                       │
└─────────────────────────────────────────┘
         │
         │ Response Schema
         ▼
Response Schema:
┌─────────────────────────────────────────┐
│ {                                       │
│   "dataPath": "choices[0].message.content"│
│ }                                       │
└─────────────────────────────────────────┘
         │
         │ Path Extraction
         ▼
┌─────────────────────────────────────────┐
│ "AI response..."                        │
└─────────────────────────────────────────┘
```

## Component Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │  Settings Tabs   │  │  Custom API      │               │
│  │  - LLM Config    │  │  Provider Screen │               │
│  │  - Image Gen     │  │  - Provider List │               │
│  │  - Image Edit    │  │  - Dialogs       │               │
│  └────────┬─────────┘  └────────┬─────────┘               │
└───────────┼────────────────────┼─────────────────────────────┘
            │                    │
            │                    │
┌───────────┼────────────────────┼─────────────────────────────┐
│           │   Domain Layer     │                             │
│  ┌────────▼─────────┐  ┌──────▼──────────┐                 │
│  │  SettingsViewModel│  │  CustomApi      │                 │
│  │  - Provider      │  │  ProviderViewModel│                │
│  │    selection     │  │  - CRUD ops     │                 │
│  └────────┬─────────┘  └──────┬──────────┘                 │
└───────────┼────────────────────┼─────────────────────────────┘
            │                    │
            │                    │
┌───────────┼────────────────────┼─────────────────────────────┐
│           │    Data Layer      │                             │
│  ┌────────▼─────────┐  ┌──────▼──────────┐                 │
│  │  DataStore       │  │  Repository     │                 │
│  │  - Old settings  │  │  - Provider     │                 │
│  │    (preserved)   │  │  - Endpoint     │                 │
│  └──────────────────┘  │  - Model        │                 │
│                        └──────┬──────────┘                 │
│                               │                             │
│                        ┌──────▼──────────┐                 │
│                        │  Room Database  │                 │
│                        │  - Providers    │                 │
│                        │  - Endpoints    │                 │
│                        │  - Models       │                 │
│                        │  - Parameters   │                 │
│                        └─────────────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

## State Management

```
┌─────────────────────────────────────────┐
│  CustomApiProviderUiState               │
├─────────────────────────────────────────┤
│  - providers: List<CustomApiProvider>   │
│  - selectedProvider: CustomApiProvider? │
│  - endpoints: List<CustomApiEndpoint>   │
│  - models: List<CustomApiModel>         │
│  - selectedModel: CustomApiModel?       │
│  - parameters: List<CustomApiParameter> │
│  - isLoading: Boolean                   │
│  - error: String?                       │
│  - successMessage: String?              │
└─────────────────────────────────────────┘
         │
         │ StateFlow
         ▼
┌─────────────────────────────────────────┐
│  UI Components                          │
│  - Observe state changes                │
│  - Render UI accordingly                │
│  - Handle user interactions             │
└─────────────────────────────────────────┘
```

## Error Handling Flow

```
┌──────────────┐
│  API Call    │
└──────┬───────┘
       │
       ▼
┌──────────────────────┐
│  Try-Catch Block     │
└──────┬───────────────┘
       │
       ├─ Success ──────────────────┐
       │                            │
       └─ Error ───┐                │
                   │                │
                   ▼                ▼
         ┌─────────────────┐  ┌─────────────┐
         │  Error Handler  │  │  Success    │
         │  - Network      │  │  Handler    │
         │  - Auth         │  │  - Parse    │
         │  - Validation   │  │  - Display  │
         │  - Unknown      │  └─────────────┘
         └─────────┬───────┘
                   │
                   ▼
         ┌─────────────────┐
         │  Error Message  │
         │  - User-friendly│
         │  - Actionable   │
         │  - Detailed     │
         └─────────────────┘
```

---

**Legend:**
- `┌─┐` = Component boundary
- `│` = Vertical connection
- `─` = Horizontal connection
- `▼` = Data flow direction
- `├─` = Branch point
- `└─` = End point

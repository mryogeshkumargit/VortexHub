# OpenRouter API Endpoints

OpenRouter provides a unified API that gives developers access to hundreds of AI models through a single endpoint, with automatic fallbacks and cost-effective provider routing. The API is designed to be compatible with the OpenAI API specification, with normalized schemas across models and providers. This document includes details on chat completions, text completions, image generation, audio generation (text-to-speech and speech-to-text), and recommended models for specific use cases, including roleplay.

## General API Details
- **Base URL**: `https://openrouter.ai/api/v1`
- **Authentication**: Use your OpenRouter API key in the Authorization header as a Bearer token: `Authorization: Bearer YOUR_API_KEY`. Create an API key on the [OpenRouter Keys page](https://openrouter.ai/docs#authentication).
- **Optional Headers**: To make your app discoverable on OpenRouter's rankings, include:
  - `HTTP-Referer`: Your site URL.
  - `X-Title`: Your site name.

## 1. Chat Completions
Interact with language models using the chat completions endpoint, supporting a variety of models with a normalized schema similar to OpenAI's Chat API.

- **Endpoint**: `https://openrouter.ai/api/v1/chat/completions`
- **Method**: POST
- **Request Body (JSON)**:
  - `model`: The model ID (e.g., `meta-llama/llama-3-70b-instruct`). If omitted, the user's default model is used.
  - `messages`: An array of message objects with `role` (system, user, or assistant) and `content` fields.
  - (Optional) `temperature` (range: [0, 2], default: 1.0), `max_tokens` (range: [1, context_length]), `top_p` (range: (0, 1]), `stream` (set to `true` for Server-Sent Events), etc.
  - `provider`: Optional object to specify provider preferences (e.g., `order`, `sort: "price"` or `sort: "throughput"`, `require_parameters: true`).
- **Response**: Normalized to include `choices` as an array, with `delta` (for streaming) or `message` properties. `finish_reason` is standardized to `tool_calls`, `stop`, `length`, `content_filter`, or `error`. Token counts use a model-agnostic GPT-4o tokenizer.

### Example using curl:
```bash
curl -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "Content-Type: application/json" \
  -H "HTTP-Referer: YOUR_SITE_URL" \
  -H "X-Title: YOUR_SITE_NAME" \
  -d '{
    "model": "meta-llama/llama-3-70b-instruct",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "What is the capital of France?"}
    ],
    "temperature": 0.7,
    "max_tokens": 50,
    "stream": false
  }'
```

## 2. Completions (Text-Only)
For text-only completion requests, useful for models that don't support chat formats.

- **Endpoint**: `https://openrouter.ai/api/v1/completions`
- **Method**: POST
- **Request Body (JSON)**:
  - `model`: The model ID.
  - `prompt`: The input text.
  - (Optional) `temperature`, `max_tokens`, `top_p`, `frequency_penalty` (range: [-2, 2]), `presence_penalty` (range: [-2, 2]), `repetition_penalty` (range: (0, 2]), `stream`, etc.
  - `provider`: Optional provider routing preferences.
- **Response**: Similar to chat completions, with normalized `choices` array and `finish_reason`.

### Example using curl:
```bash
curl -X POST "https://openrouter.ai/api/v1/completions" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/llama-3-70b-instruct",
    "prompt": "Write a short story about a robot.",
    "temperature": 0.8,
    "max_tokens": 100
  }'
```

## 3. Image Generation
OpenRouter supports image generation through multimodal models that accept text prompts and/or image inputs via the chat completions endpoint. Images can be sent as URLs or base64-encoded data in the `messages` array.

- **Endpoint**: `https://openrouter.ai/api/v1/chat/completions`
- **Method**: POST
- **Request Body (JSON)**:
  - `model`: A multimodal model ID that supports image generation (e.g., `qwen/qwen-2-vl-72b-instruct`).
  - `messages`: An array including a text prompt and optionally an `image_url` (URL or base64-encoded image).
  - (Optional) `width`, `height`, or other model-specific parameters.
  - `provider`: Optional provider routing preferences.
- **Response**: Returns generated image data or descriptions, depending on the model. Check model documentation for output format.

### Example using curl (with base64-encoded image):
```bash
curl -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen/qwen-2-vl-72b-instruct",
    "messages": [
      {
        "role": "user",
        "content": [
          {"type": "text", "text": "Generate an image of a futuristic city at night."},
          {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,/9j/4AA..."}}
        ]
      }
    ],
    "max_tokens": 100
  }'
```

**Note**: The number of images that can be sent in a single request varies by provider and model. It’s recommended to place the text prompt before the image in the `messages` array. Check the [OpenRouter documentation](https://openrouter.ai/docs) for supported models and parameters.[](https://openrouter.ai/docs/features/images-and-pdfs)

## 4. Audio Generation (Text-to-Speech and Speech-to-Text)
OpenRouter does not currently offer dedicated endpoints for text-to-speech (TTS) or speech-to-text (STT) as of the latest documentation. However, some multimodal models accessible via the chat completions endpoint may support audio-related tasks, and OpenRouter’s integration with platforms like SillyTavern suggests compatibility with TTS/STT through third-party tools or custom setups. For example, community-driven projects like `openedai-speech` provide OpenAI-compatible TTS/STT using models like XTTS-v2 or Piper TTS, which can be integrated with OpenRouter.[](https://github.com/Aider-AI/aider/issues/1972)[](https://github.com/matatonic/openedai-speech)

### Text-to-Speech (TTS)
- **Endpoint**: Not natively supported; use `/api/v1/chat/completions` with a compatible model or third-party integration.
- **Method**: POST
- **Request Body (JSON)**:
  - `model`: A model supporting TTS (e.g., via integration with `openedai-speech` or platforms like SillyTavern).
  - `messages`: Include the text to be converted to speech.
  - `voice`: Specify the voice if supported by the model or integration (e.g., `alloy`, `nova`).
  - (Optional) `response_format` (e.g., `mp3`), `speed`, etc.
- **Response**: Audio data or a URL to the generated audio, depending on the integration.

### Example using curl (hypothetical, with third-party integration):
```bash
curl -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "custom/tts-integration",
    "messages": [
      {"role": "user", "content": "Hello, this is a test of text-to-speech."}
    ],
    "voice": "alloy",
    "response_format": "mp3"
  }' \
  --output speech_output.mp3
```

### Speech-to-Text (STT)
- **Endpoint**: Not natively supported; use `/api/v1/chat/completions` with a compatible model or third-party integration (e.g., Whisper-based models via `openedai-speech`).
- **Method**: POST
- **Request Body (Multipart Form Data or JSON)**:
  - `model`: A model supporting STT (e.g., `openai/whisper-large-v3` via integration).
  - `file`: The audio file (e.g., `audio.mp3`) or base64-encoded audio data.
- **Response**: Transcribed text, depending on the model or integration.

### Example using curl (hypothetical, with third-party integration):
```bash
curl -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "Content-Type: multipart/form-data" \
  -F "model=openai/whisper-large-v3" \
  -F "file=@/path/to/audio.mp3"
```

**Note**: For TTS/STT, developers often use OpenRouter with external tools like `openedai-speech` (using XTTS-v2 or Piper TTS) or platforms like SillyTavern for roleplay scenarios. Check the [OpenRouter community forums](https://openrouter.ai) or GitHub repositories like `matatonic/openedai-speech` for integration details.[](https://github.com/matatonic/openedai-speech)[](https://www.reddit.com/r/SillyTavernAI/comments/1hhd05t/recommendation_for_openrouter_models/)

## 5. Generation Stats
Retrieve token counts and cost details for a specific generation.

- **Endpoint**: `https://openrouter.ai/api/v1/generation`
- **Method**: GET
- **Parameters**:
  - `id`: The generation ID returned from a completion request.
- **Response**: Includes token counts (prompt and completion) and cost in USD, based on the model's native tokenizer.

### Example using curl:
```bash
curl -X GET "https://openrouter.ai/api/v1/generation?id=GENERATION_ID" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY"
```

## 6. Model and Provider Information
List available models or endpoints for a specific model.

- **List Models**:
  - **Endpoint**: `https://openrouter.ai/api/v1/models`
  - **Method**: GET
  - **Response**: Returns a list of models with fields like `id`, `pricing`, and `supported_parameters`.
  - **Example**:
    ```bash
    curl -X GET "https://openrouter.ai/api/v1/models" \
      -H "Authorization: Bearer $OPENROUTER_API_KEY"
    ```

- **List Endpoints for a Model**:
  - **Endpoint**: `https://openrouter.ai/api/v1/models/:author/:slug/endpoints`
  - **Method**: GET
  - **Parameters**: `author` and `slug` (e.g., `meta-llama/llama-3-70b-instruct`).
  - **Response**: Lists provider endpoints for the specified model.

### Example using curl:
```bash
curl -X GET "https://openrouter.ai/api/v1/models/meta-llama/llama-3-70b-instruct/endpoints" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY"
```

## 7. Credit and Key Management
Manage API keys and check credit balance programmatically.

- **Get Credits**:
  - **Endpoint**: `https://openrouter.ai/api/v1/credits`
  - **Method**: GET
  - **Response**: Returns the credit balance for the account.
  - **Example**:
    ```bash
    curl -X GET "https://openrouter.ai/api/v1/credits" \
      -H "Authorization: Bearer $OPENROUTER_API_KEY"
    ```

- **Get API Key Details**:
  - **Endpoint**: `https://openrouter.ai/api/v1/keys/:hash`
  - **Method**: GET
  - **Parameters**: `hash` (the API key identifier).
  - **Response**: Returns details about a specific API key. Requires a Provisioning API key.
  - **Example**:
    ```bash
    curl -X GET "https://openrouter.ai/api/v1/keys/KEY_HASH" \
      -H "Authorization: Bearer $PROVISIONING_API_KEY"
    ```

- **Create API Key**:
  - **Endpoint**: `https://openrouter.ai/api/v1/keys`
  - **Method**: POST
  - **Request Body (JSON)**: Specify key name and optional credit limit.
  - **Response**: Returns the new API key. Requires a Provisioning API key.
  - **Example**:
    ```bash
    curl -X POST "https://openrouter.ai/api/v1/keys" \
      -H "Authorization: Bearer $PROVISIONING_API_KEY" \
      -H "Content-Type: application/json" \
      -d '{"name": "MyAppKey", "credit_limit": 10}'
    ```

## Recommended Models for Specific Use Cases on OpenRouter

### Top 5 Image Generation Models for Realistic Images
These models, accessible via the chat completions endpoint, are recommended for generating photorealistic images based on their multimodal capabilities and community feedback:
- `qwen/qwen-2-vl-72b-instruct`: Advanced vision-language model optimized for high-quality image generation with detailed text prompts. Supports complex scenes and high-resolution outputs.[](https://apidog.com/blog/free-ai-models/)
- `xai/flux-1.1-pro`: High-fidelity image generation with strong detail and realism, ideal for photorealistic landscapes and portraits.
- `stabilityai/stable-diffusion-xl-base-1.0`: Widely used for generating detailed, realistic images with customizable styles.
- `stabilityai/stable-diffusion-3.5-large`: Enhanced version with improved realism and prompt adherence for complex scenes.
- `black-forest-labs/flux-1-schnell`: Fast and efficient model for realistic image generation, suitable for rapid prototyping.

**Note**: Use these model IDs exactly as listed in the `model` parameter. Check [OpenRouter’s models page](https://openrouter.ai/models) for provider availability and pricing.

### Top 5 Speech-to-Text (STT) Models for Realistic Transcription
While OpenRouter does not natively support dedicated STT endpoints, the following models or integrations are recommended for accurate transcription, based on community usage and third-party integrations like `openedai-speech`:
- `openai/whisper-large-v3`: High-accuracy transcription model, supports multiple languages and handles noisy environments well. Requires integration via third-party tools.[](https://github.com/Aider-AI/aider/issues/1972)
- `deepseek/whisper-v3`: Efficient and accurate, optimized for technical and conversational speech.
- `openai/gpt-4o-transcribe`: Advanced STT model with improved word error rates and accent handling, accessible via custom integrations.[](https://dev.to/projedefteri/openaifm-openais-newest-text-to-speech-model-proje-defteri-1gd5)
- `openai/gpt-4o-mini-transcribe`: Lightweight version of gpt-4o-transcribe, offering high accuracy with lower computational cost.[](https://dev.to/projedefteri/openaifm-openais-newest-text-to-speech-model-proje-defteri-1gd5)
- `coqui/xtts-v2-transcribe`: Community-driven model with strong performance in multilingual transcription, available via `openedai-speech`.[](https://github.com/matatonic/openedai-speech)

**Note**: STT functionality typically requires third-party tools or custom integrations. Check model-specific documentation for supported audio formats and parameters.

### Top 5 Text-to-Speech (TTS) Voices for Roleplay
For roleplay scenarios, the following voices are recommended for their expressiveness and realism, particularly when used with TTS integrations like `openedai-speech` or platforms like SillyTavern. These voices are available via models like `coqui/xtts-v2` or `parler-tts`:
- `alloy` (via `tts-1` or `tts-1-hd`): A versatile, neutral voice with clear enunciation, ideal for professional or narrative roleplay.[](https://github.com/matatonic/openedai-speech)
- `nova` (via `tts-1`): A warm, female voice with emotional depth, suitable for empathetic characters.
- `shimmer` (via `tts-1-hd`): A vibrant, expressive voice perfect for dynamic roleplay scenarios.[](https://github.com/matatonic/openedai-speech)
- `parler-tts: storyteller` (via `parler-tts`): A narrative-focused voice with dramatic flair, ideal for storytelling or fantasy roleplay.[](https://github.com/matatonic/openedai-speech)
- `xtts: expressive female` (via `coqui/xtts-v2`): A customizable voice with emotion exaggeration control, great for character-driven roleplay.[](https://github.com/matatonic/openedai-speech)

**Note**: Voice availability depends on the TTS model and integration used. For custom voices, provide a 6–30 second WAV sample for cloning (e.g., with `xtts-v2`). See `matatonic/openedai-speech` on GitHub for setup details.[](https://github.com/matatonic/openedai-speech)

### Top 5 Serverless Models for Roleplay (Uncensored)
These models are highly regarded by the community (e.g., SillyTavern users) for uncensored and creative roleplay, accessible via the chat completions endpoint:
- `nousresearch/hermes-3-llama-3.1-405b`: High-capacity model with strong creative output, though some users note it may not be fully uncensored.[](https://medium.com/%40mahesh.paul.j/implementing-a-free-llm-ai-using-openrouter-ai-a-step-by-step-guide-8990d3e5cf77)[](https://www.reddit.com/r/SillyTavernAI/comments/1hhd05t/recommendation_for_openrouter_models/)
- `wizardlm/wizardlm-2-8x22b`: Popular for its unmoderated responses and cost-effectiveness, ideal for flexible roleplay scenarios.[](https://www.reddit.com/r/SillyTavernAI/comments/1hhd05t/recommendation_for_openrouter_models/)
- `meta-llama/llama-3-70b-instruct`: Balanced model with strong conversational abilities, suitable for creative and uncensored roleplay.
- `mistral/unslopnemo-12b`: Fully uncensored, compact model with good performance for NSFW and creative roleplay.[](https://www.reddit.com/r/SillyTavernAI/comments/1hhd05t/recommendation_for_openrouter_models/)
- `austism/chronos-hermes-13b`: Known for creative and unfiltered responses, ideal for niche roleplay scenarios.

**Note**: Uncensored models may still have provider-specific restrictions. Check [OpenRouter’s rankings](https://openrouter.ai/rankings/roleplay) for usage trends.[](https://www.reddit.com/r/SillyTavernAI/comments/1hhd05t/recommendation_for_openrouter_models/)

## Notes
- **Streaming**: Enable streaming by setting `stream: true` in the request body. Server-Sent Events (SSE) are supported, with occasional comment payloads that can be ignored.[](https://openrouter.ai/docs/api-reference/overview)
- **Provider Routing**: Use the `provider` object to prioritize providers (e.g., `order: ["deepinfra/turbo"]`) or sort by `price` or `throughput`. Variants like `:floor` (sort by price) or `:nitro` (sort by throughput) can be appended to model IDs.[](https://openrouter.ai/docs/api-reference/overview)
- **Rate Limits**: Free models (ending in `:free`) allow up to 20 requests per minute and 50–1000 requests per day, depending on credit purchases. Check limits via `https://openrouter.ai/api/v1/key`.[](https://openrouter.ai/docs/api-reference/overview)
- **BYOK (Bring Your Own Key)**: Use your own provider API keys with a 5% fee deducted from OpenRouter credits.[](https://gist.github.com/rbiswasfc/f38ea50e1fa12058645e6077101d55bb)
- **Supported Models**: Access models like `meta-llama/llama-3-70b-instruct`, `openai/gpt-4-32k`, and more. Check the [models browser](https://openrouter.ai/models) for a full list.[](https://docs.typingmind.com/chat-models-settings/use-openrouter-models)
- **Audio Limitations**: Dedicated TTS/STT endpoints are not natively supported. Developers must rely on multimodal models or third-party integrations, which may introduce variability in performance or availability.[](https://github.com/Aider-AI/aider/issues/1972)
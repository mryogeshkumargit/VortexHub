# Together AI API Endpoints

Together AI offers a unified API for a wide range of models, including large language models (LLMs), image models, and audio models. The API is designed to be highly accessible and is compatible with the OpenAI API format.

## General API Details
- **Base URL**: `https://api.together.xyz/v1/`
- **Authentication**: Use your Together AI API key in the Authorization header as a Bearer token: `Authorization: Bearer YOUR_API_KEY`.

## 1. Large Language Models (LLMs)
You can interact with hundreds of open-source LLMs using the chat completions endpoint.

- **Endpoint**: `https://api.together.xyz/v1/chat/completions`
- **Method**: POST
- **Request Body (JSON)**:
  - `model`: The model string.
  - `messages`: An array of message objects, with `role` (user, system, or assistant) and `content` fields.
  - (Optional) `temperature`, `max_tokens`, `top_p`, etc.

### Example using curl:
```bash
curl -X POST "https://api.together.xyz/v1/chat/completions" \
  -H "Authorization: Bearer $TOGETHER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3-70b-chat-hf",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "What is the capital of Japan?"}
    ],
    "temperature": 0.7,
    "max_tokens": 50
  }'
```

## 2. Image Models
Together AI's image generation models allow you to create images from a text prompt.

- **Endpoint**: `https://api.together.xyz/v1/images/generations`
- **Method**: POST
- **Request Body (JSON)**:
  - `model`: The model string for image generation.
  - `prompt`: The text prompt describing the image.
  - (Optional) `width`, `height`, etc.

### Example using curl:
```bash
curl -X POST "https://api.together.xyz/v1/images/generations" \
  -H "Authorization: Bearer $TOGETHER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "A digital art painting of a cat wearing a space suit in a futuristic city, vivid colors.",
    "model": "black-forest-labs/FLUX.1-schnell",
    "width": 1024,
    "height": 1024
  }'
```

## 3. Audio Models
The platform supports both speech-to-text and text-to-speech functionalities.

### Speech-to-Text (Transcription)
- **Endpoint**: `https://api.together.xyz/v1/audio/transcriptions`
- **Method**: POST
- **Request Body (Multipart Form Data)**:
  - `model`: The transcription model (e.g., "openai/whisper-large-v3").
  - `file`: The audio file to be transcribed.

#### Example using curl:
```bash
curl -X POST "https://api.together.xyz/v1/audio/transcriptions" \
  -H "Authorization: Bearer $TOGETHER_API_KEY" \
  -F "model=openai/whisper-large-v3" \
  -F "file=@/path/to/your/audio.mp3"
```

### Text-to-Speech (Audio Generation)
To use the text-to-speech API, you need to know the available voices for the model you are using. You specify the voice in the `voice` parameter of the API request.

- **Endpoint**: `https://api.together.xyz/v1/audio/speech`
- **Method**: POST
- **Request Body (JSON)**:
  - `model`: The text-to-speech model.
  - `input`: The text to be converted to speech.
  - `voice`: The name of the voice to use.
  - (Optional) `response_format`, `response_encoding`, etc.

#### Example using curl:
```bash
curl --location 'https://api.together.ai/v1/audio/speech' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer $TOGETHER_API_KEY' \
  --data '{
    "model": "cartesia/sonic-2",
    "input": "Hello, this is a test of the text to speech model.",
    "voice": "laidback woman"
  }' \
  --output speech_output.mp3
```

## Recommended Models for Specific Use Cases on Together AI

### 5 Image Generation Models for Realistic Images
These are the exact model strings you can use in your API requests to generate photorealistic images:
- `black-forest-labs/FLUX.1-schnell-Free`
- `black-forest-labs/FLUX.1-schnell`
- `black-forest-labs/FLUX.1.1-pro`
- `stabilityai/stable-diffusion-xl-base-1.0`
- `stabilityai/stable-diffusion-3.5-large`

### 5 Text-to-Speech Realistic Voices for Roleplay
For the `cartesia/sonic-2` model, these are some of the available voices that are well-suited for realistic and expressive roleplay. Use the exact string in the `voice` parameter:
- `laidback woman`
- `helpful woman`
- `british reading lady`
- `nonfiction man`
- `indian lady`

### 5 Best Serverless Models for Roleplay (Uncensored)
These are the exact API model strings for models highly regarded by the community for uncensored and creative roleplay:
- `Gryphe/MythoMax-L2-13b`
- `NousResearch/Nous-Hermes-2-Mixtral-8x7B-DPO`
- `teknium/OpenHermes-2.5-Mistral-7B`
- `Austism/chronos-hermes-13b`
- `meta-llama/Llama-3-70b-chat-hf`
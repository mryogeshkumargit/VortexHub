
# SamuraiAPI — Developer Documentation (Draft)
*Version:* 0.9 (proposed)  
*Last updated:* 2025-08-24 14:00 IST  
*Base URL (proposed):* `https://samuraiapi.in`

> **Important note**: This documentation is a **complete, production-style specification** drafted to cover the feature areas you requested (LLM, chat, image generation, image edit, TTS, STT, audio/speech, and music). It follows widely adopted conventions (OpenAI-compatible where appropriate) so you can start building immediately. Replace placeholder details (marked as _proposed_) with your actual implementation specifics as they become available.

---

## 1. Overview

SamuraiAPI is a unified Generative AI platform that exposes REST endpoints for:
- **LLMs**: text completions, chat completions, embeddings, and moderation
- **Vision & Image**: text-to-image, image edits, and variations
- **Audio**: text-to-speech (TTS), speech-to-text (STT), translation, and generic audio generation
- **Music**: text-to-music (proposed endpoint & schema)
- **Jobs**: long-running task polling & webhooks
- **Files**: managed uploads for multi-part operations

> This guide also includes **streaming**, **webhooks**, **rate limits**, **errors**, and runnable examples in **cURL**, **Node.js**, and **Python**.

---

## 2. Authentication

Use **API keys** via the `Authorization` header.

```
Authorization: Bearer <YOUR_API_KEY>
```

- All requests must be made over **HTTPS**.
- Keys are scoped by account and environment (e.g., test/production).
- Rotate keys periodically; revoke compromised keys immediately.

---

## 3. Conventions

- **Base URL:** `https://samuraiapi.in`
- **Versioning:** Prefix with `/v1/` (e.g., `/v1/chat/completions`). Backward-incompatible changes increment the major version.
- **Content Types:** `application/json` for JSON bodies, `multipart/form-data` for file uploads.
- **Timestamps:** ISO-8601 in UTC unless otherwise specified.
- **IDs:** Short, URL-safe strings (e.g., `job_abc123`, `file_...`).

---

## 4. Models

### List Models
```
GET /v1/models
```
**Response**
```json
{
  "object": "list",
  "data": [
    {
      "id": "samurai-chat-pro",
      "object": "model",
      "category": "chat",
      "context_window": 128000,
      "input_cost_per_1k_tokens": 0.2,
      "output_cost_per_1k_tokens": 0.6,
      "available": true
    },
    {
      "id": "samurai-image-v1",
      "object": "model",
      "category": "image",
      "sizes": ["512x512", "768x768", "1024x1024"],
      "available": true
    }
  ]
}
```

---

## 5. Text & Chat LLM

### 5.1 Chat Completions (OpenAI-compatible)

```
POST /v1/chat/completions
Content-Type: application/json
```
**Request**
```json
{
  "model": "samurai-chat-pro",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Write a haiku about rivers"}
  ],
  "temperature": 0.7,
  "top_p": 0.95,
  "max_tokens": 512,
  "stream": false,
  "tools": [{"type":"function", "function":{"name":"get_weather","description":"...","parameters":{"type":"object","properties":{"city":{"type":"string"}},"required":["city"]}}}]
}
```
**Response**
```json
{
  "id": "chatcmpl_abc123",
  "object": "chat.completion",
  "created": 1735072000,
  "model": "samurai-chat-pro",
  "choices": [{"index":0,"message":{"role":"assistant","content":"Quiet winding streams\nwhisper over ancient stones\ncarving time in blue"},"finish_reason":"stop"}],
  "usage": {"prompt_tokens": 18, "completion_tokens": 20, "total_tokens": 38}
}
```

#### Streaming (SSE)
```
POST /v1/chat/completions   (with "stream": true)
Accept: text/event-stream
```
Server streams `data: {delta}` chunks ending with `data: [DONE]`.

### 5.2 Text Completions
```
POST /v1/completions
```
OpenAI-style `prompt`, `max_tokens`, `temperature`, etc.

### 5.3 Embeddings
```
POST /v1/embeddings
```
**Request**
```json
{ "model": "samurai-embed-v1", "input": ["hello world", "नमस्ते दुनिया"] }
```
**Response**
```json
{
  "object": "list",
  "data": [{"object":"embedding","index":0,"embedding":[0.01,-0.02,0.03]}, {"object":"embedding","index":1,"embedding":[0.03,0.11,-0.07]}],
  "model": "samurai-embed-v1",
  "usage": {"prompt_tokens": 8, "total_tokens": 8}
}
```

### 5.4 Moderations (optional)
```
POST /v1/moderations
```

---

## 6. Images (Generation, Edits, Variations)

### 6.1 Text-to-Image
```
POST /v1/images/generations
```
**Request**
```json
{
  "model": "samurai-image-v1",
  "prompt": "A serene temple under cherry blossoms, cinematic lighting",
  "n": 1,
  "size": "1024x1024",
  "response_format": "url"
}
```
**Response**
```json
{
  "created": 1735072000,
  "data": [{"url": "https://samuraiapi.in/files/img_abc123.png"}]
}
```

### 6.2 Image Edits
```
POST /v1/images/edits
Content-Type: multipart/form-data
```
**Form fields**
- `image` (required): original image file
- `mask` (optional): PNG with transparent areas to modify
- `prompt` (required)
- `size`, `n`, `model`, `background` (optional)

**Response**: same shape as generations.

### 6.3 Variations
```
POST /v1/images/variations
```
Upload `image` and set `n`, `size`.

---

## 7. Audio & Speech

### 7.1 Text-to-Speech (TTS)
```
POST /v1/audio/speech
```
**Request**
```json
{
  "model": "samurai-tts-v1",
  "input": "नमस्ते, स्वागत है SamuraiAPI में!",
  "voice": "arun",
  "format": "mp3",
  "sample_rate": 22050,
  "speed": 1.0,
  "emotion": "neutral"
}
```
**Response**
```json
{ "object":"audio","id":"aud_abc123","url":"https://samuraiapi.in/files/aud_abc123.mp3","duration_sec":3.5 }
```
> To stream TTS, set `"stream": true` and consume via SSE or chunked transfer.

### 7.2 Speech-to-Text (STT) — Transcribe
```
POST /v1/audio/transcriptions
Content-Type: multipart/form-data
```
**Form fields**
- `file` (required): audio/video file
- `model`: `samurai-stt-v1`
- `language` (optional, e.g., `en`, `hi`)
- `prompt` (optional, biasing text)

**Response**
```json
{ "text": "hello and welcome to SamuraiAPI" }
```

### 7.3 Speech Translation
```
POST /v1/audio/translations
```
Same as transcriptions but target language specified (`target_language`).

### 7.4 Generic Audio Generation (Sound FX / Music Stems)
```
POST /v1/audio/generations
```
**Request**
```json
{
  "model": "samurai-audio-gen-v1",
  "prompt": "ambient rain with soft chimes",
  "duration": 12,
  "format": "wav"
}
```
**Response**
```json
{ "id":"audgen_abc123", "url":"https://samuraiapi.in/files/audgen_abc123.wav" }
```

---

## 8. Music Generation (Proposed)

```
POST /v1/music/generations
```
**Request**
```json
{
  "model": "samurai-music-v1",
  "prompt": "bollywood-style upbeat track with tabla and synth bass",
  "duration": 30,
  "bpm": 120,
  "key": "C#m",
  "stems": false,
  "format": "mp3"
}
```
**Response**
```json
{
  "id": "music_abc123",
  "object": "music",
  "url": "https://samuraiapi.in/files/music_abc123.mp3",
  "duration_sec": 30,
  "metadata": {"bpm": 120, "key": "C#m"}
}
```

---

## 9. Files API

### Upload a File
```
POST /v1/files
Content-Type: multipart/form-data
```
**Fields:** `file`, `purpose` (e.g., `fine-tune`, `image-edit`, `tts-lexicon`)

**Response**
```json
{ "id":"file_abc123","object":"file","filename":"mask.png","bytes":12345,"created_at":1735072000,"purpose":"image-edit","status":"uploaded" }
```

### Retrieve / List / Delete
```
GET    /v1/files
GET    /v1/files/{file_id}
DELETE /v1/files/{file_id}
```

---

## 10. Jobs & Webhooks

Long-running operations return `202 Accepted` with a job resource.

**Job object**
```json
{
  "id":"job_abc123",
  "object":"job",
  "type":"image.edit",
  "status":"queued",
  "created_at":1735072000,
  "result": null,
  "error": null
}
```

### Poll Job
```
GET /v1/jobs/{job_id}
```

### Webhooks
- Configure at **Dashboard → Webhooks** (proposed).
- Events:
  - `job.succeeded`
  - `job.failed`
  - `rate.limit.warning`
  - `billing.usage.updated`

**Webhook delivery**
- POST with `application/json` to your endpoint
- Signed with `X-Samurai-Signature` (HMAC-SHA256 using your webhook secret)
- Retry with exponential backoff on 4xx/5xx (except 410)

**Verification (pseudo)**
```python
import hmac, hashlib

def verify(signature, payload, secret):
    expected = hmac.new(secret.encode(), payload, hashlib.sha256).hexdigest()
    return hmac.compare_digest(signature, expected)
```

---

## 11. Streaming

Two streaming modes are supported:

1) **Event Stream (SSE)** for chat & TTS:
```
Accept: text/event-stream
```
Server emits `event: message` with `data: ...` JSON deltas.

2) **Chunked audio** for TTS:
- `Content-Type: audio/mpeg`
- Each chunk is a playable fragment; client appends to a source buffer.

---

## 12. Error Handling

Errors use consistent JSON schema and HTTP status codes.

```json
{
  "error": {
    "type": "invalid_request_error",
    "code": "bad_argument",
    "message": "size must be one of 512x512, 768x768, 1024x1024",
    "param": "size",
    "request_id": "req_abc123"
  }
}
```

**Common status codes**
- `400` Invalid request
- `401` Authentication failed / missing
- `403` Insufficient scope
- `404` Resource not found
- `409` Conflict
- `422` Unprocessable entity
- `429` Rate limited
- `500/502/503` Server or upstream error

---

## 13. Rate Limits (Proposed)

- Rate window: 60 seconds
- **Default**: 600 requests / minute, burst 1200
- **Headers**:
  - `X-RateLimit-Limit`
  - `X-RateLimit-Remaining`
  - `X-RateLimit-Reset` (epoch seconds)

Contact support to increase limits.

---

## 14. Security & Data Handling

- Requests are encrypted in transit (TLS 1.2+).  
- **Data retention (proposed):**
  - Prompts and outputs retained up to 30 days for abuse detection unless `X-No-Store: true` is set.
  - You may opt out at the account level (Dashboard → Data Controls).
- **PII:** Do not send regulated or sensitive PII unless you have a DPA in place.
- **Regionality:** Set `X-Region: in` to pin processing to India (if supported).

---

## 15. SDK & Examples

### 15.1 cURL — Chat
```bash
curl https://samuraiapi.in/v1/chat/completions   -H "Authorization: Bearer $SAMURAI_API_KEY"   -H "Content-Type: application/json"   -d '{
    "model": "samurai-chat-pro",
    "messages": [
      {"role":"user","content":"Summarize RBI monetary policy highlights"}
    ]
  }'
```

### 15.2 Node.js (fetch) — Image Generation
```javascript
const res = await fetch("https://samuraiapi.in/v1/images/generations", {
  method: "POST",
  headers: {
    "Authorization": `Bearer ${process.env.SAMURAI_API_KEY}`,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "samurai-image-v1",
    prompt: "A watercolor of Varanasi ghats at sunrise",
    size: "1024x1024"
  })
});
const data = await res.json();
console.log(data);
```

### 15.3 Python (requests) — STT
```python
import requests

files = {"file": open("meeting.mp3","rb")}
data = {"model": "samurai-stt-v1", "language":"en"}
r = requests.post("https://samuraiapi.in/v1/audio/transcriptions",
                  headers={"Authorization": f"Bearer {SAMURAI_API_KEY}"},
                  files=files, data=data)
print(r.json())
```

### 15.4 Webhooks — Express (Node.js)
```javascript
import crypto from "crypto";
import express from "express";

const app = express();
app.use(express.raw({ type: "application/json" }));

app.post("/webhooks/samurai", (req, res) => {
  const sig = req.header("X-Samurai-Signature") || "";
  const expected = crypto.createHmac("sha256", process.env.SAMURAI_WEBHOOK_SECRET)
                         .update(req.body)
                         .digest("hex");
  if (!crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) {
    return res.status(400).send("Invalid signature");
  }
  const event = JSON.parse(req.body.toString());
  // handle event.type
  res.json({ received: true });
});

app.listen(3000);
```

---

## 16. OpenAI Compatibility Notes

Where feasible, SamuraiAPI mirrors OpenAI route shapes and response envelopes so existing clients can switch with minimal changes:
- `/v1/models`, `/v1/chat/completions`, `/v1/completions`, `/v1/embeddings`  
- `/v1/images/generations|edits|variations`  
- `/v1/audio/speech|transcriptions|translations`

> If you already use an OpenAI SDK, you can point it to `https://samuraiapi.in` and set `apiKey`. Some SDKs allow `baseURL` overrides.

---

## 17. Pagination & Filtering

List endpoints support:
```
?limit=50&after=cursor_abc123&before=cursor_xyz
```
Sort via `sort=created_at&order=desc`. Filters vary by resource (e.g., `model=`, `status=`).

---

## 18. Idempotency

For mutating endpoints, support:
```
Idempotency-Key: <unique-id>
```
Reuse the same key to safely retry without duplicating work.

---

## 19. SLA & Support (Proposed)

- **Uptime target:** 99.9% monthly
- **Regions:** in, ap-south-1 (proposed)
- **Support:** `support@samuraiapi.in` (proposed), priority for enterprise tiers.

---

## 20. Changelog (Sample)

- **0.9**: Initial draft with LLM, Images, Audio, Music, Files, Jobs, Webhooks.
- **0.9.1 (planned)**: Add fine-tuning endpoints and batch inference.

---

## 21. Glossary

- **TTS**: Text to Speech  
- **STT**: Speech to Text  
- **SSE**: Server Sent Events  
- **Job**: Long-running asynchronous task
- **Embedding**: Numeric vector for semantic similarity

---

## 22. HTTP Reference (Index)

- `GET  /v1/models`
- `POST /v1/chat/completions`
- `POST /v1/completions`
- `POST /v1/embeddings`
- `POST /v1/moderations`
- `POST /v1/images/generations`
- `POST /v1/images/edits` *(multipart)*
- `POST /v1/images/variations` *(multipart)*
- `POST /v1/audio/speech`
- `POST /v1/audio/transcriptions` *(multipart)*
- `POST /v1/audio/translations` *(multipart)*
- `POST /v1/audio/generations`
- `POST /v1/music/generations`
- `POST /v1/files` *(multipart)*
- `GET  /v1/files`
- `GET  /v1/files/{file_id}`
- `DELETE /v1/files/{file_id}`
- `GET  /v1/jobs/{job_id}`

---

### Attribution
This document is a **proposed** API contract. Replace placeholders as your platform’s implementation finalizes.

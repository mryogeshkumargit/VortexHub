#!/bin/bash

echo "Testing API endpoints with provided keys..."

TOGETHER_KEY="31980f30c8041ce661665b782482f885d89a2296abf856111bafee8507c64d5c"
OPENROUTER_KEY="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"

echo ""
echo "=== Testing Together AI ==="
curl -X POST "https://api.together.xyz/v1/chat/completions" \
  -H "Authorization: Bearer $TOGETHER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3.2-3B-Instruct-Turbo",
    "messages": [{"role": "user", "content": "Hello, respond with just: Together AI working!"}],
    "max_tokens": 20
  }' \
  --connect-timeout 10 \
  --max-time 30

echo ""
echo ""
echo "=== Testing OpenRouter ==="
curl -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer $OPENROUTER_KEY" \
  -H "Content-Type: application/json" \
  -H "HTTP-Referer: https://vortexai.app" \
  -H "X-Title: VortexAI Test" \
  -d '{
    "model": "openai/gpt-4o-mini",
    "messages": [{"role": "user", "content": "Hello, respond with just: OpenRouter working!"}],
    "max_tokens": 20
  }' \
  --connect-timeout 10 \
  --max-time 30

echo ""
echo "Test completed!"
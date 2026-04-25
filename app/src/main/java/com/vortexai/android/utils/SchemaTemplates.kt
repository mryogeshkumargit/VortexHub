package com.vortexai.android.utils

object SchemaTemplates {
    
    // Text Generation Templates
    val OPENAI_COMPATIBLE_TEXT = Template(
        name = "OpenAI Compatible",
        requestSchema = """{
            "headers": {
                "Authorization": "Bearer {apiKey}",
                "Content-Type": "application/json"
            },
            "body": {
                "model": "{modelId}",
                "messages": "{messages}",
                "temperature": "{temperature}",
                "max_tokens": "{maxTokens}",
                "stream": "{stream}"
            },
            "parameterMapping": {
                "messages": "array",
                "temperature": "float",
                "maxTokens": "integer",
                "stream": "boolean"
            }
        }""",
        responseSchema = """{
            "dataPath": "choices[0].message.content",
            "streamingPath": "choices[0].delta.content",
            "errorPath": "error.message"
        }""",
        endpointPath = "/v1/chat/completions"
    )
    
    val ANTHROPIC_TEXT = Template(
        name = "Anthropic Claude",
        requestSchema = """{
            "headers": {
                "x-api-key": "{apiKey}",
                "anthropic-version": "2023-06-01",
                "Content-Type": "application/json"
            },
            "body": {
                "model": "{modelId}",
                "messages": "{messages}",
                "max_tokens": "{maxTokens}",
                "temperature": "{temperature}"
            },
            "parameterMapping": {
                "messages": "array",
                "maxTokens": "integer",
                "temperature": "float"
            }
        }""",
        responseSchema = """{
            "dataPath": "content[0].text",
            "streamingPath": "delta.text",
            "errorPath": "error.message"
        }""",
        endpointPath = "/v1/messages"
    )
    
    // Image Generation Templates
    val STABILITY_AI_IMAGE = Template(
        name = "Stability AI",
        requestSchema = """{
            "headers": {
                "Authorization": "Bearer {apiKey}",
                "Content-Type": "application/json"
            },
            "body": {
                "text_prompts": "{prompts}",
                "cfg_scale": "{guidanceScale}",
                "steps": "{steps}",
                "width": "{width}",
                "height": "{height}"
            },
            "parameterMapping": {
                "prompts": "array",
                "guidanceScale": "float",
                "steps": "integer",
                "width": "integer",
                "height": "integer"
            }
        }""",
        responseSchema = """{
            "imageUrlPath": "artifacts[0].base64",
            "errorPath": "message"
        }""",
        endpointPath = "/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image"
    )
    
    val REPLICATE_IMAGE = Template(
        name = "Replicate",
        requestSchema = """{
            "headers": {
                "Authorization": "Token {apiKey}",
                "Content-Type": "application/json"
            },
            "body": {
                "version": "{modelId}",
                "input": {
                    "prompt": "{prompt}",
                    "width": "{width}",
                    "height": "{height}",
                    "num_outputs": "1"
                }
            },
            "parameterMapping": {
                "prompt": "string",
                "width": "integer",
                "height": "integer"
            }
        }""",
        responseSchema = """{
            "imageUrlPath": "output[0]",
            "statusPath": "status",
            "errorPath": "error"
        }""",
        endpointPath = "/v1/predictions"
    )
    
    // Image Editing Templates
    val STABILITY_AI_EDIT = Template(
        name = "Stability AI Inpainting",
        requestSchema = """{
            "headers": {
                "Authorization": "Bearer {apiKey}",
                "Content-Type": "multipart/form-data"
            },
            "body": {
                "init_image": "{initImage}",
                "text_prompts": "{prompts}",
                "cfg_scale": "{guidanceScale}",
                "steps": "{steps}"
            },
            "parameterMapping": {
                "initImage": "file",
                "prompts": "array",
                "guidanceScale": "float",
                "steps": "integer"
            }
        }""",
        responseSchema = """{
            "imageUrlPath": "artifacts[0].base64",
            "errorPath": "message"
        }""",
        endpointPath = "/v1/generation/stable-diffusion-xl-1024-v1-0/image-to-image"
    )
    
    val REPLICATE_EDIT = Template(
        name = "Replicate Image Edit",
        requestSchema = """{
            "headers": {
                "Authorization": "Token {apiKey}",
                "Content-Type": "application/json"
            },
            "body": {
                "version": "{modelId}",
                "input": {
                    "image": "{image}",
                    "prompt": "{prompt}",
                    "go_fast": "true",
                    "output_format": "webp",
                    "output_quality": "80"
                }
            },
            "parameterMapping": {
                "image": "string",
                "prompt": "string"
            }
        }""",
        responseSchema = """{
            "imageUrlPath": "output[0]",
            "statusPath": "status",
            "errorPath": "error"
        }""",
        endpointPath = "/v1/predictions"
    )
    
    data class Template(
        val name: String,
        val requestSchema: String,
        val responseSchema: String,
        val endpointPath: String
    )
    
    fun getTextGenerationTemplates() = listOf(
        OPENAI_COMPATIBLE_TEXT,
        ANTHROPIC_TEXT
    )
    
    fun getImageGenerationTemplates() = listOf(
        STABILITY_AI_IMAGE,
        REPLICATE_IMAGE
    )
    
    fun getImageEditingTemplates() = listOf(
        STABILITY_AI_EDIT,
        REPLICATE_EDIT
    )
}

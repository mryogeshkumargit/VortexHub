package com.vortexai.android.ui.screens.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import com.vortexai.android.ui.screens.settings.components.*

@Composable
fun LLMConfigurationTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigateToCustomApi: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(title = "LLM Provider") {
                Button(
                    onClick = onNavigateToCustomApi,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Api, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Configure Custom Text Generation APIs")
                }
                
                SettingsDropdownItem(
                    title = "LLM Provider",
                    description = "Choose your AI language model provider",
                    selectedValue = uiState.llmProvider,
                    options = listOf("Together AI", "Gemini API", "Open Router", "Hugging Face", "Grok", "Ollama", "Kobold AI", "LMStudio", "ModelsLab", "Custom API"),
                    onValueChange = viewModel::updateLlmProvider
                )
                
                when (uiState.llmProvider) {
                    "Together AI" -> TogetherAIConfig(uiState, viewModel)
                    "Gemini API" -> GeminiAPIConfig(uiState, viewModel)
                    "Open Router" -> OpenRouterConfig(uiState, viewModel)
                    "Hugging Face" -> HuggingFaceConfig(uiState, viewModel)
                    "Grok" -> GrokConfig(uiState, viewModel)
                    "Ollama" -> OllamaConfig(uiState, viewModel)
                    "Kobold AI" -> KoboldAIConfig(uiState, viewModel)
                    "LMStudio" -> LMStudioConfig(uiState, viewModel)
                    "ModelsLab" -> ModelsLabLLMConfig(uiState, viewModel)
                    "Custom API" -> CustomAPIConfig(uiState, viewModel)
                }
            }
        }
        
        item {
            SettingsSection(title = "Model Configuration") {
                val isCustomApi = uiState.llmProvider == "Custom API"
                
                if (!isCustomApi) {
                when (uiState.llmProvider) {
                    "ModelsLab" -> {
                        // For ModelsLab, show hardcoded + manually added models
                        val hardcodedModels = listOf(
                            "Yarn-Mistral-7b-128k",
                            "MistralLite", 
                            "OpenHermes-2.5-Mistral-7B",
                            "dolphin-2.2.1-mistral-7b",
                            "deepseek-ai-DeepSeek-R1-Distill-Llama-70B",
                            "Qwen-Qwen3-235B-A22B-fp8-tput",
                            "meta-llama-Llama-3.3-70B-Instruct-Turbo-Free",
                            "deepseek-ai-DeepSeek-R1-Distill-Llama-70B-free"
                        )
                        val allModelsLabModels = hardcodedModels + uiState.manuallyAddedLlmModels
                        
                        if (allModelsLabModels.isNotEmpty()) {
                            SettingsDropdownItem(
                                title = "Model",
                                description = "Select from hardcoded and manually added models",
                                selectedValue = uiState.llmModel,
                                options = allModelsLabModels,
                                onValueChange = viewModel::updateLlmModel
                            )
                        } else {
                            SettingsTextFieldItem(
                                title = "Model Name",
                                value = uiState.llmModel,
                                onValueChange = viewModel::updateLlmModel,
                                placeholder = "e.g., Yarn-Mistral-7b-128k"
                            )
                        }
                    }
                    else -> {
                        // For other providers, use the existing logic
                        if (uiState.availableModels.isNotEmpty()) {
                            SettingsDropdownItem(
                                title = "Model",
                                description = "Select from available models",
                                selectedValue = uiState.llmModel,
                                options = uiState.availableModels.map { "${it.name} (${it.id})" },
                                onValueChange = { selectedModel ->
                                    val modelId = uiState.availableModels.find { model ->
                                        "${model.name} (${model.id})" == selectedModel
                                    }?.id ?: selectedModel
                                    viewModel.updateLlmModel(modelId)
                                }
                            )
                        } else {
                            SettingsTextFieldItem(
                                title = "Model Name",
                                value = uiState.llmModel,
                                onValueChange = viewModel::updateLlmModel,
                                placeholder = "e.g., meta-llama/Llama-2-7b-chat-hf"
                            )
                            if (uiState.availableModels.isEmpty() && !uiState.isLoadingModels && uiState.llmProvider != "ModelsLab") {
                                Text(
                                    text = "Enter API key and click 'Fetch Available Models' to see model options",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                } else {
                    Text(
                        text = "Model configuration is managed in the Custom API provider settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isCustomApi) {
                SettingsSliderItem(
                    title = "Temperature",
                    description = "Controls randomness (0.0 = focused, 1.0 = creative)",
                    value = uiState.responseTemperature,
                    onValueChange = viewModel::updateResponseTemperature,
                    valueRange = 0.0f..1.0f,
                    steps = 19,
                    displayValue = "%.1f".format(uiState.responseTemperature)
                )
                
                SettingsSliderItem(
                    title = "Max Tokens",
                    description = "Maximum length of generated responses",
                    value = uiState.maxTokens.toFloat(),
                    onValueChange = { viewModel.updateMaxTokens(it.roundToInt()) },
                    valueRange = 512f..8192f,
                    steps = 15,
                    displayValue = uiState.maxTokens.toString()
                )
                
                SettingsSliderItem(
                    title = "Top P",
                    description = "Nucleus sampling parameter",
                    value = uiState.topP,
                    onValueChange = viewModel::updateTopP,
                    valueRange = 0.1f..1.0f,
                    steps = 17,
                    displayValue = "%.1f".format(uiState.topP)
                )
                
                SettingsSliderItem(
                    title = "Frequency Penalty",
                    description = "Reduce repetition in responses",
                    value = uiState.frequencyPenalty,
                    onValueChange = viewModel::updateFrequencyPenalty,
                    valueRange = 0.0f..2.0f,
                    steps = 19,
                    displayValue = "%.1f".format(uiState.frequencyPenalty)
                )
                
                // Test Connection Button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.testLLMConnection() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.llmModel.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }
                
                // Show connection status
                if (uiState.endpointError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.endpointError.startsWith("✅")) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Text(
                            text = uiState.endpointError,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.endpointError.startsWith("✅")) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
                }
            }
        }
        
        item {
            SettingsSection(title = "Response Formatting") {
                SettingsDropdownItem(
                    title = "Response Length Style",
                    description = "Choose how long AI responses should be",
                    selectedValue = uiState.responseLengthStyle,
                    options = listOf("short", "natural", "long", "unlimited", "custom"),
                    onValueChange = viewModel::updateResponseLengthStyle
                )
                
                SettingsSwitchItem(
                    title = "Enable Roleplay Formatting",
                    description = "Add beautiful formatting with *actions* and \"dialogue\"",
                    checked = uiState.enableResponseFormatting,
                    onCheckedChange = viewModel::updateEnableResponseFormatting
                )
                
                if (uiState.responseLengthStyle == "custom") {
                    SettingsSliderItem(
                        title = "Custom Max Tokens",
                        description = "Set custom token limit for responses",
                        value = uiState.customMaxTokens.toFloat(),
                        onValueChange = { viewModel.updateCustomMaxTokens(it.roundToInt()) },
                        valueRange = 100f..8192f,
                        steps = 80,
                        displayValue = "${uiState.customMaxTokens} tokens"
                    )
                }
            }
        }
        
        if (uiState.llmProvider == "ModelsLab") {
            item {
                ManualModelsLabLLMModels(uiState, viewModel)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveLLMSettings() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Settings")
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearModelCache() },
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text("Clear Cache")
                    }
                }
                

            }
        }
    }
}

@Composable
private fun TogetherAIConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Together AI API Key",
        value = uiState.togetherAiApiKey,
        onValueChange = viewModel::updateTogetherAiApiKey,
        placeholder = "Enter your Together AI API key"
    )
    if (uiState.togetherAiApiKey.isNotBlank()) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    viewModel.clearModelCache() 
                    viewModel.fetchModels() 
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoadingModels
            ) {
                if (uiState.isLoadingModels) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Fetch Models")
            }
            OutlinedButton(
                onClick = { viewModel.clearModelCache() },
                modifier = Modifier.weight(0.5f),
                enabled = !uiState.isLoadingModels
            ) {
                Text("Clear Cache")
            }
        }
    }
}

@Composable
private fun GeminiAPIConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Gemini API Key",
        value = uiState.geminiApiKey,
        onValueChange = viewModel::updateGeminiApiKey,
        placeholder = "Enter your Google Gemini API key"
    )
    if (uiState.geminiApiKey.isNotBlank()) {
        Button(
            onClick = { viewModel.fetchModels() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isLoadingModels
        ) {
            if (uiState.isLoadingModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Fetch Available Models")
        }
    }
}

@Composable
private fun OpenRouterConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "OpenRouter API Key",
        value = uiState.openRouterApiKey,
        onValueChange = viewModel::updateOpenRouterApiKey,
        placeholder = "Enter your OpenRouter API key"
    )
    if (uiState.openRouterApiKey.isNotBlank()) {
        Button(
            onClick = { viewModel.fetchModels() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isLoadingModels
        ) {
            if (uiState.isLoadingModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Fetch Available Models")
        }
    }
}

@Composable
private fun HuggingFaceConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Hugging Face API Key",
        value = uiState.huggingFaceApiKey,
        onValueChange = viewModel::updateHuggingFaceApiKey,
        placeholder = "Enter your Hugging Face API key"
    )
    if (uiState.huggingFaceApiKey.isNotBlank()) {
        Button(
            onClick = { viewModel.fetchModels() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isLoadingModels
        ) {
            if (uiState.isLoadingModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Fetch Available Models")
        }
    }
}

@Composable
private fun GrokConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Grok API Key",
        value = uiState.grokApiKey,
        onValueChange = viewModel::updateGrokApiKey,
        placeholder = "Enter your Grok API key"
    )
    SettingsTextFieldItem(
        title = "Grok Endpoint",
        value = uiState.grokEndpoint,
        onValueChange = viewModel::updateGrokEndpoint,
        placeholder = "https://api.x.ai/v1"
    )
}

@Composable
private fun OllamaConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "Ollama runs locally without API keys. For network access, use your laptop's IP address.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    SettingsTextFieldItem(
        title = "Ollama Endpoint",
        value = uiState.ollamaEndpoint,
        onValueChange = viewModel::updateOllamaEndpoint,
        placeholder = "http://192.168.1.100:11435"
    )
    if (uiState.endpointError.isNotBlank()) {
        Text(
            text = uiState.endpointError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    if (uiState.ollamaEndpoint.isNotBlank()) {
        Button(
            onClick = { viewModel.fetchModels() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isLoadingModels
        ) {
            if (uiState.isLoadingModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Fetch Available Models")
        }
    }
}

@Composable
private fun KoboldAIConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "Kobold AI runs locally without API keys. For network access, use your laptop's IP address.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    SettingsTextFieldItem(
        title = "Kobold AI Endpoint",
        value = uiState.koboldEndpoint,
        onValueChange = viewModel::updateKoboldEndpoint,
        placeholder = "http://192.168.1.100:5000"
    )
    if (uiState.endpointError.isNotBlank()) {
        Text(
            text = uiState.endpointError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    if (uiState.koboldEndpoint.isNotBlank()) {
        Button(
            onClick = { viewModel.fetchModels() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isLoadingModels
        ) {
            if (uiState.isLoadingModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Fetch Available Models")
        }
    }
}

@Composable
private fun LMStudioConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "LMStudio runs locally without API keys. For network access, use your laptop's IP address.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    SettingsTextFieldItem(
        title = "LMStudio Endpoint",
        value = uiState.lmStudioEndpoint,
        onValueChange = viewModel::updateLmStudioEndpoint,
        placeholder = "http://192.168.1.100:5000"
    )
    if (uiState.endpointError.isNotBlank()) {
        Text(
            text = uiState.endpointError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    if (uiState.lmStudioEndpoint.isNotBlank()) {
        Button(
            onClick = { viewModel.fetchModels() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isLoadingModels
        ) {
            if (uiState.isLoadingModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Fetch Available Models")
        }
    }
}

@Composable
private fun ModelsLabLLMConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "ModelsLab provides uncensored AI chat and image generation. Get your API key from modelslab.com",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    SettingsPasswordFieldItem(
        title = "ModelsLab API Key",
        value = uiState.modelsLabApiKey,
        onValueChange = viewModel::updateModelsLabApiKey,
        placeholder = "Enter your ModelsLab API key"
    )
}



@Composable
private fun ManualModelsLabLLMModels(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsSection(title = "Add Custom Models") {
        Text(
            text = "Add custom LLM models for ModelsLab API. These will be available in addition to the hardcoded models.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        var newModelId by remember { mutableStateOf("") }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newModelId,
                onValueChange = { newModelId = it },
                placeholder = { Text("Enter model ID (e.g., custom-model-1)") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newModelId.isNotBlank()) {
                        viewModel.addManualLlmModel(newModelId.trim())
                        newModelId = ""
                    }
                },
                enabled = newModelId.isNotBlank()
            ) {
                Text("Add")
            }
        }
        
        if (uiState.manuallyAddedLlmModels.isNotEmpty()) {
            Text(
                text = "Custom Models:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            uiState.manuallyAddedLlmModels.forEach { modelId ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modelId,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.removeManualLlmModel(modelId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Remove model",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomAPIConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "Select a custom API provider from the database or use the button above to configure new ones.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    if (uiState.customLlmProviders.isNotEmpty()) {
        SettingsDropdownItem(
            title = "Custom Provider",
            description = "Select from configured custom API providers",
            selectedValue = uiState.customLlmProviders.find { it.id == uiState.selectedCustomLlmProviderId }?.name ?: "Select provider",
            options = uiState.customLlmProviders.map { it.name },
            onValueChange = { selectedName ->
                val provider = uiState.customLlmProviders.find { it.name == selectedName }
                provider?.let { viewModel.updateSelectedCustomLlmProvider(it.id) }
            }
        )
        
        // Show selected provider details
        uiState.customLlmProviders.find { it.id == uiState.selectedCustomLlmProviderId }?.let { provider ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Provider: ${provider.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Base URL: ${provider.baseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Status: ${if (provider.isEnabled) "✅ Enabled" else "❌ Disabled"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (provider.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    } else {
        Text(
            text = "No custom API providers configured. Use the 'Configure Custom Text Generation APIs' button above to add providers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
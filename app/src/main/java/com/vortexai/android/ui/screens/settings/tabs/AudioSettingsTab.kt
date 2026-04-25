package com.vortexai.android.ui.screens.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortexai.android.BuildConfig
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import com.vortexai.android.ui.screens.settings.components.*

@Composable
fun AudioSettingsTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(title = "Audio Generation") {
                SettingsDropdownItem(
                    title = "Text-to-Speech Provider",
                    description = "Choose TTS service based on API documentation",
                    selectedValue = uiState.ttsProvider,
                    options = listOf("ModelsLab", "Together AI", "ElevenLabs", "Google TTS", "Custom TTS"),
                    onValueChange = viewModel::updateTtsProvider
                )
                
                when (uiState.ttsProvider) {
                    "ModelsLab" -> ModelsLabTTSConfig(uiState, viewModel)
                    "Together AI" -> TogetherAITTSConfig(uiState, viewModel)
                    "ElevenLabs" -> ElevenLabsTTSConfig(uiState, viewModel)
                    "Custom TTS" -> CustomTTSConfig(uiState, viewModel)
                }
            }
        }
        
        item {
            SettingsSection(title = "Audio Settings") {
                SettingsSwitchItem(
                    title = "Auto-play TTS",
                    description = "Automatically read AI responses aloud",
                    checked = uiState.autoPlayTts,
                    onCheckedChange = viewModel::updateAutoPlayTts
                )
                
                SettingsSwitchItem(
                    title = "Voice Activation",
                    description = "Enable voice input for messages",
                    checked = uiState.voiceActivation,
                    onCheckedChange = viewModel::updateVoiceActivation
                )
            }
        }
        
        item {
            Button(
                onClick = { viewModel.testAudioConnection() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Test Audio Connection")
            }
            
            if (uiState.endpointError.isNotBlank()) {
                Text(
                    text = uiState.endpointError,
                    color = if (uiState.endpointError.startsWith("✅")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        if (uiState.ttsProvider == "ModelsLab") {
            item {
                ManualModelsLabVoices(uiState, viewModel)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveAudioSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Audio Settings")
            }
        }
    }
}

@Composable
private fun ModelsLabTTSConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "ModelsLab API Key",
        value = uiState.modelsLabApiKey,
        onValueChange = viewModel::updateModelsLabApiKey,
        placeholder = "Enter your ModelsLab API key"
    )
    
    SettingsDropdownItem(
        title = "ModelsLab TTS Model",
        description = "Select TTS model for ModelsLab",
        selectedValue = if (uiState.ttsModel.isBlank()) "inworld-tts-1" else uiState.ttsModel,
        options = listOf("inworld-tts-1", "eleven_multilingual_v2"),
        onValueChange = viewModel::updateTtsModel
    )
    
    when (uiState.ttsModel.ifBlank { "inworld-tts-1" }) {
        "inworld-tts-1" -> {
            val inworldVoices = listOf("Olivia", "Sarah")
            SettingsDropdownItem(
                title = "TTS Voice",
                description = "Select a voice for inworld-tts-1",
                selectedValue = if (uiState.ttsVoice.isBlank()) inworldVoices.first() else uiState.ttsVoice,
                options = inworldVoices,
                onValueChange = viewModel::updateTtsVoice
            )
        }
        "eleven_multilingual_v2" -> {
            val elevenVoices = listOf("Olivia", "Bunny", "Zoe", "sScFwemjGrAkDDiTXWMH", "egBwWoDtXGCSMgwiS3vn")
            SettingsDropdownItem(
                title = "TTS Voice",
                description = "Select a voice for Eleven Multilingual V2",
                selectedValue = if (uiState.ttsVoice.isBlank()) elevenVoices.first() else uiState.ttsVoice,
                options = elevenVoices,
                onValueChange = viewModel::updateTtsVoice
            )
        }
    }
    
    // Test voice section
    var testText by remember { mutableStateOf("Hello! This is a test of the selected voice.") }
    OutlinedTextField(
        value = testText,
        onValueChange = { testText = it },
        label = { Text("Enter text to test voice") },
        placeholder = { Text("Type something to hear how the voice sounds...") },
        modifier = Modifier.fillMaxWidth(),
        isError = uiState.testAudioError.isNotBlank()
    )
    
    if (uiState.testAudioError.isNotBlank()) {
        Text(
            text = uiState.testAudioError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    
    Button(
        onClick = { viewModel.testAudio(testText) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        enabled = !uiState.isTestingAudio && testText.isNotBlank()
    ) {
        if (uiState.isTestingAudio) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generating Audio...")
        } else {
            Text("Test Voice")
        }
    }
    
    SettingsTextFieldItem(
        title = "Init Audio URL (Optional)",
        value = uiState.ttsInitAudio,
        onValueChange = viewModel::updateTtsInitAudio,
        placeholder = "URL to 4-30 second MP3/WAV for voice cloning"
    )
}

@Composable
private fun TogetherAITTSConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Together AI TTS API Key",
        value = uiState.togetherAiTtsApiKey,
        onValueChange = viewModel::updateTogetherAiTtsApiKey,
        placeholder = "Enter your Together AI API key for TTS"
    )
    
    if (uiState.togetherAiTtsApiKey.isBlank()) {
        Text(
            text = "⚠️ Together AI API key required for TTS functionality",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
    
    if (uiState.togetherAiTtsApiKey.isNotBlank()) {
        SettingsDropdownItem(
            title = "TTS Model",
            description = "Select Together AI TTS model",
            selectedValue = if (uiState.ttsModel.isBlank()) "cartesia/sonic" else uiState.ttsModel,
            options = listOf("cartesia/sonic", "cartesia/sonic-2"),
            onValueChange = viewModel::updateTtsModel
        )
        
        SettingsDropdownItem(
            title = "TTS Voice",
            description = "Select voice for speech synthesis",
            selectedValue = if (uiState.ttsVoice.isBlank()) "Barbershop Man" else uiState.ttsVoice,
            options = listOf(
                "Barbershop Man",
                "Conversational Woman", 
                "Customer Service Woman",
                "Newscaster Man",
                "Newscaster Woman"
            ),
            onValueChange = viewModel::updateTtsVoice
        )
    }
    
    Text(
        text = "Together AI provides cartesia/sonic and cartesia/sonic-2 models with 5 realistic voices",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ElevenLabsTTSConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "ElevenLabs API Key",
        value = uiState.elevenLabsApiKey,
        onValueChange = viewModel::updateElevenLabsApiKey,
        placeholder = "Enter your ElevenLabs API key"
    )
    
    if (uiState.elevenLabsApiKey.isBlank()) {
        Text(
            text = "⚠️ ElevenLabs API key required for TTS functionality",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
    
    if (uiState.elevenLabsApiKey.isNotBlank()) {
        SettingsDropdownItem(
            title = "TTS Model",
            description = "Select ElevenLabs TTS model",
            selectedValue = if (uiState.ttsModel.isBlank()) "eleven_multilingual_v2" else uiState.ttsModel,
            options = listOf(
                "eleven_multilingual_v2",
                "eleven_turbo_v2_5",
                "eleven_turbo_v2",
                "eleven_monolingual_v1",
                "eleven_multilingual_v1"
            ),
            onValueChange = viewModel::updateTtsModel
        )
        
        SettingsDropdownItem(
            title = "Language",
            description = "Select language for TTS",
            selectedValue = uiState.elevenLabsLanguage,
            options = listOf("English", "Hindi"),
            onValueChange = viewModel::updateElevenLabsLanguage
        )
        
        when (uiState.elevenLabsLanguage) {
            "English" -> {
                SettingsDropdownItem(
                    title = "TTS Voice",
                    description = "Select English voice for speech synthesis",
                    selectedValue = if (uiState.ttsVoice.isBlank()) "Rachel" else uiState.ttsVoice,
                    options = listOf(
                        "Rachel", "Drew", "Clyde", "Paul", "Domi", "Dave", "Fin", "Sarah", "Antoni", "Thomas",
                        "j05EIz3iI3JmBTWC3CsA", "PB6BdkFkZLbI39GHdnbQ", "kCx3Qoh3lfILbbTZftSq"
                    ),
                    onValueChange = viewModel::updateTtsVoice
                )
            }
            "Hindi" -> {
                SettingsDropdownItem(
                    title = "TTS Voice",
                    description = "Select Hindi voice for speech synthesis",
                    selectedValue = if (uiState.ttsVoiceHindi.isBlank()) "Prabhat" else uiState.ttsVoiceHindi,
                    options = listOf(
                        "Prabhat", "Abhishek", "Aditi", "Arjun", "Kavya"
                    ),
                    onValueChange = viewModel::updateTtsVoiceHindi
                )
            }
        }
        
        // Test voice section for ElevenLabs
        var testText by remember { mutableStateOf("Hello! This is a test of the selected voice.") }
        OutlinedTextField(
            value = testText,
            onValueChange = { testText = it },
            label = { Text("Enter text to test voice") },
            placeholder = { Text("Type something to hear how the voice sounds...") },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.testAudioError.isNotBlank()
        )
        
        if (uiState.testAudioError.isNotBlank()) {
            Text(
                text = uiState.testAudioError,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.testAudioError.startsWith("✅")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Button(
            onClick = { viewModel.testAudio(testText) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isTestingAudio && testText.isNotBlank()
        ) {
            if (uiState.isTestingAudio) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating Audio...")
            } else {
                Text("Test Voice")
            }
        }
    }
    
    Text(
        text = "ElevenLabs provides high-quality multilingual TTS with natural voices in English and Hindi",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun CustomTTSConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    // Help Documentation
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Help",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Custom TTS Configuration Help",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "How to configure your Custom TTS API:\n" +
                      "1. Enter your API endpoint (e.g., https://api.example.com)\n" +
                      "2. Set the API prefix (default: /v1)\n" +
                      "3. Add your API key\n" +
                      "4. Add voices manually\n" +
                      "5. Your endpoint should support OpenAI-compatible TTS",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
    
    SettingsTextFieldItem(
        title = "Custom Endpoint",
        value = uiState.customAudioEndpoint,
        onValueChange = viewModel::updateCustomAudioEndpoint,
        placeholder = "https://your-tts-api.com"
    )
    SettingsTextFieldItem(
        title = "API Prefix",
        value = uiState.customAudioApiPrefix,
        onValueChange = viewModel::updateCustomAudioApiPrefix,
        placeholder = "/v1"
    )
    SettingsPasswordFieldItem(
        title = "TTS API Key",
        value = uiState.ttsApiKey,
        onValueChange = viewModel::updateTtsApiKey,
        placeholder = "Enter your TTS API key"
    )
    
    // Manual Voice Input Section
    var manualAudioVoiceInput by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        Text(
            text = "Manual Voice Management",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualAudioVoiceInput,
                onValueChange = { manualAudioVoiceInput = it },
                placeholder = { Text("Enter voice name (e.g., alloy, echo, fable)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (manualAudioVoiceInput.isNotBlank()) {
                        viewModel.addManualCustomAudioModel(manualAudioVoiceInput.trim())
                        manualAudioVoiceInput = ""
                    }
                },
                enabled = manualAudioVoiceInput.isNotBlank()
            ) {
                Text("Add")
            }
        }
        
        // Display manually added voices
        if (uiState.manuallyAddedCustomAudioModels.isNotEmpty()) {
            Text(
                text = "Manually Added Voices:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            uiState.manuallyAddedCustomAudioModels.forEach { voiceId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = voiceId,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.removeManualCustomAudioModel(voiceId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove voice",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualModelsLabVoices(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsSection(title = "Add New Models and Voices") {
        Text(
            text = "Add custom TTS models and their respective voices for ModelsLab API. These will be available in addition to the default models.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        var newVoiceId by remember { mutableStateOf("") }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newVoiceId,
                onValueChange = { newVoiceId = it },
                placeholder = { Text("Enter voice ID (e.g., custom-voice-1)") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newVoiceId.isNotBlank()) {
                        viewModel.addManualVoice(newVoiceId)
                        newVoiceId = ""
                    }
                },
                enabled = newVoiceId.isNotBlank()
            ) {
                Text("Add")
            }
        }
        
        if (uiState.manuallyAddedVoices.isNotEmpty()) {
            Text(
                text = "Manually Added Voices:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            uiState.manuallyAddedVoices.forEach { voiceId ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = voiceId,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.removeManualVoice(voiceId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Remove voice",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
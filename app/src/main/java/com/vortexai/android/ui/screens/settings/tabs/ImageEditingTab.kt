package com.vortexai.android.ui.screens.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import com.vortexai.android.ui.screens.settings.components.*
import androidx.compose.material3.Slider

@Composable
fun ImageEditingTab(
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
            SettingsSection(title = "Image Editing Provider") {
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
                    Text("Configure Custom Image Editing APIs")
                }
                
                SettingsDropdownItem(
                    title = "Image Editing Provider",
                    description = "Choose your image-to-image editing provider",
                    selectedValue = uiState.imageEditingProvider,
                    options = listOf("Replicate", "Together AI", "Modelslab", "ComfyUI", "Custom API"),
                    onValueChange = viewModel::updateImageEditingProvider
                )
                
                when (uiState.imageEditingProvider) {
                    "Replicate" -> ReplicateEditingConfig(uiState, viewModel)
                    "Together AI" -> TogetherAIEditingConfig(uiState, viewModel)
                    "Modelslab" -> ModelslabEditingConfig(uiState, viewModel)
                    "ComfyUI" -> ComfyUIEditingConfig(uiState, viewModel)
                    "Custom API" -> CustomAPIImageEditConfig(uiState, viewModel)
                }
            }
        }
        
        item {
            SettingsSection(title = "Image Editing Settings") {
                when (uiState.imageEditingProvider) {
                    "Modelslab" -> {
                        SettingsDropdownItem(
                            title = "Modelslab Model",
                            description = "Choose Modelslab model for image editing",
                            selectedValue = uiState.modelslabEditingModel,
                            options = listOf(
                                "flux-kontext-pro",
                                "flux-kontext-dev",
                                "seedream-4",
                                "nano-banana"
                            ),
                            onValueChange = viewModel::updateModelslabEditingModel
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Modelslab Image Editing",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• flux-kontext-pro: Professional model with aspect ratio\n• flux-kontext-dev: Standard model with aspect ratio\n• seedream-4: Simple array-based model\n• nano-banana: Simple string-based model\n• Most models use v7 API",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (uiState.modelslabEditingModel == "flux-kontext-dev") {
                            SettingsSliderItem(
                                title = "Edit Strength",
                                description = "How much to change the original image (0.1 - 1.0)",
                                value = uiState.modelslabEditingStrengthFloat ?: 0.7f,
                                onValueChange = viewModel::updateModelslabEditingStrengthFloat,
                                valueRange = 0.1f..1.0f,
                                steps = 17,
                                displayValue = String.format("%.1f", uiState.modelslabEditingStrengthFloat ?: 0.7f)
                            )
                        }
                        
                        SettingsTextFieldItem(
                            title = "Modelslab Negative Prompt",
                            value = uiState.modelslabNegativePrompt,
                            onValueChange = viewModel::updateModelslabNegativePrompt,
                            placeholder = "Enter negative prompt for Modelslab"
                        )
                    }
                    "Replicate" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Replicate Image Editing",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Replicate models use optimized settings for image editing. Quality, format, and enhancement options are handled automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    "Together AI" -> {
                        SettingsDropdownItem(
                            title = "Image Editing Model",
                            description = "Choose Together AI model for image editing",
                            selectedValue = uiState.imageEditingModel,
                            options = listOf(
                                "black-forest-labs/FLUX.1-kontext-dev",
                                "black-forest-labs/FLUX.1-kontext-pro",
                                "black-forest-labs/FLUX.1-kontext-max"
                            ),
                            onValueChange = viewModel::updateImageEditingModel
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "FLUX Kontext Models",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• kontext-dev: Available on all tiers\n• kontext-pro/max: Require higher tier access\n• Specialized for image-to-image editing\n• Maintains image structure while applying changes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        SettingsDropdownItem(
                            title = "Edit Strength",
                            description = "How much to change the original image",
                            selectedValue = uiState.imageEditingStrength,
                            options = listOf("Low (0.3)", "Medium (0.5)", "High (0.7)", "Maximum (0.9)"),
                            onValueChange = viewModel::updateImageEditingStrength
                        )
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = { viewModel.saveImageEditingSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Image Editing Settings")
            }
        }
    }
}

@Composable
private fun ReplicateEditingConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Replicate API Key",
        value = uiState.replicateEditingApiKey,
        onValueChange = viewModel::updateReplicateEditingApiKey,
        placeholder = "r8_... (example)"
    )
    
    if (uiState.replicateEditingApiKey.isNotBlank()) {
        SettingsDropdownItem(
            title = "Replicate Editing Model",
            description = "Choose model for image editing",
            selectedValue = uiState.replicateEditingModel,
            options = listOf("qwen-image-edit"),
            onValueChange = viewModel::updateReplicateEditingModel
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Qwen Image Edit Features",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Advanced image-to-image editing\n• Natural language prompts\n• Preserves image structure\n• Fast processing (go_fast: true)\n• WebP output format\n• Quality: 80\n• No prompt enhancement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModelslabEditingConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Modelslab API Key",
        value = uiState.modelslabEditingApiKey,
        onValueChange = viewModel::updateModelslabEditingApiKey,
        placeholder = "Enter your Modelslab API key"
    )
    
    SettingsPasswordFieldItem(
        title = "imgbb API Key",
        value = uiState.imgbbApiKey,
        onValueChange = viewModel::updateImgbbApiKey,
        placeholder = "Enter your imgbb API key for image upload"
    )
    
    Text(
        text = "Modelslab v7 provides three models for image-to-image editing. imgbb is used to upload local images to get URLs required by Modelslab API.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun TogetherAIEditingConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Together AI API Key",
        value = uiState.togetherAiEditingApiKey,
        onValueChange = viewModel::updateTogetherAiEditingApiKey,
        placeholder = "Enter your Together AI API key"
    )
    
    Text(
        text = "Together AI provides FLUX models for high-quality image editing with customizable strength settings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ComfyUIEditingConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.importComfyUiEditingWorkflow(context, it)
        }
    }

    SettingsTextFieldItem(
        title = "ComfyUI Endpoint",
        value = uiState.comfyUiEditingEndpoint,
        onValueChange = viewModel::updateComfyUiEditingEndpoint,
        placeholder = "http://localhost:8188"
    )
    
    // Default to the embedded workflow if nothing is selected yet
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadAvailableComfyUiEditingWorkflows(context)
    }

    if (uiState.availableComfyUiEditingWorkflows.isNotEmpty()) {
        SettingsDropdownItem(
            title = "Workflow Selection",
            description = "Select the ComfyUI Workflow JSON to execute",
            options = uiState.availableComfyUiEditingWorkflows,
            selectedValue = uiState.comfyUiEditingWorkflow.ifBlank { uiState.availableComfyUiEditingWorkflows.first() },
            onValueChange = viewModel::updateComfyUiEditingWorkflow
        )
    }

    SettingsTextFieldItem(
        title = "Model Checkpoint Options (Optional)",
        value = uiState.comfyUiEditingCheckpoint,
        onValueChange = viewModel::updateComfyUiEditingCheckpoint,
        placeholder = "e.g. flux1-dev.safetensors"
    )
    
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
    ) {
        androidx.compose.material3.TextButton(onClick = { launcher.launch("application/json") }) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp).padding(end = 4.dp)
            )
            Text("Import Custom (.json)")
        }
    }
    SettingsSwitchItem(
        title = "Maintain Aspect Ratio",
        description = "Keep source image aspect ratio (resize if needed)",
        checked = uiState.comfyUiMaintainAspectRatio,
        onCheckedChange = viewModel::updateComfyUiMaintainAspectRatio
    )
}

@Composable
private fun CustomAPIImageEditConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "Select a custom API provider from the database for image editing.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    if (uiState.customImageEditProviders.isNotEmpty()) {
        SettingsDropdownItem(
            title = "Custom Provider",
            description = "Select from configured custom image editing providers",
            selectedValue = uiState.customImageEditProviders.find { it.id == uiState.selectedCustomImageEditProviderId }?.name ?: "Select provider",
            options = uiState.customImageEditProviders.map { it.name },
            onValueChange = { selectedName ->
                val provider = uiState.customImageEditProviders.find { it.name == selectedName }
                provider?.let { viewModel.updateSelectedCustomImageEditProvider(it.id) }
            }
        )
        
        uiState.customImageEditProviders.find { it.id == uiState.selectedCustomImageEditProviderId }?.let { provider ->
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
            text = "No custom image editing providers configured. Use the 'Configure Custom Image Editing APIs' button above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
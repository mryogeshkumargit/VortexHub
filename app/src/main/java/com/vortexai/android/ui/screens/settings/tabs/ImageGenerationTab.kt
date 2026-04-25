package com.vortexai.android.ui.screens.settings.tabs

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import com.vortexai.android.ui.screens.settings.components.*

@Composable
fun ImageGenerationTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigateToSSLSettings: () -> Unit,
    onNavigateToCustomApi: () -> Unit = {},
    onShowDeleteAllCharactersDialog: () -> Unit
) {
    val context = LocalContext.current
    val bulkImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        viewModel.processBulkImport(context, uri)
    }
    
    val workflowFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadComfyUiWorkflowFromFile(context, it) }
    }
    
    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.setBulkImportCallback { bulkImportLauncher.launch(null) }
        viewModel.setWorkflowFileLauncher { workflowFileLauncher.launch(arrayOf("application/json", "text/plain")) }
        onDispose { }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(title = "Image Provider") {
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
                    Text("Configure Custom Image Generation APIs")
                }
                
                SettingsDropdownItem(
                    title = "Image Provider",
                    description = "Choose your image generation provider",
                    selectedValue = uiState.imageProvider,
                    options = listOf("Together AI", "Hugging Face", "ComfyUI", "ModelsLab", "Replicate", "Grok", "Custom API"),
                    onValueChange = viewModel::updateImageProvider
                )
                
                when (uiState.imageProvider) {
                    "Together AI" -> TogetherAIImageConfig(uiState, viewModel)
                    "Hugging Face" -> HuggingFaceImageConfig(uiState, viewModel)
                    "ComfyUI" -> ComfyUIConfig(uiState, viewModel)
                    "ModelsLab" -> ModelsLabConfig(uiState, viewModel)
                    "Replicate" -> ReplicateConfig(uiState, viewModel, onShowDeleteAllCharactersDialog, LocalContext.current)
                    "Grok" -> GrokImageConfig(uiState, viewModel)
                    "Custom API" -> CustomAPIImageConfig(uiState, viewModel)
                }
            }
        }
        
        item {
            SettingsSection(title = "Image Settings") {
                val isCustomApi = uiState.imageProvider == "Custom API"
                val isModelsLabFlux = uiState.imageProvider == "ModelsLab" && uiState.modelsLabWorkflow == "flux"
                val isReplicate = uiState.imageProvider == "Replicate"
                val isGrok = uiState.imageProvider == "Grok"
                
                if (!isReplicate && !isGrok && !isCustomApi) {
                    SettingsDropdownItem(
                        title = "Image Size",
                        description = "Generated image dimensions",
                        selectedValue = uiState.imageSize,
                        options = listOf("512x512", "768x768", "1024x1024", "1024x768", "768x1024"),
                        onValueChange = viewModel::updateImageSize
                    )
                }
                
                if (!isModelsLabFlux && !isReplicate && !isGrok && !isCustomApi && uiState.imageProvider != "ComfyUI") {
                    SettingsDropdownItem(
                        title = "Image Quality",
                        description = "Balance between quality and speed",
                        selectedValue = uiState.imageQuality,
                        options = listOf("Standard", "HD"),
                        onValueChange = viewModel::updateImageQuality
                    )
                    
                    val currentProviderModels = when (uiState.imageProvider) {
                        "Together AI" -> uiState.manuallyAddedTogetherAiImageModels
                        "Hugging Face" -> uiState.manuallyAddedHuggingFaceImageModels
                        "ModelsLab" -> uiState.manuallyAddedImageModels
                        "ComfyUI" -> uiState.availableImageModels
                        else -> emptyList()
                    }
                    
                    if (currentProviderModels.isNotEmpty()) {
                        SettingsDropdownItem(
                            title = "Image Model",
                            description = "Choose from available image generation models",
                            selectedValue = uiState.imageModel,
                            options = currentProviderModels,
                            onValueChange = viewModel::updateImageModel
                        )
                    } else {
                        SettingsTextFieldItem(
                            title = "Image Model",
                            value = uiState.imageModel,
                            onValueChange = viewModel::updateImageModel,
                            placeholder = "e.g., stabilityai/stable-diffusion-xl-base-1.0"
                        )
                    }
                }
                
                if (!isReplicate && !isGrok && !isCustomApi) {
                    SettingsSliderItem(
                        title = "Steps",
                        description = "Number of diffusion steps (more = better quality)",
                        value = uiState.steps.toFloat(),
                        onValueChange = { viewModel.updateSteps(it.roundToInt()) },
                        valueRange = 10f..100f,
                        steps = 17,
                        displayValue = uiState.steps.toString()
                    )
                    
                    SettingsSliderItem(
                        title = "Guidance Scale",
                        description = "How closely to follow the prompt",
                        value = uiState.guidanceScale,
                        onValueChange = viewModel::updateGuidanceScale,
                        valueRange = 1.0f..20.0f,
                        steps = 18,
                        displayValue = "%.1f".format(uiState.guidanceScale)
                    )
                    
                    SettingsTextFieldItem(
                        title = "Negative Prompt (Optional)",
                        value = uiState.negativePrompt,
                        onValueChange = viewModel::updateNegativePrompt,
                        placeholder = "What to avoid in the image (e.g., bad quality, blurry, distorted)"
                    )
                } else if (isReplicate) {
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
                                    text = "Replicate Configuration",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Replicate models use optimized settings. Image size, quality, steps, guidance scale, and negative prompts are handled automatically by each model.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (isGrok) {
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
                                    text = "Grok Configuration",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Grok uses grok-2-image-1212 model with optimized settings. Image size, quality, steps, and guidance scale are handled automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        if (uiState.imageProvider == "ModelsLab") {
            item {
                ManualModelsLabImageModels(uiState, viewModel)
            }
            
            item {
                ManualModelsLabLoRAModels(uiState, viewModel)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveImageSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Image Settings")
            }
        }
        
        item {
            SSLTroubleshootingCard(onNavigateToSSLSettings)
        }
    }
}

@Composable
private fun TogetherAIImageConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Together AI API Key",
        value = uiState.togetherAiImageApiKey,
        onValueChange = viewModel::updateTogetherAiImageApiKey,
        placeholder = "Enter your Together AI API key"
    )
    
    AddImageModelSection(
        provider = "Together AI",
        manualModels = uiState.manuallyAddedTogetherAiImageModels,
        onAddModel = viewModel::addManualTogetherAiImageModel,
        onRemoveModel = viewModel::removeManualTogetherAiImageModel
    )
}

@Composable
private fun HuggingFaceImageConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Hugging Face API Key",
        value = uiState.huggingFaceImageApiKey,
        onValueChange = viewModel::updateHuggingFaceImageApiKey,
        placeholder = "Enter your Hugging Face API key"
    )
    
    AddImageModelSection(
        provider = "Hugging Face",
        manualModels = uiState.manuallyAddedHuggingFaceImageModels,
        onAddModel = viewModel::addManualHuggingFaceImageModel,
        onRemoveModel = viewModel::removeManualHuggingFaceImageModel
    )
}

@Composable
private fun ComfyUIConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsTextFieldItem(
        title = "ComfyUI Endpoint",
        value = uiState.comfyUiEndpoint,
        onValueChange = viewModel::updateComfyUiEndpoint,
        placeholder = "http://localhost:8188"
    )
    
    if (uiState.comfyUiEndpoint.isNotBlank()) {
        Button(
            onClick = { viewModel.fetchComfyUiModels() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !uiState.isLoadingComfyUiModels
        ) {
            if (uiState.isLoadingComfyUiModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Fetch Available Models & LoRAs")
        }
    }
    
    // Model Selection
    if (uiState.comfyUiModels.isNotEmpty()) {
        SettingsDropdownItem(
            title = "Model",
            description = "Choose from available checkpoints and UNets",
            selectedValue = uiState.imageModel,
            options = uiState.comfyUiModels,
            onValueChange = viewModel::updateImageModel,
            searchable = true
        )
    }
    
    // LoRA Configuration
    SettingsSwitchItem(
        title = "Use LoRA",
        description = "Enable LoRA blending for image generation",
        checked = uiState.useLora,
        onCheckedChange = viewModel::updateUseLora
    )
    
    if (uiState.useLora && uiState.comfyUiLoraModels.isNotEmpty()) {
        SettingsDropdownItem(
            title = "LoRA Model",
            description = "Choose a LoRA model for blending",
            selectedValue = uiState.loraModel,
            options = uiState.comfyUiLoraModels,
            onValueChange = viewModel::updateLoraModel,
            searchable = true
        )
        
        SettingsSliderItem(
            title = "LoRA Strength",
            description = "How strongly to apply the LoRA effect",
            value = uiState.loraStrength,
            onValueChange = viewModel::updateLoraStrength,
            valueRange = 0.0f..2.0f,
            steps = 20,
            displayValue = "%.2f".format(uiState.loraStrength)
        )
    }
    
    // Image Dimensions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = uiState.imageWidth.toString(),
            onValueChange = { 
                it.toIntOrNull()?.let { width -> 
                    if (width > 0) viewModel.updateImageWidth(width)
                }
            },
            label = { Text("Width") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.imageHeight.toString(),
            onValueChange = { 
                it.toIntOrNull()?.let { height -> 
                    if (height > 0) viewModel.updateImageHeight(height)
                }
            },
            label = { Text("Height") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
    
    // Seed Configuration
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = if (uiState.imageSeed == -1L) "Random" else uiState.imageSeed.toString(),
            onValueChange = { 
                if (it.lowercase() == "random" || it.isBlank()) {
                    viewModel.updateImageSeed(-1L)
                } else {
                    it.toLongOrNull()?.let { seed -> 
                        viewModel.updateImageSeed(seed)
                    }
                }
            },
            label = { Text("Seed") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Button(
            onClick = { viewModel.randomizeImageSeed() }
        ) {
            Text("Random")
        }
    }
    
    // ComfyUI Negative Prompt
    SettingsTextFieldItem(
        title = "Negative Prompt (ComfyUI)",
        value = uiState.comfyUiNegativePrompt,
        onValueChange = viewModel::updateComfyUiNegativePrompt,
        placeholder = "What to avoid in the image (e.g., bad quality, blurry, distorted)"
    )
    
    // Custom Workflow File Browser
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        Text(
            text = "Custom Workflow (JSON)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.comfyUiWorkflowFileName,
                onValueChange = { },
                label = { Text("Selected File") },
                modifier = Modifier.weight(1f),
                readOnly = true,
                placeholder = { Text("No file selected") }
            )
            Button(
                onClick = { viewModel.browseComfyUiWorkflow() }
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "Browse",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Browse")
            }
        }
        
        if (uiState.comfyUiCustomWorkflow.isNotBlank()) {
            Text(
                text = "Workflow loaded (${uiState.comfyUiCustomWorkflow.length} characters)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ModelsLabConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "ModelsLab provides high-quality AI image generation with various models. Get your API key from modelslab.com",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    SettingsPasswordFieldItem(
        title = "ModelsLab API Key",
        value = uiState.modelsLabImageApiKey,
        onValueChange = viewModel::updateModelsLabImageApiKey,
        placeholder = "Enter your ModelsLab API key"
    )
    
    AddImageModelSection(
        provider = "ModelsLab",
        manualModels = uiState.manuallyAddedImageModels,
        onAddModel = viewModel::addManualImageModel,
        onRemoveModel = viewModel::removeManualImageModel
    )
    
    SettingsDropdownItem(
        title = "ModelsLab Workflow",
        description = "Choose generation backend",
        selectedValue = uiState.modelsLabWorkflow,
        options = listOf("default", "realtime", "flux"),
        onValueChange = viewModel::updateModelsLabWorkflow
    )
    SettingsSwitchItem(
        title = "Use character image as source",
        description = "Enable img2img with current character portrait",
        checked = uiState.useCharacterImgAsSource,
        onCheckedChange = viewModel::updateUseCharacterImgAsSource
    )
    
    if (uiState.modelsLabWorkflow != "flux") {
        SettingsSwitchItem(
            title = "Use LoRA",
            description = "Enable LoRA blending for image generation",
            checked = uiState.useLora,
            onCheckedChange = viewModel::updateUseLora
        )
        
        if (uiState.manuallyAddedImageModels.isNotEmpty()) {
            SettingsDropdownItem(
                title = "Image Model",
                description = "Choose a ModelsLab image model",
                selectedValue = uiState.imageModel,
                options = uiState.manuallyAddedImageModels,
                onValueChange = viewModel::updateImageModel
            )
        } else {
            SettingsTextFieldItem(
                title = "Image Model",
                value = uiState.imageModel,
                onValueChange = viewModel::updateImageModel,
                placeholder = "e.g., stable-diffusion-v1-5"
            )
        }
        
        if (uiState.useLora) {
            SettingsDropdownItem(
                title = "LoRA Model",
                description = "Choose a LoRA model",
                selectedValue = uiState.loraModel,
                options = listOf(
                    "flux-image-enhancer-by-dever-v1-0",
                    "dark-gothic-anime-flux-v0-1",
                    "smoke-flux-sdxl-by-dever-flux"
                ),
                onValueChange = viewModel::updateLoraModel
            )
            SettingsSliderItem(
                title = "LoRA Strength",
                description = "Blend strength",
                value = uiState.loraStrength,
                onValueChange = viewModel::updateLoraStrength,
                valueRange = 0.0f..1.0f,
                steps = 10,
                displayValue = "%.2f".format(uiState.loraStrength)
            )
        }
    } else {
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
                        text = "Flux Configuration",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Flux workflow uses optimized settings. LoRA models, image quality, and model selection are not required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReplicateConfig(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onShowDeleteAllCharactersDialog: () -> Unit,
    context: Context
) {
    Text(
        text = "Replicate provides image-to-image and text-to-image generation using advanced AI models. Get your API key from replicate.com",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    SettingsPasswordFieldItem(
        title = "Replicate API Key",
        value = uiState.replicateApiKey,
        onValueChange = viewModel::updateReplicateApiKey,
        placeholder = "r8_... (example)"
    )
    
    if (uiState.replicateApiKey.isNotBlank()) {
        SettingsSwitchItem(
            title = "Disable Safety Checker (NSFW Filter)",
            description = "Allows generation of adult content. Only works with FLUX and Stable Diffusion models. Use responsibly.",
            checked = uiState.replicateDisableSafetyChecker,
            onCheckedChange = viewModel::updateReplicateDisableSafetyChecker
        )
        
        SettingsTextFieldItem(
            title = "Negative Prompt (Optional)",
            value = uiState.replicateNegativePrompt,
            onValueChange = viewModel::updateReplicateNegativePrompt,
            placeholder = "bad quality, blurry, distorted, deformed, ugly"
        )
        
        val usesAspectRatio = uiState.replicateModel in listOf(
            "black-forest-labs/flux-dev",
            "black-forest-labs/flux-dev-lora",
            "black-forest-labs/flux-schnell",
            "tencent/hunyuan-image-3",
            "qwen/qwen-image"
        )
        val usesWidthHeight = uiState.replicateModel in listOf(
            "lucataco/ssd-1b",
            "adirik/realvisxl-v3.0-turbo",
            "playgroundai/playground-v2.5-1024px-aesthetic",
            "stability-ai/sdxl"
        )
        
        if (usesAspectRatio) {
            SettingsDropdownItem(
                title = "Aspect Ratio",
                description = "Image aspect ratio",
                selectedValue = "${uiState.replicateWidth}:${uiState.replicateHeight}",
                options = listOf("1:1", "16:9", "9:16", "21:9", "9:21"),
                onValueChange = { ratio ->
                    val parts = ratio.split(":")
                    val w = parts[0].toIntOrNull() ?: 1
                    val h = parts[1].toIntOrNull() ?: 1
                    viewModel.updateReplicateWidth(w * 128)
                    viewModel.updateReplicateHeight(h * 128)
                }
            )
        } else if (usesWidthHeight) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.replicateWidth.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { width -> 
                            if (width > 0) viewModel.updateReplicateWidth(width)
                        }
                    },
                    label = { Text("Width") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.replicateHeight.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { height -> 
                            if (height > 0) viewModel.updateReplicateHeight(height)
                        }
                    },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        
        SettingsDropdownItem(
            title = "Replicate Model",
            description = "Choose from available Replicate models",
            selectedValue = uiState.replicateModel,
            options = listOf(
                "lucataco/ssd-1b",
                "lucataco/dreamshaper-xl-turbo",
                "adirik/realvisxl-v3.0-turbo",
                "playgroundai/playground-v2.5-1024px-aesthetic",
                "stability-ai/sdxl",
                "black-forest-labs/flux-dev",
                "black-forest-labs/flux-dev-lora",
                "tencent/hunyuan-image-3",
                "qwen/qwen-image-edit"
            ),
            onValueChange = viewModel::updateReplicateModel
        )
        
        when (uiState.replicateModel) {
            "qwen-image-edit" -> {
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
                            text = "Qwen Image Edit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Image editing with text prompts\n• Automatically uses current character's avatar\n• Designed for chat conversations\n• Output format: WebP\n• Quality: 80\n• Fast generation enabled\n• Prompt enhancement disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { viewModel.showAvatarFixMessage() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Fix Avatar",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔧 Fix Victoria Orlov Avatar Issue")
                        }
                        
                        Text(
                            text = "This will convert the asset image to base64 format, making it accessible for qwen-image-edit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Character Management",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Button(
                            onClick = { viewModel.bulkImportCharacterCards(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Bulk Import",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📁 Bulk Import Character Cards")
                        }
                        
                        Text(
                            text = "Import multiple character cards at once from a folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { onShowDeleteAllCharactersDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete All",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🗑️ Delete All Characters")
                        }
                        
                        Text(
                            text = "Permanently remove all character cards and data (requires confirmation)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            "google/nano-banana" -> {
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
                            text = "Google Nano Banana",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Text-to-image generation\n• Supports optional image input\n• Output format: JPG\n• High-quality results\n• No additional parameters needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        var debugPrompt by remember { mutableStateOf("A beautiful landscape") }
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text(
                text = "Debug Replicate Input",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = debugPrompt,
                onValueChange = { debugPrompt = it },
                label = { Text("Test Prompt") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            
            Button(
                onClick = { viewModel.debugReplicateInput(debugPrompt) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Debug Input Structure")
            }
            
            if (uiState.debugMessage.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Debug Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.debugMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrokImageConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsPasswordFieldItem(
        title = "Grok API Key",
        value = uiState.grokImageApiKey,
        onValueChange = viewModel::updateGrokImageApiKey,
        placeholder = "Enter your Grok API key"
    )
    Text(
        text = "Grok provides grok-2-image-1212 model for image generation.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}



@Composable
private fun AddImageModelSection(
    provider: String,
    manualModels: List<String>,
    onAddModel: (String) -> Unit,
    onRemoveModel: (String) -> Unit
) {
    var newModelInput by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        Text(
            text = "Add $provider Image Models",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newModelInput,
                onValueChange = { newModelInput = it },
                placeholder = { Text("Enter image model name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newModelInput.isNotBlank()) {
                        onAddModel(newModelInput.trim())
                        newModelInput = ""
                    }
                },
                enabled = newModelInput.isNotBlank()
            ) {
                Text("Add")
            }
        }
        
        if (manualModels.isNotEmpty()) {
            Text(
                text = "Added Models:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            manualModels.forEach { modelId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modelId,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onRemoveModel(modelId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
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
private fun ManualModelsLabImageModels(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsSection(title = "Manual ModelsLab Image Models") {
        Text(
            text = "Add or remove custom image models for ModelsLab API. These models will be available in addition to the hardcoded list.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        var newImageModelId by remember { mutableStateOf("") }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newImageModelId,
                onValueChange = { newImageModelId = it },
                placeholder = { Text("Enter image model ID") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newImageModelId.isNotBlank()) {
                        viewModel.addManualImageModel(newImageModelId)
                        newImageModelId = ""
                    }
                },
                enabled = newImageModelId.isNotBlank()
            ) {
                Text("Add")
            }
        }
        
        if (uiState.manuallyAddedImageModels.isNotEmpty()) {
            Text(
                text = "Manually Added Image Models:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            uiState.manuallyAddedImageModels.forEach { modelId ->
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
                        onClick = { viewModel.removeManualImageModel(modelId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Remove image model",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualModelsLabLoRAModels(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsSection(title = "Manual ModelsLab LoRA Models") {
        Text(
            text = "Add or remove custom LoRA models for ModelsLab API. These models will be available in addition to the hardcoded list.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        var newLoraModelId by remember { mutableStateOf("") }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newLoraModelId,
                onValueChange = { newLoraModelId = it },
                placeholder = { Text("Enter LoRA model ID") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newLoraModelId.isNotBlank()) {
                        viewModel.addManualLoraModel(newLoraModelId)
                        newLoraModelId = ""
                    }
                },
                enabled = newLoraModelId.isNotBlank()
            ) {
                Text("Add")
            }
        }
        
        if (uiState.manuallyAddedLoraModels.isNotEmpty()) {
            Text(
                text = "Manually Added LoRA Models:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            uiState.manuallyAddedLoraModels.forEach { modelId ->
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
                        onClick = { viewModel.removeManualLoraModel(modelId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Remove LoRA model",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SSLTroubleshootingCard(onNavigateToSSLSettings: () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Troubleshooting",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "If you're experiencing SSL connection issues with ModelsLab API, you can configure advanced SSL settings.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onNavigateToSSLSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "SSL Settings"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("SSL Security Settings")
            }
        }
    }
}

@Composable
private fun CustomAPIImageConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Text(
        text = "Select a custom API provider from the database for image generation.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    if (uiState.customImageProviders.isNotEmpty()) {
        SettingsDropdownItem(
            title = "Custom Provider",
            description = "Select from configured custom image generation providers",
            selectedValue = uiState.customImageProviders.find { it.id == uiState.selectedCustomImageProviderId }?.name ?: "Select provider",
            options = uiState.customImageProviders.map { it.name },
            onValueChange = { selectedName ->
                val provider = uiState.customImageProviders.find { it.name == selectedName }
                provider?.let { viewModel.updateSelectedCustomImageProvider(it.id) }
            }
        )
        
        uiState.customImageProviders.find { it.id == uiState.selectedCustomImageProviderId }?.let { provider ->
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
            text = "No custom image generation providers configured. Use the 'Configure Custom Image Generation APIs' button above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
package com.vortexai.android.ui.screens.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vortexai.android.ui.screens.settings.SettingsUiState
import com.vortexai.android.ui.screens.settings.SettingsViewModel
import com.vortexai.android.ui.screens.settings.components.*

@Composable
fun VideoGenerationTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Provider Selection
        SettingsSection(title = "Video Generation Provider") {
            SettingsDropdownItem(
                title = "Video Provider",
                description = "The selected provider handles image-to-video capabilities across the app.",
                selectedValue = uiState.videoProvider,
                options = listOf("fal.ai", "Replicate", "ModelsLab"),
                onValueChange = viewModel::updateVideoProvider
            )
        }

        // Provider Specific Config
        when (uiState.videoProvider) {
            "fal.ai" -> FalAiVideoConfig(uiState, viewModel)
            "Replicate" -> ReplicateVideoConfig(uiState, viewModel)
            "ModelsLab" -> ModelsLabVideoConfig(uiState, viewModel)
        }
    }
}

@Composable
private fun FalAiVideoConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsSection(title = "Fal AI Video Settings") {
        SettingsPasswordFieldItem(
            title = "Fal AI API Key",
            value = uiState.falAiVideoApiKey,
            onValueChange = viewModel::updateFalAiVideoApiKey,
            placeholder = "Enter your Fal AI API key"
        )

        val models = listOf(
            "fal-ai/kling-video/v1/standard/image-to-video",
            "fal-ai/luma-dream-machine/image-to-video",
            "fal-ai/kling-video/v1.5/pro/image-to-video",
            "fal-ai/minimax/video-01/image-to-video"
        )

        SettingsDropdownItem(
            title = "Video Model",
            description = "Choose which model to use for animation.",
            selectedValue = uiState.falAiVideoModel,
            options = models,
            onValueChange = viewModel::updateFalAiVideoModel
        )
    }
}

@Composable
private fun ReplicateVideoConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsSection(title = "Replicate Video Settings") {
        SettingsPasswordFieldItem(
            title = "Replicate API Key",
            value = uiState.replicateVideoApiKey,
            onValueChange = viewModel::updateReplicateVideoApiKey,
            placeholder = "Enter your Replicate API key"
        )

        val models = listOf(
            "stability-ai/stable-video-diffusion",
            "luma/ray",
            "lucataco/hotshot-xl",
            "minimax/video-01"
        )

        SettingsDropdownItem(
            title = "Video Model",
            description = "Select the Replicate model for video generation.",
            selectedValue = uiState.replicateVideoModel,
            options = models,
            onValueChange = viewModel::updateReplicateVideoModel
        )
    }
}

@Composable
private fun ModelsLabVideoConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsSection(title = "ModelsLab Video Settings") {
        SettingsPasswordFieldItem(
            title = "ModelsLab API Key",
            value = uiState.modelslabVideoApiKey,
            onValueChange = viewModel::updateModelsLabVideoApiKey,
            placeholder = "Enter your ModelsLab API key"
        )

        SettingsTextFieldItem(
            title = "Endpoint/Model Name",
            value = uiState.modelslabVideoModel,
            onValueChange = viewModel::updateModelsLabVideoModel,
            placeholder = "e.g. video/text2video"
        )
    }
}

package com.vortexai.android.ui.screens.characters

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreateScreen(
    onNavigateBack: () -> Unit,
    onCharacterCreated: (String) -> Unit,
    characterIdToEdit: String? = null,
    viewModel: CharacterCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // File picker for character card import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.importCharacterCard(context, it)
        }
    }
    
    // Image picker for avatar selection
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            viewModel.handleImageSelection(context, selectedUri)
        }
    }
    
    var showImportDialog by remember { mutableStateOf(false) }
    
    // Load character for editing if characterIdToEdit is provided
    LaunchedEffect(characterIdToEdit) {
        characterIdToEdit?.let { id ->
            viewModel.loadCharacterForEditing(id)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (characterIdToEdit != null) "Edit Character" else "Create Character",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Import button
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Import character card"
                        )
                    }
                    
                    // Save button
                    IconButton(
                        onClick = { 
                            if (characterIdToEdit != null) {
                                viewModel.updateCharacter(characterIdToEdit) { characterId ->
                                    onCharacterCreated(characterId)
                                }
                            } else {
                                viewModel.createCharacter { characterId ->
                                    onCharacterCreated(characterId)
                                }
                            }
                        },
                        enabled = uiState.isFormValid && !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save character"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                CharacterAvatarSection(
                    character = uiState.character,
                    onAvatarUrlChange = viewModel::updateAvatarUrl,
                    onImagePickerClick = { imagePickerLauncher.launch("image/*") },
                    onAnimateAvatarClick = { provider, model, prompt ->
                        viewModel.generateVideoFace(provider, model, prompt)
                    }
                )
            }
            
            item {
                CharacterBasicInfoSection(
                    character = uiState.character,
                    onNameChange = viewModel::updateName,
                    onDisplayNameChange = viewModel::updateDisplayName,
                    onDescriptionChange = viewModel::updateDescription,
                    onShortDescriptionChange = viewModel::updateShortDescription,
                    onLongDescriptionChange = viewModel::updateLongDescription,
                    onPersonaChange = viewModel::updatePersona,
                    onBackstoryChange = viewModel::updateBackstory
                )
            }
            
            item {
                CharacterEnhancedDetailsSection(
                    character = uiState.character,
                    onAppearanceChange = viewModel::updateAppearance,
                    onPersonalityChange = viewModel::updatePersonality,
                    onScenarioChange = viewModel::updateScenario,
                    onExampleDialogueChange = viewModel::updateExampleDialogue
                )
            }
            
            item {
                CharacterMetadataSection(
                    character = uiState.character,
                    onCreatorChange = viewModel::updateCreator,
                    onCreatorNotesChange = viewModel::updateCreatorNotes,
                    onCharacterVersionChange = viewModel::updateCharacterVersion,
                    onTagsChange = viewModel::updateTags,
                    onCategoriesChange = viewModel::updateCategories
                )
            }
            
            item {
                CharacterExtensionFieldsSection(
                    character = uiState.character,
                    onAgeChange = viewModel::updateAge,
                    onGenderChange = viewModel::updateGender,
                    onOccupationChange = viewModel::updateOccupation
                )
            }
            
            item {
                RoleplaySettingsSection(
                    character = uiState.character,
                    onGreetingChange = viewModel::updateGreeting,
                    onSystemPromptChange = viewModel::updateSystemPrompt,
                    onPostHistoryInstructionsChange = viewModel::updatePostHistoryInstructions,
                    onAlternateGreetingsChange = viewModel::updateAlternateGreetings,
                    onAddAlternateGreeting = viewModel::addAlternateGreeting,
                    onRemoveAlternateGreeting = viewModel::removeAlternateGreeting
                )
            }
            
            item {
                GenerationSettingsSection(
                    character = uiState.character,
                    onTemperatureChange = viewModel::updateTemperature,
                    onTopPChange = viewModel::updateTopP,
                    onMaxTokensChange = viewModel::updateMaxTokens
                )
            }
            
            item {
                ContentSettingsSection(
                    character = uiState.character,
                    onNsfwChange = viewModel::updateNsfw,
                    onPublicChange = viewModel::updatePublic,
                    onFeaturedChange = viewModel::updateIsFeatured,
                    onDynamicStatsChange = viewModel::updateDynamicStatsEnabled
                )
            }
            
            item {
                CharacterBookSection(
                    character = uiState.character,
                    viewModel = viewModel
                )
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error message
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
    
    // Import dialog
    if (showImportDialog) {
        ImportCharacterDialog(
            onDismiss = { showImportDialog = false },
            onImportFromFile = {
                showImportDialog = false
                filePickerLauncher.launch("*/*")
            },
            onImportFromUrl = { url ->
                showImportDialog = false
                viewModel.importCharacterFromUrl(url)
            }
        )
    }
}

@Composable
private fun CharacterAvatarSection(
    character: CharacterCreateState,
    onAvatarUrlChange: (String) -> Unit,
    onImagePickerClick: () -> Unit,
    onAnimateAvatarClick: (String, String, String) -> Unit
) {
    var showAnimateDialog by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Character Avatar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (character.avatarVideoUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                 com.vortexai.android.ui.components.VideoAvatar(
                     videoUrl = character.avatarVideoUrl,
                     imageUrl = character.avatarUrl.ifBlank { null },
                     modifier = Modifier.fillMaxSize()
                 )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onImagePickerClick() },
            contentAlignment = Alignment.Center
        ) {
            if (character.avatarBase64.isNotBlank()) {
                // Display base64 image (from PNG import or image picker)
                val imageBytes = Base64.decode(character.avatarBase64, Base64.DEFAULT)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageBytes)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Character avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (character.avatarUrl.isNotBlank()) {
                // Display URL image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(character.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Character avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default avatar",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Add overlay for upload
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload avatar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = character.avatarUrl,
            onValueChange = onAvatarUrlChange,
            label = { Text("Avatar URL (Optional)") },
            placeholder = { Text("https://example.com/avatar.jpg") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Text(
            text = "Tap the avatar to select an image from your device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        if (character.avatarBase64.isNotBlank() || character.avatarUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showAnimateDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Animate Avatar",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (character.avatarVideoUrl.isNotBlank()) "Re-animate Avatar" else "Animate Avatar")
            }
        }
        
        if (showAnimateDialog) {
            AnimateAvatarDialog(
                onDismiss = { showAnimateDialog = false },
                onConfirm = { provider, model, prompt ->
                    showAnimateDialog = false
                    onAnimateAvatarClick(provider, model, prompt)
                }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnimateAvatarDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf("fal_ai") }
    var selectedModel by remember { mutableStateOf("fal-ai/kling-video/v1/standard/image-to-video") }
    // A simple default prompt to make the character blink and smile slightly
    var prompt by remember { mutableStateOf("Subtle animation, blinks, slight smile, high quality, masterpiece") }
    
    val providers = listOf(
        "fal_ai" to "Fal AI",
        "replicate" to "Replicate",
        "modelslab" to "ModelsLab"
    )
    
    val modelOptions = mapOf(
        "fal_ai" to listOf(
            "fal-ai/kling-video/v1/standard/image-to-video" to "Kling (Standard)",
            "fal-ai/kling-video/v1/pro/image-to-video" to "Kling (Pro)",
            "fal-ai/luma-dream-machine/image-to-video" to "Luma Dream Machine"
        ),
        "replicate" to listOf(
            "stability-ai/stable-video-diffusion" to "Stable Video Diffusion",
            "luma/ray" to "Luma Ray"
        ),
        "modelslab" to listOf(
            "video" to "ModelsLab Video"
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
             Text(text = "Animate Character Avatar")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select an API provider and model to generate a video face from the existing avatar image.")
                
                // Provider Dropdown
                var providerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded }
                ) {
                    OutlinedTextField(
                        value = providers.find { it.first == selectedProvider }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        providers.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedProvider = id
                                    // Reset model selection when provider changes
                                    selectedModel = modelOptions[id]?.firstOrNull()?.first ?: ""
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Model Dropdown
                var modelExpanded by remember { mutableStateOf(false) }
                val currentModels = modelOptions[selectedProvider] ?: emptyList()
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded }
                ) {
                    OutlinedTextField(
                        value = currentModels.find { it.first == selectedModel }?.second ?: selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        currentModels.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedModel = id
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Prompt Input
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Animation Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedProvider, selectedModel, prompt) }
            ) {
                Text("Generate Video")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CharacterBasicInfoSection(
    character: CharacterCreateState,
    onNameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onShortDescriptionChange: (String) -> Unit,
    onLongDescriptionChange: (String) -> Unit,
    onPersonaChange: (String) -> Unit,
    onBackstoryChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = character.name,
                onValueChange = onNameChange,
                label = { Text("Character Name *") },
                placeholder = { Text("Enter character name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = character.name.isBlank()
            )
            
            OutlinedTextField(
                value = character.displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Display Name") },
                placeholder = { Text("Alternative display name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = character.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                placeholder = { Text("Brief description of the character") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            OutlinedTextField(
                value = character.shortDescription,
                onValueChange = onShortDescriptionChange,
                label = { Text("Short Description") },
                placeholder = { Text("Very brief description for previews") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )
            
            OutlinedTextField(
                value = character.longDescription,
                onValueChange = onLongDescriptionChange,
                label = { Text("Long Description") },
                placeholder = { Text("Detailed description of the character") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            
            OutlinedTextField(
                value = character.persona,
                onValueChange = onPersonaChange,
                label = { Text("Persona") },
                placeholder = { Text("Character's persona and role") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            OutlinedTextField(
                value = character.backstory,
                onValueChange = onBackstoryChange,
                label = { Text("Backstory") },
                placeholder = { Text("Character's history and background") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
        }
    }
}

@Composable
private fun CharacterEnhancedDetailsSection(
    character: CharacterCreateState,
    onAppearanceChange: (String) -> Unit,
    onPersonalityChange: (String) -> Unit,
    onScenarioChange: (String) -> Unit,
    onExampleDialogueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enhanced Character Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = character.appearance,
                onValueChange = onAppearanceChange,
                label = { Text("Appearance") },
                placeholder = { Text("Physical appearance and characteristics") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            
            OutlinedTextField(
                value = character.personality,
                onValueChange = onPersonalityChange,
                label = { Text("Personality") },
                placeholder = { Text("Describe the character's personality traits") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            
            OutlinedTextField(
                value = character.scenario,
                onValueChange = onScenarioChange,
                label = { Text("Scenario") },
                placeholder = { Text("Setting and background context") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            OutlinedTextField(
                value = character.exampleDialogue,
                onValueChange = onExampleDialogueChange,
                label = { Text("Example Dialogue") },
                placeholder = { Text("Example conversations showing speech patterns") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
        }
    }
}

@Composable
private fun CharacterMetadataSection(
    character: CharacterCreateState,
    onCreatorChange: (String) -> Unit,
    onCreatorNotesChange: (String) -> Unit,
    onCharacterVersionChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onCategoriesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Metadata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = character.creator,
                onValueChange = onCreatorChange,
                label = { Text("Creator") },
                placeholder = { Text("Character creator name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = character.creatorNotes,
                onValueChange = onCreatorNotesChange,
                label = { Text("Creator Notes") },
                placeholder = { Text("Notes from the character creator") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            OutlinedTextField(
                value = character.characterVersion,
                onValueChange = onCharacterVersionChange,
                label = { Text("Character Version") },
                placeholder = { Text("1.0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = character.tags,
                onValueChange = onTagsChange,
                label = { Text("Tags") },
                placeholder = { Text("fantasy, magic, adventure (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )
            
            OutlinedTextField(
                value = character.categories,
                onValueChange = onCategoriesChange,
                label = { Text("Categories") },
                placeholder = { Text("anime, fantasy, sci-fi (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun CharacterExtensionFieldsSection(
    character: CharacterCreateState,
    onAgeChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onOccupationChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Extension Fields",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = character.age,
                    onValueChange = onAgeChange,
                    label = { Text("Age") },
                    placeholder = { Text("25") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = character.gender,
                    onValueChange = onGenderChange,
                    label = { Text("Gender") },
                    placeholder = { Text("Female") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            OutlinedTextField(
                value = character.occupation,
                onValueChange = onOccupationChange,
                label = { Text("Occupation") },
                placeholder = { Text("Student, Teacher, etc.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun RoleplaySettingsSection(
    character: CharacterCreateState,
    onGreetingChange: (String) -> Unit,
    onSystemPromptChange: (String) -> Unit,
    onPostHistoryInstructionsChange: (String) -> Unit,
    onAlternateGreetingsChange: (List<String>) -> Unit,
    onAddAlternateGreeting: (String) -> Unit,
    onRemoveAlternateGreeting: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Roleplay Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = character.greeting,
                onValueChange = onGreetingChange,
                label = { Text("Greeting Message") },
                placeholder = { Text("Hello! I'm excited to chat with you!") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            OutlinedTextField(
                value = character.systemPrompt,
                onValueChange = onSystemPromptChange,
                label = { Text("System Prompt") },
                placeholder = { Text("System instructions for the AI") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            OutlinedTextField(
                value = character.postHistoryInstructions,
                onValueChange = onPostHistoryInstructionsChange,
                label = { Text("Post History Instructions") },
                placeholder = { Text("Instructions for after conversation history") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            // Alternate Greetings
            Text(
                text = "Alternate Greetings",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            
            character.alternateGreetings.forEachIndexed { index, greeting ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = greeting,
                        onValueChange = { newGreeting ->
                            val newList = character.alternateGreetings.toMutableList()
                            newList[index] = newGreeting
                            onAlternateGreetingsChange(newList)
                        },
                        label = { Text("Greeting ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    IconButton(onClick = { onRemoveAlternateGreeting(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove greeting"
                        )
                    }
                }
            }
            
            Button(
                onClick = { onAddAlternateGreeting("") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Alternate Greeting")
            }
        }
    }
}

@Composable
private fun GenerationSettingsSection(
    character: CharacterCreateState,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Generation Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = character.temperature.toString(),
                    onValueChange = { value -> 
                        try {
                            onTemperatureChange(value.toFloat())
                        } catch (e: NumberFormatException) {
                            // Handle invalid input
                        }
                    },
                    label = { Text("Temperature") },
                    placeholder = { Text("0.7") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = character.topP.toString(),
                    onValueChange = { value -> 
                        try {
                            onTopPChange(value.toFloat())
                        } catch (e: NumberFormatException) {
                            // Handle invalid input
                        }
                    },
                    label = { Text("Top P") },
                    placeholder = { Text("0.9") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            OutlinedTextField(
                value = character.maxTokens.toString(),
                onValueChange = { value -> 
                    try {
                        onMaxTokensChange(value.toInt())
                    } catch (e: NumberFormatException) {
                        // Handle invalid input
                    }
                },
                label = { Text("Max Tokens") },
                placeholder = { Text("1000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ContentSettingsSection(
    character: CharacterCreateState,
    onNsfwChange: (Boolean) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onFeaturedChange: (Boolean) -> Unit,
    onDynamicStatsChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Content Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NSFW Content",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = character.isNsfw,
                    onCheckedChange = onNsfwChange
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Public Character",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = character.isPublic,
                    onCheckedChange = onPublicChange
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Featured Character",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = character.isFeatured,
                    onCheckedChange = onFeaturedChange
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Dynamic Stats System",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Enable character-specific stats tracking (XP, Affection, etc.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = character.dynamicStatsEnabled,
                    onCheckedChange = onDynamicStatsChange
                )
            }
        }
    }
}

@Composable
private fun CharacterBookSection(
    character: CharacterCreateState,
    viewModel: CharacterCreateViewModel
) {
    var showAllEntries by remember { mutableStateOf(false) }
    var selectedEntryIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    
    // File picker for character book import
    val bookImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.importCharacterBookFromFile(context, it)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Character Book / Lorebook",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (character.characterBook != null) {
                EnhancedCharacterBookPreview(
                    characterBook = character.characterBook,
                    showAllEntries = showAllEntries,
                    onShowAllEntriesChange = { showAllEntries = it },
                    selectedEntryIndex = selectedEntryIndex,
                    onSelectedEntryIndexChange = { selectedEntryIndex = it },
                    viewModel = viewModel,
                    onImportBook = { bookImportLauncher.launch("application/json") }
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No character book configured. Character books are imported from SillyTavern character cards.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Create New Character Book",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = "You can create a new character book and add lorebook entries manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { bookImportLauncher.launch("application/json") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Import Character Book JSON")
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.createNewCharacterBook() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create Empty")
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.addNewLorebookEntry() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("With Entry")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedCharacterBookPreview(
    characterBook: CharacterBook,
    showAllEntries: Boolean,
    onShowAllEntriesChange: (Boolean) -> Unit,
    selectedEntryIndex: Int?,
    onSelectedEntryIndexChange: (Int?) -> Unit,
    viewModel: CharacterCreateViewModel,
    onImportBook: () -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Character Book Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Character Book Settings",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = characterBook.name,
                    onValueChange = viewModel::updateCharacterBookName,
                    label = { Text("Book Name") },
                    placeholder = { Text("Enter book name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = characterBook.description,
                    onValueChange = viewModel::updateCharacterBookDescription,
                    label = { Text("Book Description") },
                    placeholder = { Text("Enter book description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = characterBook.scanDepth.toString(),
                        onValueChange = { value ->
                            try {
                                viewModel.updateCharacterBookScanDepth(value.toInt())
                            } catch (e: NumberFormatException) {
                                // Handle invalid input
                            }
                        },
                        label = { Text("Scan Depth") },
                        placeholder = { Text("50") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = characterBook.tokenBudget.toString(),
                        onValueChange = { value ->
                            try {
                                viewModel.updateCharacterBookTokenBudget(value.toInt())
                            } catch (e: NumberFormatException) {
                                // Handle invalid input
                            }
                        },
                        label = { Text("Token Budget") },
                        placeholder = { Text("500") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recursive Scanning",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = characterBook.recursiveScanning,
                        onCheckedChange = viewModel::updateCharacterBookRecursiveScanning
                    )
                }
            }
        }
        
        // Scan Depth and Token Budget Explanation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📖 Character Book Settings Explained:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "• Scan Depth: How many characters back from the current message to search for keywords (default: 50)",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "• Token Budget: Maximum tokens allowed for lorebook entries in the prompt (default: 500)",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "• Recursive Scanning: Whether to scan through nested lorebook entries",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "• Primary Keys: Comma-separated keywords that trigger this entry in chat",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        // Entries Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lorebook Entries (${characterBook.entries.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "ShowAllEntries: $showAllEntries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.addNewLorebookEntry() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Entry")
                            }
                            
                            // Show All Entries button - always visible when character book exists
                            Button(
                                onClick = { onShowAllEntriesChange(!showAllEntries) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text(
                                    text = if (showAllEntries) "Show Preview" else "Show All",
                                    maxLines = 1
                                )
                            }
                        }
                        
                        Button(
                            onClick = onImportBook,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Replace with Imported Book")
                        }
                    }
                }
                
                // Show entries if any exist
                if (characterBook.entries.isNotEmpty()) {
                    val entriesToShow = if (showAllEntries) characterBook.entries else characterBook.entries.take(3)
                    
                    entriesToShow.forEachIndexed { index, entry ->
                        LorebookEntryCard(
                            entry = entry,
                            index = index,
                            isSelected = selectedEntryIndex == index,
                            onSelect = { onSelectedEntryIndexChange(if (selectedEntryIndex == index) null else index) },
                            onUpdate = { updatedEntry ->
                                viewModel.updateLorebookEntry(index, updatedEntry)
                            },
                            onDelete = {
                                viewModel.removeLorebookEntry(index)
                                if (selectedEntryIndex == index) {
                                    onSelectedEntryIndexChange(null)
                                }
                            }
                        )
                    }
                    
                    if (!showAllEntries && characterBook.entries.size > 3) {
                        Text(
                            text = "... and ${characterBook.entries.size - 3} more entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Show message when no entries exist
                    Text(
                        text = "No lorebook entries yet. Click 'Add Entry' to create your first entry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LorebookEntryCard(
    entry: LorebookEntry,
    index: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (LorebookEntry) -> Unit,
    onDelete: () -> Unit
) {
    var showFullContent by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Entry Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (entry.name.isNotBlank()) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Entry ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = "Keys: ${entry.keys.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(
                        onClick = { showFullContent = !showFullContent }
                    ) {
                        Icon(
                            imageVector = if (showFullContent) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showFullContent) "Show less" else "Show more"
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Entry Content
            if (showFullContent) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Entry Name (editable)
                    OutlinedTextField(
                        value = entry.name,
                        onValueChange = { onUpdate(entry.copy(name = it)) },
                        label = { Text("Entry Name") },
                        placeholder = { Text("Enter entry name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Primary Keys (editable)
                    OutlinedTextField(
                        value = entry.keys.joinToString(", "),
                        onValueChange = { 
                            val keys = it.split(",").map { key -> key.trim() }.filter { key -> key.isNotEmpty() }
                            onUpdate(entry.copy(keys = keys))
                        },
                        label = { Text("Primary Keys (comma-separated)") },
                        placeholder = { Text("keyword1, keyword2, keyword3") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Secondary Keys (editable)
                    OutlinedTextField(
                        value = entry.secondaryKeys.joinToString(", "),
                        onValueChange = { 
                            val keys = it.split(",").map { key -> key.trim() }.filter { key -> key.isNotEmpty() }
                            onUpdate(entry.copy(secondaryKeys = keys))
                        },
                        label = { Text("Secondary Keys (comma-separated)") },
                        placeholder = { Text("secondary1, secondary2") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Entry Content (editable)
                    OutlinedTextField(
                        value = entry.content,
                        onValueChange = { onUpdate(entry.copy(content = it)) },
                        label = { Text("Entry Content") },
                        placeholder = { Text("Enter the content for this lorebook entry...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8
                    )
                    
                    // Entry Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enabled",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = entry.enabled,
                            onCheckedChange = { onUpdate(entry.copy(enabled = it)) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Case Sensitive",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = entry.caseSensitive,
                            onCheckedChange = { onUpdate(entry.copy(caseSensitive = it)) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selective",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = entry.selective,
                            onCheckedChange = { onUpdate(entry.copy(selective = it)) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Constant",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = entry.constant,
                            onCheckedChange = { onUpdate(entry.copy(constant = it)) }
                        )
                    }
                    
                    // Priority and Insertion Order
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = entry.priority.toString(),
                            onValueChange = { value ->
                                try {
                                    onUpdate(entry.copy(priority = value.toInt()))
                                } catch (e: NumberFormatException) {
                                    // Handle invalid input
                                }
                            },
                            label = { Text("Priority") },
                            placeholder = { Text("100") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = entry.insertionOrder.toString(),
                            onValueChange = { value ->
                                try {
                                    onUpdate(entry.copy(insertionOrder = value.toInt()))
                                } catch (e: NumberFormatException) {
                                    // Handle invalid input
                                }
                            },
                            label = { Text("Insertion Order") },
                            placeholder = { Text("100") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    // Position Selection
                    OutlinedTextField(
                        value = entry.position,
                        onValueChange = { onUpdate(entry.copy(position = it)) },
                        label = { Text("Position") },
                        placeholder = { Text("before_char, after_char, top, bottom, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Comment
                    OutlinedTextField(
                        value = entry.comment,
                        onValueChange = { onUpdate(entry.copy(comment = it)) },
                        label = { Text("Comment") },
                        placeholder = { Text("Optional comment for this entry") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            } else {
                // Show preview content
                Text(
                    text = entry.content.take(150) + if (entry.content.length > 150) "..." else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ImportCharacterDialog(
    onDismiss: () -> Unit,
    onImportFromFile: () -> Unit,
    onImportFromUrl: (String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var showUrlInput by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Character Card") },
        text = {
            Column {
                if (!showUrlInput) {
                    Text("Choose how to import your SillyTavern character card:")
                } else {
                    Text("Enter the URL to the character card:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("Character Card URL") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            if (!showUrlInput) {
                Row {
                    TextButton(onClick = onImportFromFile) {
                        Text("Import File")
                    }
                    TextButton(onClick = { showUrlInput = true }) {
                        Text("Import URL")
                    }
                }
            } else {
                TextButton(
                    onClick = { onImportFromUrl(urlText) },
                    enabled = urlText.isNotBlank()
                ) {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 
package com.vortexai.android.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vortexai.android.data.models.MessageSenderType
import com.vortexai.android.ui.screens.chat.components.*
import com.vortexai.android.ui.screens.chat.dialogs.CharacterDetailsDialog
import com.vortexai.android.ui.screens.chat.dialogs.DeleteChatConfirmationDialog
import com.vortexai.android.ui.screens.chat.dialogs.ChatBackgroundSettingsDialog
import com.vortexai.android.ui.screens.chat.dialogs.ReplicateDebugDialog
import com.vortexai.android.ui.theme.VortexAndroidTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    characterId: String? = null,
    conversationId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToImageSettings: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var showCharacterDetails by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val isTTSEnabled by viewModel.autoPlayTts.collectAsStateWithLifecycle()
    val bubbleStyle by viewModel.chatBubbleStyle.collectAsStateWithLifecycle()
    val showCharacterBackground by viewModel.showCharacterBackground.collectAsStateWithLifecycle()
    val characterBackgroundOpacity by viewModel.characterBackgroundOpacity.collectAsStateWithLifecycle()
    val isVortexModeEnabled by viewModel.vortexModeEnabled.collectAsStateWithLifecycle()
    var isSTTEnabled by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showBackgroundSettings by remember { mutableStateOf(false) }
    var showReplicateDebugDialog by remember { mutableStateOf(false) }
    var pendingNavigateToImageSettings by remember { mutableStateOf(false) }
    var showImageSettingsDialog by remember { mutableStateOf(false) }
    var showGenerationLogs by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Update messageText when AI generates a prompt
    LaunchedEffect(uiState.generatedUserPrompt) {
        uiState.generatedUserPrompt?.let { prompt ->
            messageText = prompt
            viewModel.clearGeneratedPrompt()
        }
    }

    LaunchedEffect(characterId, conversationId) {
        when {
            conversationId != null -> {
                viewModel.resumeConversation(conversationId)
            }
            characterId != null -> {
                viewModel.initializeChat(characterId)
            }
        }
    }

    // If user requested Image Settings before a conversation existed, navigate once it's created
    LaunchedEffect(uiState.conversation?.id, pendingNavigateToImageSettings) {
        val currentId = uiState.conversation?.id ?: conversationId
        if (pendingNavigateToImageSettings && !currentId.isNullOrBlank()) {
            onNavigateToImageSettings(currentId)
            pendingNavigateToImageSettings = false
        }
    }
    
    // Monitor lifecycle events to refresh messages when screen becomes active
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Refresh messages when screen becomes active
                    viewModel.refreshMessages()
                    viewModel.checkForPendingResponses()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Refresh messages when screen becomes active
    LaunchedEffect(Unit) {
        // Refresh messages from database to ensure we have the latest state
        viewModel.refreshMessages()
        // Check for any pending character responses
        viewModel.checkForPendingResponses()
        // Check for completed image generations
        viewModel.checkForCompletedImageGenerations()
    }
    
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    LaunchedEffect(isTTSEnabled) {
        if (!isTTSEnabled) {
            // Stop any playing TTS when disabled
            viewModel.stopTTS()
        }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.id, isTTSEnabled) {
        android.util.Log.d("ChatScreen", "LaunchedEffect triggered - messages: ${uiState.messages.size}, TTS enabled: $isTTSEnabled")
        if (isTTSEnabled && uiState.messages.isNotEmpty()) {
            val last = uiState.messages.last()
            android.util.Log.d("ChatScreen", "Last message sender: ${last.senderType}, content: ${last.content.take(50)}...")
            if (last.senderType == MessageSenderType.CHARACTER) {
                android.util.Log.d("ChatScreen", "Calling speakText for character message")
                viewModel.speakText(last.content, context)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ChatTopBar(
            character = uiState.character,
            isCharacterTyping = uiState.isCharacterTyping,
            onNavigateBack = onNavigateBack,
            onCharacterClick = { showCharacterDetails = true },
            onOptionsClick = { showOptionsMenu = true }
        )
        
        Box {
            ChatOptionsMenu(
                expanded = showOptionsMenu,
                onDismiss = { showOptionsMenu = false },
                isTTSEnabled = isTTSEnabled,
                isSTTEnabled = isSTTEnabled,
                isVortexModeEnabled = isVortexModeEnabled,
                onTTSToggle = { viewModel.setAutoPlayTts(!isTTSEnabled) },
                onSTTToggle = { isSTTEnabled = !isSTTEnabled },
                onVortexModeToggle = { viewModel.setVortexModeEnabled(!isVortexModeEnabled) },
                onBackgroundSettings = { showBackgroundSettings = true },
                onImageSettings = { showImageSettingsDialog = true },
                onNewConversation = { viewModel.forceNewConversation() },
                onClearAll = { viewModel.clearAllConversationsAndStartNew() },
                onGenerationLogs = { showGenerationLogs = true },
                onDeleteChat = { showDeleteConfirmation = true }
            )
        }
        
        // Lorebook notification
        uiState.lorebookNotification?.let { notification ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(
                        onClick = { viewModel.hideLorebookNotification() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Replicate Debug Info
        uiState.replicateDebugInfo?.let { debugInfo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Replicate Debug Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(
                            onClick = { viewModel.hideReplicateDebugInfo() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = debugInfo,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Character background image
            if (showCharacterBackground && uiState.character?.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uiState.character?.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Character background",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(characterBackgroundOpacity),
                    contentScale = ContentScale.Crop
                )
            }
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading conversation...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                uiState.errorMessage != null -> {
                    val errorMessage = uiState.errorMessage
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "😕",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Failed to load conversation",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { 
                                characterId?.let { viewModel.initializeChat(it) }
                                conversationId?.let { viewModel.resumeConversation(it) }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                uiState.messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "💬",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Start a conversation",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Send a message to begin chatting with ${uiState.character?.name ?: "this character"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages) { message ->
                            MessageBubbleResponse(
                                message = message,
                                isFromUser = message.senderType == MessageSenderType.USER,
                                characterAvatarUrl = uiState.character?.avatarUrl,
                                onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                                onAnimateImage = { id -> viewModel.animateImageMessage(id) },
                                onApplyVideoAvatar = { url -> 
                                    viewModel.applyVideoAvatar(url)
                                    android.widget.Toast.makeText(context, "Avatar updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                bubbleStyle = bubbleStyle
                            )
                        }
                        
                        if (uiState.isCharacterTyping) {
                            item {
                                TypingIndicator(
                                    characterAvatarUrl = uiState.character?.avatarUrl
                                )
                            }
                        }
                        

                        
                        // Stats display - only show if character has Dynamic Stats enabled
                        if (uiState.showStats && uiState.character?.dynamicStatsEnabled == true) {
                            item {
                                StatsDisplay(
                                    stats = uiState.currentStats,
                                    onHide = { viewModel.hideStats() }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                    value = messageText,
                    onValueChange = { 
                        messageText = it
                        viewModel.updateTypingStatus(true)
                    },
                    placeholder = { Text(if (uiState.isGeneratingUserPrompt) "Generating prompt..." else "Type a message...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                    shape = RoundedCornerShape(18.dp),
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodySmall,
                    enabled = !uiState.isSending && !uiState.isGeneratingUserPrompt
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText.trim())
                                messageText = ""
                            }
                        },
                        modifier = Modifier.size(28.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    val canGeneratePrompt = !uiState.isGeneratingUserPrompt && 
                                            uiState.messages.isNotEmpty() && 
                                            uiState.messages.lastOrNull()?.senderType == MessageSenderType.CHARACTER
                    
                    FloatingActionButton(
                        onClick = {
                            if (canGeneratePrompt) {
                                viewModel.generateUserPromptFromAIResponse()
                            }
                        },
                        modifier = Modifier.size(28.dp).alpha(if (canGeneratePrompt) 1f else 0.5f),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        if (uiState.isGeneratingUserPrompt) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Generate AI prompt",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        
        // Dialogs
        if (showCharacterDetails && uiState.character != null) {
            CharacterDetailsDialog(
                character = uiState.character!!,
                onDismiss = { showCharacterDetails = false }
            )
        }
        
        if (showDeleteConfirmation) {
            DeleteChatConfirmationDialog(
                onConfirm = {
                    showDeleteConfirmation = false
                    onNavigateBack()
                },
                onDismiss = { showDeleteConfirmation = false }
            )
        }
        

        
        // Background Settings Dialog
        if (showBackgroundSettings) {
            ChatBackgroundSettingsDialog(
                onDismiss = { showBackgroundSettings = false },
                viewModel = viewModel,
                showCharacterBackground = showCharacterBackground,
                characterBackgroundOpacity = characterBackgroundOpacity
            )
        }
        
        // Replicate Debug Dialog
        if (showReplicateDebugDialog) {
            ReplicateDebugDialog(
                onDismiss = { showReplicateDebugDialog = false },
                viewModel = viewModel,
                context = context
            )
        }

        // Generation Debug Dialog
        if (showGenerationLogs) {
            com.vortexai.android.ui.screens.chat.components.GenerationDebugDialog(
                onDismiss = { showGenerationLogs = false },
                logger = viewModel.generationLogger
            )
        }

        // Image Input Settings Dialog (chat-specific)
        if (showImageSettingsDialog) {
            val chatId = uiState.conversation?.id ?: ""
            AlertDialog(
                onDismissRequest = { showImageSettingsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Image Input Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        item {
                            ChatImageSettingsDialogContent(chatId = chatId)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showImageSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
        
        // Video Generation Error Dialog
        uiState.videoGenerationError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissVideoGenerationError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Video Generation Error")
                    }
                },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissVideoGenerationError() }) {
                        Text("Dismiss")
                    }
                }
            )
        }
        
        // Vortex Image Error Dialog
        uiState.vortexImageError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissVortexImageError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vortex Mode Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissVortexImageError() }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // TTS Error Dialog
        uiState.ttsError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissTTSError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TTS Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissTTSError() }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Image Generation Error Dialog
        uiState.imageGenerationError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissImageGenerationError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Image Generation Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissImageGenerationError() }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // LLM Response Error Dialog
        uiState.llmResponseError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissLLMResponseError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Response Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissLLMResponseError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatImageSettingsDialogContent(chatId: String) {
    val vm: ChatImageSettingsViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    var cloudImageUrl by remember { mutableStateOf("") }
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    // Get current image editing provider from settings
    val settingsViewModel: com.vortexai.android.ui.screens.settings.SettingsViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(chatId) {
        vm.loadChatImageSettings(chatId)
    }

    // Launcher for picking images
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            when (uiState.inputImageOption) {
                com.vortexai.android.data.model.InputImageOption.LOCAL_IMAGE -> vm.selectLocalImage(context, it)
                com.vortexai.android.data.model.InputImageOption.CLOUD_IMAGE -> vm.uploadImageToCloud(context, it)
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Image Editing Provider Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current Image Editing Provider: ${settingsUiState.imageEditingProvider}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure providers in Settings → Image Editing. This chat uses ${settingsUiState.imageEditingProvider} for image-to-image editing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 1. Prediction Creation
        Text(
            text = "1. Prediction Creation",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Auto
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = uiState.predictionCreationMethod == com.vortexai.android.data.model.PredictionCreationMethod.AUTO,
                onClick = { vm.updatePredictionCreationMethod(com.vortexai.android.data.model.PredictionCreationMethod.AUTO) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Auto (use prompt)")
        }

        // Manual
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = uiState.predictionCreationMethod == com.vortexai.android.data.model.PredictionCreationMethod.MANUAL,
                onClick = { vm.updatePredictionCreationMethod(com.vortexai.android.data.model.PredictionCreationMethod.MANUAL) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manual prediction text")
        }

        if (uiState.predictionCreationMethod == com.vortexai.android.data.model.PredictionCreationMethod.MANUAL) {
            OutlinedTextField(
                value = uiState.manualPredictionInput ?: "",
                onValueChange = { vm.updateManualPredictionInput(it) },
                label = { Text("Manual Prediction") },
                placeholder = { Text("Prompt will be replaced by text after '/image' in chat") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )
        }

        // Show Input Image Options only for Auto prediction
        if (uiState.predictionCreationMethod == com.vortexai.android.data.model.PredictionCreationMethod.AUTO) {
            HorizontalDivider()

            // 2. Input Image Options
            Text(
                text = "2. Input Image Options",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Character Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.inputImageOption == com.vortexai.android.data.model.InputImageOption.CHARACTER_AVATAR,
                    onClick = { vm.updateInputImageOption(com.vortexai.android.data.model.InputImageOption.CHARACTER_AVATAR) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Character Avatar")
            }

            // Local Image
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.inputImageOption == com.vortexai.android.data.model.InputImageOption.LOCAL_IMAGE,
                    onClick = {
                        vm.updateInputImageOption(com.vortexai.android.data.model.InputImageOption.LOCAL_IMAGE)
                        picker.launch("image/*")
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Local Image (Base64)")
            }

            // Cloud Image
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.inputImageOption == com.vortexai.android.data.model.InputImageOption.CLOUD_IMAGE,
                    onClick = { vm.updateInputImageOption(com.vortexai.android.data.model.InputImageOption.CLOUD_IMAGE) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cloud Image (URL)")
            }

            // Cloud Image Options
            if (uiState.inputImageOption == com.vortexai.android.data.model.InputImageOption.CLOUD_IMAGE) {
                Column(
                    modifier = Modifier.padding(start = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cloudImageUrl,
                        onValueChange = { cloudImageUrl = it },
                        label = { Text("Input Image URL") },
                        placeholder = { Text("https://example.com/image.jpg") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Button(
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse & Upload to Cloud")
                    }
                }
            }

            // Show selected info
            uiState.imageInfo?.let { info ->
                Text(
                    text = "Selected: ${info.width}x${info.height}, ${info.fileSize / 1024}KB, ${info.mimeType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!uiState.cloudImageUrl.isNullOrBlank()) {
                Text(
                    text = "Cloud URL: ${uiState.cloudImageUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Save Button
        Button(
            onClick = {
                vm.saveChatSettings()
                showSaveSuccess = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Settings")
        }

        // Success message
        if (showSaveSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings saved! Ready for image generation using '/image prompt'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                showSaveSuccess = false
            }
        }
    }
}







@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    VortexAndroidTheme {
        ChatScreen(
            characterId = "1",
            conversationId = null,
            onNavigateBack = {}
        )
    }
}

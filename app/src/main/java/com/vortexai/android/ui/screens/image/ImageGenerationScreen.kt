package com.vortexai.android.ui.screens.image

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vortexai.android.ui.components.ClickableImage
import kotlin.math.sin
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import android.graphics.drawable.BitmapDrawable
import coil.Coil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImageGenerationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var promptText by remember { mutableStateOf("") }
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<GeneratedImageData?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedImageForEdit by remember { mutableStateOf<GeneratedImageData?>(null) }
    
    // Image picker launcher
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Convert URI to local path for editing
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = java.io.File(context.cacheDir, "temp_edit_${System.currentTimeMillis()}.jpg")
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            selectedImageForEdit = GeneratedImageData(
                id = "temp_edit",
                prompt = "Selected for editing",
                localPath = tempFile.absolutePath
            )
        }
    }
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    val animatedFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_float"
    )
    
    // Check for completed image generations when screen becomes active
    LaunchedEffect(Unit) {
        viewModel.checkForCompletedImageGenerations()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2D1B69).copy(alpha = 0.9f),
                        Color(0xFF0F0C29).copy(alpha = 0.95f),
                        Color(0xFF24243e).copy(alpha = 1f)
                    ),
                    radius = 1200f + animatedFloat * 200f
                )
            )
    ) {
        // Floating orbs background
        FloatingOrbs()
        
    Scaffold(
            containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                    Text(
                                text = "AI Artistry",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFE91E63),
                                            Color(0xFF9C27B0),
                                            Color(0xFF673AB7)
                                        )
                                    )
                                )
                            )
                        }
                },
                navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    when {
                        uiState.isLoading && uiState.generatedImages.isEmpty() -> {
                            CreativeLoadingState()
                        }
                        
                        uiState.errorMessage != null -> {
                            ErrorState(
                                errorMessage = uiState.errorMessage ?: "Unknown error",
                                onRetry = { viewModel.clearError() }
                            )
                        }
                        
                        uiState.generatedImages.isEmpty() -> {
                            EmptyState()
                        }
                        
                        else -> {
                            // Beautiful image gallery
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 12.dp,
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(uiState.generatedImages) { imageItem ->
                                    EnhancedImageCard(
                                        imageItem = imageItem,
                                        onClick = { selectedImage = imageItem },
                                        onSave = { ctx -> saveImageToGallery(ctx, imageItem) },
                                        onShare = { ctx -> shareImage(ctx, imageItem) },
                                        onDelete = { viewModel.removeImage(imageItem.id) }
                                    )
                                }
                                
                                // Loading placeholder while generating
                                if (uiState.isLoading) {
                                    item {
                                        GeneratingCard()
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Parameter controls section - always show so users can access it
                ParameterControlsSection(
                    parameters = uiState.availableParameters,
                    currentValues = uiState.currentParameterValues,
                    isExpanded = uiState.showParameterPanel,
                    onToggleExpand = { viewModel.toggleParameterPanel() },
                    onValueChange = { name, value -> viewModel.updateParameterValue(name, value) },
                    onResetToDefaults = { viewModel.resetParametersToDefault() }
                )
                
                // Enhanced prompt input area with edit mode
                EnhancedPromptInput(
                    promptText = promptText,
                    onPromptChange = { promptText = it },
                    onGenerate = {
                        if (promptText.isNotBlank()) {
                            if (isEditMode && selectedImageForEdit != null) {
                                viewModel.editImage(promptText.trim(), selectedImageForEdit!!.localPath!!)
                                isEditMode = false
                                selectedImageForEdit = null
                            } else {
                                viewModel.generateImage(promptText.trim())
                            }
                            promptText = ""
                        }
                    },
                    onToggleEditMode = {
                        if (isEditMode) {
                            isEditMode = false
                            selectedImageForEdit = null
                        } else {
                            imagePickerLauncher.launch("image/*")
                            isEditMode = true
                        }
                    },
                    isLoading = uiState.isLoading,
                    isEditMode = isEditMode,
                    selectedImageForEdit = selectedImageForEdit
                )
            }
        }
    }

    // Full-screen viewer outside of LazyGrid scope
    selectedImage?.let { img ->
        FullScreenImageViewer(
            imageItem = img,
            onClose = { selectedImage = null },
            onSave = { ctx -> saveImageToGallery(ctx, img) },
            onShare = { ctx -> shareImage(ctx, img) }
        )
    }
}

@Composable
private fun FloatingOrbs() {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    
    // Multiple floating orbs with different animations
    repeat(6) { index ->
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 50f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000 + index * 500,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_$index"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000 + index * 300,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_alpha_$index"
        )
        
        Box(
            modifier = Modifier
                .offset(
                    x = (50 + index * 60).dp,
                    y = (100 + index * 80 + offsetY).dp
                )
                .size((40 + index * 20).dp)
                .background(
                    Color(0xFFE91E63).copy(alpha = alpha),
                    CircleShape
                )
                .blur(radius = 20.dp)
        )
    }
}

@Composable
private fun CreativeLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated creative icon
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing)
                ),
                label = "rotation"
            )
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(rotationZ = rotation)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE91E63),
                                Color(0xFF9C27B0),
                                Color(0xFF673AB7)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Creating Magic...",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your imagination is becoming reality",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF5722)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Try Again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated sparkle icon
            val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale),
                tint = Color(0xFFE91E63)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Unleash Your Creativity",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE91E63),
                            Color(0xFF9C27B0)
                        )
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Transform your wildest ideas into stunning visual art with the power of AI",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun EnhancedImageCard(
    imageItem: GeneratedImageData,
    onClick: () -> Unit,
    onSave: suspend (Context) -> Unit,
    onShare: (Context) -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { isMenuExpanded = true }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            Column {
                // Image with overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 300.dp)
                        .clickable { onClick() }
                ) {
                    ClickableImage(
                        imageUrl = imageItem.imageUrl,
                        imageBase64 = imageItem.imageBase64,
                        localPath = imageItem.localPath,
                        contentDescription = imageItem.prompt,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )
                }
                
                // Content area
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = imageItem.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Metadata row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFE91E63)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${imageItem.generationTime}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            // Dropdown menu
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier.background(
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(12.dp)
                )
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save", color = Color.White)
                        }
                    },
                    onClick = {
                        coroutineScope.launch { onSave(ctx) }
                        isMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share", color = Color.White)
                        }
                    },
                    onClick = {
                        onShare(ctx)
                        isMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete", color = Color(0xFFFF5722))
                        }
                    },
                    onClick = {
                        onDelete()
                        isMenuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GeneratingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFE91E63),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Creating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
} 

@Composable
private fun EnhancedPromptInput(
    promptText: String,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onToggleEditMode: () -> Unit,
    isLoading: Boolean,
    isEditMode: Boolean,
    selectedImageForEdit: GeneratedImageData?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Edit mode indicator
            if (isEditMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE91E63).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Image Edit Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (selectedImageForEdit != null) {
                                Text(
                                    text = "Image selected - describe your changes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            } else {
                                Text(
                                    text = "Select an image to edit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (selectedImageForEdit != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                ClickableImage(
                                    imageUrl = null,
                                    imageBase64 = null,
                                    localPath = selectedImageForEdit.localPath,
                                    contentDescription = "Selected image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = onPromptChange,
                    placeholder = {
                        Text(
                            if (isEditMode) "Describe the changes you want..." else "Describe your vision...",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 4,
                    enabled = !isLoading && (!isEditMode || selectedImageForEdit != null),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isEditMode) Color(0xFFE91E63) else Color(0xFFE91E63),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFE91E63)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Edit mode toggle button
                IconButton(
                    onClick = onToggleEditMode,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isEditMode) Color(0xFFE91E63) else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isEditMode) "Exit edit mode" else "Enter edit mode",
                        tint = if (isEditMode) Color.White else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Animated generate button
                val scale by animateFloatAsState(
                    targetValue = if (isLoading) 0.9f else 1f,
                    animationSpec = tween(150),
                    label = "button_scale"
                )
                
                FloatingActionButton(
                    onClick = onGenerate,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(scale),
                    containerColor = if (isEditMode) Color(0xFF9C27B0) else Color(0xFFE91E63),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.AutoFixHigh else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (isEditMode) "Edit Image" else "Generate",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenImageViewer(
    imageItem: GeneratedImageData,
    onClose: () -> Unit,
    onSave: suspend (Context) -> Unit,
    onShare: (Context) -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onClose() }
        ) {
            ClickableImage(
                imageUrl = imageItem.imageUrl,
                imageBase64 = imageItem.imageBase64,
                localPath = imageItem.localPath,
                contentDescription = imageItem.prompt,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// Util helpers inside same file (for brevity)
private suspend fun saveImageToGallery(context: Context, image: GeneratedImageData) {
    withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val filename = "VortexAI_${System.currentTimeMillis()}.jpg"
            val imageCollection = android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val uri = resolver.insert(imageCollection, contentValues) ?: return@withContext

            resolver.openOutputStream(uri)?.use { outStream ->
                if (image.localPath != null && java.io.File(image.localPath).exists()) {
                    // Copy file directly
                    java.io.File(image.localPath).inputStream().copyTo(outStream)
                } else {
                    // Download via Coil
                    val loader = coil.Coil.imageLoader(context)
                    val dataSrc = image.imageUrl ?: image.imageBase64 ?: return@withContext
                    val request = ImageRequest.Builder(context).data(dataSrc).build()
                    val bitmap = loader.execute(request).drawable?.toBitmap()
                    bitmap?.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outStream)
                }
            }

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Image saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageSave", "Error saving image", e)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Failed to save image", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun shareImage(context: Context, image: GeneratedImageData) {
    // Launch in IO scope to avoid blocking main thread
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val loader = coil.Coil.imageLoader(context)
            val request = ImageRequest.Builder(context).data(image.imageUrl ?: image.localPath ?: image.imageBase64).build()
            val bitmap = loader.execute(request).drawable?.toBitmap() ?: return@launch

            val cachePath = java.io.File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "share_${System.currentTimeMillis()}.png")
            val out = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            out.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )

            // Switch back to main thread for UI operations
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Image"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageShare", "Error sharing image", e)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Failed to share image", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class GeneratedImageData(
    val id: String,
    val prompt: String,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val model: String? = null,
    val generationTime: Long = 0,
    val size: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val localPath: String? = null
)

@Composable
private fun ParameterControlsSection(
    parameters: List<com.vortexai.android.data.models.CustomApiParameter>,
    currentValues: Map<String, Any>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onValueChange: (String, Any) -> Unit,
    onResetToDefaults: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advanced Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (parameters.isNotEmpty() && isExpanded) {
                        IconButton(
                            onClick = onResetToDefaults,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Expanded content with parameters
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded
            ) {
                if (parameters.isEmpty()) {
                    Text(
                        text = "No parameters available. Make sure you have a Custom API provider configured.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                } else {
                    // Scrollable container with max height
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp) // Limit height
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        parameters.forEach { param ->
                            DynamicParameterField(
                                parameter = param,
                                currentValue = currentValues[param.paramName],
                                onValueChange = { newValue ->
                                    onValueChange(param.paramName, newValue)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicParameterField(
    parameter: com.vortexai.android.data.models.CustomApiParameter,
    currentValue: Any?,
    onValueChange: (Any) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = parameter.paramName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (parameter.isRequired) {
                Surface(
                    color = Color(0xFFE91E63).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        parameter.description?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Render appropriate control based on parameter type
        when (parameter.paramType) {
            com.vortexai.android.data.models.ParameterType.INTEGER,
            com.vortexai.android.data.models.ParameterType.FLOAT -> {
                val value = (currentValue?.toString()?.toFloatOrNull() ?: parameter.defaultValue?.toFloatOrNull() ?: 0f)
                val min = parameter.minValue?.toFloatOrNull() ?: 0f
                val max = parameter.maxValue?.toFloatOrNull() ?: 100f
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = min.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                        Text(
                            text = max.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    Slider(
                        value = value,
                        onValueChange = { newValue ->
                            onValueChange(if (parameter.paramType == com.vortexai.android.data.models.ParameterType.INTEGER) newValue.toInt() else newValue)
                        },
                        valueRange = min..max,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE91E63),
                            activeTrackColor = Color(0xFFE91E63),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            
            com.vortexai.android.data.models.ParameterType.BOOLEAN -> {
                val checked = currentValue as? Boolean ?: parameter.defaultValue?.toBoolean() ?: false
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (checked) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Switch(
                        checked = checked,
                        onCheckedChange = { onValueChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFE91E63),
                            checkedTrackColor = Color(0xFFE91E63).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            
            com.vortexai.android.data.models.ParameterType.STRING -> {
                val textValue = currentValue?.toString() ?: parameter.defaultValue ?: ""
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { onValueChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE91E63),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        cursorColor = Color(0xFFE91E63)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            else -> {
                // For ARRAY and OBJECT, show as text input
                val textValue = currentValue?.toString() ?: parameter.defaultValue ?: ""
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { onValueChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE91E63),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        cursorColor = Color(0xFFE91E63)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        }
    }
}
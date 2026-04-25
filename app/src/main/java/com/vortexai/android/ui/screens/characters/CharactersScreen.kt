package com.vortexai.android.ui.screens.characters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vortexai.android.data.models.Character
import com.vortexai.android.ui.theme.VortexAndroidTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Warning
import com.vortexai.android.utils.NSFWBlurredCharacterImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    initialSearchQuery: String? = null,
    onCharacterClick: (Character) -> Unit,
    onCreateCharacter: () -> Unit,
    onEditCharacter: (Character) -> Unit,
    viewModel: CharactersViewModel = hiltViewModel(),
    settingsViewModel: com.vortexai.android.ui.screens.settings.SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf(initialSearchQuery ?: "") }
    var showSearchBar by remember { mutableStateOf(initialSearchQuery != null) }
    var isInitialized by remember { mutableStateOf(false) }
    
    // Refresh characters when screen becomes visible
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            isInitialized = true
            if (initialSearchQuery != null) {
                viewModel.searchCharacters(initialSearchQuery)
            } else {
                viewModel.loadCharacters(refresh = true)
            }
        }
    }
    
    // Add refresh capability via pull-to-refresh gesture
    val refreshing = uiState.isLoading && uiState.characters.isNotEmpty()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Handle pull-to-refresh
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh()
        }
    }
    
    LaunchedEffect(refreshing) {
        if (!refreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Characters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showSearchBar = !showSearchBar },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Search Bar
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchCharacters(it)
                    },
                    label = { Text("Search characters...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
            
            // Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(listOf("All", "Favorites", "My Characters", "Popular")) { filter ->
                    FilterChip(
                        onClick = { viewModel.applyFilter(filter) },
                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                        selected = uiState.selectedFilter == filter,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.characters.isEmpty() -> {
                        // Loading state
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
                                    text = "Loading characters...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    uiState.errorMessage != null -> {
                        // Error state
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
                                    text = "Failed to load characters",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = errorMessage ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadCharacters() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    
                    uiState.characters.isEmpty() -> {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎭",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No characters found",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Create your first character to get started",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onCreateCharacter) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Character")
                                }
                            }
                        }
                    }
                    
                    else -> {
                        // Characters grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.characters) { character ->
                                CharacterCard(
                                    character = character,
                                    onClick = { 
                                        // When clicking a character, it will resume existing chat or create new one
                                        onCharacterClick(character) 
                                    },
                                    onFavoriteClick = { viewModel.toggleFavorite(character.id, !character.isFavorite) },
                                    onDeleteClick = { viewModel.deleteCharacter(character.id) },
                                    onEditClick = { onEditCharacter(character) },
                                    nsfwBlurEnabled = settingsUiState.nsfwBlurEnabled,
                                    nsfwWarningEnabled = settingsUiState.nsfwWarningEnabled
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = onCreateCharacter,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Character"
            )
        }
        
        // Pull to refresh container
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    nsfwBlurEnabled: Boolean,
    nsfwWarningEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDetailsDialog = true }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column {
            // Character Avatar with NSFW handling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (character.nsfwEnabled) {
                    // Use NSFW blurred image for NSFW characters
                    NSFWBlurredCharacterImage(
                        imageUrl = character.avatarUrl,
                        videoUrl = character.avatarVideoUrl,
                        characterName = character.name,
                        isNsfw = character.nsfwEnabled,
                        onImageClick = onClick,
                        nsfwBlurEnabled = nsfwBlurEnabled,
                        nsfwWarningEnabled = nsfwWarningEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Regular image display for non-NSFW characters
                    com.vortexai.android.ui.components.VideoAvatar(
                        imageUrl = character.avatarUrl,
                        videoUrl = character.avatarVideoUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Action buttons overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Favorite button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                CircleShape
                            )
                    ) {
                                                 Icon(
                             imageVector = if (character.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                             contentDescription = "Toggle favorite",
                             tint = if (character.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                         )
                    }
                    
                    // More options button
                    IconButton(
                        onClick = { showDetailsDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Character Info
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                // NSFW indicator
                if (character.nsfwEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "NSFW content",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NSFW Content",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    
    // Details dialog
    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Character Options") },
            text = { Text("Choose an action for ${character.name}") },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            showDetailsDialog = false
                            onEditClick()
                        }
                    ) {
                        Text("Edit Character")
                    }
                    TextButton(
                        onClick = {
                            showDetailsDialog = false
                            showDeleteDialog = true
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Character")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Character") },
            text = { Text("Are you sure you want to delete ${character.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CharactersScreenPreview() {
    VortexAndroidTheme {
        CharactersScreen(
            onCharacterClick = {},
            onCreateCharacter = {},
            onEditCharacter = {}
        )
    }
} 
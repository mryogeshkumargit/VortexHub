package com.vortexai.android.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vortexai.android.data.models.Character
import com.vortexai.android.data.models.Conversation
import com.vortexai.android.utils.NSFWBlurredCharacterImage
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToCharacters: (String?) -> Unit,
    onNavigateToImageGeneration: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: com.vortexai.android.ui.screens.settings.SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    
    var characterToDelete by remember { mutableStateOf<Character?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compact header
        item {
            AnimatedVortexTitle()
        }

        // #1 — Featured Character Spotlight
        if (uiState.featuredCharacters.isNotEmpty()) {
            item {
                FeaturedCharacterSpotlight(
                    character = uiState.featuredCharacters.first(),
                    onChatClick = { onNavigateToChat(it.id) },
                    nsfwBlurEnabled = settingsUiState.nsfwBlurEnabled
                )
            }
        }
        
        // Quick Actions
        item {
            QuickActionsSection(
                onNavigateToChat = onNavigateToChat,
                onNavigateToCharacters = { onNavigateToCharacters(null) },
                onNavigateToImageGeneration = onNavigateToImageGeneration
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // #4 — Continue Where You Left Off
        if (uiState.recentChats.isNotEmpty()) {
            item {
                ContinueWhereYouLeftOff(
                    conversations = uiState.recentChats,
                    lastMessages = uiState.lastMessagePreviews,
                    characterAvatars = uiState.characterAvatars,
                    onChatClick = onNavigateToChat
                )
            }
        }
        
        // #3 — User Stats Card
        if (uiState.userStats.totalMessages > 0 || uiState.userStats.totalCharacters > 0) {
            item {
                UserStatsCard(stats = uiState.userStats)
            }
        }
        
        // #7 — Daily Prompt
        if (uiState.dailyPrompt != null) {
            item {
                DailyPromptCard(
                    prompt = uiState.dailyPrompt!!,
                    onClick = { onNavigateToChat(uiState.dailyPrompt!!.characterId) }
                )
            }
        }
        
        // #2 — Category Discovery
        if (uiState.categories.isNotEmpty()) {
            item {
                CategoryDiscoveryRow(
                    categories = uiState.categories,
                    onCategoryClick = { category ->
                        onNavigateToCharacters(category)
                    }
                )
            }
        }
        
        // #5 — New & Trending
        if (uiState.newCharacters.isNotEmpty()) {
            item {
                NewAndTrendingSection(
                    characters = uiState.newCharacters,
                    onCharacterClick = { onNavigateToChat(it.id) },
                    nsfwBlurEnabled = settingsUiState.nsfwBlurEnabled,
                    nsfwWarningEnabled = settingsUiState.nsfwWarningEnabled
                )
            }
        }
        
        // #8 — Video Avatar Showcase
        if (uiState.videoAvatarCharacters.isNotEmpty()) {
            item {
                VideoAvatarShowcase(
                    characters = uiState.videoAvatarCharacters,
                    onCharacterClick = { onNavigateToChat(it.id) },
                    nsfwBlurEnabled = settingsUiState.nsfwBlurEnabled
                )
            }
        }
        
        // Popular Characters
        if (uiState.popularCharacters.isNotEmpty()) {
            item {
                PopularCharactersSection(
                    characters = uiState.popularCharacters,
                    onCharacterClick = { onNavigateToChat(it.id) },
                    onCharacterLongPress = { character ->
                        characterToDelete = character
                        showDeleteDialog = true
                    },
                    nsfwBlurEnabled = settingsUiState.nsfwBlurEnabled,
                    nsfwWarningEnabled = settingsUiState.nsfwWarningEnabled
                )
            }
        }
        
        // Removed extra spacer item as it's handled by contentPadding bottom
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && characterToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                characterToDelete = null
            },
            title = { Text("Delete Character", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Are you sure you want to delete \"${characterToDelete?.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        characterToDelete?.let { viewModel.deleteCharacter(it) }
                        showDeleteDialog = false
                        characterToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    characterToDelete = null
                }) { Text("Cancel") }
            },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
    }
    
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            delay(3000)
            viewModel.clearError()
        }
    }
}

// ─── Animated Title ──────────────────────────

@Composable
private fun AnimatedVortexTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "vortex_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Text(
        text = "VortexAI",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            brush = Brush.linearGradient(listOf(Color(0xFF6200EE), Color(0xFF03DAC6)))
        ),
        modifier = Modifier.scale(scale)
    )
}

// ─── #1 Featured Character Spotlight ─────────

@Composable
private fun FeaturedCharacterSpotlight(
    character: Character,
    onChatClick: (Character) -> Unit,
    nsfwBlurEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable { onChatClick(character) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(character.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = character.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Featured",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF03DAC6),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                character.shortDescription?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Chat now chip
            AssistChip(
                onClick = { onChatClick(character) },
                label = { Text("Chat Now", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.Chat, null, Modifier.size(14.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ─── Quick Actions ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSection(
    onNavigateToChat: (String) -> Unit,
    onNavigateToCharacters: () -> Unit,
    onNavigateToImageGeneration: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionCard(Icons.Default.People, "Characters", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onNavigateToCharacters, Modifier.weight(1f))
        ActionCard(Icons.Default.Chat, "New Chat", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onNavigateToCharacters, Modifier.weight(1f))
        ActionCard(Icons.Default.Image, "Images", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, onNavigateToImageGeneration, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, title, Modifier.size(24.dp), tint = contentColor)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── #4 Continue Where You Left Off ──────────

@Composable
private fun ContinueWhereYouLeftOff(
    conversations: List<Conversation>,
    lastMessages: Map<String, String>,
    characterAvatars: Map<String, String?>,
    onChatClick: (String) -> Unit
) {
    Column {
        Text("Continue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(conversations) { conversation ->
                ContinueChatCard(
                    conversation = conversation,
                    lastMessage = lastMessages[conversation.id],
                    avatarUrl = characterAvatars[conversation.characterId],
                    onClick = { onChatClick(conversation.characterId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinueChatCard(
    conversation: Conversation,
    lastMessage: String?,
    avatarUrl: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Character avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = conversation.characterName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.characterName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lastMessage ?: "${conversation.totalMessages} messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                "Continue",
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─── #3 User Stats Card ─────────────────────

@Composable
private fun UserStatsCard(stats: UserStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(Icons.Default.Chat, "${stats.totalConversations}", "Chats")
            VerticalDivider(modifier = Modifier.height(32.dp))
            StatItem(Icons.Default.TextSnippet, "${stats.totalMessages}", "Messages")
            VerticalDivider(modifier = Modifier.height(32.dp))
            StatItem(Icons.Default.People, "${stats.totalCharacters}", "Characters")
        }
        
        stats.favoriteCharacterName?.let { favName ->
            Divider(modifier = Modifier.padding(horizontal = 14.dp))
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Favorite, null, Modifier.size(12.dp), tint = Color(0xFFE91E63))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Most chatted: $favName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── #7 Daily Prompt Card ───────────────────

@Composable
private fun DailyPromptCard(prompt: DailyPrompt, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Try this", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                Text(prompt.prompt, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowForward, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

// ─── #2 Category Discovery Row ──────────────

@Composable
private fun CategoryDiscoveryRow(
    categories: List<String>,
    onCategoryClick: (String) -> Unit
) {
    Column {
        Text("Explore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { category ->
                val icon = when (category.lowercase()) {
                    "anime" -> Icons.Default.Face
                    "fantasy" -> Icons.Default.AutoAwesome
                    "roleplay" -> Icons.Default.TheaterComedy
                    "assistant" -> Icons.Default.SmartToy
                    "companion" -> Icons.Default.Favorite
                    "game" -> Icons.Default.SportsEsports
                    else -> Icons.Default.Tag
                }
                AssistChip(
                    onClick = { onCategoryClick(category) },
                    label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(icon, null, Modifier.size(14.dp)) },
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

// ─── #5 New & Trending ──────────────────────

@Composable
private fun NewAndTrendingSection(
    characters: List<Character>,
    onCharacterClick: (Character) -> Unit,
    nsfwBlurEnabled: Boolean,
    nsfwWarningEnabled: Boolean
) {
    Column {
        Text("New & Trending", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(characters) { character ->
                CompactCharacterCard(character, { onCharacterClick(character) }, nsfwBlurEnabled)
            }
        }
    }
}

// ─── #8 Video Avatar Showcase ───────────────

@Composable
private fun VideoAvatarShowcase(
    characters: List<Character>,
    onCharacterClick: (Character) -> Unit,
    nsfwBlurEnabled: Boolean
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Default.PlayCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text("Animated Avatars", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(characters) { character ->
                CompactCharacterCard(character, { onCharacterClick(character) }, nsfwBlurEnabled)
            }
        }
    }
}

// ─── Compact Character Card (shared) ────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactCharacterCard(
    character: Character,
    onClick: () -> Unit,
    nsfwBlurEnabled: Boolean
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(character.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = character.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = character.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

// ─── Popular Characters ─────────────────────

@Composable
private fun PopularCharactersSection(
    characters: List<Character>,
    onCharacterClick: (Character) -> Unit,
    onCharacterLongPress: (Character) -> Unit,
    nsfwBlurEnabled: Boolean,
    nsfwWarningEnabled: Boolean
) {
    Column {
        Text("Popular", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(characters) { character ->
                CharacterCard(character, { onCharacterClick(character) }, { onCharacterLongPress(character) }, nsfwBlurEnabled, nsfwWarningEnabled)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    nsfwBlurEnabled: Boolean,
    nsfwWarningEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                NSFWBlurredCharacterImage(
                    imageUrl = character.avatarUrl,
                    characterName = character.name,
                    isNsfw = character.nsfwEnabled,
                    onImageClick = onClick,
                    nsfwBlurEnabled = nsfwBlurEnabled,
                    nsfwWarningEnabled = nsfwWarningEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                character.shortDescription?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Chat, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = formatNumber(character.totalMessages),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun formatNumber(number: Int): String = when {
    number >= 1_000_000 -> "${(number / 1_000_000.0).format(1)}M"
    number >= 1_000 -> "${(number / 1_000.0).format(1)}K"
    else -> number.toString()
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
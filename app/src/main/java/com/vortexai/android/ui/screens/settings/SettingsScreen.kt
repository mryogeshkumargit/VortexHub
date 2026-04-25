package com.vortexai.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.vortexai.android.ui.screens.settings.tabs.*
import com.vortexai.android.ui.screens.settings.components.SettingsDialogs

data class SettingsTabItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSSLSettings: () -> Unit = {},
    onNavigateToCustomApiText: () -> Unit = {},
    onNavigateToCustomApiImage: () -> Unit = {},
    onNavigateToCustomApiEdit: () -> Unit = {},
    onCreateBackup: (String) -> Unit = {},
    onOpenBackup: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDeleteAllCharactersDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { 7 })
    val coroutineScope = rememberCoroutineScope()

    val tabItems = listOf(
        SettingsTabItem("Interface", Icons.Default.Tune),
        SettingsTabItem("LLM Config", Icons.Default.Psychology),
        SettingsTabItem("Image Gen", Icons.Default.Image),
        SettingsTabItem("Image Edit", Icons.Default.Edit),
        SettingsTabItem("Video Gen", Icons.Default.Movie),
        SettingsTabItem("Audio", Icons.Default.AudioFile),
        SettingsTabItem("Profile", Icons.Default.AccountCircle)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabItems.forEachIndexed { index, tabItem ->
                    val tooltipState = rememberTooltipState()
                    
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                        tooltip = {
                            Text(
                                text = tabItem.title,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        state = tooltipState
                    ) {
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tabItem.icon,
                                    contentDescription = tabItem.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            // Pager for tab content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // Update selected tab when pager scrolls
                if (pagerState.currentPage != selectedTabIndex) {
                    selectedTabIndex = pagerState.currentPage
                }
                
                when (page) {
                    0 -> InterfaceSettingsTab(uiState, viewModel)
                    1 -> LLMConfigurationTab(uiState, viewModel, onNavigateToCustomApiText)
                    2 -> ImageGenerationTab(uiState, viewModel, onNavigateToSSLSettings, onNavigateToCustomApiImage) { showDeleteAllCharactersDialog = true }
                    3 -> ImageEditingTab(uiState, viewModel, onNavigateToCustomApiEdit)
                    4 -> VideoGenerationTab(uiState, viewModel)
                    5 -> AudioSettingsTab(uiState, viewModel)
                    6 -> ProfileAccountTab(
                        uiState = uiState, 
                        viewModel = viewModel,
                        onShowClearDataDialog = { showClearDataDialog = true },
                        onCreateBackup = onCreateBackup,
                        onOpenBackup = onOpenBackup
                    )
                }
            }
        }
    }

    // Dialogs
    SettingsDialogs(
        showClearDataDialog = showClearDataDialog,
        onDismissClearDataDialog = { showClearDataDialog = false },
        showDeleteAllCharactersDialog = showDeleteAllCharactersDialog,
        onDismissDeleteAllCharactersDialog = { showDeleteAllCharactersDialog = false },
        deleteConfirmationText = deleteConfirmationText,
        onDeleteConfirmationTextChange = { deleteConfirmationText = it },
        viewModel = viewModel
    )
}
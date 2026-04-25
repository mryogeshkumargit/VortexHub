package com.vortexai.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vortexai.android.data.models.CustomApiParameter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelParametersScreen(
    modelId: String,
    modelName: String,
    onNavigateBack: () -> Unit,
    viewModel: CustomApiProviderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(modelId) {
        // Parameters are already loaded when model is selected
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$modelName Parameters") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Parameter")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.parameters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No parameters configured",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Add parameters to customize model behavior",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Parameter")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.parameters) { parameter ->
                    ParameterCard(
                        parameter = parameter,
                        onDelete = { viewModel.deleteParameter(parameter) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddParameterDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, type, default, min, max, required, desc ->
                viewModel.saveParameter(modelId, name, type, default, min, max, required, desc)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ParameterCard(
    parameter: CustomApiParameter,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = parameter.paramName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (parameter.isRequired) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Required",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Type: ${parameter.paramType.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            parameter.description?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            parameter.defaultValue?.let { default ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Default: $default",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (parameter.minValue != null || parameter.maxValue != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Range: ${parameter.minValue ?: "∞"} to ${parameter.maxValue ?: "∞"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

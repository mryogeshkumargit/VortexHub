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
import com.vortexai.android.data.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomApiProviderScreen(
    apiType: ApiProviderType,
    onNavigateBack: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    viewModel: CustomApiProviderViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEndpointDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<CustomApiProvider?>(null) }
    var editingEndpoint by remember { mutableStateOf<CustomApiEndpoint?>(null) }
    var editingModel by remember { mutableStateOf<CustomApiModel?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showServerImportDialog by remember { mutableStateOf(false) }
    var showParametersDialog by remember { mutableStateOf(false) }
    var configuringModelId by remember { mutableStateOf<String?>(null) }
    var configuringParameters by remember { mutableStateOf<List<CustomApiParameter>>(emptyList()) }
    var showTestConnectionDialog by remember { mutableStateOf(false) }
    var testingProvider by remember { mutableStateOf<CustomApiProvider?>(null) }
    var testingEndpoint by remember { mutableStateOf<CustomApiEndpoint?>(null) }
    var testingModel by remember { mutableStateOf<CustomApiModel?>(null) }
    
    LaunchedEffect(apiType) {
        viewModel.loadProviders(apiType)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getApiTypeTitle(apiType)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.Help, "Help")
                    }
                    IconButton(onClick = { showServerImportDialog = true }) {
                        Icon(Icons.Default.Cloud, "Import from Server")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileDownload, "Import")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Provider")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.providers.isEmpty()) {
            EmptyState(apiType) { showAddDialog = true }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.providers) { provider ->
                    ProviderCard(
                        provider = provider,
                        isSelected = uiState.selectedProvider?.id == provider.id,
                        onSelect = { viewModel.selectProvider(provider) },
                        onToggle = { viewModel.toggleProviderEnabled(provider) },
                        onEdit = { editingProvider = provider },
                        onDelete = { viewModel.deleteProvider(provider) }
                    )
                    
                    if (uiState.selectedProvider?.id == provider.id) {
                        ProviderDetails(
                            endpoints = uiState.endpoints,
                            models = uiState.models,
                            parameters = uiState.parameters,
                            selectedModel = uiState.selectedModel,
                            onAddEndpoint = { showEndpointDialog = true },
                            onAddModel = { showModelDialog = true },
                            onEditEndpoint = { editingEndpoint = it },
                            onDeleteEndpoint = { viewModel.deleteEndpoint(it) },
                            onSelectModel = { viewModel.selectModel(it) },
                            onEditModel = { editingModel = it },
                            onDeleteModel = { viewModel.deleteModel(it) },
                            onTestConnection = {
                                if (uiState.endpoints.isNotEmpty() && uiState.models.isNotEmpty()) {
                                    testingProvider = provider
                                    testingEndpoint = uiState.endpoints.first()
                                    testingModel = uiState.models.first()
                                    showTestConnectionDialog = true
                                }
                            },
                            onConfigureParameters = { modelId, params ->
                                configuringModelId = modelId
                                configuringParameters = params
                                showParametersDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog || editingProvider != null) {
        AddProviderDialog(
            apiType = apiType,
            existingProvider = editingProvider,
            onDismiss = { 
                showAddDialog = false
                editingProvider = null
            },
            onSave = { name, baseUrl, apiKey ->
                if (editingProvider != null) {
                    viewModel.updateProvider(editingProvider!!.copy(
                        name = name,
                        baseUrl = baseUrl,
                        apiKey = apiKey
                    ))
                } else {
                    viewModel.saveProvider(name, apiType, baseUrl, apiKey)
                }
                showAddDialog = false
                editingProvider = null
            }
        )
    }
    
    if ((showEndpointDialog || editingEndpoint != null) && uiState.selectedProvider != null) {
        AddEndpointDialog(
            apiType = apiType,
            existingEndpoint = editingEndpoint,
            onDismiss = { 
                showEndpointDialog = false
                editingEndpoint = null
            },
            onSave = { path, method, reqSchema, resSchema, purpose ->
                if (editingEndpoint != null) {
                    viewModel.updateEndpoint(editingEndpoint!!.copy(
                        endpointPath = path,
                        httpMethod = method,
                        requestSchemaJson = reqSchema,
                        responseSchemaJson = resSchema,
                        purpose = purpose
                    ))
                } else {
                    viewModel.saveEndpoint(
                        uiState.selectedProvider!!.id,
                        path, method, reqSchema, resSchema, purpose
                    )
                }
                showEndpointDialog = false
                editingEndpoint = null
            }
        )
    }
    
    if ((showModelDialog || editingModel != null) && uiState.selectedProvider != null) {
        AddModelDialog(
            existingModel = editingModel,
            onDismiss = { 
                showModelDialog = false
                editingModel = null
            },
            onSave = { modelId, displayName, capabilities ->
                if (editingModel != null) {
                    viewModel.updateModel(editingModel!!.copy(
                        modelId = modelId,
                        displayName = displayName,
                        capabilitiesJson = org.json.JSONObject(capabilities).toString()
                    ))
                } else {
                    viewModel.saveModel(uiState.selectedProvider!!.id, modelId, displayName, capabilities)
                }
                showModelDialog = false
                editingModel = null
            }
        )
    }
    
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("OK")
                }
            }
        )
    }
    
    uiState.successMessage?.let { message ->
        // Only auto-clear if there's no test result to show (test result dialog has its own close button)
        if (uiState.testResult == null) {
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearMessages()
            }
        }
    }
    
    uiState.testResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("✅ Test Successful") },
            text = {
                LazyColumn {
                    when (result.type) {
                        ApiProviderType.TEXT_GENERATION -> {
                            item {
                                Text("AI Response:", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = result.textResponse ?: "No response",
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        ApiProviderType.IMAGE_GENERATION, ApiProviderType.IMAGE_EDITING -> {
                            item {
                                Text("Generated Image:", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                result.imageUrl?.let { url ->
                                    coil.compose.AsyncImage(
                                        model = url,
                                        contentDescription = "Test image",
                                        modifier = Modifier.fillMaxWidth().height(300.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("Close")
                }
            }
        )
    }
    
    if (showImportDialog) {
        ImportDialog(
            apiType = apiType,
            onDismiss = { showImportDialog = false },
            onImport = { json ->
                viewModel.importFromJson(json)
                showImportDialog = false
            }
        )
    }
    
    if (showHelpDialog) {
        HelpDialog(
            apiType = apiType,
            onDismiss = { showHelpDialog = false }
        )
    }
    
    if (showServerImportDialog) {
        ServerImportDialog(
            viewModel = settingsViewModel,
            onDismiss = { showServerImportDialog = false },
            onImport = { json ->
                viewModel.importFromJson(json)
                showServerImportDialog = false
            }
        )
    }
    
    if (showParametersDialog && configuringModelId != null) {
        val parameterValuesFlow = remember(configuringModelId) {
            viewModel.getParameterValues(configuringModelId!!)
        }
        val parameterValues by parameterValuesFlow.collectAsState(initial = emptyMap())
        
        CustomApiParametersDialog(
            parameters = configuringParameters,
            initialValues = parameterValues,
            onDismiss = { showParametersDialog = false },
            onConfirm = { values ->
                viewModel.saveParameterValues(configuringModelId!!, values)
                showParametersDialog = false
            }
        )
    }
    
    if (showTestConnectionDialog && testingProvider != null && testingEndpoint != null && testingModel != null) {
        TestConnectionDialog(
            provider = testingProvider!!,
            endpoint = testingEndpoint!!,
            model = testingModel!!,
            onDismiss = { showTestConnectionDialog = false },
            onTest = { sourceImageBase64 ->
                viewModel.testConnection(testingProvider!!, testingEndpoint!!, testingModel!!, sourceImageBase64)
                showTestConnectionDialog = false
            }
        )
    }
}

@Composable
private fun EmptyState(apiType: ApiProviderType, onAdd: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Api,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No ${getApiTypeTitle(apiType)} providers",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Add a custom API provider to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Provider")
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: CustomApiProvider,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
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
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = provider.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    Switch(
                        checked = provider.isEnabled,
                        onCheckedChange = { onToggle() }
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderDetails(
    endpoints: List<CustomApiEndpoint>,
    models: List<CustomApiModel>,
    parameters: List<CustomApiParameter>,
    selectedModel: CustomApiModel?,
    onAddEndpoint: () -> Unit,
    onAddModel: () -> Unit,
    onEditEndpoint: (CustomApiEndpoint) -> Unit,
    onDeleteEndpoint: (CustomApiEndpoint) -> Unit,
    onSelectModel: (CustomApiModel) -> Unit,
    onEditModel: (CustomApiModel) -> Unit,
    onDeleteModel: (CustomApiModel) -> Unit,
    onTestConnection: () -> Unit,
    onConfigureParameters: (String, List<CustomApiParameter>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Endpoints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Endpoints", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddEndpoint) {
                    Icon(Icons.Default.Add, "Add Endpoint")
                }
            }
            
            if (endpoints.isEmpty()) {
                Text("No endpoints", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                endpoints.forEach { endpoint ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(endpoint.endpointPath, style = MaterialTheme.typography.bodyMedium)
                            Text("${endpoint.httpMethod.name} - ${endpoint.purpose}", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            IconButton(onClick = { onEditEndpoint(endpoint) }) {
                                Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onDeleteEndpoint(endpoint) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Models
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Models", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddModel) {
                    Icon(Icons.Default.Add, "Add Model")
                }
            }
            
            if (models.isEmpty()) {
                Text("No models", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                models.forEach { model ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(model.modelId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = { onSelectModel(model) }) {
                                    Icon(Icons.Default.Settings, "Configure", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { onEditModel(model) }) {
                                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { onDeleteModel(model) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        if (selectedModel?.id == model.id && parameters.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Parameters",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { onConfigureParameters(model.id, parameters) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Tune,
                                                "Configure",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    parameters.forEach { param ->
                                        Text(
                                            "${param.paramName} (${param.paramType.name}${if (param.isRequired) ", required" else ""})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (param.description != null) {
                                            Text(
                                                param.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (endpoints.isNotEmpty() && models.isNotEmpty()) {
                Button(
                    onClick = onTestConnection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }
        }
    }
}

private fun getApiTypeTitle(type: ApiProviderType): String = when (type) {
    ApiProviderType.TEXT_GENERATION -> "Text Generation APIs"
    ApiProviderType.IMAGE_GENERATION -> "Image Generation APIs"
    ApiProviderType.IMAGE_EDITING -> "Image Editing APIs"
}

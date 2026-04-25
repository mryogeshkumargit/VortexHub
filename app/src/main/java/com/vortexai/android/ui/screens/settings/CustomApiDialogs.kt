package com.vortexai.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vortexai.android.data.models.*
import com.vortexai.android.utils.SchemaTemplates
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddProviderDialog(
    apiType: ApiProviderType,
    existingProvider: CustomApiProvider? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existingProvider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(existingProvider?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(existingProvider?.apiKey ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingProvider != null) "Edit Provider" else "Add Provider") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Provider Name") },
                        placeholder = { Text("My Custom API") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()) {
                        onSave(name, baseUrl, apiKey)
                    }
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()
            ) {
                Text("Save")
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
fun AddEndpointDialog(
    apiType: ApiProviderType,
    existingEndpoint: CustomApiEndpoint? = null,
    onDismiss: () -> Unit,
    onSave: (String, HttpMethod, String, String, String) -> Unit
) {
    var endpointPath by remember { mutableStateOf(existingEndpoint?.endpointPath ?: "") }
    var httpMethod by remember { mutableStateOf(existingEndpoint?.httpMethod ?: HttpMethod.POST) }
    var requestSchema by remember { mutableStateOf(existingEndpoint?.requestSchemaJson ?: "") }
    var responseSchema by remember { mutableStateOf(existingEndpoint?.responseSchemaJson ?: "") }
    var purpose by remember { mutableStateOf(existingEndpoint?.purpose ?: "") }
    var selectedTemplate by remember { mutableStateOf<SchemaTemplates.Template?>(null) }
    
    val templates = when (apiType) {
        ApiProviderType.TEXT_GENERATION -> SchemaTemplates.getTextGenerationTemplates()
        ApiProviderType.IMAGE_GENERATION -> SchemaTemplates.getImageGenerationTemplates()
        ApiProviderType.IMAGE_EDITING -> SchemaTemplates.getImageEditingTemplates()
    }
    
    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { template ->
            if (existingEndpoint == null) {
                endpointPath = template.endpointPath
                requestSchema = template.requestSchema
                responseSchema = template.responseSchema
                purpose = when (apiType) {
                    ApiProviderType.TEXT_GENERATION -> "chat"
                    ApiProviderType.IMAGE_GENERATION -> "image_gen"
                    ApiProviderType.IMAGE_EDITING -> "image_edit"
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingEndpoint != null) "Edit Endpoint" else "Add Endpoint") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTemplate?.name ?: "Select Template",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Template (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            enabled = existingEndpoint == null
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            templates.forEach { template ->
                                DropdownMenuItem(
                                    text = { Text(template.name) },
                                    onClick = {
                                        selectedTemplate = template
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = endpointPath,
                        onValueChange = { endpointPath = it },
                        label = { Text("Endpoint Path") },
                        placeholder = { Text("/v1/chat/completions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = httpMethod.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("HTTP Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            HttpMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = {
                                        httpMethod = method
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = requestSchema,
                        onValueChange = { requestSchema = it },
                        label = { Text("Request Schema (JSON)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 6
                    )
                }
                item {
                    OutlinedTextField(
                        value = responseSchema,
                        onValueChange = { responseSchema = it },
                        label = { Text("Response Schema (JSON)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 6
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (endpointPath.isNotBlank() && requestSchema.isNotBlank() && responseSchema.isNotBlank()) {
                        onSave(endpointPath, httpMethod, requestSchema, responseSchema, purpose)
                    }
                },
                enabled = endpointPath.isNotBlank() && requestSchema.isNotBlank() && responseSchema.isNotBlank()
            ) {
                Text("Save")
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
fun AddModelDialog(
    existingModel: CustomApiModel? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Map<String, Boolean>) -> Unit
) {
    var modelId by remember { mutableStateOf(existingModel?.modelId ?: "") }
    var displayName by remember { mutableStateOf(existingModel?.displayName ?: "") }
    var supportsStreaming by remember { mutableStateOf(false) }
    var supportsVision by remember { mutableStateOf(false) }
    
    LaunchedEffect(existingModel) {
        existingModel?.let {
            try {
                val capabilities = org.json.JSONObject(it.capabilitiesJson)
                supportsStreaming = capabilities.optBoolean("streaming", false)
                supportsVision = capabilities.optBoolean("vision", false)
            } catch (e: Exception) {}
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingModel != null) "Edit Model" else "Add Model") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = modelId,
                        onValueChange = { modelId = it },
                        label = { Text("Model ID") },
                        placeholder = { Text("gpt-4, flux-dev, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("GPT-4") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Supports Streaming")
                        Switch(
                            checked = supportsStreaming,
                            onCheckedChange = { supportsStreaming = it }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Supports Vision")
                        Switch(
                            checked = supportsVision,
                            onCheckedChange = { supportsVision = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (modelId.isNotBlank() && displayName.isNotBlank()) {
                        val capabilities = mapOf(
                            "streaming" to supportsStreaming,
                            "vision" to supportsVision
                        )
                        onSave(modelId, displayName, capabilities)
                    }
                },
                enabled = modelId.isNotBlank() && displayName.isNotBlank()
            ) {
                Text("Add")
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
fun AddParameterDialog(
    onDismiss: () -> Unit,
    onSave: (String, ParameterType, String?, String?, String?, Boolean, String?) -> Unit
) {
    var paramName by remember { mutableStateOf("") }
    var paramType by remember { mutableStateOf(ParameterType.STRING) }
    var defaultValue by remember { mutableStateOf("") }
    var minValue by remember { mutableStateOf("") }
    var maxValue by remember { mutableStateOf("") }
    var isRequired by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Parameter") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = paramName,
                        onValueChange = { paramName = it },
                        label = { Text("Parameter Name") },
                        placeholder = { Text("temperature") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = paramType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ParameterType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        paramType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = defaultValue,
                        onValueChange = { defaultValue = it },
                        label = { Text("Default Value (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (paramType == ParameterType.INTEGER || paramType == ParameterType.FLOAT) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = minValue,
                                onValueChange = { minValue = it },
                                label = { Text("Min") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxValue,
                                onValueChange = { maxValue = it },
                                label = { Text("Max") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Required")
                        Switch(
                            checked = isRequired,
                            onCheckedChange = { isRequired = it }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (paramName.isNotBlank()) {
                        onSave(
                            paramName,
                            paramType,
                            defaultValue.ifBlank { null },
                            minValue.ifBlank { null },
                            maxValue.ifBlank { null },
                            isRequired,
                            description.ifBlank { null }
                        )
                    }
                },
                enabled = paramName.isNotBlank()
            ) {
                Text("Add")
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
fun ImportDialog(
    apiType: ApiProviderType,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var showTemplate by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                jsonInput = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
            } catch (e: Exception) {
                android.util.Log.e("ImportDialog", "Failed to read file", e)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Custom API") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Import a complete API configuration from JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Button(
                        onClick = { filePicker.launch("application/json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse & Select JSON File")
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTemplate = !showTemplate },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (showTemplate) "Hide" else "Show Template")
                        }
                        OutlinedButton(
                            onClick = { 
                                jsonInput = com.vortexai.android.utils.CustomApiImporter.generateTemplate(apiType)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Use Template")
                        }
                    }
                }
                if (showTemplate) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = com.vortexai.android.utils.CustomApiImporter.generateTemplate(apiType),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        label = { Text("JSON Configuration") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        maxLines = 10
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (jsonInput.isNotBlank()) {
                        onImport(jsonInput)
                    }
                },
                enabled = jsonInput.isNotBlank()
            ) {
                Text("Import")
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
fun HelpDialog(
    apiType: ApiProviderType,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom API Guide") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "How to Configure Custom APIs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Text(
                        text = "1. Add Provider",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Name: Friendly name for your API\n• Base URL: API endpoint (e.g., https://api.example.com)\n• API Key: Your authentication key",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text(
                        text = "2. Add Endpoint",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Path: API endpoint path (e.g., /v1/chat)\n• Method: HTTP method (POST, GET, etc.)\n• Request Schema: Define headers and body\n• Response Schema: Define how to extract data",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text(
                        text = "3. Request Schema",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = """
{
  "headers": {
    "Authorization": "Bearer {apiKey}",
    "Content-Type": "application/json"
  },
  "body": {
    "prompt": "{prompt}",
    "model": "{modelId}"
  }
}
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                item {
                    Text(
                        text = "4. Response Schema",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = """
{
  "dataPath": "choices[0].message.content",
  "imageUrlPath": "images[0].url",
  "errorPath": "error.message"
}
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                item {
                    Text(
                        text = "5. Placeholders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• {apiKey}: Your API key\n• {modelId}: Selected model\n• {prompt}: User prompt\n• {messages}: Chat messages\n• {temperature}: Temperature setting\n• {maxTokens}: Max tokens\n• {image}: Image data",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text(
                        text = "6. Import/Export",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Use Import button to load complete configurations\n• Templates available for each API type\n• Share configurations as JSON files",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}


@Composable
fun ServerImportDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var serverUrl by remember { mutableStateOf(uiState.importServerUrl) }
    var selectedFile by remember { mutableStateOf(uiState.importSelectedFile) }
    var availableFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    fun loadFileList() {
        if (serverUrl.isBlank()) return
        isLoadingFiles = true
        errorMessage = ""
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = if (serverUrl.endsWith("/")) "${serverUrl}list" else "$serverUrl/list"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val json = org.json.JSONObject(response.body?.string() ?: "{}")
                        val filesArray = json.getJSONArray("files")
                        availableFiles = (0 until filesArray.length()).map { filesArray.getString(it) }
                        isLoadingFiles = false
                    } else {
                        errorMessage = "Failed to load files: HTTP ${response.code}"
                        isLoadingFiles = false
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    errorMessage = "Error: ${e.message}"
                    isLoadingFiles = false
                }
            }
        }
    }
    
    fun downloadFile() {
        if (serverUrl.isBlank() || selectedFile.isBlank()) return
        isDownloading = true
        errorMessage = ""
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = if (serverUrl.endsWith("/")) "${serverUrl}$selectedFile" else "$serverUrl/$selectedFile"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) {
                        onImport(response.body?.string() ?: "")
                    } else {
                        errorMessage = "Failed: HTTP ${response.code}"
                        isDownloading = false
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    errorMessage = "Error: ${e.message}"
                    isDownloading = false
                }
            }
        }
    }
    
    if (showQrScanner) {
        com.vortexai.android.ui.components.QRCodeScanner(
            onQRCodeScanned = { scannedUrl ->
                serverUrl = scannedUrl
                showQrScanner = false
                loadFileList()
            },
            onDismiss = { showQrScanner = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Import from Server") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Import custom API configuration from file_transfer_server.py",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { showQrScanner = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Scan QR Code")
                        }
                    }
                    item {
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { 
                                    serverUrl = it
                                    viewModel.updateImportServerUrl(it)
                                },
                                label = { Text("Server URL") },
                                placeholder = { Text("http://192.168.1.100:8000") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = { loadFileList() },
                                enabled = serverUrl.isNotBlank() && !isLoadingFiles
                            ) {
                                if (isLoadingFiles) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.Refresh, "Load Files")
                                }
                            }
                        }
                    }
                    if (availableFiles.isNotEmpty()) {
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedFile,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select File") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    availableFiles.forEach { file ->
                                        DropdownMenuItem(
                                            text = { Text(file) },
                                            onClick = {
                                                selectedFile = file
                                                viewModel.updateImportSelectedFile(file)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (errorMessage.isNotBlank()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { downloadFile() },
                    enabled = selectedFile.isNotBlank() && !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Download & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun CustomApiParametersDialog(
    parameters: List<CustomApiParameter>,
    initialValues: Map<String, Any> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var parameterValues by remember(initialValues) { 
        mutableStateOf(
            parameters.associate { param ->
                param.paramName to (initialValues[param.paramName] 
                    ?: param.defaultValue 
                    ?: when (param.paramType) {
                        ParameterType.STRING -> ""
                        ParameterType.INTEGER -> 0
                        ParameterType.FLOAT -> 0f
                        ParameterType.BOOLEAN -> false
                        ParameterType.ARRAY -> "[]"
                        ParameterType.OBJECT -> "{}"
                    })
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Parameters") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Adjust parameters for this API request",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    com.vortexai.android.ui.components.DynamicParameterFields(
                        parameters = parameters,
                        values = parameterValues,
                        onValueChange = { name, value ->
                            parameterValues = parameterValues + (name to value)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parameterValues) }
            ) {
                Text("Confirm")
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
fun TestConnectionDialog(
    provider: CustomApiProvider,
    endpoint: CustomApiEndpoint,
    model: CustomApiModel,
    onDismiss: () -> Unit,
    onTest: (String?) -> Unit
) {
    var sourceImageBase64 by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                sourceImageBase64 = bytes?.let { data ->
                    android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                android.util.Log.e("TestConnectionDialog", "Failed to read image", e)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test Connection") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Test ${provider.name} connection",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Provider: ${provider.name}", style = MaterialTheme.typography.bodySmall)
                            Text("Model: ${model.displayName}", style = MaterialTheme.typography.bodySmall)
                            Text("Type: ${provider.type.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                if (provider.type == ApiProviderType.IMAGE_EDITING) {
                    item {
                        Text(
                            text = "Source Image (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upload an image to test editing, or use default test image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (sourceImageBase64 != null) "Change Image" else "Upload Image")
                        }
                    }
                    if (sourceImageBase64 != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✅ Image uploaded (${sourceImageBase64!!.length} bytes)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(onClick = { sourceImageBase64 = null }) {
                                        Icon(Icons.Default.Close, "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onTest(sourceImageBase64) }) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Test")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

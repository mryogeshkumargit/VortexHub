package com.vortexai.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomApiHelpScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom API Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Quick Start", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("1. Add Provider → Enter name, URL, API key")
                        Text("2. Add Endpoint → Select template or configure")
                        Text("3. Add Models → Enter model IDs")
                        Text("4. Test Connection → Verify setup")
                    }
                }
            }
            
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Request Schema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Placeholders:", fontWeight = FontWeight.Medium)
                        Text("• {apiKey} - Your API key", style = MaterialTheme.typography.bodySmall)
                        Text("• {modelId} - Selected model", style = MaterialTheme.typography.bodySmall)
                        Text("• {paramName} - Parameter value", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                """
                                {
                                  "headers": {
                                    "Authorization": "Bearer {apiKey}"
                                  },
                                  "body": {
                                    "model": "{modelId}",
                                    "temperature": "{temperature}"
                                  }
                                }
                                """.trimIndent(),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Response Schema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Path Format:", fontWeight = FontWeight.Medium)
                        Text("• Dot notation: object.field", style = MaterialTheme.typography.bodySmall)
                        Text("• Arrays: array[0]", style = MaterialTheme.typography.bodySmall)
                        Text("• Combined: choices[0].message.content", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                """
                                {
                                  "dataPath": "choices[0].message.content",
                                  "errorPath": "error.message",
                                  "imageUrlPath": "data[0].url"
                                }
                                """.trimIndent(),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Common Providers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        ProviderExample("OpenAI", "https://api.openai.com", "gpt-4, gpt-3.5-turbo")
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        ProviderExample("Anthropic", "https://api.anthropic.com", "claude-3-opus, claude-3-sonnet")
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        ProviderExample("Together AI", "https://api.together.xyz", "Llama-3-70b-chat-hf")
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        ProviderExample("Replicate", "https://api.replicate.com", "stability-ai/sdxl, flux-dev")
                    }
                }
            }
            
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Parameter Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        ParamType("STRING", "Text values", "\"hello world\"")
                        ParamType("INTEGER", "Whole numbers", "100, 512, 1024")
                        ParamType("FLOAT", "Decimals", "0.7, 1.5, 7.5")
                        ParamType("BOOLEAN", "True/false", "true, false")
                        ParamType("ARRAY", "Lists", "[1, 2, 3]")
                        ParamType("OBJECT", "Complex data", "{\"key\": \"value\"}")
                    }
                }
            }
            
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Troubleshooting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        
                        Text("Connection Failed:", fontWeight = FontWeight.Medium)
                        Text("• Check Base URL (no trailing slash)", style = MaterialTheme.typography.bodySmall)
                        Text("• Verify API Key is valid", style = MaterialTheme.typography.bodySmall)
                        Text("• Ensure endpoint path is correct", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text("Invalid Schema:", fontWeight = FontWeight.Medium)
                        Text("• Validate JSON syntax", style = MaterialTheme.typography.bodySmall)
                        Text("• Check placeholder names", style = MaterialTheme.typography.bodySmall)
                        Text("• Verify path format", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderExample(name: String, url: String, models: String) {
    Column {
        Text(name, fontWeight = FontWeight.Bold)
        Text("URL: $url", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Models: $models", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ParamType(type: String, desc: String, example: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(type, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Column(Modifier.weight(1f)) {
            Text(desc, style = MaterialTheme.typography.bodySmall)
            Text("Ex: $example", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

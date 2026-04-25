package com.vortexai.android.ui.screens.chat.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vortexai.android.ui.screens.chat.ChatViewModel

@Composable
fun ReplicateDebugDialog(
    onDismiss: () -> Unit,
    viewModel: ChatViewModel,
    context: android.content.Context
) {
    var testPrompt by remember { mutableStateOf("A beautiful landscape") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Replicate Debug",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Debug the 'Invalid input parameter' error by examining the request structure or testing the actual API call.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = testPrompt,
                    onValueChange = { testPrompt = it },
                    label = { Text("Test Prompt") },
                    placeholder = { Text("Enter a prompt to test...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Text(
                    text = "Choose your debugging approach:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "📝 Show Structure: View expected input format and validation\n🚀 Test API Call: Make actual API request to identify the exact error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Results will appear in the conversation area above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.debugReplicateInput(testPrompt, context)
                        onDismiss()
                    },
                    enabled = testPrompt.isNotBlank()
                ) {
                    Text("Show Structure")
                }
                Button(
                    onClick = {
                        viewModel.testReplicateApiCall(testPrompt, context)
                        onDismiss()
                    },
                    enabled = testPrompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Test API Call")
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
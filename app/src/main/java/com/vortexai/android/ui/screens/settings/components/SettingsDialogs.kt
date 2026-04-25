package com.vortexai.android.ui.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vortexai.android.ui.screens.settings.SettingsViewModel

@Composable
fun SettingsDialogs(
    showClearDataDialog: Boolean,
    onDismissClearDataDialog: () -> Unit,
    showDeleteAllCharactersDialog: Boolean,
    onDismissDeleteAllCharactersDialog: () -> Unit,
    deleteConfirmationText: String,
    onDeleteConfirmationTextChange: (String) -> Unit,
    viewModel: SettingsViewModel
) {
    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearDataDialog,
            title = { Text("Clear All Data") },
            text = { 
                Text("This will reset all settings to default values and clear all stored data. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        onDismissClearDataDialog()
                    }
                ) {
                    Text("Clear Data")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearDataDialog) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete All Characters Dialog
    if (showDeleteAllCharactersDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteAllCharactersDialog,
            title = { 
                Text(
                    text = "⚠️ Delete All Characters",
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { 
                Column {
                    Text(
                        text = "This action will permanently delete ALL character cards and their data. This action cannot be undone!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To confirm deletion, type 'delete' in the field below:"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = onDeleteConfirmationTextChange,
                        label = { Text("Type 'delete' to confirm") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = deleteConfirmationText.isNotBlank() && deleteConfirmationText.lowercase() != "delete"
                    )
                    if (deleteConfirmationText.isNotBlank() && deleteConfirmationText.lowercase() != "delete") {
                        Text(
                            text = "❌ You must type 'delete' exactly to confirm",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllCharacters(deleteConfirmationText)
                        onDismissDeleteAllCharactersDialog()
                        onDeleteConfirmationTextChange("")
                    },
                    enabled = deleteConfirmationText.lowercase() == "delete"
                ) {
                    Text(
                        text = "Delete All Characters",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        onDismissDeleteAllCharactersDialog()
                        onDeleteConfirmationTextChange("")
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
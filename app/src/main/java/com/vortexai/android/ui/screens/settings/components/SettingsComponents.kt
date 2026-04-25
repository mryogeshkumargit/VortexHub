package com.vortexai.android.ui.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsTextFieldItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    multiline: Boolean = false,
    maxLines: Int = 1
) {
    var localValue by remember(value) { mutableStateOf(value) }
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        OutlinedTextField(
            value = localValue,
            onValueChange = { localValue = it },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = !multiline,
            maxLines = if (multiline) maxLines else 1
        )
    }
    
    LaunchedEffect(localValue) {
        if (localValue != value) {
            delay(500)
            onValueChange(localValue)
        }
    }
}

@Composable
fun SettingsPasswordFieldItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var isVisible by remember { mutableStateOf(false) }
    var localValue by remember(value) { mutableStateOf(value) }
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        OutlinedTextField(
            value = localValue,
            onValueChange = { localValue = it },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
    }
    
    LaunchedEffect(localValue) {
        if (localValue != value) {
            delay(500)
            onValueChange(localValue)
        }
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    description: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    searchable: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredOptions = if (searchable && searchQuery.isNotEmpty()) {
        options.filter { it.contains(searchQuery, ignoreCase = true) }
    } else {
        options
    }
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Box {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = {
                    IconButton(onClick = { 
                        expanded = true
                        searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Open dropdown"
                        )
                    }
                }
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                if (searchable) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    )
                }
                
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                            searchQuery = ""
                        }
                    )
                }
                
                if (filteredOptions.isEmpty() && searchQuery.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        TextButton(onClick = onClick) {
            Text(
                text = when {
                    title.contains("Clear") -> "Clear"
                    title.contains("Change") -> "Change"
                    title.contains("Logout") -> "Logout"
                    title.contains("Delete") -> "Delete"
                    else -> "Action"
                },
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsDatePickerItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var localValue by remember(value) { mutableStateOf(value) }
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        OutlinedTextField(
            value = localValue,
            onValueChange = { localValue = it },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Date picker"
                )
            }
        )
    }
    
    LaunchedEffect(localValue) {
        if (localValue != value) {
            delay(500)
            onValueChange(localValue)
        }
    }
}
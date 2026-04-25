package com.vortexai.android.ui.screens.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vortexai.android.domain.service.GenerationLogger
import com.vortexai.android.domain.service.LogEntry
import com.vortexai.android.domain.service.LogType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationDebugDialog(
    onDismiss: () -> Unit,
    logger: GenerationLogger
) {
    val logs by logger.logs.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                TopAppBar(
                    title = { Text("Generation Logs") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = { logger.clearLogs() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )

                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No generation logs recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { log ->
                            LogItemCard(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemCard(log: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        log.isError -> MaterialTheme.colorScheme.errorContainer
        log.type == LogType.REQUEST -> MaterialTheme.colorScheme.primaryContainer
        log.type == LogType.RESPONSE -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when {
        log.isError -> MaterialTheme.colorScheme.onErrorContainer
        log.type == LogType.REQUEST -> MaterialTheme.colorScheme.onPrimaryContainer
        log.type == LogType.RESPONSE -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeString = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${log.type.name} - ${log.provider}",
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = timeString,
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = contentColor
                )
            }

            if (!log.modelOrWorkflow.isNullOrBlank()) {
                Text(
                    text = "Model/Workflow: ${log.modelOrWorkflow}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (!log.endpoint.isNullOrBlank()) {
                Text(
                    text = "Endpoint: ${log.endpoint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (!log.message.isNullOrBlank()) {
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (!log.requestData.isNullOrBlank()) {
                    Text("Request Data:", fontWeight = FontWeight.SemiBold, color = contentColor, fontSize = 12.sp)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                    ) {
                        Text(
                            text = log.requestData,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = contentColor
                        )
                    }
                }
                
                if (!log.responseData.isNullOrBlank()) {
                    if (!log.requestData.isNullOrBlank()) Spacer(modifier = Modifier.height(8.dp))
                    Text("Response Data:", fontWeight = FontWeight.SemiBold, color = contentColor, fontSize = 12.sp)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                    ) {
                        Text(
                            text = log.responseData,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

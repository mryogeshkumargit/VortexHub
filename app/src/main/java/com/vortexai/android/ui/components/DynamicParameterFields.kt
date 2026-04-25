package com.vortexai.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vortexai.android.data.models.CustomApiParameter
import com.vortexai.android.data.models.ParameterType

@Composable
fun DynamicParameterFields(
    parameters: List<CustomApiParameter>,
    values: Map<String, Any>,
    onValueChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        parameters.forEach { param ->
            DynamicParameterField(
                parameter = param,
                value = values[param.paramName],
                onValueChange = { onValueChange(param.paramName, it) }
            )
        }
    }
}

@Composable
private fun DynamicParameterField(
    parameter: CustomApiParameter,
    value: Any?,
    onValueChange: (Any) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = parameter.paramName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            if (parameter.isRequired) {
                Text(
                    text = "*",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        parameter.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        when (parameter.paramType) {
            ParameterType.STRING -> {
                StringParameterField(
                    value = value?.toString() ?: parameter.defaultValue ?: "",
                    onValueChange = onValueChange
                )
            }
            ParameterType.INTEGER -> {
                IntegerParameterField(
                    value = value?.toString()?.toIntOrNull() ?: parameter.defaultValue?.toIntOrNull() ?: 0,
                    minValue = parameter.minValue?.toIntOrNull(),
                    maxValue = parameter.maxValue?.toIntOrNull(),
                    onValueChange = onValueChange
                )
            }
            ParameterType.FLOAT -> {
                FloatParameterField(
                    value = value?.toString()?.toFloatOrNull() ?: parameter.defaultValue?.toFloatOrNull() ?: 0f,
                    minValue = parameter.minValue?.toFloatOrNull(),
                    maxValue = parameter.maxValue?.toFloatOrNull(),
                    onValueChange = onValueChange
                )
            }
            ParameterType.BOOLEAN -> {
                BooleanParameterField(
                    value = when (value) {
                        is Boolean -> value
                        is String -> value.lowercase() == "true"
                        else -> parameter.defaultValue?.lowercase() == "true"
                    },
                    onValueChange = onValueChange
                )
            }
            ParameterType.ARRAY, ParameterType.OBJECT -> {
                JsonParameterField(
                    value = value?.toString() ?: parameter.defaultValue ?: "",
                    onValueChange = onValueChange
                )
            }
        }
    }
}

@Composable
private fun StringParameterField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun IntegerParameterField(
    value: Int,
    minValue: Int?,
    maxValue: Int?,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            newValue.toIntOrNull()?.let { intValue ->
                val clampedValue = when {
                    minValue != null && intValue < minValue -> minValue
                    maxValue != null && intValue > maxValue -> maxValue
                    else -> intValue
                }
                onValueChange(clampedValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = {
            val range = when {
                minValue != null && maxValue != null -> "Range: $minValue - $maxValue"
                minValue != null -> "Min: $minValue"
                maxValue != null -> "Max: $maxValue"
                else -> null
            }
            range?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    )
}

@Composable
private fun FloatParameterField(
    value: Float,
    minValue: Float?,
    maxValue: Float?,
    onValueChange: (Float) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                newValue.toFloatOrNull()?.let { floatValue ->
                    val clampedValue = when {
                        minValue != null && floatValue < minValue -> minValue
                        maxValue != null && floatValue > maxValue -> maxValue
                        else -> floatValue
                    }
                    onValueChange(clampedValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                val range = when {
                    minValue != null && maxValue != null -> "Range: $minValue - $maxValue"
                    minValue != null -> "Min: $minValue"
                    maxValue != null -> "Max: $maxValue"
                    else -> null
                }
                range?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        )
        
        if (minValue != null && maxValue != null) {
            Slider(
                value = value,
                onValueChange = { onValueChange(it) },
                valueRange = minValue..maxValue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BooleanParameterField(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (value) "Enabled" else "Disabled",
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = value,
            onCheckedChange = onValueChange
        )
    }
}

@Composable
private fun JsonParameterField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        maxLines = 5,
        placeholder = { Text("JSON format") }
    )
}

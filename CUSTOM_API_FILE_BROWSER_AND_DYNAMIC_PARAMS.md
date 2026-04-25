# Custom API Enhancements: File Browser & Dynamic Parameters

## Overview
Implemented two major enhancements to the custom API system:
1. **File Browser for JSON Import**: Browse and select JSON files instead of paste-only
2. **Dynamic Parameter Fields**: UI fields generated from custom API parameter definitions with persistent storage

## 1. File Browser for JSON Import

### Implementation
- **File**: `CustomApiDialogs.kt` - `ImportDialog`
- **Technology**: Android ActivityResultContracts.GetContent()
- **File Type**: `application/json`

### Features
- Browse device storage for JSON files
- Automatic content reading and population
- Maintains existing paste functionality
- Template generation still available

### User Flow
1. Click "Import" button in Custom API screen
2. Click "Browse & Select JSON File" button
3. System file picker opens
4. Select JSON configuration file
5. Content automatically loads into text field
6. Review and click "Import"

## 2. Dynamic Parameter Fields

### Architecture

#### Database Layer
**New Entity**: `CustomApiParameterValue`
```kotlin
@Entity(tableName = "custom_api_parameter_values")
data class CustomApiParameterValue(
    val id: String,
    val modelId: String,
    val paramName: String,
    val value: String, // JSON string for all types
    val updatedAt: Long
)
```

**Database Migration**: Version 9 → 10
- Creates `custom_api_parameter_values` table
- Foreign key to `custom_api_models` with CASCADE delete
- Indexes on `modelId` and `paramName`

#### Repository Layer
**New Methods in CustomApiProviderRepository**:
- `getParameterValuesByModel(modelId)`: Flow of parameter values
- `getParameterValue(modelId, paramName)`: Single parameter value
- `saveParameterValue(value)`: Save single value
- `saveParameterValues(values)`: Batch save
- `deleteParameterValuesByModel(modelId)`: Clear all values
- `getParameterValuesMap(modelId)`: Get as Map<String, Any>

#### UI Components

**DynamicParameterFields.kt**
- `DynamicParameterFields`: Main composable for rendering parameter list
- `DynamicParameterField`: Individual parameter field renderer
- Type-specific fields:
  - `StringParameterField`: Text input
  - `IntegerParameterField`: Number input with range validation
  - `FloatParameterField`: Decimal input with slider (if min/max defined)
  - `BooleanParameterField`: Switch toggle
  - `JsonParameterField`: Multi-line JSON input

**CustomApiParametersDialog**
- Shows all parameters for a model
- Pre-populates with saved values or defaults
- Validates required fields
- Saves to database on confirm

### Parameter Types & UI

| Type | UI Component | Features |
|------|-------------|----------|
| STRING | OutlinedTextField | Single-line text input |
| INTEGER | OutlinedTextField | Number validation, min/max range display |
| FLOAT | OutlinedTextField + Slider | Decimal validation, visual slider if range defined |
| BOOLEAN | Switch | On/off toggle with label |
| ARRAY | OutlinedTextField | Multi-line JSON array input |
| OBJECT | OutlinedTextField | Multi-line JSON object input |

### Integration Points

#### CustomApiProviderScreen
- Added "Configure" button (Tune icon) in parameters section
- Opens `CustomApiParametersDialog` when clicked
- Loads saved values from database
- Saves values back to database

#### CustomApiProviderViewModel
- `getParameterValues(modelId)`: Returns Flow<Map<String, Any>>
- `saveParameterValues(modelId, values)`: Persists to database
- Automatic type parsing (String → Boolean/Int/Float)

#### DatabaseCustomAPIProvider
- Loads saved parameter values from database
- Merges with standard request parameters (temperature, max_tokens, etc.)
- Passes to CustomApiExecutor for API request

### Data Flow

```
1. User clicks "Configure" on model parameters
   ↓
2. CustomApiProviderScreen shows CustomApiParametersDialog
   ↓
3. Dialog loads saved values from database via ViewModel
   ↓
4. User adjusts parameter values in dynamic UI fields
   ↓
5. User clicks "Confirm"
   ↓
6. ViewModel saves values to custom_api_parameter_values table
   ↓
7. When API request is made:
   - DatabaseCustomAPIProvider loads parameter values
   - Merges with request parameters
   - CustomApiExecutor builds request body
   - API call includes custom parameter values
```

### Persistence

**Storage**: Room database table `custom_api_parameter_values`
**Scope**: Per-model configuration
**Lifecycle**: 
- Created when user configures parameters
- Updated on each save
- Deleted when model is deleted (CASCADE)
- Survives app restarts

### Example Usage

#### Define Parameters (via JSON import or UI)
```json
{
  "models": [{
    "modelId": "flux-pro",
    "parameters": [
      {
        "paramName": "guidance_scale",
        "paramType": "FLOAT",
        "defaultValue": "7.5",
        "minValue": "1.0",
        "maxValue": "20.0",
        "isRequired": false,
        "description": "How closely to follow the prompt"
      },
      {
        "paramName": "num_inference_steps",
        "paramType": "INTEGER",
        "defaultValue": "50",
        "minValue": "1",
        "maxValue": "150",
        "isRequired": true,
        "description": "Number of denoising steps"
      }
    ]
  }]
}
```

#### Configure Values (UI)
1. Navigate to Custom API → Select Provider → Select Model
2. Click "Configure" button (Tune icon)
3. Adjust sliders/inputs:
   - guidance_scale: 12.0 (slider)
   - num_inference_steps: 75 (text input)
4. Click "Confirm"

#### Runtime Usage
When API request is made:
```kotlin
// Automatic merge in DatabaseCustomAPIProvider
val requestParams = mutableMapOf(
    "messages" to messages,
    "temperature" to 0.7,
    "max_tokens" to 2048
)
// Saved values automatically added:
// "guidance_scale" to 12.0
// "num_inference_steps" to 75
```

## Benefits

### File Browser
- ✅ Easier file selection (no copy-paste needed)
- ✅ Works with cloud storage (Google Drive, Dropbox)
- ✅ Reduces user errors from manual copying
- ✅ Maintains backward compatibility with paste

### Dynamic Parameters
- ✅ Type-safe parameter input
- ✅ Visual validation (sliders, ranges)
- ✅ Persistent configuration per model
- ✅ No code changes needed for new parameters
- ✅ Automatic integration with API requests
- ✅ User-friendly UI for complex configurations

## Files Modified

### New Files
1. `CustomApiParameterValue.kt` - Entity for parameter values
2. `DynamicParameterFields.kt` - Dynamic UI components

### Modified Files
1. `CustomApiDialogs.kt` - Added file picker to ImportDialog, added CustomApiParametersDialog
2. `CustomApiProviderDao.kt` - Added parameter value queries
3. `CustomApiProviderRepository.kt` - Added parameter value operations
4. `VortexDatabase.kt` - Added entity and migration 9→10
5. `CustomApiProviderScreen.kt` - Added configure button and dialog
6. `CustomApiProviderViewModel.kt` - Added parameter value methods
7. `DatabaseCustomAPIProvider.kt` - Integrated parameter values into requests

## Testing Checklist

- [ ] Import JSON via file browser
- [ ] Import JSON via paste (backward compatibility)
- [ ] Configure STRING parameter
- [ ] Configure INTEGER parameter with range
- [ ] Configure FLOAT parameter with slider
- [ ] Configure BOOLEAN parameter
- [ ] Configure required vs optional parameters
- [ ] Save parameter values
- [ ] Load saved parameter values on reopen
- [ ] Verify parameters included in API request
- [ ] Delete model (verify CASCADE delete of values)
- [ ] App restart (verify persistence)

## Future Enhancements

1. **Parameter Presets**: Save/load parameter configurations
2. **Parameter Validation**: Real-time validation feedback
3. **Parameter Groups**: Organize related parameters
4. **Parameter History**: Track parameter changes over time
5. **Export Parameters**: Share parameter configurations

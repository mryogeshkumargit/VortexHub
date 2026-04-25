package com.vortexai.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VortexShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shapes for specific components
val ChatBubbleShapes = object {
    val userMessage = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 4.dp
    )
    
    val botMessage = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 4.dp,
        bottomEnd = 20.dp
    )
    
    val systemMessage = RoundedCornerShape(12.dp)
}

val CardShapes = object {
    val character = RoundedCornerShape(16.dp)
    val conversation = RoundedCornerShape(12.dp)
    val imageGeneration = RoundedCornerShape(12.dp)
    val settings = RoundedCornerShape(8.dp)
}

val ButtonShapes = object {
    val primary = RoundedCornerShape(12.dp)
    val secondary = RoundedCornerShape(8.dp)
    val floating = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(20.dp)
}

val InputShapes = object {
    val textField = RoundedCornerShape(12.dp)
    val searchBar = RoundedCornerShape(24.dp)
    val chatInput = RoundedCornerShape(24.dp)
}

val ModalShapes = object {
    val bottomSheet = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val dialog = RoundedCornerShape(16.dp)
    val alertDialog = RoundedCornerShape(20.dp)
} 
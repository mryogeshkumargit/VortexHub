package com.vortexai.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.regex.Pattern

/**
 * Component for rendering formatted text with roleplay-style formatting
 * Supports various text styles commonly used in AI roleplay applications
 */
@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    isFromUser: Boolean = false
) {
    val annotatedText = buildAnnotatedString {
        val tokens = parseRoleplayText(text)
        
        tokens.forEach { token ->
            when (token.type) {
                TokenType.NORMAL -> {
                    withStyle(
                        style = SpanStyle(
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.BOLD -> {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.ITALIC -> {
                    withStyle(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.ACTION -> {
                    withStyle(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) 
                            else 
                                MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.DIALOGUE -> {
                    withStyle(
                        style = SpanStyle(
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.THOUGHT -> {
                    withStyle(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.EMPHASIS -> {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.error,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.WHISPER -> {
                    withStyle(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    ) {
                        append(token.content)
                    }
                }
                TokenType.NARRATOR -> {
                    withStyle(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isFromUser) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) 
                            else 
                                MaterialTheme.colorScheme.tertiary,
                            fontSize = 13.sp
                        )
                    ) {
                        append(token.content)
                    }
                }
            }
        }
    }
    
    Text(
        text = annotatedText,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 20.sp
        )
    )
}

/**
 * Enhanced component for rendering formatted text with image URL support
 * Detects image URLs in text and renders them as images
 */
@Composable
fun RichFormattedText(
    text: String,
    modifier: Modifier = Modifier,
    isFromUser: Boolean = false
) {
    val imageUrls = extractImageUrls(text)
    
    if (imageUrls.isEmpty()) {
        // No images found, use regular FormattedText
        FormattedText(
            text = text,
            modifier = modifier,
            isFromUser = isFromUser
        )
    } else {
        // Images found, render text and images
        Column(modifier = modifier) {
            var currentText = text
            
            imageUrls.forEach { imageUrl ->
                // Split text around the image URL
                val parts = currentText.split(imageUrl, limit = 2)
                
                if (parts.isNotEmpty()) {
                    // Render text before the image
                    if (parts[0].isNotBlank()) {
                        FormattedText(
                            text = parts[0],
                            isFromUser = isFromUser
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Render the image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image from message",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Update text for next iteration
                    currentText = if (parts.size > 1) parts[1] else ""
                }
            }
            
            // Render any remaining text after the last image
            if (currentText.isNotBlank()) {
                FormattedText(
                    text = currentText,
                    isFromUser = isFromUser
                )
            }
        }
    }
}

/**
 * Extract image URLs from text
 */
fun extractImageUrls(text: String): List<String> {
    val imageUrlPattern = Pattern.compile(
        "https?://[^\\s]+?\\.(jpg|jpeg|png|gif|webp|bmp)(\\?[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )
    
    val matcher = imageUrlPattern.matcher(text)
    val imageUrls = mutableListOf<String>()
    
    while (matcher.find()) {
        imageUrls.add(matcher.group())
    }
    
    return imageUrls
}

/**
 * Data class for text tokens with formatting
 */
data class TextToken(
    val content: String,
    val type: TokenType
)

/**
 * Enum for different text formatting types
 */
enum class TokenType {
    NORMAL,
    BOLD,
    ITALIC,
    ACTION,
    DIALOGUE,
    THOUGHT,
    EMPHASIS,
    WHISPER,
    NARRATOR
}

/**
 * Parse roleplay text and return formatted tokens
 * Supports various roleplay formatting conventions:
 * - *text* = actions/italics
 * - **text** = bold
 * - "text" = dialogue
 * - (text) = thoughts
 * - ***text*** = emphasis
 * - ~text~ = whispers
 * - [text] = narrator text
 * 
 * Uses regex-based parsing for robustness and proper newline handling
 */
fun parseRoleplayText(text: String): List<TextToken> {
    val tokens = mutableListOf<TextToken>()
    
    // Regex pattern with DOTALL flag to handle newlines
    // Order matters: longest patterns first (*** before ** before *)
    val pattern = Pattern.compile(
        "\\*\\*\\*([^*]+?)\\*\\*\\*|" +  // Emphasis (triple asterisk) - [^*] prevents crossing markers
        "\\*\\*([^*]+?)\\*\\*|" +         // Bold (double asterisk)
        "\\*([^*\\n]+?)\\*|" +             // Action (single asterisk) - single line preferred
        "\"([^\"]+?)\"|" +                // Dialogue
        "\\(([^)]+?)\\)|" +               // Thought
        "~([^~]+?)~|" +                    // Whisper
        "\\[([^\\]]+?)\\]",              // Narrator
        Pattern.DOTALL
    )
    
    val matcher = pattern.matcher(text)
    var lastEnd = 0
    
    while (matcher.find()) {
        // Add normal text before match
        if (matcher.start() > lastEnd) {
            tokens.add(TextToken(text.substring(lastEnd, matcher.start()), TokenType.NORMAL))
        }
        
        // Add formatted token based on which group matched
        when {
            matcher.group(1) != null -> tokens.add(TextToken(matcher.group(1)!!, TokenType.EMPHASIS))
            matcher.group(2) != null -> tokens.add(TextToken(matcher.group(2)!!, TokenType.BOLD))
            matcher.group(3) != null -> tokens.add(TextToken(matcher.group(3)!!, TokenType.ACTION))
            matcher.group(4) != null -> tokens.add(TextToken("\"${matcher.group(4)!!}\"", TokenType.DIALOGUE))
            matcher.group(5) != null -> tokens.add(TextToken("(${matcher.group(5)!!})", TokenType.THOUGHT))
            matcher.group(6) != null -> tokens.add(TextToken(matcher.group(6)!!, TokenType.WHISPER))
            matcher.group(7) != null -> tokens.add(TextToken("[${matcher.group(7)!!}]", TokenType.NARRATOR))
        }
        
        lastEnd = matcher.end()
    }
    
    // Add remaining normal text
    if (lastEnd < text.length) {
        tokens.add(TextToken(text.substring(lastEnd), TokenType.NORMAL))
    }
    
    return tokens
} 
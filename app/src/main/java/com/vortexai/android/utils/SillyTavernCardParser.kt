package com.vortexai.android.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.vortexai.android.ui.screens.characters.CharacterCreateState
import com.vortexai.android.ui.screens.characters.CharacterBook
import com.vortexai.android.ui.screens.characters.LorebookEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SillyTavernCardParser @Inject constructor(
    private val gson: Gson,
    private val httpClient: OkHttpClient
) {

    suspend fun parseCharacterCard(context: Context, uri: Uri): CharacterCreateState {
        return withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open file")

            val mimeType = contentResolver.getType(uri)
            
            when {
                mimeType?.startsWith("image/") == true -> {
                    parseCharacterCardFromPng(inputStream)
                }
                mimeType == "application/json" || uri.toString().endsWith(".json") -> {
                    parseCharacterCardFromJson(inputStream)
                }
                else -> {
                    // Try to parse as both PNG and JSON
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    
                    try {
                        // First try PNG
                        parseCharacterCardFromPngBytes(bytes)
                    } catch (e: Exception) {
                        try {
                            // Then try JSON
                            val jsonString = String(bytes, StandardCharsets.UTF_8)
                            parseCharacterCardFromJsonString(jsonString)
                        } catch (e2: Exception) {
                            throw IOException("Unable to parse character card: not a valid PNG or JSON file")
                        }
                    }
                }
            }
        }
    }

    suspend fun parseCharacterCardFromUrl(url: String): CharacterCreateState {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Failed to download character card: ${response.code}")
            }

            val bytes = response.body?.bytes() 
                ?: throw IOException("Empty response body")

            // Determine file type from URL or content
            when {
                url.contains(".png", ignoreCase = true) -> {
                    parseCharacterCardFromPngBytes(bytes)
                }
                url.contains(".json", ignoreCase = true) -> {
                    val jsonString = String(bytes, StandardCharsets.UTF_8)
                    parseCharacterCardFromJsonString(jsonString)
                }
                else -> {
                    // Try both formats
                    try {
                        parseCharacterCardFromPngBytes(bytes)
                    } catch (e: Exception) {
                        val jsonString = String(bytes, StandardCharsets.UTF_8)
                        parseCharacterCardFromJsonString(jsonString)
                    }
                }
            }
        }
    }

    private fun parseCharacterCardFromPng(inputStream: InputStream): CharacterCreateState {
        val bytes = inputStream.readBytes()
        inputStream.close()
        return parseCharacterCardFromPngBytes(bytes)
    }

    private fun parseCharacterCardFromPngBytes(bytes: ByteArray): CharacterCreateState {
        // Look for PNG chunks containing character data
        // SillyTavern stores character data in tEXt chunks with key "chara"
        val jsonData = extractCharacterDataFromPng(bytes)
            ?: throw IOException("No character data found in PNG file")

        // Extract image data as base64 for display
        val imageBase64 = encodeImageToBase64(bytes)
        
        val characterState = parseCharacterCardFromJsonString(jsonData)
        return characterState.copy(avatarBase64 = imageBase64)
    }

    private fun parseCharacterCardFromJson(inputStream: InputStream): CharacterCreateState {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        inputStream.close()
        return parseCharacterCardFromJsonString(jsonString)
    }

    private fun parseCharacterCardFromJsonString(jsonString: String): CharacterCreateState {
        try {
            // Try to parse as SillyTavern Character Card v2 format
            val characterCard = gson.fromJson(jsonString, SillyTavernCharacterCardV2::class.java)
            return convertToCharacterCreateState(characterCard)
        } catch (e: JsonSyntaxException) {
            try {
                // Try to parse as legacy format
                val legacyCard = gson.fromJson(jsonString, SillyTavernCharacterCardLegacy::class.java)
                return convertLegacyToCharacterCreateState(legacyCard)
            } catch (e2: JsonSyntaxException) {
                throw IOException("Invalid character card JSON format")
            }
        }
    }

    private fun extractCharacterDataFromPng(bytes: ByteArray): String? {
        // PNG format: 8-byte signature + chunks
        if (bytes.size < 8) return null
        
        // Check PNG signature
        val pngSignature = intArrayOf(
            0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        )
        
        for (i in pngSignature.indices) {
            if ((bytes[i].toInt() and 0xFF) != pngSignature[i]) return null
        }

        var offset = 8
        while (offset < bytes.size - 8) {
            // Read chunk length (4 bytes, big-endian)
            val length = ((bytes[offset].toInt() and 0xFF) shl 24) or
                        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                        (bytes[offset + 3].toInt() and 0xFF)
            
            // Read chunk type (4 bytes)
            val chunkType = String(bytes, offset + 4, 4, StandardCharsets.US_ASCII)
            
            // Check if this is a text chunk containing character data
            if (chunkType == "tEXt" && length > 5) {
                val chunkData = bytes.sliceArray(offset + 8 until offset + 8 + length)
                
                // Find null separator
                val nullIndex = chunkData.indexOf(0.toByte())
                if (nullIndex > 0) {
                    val keyword = String(chunkData, 0, nullIndex, StandardCharsets.US_ASCII)
                    if (keyword == "chara") {
                        val encodedData = String(chunkData, nullIndex + 1, chunkData.size - nullIndex - 1, StandardCharsets.US_ASCII)
                        
                        // Decode base64
                        return try {
                            val decodedBytes = Base64.decode(encodedData, Base64.DEFAULT)
                            String(decodedBytes, StandardCharsets.UTF_8)
                        } catch (e: Exception) {
                            encodedData // If not base64, return as-is
                        }
                    }
                }
            }
            
            // Move to next chunk (safely check bounds)
            if (offset + 8 + length + 4 > bytes.size) break
            offset += 8 + length + 4 // length + type + data + CRC
        }
        
        return null
    }

    private fun encodeImageToBase64(imageBytes: ByteArray): String {
        return try {
            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseCharacterBook(characterBookData: Any?): CharacterBook? {
        return try {
            when (characterBookData) {
                is Map<*, *> -> {
                    val bookMap = characterBookData as Map<String, Any>
                    val entries = (bookMap["entries"] as? List<*>)?.mapNotNull { entryData ->
                        parseLorebookEntry(entryData)
                    } ?: emptyList()
                    
                    val characterBook = CharacterBook(
                        name = bookMap["name"]?.toString() ?: "",
                        description = bookMap["description"]?.toString() ?: "",
                        scanDepth = (bookMap["scan_depth"] as? Number)?.toInt() ?: 100,
                        tokenBudget = (bookMap["token_budget"] as? Number)?.toInt() ?: 512,
                        recursiveScanning = bookMap["recursive_scanning"] as? Boolean ?: false,
                        extensions = bookMap["extensions"] as? Map<String, Any> ?: emptyMap(),
                        entries = entries
                    )
                    
                    android.util.Log.d("SillyTavernParser", "Parsed character book: ${characterBook.name} with ${characterBook.entries.size} entries")
                    return characterBook
                }
                else -> {
                    android.util.Log.d("SillyTavernParser", "Character book data is not a Map: ${characterBookData?.javaClass?.simpleName}")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SillyTavernParser", "Error parsing character book", e)
            null
        }
    }

    private fun parseLorebookEntry(entryData: Any?): LorebookEntry? {
        return try {
            when (entryData) {
                is Map<*, *> -> {
                    val entryMap = entryData as Map<String, Any>
                    LorebookEntry(
                        id = entryMap["id"]?.toString() ?: "",
                        keys = (entryMap["keys"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        content = entryMap["content"]?.toString() ?: "",
                        extensions = entryMap["extensions"] as? Map<String, Any> ?: emptyMap(),
                        enabled = entryMap["enabled"] as? Boolean ?: true,
                        insertionOrder = (entryMap["insertion_order"] as? Number)?.toInt() ?: 100,
                        caseSensitive = entryMap["case_sensitive"] as? Boolean ?: false,
                        name = entryMap["name"]?.toString() ?: "",
                        priority = (entryMap["priority"] as? Number)?.toInt() ?: 100,
                        comment = entryMap["comment"]?.toString() ?: "",
                        selective = entryMap["selective"] as? Boolean ?: false,
                        secondaryKeys = (entryMap["secondary_keys"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        constant = entryMap["constant"] as? Boolean ?: false,
                        position = entryMap["position"]?.toString() ?: "before_char"
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun convertToCharacterCreateState(card: SillyTavernCharacterCardV2): CharacterCreateState {
        val data = card.data
        
        // Parse character book/lorebook
        val characterBook = parseCharacterBook(data.characterBook)
        android.util.Log.d("SillyTavernParser", "Character book parsed: ${characterBook != null}, entries: ${characterBook?.entries?.size ?: 0}")
        
        // Extract extension fields as string map for easier UI handling
        val extensionFields = data.extensions?.mapValues { it.value.toString() } ?: emptyMap()
        
        return CharacterCreateState(
            name = data.name,
            description = data.description,
            personality = data.personality,
            scenario = data.scenario,
            avatarUrl = "", // Avatar is usually embedded in PNG
            avatarBase64 = "", // Will be set by PNG parser
            age = data.extensions?.get("age")?.toString() ?: "",
            gender = data.extensions?.get("gender")?.toString() ?: "",
            occupation = data.extensions?.get("occupation")?.toString() ?: "",
            tags = data.tags?.joinToString(", ") ?: "",
            greeting = data.firstMes,
            exampleDialogue = data.mesExample,
            systemPrompt = data.systemPrompt ?: "",
            creator = data.creator,
            characterVersion = data.characterVersion ?: "1.0",
            isNsfw = data.extensions?.get("nsfw") == true,
            isPublic = true,
            // Additional SillyTavern v2 fields
            creatorNotes = data.creatorNotes ?: "",
            postHistoryInstructions = data.postHistoryInstructions ?: "",
            alternateGreetings = data.alternateGreetings ?: emptyList(),
            characterBook = characterBook,
            extensionFields = extensionFields
        )
    }

    private fun convertLegacyToCharacterCreateState(card: SillyTavernCharacterCardLegacy): CharacterCreateState {
        return CharacterCreateState(
            name = card.name,
            description = card.description,
            personality = card.personality,
            scenario = card.scenario,
            avatarUrl = "",
            avatarBase64 = "", // Will be set by PNG parser if applicable
            age = "",
            gender = "",
            occupation = "",
            tags = "",
            greeting = card.firstMes,
            exampleDialogue = card.mesExample,
            systemPrompt = "",
            creator = card.creator ?: "",
            characterVersion = "1.0",
            isNsfw = false,
            isPublic = true,
            // Legacy format doesn't have these fields
            creatorNotes = "",
            postHistoryInstructions = "",
            alternateGreetings = emptyList(),
            characterBook = null,
            extensionFields = emptyMap()
        )
    }
}

// SillyTavern Character Card v2 format
data class SillyTavernCharacterCardV2(
    @SerializedName("spec")
    val spec: String,
    @SerializedName("spec_version")
    val specVersion: String,
    @SerializedName("data")
    val data: CharacterCardDataV2
)

data class CharacterCardDataV2(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("personality")
    val personality: String,
    @SerializedName("scenario")
    val scenario: String,
    @SerializedName("first_mes")
    val firstMes: String,
    @SerializedName("mes_example")
    val mesExample: String,
    @SerializedName("creator_notes")
    val creatorNotes: String? = null,
    @SerializedName("system_prompt")
    val systemPrompt: String? = null,
    @SerializedName("post_history_instructions")
    val postHistoryInstructions: String? = null,
    @SerializedName("alternate_greetings")
    val alternateGreetings: List<String>? = null,
    @SerializedName("character_book")
    val characterBook: Any? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,
    @SerializedName("creator")
    val creator: String,
    @SerializedName("character_version")
    val characterVersion: String? = null,
    @SerializedName("extensions")
    val extensions: Map<String, Any>? = null
)

// Legacy format for older character cards
data class SillyTavernCharacterCardLegacy(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("personality")
    val personality: String,
    @SerializedName("scenario")
    val scenario: String,
    @SerializedName("first_mes")
    val firstMes: String,
    @SerializedName("mes_example")
    val mesExample: String,
    @SerializedName("creator")
    val creator: String? = null
) 
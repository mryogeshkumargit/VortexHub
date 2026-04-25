package com.vortexai.android.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data models for SillyTavern character book format
 */
data class CharacterBookEntry(
    @SerializedName("uid") val uid: Int? = null,
    @SerializedName("key") val key: List<String> = emptyList(),
    @SerializedName("keysecondary") val keysecondary: List<String> = emptyList(),
    @SerializedName("content") val content: String = "",
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("name") val name: String? = null,
    @SerializedName("priority") val priority: Int? = 100,
    @SerializedName("depth") val depth: Int? = 4,
    @SerializedName("probability") val probability: Int? = 100,
    @SerializedName("case_sensitive") val caseSensitive: Boolean? = false
)

data class CharacterBook(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("scan_depth") val scanDepth: Int? = 4,
    @SerializedName("token_budget") val tokenBudget: Int? = 1024,
    @SerializedName("recursive_scanning") val recursiveScanning: Boolean? = false,
    @SerializedName("entries") val entries: Map<String, CharacterBookEntry>? = null
)

@Singleton
class CharacterBookParser @Inject constructor() {
    
    companion object {
        private const val TAG = "CharacterBookParser"
    }
    
    private val gson = Gson()
    
    /**
     * Parse character book JSON file
     */
    fun parseCharacterBook(context: Context, uri: Uri): Result<CharacterBook> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return Result.failure(Exception("Failed to read file"))
            
            val book = gson.fromJson(jsonString, CharacterBook::class.java)
            
            if (book.entries.isNullOrEmpty()) {
                return Result.failure(Exception("Character book has no entries"))
            }
            
            Log.d(TAG, "Parsed character book: ${book.name}, ${book.entries.size} entries")
            Result.success(book)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse character book", e)
            Result.failure(e)
        }
    }
    
    /**
     * Convert character book to JSON string for storage
     */
    fun toJson(book: CharacterBook): String {
        return gson.toJson(book)
    }
    
    /**
     * Parse character book from JSON string
     */
    fun fromJson(json: String): CharacterBook? {
        return try {
            gson.fromJson(json, CharacterBook::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse character book from JSON", e)
            null
        }
    }
}

package com.vortexai.android.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Room TypeConverters for handling complex data types
 */
class Converters {
    
    companion object {
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val ISO_DATE_FORMAT_SHORT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        fun parseIsoDate(dateString: String?): Date? {
            if (dateString == null) return null
            return try {
                // Try with microseconds first
                ISO_DATE_FORMAT.parse(dateString)
            } catch (e: Exception) {
                try {
                    // Try without microseconds
                    ISO_DATE_FORMAT_SHORT.parse(dateString)
                } catch (e2: Exception) {
                    null
                }
            }
        }
        
        fun formatIsoDate(date: Date?): String? {
            return date?.let { ISO_DATE_FORMAT.format(it) }
        }
    }
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) null else {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(value, listType)
        }
    }
    
    @TypeConverter
    fun fromMapStringAny(value: Map<String, Any>?): String? {
        return if (value == null) null else Gson().toJson(value)
    }
    
    @TypeConverter
    fun toMapStringAny(value: String?): Map<String, Any>? {
        return if (value == null) null else {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            Gson().fromJson(value, mapType)
        }
    }
    
    @TypeConverter
    fun fromDateString(value: String?): Date? {
        return parseIsoDate(value)
    }
    
    @TypeConverter
    fun toDateString(date: Date?): String? {
        return formatIsoDate(date)
    }
    
    @TypeConverter
    fun fromMapStringString(value: Map<String, String>?): String? {
        return if (value == null) null else Gson().toJson(value)
    }
    
    @TypeConverter
    fun toMapStringString(value: String?): Map<String, String>? {
        return if (value == null || value.isEmpty()) emptyMap() else {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(value, mapType)
        }
    }
}

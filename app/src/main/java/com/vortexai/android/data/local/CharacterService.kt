package com.vortexai.android.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.vortexai.android.data.database.dao.CharacterDao
import com.vortexai.android.data.models.Character
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified local character service
 */
@Singleton
class CharacterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val characterDao: CharacterDao,
    private val gson: Gson
) {
    
    companion object {
        private const val TAG = "CharacterService"
    }
    
    /**
     * Get all characters
     */
    fun getAllCharacters(): Flow<Result<List<Character>>> = flow {
        try {
            Log.d(TAG, "Getting all characters")
            val characters = characterDao.getAllActiveCharacters()
            emit(Result.success(emptyList<Character>()))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting characters", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get character by ID
     */
    fun getCharacterById(id: String): Flow<Result<Character?>> = flow {
        try {
            Log.d(TAG, "Getting character: $id")
            val character = characterDao.getCharacterById(id)
            emit(Result.success(character))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting character", e)
            emit(Result.failure(e))
        }
    }
} 
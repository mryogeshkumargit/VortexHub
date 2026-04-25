package com.vortexai.android.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for playing audio from URLs
 * Used for TTS audio playback and testing voices
 */
object AudioPlayer {
    private const val TAG = "AudioPlayer"
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Play audio from a URL
     * @param audioUrl URL of the audio file to play
     * @param onCompletion Callback when playback completes
     * @param onError Callback when an error occurs
     */
    suspend fun playAudio(
        audioUrl: String,
        onCompletion: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.Main) {
        try {
            // Stop any currently playing audio
            stopAudio()
            
            Log.d(TAG, "Starting audio playback for URL: $audioUrl")
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setDataSource(audioUrl)
                
                setOnPreparedListener {
                    Log.d(TAG, "Audio prepared, starting playback")
                    start()
                }
                
                setOnCompletionListener {
                    Log.d(TAG, "Audio playback completed")
                    onCompletion?.invoke()
                    release()
                    mediaPlayer = null
                }
                
                setOnErrorListener { _, what, extra ->
                    val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                    Log.e(TAG, errorMsg)
                    onError?.invoke(errorMsg)
                    release()
                    mediaPlayer = null
                    true
                }
                
                prepareAsync()
            }
        } catch (e: IOException) {
            val errorMsg = "Failed to play audio: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onError?.invoke(errorMsg)
        } catch (e: Exception) {
            val errorMsg = "Unexpected error playing audio: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onError?.invoke(errorMsg)
        }
    }
    
    /**
     * Stop any currently playing audio
     */
    fun stopAudio() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    Log.d(TAG, "Stopping audio playback")
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio", e)
            } finally {
                mediaPlayer = null
            }
        }
    }
    
    /**
     * Play audio from byte array
     * @param audioBytes Audio data as byte array
     * @param onCompletion Callback when playback completes
     * @param onError Callback when an error occurs
     */
    suspend fun playAudioBytes(
        audioBytes: ByteArray,
        onCompletion: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            // Create temporary file
            val tempFile = File.createTempFile("tts_audio", ".mp3")
            tempFile.deleteOnExit()
            
            // Write bytes to temp file
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            
            Log.d(TAG, "Created temp audio file: ${tempFile.absolutePath}, size: ${audioBytes.size} bytes")
            
            // Play the temp file
            withContext(Dispatchers.Main) {
                playAudio(
                    audioUrl = tempFile.absolutePath,
                    onCompletion = {
                        onCompletion?.invoke()
                        // Clean up temp file
                        try {
                            tempFile.delete()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to delete temp file", e)
                        }
                    },
                    onError = { error ->
                        onError?.invoke(error)
                        // Clean up temp file
                        try {
                            tempFile.delete()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to delete temp file", e)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to play audio bytes: ${e.message}"
            Log.e(TAG, errorMsg, e)
            withContext(Dispatchers.Main) {
                onError?.invoke(errorMsg)
            }
        }
    }
    
    /**
     * Check if audio is currently playing
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
    
    /**
     * Pause audio playback
     */
    fun pauseAudio() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    Log.d(TAG, "Pausing audio playback")
                    player.pause()
                } else {
                    // Audio is not playing
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing audio", e)
            }
        }
    }
    
    /**
     * Resume audio playback
     */
    fun resumeAudio() {
        mediaPlayer?.let { player ->
            try {
                if (!player.isPlaying) {
                    Log.d(TAG, "Resuming audio playback")
                    player.start()
                } else {
                    // Audio is already playing
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming audio", e)
            }
        }
    }
}

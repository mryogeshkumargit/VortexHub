package com.vortexai.android.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object TTSManager : TextToSpeech.OnInitListener {
    private const val TAG = "TTSManager"
    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            tts?.language = Locale.US
            Log.d(TAG, "TTS initialized successfully")
        } else {
            Log.e(TAG, "TTS initialization failed: status=$status")
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "tts_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }
} 
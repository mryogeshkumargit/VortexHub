package com.vortexai.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.vortexai.android.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep the splash screen on-screen for a longer period
        splashScreen.setKeepOnScreenCondition { true }
        
        // Initialize app and navigate to main activity
        lifecycleScope.launch {
            // Simulate initialization time
            delay(1500)
            
            // Navigate to main activity
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
} 
package com.vortexai.android.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure API key encryption using Android Keystore.
 * The encryption key is stored in the hardware-backed Android Keystore,
 * ensuring it persists across app restarts and is secure.
 */
object ApiKeyEncryption {
    
    private const val TAG = "ApiKeyEncryption"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "VortexAI_ApiKey_Master"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    
    /**
     * Get or create the master key from Android Keystore.
     * This key persists across app restarts.
     */
    private fun getMasterKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            // Check if key already exists
            val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existingKey != null) {
                return existingKey
            }
            
            // Generate new key if not exists
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get/create master key", e)
            null
        }
    }
    
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return plainText
        
        try {
            val masterKey = getMasterKey() ?: run {
                Log.w(TAG, "Master key unavailable, storing plain text")
                return plainText
            }
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV + encrypted data
            val combined = iv + encrypted
            
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            return plainText // Fallback to plain text
        }
    }
    
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isBlank()) return encryptedText
        
        // Check if this looks like encrypted data (base64 + minimum length for IV + data)
        if (!isLikelyEncrypted(encryptedText)) {
            return encryptedText // Return as-is if it's not encrypted
        }
        
        try {
            val masterKey = getMasterKey() ?: run {
                Log.w(TAG, "Master key unavailable, returning as-is")
                return encryptedText
            }
            
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            // Check minimum length (IV_SIZE + at least 1 byte of data + auth tag)
            if (combined.size < IV_SIZE + 17) {
                Log.w(TAG, "Data too short to be encrypted, returning as-is")
                return encryptedText
            }
            
            val iv = combined.copyOfRange(0, IV_SIZE)
            val encrypted = combined.copyOfRange(IV_SIZE, combined.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
            
            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed - key may have been encrypted with old key", e)
            // If decryption fails, it's likely plain text or encrypted with old ephemeral key
            // Return as-is and let user re-enter API key
            return encryptedText
        }
    }
    
    /**
     * Check if the text appears to be encrypted (base64 encoded with minimum length).
     */
    private fun isLikelyEncrypted(text: String): Boolean {
        if (text.isBlank()) return false
        
        return try {
            val decoded = Base64.decode(text, Base64.NO_WRAP)
            // Encrypted data should have at least IV + auth tag overhead
            decoded.size >= IV_SIZE + 16
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if API key looks valid (not encrypted garbage).
     * Valid API keys typically contain alphanumeric characters, hyphens, underscores, and colons.
     */
    fun isValidApiKey(text: String): Boolean {
        if (text.isBlank()) return false
        // Valid API keys should be mostly printable ASCII and not random binary
        return text.all { it.code in 32..126 } && text.length > 10
    }
}

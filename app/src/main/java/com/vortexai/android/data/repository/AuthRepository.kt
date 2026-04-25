package com.vortexai.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vortexai.android.data.api.AuthApiService
import com.vortexai.android.data.models.AuthResponse
import com.vortexai.android.data.models.LoginRequest
import com.vortexai.android.data.models.RegisterRequest
import com.vortexai.android.data.models.User
import com.vortexai.android.utils.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

/**
 * Repository for handling authentication operations
 * Manages API calls, token storage, and user session
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService?,
    private val dataStore: DataStore<Preferences>,
    private val accountDao: com.vortexai.android.data.database.dao.AccountDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AuthRepository"
        
        // DataStore keys
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_USERNAME_KEY = stringPreferencesKey("user_username")
        private val USER_AVATAR_URL_KEY = stringPreferencesKey("user_avatar_url")
        private val IS_PREMIUM_KEY = stringPreferencesKey("is_premium")
    }
    
    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String, rememberMe: Boolean = false): Flow<Result<User>> = flow {
        try {
            Log.d(TAG, "Attempting login for email: $email")
            
            if (authApiService == null) {
                // Simplified local login for demo purposes
                if (email.isNotBlank() && password.isNotBlank()) {
                    val user = User(
                        id = IdGenerator.generateUserId(),
                        username = email.substringBefore("@"),
                        email = email
                    )
                    
                    // Save simple session
                    dataStore.edit { preferences ->
                        preferences[USER_ID_KEY] = user.id
                        preferences[USER_EMAIL_KEY] = user.email ?: ""
                        preferences[USER_USERNAME_KEY] = user.username
                        preferences[ACCESS_TOKEN_KEY] = "demo_token_${System.currentTimeMillis()}"
                    }
                    
                    Log.d(TAG, "Local login successful for user: ${user.username}")
                    emit(Result.success(user))
                } else {
                    emit(Result.failure(Exception("Email and password required")))
                }
                return@flow
            }
            
            val request = LoginRequest(email, password, rememberMe)
            val response = authApiService.login(request)
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.user != null) {
                    // Save tokens and user data
                    saveUserSession(authResponse)
                    Log.d(TAG, "Login successful for user: ${authResponse.user.username}")
                    emit(Result.success(authResponse.user))
                } else {
                    val errorMsg = authResponse?.message ?: "Login failed"
                    Log.e(TAG, "Login failed: $errorMsg")
                    emit(Result.failure(Exception(errorMsg)))
                }
            } else {
                // Handle error response
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Login failed with response: $errorBody")
                
                val errorMsg = try {
                    val errorJson = com.google.gson.JsonParser.parseString(errorBody)
                    errorJson.asJsonObject.get("error")?.asString ?: "Network error: ${response.code()}"
                } catch (e: Exception) {
                    "Network error: ${response.code()}"
                }
                
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login exception", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Register new user
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Flow<Result<User>> = flow {
        try {
            Log.d(TAG, "Attempting registration for email: $email")
            
            if (authApiService == null) {
                // Simplified local registration for demo purposes
                if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && password == confirmPassword) {
                    val user = User(
                        id = IdGenerator.generateUserId(),
                        username = username,
                        email = email
                    )
                    
                    // Save simple session
                    dataStore.edit { preferences ->
                        preferences[USER_ID_KEY] = user.id
                        preferences[USER_EMAIL_KEY] = user.email ?: ""
                        preferences[USER_USERNAME_KEY] = user.username
                        preferences[ACCESS_TOKEN_KEY] = "demo_token_${System.currentTimeMillis()}"
                    }
                    
                    Log.d(TAG, "Local registration successful for user: ${user.username}")
                    emit(Result.success(user))
                } else {
                    emit(Result.failure(Exception("All fields required and passwords must match")))
                }
                return@flow
            }
            
            val request = RegisterRequest(username, email, password, confirmPassword)
            val response = authApiService?.register(request)
            
            if (response?.isSuccessful == true) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.user != null) {
                    // Save tokens and user data
                    saveUserSession(authResponse)
                    Log.d(TAG, "Registration successful for user: ${authResponse.user.username}")
                    emit(Result.success(authResponse.user))
                } else {
                    val errorMsg = authResponse?.message ?: "Registration failed"
                    Log.e(TAG, "Registration failed: $errorMsg")
                    emit(Result.failure(Exception(errorMsg)))
                }
            } else {
                val errorMsg = "Network error: ${response?.code() ?: 0}"
                Log.e(TAG, errorMsg)
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration exception", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Logout current user
     */
    suspend fun logout(): Flow<Result<Unit>> = flow {
        try {
            Log.d(TAG, "Attempting logout")
            
            if (authApiService == null) {
                // Simple local logout
                clearUserSession()
                Log.d(TAG, "Local logout successful")
                emit(Result.success(Unit))
                return@flow
            }
            
            val token = getAccessToken()
            if (token != null) {
                val response = authApiService?.logout("Bearer $token")
                // Clear local session regardless of API response
                clearUserSession()
                
                if (response?.isSuccessful == true) {
                    Log.d(TAG, "Logout successful")
                    emit(Result.success(Unit))
                } else {
                    Log.w(TAG, "Logout API failed but local session cleared")
                    emit(Result.success(Unit))
                }
            } else {
                // No token found, just clear local session
                clearUserSession()
                Log.d(TAG, "No token found, cleared local session")
                emit(Result.success(Unit))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Logout exception", e)
            // Still clear local session on error
            clearUserSession()
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get current user from API
     */
    suspend fun getCurrentUser(): Flow<Result<User>> = flow {
        try {
            val token = getAccessToken()
            if (token == null) {
                emit(Result.failure(Exception("No access token found")))
                return@flow
            }
            
            val response = authApiService?.getCurrentUser("Bearer $token")
            
            if (response?.isSuccessful == true) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    Log.d(TAG, "Got current user: ${apiResponse.data.username}")
                    emit(Result.success(apiResponse.data))
                } else {
                    val errorMsg = apiResponse?.message ?: "Failed to get user"
                    emit(Result.failure(Exception(errorMsg)))
                }
            } else {
                emit(Result.failure(Exception("Network error: ${response?.code() ?: 0}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get current user exception", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            val token = preferences[ACCESS_TOKEN_KEY]
            val userId = preferences[USER_ID_KEY]
            !token.isNullOrBlank() && !userId.isNullOrBlank()
        }
    }
    
    /**
     * Get stored access token
     */
    private suspend fun getAccessToken(): String? {
        return dataStore.data.first()[ACCESS_TOKEN_KEY]
    }
    
    /**
     * Get stored refresh token
     */
    private suspend fun getRefreshToken(): String? {
        return dataStore.data.first()[REFRESH_TOKEN_KEY]
    }
    
    /**
     * Save user session data to DataStore and Room Database
     */
    private suspend fun saveUserSession(authResponse: AuthResponse) {
        dataStore.edit { preferences ->
            authResponse.accessToken?.let { preferences[ACCESS_TOKEN_KEY] = it }
            authResponse.refreshToken?.let { preferences[REFRESH_TOKEN_KEY] = it }
            
            authResponse.user?.let { user ->
                preferences[USER_ID_KEY] = user.id
                preferences[USER_EMAIL_KEY] = user.email ?: ""
                preferences[USER_USERNAME_KEY] = user.username
                user.avatarUrl?.let { preferences[USER_AVATAR_URL_KEY] = it }
                preferences[IS_PREMIUM_KEY] = user.isPremium.toString()
            }
        }
        
        // Persist to Room Database for survival across reinstalls
        authResponse.user?.let { user ->
            val account = com.vortexai.android.data.models.Account(
                id = user.id,
                username = user.username,
                email = user.email,
                fullName = user.displayName,
                dateOfBirth = null,
                avatarUrl = user.avatarUrl,
                isPremium = user.isPremium,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )
            accountDao.insertAccount(account)
        }
        Log.d(TAG, "User session saved to DataStore and Room Database")
    }
    
    /**
     * Clear user session data from DataStore and Room Database
     */
    private suspend fun clearUserSession() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(USER_USERNAME_KEY)
            preferences.remove(USER_AVATAR_URL_KEY)
            preferences.remove(IS_PREMIUM_KEY)
        }
        accountDao.deleteAllAccounts()
        Log.d(TAG, "User session cleared from DataStore and Room Database")
    }
    
    /**
     * Get cached user data from DataStore
     */
    fun getCachedUser(): Flow<User?> {
        return dataStore.data.map { preferences ->
            val id = preferences[USER_ID_KEY]
            val email = preferences[USER_EMAIL_KEY]
            val username = preferences[USER_USERNAME_KEY]
            
            if (id != null && email != null && username != null) {
                User(
                    id = id,
                    email = email,
                    username = username,
                    displayName = preferences[stringPreferencesKey("full_name")],
                    avatarUrl = preferences[USER_AVATAR_URL_KEY],
                    isPremium = preferences[IS_PREMIUM_KEY]?.toBoolean() ?: false,
                    preferences = null
                )
            } else {
                null
            }
        }
    }
    
    /**
     * Update user profile information
     */
    suspend fun updateUserProfile(
        username: String,
        fullName: String? = null,
        email: String? = null,
        dateOfBirth: String? = null
    ): Flow<Result<User>> = flow {
        try {
            Log.d(TAG, "Updating user profile for username: $username")
            
            // Update local DataStore
            dataStore.edit { preferences ->
                preferences[USER_USERNAME_KEY] = username
                fullName?.let { preferences[stringPreferencesKey("full_name")] = it }
                email?.let { preferences[USER_EMAIL_KEY] = it }
                dateOfBirth?.let { preferences[stringPreferencesKey("date_of_birth")] = it }
            }
            
            // Get updated user
            val updatedUser = getCachedUser().first()
            if (updatedUser != null) {
                Log.d(TAG, "Profile update successful for user: ${updatedUser.username}")
                emit(Result.success(updatedUser))
            } else {
                emit(Result.failure(Exception("Failed to retrieve updated user")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Profile update exception", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Guest login - creates a temporary user account for anonymous usage
     */
    suspend fun guestLogin(): Flow<Result<User>> = flow {
        try {
            Log.d(TAG, "Attempting guest login...")
            
            // Generate a unique guest username and ID
            val guestId = IdGenerator.generateShortId()
            val guestUsername = "guest_$guestId"
            val guestEmail = "guest_$guestId@vortex.app"
            
            // Create guest user without API call
            val guestUser = User(
                id = IdGenerator.generateUserId(),
                username = guestUsername,
                email = guestEmail,
                isPremium = false,
                avatarUrl = null,
                preferences = null
            )
            
            Log.d(TAG, "Creating local guest account: $guestUsername")
            
            // Save guest session locally
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = guestUser.id
                preferences[USER_EMAIL_KEY] = guestUser.email ?: ""
                preferences[USER_USERNAME_KEY] = guestUser.username
                preferences[ACCESS_TOKEN_KEY] = "guest_token_${System.currentTimeMillis()}"
                preferences[IS_PREMIUM_KEY] = "false"
            }
            
            Log.d(TAG, "Guest login successful for user: ${guestUser.username}")
            emit(Result.success(guestUser))
            
        } catch (e: Exception) {
            Log.e(TAG, "Guest login exception", e)
            emit(Result.failure(e))
        }
    }
} 
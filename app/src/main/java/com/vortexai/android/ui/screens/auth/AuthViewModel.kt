package com.vortexai.android.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexai.android.data.models.User
import com.vortexai.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens
 * Handles login, registration, and authentication state
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        // Check if user is already logged in
        checkLoginStatus()
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                authRepository.login(email, password, rememberMe).collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d(TAG, "Login successful: ${user.username}")
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    isLoginSuccessful = true,
                                    currentUser = user,
                                    errorMessage = null
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Login failed", exception)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    isLoginSuccessful = false,
                                    errorMessage = exception.message ?: "Login failed"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login exception", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoginSuccessful = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
    
    /**
     * Register new user
     */
    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            // Validate passwords match
            if (password != confirmPassword) {
                _uiState.update { 
                    it.copy(errorMessage = "Passwords do not match")
                }
                return@launch
            }
            
            // Validate password strength
            if (password.length < 8) {
                _uiState.update { 
                    it.copy(errorMessage = "Password must be at least 8 characters long")
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                authRepository.register(username, email, password, confirmPassword).collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d(TAG, "Registration successful: ${user.username}")
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    isRegistrationSuccessful = true,
                                    currentUser = user,
                                    errorMessage = null
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Registration failed", exception)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    isRegistrationSuccessful = false,
                                    errorMessage = exception.message ?: "Registration failed"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration exception", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isRegistrationSuccessful = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                authRepository.logout().collect { result ->
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Logout successful")
                            _uiState.update { 
                                AuthUiState(isLoggedOut = true)
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Logout failed", exception)
                            // Even if logout fails on server, clear local state
                            _uiState.update { 
                                AuthUiState(
                                    isLoggedOut = true,
                                    errorMessage = "Logout completed (with warnings)"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logout exception", e)
                // Clear local state even on exception
                _uiState.update { 
                    AuthUiState(
                        isLoggedOut = true,
                        errorMessage = "Logout completed (offline)"
                    )
                }
            }
        }
    }
    
    /**
     * Guest login - creates a temporary user account for anonymous usage
     */
    fun guestLogin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                authRepository.guestLogin().collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d(TAG, "Guest login successful: ${user.username}")
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    isLoginSuccessful = true,
                                    currentUser = user,
                                    errorMessage = null
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Guest login failed", exception)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    isLoginSuccessful = false,
                                    errorMessage = exception.message ?: "Guest login failed"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Guest login exception", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoginSuccessful = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
    
    /**
     * Check if user is currently logged in
     */
    private fun checkLoginStatus() {
        viewModelScope.launch {
            authRepository.isLoggedIn().collect { isLoggedIn ->
                if (isLoggedIn) {
                    // Get cached user data
                    authRepository.getCachedUser().collect { user ->
                        _uiState.update { 
                            it.copy(
                                isLoggedIn = true,
                                currentUser = user
                            )
                        }
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoggedIn = false,
                            currentUser = null
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Reset success states (used after navigation)
     */
    fun resetSuccessStates() {
        _uiState.update { 
            it.copy(
                isLoginSuccessful = false,
                isRegistrationSuccessful = false,
                isLoggedOut = false
            )
        }
    }
}

/**
 * UI state for authentication screens
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val isRegistrationSuccessful: Boolean = false,
    val isLoggedOut: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null
) 
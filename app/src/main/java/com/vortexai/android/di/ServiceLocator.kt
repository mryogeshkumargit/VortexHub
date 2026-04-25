package com.vortexai.android.di

import com.vortexai.android.data.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service locator for accessing repositories when dependency injection is not available
 */
@Singleton
class ServiceLocator @Inject constructor(
    private val authRepository: AuthRepository
) {
    companion object {
        @Volatile
        private var instance: ServiceLocator? = null
        
        fun initialize(serviceLocator: ServiceLocator) {
            instance = serviceLocator
        }
        
        fun getAuthRepository(): AuthRepository {
            return instance?.authRepository 
                ?: throw IllegalStateException("ServiceLocator not initialized")
        }
    }
} 
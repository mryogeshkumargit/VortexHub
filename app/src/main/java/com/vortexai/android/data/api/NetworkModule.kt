package com.vortexai.android.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network module for API configuration
 * Configured to handle large responses including massive lorebook data
 */
object NetworkModule {
    
    // Base URL from build config
    private const val BASE_URL = "http://10.0.2.2:5000/"
    
    /**
     * Create OkHttpClient with extended timeouts for large data
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)  // Extended for large responses
            .readTimeout(120, TimeUnit.SECONDS)    // Extended for massive lorebook data
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(150, TimeUnit.SECONDS)    // Overall timeout for complete operation
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Create Gson with custom configuration for large data
     */
    private fun createGson() = GsonBuilder()
        .setLenient()  // Allow lenient parsing for large JSON
        .serializeNulls()
        .create()
    
    /**
     * Create Retrofit instance configured for large responses
     */
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(createGson()))
            .build()
    }
    
    /**
     * Provide CharacterApiService instance
     */
    fun provideCharacterApiService(): CharacterApiService {
        return createRetrofit().create(CharacterApiService::class.java)
    }
} 
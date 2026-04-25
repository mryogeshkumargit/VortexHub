package com.vortexai.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.vortexai.android.BuildConfig
import com.vortexai.android.data.api.AuthApiService
import com.vortexai.android.data.api.CharacterApiService
import com.vortexai.android.data.api.ChatApiService
import com.vortexai.android.data.api.AIApiService
import com.vortexai.android.data.api.AIApiServiceImpl
import com.vortexai.android.data.models.Character
import com.vortexai.android.domain.service.ChatLLMService
import com.vortexai.android.domain.service.ModelsLabTTSApi
import com.vortexai.android.domain.service.VideoGenerationService
import com.vortexai.android.domain.service.VideoGenerationTracker
import com.vortexai.android.domain.service.ModelsLabImageApi
import com.vortexai.android.utils.MacroProcessor
import com.vortexai.android.utils.SillyTavernCardParser
import com.vortexai.android.utils.ChatTTSManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Named
import javax.inject.Singleton

/**
 * Network module for dependency injection
 * Provides different Retrofit instances for various AI APIs
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // DataStore extension
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vortex_preferences")
    
    // API Base URLs
    private const val TOGETHER_AI_BASE_URL = "https://api.together.xyz/"
    private const val GEMINI_AI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val MODELSLAB_BASE_URL = "https://modelslab.com/api/"
    private const val CUSTOM_API_BASE_URL = "https://your-custom-api.com/" // Replace with your custom API URL
    
    /**
     * Provides DataStore instance
     */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
    
    /**
     * Provides Gson with configuration for large data handling and safe collection deserialization
     */
    @Provides
    @Singleton
    fun provideGson() = GsonBuilder()
        .setLenient()  // Allow lenient parsing for large JSON
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")  // Handle ISO dates with microseconds
        .registerTypeAdapter(object : TypeToken<List<String>>() {}.type, JsonDeserializer<List<String>> { json, _, _ ->
            try {
                when {
                    json.isJsonNull -> emptyList()
                    json.isJsonArray -> json.asJsonArray.mapNotNull { 
                        if (it.isJsonPrimitive && it.asJsonPrimitive.isString) it.asString else null 
                    }
                    json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                        val str = json.asString
                        if (str.isBlank()) emptyList() else listOf(str)
                    }
                    else -> emptyList()
                }
            } catch (e: Exception) {
                emptyList<String>()
            }
        })
        .registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, JsonDeserializer<Map<String, Any>> { json, _, _ ->
            try {
                when {
                    json.isJsonNull -> emptyMap()
                    json.isJsonObject -> {
                        val map = mutableMapOf<String, Any>()
                        json.asJsonObject.entrySet().forEach { (key, value) ->
                            when {
                                value.isJsonPrimitive -> {
                                    val primitive = value.asJsonPrimitive
                                    map[key] = when {
                                        primitive.isString -> primitive.asString
                                        primitive.isNumber -> primitive.asNumber
                                        primitive.isBoolean -> primitive.asBoolean
                                        else -> primitive.asString
                                    }
                                }
                                value.isJsonArray -> map[key] = value.toString()
                                value.isJsonObject -> map[key] = value.toString()
                                else -> map[key] = value.toString()
                            }
                        }
                        map.toMap()
                    }
                    else -> emptyMap()
                }
            } catch (e: Exception) {
                emptyMap<String, Any>()
            }
        })
        .create()
    
    /**
     * Provides OkHttpClient with extended timeouts for large data
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // Extended for large responses
            .readTimeout(180, TimeUnit.SECONDS)    // 3 minutes for massive lorebook data
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(240, TimeUnit.SECONDS)    // 4 minutes overall timeout
            .retryOnConnectionFailure(true)
        
        // Add logging interceptor for debug builds
        if (BuildConfig.DEBUG_MODE) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS  // Only headers to avoid logging massive data
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    /**
     * Provides Together AI Retrofit instance
     */
    @Provides
    @Singleton
    @Named("together")
    fun provideTogetherRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TOGETHER_AI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Provides Gemini AI Retrofit instance
     */
    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(GEMINI_AI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Provides Custom API Retrofit instance
     */
    @Provides
    @Singleton
    @Named("custom")
    fun provideCustomRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(CUSTOM_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Provides ModelsLab API Retrofit instance
     */
    @Provides
    @Singleton
    @Named("modelslab")
    fun provideModelsLabRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MODELSLAB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provides AI API Service implementation
     */
    @Provides
    @Singleton
    fun provideAIApiService(
        @Named("together") togetherRetrofit: Retrofit,
        @Named("gemini") geminiRetrofit: Retrofit,
        @Named("custom") customRetrofit: Retrofit
    ): AIApiService {
        return AIApiServiceImpl(togetherRetrofit, geminiRetrofit, customRetrofit)
    }
    
    /**
     * Provides CharacterApiService
     */
    @Provides
    @Singleton
    fun provideCharacterApiService(retrofit: Retrofit): CharacterApiService {
        return retrofit.create(CharacterApiService::class.java)
    }
    
    /**
     * Provides AuthApiService
     */
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    /**
     * Provides ChatApiService
     */
    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }
    
    /**
     * Provides SillyTavernCardParser for character card imports
     */
    @Provides
    @Singleton
    fun provideSillyTavernCardParser(gson: Gson, httpClient: OkHttpClient): SillyTavernCardParser {
        return SillyTavernCardParser(gson, httpClient)
    }
    
    /**
     * Provides ChatLLMService for real AI model interactions
     */
    @Provides
    @Singleton
    fun provideChatLLMService(
        dataStore: DataStore<Preferences>,
        macroProcessor: MacroProcessor,
        apiConnectionTester: com.vortexai.android.utils.ApiConnectionTester,
        customApiProviderRepository: com.vortexai.android.data.repository.CustomApiProviderRepository
    ): ChatLLMService {
        return ChatLLMService(dataStore, macroProcessor, apiConnectionTester, customApiProviderRepository)
    }
    
    /**
     * Provides API Connection Tester
     */
    @Provides
    @Singleton
    fun provideApiConnectionTester(): com.vortexai.android.utils.ApiConnectionTester {
        return com.vortexai.android.utils.ApiConnectionTester()
    }
    
    /**
     * Provides ModelsLab TTS API service
     */
    @Provides
    @Singleton
    fun provideModelsLabTTSApi(): ModelsLabTTSApi {
        return ModelsLabTTSApi()
    }
    
    /**
     * Provides ChatTTSManager for intelligent TTS with ModelsLab integration
     */
    @Provides
    @Singleton
    fun provideChatTTSManager(dataStore: DataStore<Preferences>): ChatTTSManager {
        return ChatTTSManager(dataStore)
    }
    
    /**
     * Provides TogetherApi for Together AI functionality
     */
    @Provides
    @Singleton
    fun provideTogetherApi(): com.vortexai.android.domain.service.together.TogetherApi {
        return com.vortexai.android.domain.service.together.TogetherApi()
    }
    
    /**
     * Provides ImageEditingService for image editing functionality
     */
    @Provides
    @Singleton
    fun provideImageEditingService(
        okHttpClient: OkHttpClient,
        togetherApi: com.vortexai.android.domain.service.together.TogetherApi,
        dataStore: DataStore<Preferences>,
        logger: com.vortexai.android.domain.service.GenerationLogger
    ): com.vortexai.android.domain.service.ImageEditingService {
        return com.vortexai.android.domain.service.ImageEditingService(okHttpClient, togetherApi, dataStore, logger)
    }
    
    /**
     * Provides TogetherAITTSProvider for Together AI TTS functionality
     */
    @Provides
    @Singleton
    fun provideTogetherAITTSProvider(): com.vortexai.android.domain.service.audio.TogetherAITTSProvider {
        return com.vortexai.android.domain.service.audio.TogetherAITTSProvider()
    }
    
    /**
     * Provides VideoGenerationTracker for tracking video generation status
     */
    @Provides
    @Singleton
    fun provideVideoGenerationTracker(): com.vortexai.android.domain.service.VideoGenerationTracker {
        return com.vortexai.android.domain.service.VideoGenerationTracker()
    }
    
    /**
     * Provides ModelsLab Image API for use in Image and Video generation
     */
    @Provides
    @Singleton
    fun provideModelsLabImageApi(
        @Named("modelslab") retrofit: Retrofit,
        dataStore: DataStore<Preferences>
    ): com.vortexai.android.domain.service.ModelsLabImageApi {
        // Based on its usage inside ImageGenerationService, it acts as a standalone class holding API calls
        // In the vortex architecture, usually we just inject it directly without passing retrofit if it handles its own okhttp requests. Let's see if parameterless const works.
        return com.vortexai.android.domain.service.ModelsLabImageApi(dataStore = dataStore)
    }
    
    /**
     * Provides VideoGenerationService for animating character avatars
     */
    @Provides
    @Singleton
    fun provideVideoGenerationService(
        tracker: com.vortexai.android.domain.service.VideoGenerationTracker,
        modelsLabApi: com.vortexai.android.domain.service.ModelsLabImageApi,
        settingsDataStore: com.vortexai.android.ui.screens.settings.managers.SettingsDataStore,
        logger: com.vortexai.android.domain.service.GenerationLogger
    ): com.vortexai.android.domain.service.VideoGenerationService {
        return com.vortexai.android.domain.service.VideoGenerationService(tracker, modelsLabApi, settingsDataStore, logger)
    }
} 
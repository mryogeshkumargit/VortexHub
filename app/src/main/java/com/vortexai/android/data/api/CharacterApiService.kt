package com.vortexai.android.data.api

import com.vortexai.android.data.models.*
import retrofit2.Response
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * API service for character-related operations
 * Uses mobile-optimized endpoints to prevent crashes with large data
 */
interface CharacterApiService {
    
    /**
     * Get characters with mobile optimization to prevent large data crashes
     */
    @GET("api/characters")
    suspend fun getCharacters(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("mobile") mobile: Boolean = true  // Always use mobile optimization
    ): Response<CharacterListResponse>
    
    /**
     * Get character by ID (full data for individual character view)
     */
    @GET("api/characters/{id}")
    suspend fun getCharacter(
        @Path("id") characterId: String
    ): Response<Character>
    
    /**
     * Get featured characters with mobile optimization
     */
    @GET("api/characters")
    suspend fun getFeaturedCharacters(
        @Query("featured") featured: Boolean = true,
        @Query("limit") limit: Int = 10,
        @Query("mobile") mobile: Boolean = true
    ): Response<CharacterListResponse>
    
    /**
     * Get popular characters with mobile optimization
     */
    @GET("api/characters")
    suspend fun getPopularCharacters(
        @Query("sort") sort: String = "popular",
        @Query("limit") limit: Int = 10,
        @Query("mobile") mobile: Boolean = true
    ): Response<CharacterListResponse>
    
    /**
     * Search characters with mobile optimization
     */
    @GET("api/characters")
    suspend fun searchCharacters(
        @Query("search") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("mobile") mobile: Boolean = true
    ): Response<CharacterListResponse>
    
    /**
     * Get characters by category with mobile optimization
     */
    @GET("api/characters")
    suspend fun getCharactersByCategory(
        @Query("category") category: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("mobile") mobile: Boolean = true
    ): Response<CharacterListResponse>
    
    /**
     * Get favorite characters (requires authentication)
     */
    @GET("api/characters/favorites")
    suspend fun getFavoriteCharacters(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<CharacterListResponse>
    
    /**
     * Toggle favorite status
     */
    @POST("api/characters/{id}/favorite")
    suspend fun toggleFavorite(
        @Header("Authorization") token: String,
        @Path("id") characterId: String,
        @Body request: FavoriteCharacterRequest
    ): Response<ApiResponse<Unit>>
    
    /**
     * Get my characters (requires authentication)
     */
    @GET("api/characters/my")
    suspend fun getMyCharacters(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<CharacterListResponse>
    
    /**
     * Create new character (requires authentication)
     */
    @POST("api/characters")
    suspend fun createCharacter(
        @Header("Authorization") token: String,
        @Body character: CreateCharacterRequest
    ): Response<Character>
    
    /**
     * Update character (requires authentication)
     */
    @PUT("api/characters/{id}")
    suspend fun updateCharacter(
        @Header("Authorization") token: String,
        @Path("id") characterId: String,
        @Body character: UpdateCharacterRequest
    ): Response<Character>
    
    /**
     * Delete character (requires authentication)
     */
    @DELETE("api/characters/{id}")
    suspend fun deleteCharacter(
        @Header("Authorization") token: String,
        @Path("id") characterId: String
    ): Response<ApiResponse<Unit>>
    
    /**
     * Rate character
     */
    @POST("api/characters/{id}/rate")
    suspend fun rateCharacter(
        @Path("id") characterId: String,
        @Body request: RateCharacterRequest
    ): Response<ApiResponse<Unit>>
    
    /**
     * Upload character avatar
     */
    @Multipart
    @POST("api/characters/{id}/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Path("id") characterId: String,
        @Part avatar: okhttp3.MultipartBody.Part
    ): Response<ApiResponse<Unit>>
    
    /**
     * Generate character avatar using AI
     */
    @POST("api/characters/{id}/generate-avatar")
    suspend fun generateAvatar(
        @Header("Authorization") token: String,
        @Path("id") characterId: String,
        @Body request: GenerateAvatarRequest
    ): Response<ApiResponse<Unit>>
} 
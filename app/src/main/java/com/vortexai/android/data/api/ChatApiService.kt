package com.vortexai.android.data.api

import com.vortexai.android.data.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Chat API service interface
 * Defines all chat-related network endpoints
 */
interface ChatApiService {
    
    /**
     * Get list of conversations
     */
    @GET("conversations")
    suspend fun getConversations(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ConversationResponseListResponse>
    
    /**
     * Create new conversation
     */
    @POST("conversations")
    suspend fun createConversation(
        @Header("Authorization") token: String,
        @Body request: CreateConversationRequest
    ): Response<ApiResponse<ConversationResponse>>
    
    /**
     * Get conversation by ID
     */
    @GET("conversations/{id}")
    suspend fun getConversation(
        @Header("Authorization") token: String,
        @Path("id") conversationId: String
    ): Response<ApiResponse<ConversationResponse>>
    
    /**
     * Delete conversation
     */
    @DELETE("conversations/{id}")
    suspend fun deleteConversation(
        @Header("Authorization") token: String,
        @Path("id") conversationId: String
    ): Response<ApiResponse<Unit>>
    
    /**
     * Get messages for a conversation
     */
    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("id") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<MessageResponseListResponse>
    
    /**
     * Send message
     */
    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("id") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<ApiResponse<MessageResponse>>
    
    /**
     * Edit message
     */
    @PUT("messages/{id}")
    suspend fun editMessage(
        @Header("Authorization") token: String,
        @Path("id") messageId: String,
        @Body content: Map<String, String>
    ): Response<ApiResponse<MessageResponse>>
    
    /**
     * Delete message
     */
    @DELETE("messages/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Path("id") messageId: String
    ): Response<ApiResponse<Unit>>
    
    /**
     * Get character response (AI generation)
     */
    @POST("conversations/{id}/generate")
    suspend fun generateCharacterResponse(
        @Header("Authorization") token: String,
        @Path("id") conversationId: String,
        @Body request: Map<String, Any> = emptyMap()
    ): Response<ApiResponse<MessageResponse>>
    
    /**
     * Update typing status
     */
    @POST("conversations/{id}/typing")
    suspend fun updateTypingStatus(
        @Header("Authorization") token: String,
        @Path("id") conversationId: String,
        @Body status: Map<String, Boolean>
    ): Response<ApiResponse<Unit>>
    
    /**
     * Pin/unpin conversation
     */
    @POST("conversations/{id}/pin")
    suspend fun toggleConversationPin(
        @Header("Authorization") token: String,
        @Path("id") conversationId: String,
        @Body pinStatus: Map<String, Boolean>
    ): Response<ApiResponse<Unit>>
    
    /**
     * Get chat statistics
     */
    @GET("chat/statistics")
    suspend fun getChatStatistics(
        @Header("Authorization") token: String
    ): Response<ApiResponse<ChatStatistics>>
} 

package com.vortexai.android.data.database.dao

import androidx.room.*
import com.vortexai.android.data.models.UserSession
import kotlinx.coroutines.flow.Flow


@Dao
interface UserSessionDao {
    
    // Basic CRUD Operations
    @Query("SELECT * FROM user_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): UserSession?
    
    @Query("SELECT * FROM user_sessions WHERE sessionToken = :sessionToken")
    suspend fun getSessionByToken(sessionToken: String): UserSession?
    
    @Query("SELECT * FROM user_sessions WHERE sessionToken = :sessionToken")
    fun getSessionByTokenFlow(sessionToken: String): Flow<UserSession?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSession)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<UserSession>)
    
    @Update
    suspend fun updateSession(session: UserSession)
    
    @Delete
    suspend fun deleteSession(session: UserSession)
    
    @Query("DELETE FROM user_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
    
    // User-based queries
    @Query("SELECT * FROM user_sessions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSessionsByUser(userId: String): Flow<List<UserSession>>
    
    @Query("SELECT * FROM user_sessions WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getSessionsByUserSync(userId: String): List<UserSession>
    
    @Query("SELECT * FROM user_sessions WHERE userId = :userId AND isActive = 1 ORDER BY lastAccessedAt DESC")
    suspend fun getActiveSessionsByUser(userId: String): List<UserSession>
    
    // Active sessions
    @Query("SELECT * FROM user_sessions WHERE isActive = 1 ORDER BY lastAccessedAt DESC")
    fun getActiveSessions(): Flow<List<UserSession>>
    
    @Query("SELECT * FROM user_sessions WHERE isActive = 1 ORDER BY lastAccessedAt DESC")
    suspend fun getActiveSessionsSync(): List<UserSession>
    
    // Session validation
    @Query("SELECT * FROM user_sessions WHERE sessionToken = :sessionToken AND isActive = 1 AND expiresAt > :currentTime")
    suspend fun getValidSession(sessionToken: String, currentTime: Long = System.currentTimeMillis()): UserSession?
    
    // Statistics
    @Query("SELECT COUNT(*) FROM user_sessions WHERE userId = :userId")
    suspend fun getSessionCountByUser(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM user_sessions WHERE isActive = 1")
    suspend fun getActiveSessionCount(): Int
    
    // Cleanup operations
    @Query("DELETE FROM user_sessions WHERE userId = :userId")
    suspend fun deleteSessionsByUser(userId: String)
    
    @Query("DELETE FROM user_sessions WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredSessions(currentTime: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM user_sessions WHERE isActive = 0")
    suspend fun deleteInactiveSessions()
    
    @Query("DELETE FROM user_sessions")
    suspend fun deleteAllSessions()
    
    @Query("SELECT COUNT(*) FROM user_sessions")
    suspend fun getTotalSessionCount(): Int
    
    // Session management
    @Query("UPDATE user_sessions SET isActive = 0 WHERE userId = :userId AND sessionToken != :currentSessionToken")
    suspend fun deactivateOtherUserSessions(userId: String, currentSessionToken: String)
    
    @Query("UPDATE user_sessions SET lastAccessedAt = :accessTime WHERE sessionToken = :sessionToken")
    suspend fun updateSessionAccess(sessionToken: String, accessTime: Long = System.currentTimeMillis())
}

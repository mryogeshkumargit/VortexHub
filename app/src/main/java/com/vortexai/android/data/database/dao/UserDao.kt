package com.vortexai.android.data.database.dao

import androidx.room.*
import com.vortexai.android.data.models.User
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface UserDao {
    
    // Basic CRUD Operations
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?
    
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdFlow(id: String): Flow<User?>
    
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
    
    // User queries
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveUsersSync(): List<User>
    
    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentUsers(limit: Int = 20): List<User>
    
    // Statistics
    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    suspend fun getActiveUserCount(): Int
    
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getTotalUserCount(): Int
    
    // Search functionality
    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchUsers(query: String, limit: Int = 50): List<User>
    
    // Cleanup operations
    @Query("DELETE FROM users WHERE isActive = 0")
    suspend fun deleteInactiveUsers()
}

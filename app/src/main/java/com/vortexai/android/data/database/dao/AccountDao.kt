package com.vortexai.android.data.database.dao

import androidx.room.*
import com.vortexai.android.data.models.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    
    @Query("SELECT * FROM accounts LIMIT 1")
    fun getAccount(): Flow<Account?>
    
    @Query("SELECT * FROM accounts LIMIT 1")
    suspend fun getAccountSync(): Account?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)
    
    @Update
    suspend fun updateAccount(account: Account)
    
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
    
    @Query("UPDATE accounts SET accessToken = :token WHERE id = :accountId")
    suspend fun updateAccessToken(accountId: String, token: String)
    
    @Query("UPDATE accounts SET refreshToken = :token WHERE id = :accountId")
    suspend fun updateRefreshToken(accountId: String, token: String)
}

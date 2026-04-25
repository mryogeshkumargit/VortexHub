package com.vortexai.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vortexai.android.data.models.GeneratedImage

@Dao
interface GeneratedImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImage)

    @Query("SELECT * FROM generated_images ORDER BY timestamp DESC")
    suspend fun getAllImages(): List<GeneratedImage>

    @Query("DELETE FROM generated_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: String)
    
    @Query("DELETE FROM generated_images")
    suspend fun deleteAllGeneratedImages()
} 
package com.popcorntime.android.data.db.dao

import androidx.room.*
import com.popcorntime.android.data.db.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Query("SELECT * FROM downloads")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("DELETE FROM downloads WHERE imdbId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM downloads WHERE completedAt IS NULL")
    suspend fun deleteIncomplete()

    @Query("UPDATE downloads SET filePath = :filePath, completedAt = :completedAt WHERE imdbId = :imdbId")
    suspend fun markComplete(imdbId: String, filePath: String?, completedAt: Long)
}

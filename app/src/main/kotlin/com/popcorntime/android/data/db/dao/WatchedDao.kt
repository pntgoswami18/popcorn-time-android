package com.popcorntime.android.data.db.dao

import androidx.room.*
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedDao {
    @Query("SELECT imdbId FROM watched")
    fun observeAll(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM watched WHERE imdbId = :imdbId)")
    suspend fun isWatched(imdbId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchedEntity)

    @Query("DELETE FROM watched WHERE imdbId = :imdbId")
    suspend fun delete(imdbId: String)
}

@Dao
interface BookmarkedDao {
    @Query("SELECT imdbId FROM bookmarked")
    fun observeAll(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked WHERE imdbId = :imdbId)")
    suspend fun isBookmarked(imdbId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkedEntity)

    @Query("DELETE FROM bookmarked WHERE imdbId = :imdbId")
    suspend fun delete(imdbId: String)
}

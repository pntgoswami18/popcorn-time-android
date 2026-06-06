package com.popcorntime.android.data.db.dao

import androidx.room.*
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.LibraryItemEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import com.popcorntime.android.data.db.entity.WatchlistEntity
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

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT COUNT(*) > 0 FROM watchlist WHERE imdbId = :imdbId")
    suspend fun isInWatchlist(imdbId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE imdbId = :imdbId")
    suspend fun delete(imdbId: String)
}

@Dao
interface LibraryItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LibraryItemEntity)

    @Query("SELECT * FROM library_items WHERE imdbId IN (:ids)")
    fun getByIds(ids: List<String>): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE imdbId = :imdbId")
    suspend fun getById(imdbId: String): LibraryItemEntity?
}

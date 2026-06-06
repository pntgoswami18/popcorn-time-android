package com.popcorntime.android.domain.repository

import com.popcorntime.android.domain.model.LibraryItem
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeFavourites(): Flow<List<LibraryItem>>
    suspend fun isFavourited(imdbId: String): Boolean
    suspend fun toggleFavourite(imdbId: String, metadata: LibraryItem)

    fun observeWatchlist(): Flow<List<LibraryItem>>
    suspend fun isInWatchlist(imdbId: String): Boolean
    suspend fun addToWatchlist(imdbId: String, metadata: LibraryItem)
    suspend fun removeFromWatchlist(imdbId: String)

    fun observeWatched(): Flow<List<LibraryItem>>
    suspend fun isWatched(imdbId: String): Boolean
    suspend fun markWatched(imdbId: String, metadata: LibraryItem)
    suspend fun unmarkWatched(imdbId: String)

    suspend fun syncFromTrakt()
    fun isTraktConnected(): Flow<Boolean>
}

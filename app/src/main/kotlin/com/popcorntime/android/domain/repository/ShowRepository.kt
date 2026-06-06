package com.popcorntime.android.domain.repository

import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.domain.model.ShowFilter
import kotlinx.coroutines.flow.Flow

interface ShowRepository {
    suspend fun getShows(filter: ShowFilter): Result<List<Show>>
    suspend fun getShowDetail(imdbId: String, type: ContentType): Result<Show>
    fun observeWatched(): Flow<Set<String>>
    fun observeBookmarked(): Flow<Set<String>>
    suspend fun toggleWatched(imdbId: String)
    suspend fun toggleBookmarked(imdbId: String)
}

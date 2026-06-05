package com.popcorntime.android.domain.repository

import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.MovieFilter
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun getMovies(filter: MovieFilter): Result<List<Movie>>
    suspend fun getMovieDetail(imdbId: String): Result<Movie>
    fun observeWatched(): Flow<Set<String>>
    fun observeBookmarked(): Flow<Set<String>>
    suspend fun toggleWatched(imdbId: String)
    suspend fun toggleBookmarked(imdbId: String)
    suspend fun isWatched(imdbId: String): Boolean
    suspend fun isBookmarked(imdbId: String): Boolean
}

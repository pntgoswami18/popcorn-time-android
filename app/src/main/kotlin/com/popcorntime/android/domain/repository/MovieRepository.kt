package com.popcorntime.android.domain.repository

import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.MovieFilter
import kotlinx.coroutines.flow.Flow

/**
 * One page of browse/search results.
 *
 * @param movies the page after client-side filters (quality/min-rating) ran.
 * @param rawCount how many movies the server actually returned for this page,
 *   BEFORE client-side filtering. Pagination must key off this: a page can
 *   filter down to empty while the server still has more pages.
 */
data class MoviePage(
    val movies: List<Movie>,
    val rawCount: Int,
)

interface MovieRepository {
    suspend fun getMovies(filter: MovieFilter): Result<MoviePage>
    suspend fun getMovieDetail(imdbId: String): Result<Movie>
    fun observeWatched(): Flow<Set<String>>
    fun observeBookmarked(): Flow<Set<String>>
    suspend fun toggleWatched(imdbId: String)
    suspend fun toggleBookmarked(imdbId: String)
    suspend fun isWatched(imdbId: String): Boolean
    suspend fun isBookmarked(imdbId: String): Boolean
}

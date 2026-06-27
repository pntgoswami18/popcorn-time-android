package com.popcorntime.android.data.repository

import com.popcorntime.android.data.api.JackettApiService
import com.popcorntime.android.data.api.MovieApiService
import com.popcorntime.android.data.api.applyClientSideFilters
import com.popcorntime.android.data.api.toDomain
import com.popcorntime.android.data.api.toMovieTorrent
import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import com.popcorntime.android.data.sources.TorrentSourcePrefs
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.MovieFilter
import com.popcorntime.android.domain.model.Torrent
import com.popcorntime.android.domain.model.TorrentSource
import com.popcorntime.android.domain.repository.MoviePage
import com.popcorntime.android.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: MovieApiService,
    private val watchedDao: WatchedDao,
    private val bookmarkedDao: BookmarkedDao,
    private val sourcePrefs: TorrentSourcePrefs,
    private val jackettApi: JackettApiService,
) : MovieRepository {

    override suspend fun getMovies(filter: MovieFilter): Result<MoviePage> = safeRunCatching {
        if (sourcePrefs.getMovieSource() == TorrentSource.JACKETT) {
            val baseUrl = sourcePrefs.getJackettUrl()
            val apiKey = sourcePrefs.getJackettApiKey()
            if (baseUrl.isNotBlank() && apiKey.isNotBlank()) {
                val query = filter.queryTerm.ifBlank { filter.genre.takeIf { it != "All Genre" } ?: "popular" }
                val results = jackettApi.searchMovies(query, apiKey, baseUrl)
                if (results.isNotEmpty()) {
                    val movies = results.map { dto ->
                        val torrent = dto.toMovieTorrent()
                        Movie(
                            id = dto.infoHash.ifBlank { dto.title }.hashCode().and(0x7FFFFFFF),
                            imdbId = "jackett:${dto.infoHash.ifBlank { dto.title.slugify() }}",
                            title = dto.title,
                            year = 0,
                            rating = 0.0,
                            runtime = 0,
                            genres = emptyList(),
                            synopsis = "",
                            posterUrl = "",
                            coverUrl = "",
                            backdropUrl = "",
                            trailerUrl = null,
                            certification = "",
                            language = "",
                            torrents = mapOf(torrent.quality to torrent),
                        )
                    }
                    return@safeRunCatching MoviePage(movies = movies, rawCount = movies.size)
                }
            }
        }
        // Fall through to the Butter / popcorn-ru mirrors (same servers popcorn-desktop uses).
        // quality / minimumRating have no server-side equivalent → filtered client-side.
        // rawCount carries the pre-filter page size so pagination doesn't stop early
        // when a whole server page gets filtered out.
        val raw = api.listMovies(filter)
            .filter { it.imdbId.isNotBlank() || it.id.isNotBlank() }
            .map { it.toDomain() }
        MoviePage(
            movies = raw.applyClientSideFilters(filter.quality, filter.minimumRating),
            rawCount = raw.size,
        )
    }

    override suspend fun getMovieDetail(imdbId: String): Result<Movie> {
        if (imdbId.startsWith("jackett:")) {
            // Jackett-sourced movies don't have a Butter counterpart
            return Result.failure(UnsupportedOperationException("Jackett movie detail not available"))
        }
        return safeRunCatching {
            api.getMovieByImdbId(imdbId).toDomain()
        }
    }

    override fun observeWatched(): Flow<Set<String>> =
        watchedDao.observeAll().map { it.toSet() }

    override fun observeBookmarked(): Flow<Set<String>> =
        bookmarkedDao.observeAll().map { it.toSet() }

    override suspend fun toggleWatched(imdbId: String) {
        if (watchedDao.isWatched(imdbId)) watchedDao.delete(imdbId)
        else watchedDao.insert(WatchedEntity(imdbId))
    }

    override suspend fun toggleBookmarked(imdbId: String) {
        if (bookmarkedDao.isBookmarked(imdbId)) bookmarkedDao.delete(imdbId)
        else bookmarkedDao.insert(BookmarkedEntity(imdbId))
    }

    override suspend fun isWatched(imdbId: String) = watchedDao.isWatched(imdbId)
    override suspend fun isBookmarked(imdbId: String) = bookmarkedDao.isBookmarked(imdbId)
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Like [runCatching] but re-throws [CancellationException] so coroutine cancellation
 * is not swallowed.
 */
private inline fun <T> safeRunCatching(block: () -> T): Result<T> =
    runCatching(block).also { result ->
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

// ── Mappers ──────────────────────────────────────────────────────────────────
// Butter DTO → domain mapping lives in data/api/ButterMappers.kt (unit-testable).

private fun String.slugify(): String =
    lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

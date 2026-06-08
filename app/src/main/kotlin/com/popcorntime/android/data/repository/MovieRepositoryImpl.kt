package com.popcorntime.android.data.repository

import com.popcorntime.android.data.api.MovieApiService
import com.popcorntime.android.data.api.dto.YtsMovieDto
import com.popcorntime.android.data.api.dto.YtsTorrentDto
import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.MovieFilter
import com.popcorntime.android.domain.model.Torrent
import com.popcorntime.android.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: MovieApiService,
    private val watchedDao: WatchedDao,
    private val bookmarkedDao: BookmarkedDao,
) : MovieRepository {

    override suspend fun getMovies(filter: MovieFilter): Result<List<Movie>> = runCatching {
        val response = api.listMovies(filter)
        response.data.movies.orEmpty().map { it.toDomain() }
    }

    override suspend fun getMovieDetail(imdbId: String): Result<Movie> = runCatching {
        api.getMovieByImdbId(imdbId).toDomain()
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

// ── Mappers ──────────────────────────────────────────────────────────────────

private fun YtsMovieDto.toDomain() = Movie(
    id = id,
    imdbId = imdbCode,
    title = titleEnglish.ifBlank { title },
    year = year,
    rating = rating,
    runtime = runtime,
    genres = genres.orEmpty(),
    synopsis = descriptionFull.ifBlank { synopsis.ifBlank { summary } },
    posterUrl = largeCoverImage,
    coverUrl = mediumCoverImage,
    backdropUrl = backgroundImageOriginal.ifBlank { backgroundImage },
    trailerUrl = if (ytTrailerCode.isNotBlank()) "https://www.youtube.com/watch?v=$ytTrailerCode" else null,
    certification = mpaRating,
    language = language,
    torrents = torrents.orEmpty().associate { it.quality to it.toDomain(slug) },
)

private fun YtsTorrentDto.toDomain(slug: String) = Torrent(
    url = url,
    magnet = buildMagnet(hash, slug, quality, type),
    quality = quality,
    type = type,
    size = sizeBytes,
    fileSize = size,
    seeds = seeds,
    peers = peers,
    hash = hash,
)

private fun buildMagnet(hash: String, slug: String, quality: String, type: String): String {
    val name = slug.split("-").joinToString(".") { it.replaceFirstChar(Char::uppercaseChar) }
    // Include well-known public trackers so libtorrent can find peers even before DHT resolves.
    val trackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://tracker.dler.org:6969/announce",
        "udp://open.tracker.cl:1337/announce",
    )
    val trParams = trackers.joinToString("") { "&tr=$it" }
    return "magnet:?xt=urn:btih:$hash&dn=$name.$quality.$type-YTS$trParams"
}

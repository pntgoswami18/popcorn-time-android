package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.ButterMovieDto
import com.popcorntime.android.data.api.dto.ButterTorrentDto
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.Torrent
import com.popcorntime.android.domain.model.TorrentSource

/**
 * Mappers and client-side filters for the Butter / popcorn-ru movie API.
 * Kept top-level (not private inside the repository) so they are unit-testable.
 */

fun ButterMovieDto.toDomain(): Movie = Movie(
    id = tmdbId ?: (imdbId.ifBlank { id }.hashCode() and 0x7FFFFFFF),
    imdbId = imdbId.ifBlank { id },
    title = title,
    year = year?.trim()?.toIntOrNull() ?: 0,
    rating = (rating?.percentage ?: 0.0) / 10.0,
    runtime = runtime?.trim()?.toIntOrNull() ?: 0,
    genres = genres.orEmpty().map { genre ->
        genre.split(" ").joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }
    },
    synopsis = synopsis,
    posterUrl = images?.poster.orEmpty().toHttps(),
    coverUrl = images?.banner.orEmpty().ifBlank { images?.fanart.orEmpty() }.toHttps(),
    backdropUrl = images?.fanart.orEmpty().ifBlank { images?.banner.orEmpty() }.toHttps(),
    trailerUrl = trailer?.takeIf { it.isNotBlank() },
    certification = certification,
    language = originalLanguage,
    torrents = flattenTorrents(),
)

/**
 * Butter torrents are keyed by language then quality. Prefer English torrents,
 * fall back to the first available language; result is quality -> [Torrent].
 * Entries whose URL is not a valid magnet link are dropped (see [toDomainOrNull]).
 */
fun ButterMovieDto.flattenTorrents(): Map<String, Torrent> {
    val byLanguage = torrents.orEmpty()
    val chosen = byLanguage["en"] ?: byLanguage.values.firstOrNull() ?: emptyMap()
    return chosen.entries
        .mapNotNull { (quality, dto) -> dto.toDomainOrNull(quality)?.let { quality to it } }
        .toMap()
}

/**
 * Maps a torrent entry to the domain model, validating the URL first: mirrors
 * are untrusted, so anything that is not a `magnet:` link with a parseable
 * btih infohash is dropped (returns null) rather than handed to the torrent engine.
 */
fun ButterTorrentDto.toDomainOrNull(qualityKey: String): Torrent? {
    val magnet = unescapeHtmlAmpersands(url)
    if (!magnet.startsWith("magnet:")) return null
    val infoHash = extractInfoHash(magnet)
    if (infoHash.isEmpty()) return null
    return Torrent(
        url = magnet,
        magnet = magnet,
        quality = quality ?: qualityKey,
        type = provider,
        size = size?.toLongOrNull() ?: 0L,
        fileSize = filesize.orEmpty(),
        seeds = seed,
        peers = peer,
        hash = infoHash,
        source = TorrentSource.YTS,
    )
}

/** The mirrors return magnet links with HTML-escaped separators (`&amp;`), sometimes double-encoded. */
fun unescapeHtmlAmpersands(url: String): String {
    var s = url
    var prev: String
    do { prev = s; s = s.replace("&amp;", "&") } while (s != prev)
    return s
}

/** The Butter API returns image URLs over plain HTTP; coerce to HTTPS so Android's
 *  network security policy (which blocks cleartext traffic) doesn't silently drop them. */
fun String.toHttps(): String = if (startsWith("http://")) replaceFirst("http://", "https://") else this

fun extractInfoHash(magnet: String): String =
    Regex("btih:([A-Fa-f0-9]{64}|[A-Fa-f0-9]{40}|[A-Za-z2-7]{32})", RegexOption.IGNORE_CASE)
        .find(magnet)?.groupValues?.get(1)?.uppercase().orEmpty()

/**
 * Client-side filters for options the Butter API has no server-side equivalent of.
 *
 * @param quality "All Quality" or a quality key like "1080p" — keeps movies having a torrent of that quality.
 * @param minimumRating 0..9 minimum rating on the 0-10 scale (rating.percentage / 10).
 */
fun List<Movie>.applyClientSideFilters(quality: String, minimumRating: Int): List<Movie> {
    var result = this
    if (quality != "All Quality" && quality.isNotBlank()) {
        result = result.filter { quality in it.torrents.keys }
    }
    if (minimumRating > 0) {
        result = result.filter { it.rating >= minimumRating }
    }
    return result
}

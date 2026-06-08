package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.JackettResultDto
import com.popcorntime.android.data.api.dto.JackettResultsResponse
import com.popcorntime.android.domain.model.EpisodeTorrent
import com.popcorntime.android.domain.model.Torrent
import com.popcorntime.android.domain.model.TorrentSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JackettApiService @Inject constructor(private val client: HttpClient) {

    suspend fun searchMovies(
        query: String,
        apiKey: String,
        baseUrl: String,
    ): List<JackettResultDto> = search(
        query = query,
        category = "2000",
        apiKey = apiKey,
        baseUrl = baseUrl,
    )

    suspend fun searchShows(
        query: String,
        season: Int?,
        episode: Int?,
        apiKey: String,
        baseUrl: String,
    ): List<JackettResultDto> {
        val seSuffix = if (season != null && episode != null) {
            " " + "S%02dE%02d".format(season, episode)
        } else ""
        val fullQuery = query + seSuffix
        return search(
            query = fullQuery,
            category = "5000",
            apiKey = apiKey,
            baseUrl = baseUrl,
        )
    }

    private suspend fun search(
        query: String,
        category: String,
        apiKey: String,
        baseUrl: String,
    ): List<JackettResultDto> {
        return try {
            val url = "${baseUrl.trimEnd('/')}/api/v2.0/indexers/all/results"
            client.get(url) {
                parameter("apikey", apiKey)
                parameter("Query", query)
                parameter("Category[]", category)
            }.body<JackettResultsResponse>().results
        } catch (e: Exception) {
            Timber.w(e, "JackettApiService: search failed for query=$query")
            emptyList()
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun String.detectQuality(): String {
    val fn = this.lowercase()
    return when {
        "2160p" in fn || "4k" in fn || "uhd" in fn -> "4K"
        "1080p" in fn -> "1080p"
        "720p" in fn -> "720p"
        "480p" in fn -> "480p"
        else -> "720p"
    }
}

fun JackettResultDto.toMovieTorrent(): Torrent {
    val quality = title.detectQuality()
    return Torrent(
        url = magnetUri.ifBlank { link },
        magnet = magnetUri,
        quality = quality,
        type = "web",
        size = size,
        fileSize = formatBytes(size),
        seeds = seeders,
        peers = peers,
        hash = infoHash,
        source = TorrentSource.JACKETT,
    )
}

fun JackettResultDto.toEpisodeTorrent(): EpisodeTorrent = EpisodeTorrent(
    url = magnetUri.ifBlank { link },
    seeds = seeders,
    peers = peers,
    provider = "Jackett",
)

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return "%.1f %s".format(value, units[unitIndex])
}

package com.popcorntime.android.data.api

import com.popcorntime.android.domain.model.EpisodeTorrent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches anime episode torrents from Nyaa.si via its public RSS feed.
 *
 * Nyaa is the primary public tracker for anime fansubs and encodes.
 * Category 1_2 = "Anime - English-translated".
 * No authentication required.
 */
@Singleton
class NyaaApiService @Inject constructor(private val client: HttpClient) {

    companion object {
        private const val NYAA = "https://nyaa.si"
        /** Max results per query (Nyaa RSS hard cap). */
        private const val PAGE_SIZE = 75
    }

    /**
     * Searches Nyaa for episodes of [showTitle] and returns a torrent index
     * keyed by season → episode → quality → best-seeded [EpisodeTorrent].
     *
     * Strategy: one broad query for the show title, then local SxxExx regex
     * filtering — avoids hammering Nyaa with one request per episode.
     */
    suspend fun fetchTorrentIndex(
        showTitle: String,
    ): Map<Int, Map<Int, Map<String, EpisodeTorrent>>> {
        val items = try {
            fetchRssItems(showTitle)
        } catch (e: Exception) {
            Timber.w(e, "NyaaApiService: RSS fetch failed for \"$showTitle\"")
            return emptyMap()
        }

        val sePattern = Regex("S(\\d{1,2})E(\\d{1,3})", RegexOption.IGNORE_CASE)
        // Also match the common anime format " - 01 " or "[01]"
        val epOnlyPattern = Regex("(?:[-\\s\\[])0*(\\d{1,3})(?:[\\]\\s]|$)")

        val index = mutableMapOf<Int, MutableMap<Int, MutableMap<String, EpisodeTorrent>>>()

        for (item in items) {
            val match = sePattern.find(item.title)
            val season: Int
            val episode: Int
            if (match != null) {
                season  = match.groupValues[1].toIntOrNull() ?: continue
                episode = match.groupValues[2].toIntOrNull() ?: continue
            } else {
                // Treat bare episode numbers as season 1 (common for anime)
                val epMatch = epOnlyPattern.find(item.title) ?: continue
                season  = 1
                episode = epMatch.groupValues[1].toIntOrNull() ?: continue
            }

            val quality = item.title.detectNyaaQuality()
            val torrent = EpisodeTorrent(
                url      = item.magnet.ifBlank { item.link },
                seeds    = item.seeders,
                peers    = item.leechers,
                provider = "Nyaa",
            )

            val byQuality = index.getOrPut(season) { mutableMapOf() }
                                 .getOrPut(episode) { mutableMapOf() }
            val existing = byQuality[quality]
            if (existing == null || torrent.seeds > existing.seeds) {
                byQuality[quality] = torrent
            }
        }

        return index
    }

    private suspend fun fetchRssItems(query: String): List<NyaaRssItem> {
        val raw = client.get("$NYAA/") {
            parameter("page",  "rss")
            parameter("q",     query)
            parameter("c",     "1_2")   // Anime – English-translated
            parameter("f",     "0")     // No filter
        }.body<String>()

        return parseRss(raw)
    }

    /** Minimal RSS/XML parser — avoids pulling in a full XML library. */
    private fun parseRss(xml: String): List<NyaaRssItem> {
        val items = mutableListOf<NyaaRssItem>()
        val itemBlocks = xml.split("<item>").drop(1)
        for (block in itemBlocks) {
            val title    = block.xmlText("title") ?: continue
            val link     = block.xmlText("link") ?: ""
            val magnet   = block.xmlText("nyaa:infoHash")
                ?.let { hash -> "magnet:?xt=urn:btih:$hash" } ?: ""
            val seeders  = block.xmlText("nyaa:seeders")?.toIntOrNull() ?: 0
            val leechers = block.xmlText("nyaa:leechers")?.toIntOrNull() ?: 0
            items += NyaaRssItem(title, link, magnet, seeders, leechers)
        }
        return items
    }

    private fun String.xmlText(tag: String): String? {
        val start = indexOf("<$tag>").takeIf { it >= 0 }?.plus("<$tag>".length) ?: return null
        val end   = indexOf("</$tag>", start).takeIf { it >= 0 } ?: return null
        return substring(start, end).trim()
            .removePrefix("<![CDATA[").removeSuffix("]]>").trim()
            .ifBlank { null }
    }
}

private data class NyaaRssItem(
    val title: String,
    val link: String,
    val magnet: String,
    val seeders: Int,
    val leechers: Int,
)

private fun String.detectNyaaQuality(): String {
    val fn = this.lowercase()
    return when {
        "2160p" in fn || "4k" in fn || "uhd" in fn -> "2160p"
        "1080p" in fn                               -> "1080p"
        "720p"  in fn                               -> "720p"
        "480p"  in fn                               -> "480p"
        else                                        -> "720p"
    }
}

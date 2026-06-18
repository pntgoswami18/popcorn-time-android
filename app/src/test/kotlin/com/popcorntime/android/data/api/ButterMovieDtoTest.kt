package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.ButterMovieDto
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.Torrent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deserialization + mapping tests for the Butter / popcorn-ru movie API.
 * The JSON below is a trimmed real response captured from https://fusme.link/movies/1
 * (sort=trending) on 2026-06-12.
 */
class ButterMovieDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Trimmed real sample: magnet shortened, but the HTML-escaped `&amp;` separators
    // and overall structure are exactly what the live mirrors return.
    private val sampleListJson = """
    [
      {
        "_id": "tt26443616",
        "imdb_id": "tt26443616",
        "tmdb_id": 1327819,
        "title": "Hoppers",
        "year": "2026",
        "original_language": "en",
        "exist_translations": ["en", "hr", "ru", "ua"],
        "contextLocale": "en",
        "synopsis": "Scientists have discovered how to 'hop' human consciousness into lifelike robotic animals.",
        "runtime": "105",
        "released": 1772582400,
        "certification": "PG",
        "torrents": {
          "en": {
            "1080p": {
              "url": "magnet:?xt=urn:btih:B6E0A23A7E9239DBF5B4883887DD636311B486FC&amp;dn=Hoppers+2026+1080p&amp;tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
              "provider": "T1337x",
              "source": "https://1337x.to/torrent/6612301/",
              "title": "Hoppers 2026 1080p DCP Line Audio H264-DJT",
              "quality": "1080p",
              "seed": 2069,
              "peer": 4555,
              "size": "6764573491",
              "filesize": "6.3 GB"
            }
          }
        },
        "trailer": "http://www.youtube.com/watch?v=PypDSyIRRSs",
        "genres": ["adventure", "animation", "comedy", "family", "science fiction"],
        "images": {
          "poster": "http://image.tmdb.org/t/p/w500/xjtWQ2CL1mpmMNwuU5HeS4Iuwuu.jpg",
          "fanart": "http://image.tmdb.org/t/p/w500/u53UYu5XG2hNgWGvs3xGhAVzypl.jpg",
          "banner": "http://image.tmdb.org/t/p/w500/xjtWQ2CL1mpmMNwuU5HeS4Iuwuu.jpg"
        },
        "rating": { "percentage": 76, "watching": 335, "votes": 818, "loved": 0, "hated": 0 }
      },
      {
        "_id": "tt8036976",
        "imdb_id": "tt8036976",
        "tmdb_id": 1198994,
        "title": "Send Help",
        "year": "2026",
        "original_language": "en",
        "exist_translations": ["en", "it"],
        "contextLocale": "en",
        "synopsis": "Two colleagues become stranded on a deserted island.",
        "runtime": "113",
        "released": 1769040000,
        "certification": "R",
        "torrents": {
          "en": {
            "1080p": {
              "url": "magnet:?xt=urn:btih:190B6807C473671D31F53A0276197AC83B15D8BC&amp;dn=Send+Help",
              "provider": "T1337x",
              "quality": "1080p",
              "seed": 116,
              "peer": 142,
              "size": "1073741824",
              "filesize": "1 GB"
            },
            "720p": {
              "url": "magnet:?xt=urn:btih:A9F845CA6ACD516E0CDFF0088648B7C3C14DAE61&amp;dn=Send+Help+720p",
              "provider": "T1337x",
              "quality": "720p",
              "seed": 8,
              "peer": 8,
              "size": "1073741824",
              "filesize": "1 GB"
            }
          }
        },
        "trailer": "http://www.youtube.com/watch?v=cOmnmFz_Uzs",
        "genres": ["comedy", "horror", "thriller"],
        "images": {
          "poster": "http://image.tmdb.org/t/p/w500/mjkS2iAgWj3ik1DTjvI15nHZ7yl.jpg",
          "fanart": "http://image.tmdb.org/t/p/w500/bpp58yHuQmpt6xwggI63mVRw7po.jpg",
          "banner": "http://image.tmdb.org/t/p/w500/mjkS2iAgWj3ik1DTjvI15nHZ7yl.jpg"
        },
        "rating": { "percentage": 71, "watching": 171, "votes": 1271, "loved": 0, "hated": 0 }
      }
    ]
    """.trimIndent()

    private fun decodeList(): List<ButterMovieDto> = json.decodeFromString(sampleListJson)

    // ── Deserialization ───────────────────────────────────────────────────────

    @Test
    fun `list response deserializes as plain array`() {
        val movies = decodeList()
        assertEquals(2, movies.size)
        assertEquals("tt26443616", movies[0].imdbId)
        assertEquals("Hoppers", movies[0].title)
        assertEquals("2026", movies[0].year)
        assertEquals(76.0, movies[0].rating!!.percentage, 0.0)
        assertEquals(1, movies[0].torrents!!["en"]!!.size)
        assertEquals(2069, movies[0].torrents!!["en"]!!["1080p"]!!.seed)
    }

    @Test
    fun `missing optional fields fall back to defaults`() {
        val movie: ButterMovieDto = json.decodeFromString(
            """{"_id": "tt0000001", "imdb_id": "tt0000001", "title": "Bare Minimum"}"""
        )
        assertNull(movie.year)
        assertNull(movie.images)
        assertNull(movie.rating)
        assertNull(movie.torrents)
        assertEquals(emptyMap<String, Torrent>(), movie.flattenTorrents())
        val domain = movie.toDomain()
        assertEquals(0, domain.year)
        assertEquals(0.0, domain.rating, 0.0)
        assertEquals("", domain.posterUrl)
    }

    @Test
    fun `unknown keys are ignored`() {
        val movie: ButterMovieDto = json.decodeFromString(
            """{"_id": "tt1", "title": "X", "some_future_field": {"a": 1}}"""
        )
        assertEquals("X", movie.title)
    }

    // ── Domain mapping ────────────────────────────────────────────────────────

    @Test
    fun `toDomain maps year string, rating percentage and images`() {
        val movie = decodeList()[0].toDomain()
        assertEquals(2026, movie.year)
        assertEquals(7.6, movie.rating, 0.001)
        assertEquals(105, movie.runtime)
        assertEquals(1327819, movie.id) // tmdb id
        assertEquals("tt26443616", movie.imdbId)
        assertEquals("https://image.tmdb.org/t/p/w500/xjtWQ2CL1mpmMNwuU5HeS4Iuwuu.jpg", movie.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w500/u53UYu5XG2hNgWGvs3xGhAVzypl.jpg", movie.backdropUrl)
        assertEquals("PG", movie.certification)
        assertEquals("http://www.youtube.com/watch?v=PypDSyIRRSs", movie.trailerUrl)
        assertTrue("Science Fiction" in movie.genres)
    }

    @Test
    fun `torrent map flattens by quality with unescaped magnet and parsed size`() {
        val movie = decodeList()[1].toDomain()
        assertEquals(setOf("1080p", "720p"), movie.torrents.keys)
        val t = movie.torrents["1080p"]!!
        assertTrue(t.magnet.startsWith("magnet:?xt=urn:btih:190B6807C473671D31F53A0276197AC83B15D8BC&dn="))
        assertTrue("&amp;" !in t.magnet)
        assertEquals("190B6807C473671D31F53A0276197AC83B15D8BC", t.hash)
        assertEquals(1073741824L, t.size)
        assertEquals("1 GB", t.fileSize)
        assertEquals(116, t.seeds)
        assertEquals(142, t.peers)
    }

    @Test
    fun `non-english torrents are used when english is absent`() {
        val movie: ButterMovieDto = json.decodeFromString(
            """
            {"_id": "tt2", "title": "Foreign", "torrents": {"it": {"720p": {
              "url": "magnet:?xt=urn:btih:A9F845CA6ACD516E0CDFF0088648B7C3C14DAE61",
              "seed": 1, "peer": 2, "size": "10", "filesize": "10 B"
            }}}}
            """.trimIndent()
        )
        val torrents = movie.flattenTorrents()
        assertEquals(setOf("720p"), torrents.keys)
        assertEquals("720p", torrents["720p"]!!.quality) // falls back to map key
    }

    @Test
    fun `non-magnet torrent urls are dropped`() {
        val movie: ButterMovieDto = json.decodeFromString(
            """
            {"_id": "tt3", "title": "Mixed", "torrents": {"en": {
              "1080p": {"url": "https://evil.example.com/payload.exe", "seed": 1, "peer": 1},
              "720p": {"url": "magnet:?xt=urn:btih:A9F845CA6ACD516E0CDFF0088648B7C3C14DAE61", "seed": 1, "peer": 1}
            }}}
            """.trimIndent()
        )
        val torrents = movie.flattenTorrents()
        assertEquals(setOf("720p"), torrents.keys)
    }

    @Test
    fun `magnet urls without a parseable btih infohash are dropped`() {
        val movie: ButterMovieDto = json.decodeFromString(
            """
            {"_id": "tt4", "title": "BadHash", "torrents": {"en": {
              "1080p": {"url": "magnet:?xt=urn:btih:NOTAVALIDHASH&amp;dn=x", "seed": 1, "peer": 1},
              "720p": {"url": "magnet:?dn=no-infohash-at-all", "seed": 1, "peer": 1},
              "2160p": {"url": "", "seed": 1, "peer": 1}
            }}}
            """.trimIndent()
        )
        assertEquals(emptyMap<String, Torrent>(), movie.flattenTorrents())
    }

    @Test
    fun `toDomainOrNull keeps valid magnets and extracts the infohash`() {
        val movie: ButterMovieDto = json.decodeFromString(
            """
            {"_id": "tt5", "title": "Good", "torrents": {"en": {
              "1080p": {"url": "magnet:?xt=urn:btih:b6e0a23a7e9239dbf5b4883887dd636311b486fc&amp;dn=x", "seed": 1, "peer": 1}
            }}}
            """.trimIndent()
        )
        val torrent = movie.flattenTorrents()["1080p"]!!
        assertEquals("B6E0A23A7E9239DBF5B4883887DD636311B486FC", torrent.hash)
    }

    // ── Client-side filters ───────────────────────────────────────────────────

    private fun movieWith(rating: Double, qualities: Set<String>): Movie {
        val torrents = qualities.associateWith {
            Torrent(
                url = "", magnet = "", quality = it, type = "", size = 0,
                fileSize = "", seeds = 0, peers = 0, hash = "",
            )
        }
        return Movie(
            id = 1, imdbId = "tt1", title = "T", year = 2020, rating = rating,
            runtime = 100, genres = emptyList(), synopsis = "", posterUrl = "",
            coverUrl = "", backdropUrl = "", trailerUrl = null, certification = "",
            language = "en", torrents = torrents,
        )
    }

    @Test
    fun `quality filter keeps only movies with that quality`() {
        val movies = listOf(
            movieWith(7.0, setOf("720p")),
            movieWith(7.0, setOf("1080p", "2160p")),
        )
        assertEquals(1, movies.applyClientSideFilters("1080p", 0).size)
        assertEquals(2, movies.applyClientSideFilters("All", 0).size)
    }

    @Test
    fun `minimum rating filter uses 0-10 scale`() {
        val movies = listOf(
            movieWith(5.9, setOf("1080p")),
            movieWith(6.0, setOf("1080p")),
            movieWith(8.7, setOf("1080p")),
        )
        assertEquals(2, movies.applyClientSideFilters("All", 6).size)
        assertEquals(0, movies.applyClientSideFilters("All", 9).size)
    }

    // ── Sort mapping ──────────────────────────────────────────────────────────

    @Test
    fun `legacy yts sort values map to valid butter sorts`() {
        assertEquals("last added", MovieApiService.toButterSort("date_added"))
        assertEquals("trending", MovieApiService.toButterSort("download_count"))
        assertEquals("popularity", MovieApiService.toButterSort("like_count"))
        assertEquals("rating", MovieApiService.toButterSort("rating"))
        assertEquals("year", MovieApiService.toButterSort("year"))
        assertEquals("title", MovieApiService.toButterSort("title"))
        assertEquals("last added", MovieApiService.toButterSort("last added"))
        assertEquals("trending", MovieApiService.toButterSort("unknown_value"))
    }
}

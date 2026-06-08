package com.popcorntime.android.data.subtitles

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.popcorntime.android.BuildConfig
import timber.log.Timber



@Serializable
data class OsSearchResponse(
    val data: List<OsSubtitleItem> = emptyList(),
)

@Serializable
data class OsSubtitleItem(
    val id: String = "",
    val attributes: OsSubtitleAttributes = OsSubtitleAttributes(),
)

@Serializable
data class OsSubtitleAttributes(
    val language: String = "",
    @SerialName("release") val release: String = "",
    @SerialName("download_count") val downloadCount: Int = 0,
    val files: List<OsSubtitleFile> = emptyList(),
)

@Serializable
data class OsSubtitleFile(
    @SerialName("file_id") val fileId: Int = 0,
    @SerialName("file_name") val fileName: String = "",
)

@Serializable
data class OsDownloadResponse(
    val link: String = "",
    @SerialName("file_name") val fileName: String = "",
)

@Serializable
data class OsDownloadRequest(
    @SerialName("file_id") val fileId: Int,
)

data class Subtitle(
    val language: String,
    val label: String,
    val fileId: Int,
    val fileName: String,
    var downloadUrl: String = "",
)

class SubtitleService constructor(
    private val client: HttpClient,
    private val osTokenStore: OsTokenStore,
) {
    companion object {
        private const val DEFAULT_BASE_URL = "https://api.opensubtitles.com/api/v1"
        private val API_KEY get() = BuildConfig.OS_API_KEY
    }

    private suspend fun resolveBaseUrl(): String {
        val stored = osTokenStore.getBaseUrl() ?: ""
        if (stored.isBlank()) return DEFAULT_BASE_URL
        // Strip any trailing /api/v1 suffix first
        val withoutPath = stored.trimEnd('/').removeSuffix("/api/v1").trimEnd('/')
        // Re-add /api/v1
        return if (withoutPath.startsWith("http://") || withoutPath.startsWith("https://")) {
            "$withoutPath/api/v1"
        } else {
            "https://$withoutPath/api/v1"
        }
    }

    /**
     * Search subtitles for a movie by IMDB ID.
     * Uses stored preferred languages when no explicit languages parameter is supplied.
     * Returns up to 10 subtitle options sorted by download count.
     */
    suspend fun searchSubtitles(
        imdbId: String,
        languages: List<String>? = null,
    ): List<Subtitle> = runCatching {
        val effectiveLanguages = when {
            languages != null && languages.isNotEmpty() -> languages
            else -> runBlocking { osTokenStore.getPreferredLanguages() }.ifEmpty { null }
        }
        val baseUrl = resolveBaseUrl()
        val cleanId = imdbId.removePrefix("tt")
        val response = client.get("$baseUrl/subtitles") {
            header("Api-Key", API_KEY)
            header("User-Agent", "PopcornTimeAndroid v1.0")
            parameter("imdb_id", cleanId)
            if (effectiveLanguages != null) {
                parameter("languages", effectiveLanguages.joinToString(","))
            }
            parameter("order_by", "download_count")
            parameter("order_direction", "desc")
        }.body<OsSearchResponse>()

        response.data
            .flatMap { item ->
                item.attributes.files.map { file ->
                    Subtitle(
                        language = item.attributes.language,
                        label = "${item.attributes.language.uppercase()} — ${item.attributes.release}",
                        fileId = file.fileId,
                        fileName = file.fileName,
                    )
                }
            }
            .take(10)
    }.getOrElse { e ->
        Timber.w(e, "SubtitleService: search failed for $imdbId")
        emptyList()
    }

    /**
     * Get the direct download URL for a subtitle file.
     * Uses stored auth token and base URL when the user is logged in.
     */
    suspend fun getDownloadUrl(fileId: Int): String? = runCatching {
        val baseUrl = resolveBaseUrl()
        val token = osTokenStore.getToken()
        if (token.isNullOrBlank()) {
            Timber.w("SubtitleService: download attempted without auth token — anonymous quota may be exhausted. Sign in via Library > CC icon.")
        }
        val response = client.post("$baseUrl/download") {
            header("Api-Key", API_KEY)
            header("User-Agent", "PopcornTimeAndroid v1.0")
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody(OsDownloadRequest(fileId))
        }.body<OsDownloadResponse>()
        response.link.takeIf { it.isNotBlank() }
    }.getOrElse { e ->
        Timber.w(e, "SubtitleService: download URL fetch failed for fileId=$fileId")
        null
    }

    /**
     * Download subtitle content and return it as a String (SRT/VTT text).
     */
    suspend fun downloadSubtitle(url: String): String? = runCatching {
        client.get(url).body<String>()
    }.getOrElse { e ->
        Timber.w(e, "SubtitleService: download failed from $url")
        null
    }
}

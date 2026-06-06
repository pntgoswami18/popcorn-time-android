package com.popcorntime.android.data.subtitles

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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

data class Subtitle(
    val language: String,
    val label: String,
    val fileId: Int,
    val fileName: String,
    var downloadUrl: String = "",
)

@Singleton
class SubtitleService @Inject constructor(
    private val client: HttpClient,
) {
    companion object {
        private const val BASE_URL = "https://api.opensubtitles.com/api/v1"
        private const val API_KEY = "REDACTED_API_KEY" // free public key
    }

    /**
     * Search subtitles for a movie by IMDB ID.
     * Returns up to 5 subtitle options sorted by download count.
     */
    suspend fun searchSubtitles(
        imdbId: String,
        languages: List<String> = listOf("en"),
    ): List<Subtitle> = runCatching {
        val cleanId = imdbId.removePrefix("tt")
        val response = client.get("$BASE_URL/subtitles") {
            header("Api-Key", API_KEY)
            header("User-Agent", "PopcornTimeAndroid v1.0")
            parameter("imdb_id", cleanId)
            parameter("languages", languages.joinToString(","))
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
     * OpenSubtitles requires a separate download request per file.
     */
    suspend fun getDownloadUrl(fileId: Int): String? = runCatching {
        val response = client.get("$BASE_URL/download") {
            header("Api-Key", API_KEY)
            header("User-Agent", "PopcornTimeAndroid v1.0")
            parameter("file_id", fileId)
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

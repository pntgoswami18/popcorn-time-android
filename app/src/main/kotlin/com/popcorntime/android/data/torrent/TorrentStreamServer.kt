package com.popcorntime.android.data.torrent

import fi.iki.elonen.NanoHTTPD
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * Local HTTP server that serves a (potentially still-downloading) torrent file
 * to ExoPlayer. NanoHTTPD handles byte-range requests natively, which ExoPlayer
 * requires for seeking within a partially buffered file.
 *
 * Mirrors webtorrent/lib/server.js in popcorn-desktop.
 */
class TorrentStreamServer : NanoHTTPD(STREAM_PORT) {

    @Volatile private var videoFile: File? = null

    companion object {
        const val STREAM_PORT = 8888
        const val STREAM_PATH = "/stream"
        fun localUrl() = "http://127.0.0.1:$STREAM_PORT$STREAM_PATH"
    }

    fun start(file: File): String {
        videoFile = file
        if (!isAlive) {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Timber.d("TorrentStreamServer started on port $STREAM_PORT")
        }
        return localUrl()
    }

    override fun stop() {
        videoFile = null
        if (isAlive) {
            super.stop()
            Timber.d("TorrentStreamServer stopped")
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val file = videoFile ?: return newFixedLengthResponse(
            Response.Status.NOT_FOUND, MIME_PLAINTEXT, "No file"
        )
        if (!file.exists()) return newFixedLengthResponse(
            Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "File not ready"
        )

        val fileLength = file.length()
        val mimeType = getMimeForFile(file.name)

        // Handle byte-range requests (required for ExoPlayer seeking)
        val rangeHeader = session.headers["range"]
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val range = rangeHeader.removePrefix("bytes=").split("-")
            val start = range[0].toLongOrNull() ?: 0L
            val end = if (range.size > 1 && range[1].isNotBlank()) {
                range[1].toLongOrNull() ?: (fileLength - 1)
            } else {
                fileLength - 1
            }
            if (start >= fileLength) {
                return newFixedLengthResponse(
                    Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "Range Not Satisfiable"
                )
            }
            val clampedEnd = end.coerceAtMost(fileLength - 1)
            val length = clampedEnd - start + 1
            val raf = try {
                RandomAccessFile(file, "r").also { it.seek(start) }
            } catch (e: Exception) {
                Timber.e(e, "TorrentStreamServer: failed to open file for range request")
                return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error"
                )
            }
            val fis = object : InputStream() {
                override fun read(): Int = raf.read()
                override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
                override fun close() { raf.close() }
            }
            return try {
                newFixedLengthResponse(
                    Response.Status.PARTIAL_CONTENT, mimeType, fis, length
                ).apply {
                    addHeader("Content-Range", "bytes $start-$clampedEnd/$fileLength")
                    addHeader("Accept-Ranges", "bytes")
                }
            } catch (e: Exception) {
                raf.close()
                throw e
            }
        }

        // Full file response
        val fis = try {
            FileInputStream(file)
        } catch (e: Exception) {
            Timber.e(e, "TorrentStreamServer: failed to open file")
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error"
            )
        }
        return try {
            newFixedLengthResponse(Response.Status.OK, mimeType, fis, fileLength)
                .apply { addHeader("Accept-Ranges", "bytes") }
        } catch (e: Exception) {
            fis.close()
            throw e
        }
    }

    private fun getMimeForFile(name: String) = when {
        name.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
        name.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
        name.endsWith(".avi", ignoreCase = true) -> "video/x-msvideo"
        name.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
        name.endsWith(".wmv", ignoreCase = true) -> "video/x-ms-wmv"
        name.endsWith(".webm", ignoreCase = true) -> "video/webm"
        else -> "video/*"
    }
}

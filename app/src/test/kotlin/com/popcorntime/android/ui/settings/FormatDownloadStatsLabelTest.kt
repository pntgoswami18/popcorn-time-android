package com.popcorntime.android.ui.settings

import com.popcorntime.android.data.torrent.DownloadStats
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDownloadStatsLabelTest {

    @Test
    fun `non-zero speed with known total formats percentage, sizes and speed`() {
        val stats = DownloadStats(
            imdbId = "tt0111161",
            progress = 0.45f,
            downloadedBytes = 47_185_920L, // 45.0 MB
            totalBytes = 104_857_600L, // 100.0 MB
            downloadSpeedBps = 524_288L, // 512 KB/s
        )
        val label = formatDownloadStatsLabel(stats)
        assertEquals("45% · 45.0 MB / 100.0 MB · 512 KB/s", label)
    }

    @Test
    fun `zero totalBytes uses downloaded-only branch`() {
        val stats = DownloadStats(
            imdbId = "tt0111161",
            progress = 0.1f,
            downloadedBytes = 10_485_760L, // 10.0 MB
            totalBytes = 0L,
            downloadSpeedBps = 1024L, // 1 KB/s
        )
        val label = formatDownloadStatsLabel(stats)
        assertEquals("10% · 10.0 MB downloaded · 1 KB/s", label)
    }

    @Test
    fun `does not throw for any in-range values (regression for UnknownFormatConversionException)`() {
        // The original code formatted an interpolated string containing "%" + " · ",
        // which crashed with java.util.UnknownFormatConversionException: Conversion = '·'.
        val samples = listOf(
            DownloadStats("tt1", 0f, 0L, 0L, 0L),
            DownloadStats("tt2", 1f, Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, Long.MAX_VALUE / 2),
            DownloadStats("tt3", 0.999f, 1L, 1L, 1L),
        )
        samples.forEach { stats ->
            val label = formatDownloadStatsLabel(stats) // must not throw
            org.junit.Assert.assertTrue(label.contains("% · "))
            org.junit.Assert.assertTrue(label.endsWith(" KB/s"))
        }
    }

    @Test
    fun `zero speed formats as 0 KB per s`() {
        val stats = DownloadStats("tt0", 0.5f, 5_242_880L, 10_485_760L, 0L)
        assertEquals("50% · 5.0 MB / 10.0 MB · 0 KB/s", formatDownloadStatsLabel(stats))
    }
}

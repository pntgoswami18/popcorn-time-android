package com.popcorntime.android.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ResolvePlayableVideoUriTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.fill(bytes: Int): File {
        writeBytes(ByteArray(bytes) { 1 })
        return this
    }

    @Test
    fun `null path returns null`() {
        assertNull(resolvePlayableVideoUri(null))
    }

    @Test
    fun `blank path returns null`() {
        assertNull(resolvePlayableVideoUri(""))
        assertNull(resolvePlayableVideoUri("   "))
    }

    @Test
    fun `content uri is passed through unchanged`() {
        val uri = "content://media/external/video/1234"
        assertEquals(uri, resolvePlayableVideoUri(uri))
    }

    @Test
    fun `file uri is passed through unchanged`() {
        val uri = "file:///storage/emulated/0/movie.mp4"
        assertEquals(uri, resolvePlayableVideoUri(uri))
    }

    @Test
    fun `direct path to non-empty file returns its uri`() {
        val file = tmp.newFile("movie.mkv").fill(10)
        assertEquals(file.toURI().toString(), resolvePlayableVideoUri(file.absolutePath))
    }

    @Test
    fun `direct path to empty file returns null`() {
        val file = tmp.newFile("empty.mp4") // zero bytes
        assertNull(resolvePlayableVideoUri(file.absolutePath))
    }

    @Test
    fun `directory containing video files returns the largest video`() {
        val dir = tmp.newFolder("download")
        File(dir, "sample.mp4").fill(10)
        val big = File(dir, "movie.mkv").fill(1000)
        val nested = File(dir, "extras").apply { mkdirs() }
        File(nested, "clip.avi").fill(100)
        assertEquals(big.toURI().toString(), resolvePlayableVideoUri(dir.absolutePath))
    }

    @Test
    fun `directory with no video files returns null`() {
        val dir = tmp.newFolder("download2")
        File(dir, "readme.txt").fill(50)
        File(dir, "cover.jpg").fill(50)
        assertNull(resolvePlayableVideoUri(dir.absolutePath))
    }

    @Test
    fun `directory with only zero-byte videos returns null`() {
        val dir = tmp.newFolder("download3")
        File(dir, "movie.mp4").createNewFile() // zero bytes
        assertNull(resolvePlayableVideoUri(dir.absolutePath))
    }

    @Test
    fun `nonexistent path returns null`() {
        assertNull(resolvePlayableVideoUri(File(tmp.root, "missing/path").absolutePath))
    }
}

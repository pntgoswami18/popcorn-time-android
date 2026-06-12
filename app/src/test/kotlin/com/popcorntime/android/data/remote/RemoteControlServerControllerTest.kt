package com.popcorntime.android.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the single invariant the controller exists for: the remote control
 * server runs if and only if the persisted enabled preference is true.
 *
 * Uses [Dispatchers.Unconfined] so StateFlow emissions drive the collector
 * synchronously — no Android or coroutines-test machinery needed.
 */
class RemoteControlServerControllerTest {

    private val enabled = MutableStateFlow(false)
    private val startedWith = mutableListOf<String>()
    private var stopCalls = 0
    private var tokenRequests = 0

    private fun newController(
        getToken: suspend () -> String = { tokenRequests++; "token-$tokenRequests" },
        startServer: suspend (String) -> Unit = { startedWith += it },
    ) = RemoteControlServerController(
        enabled = enabled,
        getToken = getToken,
        startServer = startServer,
        stopServer = { stopCalls++ },
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun `does not start server when preference is false at startup`() {
        newController().start()
        assertEquals(emptyList<String>(), startedWith)
        assertEquals(1, stopCalls) // ensures a stale server would be stopped
    }

    @Test
    fun `starts server with token when preference is true at startup`() {
        enabled.value = true
        newController().start()
        assertEquals(listOf("token-1"), startedWith)
        assertEquals(0, stopCalls)
    }

    @Test
    fun `enabling preference starts the server`() {
        newController().start()
        enabled.value = true
        assertEquals(listOf("token-1"), startedWith)
    }

    @Test
    fun `disabling preference stops the server`() {
        enabled.value = true
        newController().start()
        enabled.value = false
        assertEquals(1, stopCalls)
        assertEquals(listOf("token-1"), startedWith)
    }

    @Test
    fun `toggling repeatedly starts and stops accordingly`() {
        newController().start()
        enabled.value = true
        enabled.value = false
        enabled.value = true
        assertEquals(listOf("token-1", "token-2"), startedWith)
        assertEquals(2, stopCalls) // initial false + explicit disable
    }

    @Test
    fun `start is idempotent`() {
        enabled.value = true
        val controller = newController()
        controller.start()
        controller.start()
        assertEquals(listOf("token-1"), startedWith)
    }

    @Test
    fun `a failing start does not kill the collector`() {
        var attempts = 0
        newController(
            startServer = {
                attempts++
                if (attempts == 1) error("bind failed")
                startedWith += it
            },
        ).start()
        enabled.value = true // fails
        enabled.value = false
        enabled.value = true // retried and succeeds
        assertEquals(listOf("token-2"), startedWith)
        assertEquals(2, attempts)
    }
}

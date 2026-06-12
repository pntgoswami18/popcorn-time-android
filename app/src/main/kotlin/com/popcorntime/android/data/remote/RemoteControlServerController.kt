package com.popcorntime.android.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sole owner of the remote control server lifecycle.
 *
 * Observes the persisted "remote control enabled" preference and starts/stops
 * the server to match: the server runs if and only if the preference is true.
 * Everything else (settings toggle, any service) only reads or writes the
 * preference — nothing else may start or stop the server directly.
 *
 * Started once from [com.popcorntime.android.PopcornApp.onCreate] so the
 * invariant holds for the whole process lifetime, including streams started
 * without the settings screen ever being opened.
 *
 * Dependencies are plain functions so the class is unit-testable on the JVM;
 * Hilt wiring lives in [com.popcorntime.android.di.RemoteModule].
 */
class RemoteControlServerController(
    private val enabled: Flow<Boolean>,
    private val getToken: suspend () -> String,
    private val startServer: suspend (token: String) -> Unit,
    private val stopServer: () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val started = AtomicBoolean(false)

    /** Begins observing the preference. Safe to call more than once. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            enabled.distinctUntilChanged().collect { on ->
                runCatching {
                    if (on) startServer(getToken()) else stopServer()
                }.onFailure {
                    Timber.e(it, "Failed to ${if (on) "start" else "stop"} remote control server")
                }
            }
        }
    }
}

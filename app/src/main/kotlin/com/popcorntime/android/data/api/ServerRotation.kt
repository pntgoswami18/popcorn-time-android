package com.popcorntime.android.data.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Mirror-failover helper for the Butter / popcorn-ru API — mirrors
 * butter-provider/generic.js setApiUrls (shuffle) + _get (rotate on failure,
 * promote the working server to the head so it stays sticky).
 *
 * Shared by [MovieApiService] and [ShowApiService], each with its own instance
 * and server list.
 */
class ServerRotation(servers: List<String>) {

    private val serverQueue = ArrayDeque(servers.shuffled())
    private val serverMutex = Mutex()

    /**
     * Runs [block] against each server in queue order until one succeeds; failed
     * servers are demoted to the back, the working one is promoted to the head.
     *
     * The mutex only guards the queue snapshot and reordering — never the HTTP
     * request itself — so concurrent browse/search/detail calls don't serialize
     * (and a dead mirror's timeout doesn't block every other request).
     *
     * A [kotlinx.serialization.SerializationException] is rethrown without
     * rotating: bad JSON is a schema problem, not a server outage.
     */
    suspend fun <T> withRotation(block: suspend (base: String) -> T): T {
        val order = serverMutex.withLock { serverQueue.toList() }
        val errors = mutableListOf<Throwable>()
        for (base in order) {
            try {
                val result = block(base)
                // On success, promote this server to head
                serverMutex.withLock {
                    serverQueue.remove(base)
                    serverQueue.addFirst(base)
                }
                return result
            } catch (e: Exception) {
                if (e is kotlinx.serialization.SerializationException) throw e
                Timber.w(e, "Server $base failed, rotating")
                errors += e
                // Rotate — move failed server to the back
                serverMutex.withLock {
                    serverQueue.remove(base)
                    serverQueue.addLast(base)
                }
            }
        }
        throw errors.lastOrNull() ?: IllegalStateException("No servers configured")
    }
}

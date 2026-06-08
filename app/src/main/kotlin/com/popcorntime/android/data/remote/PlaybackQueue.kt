package com.popcorntime.android.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackQueue @Inject constructor() {
    private val _items = MutableStateFlow<List<QueueItem>>(emptyList())
    val items: StateFlow<List<QueueItem>> = _items.asStateFlow()

    fun enqueue(item: QueueItem) {
        _items.update { it + item }
    }

    fun dequeue(): QueueItem? {
        var removed: QueueItem? = null
        _items.update { list ->
            removed = list.firstOrNull()
            if (list.isEmpty()) list else list.drop(1)
        }
        return removed
    }

    fun clear() { _items.update { emptyList() } }
    fun peek(): QueueItem? = _items.value.firstOrNull()
}

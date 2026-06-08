package com.popcorntime.android.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackQueue @Inject constructor() {
    private val _items = MutableStateFlow<List<QueueItem>>(emptyList())
    val items: StateFlow<List<QueueItem>> = _items.asStateFlow()

    fun enqueue(item: QueueItem) {
        _items.value = _items.value + item
    }

    fun dequeue(): QueueItem? {
        val list = _items.value
        if (list.isEmpty()) return null
        _items.value = list.drop(1)
        return list.first()
    }

    fun clear() { _items.value = emptyList() }
    fun peek(): QueueItem? = _items.value.firstOrNull()
}

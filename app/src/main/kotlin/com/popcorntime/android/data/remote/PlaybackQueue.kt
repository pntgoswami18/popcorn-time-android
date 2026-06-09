package com.popcorntime.android.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackQueue @Inject constructor() {
    private val _itemsRef = AtomicReference<List<QueueItem>>(emptyList())
    private val _items = MutableStateFlow<List<QueueItem>>(emptyList())
    val items: StateFlow<List<QueueItem>> = _items.asStateFlow()

    fun enqueue(item: QueueItem) {
        while (true) {
            val current = _itemsRef.get()
            val updated = current + item
            if (_itemsRef.compareAndSet(current, updated)) {
                _items.value = updated
                break
            }
        }
    }

    fun dequeue(): QueueItem? {
        while (true) {
            val current = _itemsRef.get()
            if (current.isEmpty()) return null
            val item = current.first()
            val updated = current.drop(1)
            if (_itemsRef.compareAndSet(current, updated)) {
                _items.value = updated
                return item
            }
        }
    }

    fun clear() {
        _itemsRef.set(emptyList())
        _items.value = emptyList()
    }

    fun peek(): QueueItem? = _itemsRef.get().firstOrNull()
}

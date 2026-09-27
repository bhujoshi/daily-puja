package com.worship.nityamandir.engine

/** Bounded session cache. A monotonic clock keeps expiry independent of clock changes. */
internal class BhajanCache<K, V>(
    private val capacity: Int = 32,
    private val ttlMillis: Long = 600_000,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private data class Entry<V>(val value: V, val saved: Long)
    private val entries = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)

    @Synchronized fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (now() - entry.saved >= ttlMillis) { entries.remove(key); return null }
        return entry.value
    }

    @Synchronized fun put(key: K, value: V) {
        entries[key] = Entry(value, now())
        while (entries.size > capacity) entries.remove(entries.keys.first())
    }
}

package com.worship.nityamandir

import com.worship.nityamandir.engine.BhajanCache
import org.junit.Assert.*
import org.junit.Test

class BhajanCacheTest {
    @Test fun expiresWithoutExtendingLifetimeOnRead() {
        var time = 0L
        val cache = BhajanCache<String, String>(ttlMillis = 100, now = { time })
        cache.put("search", "result")
        time = 99
        assertEquals("result", cache.get("search"))
        time = 100
        assertNull(cache.get("search"))
    }
    @Test fun evictsLeastRecentlyUsedAndKeepsKeysSeparate() {
        val cache = BhajanCache<String, String>(capacity = 2)
        cache.put("shiva", "one")
        cache.put("rama", "two")
        assertEquals("one", cache.get("shiva"))
        cache.put("krishna", "three")
        assertNull(cache.get("rama"))
        assertEquals("one", cache.get("shiva"))
        assertEquals("three", cache.get("krishna"))
    }
}

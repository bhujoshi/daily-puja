package com.worship.nityamandir

import com.worship.nityamandir.engine.BhajanPlayer.Track
import com.worship.nityamandir.engine.BhajanQueue
import org.junit.Assert.*
import org.junit.Test

class BhajanQueueTest {
    private val songs = listOf(
        Track("Shiva", "https://example.com/31.mp3", order = 31, deity = "shiva"),
        Track("Ganesh", "https://example.com/71.mp3", order = 71, deity = "ganesh"),
        Track("Rama", "https://example.com/86.mp3", order = 86, deity = "rama", collection = "mixed"))
    @Test fun sequenceStartsWithTempleIdol() {
        assertEquals("Rama", BhajanQueue.order(songs, false, "ram_darbar").first().title)
        assertEquals("Ganesh", BhajanQueue.order(songs, false, "original").first().title)
        assertEquals("Ganesh", BhajanQueue.order(songs, false, "ganesh_hanuman").first().title)
        assertEquals(songs, BhajanQueue.order(songs.reversed(), false, "lakshmi"))
    }
    @Test fun shuffleKeepsEverySongExactlyOnce() {
        repeat(20) { assertEquals(songs.toSet(), BhajanQueue.order(songs, true, "shiva").toSet()) }
        assertTrue(BhajanQueue.order(emptyList(), false, "shiva").isEmpty())
    }
}

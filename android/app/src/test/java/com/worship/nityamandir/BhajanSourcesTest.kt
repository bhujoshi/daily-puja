package com.worship.nityamandir

import com.worship.nityamandir.engine.BhajanPlayer.Track
import com.worship.nityamandir.engine.BhajanSources
import org.junit.Assert.*
import org.junit.Test

class BhajanSourcesTest {
    private val default = Track("Jai Ganesh Deva", "https://archive.org/default.mp3",
        id = BhajanSources.DEFAULT_ID, youtubeId = "87Y-OH_mJxM", isYoutube = true)

    @Test fun defaultKeepsItsCatalogAudioEvenWithYoutubeMetadata() {
        val playable = BhajanSources.playable(default)!!
        assertEquals(default.url, playable.url)
        assertFalse(playable.isYoutubeOnly)
    }

    @Test fun otherCatalogSongsAlwaysUseTheirExactYoutubeVideo() {
        val archived = Track("Hanuman Chalisa", "https://archive.org/unrelated.mp3",
            id = "46", youtubeId = "AETFvQonfV8", volumeGainDb = -9f)
        val playable = BhajanSources.playable(archived)!!
        assertEquals("https://www.youtube.com/watch?v=AETFvQonfV8", playable.url)
        assertTrue(playable.isYoutubeOnly)
        assertEquals(0f, playable.volumeGainDb)
    }

    @Test fun songsWithoutAValidYoutubeIdCannotFallBackToArchiveAudio() {
        assertNull(BhajanSources.playable(Track("Song", "https://archive.org/song.mp3", id = "other")))
        assertNull(BhajanSources.playable(Track("Song", "https://archive.org/song.mp3",
            id = "other", youtubeId = "invalid<id>")))
    }

    @Test fun catalogDefaultsToGaneshAndDeduplicatesYoutubeVideos() {
        val other = Track("Hanuman Chalisa", "https://www.youtube.com/watch?v=AETFvQonfV8",
            id = "46", order = 0, youtubeId = "AETFvQonfV8", isYoutube = true)
        val tracks = BhajanSources.catalog(listOf(other, other.copy(id = "duplicate"), default.copy(order = 99)))
        assertEquals(listOf(BhajanSources.DEFAULT_ID, "46"), tracks.map { it.id })
    }

    @Test fun legacyArchiveEntriesDoNotSupplyUnverifiedYoutubeSongMetadata() {
        val legacy = Track("Om Namah Shivay", "https://archive.org/song.mp3",
            id = "31", youtubeId = "nVib8eC2JDI")
        assertEquals(listOf(BhajanSources.DEFAULT_ID), BhajanSources.catalog(listOf(default, legacy)).map { it.id })
    }

    @Test fun defaultCannotBecomeAYouTubeWatchPageSentToTheAudioPlayer() {
        assertNull(BhajanSources.playable(default.copy(url = "https://www.youtube.com/watch?v=87Y-OH_mJxM")))
    }
}

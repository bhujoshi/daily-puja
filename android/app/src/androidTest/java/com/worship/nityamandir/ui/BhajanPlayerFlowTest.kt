package com.worship.nityamandir.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.worship.nityamandir.engine.BhajanPlayer
import com.worship.nityamandir.engine.BhajanSources
import com.worship.nityamandir.ui.components.BhajanPlayerSheet
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BhajanPlayerFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var player: BhajanPlayer
    private var showSheet by mutableStateOf(true)
    private val youtubeTrack = BhajanPlayer.Track("Hanuman Chalisa", "https://www.youtube.com/watch?v=AETFvQonfV8",
        id = "yt_AETFvQonfV8", youtubeId = "AETFvQonfV8", isYoutube = true, artist = "Hariharan")

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(context.packageName.endsWith(".verification"))
        compose.runOnIdle { player = BhajanPlayer(context) }
        compose.setContent { if (showSheet) BhajanPlayerSheet(player, false) { showSheet = false } }
    }

    @After fun cleanUp() {
        if (::player.isInitialized) compose.runOnIdle { player.remove(); player.release() }
    }

    @Test fun opensWithCatalogAartiAndOneYoutubeList() {
        if (InstrumentationRegistry.getArguments().getString("musicNetwork") == "true") {
            compose.waitUntil(20_000) { !player.catalogLoading }
            compose.runOnIdle {
                assertFalse("YouTube song list should load", player.catalogError)
                assertTrue(player.catalog.any { it.isYoutubeOnly })
            }
        }
        compose.onNodeWithText("Jai Ganesh Jai Ganesh Deva").assertIsDisplayed()
        compose.onNodeWithText("Music catalog", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("youtube-search-input").assertIsDisplayed()
        compose.onNodeWithTag("youtube-minimize-chevron").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        java.io.File(instrumentation.targetContext.filesDir, "music-player-default.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.runOnIdle {
            assertEquals(BhajanSources.DEFAULT_ID, player.defaultTrack?.id)
            assertFalse(player.defaultTrack!!.isYoutubeOnly)
            assertTrue(player.catalog.drop(1).all { it.isYoutubeOnly && it.url.startsWith("https://www.youtube.com/watch?v=") })
        }
    }

    @Test fun defaultCanPlayPauseAndResume() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("musicNetwork") == "true")
        compose.onNodeWithContentDescription("Play / pause").performClick()
        compose.waitUntil(30_000) { player.playing || player.failed }
        compose.runOnIdle { assertFalse("Default audio failed", player.failed); assertTrue(player.playing) }
        compose.onNodeWithContentDescription("Play / pause").performClick()
        compose.runOnIdle { assertFalse(player.playing) }
        compose.onNodeWithContentDescription("Play / pause").performClick()
        compose.waitUntil(10_000) { player.playing }
    }

    @Test fun youtubeSelectionNeverSubstitutesArchiveAudioAndClosingPausesIt() {
        compose.runOnIdle { player.playCatalog(youtubeTrack, autoplay = false) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(player.youtubeMode)
            assertTrue(player.current!!.url.startsWith("https://www.youtube.com/watch?v="))
        }
        compose.onNodeWithContentDescription("Close", useUnmergedTree = true).performClick()
        compose.runOnIdle {
            // A queued WebView callback must not revive playback after the sheet is gone.
            player.updateYoutube(player.playbackGeneration, 1, 12.0, 200.0, false)
            assertFalse(player.playing)
            assertFalse(player.youtubeAutoplay)
        }
    }

    @Test fun youtubeRecordingPlaysInTheVisibleEmbed() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("musicNetwork") == "true")
        compose.runOnIdle { player.playCatalog(youtubeTrack) }
        compose.waitUntil(35_000) { (player.playing && player.position > 3000L) || player.failed }
        compose.runOnIdle { assertFalse("YouTube embed failed", player.failed); assertTrue(player.playing) }
        compose.runOnIdle { player.pause() }
        val bounds = java.util.concurrent.atomic.AtomicReference<org.json.JSONObject?>()
        androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.webkit.WebView::class.java)
        ).check { view, _ ->
            (view as android.webkit.WebView).evaluateJavascript("""
                (function(){var r=document.getElementById('player').getBoundingClientRect();
                return {width:r.width,height:r.height};})()
            """.trimIndent()) { bounds.set(org.json.JSONObject(it)) }
        }
        compose.waitUntil(5000) { bounds.get() != null }
        assertTrue("YouTube video must have visible width", bounds.get()!!.getDouble("width") >= 200)
        assertTrue("YouTube video must have visible height", bounds.get()!!.getDouble("height") >= 200)
        // Allow the embedded controls to paint before taking the visual review capture.
        Thread.sleep(1000)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        java.io.File(instrumentation.targetContext.filesDir, "music-player-youtube.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.runOnIdle { assertFalse(player.playing) }
    }

    @Test fun minimizedYoutubeUsesOriginalMiniPlayerAndExpandsToResume() {
        compose.runOnIdle { player.playCatalog(youtubeTrack, autoplay = false) }
        compose.onNodeWithTag("youtube-minimize-chevron").performClick()
        compose.onNodeWithTag("youtube-side-mini-player").assertIsDisplayed()
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(player.playing) }
        compose.onNodeWithContentDescription("Play / pause").performClick()
        compose.onNodeWithTag("youtube-minimize-chevron").assertIsDisplayed()
        compose.onNodeWithTag("youtube-side-mini-player").assertDoesNotExist()
        compose.runOnIdle { assertTrue(player.youtubeMode) }
    }

    @Test fun switchingBackToDefaultKeepsTheCatalogRecording() {
        compose.runOnIdle {
            player.playCatalog(youtubeTrack, autoplay = false)
            player.playCatalog(autoplay = false)
        }
        compose.onNodeWithText("Music catalog", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(player.defaultTrack?.url, player.current?.url)
            assertFalse(player.youtubeMode)
        }
    }
}

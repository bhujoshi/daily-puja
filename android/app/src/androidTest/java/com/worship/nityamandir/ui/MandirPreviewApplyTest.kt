package com.worship.nityamandir.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.worship.nityamandir.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Exercises the real menu, preview and main altar, rather than just the apply callback. */
class MandirPreviewApplyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var context: Context
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalWorship: Map<String, *> = emptyMap<String, Any>()
    private var originalDevotion: Map<String, *> = emptyMap<String, Any>()
    private var originalReferral: Map<String, *> = emptyMap<String, Any>()
    private var prepared = false

    @Before fun prepareGuest() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Requires -PisolatedVerification=true", context.packageName.endsWith(".verification"))
        originalWorship = context.getSharedPreferences("worship", Context.MODE_PRIVATE).all.toMap()
        originalDevotion = context.getSharedPreferences("devotion", Context.MODE_PRIVATE).all.toMap()
        originalReferral = context.getSharedPreferences("referral_install", Context.MODE_PRIVATE).all.toMap()
        prepared = true
        context.getSharedPreferences("worship", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("hindi", false)
            .putBoolean("auto_daily_temple_enabled", false).commit()
        context.getSharedPreferences("devotion", Context.MODE_PRIVATE).edit().clear().commit()
        // Avoid an install-referrer prompt in this offline guest scenario.
        context.getSharedPreferences("referral_install", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("read", true).putBoolean("processed", true).commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun restoreVerificationPreferences() {
        scenario?.close()
        if (!prepared) return
        restore("worship", originalWorship)
        restore("devotion", originalDevotion)
        restore("referral_install", originalReferral)
    }

    private fun restore(name: String, values: Map<String, *>) {
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.commit()
    }

    private fun openCustomizer() {
        compose.onNodeWithContentDescription("Temple options").performClick()
        compose.onNodeWithText("Mandir Sajaye").performClick()
        compose.onNodeWithText("Preview").assertIsDisplayed()
    }

    private fun openTemple() {
        compose.onNode(SemanticsMatcher("Open temple") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open temple"
        }).assertDoesNotExist()
        compose.onNodeWithTag("language-selection").assertDoesNotExist()
        assertNoOverlayText()
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("altar-item-lamp").assertIsDisplayed() }.isSuccess
        }
    }

    private fun relaunchWithLanguageUnset(existingUser: Boolean = false) {
        scenario!!.close()
        val editor = context.getSharedPreferences("worship", Context.MODE_PRIVATE).edit().clear()
        if (existingUser) editor.putLong("last", System.currentTimeMillis())
        editor.commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @Test fun firstLaunchEnglishChoiceOpensTempleAndSurvivesRecreation() {
        relaunchWithLanguageUnset()
        compose.onNodeWithTag("language-selection").assertIsDisplayed()
        compose.onNodeWithTag("ritual-dock").assertDoesNotExist()
        compose.onNodeWithText("हिंदी").assertIsDisplayed()
        compose.onNodeWithText("English").performClick()
        openTemple()
        assertEquals(false, context.getSharedPreferences("worship", Context.MODE_PRIVATE).getBoolean("hindi", true))
        scenario!!.recreate()
        openTemple()
        captureLayout("startup-english-temple")
    }

    @Test fun firstLaunchHindiChoiceIsRememberedAndCanBeChangedFromMenu() {
        relaunchWithLanguageUnset()
        captureLayout("startup-language-selection")
        scenario!!.recreate()
        compose.onNodeWithTag("language-selection").assertIsDisplayed()
        compose.onNodeWithText("हिंदी").performClick()
        compose.onNodeWithTag("language-selection").assertDoesNotExist()
        assertNoOverlayText()
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("altar-item-lamp").assertIsDisplayed().assertIsEnabled() }.isSuccess
        }
        assertEquals(true, context.getSharedPreferences("worship", Context.MODE_PRIVATE).getBoolean("hindi", false))
        scenario!!.recreate()
        compose.onNodeWithTag("language-selection").assertDoesNotExist()
        compose.onNodeWithContentDescription("मंदिर विकल्प").performClick()
        compose.onNodeWithText("Switch app language to English").performScrollTo().performClick()
        openTemple()
        scenario!!.recreate()
        openTemple()
    }

    @Test fun existingUserWithDefaultHindiSkipsLanguageSelection() {
        relaunchWithLanguageUnset(existingUser = true)
        compose.onNodeWithTag("language-selection").assertDoesNotExist()
        assertNoOverlayText()
        compose.onNodeWithTag("altar-item-lamp").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithContentDescription("मंदिर विकल्प").assertIsDisplayed()
    }

    @Test fun freeWorshipEnablesAllAltarItemsInAnyOrder() {
        openTemple()
        assertNoOverlayText()
        val allAltarTags = listOf(
            "altar-item-lamp",
            "altar-item-incense",
            "altar-item-water",
            "altar-item-tilak",
            "altar-item-flower-0",
            "altar-item-conch",
            "altar-item-prasad",
            "altar-item-aarti",
            "altar-item-bell",
            "altar-deity-0",
            "altar-deity-1"
        )
        allAltarTags.forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
        }
        captureLayout("puja-step-light")

        // User can start with any item (e.g., Tilak first) without any forced step order.
        compose.onNodeWithTag("altar-item-tilak").performClick()
        // Wait briefly for tilak animation then drive frames explicitly once continuous animations begin.
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(800)

        // All items remain enabled for free-form bathing out of order.
        allAltarTags.forEach { tag ->
            compose.onNodeWithTag(tag).assertIsEnabled()
        }
        compose.onNodeWithTag("altar-item-water").performClick()
        compose.mainClock.advanceTimeBy(1200)
        assertNoOverlayText()
        captureLayout("puja-step-bath")

        // Finish bathing animation and light the lamp afterward; all items stay enabled.
        compose.mainClock.advanceTimeBy(2500)
        compose.onNodeWithTag("altar-item-lamp").performClick()
        compose.mainClock.advanceTimeBy(500)
        assertNoOverlayText()
        allAltarTags.forEach { tag ->
            compose.onNodeWithTag(tag).assertIsEnabled()
        }
        captureLayout("puja-step-tilak")

        // Offer a flower directly too; user can do anything freely in any order.
        compose.onNodeWithTag("altar-item-flower-0").performClick()
        compose.mainClock.advanceTimeBy(1600)
        allAltarTags.forEach { tag ->
            compose.onNodeWithTag(tag).assertIsEnabled()
        }

        // Starting a long-running ritual like conch or bell keeps other unrelated mandir items clickable.
        compose.onNodeWithTag("altar-item-conch").performClick()
        compose.mainClock.advanceTimeBy(200)
        listOf(
            "altar-item-lamp",
            "altar-item-incense",
            "altar-item-water",
            "altar-item-tilak",
            "altar-item-flower-0",
            "altar-item-prasad",
            "altar-item-aarti",
            "altar-item-bell",
            "altar-deity-0",
            "altar-deity-1"
        ).forEach { tag ->
            compose.onNodeWithTag(tag).assertIsEnabled()
        }
        compose.onNodeWithTag("altar-item-bell").performClick()
        compose.onNodeWithTag("altar-item-incense").performClick()
        compose.onNodeWithTag("altar-deity-0").performClick()

        scenario!!.close()
        scenario = null
        compose.mainClock.autoAdvance = true
    }

    @Test fun agedTempleShowsCleanPromptAndTappingTempleOrButtonUnlocksAllMandirItems() {
        scenario!!.close()
        val staleMs = System.currentTimeMillis() - 48L * 3_600_000L
        context.getSharedPreferences("worship", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("hindi", false)
            .putBoolean("auto_daily_temple_enabled", false)
            .putLong("last", staleMs)
            .putLong("clean", staleMs)
            .commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)

        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("cleaning-prompt-pill").assertIsDisplayed() }.isSuccess
        }
        captureLayout("aged-cleaning-prompt")

        // Tapping directly on the altar item area while aged also starts the clean sweep cleanly.
        compose.onNodeWithTag("altar-item-bell").performTouchInput { click(center) }
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("cleaning-prompt-pill").assertDoesNotExist() }.isSuccess
        }
        openTemple()

        // Re-age via drawer test toggle and verify the clean button also unlocks every mandir item.
        compose.onNodeWithContentDescription("Temple options").performClick()
        compose.onNodeWithText("Pass a day (test)").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("cleaning-prompt-pill").assertIsDisplayed() }.isSuccess
        }
        compose.onNodeWithText("Tap to clean temple").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("cleaning-prompt-pill").assertDoesNotExist() }.isSuccess
        }
        openTemple()
        listOf(
            "altar-item-lamp",
            "altar-item-bell",
            "altar-item-water",
            "altar-item-tilak",
            "altar-item-flower-0",
            "altar-item-conch",
            "altar-item-prasad",
            "altar-item-aarti",
            "altar-deity-0"
        ).forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
        }
        compose.onNodeWithTag("altar-item-lamp").performClick()
        compose.onNodeWithTag("altar-item-bell").performClick()
        assertNoOverlayText()
    }

    private fun assertNoOverlayText() {
        compose.onNodeWithTag("ritual-dock").assertDoesNotExist()
        compose.onNodeWithTag("sanctum-top-guide").assertDoesNotExist()
        compose.onNodeWithTag("puja-step-guide").assertDoesNotExist()
        compose.onNodeWithTag("cleaning-prompt-pill").assertDoesNotExist()
        compose.onNodeWithTag("home-occasion-banner").assertDoesNotExist()
        assertTrue(
            "Expected zero visible text nodes on sanctum screen",
            compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                .fetchSemanticsNodes().isEmpty()
        )
    }

    private fun captureLayout(name: String) {
        compose.waitForIdle()
        Thread.sleep(500) // Allow native popup rendering to catch up with Compose semantics.
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/Download/$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    @Test fun guestUnlockOpensSignInAfterTheConsistentPreviewActionIsTapped() {
        openTemple()
        openCustomizer()
        compose.onNodeWithText("Dhoop & Agarbatti").performScrollTo().performClick()
        compose.onNodeWithText("Mayur dhoop stand").performScrollTo().performClick()
        compose.onNodeWithText("Preview").performClick()
        // Popup semantics can be registered before its separate window is placed.
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("mandir-preview-action").assertIsDisplayed() }.isSuccess
        }
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Unlock"))
            .assertIsDisplayed().performClick()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to choices").assertDoesNotExist()
    }

    @Test fun autoDailyTempleAppliesTodaysDeityAndDrawerToggleWorks() {
        val todayTemple = com.worship.nityamandir.engine.DailyTemples.forDate(
            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
        )
        scenario!!.close()
        context.getSharedPreferences("worship", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("hindi", false).commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        openTemple()
        val expectedDeities = todayTemple.presetSelection.deityNames(hindi = false).joinToString()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription(expectedDeities).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Temple options").performClick()
        compose.onNodeWithTag("drawer-daily-temple").assertDoesNotExist()
        compose.onNodeWithTag("drawer-auto-daily-temple").performScrollTo().assertIsDisplayed().performClick()
        compose.onNode(hasTestTag("drawer-auto-daily-temple") and hasText("OFF")).assertIsDisplayed()
        // Switch away to Shiva, then reactivate today's temple directly via the simple button in Mandir Sajaye.
        compose.onNodeWithText("Mandir Sajaye").performScrollTo().performClick()
        compose.onNodeWithText("Idols").performScrollTo().performClick()
        compose.onNodeWithText("Shiva").performScrollTo().performClick()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Apply")).performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Shiva").fetchSemanticsNodes().isNotEmpty()
        }
        openCustomizer()
        compose.onNodeWithTag("daily-temple-activate-button").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription(expectedDeities).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun applyingPreviewChangesTheMainAltarAndSurvivesReopening() {
        openTemple()
        compose.onNodeWithTag("altar-garland").assertExists()
        openCustomizer()
        compose.onNodeWithText("Divya Darshan (Big Idol)").performScrollTo().performClick()
        compose.onNodeWithText("Idols").performScrollTo().performClick()
        compose.onNodeWithText("Shiva").performScrollTo().performClick()
        compose.onNodeWithText("Preview").performClick()
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Apply"))
            .assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Back to choices").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithContentDescription("Shiva").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Shiva").assertIsDisplayed()
        compose.onNodeWithTag("altar-garland").assertDoesNotExist()
        compose.onNodeWithTag("altar-item-lamp").assertIsEnabled()
        assertNoOverlayText()
        captureLayout("puja-big-idol")
        // Reopen from the real menu: the active saved selection must remain selected.
        openCustomizer()
        compose.onNodeWithText("Divya Darshan (Big Idol)").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Idols").performScrollTo().performClick()
        compose.onNodeWithText("Shiva").performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Close").performClick()
        // Recreate the host too, so a callback-only temporary change cannot pass.
        scenario!!.recreate()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Shiva").assertExists()
        compose.onNodeWithTag("altar-garland").assertDoesNotExist()
        val saved = org.json.JSONObject(context.getSharedPreferences("devotion", Context.MODE_PRIVATE)
            .getString("guest_selection", "{}").orEmpty())
        assertEquals("divya_darshan", saved.getString("shrine"))
        assertEquals("shiva", saved.getString("idols"))
    }
}

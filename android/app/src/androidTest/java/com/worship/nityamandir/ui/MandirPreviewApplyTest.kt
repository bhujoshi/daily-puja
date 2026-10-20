package com.worship.nityamandir.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
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
            .putBoolean("hindi", false).commit()
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
        }).performClick()
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithText("Light oil lamp").assertIsDisplayed() }.isSuccess
        }
    }

    @Test fun pujaGuideExplainsTheCurrentStepAndKeepsTheDockCompact() {
        openTemple()
        compose.onNodeWithContentDescription("Step 1 of 7. Light the diya to begin")
            .assertIsDisplayed()
        assertCompactDock()
        captureLayout("puja-step-light")
        // The lit diya intentionally animates continuously. Drive frames explicitly
        // so the test doesn't wait for that animation to become idle.
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Light oil lamp").performClick()
        compose.waitUntil(10_000) {
            compose.mainClock.advanceTimeBy(100)
            compose.onAllNodesWithContentDescription("Step 2 of 7. Bathe each deity")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.advanceTimeBy(2_000) // Finish the step's slide-in transition.
        compose.onNodeWithContentDescription("Step 2 of 7. Bathe each deity")
            .assertIsDisplayed()
        compose.onNode(hasText("Ganesha") and hasAnyAncestor(hasTestTag("ritual-dock")))
            .assertIsDisplayed().assertHasClickAction()
        compose.onNode(hasText("Lakshmi") and hasAnyAncestor(hasTestTag("ritual-dock")))
            .assertIsDisplayed().assertHasClickAction()
        assertCompactDock()
        captureLayout("puja-step-bath")
        scenario!!.close()
        scenario = null
        compose.mainClock.autoAdvance = true
    }

    private fun assertCompactDock() {
        val bounds = compose.onNodeWithTag("ritual-dock").assertIsDisplayed().getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        assertTrue("Puja dock occupies $height; expected at most 180dp", height <= 180.dp)
        val guide = compose.onNodeWithTag("puja-step-guide").assertIsDisplayed().getUnclippedBoundsInRoot()
        val maximumGuideHeight = (40 * context.resources.configuration.fontScale).dp
        assertTrue("Step guidance must fit within two short lines", guide.bottom - guide.top <= maximumGuideHeight)
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
        compose.onNodeWithText("Ivory alcove").performScrollTo().performClick()
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

    @Test fun applyingPreviewChangesTheMainAltarAndSurvivesReopening() {
        openTemple()
        openCustomizer()
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
        compose.onNodeWithText("Light oil lamp").assertIsDisplayed()
        // Reopen from the real menu: the active saved selection must remain selected.
        openCustomizer()
        compose.onNodeWithText("Idols").performScrollTo().performClick()
        compose.onNodeWithText("Shiva").performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Close").performClick()
        // Recreate the host too, so a callback-only temporary change cannot pass.
        scenario!!.recreate()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Shiva").assertExists()
        val saved = org.json.JSONObject(context.getSharedPreferences("devotion", Context.MODE_PRIVATE)
            .getString("guest_selection", "{}").orEmpty())
        assertEquals("shiva", saved.getString("idols"))
    }
}

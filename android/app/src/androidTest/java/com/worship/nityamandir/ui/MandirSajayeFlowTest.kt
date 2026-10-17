package com.worship.nityamandir.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.worship.nityamandir.BuildConfig
import com.worship.nityamandir.data.DevotionAccount
import com.worship.nityamandir.data.ShrineCatalog
import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.BhajanPlayer
import com.worship.nityamandir.ui.components.DevotionSheet
import com.worship.nityamandir.ui.components.ShrineControlsTheme
import com.worship.nityamandir.ui.components.ShrineCustomizer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** Real UI/account integration against an isolated, on-device coin service. */
class MandirSajayeFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: FixtureContext
    private lateinit var origin: CoinOrigin
    private lateinit var account: DevotionAccount
    private lateinit var player: BhajanPlayer
    private val routed = AtomicReference<ShrineSelection?>()
    private val applied = AtomicReference<ShrineSelection?>()
    private val musicDraft = AtomicReference<ShrineSelection?>()

    @Before fun isolateServiceAndPreferences() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = URI(BuildConfig.ACCOUNT_API_URL)
        // Never send fixture identities to a remote account service or alter user app data.
        assumeTrue("Requires -PisolatedVerification=true", target.packageName.endsWith(".verification"))
        assumeTrue("Requires -PaccountApiUrl=http://127.0.0.1:8189",
            uri.scheme == "http" && uri.host == "127.0.0.1" && uri.port == 8189)
        context = FixtureContext(target)
        origin = CoinOrigin()
    }

    @After fun cleanUp() {
        if (::player.isInitialized) compose.runOnIdle { player.release() }
        if (::origin.isInitialized) origin.close()
        if (::context.isInitialized) context.clear()
    }

    private fun fixture(coins: Int, signedIn: Boolean = true, ivoryOwned: Boolean = false) {
        val profile = JSONObject().put("id", "fixture-${UUID.randomUUID()}")
            .put("coins", coins).put("google_account", false).put("email", "fixture@example.invalid")
            .put("owned", JSONObject().apply { if (ivoryOwned) put("shrine:ivory", true) })
            .put("coin_costs", JSONObject().put("shrine", 7).put("flowers", 2))
            .put("selections", JSONObject().put("shrine", "marble"))
            .put("days", JSONArray()).put("history", JSONArray())
            .put("share_count", 0).put("referrals", 0)
        origin.setProfile(profile)
        if (signedIn) {
            // Exercise the same encrypted session loading as a returning signed-in user.
            val vaultType = Class.forName("com.worship.nityamandir.data.SessionVault")
            val vault = vaultType.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            val session = vaultType.getDeclaredMethod("encrypt", String::class.java)
                .apply { isAccessible = true }.invoke(vault, "fixture-session") as String
            context.getSharedPreferences("devotion", Context.MODE_PRIVATE).edit()
                .putString("session", session).putString("profile", profile.toString()).commit()
        }
        compose.runOnIdle {
            account = DevotionAccount(context)
            // This fixture uses the idle player UI; release before it can connect/play audio.
            player = BhajanPlayer(context).also { it.release() }
        }
        assertEquals(signedIn, account.signedIn)
    }

    private fun showCustomizer(narrowWithLargeText: Boolean = false) {
        compose.setContent {
            // Dialog content inherits this density too: exercise a 320dp-wide viewport at 1.3x text.
            val density = if (narrowWithLargeText) {
                Density(context.resources.displayMetrics.widthPixels / 320f, fontScale = 1.3f)
            } else LocalDensity.current
            CompositionLocalProvider(LocalDensity provides density) {
                ShrineControlsTheme {
                    ShrineCustomizer(account, ShrineSelection(), false, {},
                        onAccount = { routed.set(it) }, onApplied = { applied.set(it) },
                        player = player, onMusic = { musicDraft.set(it) })
                }
            }
        }
    }

    /** Show a deterministic active player without starting a service, audio, or network requests. */
    private fun showFixtureMusic() {
        compose.runOnIdle {
            // A cancellation callback from release can still reset loading/failed. A current
            // track keeps the mini player present regardless of those transient flags.
            @Suppress("UNCHECKED_CAST")
            val current = BhajanPlayer::class.java.getDeclaredField("current\$delegate")
                .apply { isAccessible = true }.get(player) as MutableState<BhajanPlayer.Track?>
            current.value = BhajanPlayer.Track("Fixture bhajan", "https://example.invalid/fixture.mp3", "fixture")
        }
    }

    private fun selectIvoryAndRequestUnlock() {
        compose.onNodeWithText("Ivory alcove").performScrollTo().performClick()
        compose.onNodeWithText("Ivory alcove").assertIsSelected()
        compose.onNodeWithText("Cost: 7 coins").assertIsDisplayed()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Unlock")).performClick()
        compose.onNodeWithText("Unlock items?").assertIsDisplayed()
        val confirmationDialog = isDialog() and hasAnyDescendant(hasText("Unlock items?"))
        compose.onNode(hasText("Cost: 7 coins", substring = true) and hasAnyAncestor(confirmationDialog))
            .assertIsDisplayed()
    }

    @Test fun cancellingUnlockDoesNotSpendCoinsOrApplyTheDesign() {
        fixture(coins = 9)
        showCustomizer()
        selectIvoryAndRequestUnlock()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Unlock items?").assertDoesNotExist()
        compose.onNodeWithText("Ivory alcove").assertIsSelected()
        compose.runOnIdle {
            assertEquals(9, account.coins)
            assertFalse(account.owns("shrine", "ivory"))
            assertNull(applied.get())
        }
        assertEquals(0, origin.unlockRequests.get())
        assertEquals(0, origin.saveRequests.get())
    }

    @Test fun confirmingUnlockSpendsOnceSelectsIvoryAndAllowsApply() {
        fixture(coins = 9)
        showCustomizer()
        selectIvoryAndRequestUnlock()
        val confirmationDialog = isDialog() and hasAnyDescendant(hasText("Unlock items?"))
        compose.onNode(hasText("Unlock") and hasAnyAncestor(confirmationDialog)).performClick()
        compose.waitUntil(5_000) { account.coins == 2 && account.owns("shrine", "ivory") }
        compose.onNodeWithContentDescription("2 coins · Earn coins").assertIsDisplayed()
        compose.onNodeWithText("Ivory alcove").assertIsSelected()
        compose.runOnIdle { assertNull(applied.get()) }
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Apply")).performClick()
        compose.waitUntil(5_000) { applied.get() != null }
        assertEquals("ivory", applied.get()!!["shrine"])
        compose.runOnIdle { assertEquals("ivory", account.selection()["shrine"]) }
        assertEquals(1, origin.unlockRequests.get())
        assertEquals(1, origin.saveRequests.get())
        assertEquals(2, origin.coins())
    }

    @Test fun freeIdolCanBeAppliedDirectlyFromPreviewWithoutSigningIn() {
        fixture(coins = 0, signedIn = false)
        showCustomizer()
        compose.onNodeWithText("Idols").performScrollTo().performClick()
        compose.onNodeWithText("Shiva").performScrollTo().performClick()
        compose.onNodeWithText("Preview").performClick()
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Apply"))
            .assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { applied.get() != null }
        assertEquals("shiva", applied.get()!!["idols"])
        compose.runOnIdle { assertEquals("shiva", account.selection()["idols"]) }
        compose.onNodeWithContentDescription("Back to choices").assertDoesNotExist()
        assertEquals(0, origin.requests.get())
    }

    @Test fun paidShrineCanBeUnlockedAndAppliedStartingFromPreview() {
        fixture(coins = 9)
        showCustomizer()
        compose.onNodeWithText("Ivory alcove").performScrollTo().performClick()
        compose.onNodeWithText("Preview").performClick()
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Unlock"))
            .assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Back to choices").assertDoesNotExist()
        compose.onNodeWithText("Unlock items?").assertIsDisplayed()
        confirmUnlock(7)
        compose.waitUntil(5_000) { account.owns("shrine", "ivory") }
        compose.onNodeWithText("Preview").performClick()
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Apply"))
            .assertIsDisplayed().assertIsEnabled().performClick()
        try {
            // Software rendering can block the emulator main thread for several seconds
            // while rebuilding the scene after the purchase dialog.
            compose.waitUntil(15_000) { applied.get() != null }
        } catch (failure: ComposeTimeoutException) {
            throw AssertionError("Paid preview did not apply: save requests=${origin.saveRequests.get()}, " +
                "balance=${account.coins}, owns ivory=${account.owns("shrine", "ivory")}.\n" +
                compose.onNodeWithTag("mandir-preview-actions", useUnmergedTree = true).printToString(), failure)
        }
        assertEquals("ivory", applied.get()!!["shrine"])
        assertEquals(1, origin.unlockRequests.get())
        assertEquals(1, origin.saveRequests.get())
        assertEquals(2, origin.coins())
    }

    private fun selectOrchidAndRose() {
        compose.onNodeWithText("Flowers").performScrollTo().performClick()
        compose.onNodeWithText("Orchid").performScrollTo().performClick()
        compose.onNodeWithText("Rose").performScrollTo().performClick()
        assertOriginalOrchidAndRoseSelected()
    }

    private fun assertOriginalOrchidAndRoseSelected() {
        listOf("Sunflower & peony", "Orchid", "Rose").forEach {
            compose.onNodeWithText(it).assertIsOn()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        }
    }

    @Test fun flowersAreCheckBoxesAndKeepAtLeastOneVariety() {
        fixture(coins = 0, signedIn = false)
        showCustomizer()
        compose.onNodeWithText("Flowers").performScrollTo().performClick()
        compose.onNodeWithText("Select multiple · 1 selected").assertIsDisplayed()
        compose.onNodeWithText("Sunflower & peony").assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        compose.onNodeWithText("Orchid").assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        compose.onNodeWithText("Orchid").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithText("Select multiple · 2 selected").assertIsDisplayed()
        compose.onNodeWithText("Sunflower & peony").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithText("Orchid").performScrollTo().assertIsOn().performClick().assertIsOn()
        compose.onNodeWithText("Select multiple · 1 selected").assertIsDisplayed()
        compose.onNodeWithText("Keep at least one flower variety.").assertIsDisplayed()
        // The remaining selection can be replaced, and an unowned choice can be removed normally.
        compose.onNodeWithText("Sunflower & peony").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithText("Orchid").performScrollTo().performClick().assertIsOff()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Apply")).performClick()
        compose.waitUntil(5_000) { applied.get() != null }
        assertEquals(listOf("original"), applied.get()!!.flowerIds)
        assertEquals(0, origin.requests.get())
    }

    @Test fun previewActionsStayCompactAndMusicRemainsAvailable() {
        fixture(coins = 0, signedIn = false)
        showFixtureMusic()
        showCustomizer()
        assertCompactHeight("mandir-choice-music", 56)
        compose.onNodeWithText("Flowers").performScrollTo().performClick()
        compose.onNodeWithText("Select multiple · 1 selected").assertIsDisplayed()
        captureLayout("mandir-flowers-compact")
        compose.onNodeWithText("Preview").performClick()
        assertCompactHeight("mandir-preview-actions", 120)
        assertCompactHeight("mandir-preview-music", 56)
        captureLayout("mandir-preview-compact")
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Apply")).assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to choices").assertIsDisplayed().performClick()
        compose.onNodeWithText("Preview").assertIsDisplayed()
        compose.runOnIdle { assertNull(applied.get()) }
        assertEquals(0, origin.requests.get())
    }

    private fun assertCompactHeight(tag: String, maximumDp: Int) {
        val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        android.util.Log.i("MandirLayout", "$tag height=$height")
        assertTrue("$tag occupies $height; expected at most ${maximumDp}dp", height <= maximumDp.dp)
    }

    private fun captureLayout(name: String) {
        // Capture native SceneView and popup windows together for visual review.
        compose.waitForIdle()
        // Native rendering is independent of Compose's test clock.
        Thread.sleep(if(name.contains("preview")) 3_000 else 500)
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/Download/$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    @Test fun previewAndApplyRemainVisibleWithMusicOnNarrowScreenAndLargerText() {
        fixture(coins = 0, signedIn = false)
        showFixtureMusic()
        showCustomizer(narrowWithLargeText = true)
        compose.onNodeWithText("Preview").assertIsDisplayed().performClick()
        compose.onNodeWithTag("mandir-preview-music").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to choices").assertIsDisplayed()
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Apply"))
            .assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { applied.get() != null }
        compose.onNodeWithContentDescription("Back to choices").assertDoesNotExist()
        assertEquals(0, origin.requests.get())
    }

    @Test fun openingMusicFromPreviewKeepsTheMultiFlowerDraft() {
        fixture(coins = 0, signedIn = false)
        showFixtureMusic()
        showCustomizer()
        selectOrchidAndRose()
        compose.onNodeWithText("Preview").performClick()
        compose.onNode(SemanticsMatcher("Open music player action") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open music player"
        } and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithContentDescription("Back to choices").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("original", "orchid", "rose"), musicDraft.get()!!.flowerIds)
            assertNull(applied.get())
        }
        assertEquals(0, origin.requests.get())
    }

    private fun confirmUnlock(cost: Int) {
        val confirmationDialog = isDialog() and hasAnyDescendant(hasText("Unlock items?"))
        compose.onNode(hasText("Cost: $cost coins", substring = true) and hasAnyAncestor(confirmationDialog))
            .assertIsDisplayed()
        compose.onNode(hasText("Unlock") and hasAnyAncestor(confirmationDialog)).performClick()
    }

    @Test fun batchFlowerUnlockChargesFourCoinsAndKeepsAllSelectedFlowers() {
        fixture(coins = 10)
        showCustomizer()
        selectOrchidAndRose()
        compose.onNodeWithText("Cost: 4 coins").assertIsDisplayed()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Unlock")).performClick()
        confirmUnlock(4)
        compose.waitUntil(5_000) {
            account.coins == 6 && account.owns("flowers", "orchid") && account.owns("flowers", "rose")
        }
        assertOriginalOrchidAndRoseSelected()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Apply")).performClick()
        compose.waitUntil(5_000) { applied.get() != null }
        assertEquals(listOf("original", "orchid", "rose"), applied.get()!!.flowerIds)
        compose.runOnIdle { assertEquals(applied.get()!!.flowerIds, account.selection().flowerIds) }
        assertEquals(2, origin.unlockRequests.get())
        assertEquals(1, origin.unlockCount("flowers:orchid"))
        assertEquals(1, origin.unlockCount("flowers:rose"))
        assertEquals(1, origin.saveRequests.get())
        assertEquals(6, origin.coins())
    }

    @Test fun partialBatchFailureKeepsFirstUnlockAndRetriesOnlyTheRemainingCost() {
        fixture(coins = 10)
        origin.failNextUnlock("flowers:rose")
        showCustomizer()
        selectOrchidAndRose()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Unlock")).performClick()
        confirmUnlock(4)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Could not complete this action.", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Could not complete this action.", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(8, account.coins)
            assertTrue(account.owns("flowers", "orchid"))
            assertFalse(account.owns("flowers", "rose"))
            assertNull(applied.get())
        }
        assertOriginalOrchidAndRoseSelected()
        compose.onNodeWithText("Cost: 2 coins").assertIsDisplayed()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Unlock")).performClick()
        val confirmationDialog = isDialog() and hasAnyDescendant(hasText("Unlock items?"))
        compose.onNode(hasText("Your balance: 8 coins", substring = true) and hasAnyAncestor(confirmationDialog))
            .assertIsDisplayed()
        confirmUnlock(2)
        compose.waitUntil(5_000) { account.coins == 6 && account.owns("flowers", "rose") }
        assertOriginalOrchidAndRoseSelected()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Apply")).performClick()
        compose.waitUntil(5_000) { applied.get() != null }
        assertEquals(listOf("original", "orchid", "rose"), applied.get()!!.flowerIds)
        assertEquals(3, origin.unlockRequests.get())
        assertEquals(1, origin.unlockCount("flowers:orchid"))
        assertEquals(2, origin.unlockCount("flowers:rose"))
        assertEquals(1, origin.saveRequests.get())
        assertEquals(6, origin.coins())
    }

    @Test fun insufficientCoinsRoutesToEarningWithTheRequestedDesign() {
        fixture(coins = 1)
        showCustomizer()
        compose.onNodeWithText("Ivory alcove").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Cost: 7 coins").assertIsDisplayed()
        compose.runOnIdle { assertNull(routed.get()) }
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Unlock")).performClick()
        compose.runOnIdle {
            assertEquals("ivory", routed.get()!!["shrine"])
            assertEquals(1, account.coins)
            assertFalse(account.owns("shrine", "ivory"))
        }
        compose.onNodeWithText("Unlock items?").assertDoesNotExist()
        assertEquals(0, origin.requests.get())
        assertNull(applied.get())
    }

    @Test fun guestRoutesToSignInWithTheRequestedDesignWithoutNetwork() {
        fixture(coins = 0, signedIn = false)
        showCustomizer()
        compose.onNodeWithText("Ivory alcove").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Cost: 7 coins").assertIsDisplayed()
        compose.runOnIdle { assertNull(routed.get()) }
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Unlock")).performClick()
        compose.runOnIdle {
            assertFalse(account.signedIn)
            assertEquals("ivory", routed.get()!!["shrine"])
        }
        assertEquals(0, origin.requests.get())
        assertNull(applied.get())
    }

    @Test fun guestPreviewKeepsUnlockLabelAndRoutesToSignInOnlyAfterTapping() {
        fixture(coins = 0, signedIn = false)
        showCustomizer()
        compose.onNodeWithText("Ivory alcove").performScrollTo().performClick()
        compose.onNode(hasTestTag("mandir-selection-action") and hasText("Unlock")).assertIsDisplayed()
        compose.onNodeWithText("Cost: 7 coins").assertIsDisplayed()
        compose.onNodeWithText("Unlock selection", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Sign in to unlock").assertDoesNotExist()
        captureLayout("mandir-locked-choice-simple")
        compose.runOnIdle { assertNull(routed.get()) }
        compose.onNodeWithText("Preview").performClick()
        compose.onNode(hasTestTag("mandir-preview-action") and hasText("Unlock")).assertIsDisplayed()
        compose.onNode(hasText("Cost: 7 coins") and hasAnyAncestor(hasTestTag("mandir-preview-actions")))
            .assertIsDisplayed()
        captureLayout("mandir-locked-preview-simple")
        compose.onNodeWithTag("mandir-preview-action").performClick()
        compose.runOnIdle {
            assertEquals("ivory", routed.get()!!["shrine"])
            assertFalse(account.signedIn)
            assertNull(applied.get())
        }
        compose.onNodeWithText("Unlock items?").assertDoesNotExist()
        assertEquals(0, origin.requests.get())
    }

    @Test fun profileShowsNewOwnedItemAndOpensMandirSajayeDirectly() {
        fixture(coins = 0, ivoryOwned = true)
        val opened = AtomicInteger()
        compose.setContent {
            ShrineControlsTheme {
                DevotionSheet(account, false, {}, { opened.incrementAndGet() },
                    player = player, onMusic = {})
            }
        }
        compose.waitUntil(5_000) {
            origin.refreshRequests.get() == 1 &&
                compose.onAllNodes(hasText("Mandir Sajaye") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("1 newly unlocked item is ready").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ivory alcove").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Mandir Sajaye").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, opened.get())
        compose.onNodeWithText("View coins & unlocks").assertDoesNotExist()
        compose.runOnIdle { assertEquals("ivory", account.newUnlocks(ShrineCatalog(context)).single().option.id) }
        assertEquals(0, origin.unlockRequests.get())
    }

    /** Routes all preferences to unique names; no account, progress or music fixtures leak. */
    private class FixtureContext(base: Context) : ContextWrapper(base) {
        private val prefix = "mandir-sajaye-test-${UUID.randomUUID()}-"
        private val names = mutableSetOf<String>()
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
            val isolatedName = prefix + name
            names.add(isolatedName)
            return super.getSharedPreferences(isolatedName, mode)
        }
        fun clear() { names.forEach { super.getSharedPreferences(it, MODE_PRIVATE).edit().clear().commit() } }
    }

    private class CoinOrigin : AutoCloseable {
        val requests = AtomicInteger()
        val refreshRequests = AtomicInteger()
        val unlockRequests = AtomicInteger()
        val saveRequests = AtomicInteger()
        private var profile = JSONObject()
        private var failOnceItem: String? = null
        private val itemRequests = mutableMapOf<String, Int>()
        private val failures = AtomicReference<Throwable?>()
        private val socket = ServerSocket(8189, 8, InetAddress.getByName("127.0.0.1"))
        @Synchronized fun setProfile(value: JSONObject) { profile = JSONObject(value.toString()) }
        @Synchronized fun coins() = profile.getInt("coins")
        @Synchronized fun failNextUnlock(id: String) { failOnceItem = id }
        @Synchronized fun unlockCount(id: String) = itemRequests[id] ?: 0

        @Synchronized private fun respond(path: String, body: JSONObject): Pair<Int, JSONObject> {
            requests.incrementAndGet()
            when (path) {
                "/api/v2/me" -> refreshRequests.incrementAndGet()
                "/api/v2/coins/unlock" -> {
                    unlockRequests.incrementAndGet()
                    val category = body.getString("category")
                    val item = body.getString("item")
                    val id = "$category:$item"
                    itemRequests[id] = (itemRequests[id] ?: 0) + 1
                    if (failOnceItem == id) {
                        failOnceItem = null
                        return 503 to JSONObject().put("error", "Fixture temporarily unavailable")
                    }
                    val owned = profile.getJSONObject("owned")
                    if (!owned.optBoolean(id)) {
                        val cost = if (category == "shrine") 7 else if (category == "flowers") 2 else 0
                        if (profile.getInt("coins") < cost) return 409 to JSONObject().put("error", "Not enough coins")
                        profile.put("coins", profile.getInt("coins") - cost)
                        owned.put(id, true)
                    }
                }
                "/api/v2/shrine" -> {
                    saveRequests.incrementAndGet()
                    profile.put("selections", body.getJSONObject("selections"))
                }
                else -> return 404 to JSONObject().put("error", "Unexpected fixture request: $path")
            }
            return 200 to JSONObject(profile.toString())
        }

        private val worker = thread(name = "mandir-sajaye-fixture", isDaemon = true) {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { client ->
                        client.soTimeout = 3_000
                        val input = client.getInputStream().bufferedReader(Charsets.UTF_8)
                        val requestLine = input.readLine() ?: return@use
                        val path = requestLine.split(' ')[1]
                        var contentLength = 0
                        while (true) {
                            val line = input.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Content-Length:", true)) contentLength = line.substringAfter(':').trim().toInt()
                        }
                        val chars = CharArray(contentLength)
                        var offset = 0
                        while (offset < contentLength) {
                            val count = input.read(chars, offset, contentLength - offset)
                            check(count > 0) { "Incomplete fixture request" }
                            offset += count
                        }
                        val body = if (contentLength == 0) JSONObject() else JSONObject(String(chars))
                        val (status, response) = respond(path, body)
                        val bytes = response.toString().toByteArray(Charsets.UTF_8)
                        client.getOutputStream().apply {
                            write(("HTTP/1.1 $status OK\r\nContent-Type: application/json\r\n" +
                                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray(Charsets.UTF_8))
                            write(bytes)
                            flush()
                        }
                    }
                } catch (error: Exception) {
                    if (!socket.isClosed) failures.compareAndSet(null, error)
                }
            }
        }

        override fun close() {
            socket.close()
            worker.join(3_000)
            failures.get()?.let { throw AssertionError("Local coin fixture failed", it) }
        }
    }
}

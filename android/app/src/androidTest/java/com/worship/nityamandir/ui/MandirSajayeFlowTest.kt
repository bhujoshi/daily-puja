package com.worship.nityamandir.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

    private fun fixture(coins: Int, signedIn: Boolean = true, marbleOwned: Boolean = false) {
        val profile = JSONObject().put("id", "fixture-${UUID.randomUUID()}")
            .put("coins", coins).put("google_account", false).put("email", "fixture@example.invalid")
            .put("owned", JSONObject().apply { if (marbleOwned) put("shrine:marble", true) })
            .put("coin_costs", JSONObject().put("shrine", 7).put("flowers", 2))
            .put("selections", JSONObject().put("shrine", "original"))
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

    private fun showCustomizer() {
        compose.setContent {
            ShrineControlsTheme {
                ShrineCustomizer(account, ShrineSelection(), false, {},
                    onAccount = { routed.set(it) }, onApplied = { applied.set(it) },
                    player = player, onMusic = {})
            }
        }
    }

    private fun selectMarbleAndRequestUnlock() {
        compose.onNodeWithText("Marble arch").performScrollTo().performClick()
        compose.onNodeWithText("Marble arch").assertIsSelected()
        compose.onNodeWithText("Unlock selection · 7 coins").performClick()
        compose.onNodeWithText("Unlock items?").assertIsDisplayed()
        compose.onNodeWithText("Cost: 7 coins", substring = true).assertIsDisplayed()
    }

    @Test fun cancellingUnlockDoesNotSpendCoinsOrApplyTheDesign() {
        fixture(coins = 9)
        showCustomizer()
        selectMarbleAndRequestUnlock()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Unlock items?").assertDoesNotExist()
        compose.onNodeWithText("Marble arch").assertIsSelected()
        compose.runOnIdle {
            assertEquals(9, account.coins)
            assertFalse(account.owns("shrine", "marble"))
            assertNull(applied.get())
        }
        assertEquals(0, origin.unlockRequests.get())
        assertEquals(0, origin.saveRequests.get())
    }

    @Test fun confirmingUnlockSpendsOnceSelectsMarbleAndAllowsApply() {
        fixture(coins = 9)
        showCustomizer()
        selectMarbleAndRequestUnlock()
        val confirmationDialog = isDialog() and hasAnyDescendant(hasText("Unlock items?"))
        compose.onNode(hasText("Unlock · 7 coins") and hasAnyAncestor(confirmationDialog)).performClick()
        compose.waitUntil(5_000) { account.coins == 2 && account.owns("shrine", "marble") }
        compose.onNodeWithText("2 coins").assertIsDisplayed()
        compose.onNodeWithText("Marble arch").assertIsSelected()
        compose.runOnIdle { assertNull(applied.get()) }
        compose.onNodeWithText("Apply this temple").performClick()
        compose.waitUntil(5_000) { applied.get() != null }
        assertEquals("marble", applied.get()!!["shrine"])
        compose.runOnIdle { assertEquals("marble", account.selection()["shrine"]) }
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
            compose.onNodeWithText(it).assertIsSelected()
        }
    }

    private fun confirmUnlock(cost: Int) {
        val confirmationDialog = isDialog() and hasAnyDescendant(hasText("Unlock items?"))
        compose.onNode(hasText("Unlock · $cost coins") and hasAnyAncestor(confirmationDialog)).performClick()
    }

    @Test fun batchFlowerUnlockChargesFourCoinsAndKeepsAllSelectedFlowers() {
        fixture(coins = 10)
        showCustomizer()
        selectOrchidAndRose()
        compose.onNodeWithText("Unlock selection · 4 coins").performClick()
        compose.onNodeWithText("Cost: 4 coins", substring = true).assertIsDisplayed()
        confirmUnlock(4)
        compose.waitUntil(5_000) {
            account.coins == 6 && account.owns("flowers", "orchid") && account.owns("flowers", "rose")
        }
        assertOriginalOrchidAndRoseSelected()
        compose.onNodeWithText("Apply this temple").performClick()
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
        compose.onNodeWithText("Unlock selection · 4 coins").performClick()
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
        compose.onNodeWithText("Unlock selection · 2 coins").performClick()
        compose.onNodeWithText("Cost: 2 coins", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Your balance: 8 coins", substring = true).assertIsDisplayed()
        confirmUnlock(2)
        compose.waitUntil(5_000) { account.coins == 6 && account.owns("flowers", "rose") }
        assertOriginalOrchidAndRoseSelected()
        compose.onNodeWithText("Apply this temple").performClick()
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
        compose.onNode(hasText("Earn coins") and hasAnyAncestor(hasText("Marble arch")))
            .performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("marble", routed.get()!!["shrine"])
            assertEquals(1, account.coins)
            assertFalse(account.owns("shrine", "marble"))
        }
        compose.onNodeWithText("Unlock items?").assertDoesNotExist()
        assertEquals(0, origin.requests.get())
        assertNull(applied.get())
    }

    @Test fun guestRoutesToSignInWithTheRequestedDesignWithoutNetwork() {
        fixture(coins = 0, signedIn = false)
        showCustomizer()
        compose.onNode(hasText("Sign in to unlock") and hasAnyAncestor(hasText("Marble arch")))
            .performScrollTo().performClick()
        compose.runOnIdle {
            assertFalse(account.signedIn)
            assertEquals("marble", routed.get()!!["shrine"])
        }
        assertEquals(0, origin.requests.get())
        assertNull(applied.get())
    }

    @Test fun profileShowsNewOwnedItemAndOpensMandirSajayeDirectly() {
        fixture(coins = 0, marbleOwned = true)
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
        compose.onNodeWithText("Marble arch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Mandir Sajaye").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, opened.get())
        compose.onNodeWithText("View coins & unlocks").assertDoesNotExist()
        compose.runOnIdle { assertEquals("marble", account.newUnlocks(ShrineCatalog(context)).single().option.id) }
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

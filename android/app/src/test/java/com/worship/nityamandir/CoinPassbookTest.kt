package com.worship.nityamandir

import com.worship.nityamandir.data.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CoinPassbookTest {
    private val today = LocalDate.parse("2026-10-20")
    private val categories = listOf(
        ShrineCategory(
            "shrine", "Shrine", "मंदिर",
            listOf(
                ShrineOption("marble", "Marble arch", "संगमरमर मंदिर", "", "image", ""),
                ShrineOption("ivory", "Ivory alcove", "हाथीदाँत रंग का मंदिर", "", "image", "")
            )
        ),
        ShrineCategory(
            "flowers", "Flowers", "फूल",
            listOf(
                ShrineOption("rose", "Rose", "गुलाब", "", "model", "")
            )
        )
    )

    @Test fun passbookFiltersNonCoinNoiseAndResolvesCatalogUnlocksNewestFirst() {
        val events = listOf(
            RawHistoryEvent("1", "account_created", "2026-10-15T08:00:00Z", "Google account created"),
            RawHistoryEvent("2", "puja_completed", "2026-10-18T08:30:00Z", "2026-10-18 · +1 coin"),
            RawHistoryEvent("3", "whatsapp_opened", "2026-10-18T09:00:00Z", "WhatsApp invitation opened"),
            RawHistoryEvent("4", "referral_coin", "2026-10-19T10:00:00Z", "New referred installation · +1 coin"),
            RawHistoryEvent("5", "shrine_saved", "2026-10-19T11:00:00Z", "{\"shrine\":\"marble\"}"),
            RawHistoryEvent("6", "coin_unlock", "2026-10-20T12:00:00Z", "flowers:rose")
        )

        val ledger = CoinPassbook.build(
            balance = 3,
            history = events,
            categories = categories,
            today = today,
            pendingToday = false
        )

        assertEquals(3, ledger.balance)
        assertEquals(2, ledger.totalSpent)
        assertEquals(5, ledger.totalEarned) // max(2 earned in history, 3 balance + 2 spent)
        assertEquals(3, ledger.entries.size)

        // Newest entry first: flower unlock
        val first = ledger.entries[0]
        assertEquals(PassbookEntryKind.ITEM_UNLOCK, first.kind)
        assertEquals(-2, first.delta)
        assertEquals("Unlocked Rose", first.titleEn)
        assertEquals("गुलाब अनलॉक", first.titleHi)
        assertEquals("Flowers · Today", first.subtitleEn)
        assertEquals("फूल · आज", first.subtitleHi)

        // Second entry: referral coin yesterday
        val second = ledger.entries[1]
        assertEquals(PassbookEntryKind.REFERRAL_REWARD, second.kind)
        assertEquals(1, second.delta)
        assertEquals("Friend referral reward", second.titleEn)
        assertEquals("Yesterday", second.subtitleEn)
        assertEquals("कल", second.subtitleHi)

        // Third entry: puja completed on 18 Oct
        val third = ledger.entries[2]
        assertEquals(PassbookEntryKind.PUJA_REWARD, third.kind)
        assertEquals(1, third.delta)
        assertEquals("18 Oct", third.subtitleEn)
        assertEquals("18 अक्टू", third.subtitleHi)

        assertEquals(2, ledger.filtered(PassbookFilter.EARNED).size)
        assertEquals(1, ledger.filtered(PassbookFilter.SPENT).size)
    }

    @Test fun pendingOfflinePujaAppearsOnceAtTopUntilSynced() {
        val ledger = CoinPassbook.build(
            balance = 1,
            history = emptyList(),
            categories = categories,
            today = today,
            pendingToday = true
        )
        assertEquals(1, ledger.entries.size)
        val entry = ledger.entries.single()
        assertTrue(entry.pendingSync)
        assertEquals(1, entry.delta)
        assertEquals("Today · Sync pending", entry.subtitleEn)

        // Once synced into history for today, do not duplicate the pending entry.
        val synced = CoinPassbook.build(
            balance = 1,
            history = listOf(RawHistoryEvent("1", "puja_completed", "2026-10-20T09:00:00Z", "2026-10-20 · +1 coin")),
            categories = categories,
            today = today,
            pendingToday = true
        )
        assertEquals(1, synced.entries.size)
        assertFalse(synced.entries.single().pendingSync)
    }
}

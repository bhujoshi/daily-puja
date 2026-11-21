package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineCategory
import com.worship.nityamandir.data.ShrineOption
import com.worship.nityamandir.data.ShrineUnlocks
import org.junit.Assert.*
import org.junit.Test

class ShrineUnlocksTest {
    private fun category(id: String, vararg options: String) = ShrineCategory(
        id, id, id, options.map { ShrineOption(it, it, it, "", "image", "") }
    )
    private val categories = listOf(
        category("shrine", "marble", "ivory", "carved", "swarna"),
        category("flowers", "original", "rose", "azalea", "bouquet"),
        category("idols", "original", "shiva", "shanidev"),
        category("incense", "original", "dhoop", "mayur_dhoop")
    )
    private fun owns(ids: Set<String>): (String, String) -> Boolean = { category, item ->
        "$category:$item" in ids
    }

    @Test fun newUnlocksIncludeOnlyOwnedPaidItemsThatHaveNotBeenSeen() {
        val owned = setOf("shrine:ivory", "flowers:azalea", "flowers:bouquet", "idols:shanidev", "incense:mayur_dhoop")
        val fresh = ShrineUnlocks.newUnlocks(categories, setOf("flowers:azalea"), owns(owned))
        assertEquals(listOf("flowers:bouquet", "incense:mayur_dhoop"), fresh.map { it.id })
        assertEquals(2, fresh.first().cost)
        assertEquals("flowers", fresh.first().category.id)
        assertEquals("bouquet", fresh.first().option.id)
        assertEquals(3, fresh.last().cost)
    }

    @Test fun availableUnlocksUseExactBalanceAndExcludeOwnedItems() {
        val owned = owns(setOf("flowers:azalea"))
        assertTrue(ShrineUnlocks.availableUnlocks(categories, -1, owned).isEmpty())
        assertTrue(ShrineUnlocks.availableUnlocks(categories, 1, owned).isEmpty())
        assertEquals(listOf("flowers:bouquet"), ShrineUnlocks.availableUnlocks(categories, 2, owned).map { it.id })
        assertEquals(listOf("flowers:bouquet", "incense:mayur_dhoop"),
            ShrineUnlocks.availableUnlocks(categories, 3, owned).map { it.id })
        assertTrue(ShrineUnlocks.availableUnlocks(categories, 100, { _, _ -> true }).isEmpty())
    }

    @Test fun viewingUnlocksRetainsHistoryAndLeavesFuturePurchasesNew() {
        val previous = setOf("shrine:retired", "flowers:azalea")
        val owned = setOf("shrine:ivory", "flowers:azalea", "idols:shanidev")
        val seen = ShrineUnlocks.seenAfterViewing(categories, previous, owns(owned))
        assertEquals(previous, seen)
        assertTrue(ShrineUnlocks.newUnlocks(categories, seen, owns(owned)).isEmpty())
        assertEquals(listOf("incense:mayur_dhoop"),
            ShrineUnlocks.newUnlocks(categories, seen, owns(owned + "incense:mayur_dhoop")).map { it.id })
    }

    @Test fun purchaseAcknowledgementOnlyMarksSuccessfullyOwnedPaidItem() {
        val seen = setOf("flowers:azalea")
        assertEquals(seen, ShrineUnlocks.seenAfterUnlock(seen, "flowers", "bouquet", owns = false, cost = 2))
        assertEquals(seen, ShrineUnlocks.seenAfterUnlock(seen, "idols", "shanidev", owns = true, cost = 0))
        assertEquals(seen + "flowers:bouquet",
            ShrineUnlocks.seenAfterUnlock(seen, "flowers", "bouquet", owns = true, cost = 2))
    }

    @Test fun acknowledgementKeysKeepAccountsSeparateAndHaveNoGuestFallback() {
        assertNull(ShrineUnlocks.seenPreferenceKey(null))
        assertNull(ShrineUnlocks.seenPreferenceKey(""))
        assertNull(ShrineUnlocks.seenPreferenceKey("  "))
        val firstKey = ShrineUnlocks.seenPreferenceKey("first")!!
        val secondKey = ShrineUnlocks.seenPreferenceKey("second")!!
        assertNotEquals(firstKey, secondKey)
        val stored = mapOf(firstKey to setOf("flowers:azalea"))
        val firstNew = ShrineUnlocks.newUnlocks(categories, stored[firstKey].orEmpty(), owns(setOf("flowers:azalea")))
        val secondNew = ShrineUnlocks.newUnlocks(categories, stored[secondKey].orEmpty(), owns(setOf("flowers:azalea")))
        assertTrue(firstNew.isEmpty())
        assertEquals(listOf("flowers:azalea"), secondNew.map { it.id })
        assertEquals(firstKey, ShrineUnlocks.seenPreferenceKey("first"))
    }

    @Test fun allShrinesIdolsAndDailyPresetsAreFreeWhileOptionalExtrasRetainServerPricing() {
        for (shrine in listOf("original", "marble", "diwali", "divya_darshan", "kamal_peeth", "swarna_peeth", "ivory", "carved", "swarna")) {
            assertEquals(0, ShrineUnlocks.cost("shrine", shrine))
            assertEquals(0, ShrineUnlocks.cost("shrine", shrine, 9))
        }
        for (idol in listOf("original", "ganesh_hanuman", "radha_krishna", "shiva", "lakshmi", "durga", "ram_darbar", "hanuman", "shanidev")) {
            assertEquals(0, ShrineUnlocks.cost("idols", idol))
        }
        assertEquals(0, ShrineUnlocks.cost("garland", "original"))
        assertEquals(0, ShrineUnlocks.cost("garland", "rose"))
        assertEquals(0, ShrineUnlocks.cost("garland", "lotus"))
        assertEquals(0, ShrineUnlocks.cost("garland", "mogra"))
        assertEquals(0, ShrineUnlocks.cost("incense", "original"))
        assertEquals(0, ShrineUnlocks.cost("incense", "dhoop"))
        assertEquals(0, ShrineUnlocks.cost("incense", "kamal_dhoop"))
        assertEquals(3, ShrineUnlocks.cost("incense", "mayur_dhoop"))
        assertEquals(0, ShrineUnlocks.cost("lamp", "brass"))
        assertEquals(0, ShrineUnlocks.cost("lamp", "peacock"))
        assertEquals(0, ShrineUnlocks.cost("shankh", "ivory"))
        assertEquals(0, ShrineUnlocks.cost("shankh", "swarna"))
        assertEquals(0, ShrineUnlocks.cost("aarti", "traditional"))
        assertEquals(0, ShrineUnlocks.cost("aarti", "karpura"))
        assertEquals(0, ShrineUnlocks.cost("prasad", "halwa"))
        assertEquals(0, ShrineUnlocks.cost("prasad", "laddu"))
        assertEquals(0, ShrineUnlocks.cost("prasad", "modak"))
        assertEquals(0, ShrineUnlocks.cost("flowers", "rose"))
        assertEquals(0, ShrineUnlocks.cost("flowers", "orchid"))
        assertEquals(0, ShrineUnlocks.cost("flowers", "mogra_flower"))
        assertEquals(2, ShrineUnlocks.cost("flowers", "azalea"))
        assertEquals(2, ShrineUnlocks.cost("flowers", "bouquet"))
        assertEquals(3, ShrineUnlocks.cost("incense", "mayur_dhoop", -1))

        val pricing: (String, String) -> Int = { category, item ->
            ShrineUnlocks.cost(category, item, mapOf("incense" to 4, "flowers" to 0)[category])
        }
        assertTrue(ShrineUnlocks.availableUnlocks(categories, 3, { _, _ -> false }, pricing).isEmpty())
        assertEquals(listOf("incense:mayur_dhoop"),
            ShrineUnlocks.availableUnlocks(categories, 4, { _, _ -> false }, pricing).map { it.id })
        assertEquals(listOf("incense:mayur_dhoop"),
            ShrineUnlocks.newUnlocks(categories, emptySet(), { _, _ -> true }, pricing).map { it.id })
    }
}

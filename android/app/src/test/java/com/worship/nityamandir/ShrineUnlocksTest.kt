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
        category("shrine", "original", "marble", "ivory"),
        category("flowers", "original", "rose", "ivory"),
        category("idols", "original", "shiva"),
        category("lamp", "original", "brass")
    )
    private fun owns(ids: Set<String>): (String, String) -> Boolean = { category, item ->
        "$category:$item" in ids
    }

    @Test fun newUnlocksIncludeOnlyOwnedPaidItemsThatHaveNotBeenSeen() {
        val owned = setOf("shrine:marble", "flowers:rose", "idols:shiva", "lamp:brass", "shrine:original")
        val fresh = ShrineUnlocks.newUnlocks(categories, setOf("shrine:marble"), owns(owned))
        assertEquals(listOf("flowers:rose"), fresh.map { it.id })
        assertEquals(2, fresh.single().cost)
        assertEquals("flowers", fresh.single().category.id)
        assertEquals("rose", fresh.single().option.id)
    }

    @Test fun availableUnlocksUseExactBalanceAndExcludeOwnedItems() {
        val owned = owns(setOf("flowers:rose"))
        assertTrue(ShrineUnlocks.availableUnlocks(categories, -1, owned).isEmpty())
        assertTrue(ShrineUnlocks.availableUnlocks(categories, 1, owned).isEmpty())
        assertEquals(listOf("flowers:ivory"), ShrineUnlocks.availableUnlocks(categories, 2, owned).map { it.id })
        assertEquals(listOf("flowers:ivory"), ShrineUnlocks.availableUnlocks(categories, 6, owned).map { it.id })
        assertEquals(listOf("shrine:marble", "shrine:ivory", "flowers:ivory"),
            ShrineUnlocks.availableUnlocks(categories, 7, owned).map { it.id })
        assertTrue(ShrineUnlocks.availableUnlocks(categories, 100, { _, _ -> true }).isEmpty())
    }

    @Test fun viewingUnlocksRetainsHistoryAndLeavesFuturePurchasesNew() {
        val previous = setOf("shrine:retired", "flowers:rose")
        val owned = setOf("shrine:marble", "flowers:rose", "idols:shiva")
        val seen = ShrineUnlocks.seenAfterViewing(categories, previous, owns(owned))
        assertEquals(previous + "shrine:marble", seen)
        assertTrue(ShrineUnlocks.newUnlocks(categories, seen, owns(owned)).isEmpty())
        assertEquals(listOf("shrine:ivory"),
            ShrineUnlocks.newUnlocks(categories, seen, owns(owned + "shrine:ivory")).map { it.id })
    }

    @Test fun purchaseAcknowledgementOnlyMarksSuccessfullyOwnedPaidItem() {
        val seen = setOf("shrine:marble")
        assertEquals(seen, ShrineUnlocks.seenAfterUnlock(seen, "flowers", "rose", owns = false, cost = 2))
        assertEquals(seen, ShrineUnlocks.seenAfterUnlock(seen, "idols", "shiva", owns = true, cost = 0))
        assertEquals(seen + "flowers:rose",
            ShrineUnlocks.seenAfterUnlock(seen, "flowers", "rose", owns = true, cost = 2))
    }

    @Test fun acknowledgementKeysKeepAccountsSeparateAndHaveNoGuestFallback() {
        assertNull(ShrineUnlocks.seenPreferenceKey(null))
        assertNull(ShrineUnlocks.seenPreferenceKey(""))
        assertNull(ShrineUnlocks.seenPreferenceKey("  "))
        val firstKey = ShrineUnlocks.seenPreferenceKey("first")!!
        val secondKey = ShrineUnlocks.seenPreferenceKey("second")!!
        assertNotEquals(firstKey, secondKey)
        val stored = mapOf(firstKey to setOf("shrine:marble"))
        val firstNew = ShrineUnlocks.newUnlocks(categories, stored[firstKey].orEmpty(), owns(setOf("shrine:marble")))
        val secondNew = ShrineUnlocks.newUnlocks(categories, stored[secondKey].orEmpty(), owns(setOf("shrine:marble")))
        assertTrue(firstNew.isEmpty())
        assertEquals(listOf("shrine:marble"), secondNew.map { it.id })
        assertEquals(firstKey, ShrineUnlocks.seenPreferenceKey("first"))
    }

    @Test fun serverPricingOverridesFallbackAndFreeItemsNeverCreateBadges() {
        assertEquals(7, ShrineUnlocks.cost("shrine", "marble"))
        assertEquals(2, ShrineUnlocks.cost("flowers", "rose"))
        assertEquals(0, ShrineUnlocks.cost("idols", "shiva"))
        assertEquals(0, ShrineUnlocks.cost("shrine", "original", 9))
        assertEquals(7, ShrineUnlocks.cost("shrine", "marble", -1))
        val pricing: (String, String) -> Int = { category, item ->
            ShrineUnlocks.cost(category, item, mapOf("shrine" to 4, "flowers" to 0)[category])
        }
        assertTrue(ShrineUnlocks.availableUnlocks(categories, 3, { _, _ -> false }, pricing).isEmpty())
        assertEquals(listOf("shrine:marble", "shrine:ivory"),
            ShrineUnlocks.availableUnlocks(categories, 4, { _, _ -> false }, pricing).map { it.id })
        assertEquals(listOf("shrine:marble", "shrine:ivory"),
            ShrineUnlocks.newUnlocks(categories, emptySet(), { _, _ -> true }, pricing).map { it.id })
    }
}

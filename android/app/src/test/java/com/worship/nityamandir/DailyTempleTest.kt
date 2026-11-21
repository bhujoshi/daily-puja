package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelectionSync
import com.worship.nityamandir.data.ShrineUnlocks
import com.worship.nityamandir.engine.DailyTempleSync
import com.worship.nityamandir.engine.DailyTemples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

class DailyTempleTest {

    @Test
    fun `maps every day of the week to the expected deity and unique shrine`() {
        assertEquals(DayOfWeek.values().toSet(), DailyTemples.all.map { it.dayOfWeek }.toSet())
        assertEquals(7, DailyTemples.all.map { it.presetSelection["shrine"] }.toSet().size)

        assertEquals("shiva", DailyTemples.forDay(DayOfWeek.MONDAY).presetSelection["idols"])
        assertEquals("hanuman", DailyTemples.forDay(DayOfWeek.TUESDAY).presetSelection["idols"])
        assertEquals("ganesh_hanuman", DailyTemples.forDay(DayOfWeek.WEDNESDAY).presetSelection["idols"])
        assertEquals("radha_krishna", DailyTemples.forDay(DayOfWeek.THURSDAY).presetSelection["idols"])
        assertEquals("durga", DailyTemples.forDay(DayOfWeek.FRIDAY).presetSelection["idols"])
        assertEquals("shanidev", DailyTemples.forDay(DayOfWeek.SATURDAY).presetSelection["idols"])
        assertEquals("lakshmi", DailyTemples.forDay(DayOfWeek.SUNDAY).presetSelection["idols"])

        // 2026-11-21 is Saturday -> Shani Dev; 2026-11-22 is Sunday -> Maa Lakshmi
        assertEquals("saturday_shanidev", DailyTemples.forDate(LocalDate.of(2026, 11, 21)).id)
        assertEquals("sunday_lakshmi", DailyTemples.forDate(LocalDate.of(2026, 11, 22)).id)
    }

    @Test
    fun `every daily temple preset uses only free catalog items`() {
        val catalogFile = listOf(
            File("src/main/assets/shrine/catalog.json"),
            File("android/app/src/main/assets/shrine/catalog.json")
        ).first { it.isFile }
        @Suppress("UNCHECKED_CAST")
        val root = parseJson(catalogFile.readText()) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val categories = root["categories"] as List<Map<String, Any?>>
        val optionsByCategory = categories.associate { category ->
            @Suppress("UNCHECKED_CAST")
            val opts = category["options"] as List<Map<String, Any?>>
            (category["id"] as String) to opts.map { it["id"] as String }.toSet()
        }

        for (temple in DailyTemples.all) {
            assertTrue("${temple.id} must be owned without unlocking", ShrineSelectionSync.ownsSelection(temple.presetSelection))
            assertTrue(temple.matchesSelection(temple.presetSelection))
            for ((category, value) in temple.presetSelection.values) {
                val available = optionsByCategory[category] ?: error("Missing category $category in catalog")
                value.split(',').forEach { itemId ->
                    assertTrue("$category:$itemId missing from Android catalog", itemId in available)
                    assertEquals(
                        "Daily temple ${temple.id} item $category:$itemId must cost 0 coins",
                        0,
                        ShrineUnlocks.cost(category, itemId)
                    )
                }
            }
        }
    }

    private fun parseJson(text: String): Any? {
        var idx = 0
        fun skipWs() {
            while (idx < text.length && text[idx].isWhitespace()) idx++
        }
        fun parseString(): String {
            idx++ // '"'
            val sb = StringBuilder()
            while (idx < text.length) {
                val ch = text[idx++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> sb.append(text[idx++])
                    else -> sb.append(ch)
                }
            }
            error("Unterminated string")
        }
        fun parseValue(): Any? {
            skipWs()
            return when (val ch = text[idx]) {
                '{' -> {
                    idx++
                    val map = linkedMapOf<String, Any?>()
                    skipWs()
                    if (text[idx] == '}') { idx++; return map }
                    while (true) {
                        skipWs()
                        val key = parseString()
                        skipWs()
                        require(text[idx++] == ':')
                        map[key] = parseValue()
                        skipWs()
                        if (text[idx] == '}') { idx++; return map }
                        require(text[idx++] == ',')
                    }
                }
                '[' -> {
                    idx++
                    val list = mutableListOf<Any?>()
                    skipWs()
                    if (text[idx] == ']') { idx++; return list }
                    while (true) {
                        list.add(parseValue())
                        skipWs()
                        if (text[idx] == ']') { idx++; return list }
                        require(text[idx++] == ',')
                    }
                }
                '"' -> parseString()
                't' -> { idx += 4; true }
                'f' -> { idx += 5; false }
                'n' -> { idx += 4; null }
                else -> {
                    val start = idx
                    while (idx < text.length && (text[idx].isDigit() || text[idx] in "-+.eE")) idx++
                    text.substring(start, idx).toDouble()
                }
            }
        }
        return parseValue()
    }

    @Test
    fun `auto-switch applies once per day, honors same-day manual override, and resumes next day`() {
        val saturday = "2026-11-21"
        val sunday = "2026-11-22"

        // Fresh day with auto enabled -> applies
        assertTrue(
            DailyTempleSync.shouldAutoApply(
                autoEnabled = true,
                todayIsoDate = saturday,
                lastAppliedIsoDate = null,
                manualOverrideIsoDate = null
            )
        )

        // Already applied today -> does not overwrite active worship session
        assertFalse(
            DailyTempleSync.shouldAutoApply(
                autoEnabled = true,
                todayIsoDate = saturday,
                lastAppliedIsoDate = saturday,
                manualOverrideIsoDate = null
            )
        )

        // User manually customized today -> remains overridden for today
        assertFalse(
            DailyTempleSync.shouldAutoApply(
                autoEnabled = true,
                todayIsoDate = saturday,
                lastAppliedIsoDate = "2026-11-20",
                manualOverrideIsoDate = saturday
            )
        )

        // Next calendar day arrives -> automatically switches again
        assertTrue(
            DailyTempleSync.shouldAutoApply(
                autoEnabled = true,
                todayIsoDate = sunday,
                lastAppliedIsoDate = saturday,
                manualOverrideIsoDate = saturday
            )
        )

        // Auto disabled -> never auto-switches
        assertFalse(
            DailyTempleSync.shouldAutoApply(
                autoEnabled = false,
                todayIsoDate = sunday,
                lastAppliedIsoDate = null,
                manualOverrideIsoDate = null
            )
        )

        assertEquals("guest", DailyTempleSync.scopeKey(accountId = "acct-1", signedIn = false))
        assertEquals("acct-1", DailyTempleSync.scopeKey(accountId = "acct-1", signedIn = true))
        assertEquals("guest", DailyTempleSync.scopeKey(accountId = " ", signedIn = true))
    }
}

package com.worship.nityamandir
import com.worship.nityamandir.data.DevotionStreakCounter
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
class DevotionStreakCounterTest {
    private val today = LocalDate.parse("2026-09-29")
    @Test fun consecutiveAndDuplicateDays() {
        assertEquals(2, DevotionStreakCounter.count(setOf("2026-09-28", "2026-09-29", "2026-09-29"), today))
    }
    @Test fun yesterdayStaysActive() {
        assertEquals(2, DevotionStreakCounter.count(setOf("2026-09-27", "2026-09-28"), today))
    }
    @Test fun missedDayResets() {
        assertEquals(0, DevotionStreakCounter.count(setOf("2026-09-27"), today))
        assertEquals(1, DevotionStreakCounter.count(setOf("2026-09-27", "2026-09-29"), today))
    }
}

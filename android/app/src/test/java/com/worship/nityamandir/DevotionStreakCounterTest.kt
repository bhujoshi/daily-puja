package com.worship.nityamandir

import com.worship.nityamandir.data.DevotionStreakCounter
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
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

    @Test fun monthCellsAlignWeekRowsAndConnectConsecutiveStreakDays() {
        val month = YearMonth.of(2026, 9) // 2026-09-01 is a Tuesday (2 leading nulls on Sunday-first grid)
        val completed = setOf("2026-09-27", "2026-09-28", "2026-09-29", "2026-10-01")
        val cells = DevotionStreakCounter.monthCells(month, completed, today)

        assertEquals(0, cells.size % 7)
        assertNull(cells[0])
        assertNull(cells[1])
        assertEquals(LocalDate.of(2026, 9, 1), cells[2]?.date)
        assertEquals(3, DevotionStreakCounter.completedInMonth(month, completed))

        val sep27 = cells.filterNotNull().first { it.date == LocalDate.of(2026, 9, 27) } // Sunday
        val sep28 = cells.filterNotNull().first { it.date == LocalDate.of(2026, 9, 28) } // Monday
        val sep29 = cells.filterNotNull().first { it.date == LocalDate.of(2026, 9, 29) } // Tuesday (today)
        val sep30 = cells.filterNotNull().first { it.date == LocalDate.of(2026, 9, 30) } // Future

        assertTrue(sep27.completed)
        assertFalse(sep27.connectsPrevious)
        assertTrue(sep27.connectsNext)

        assertTrue(sep28.completed)
        assertTrue(sep28.connectsPrevious)
        assertTrue(sep28.connectsNext)

        assertTrue(sep29.completed)
        assertTrue(sep29.isToday)
        assertTrue(sep29.connectsPrevious)
        assertFalse(sep29.connectsNext)

        assertFalse(sep30.completed)
        assertTrue(sep30.isFuture)
    }
}

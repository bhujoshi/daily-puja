package com.worship.nityamandir.data

import java.time.LocalDate
import java.time.YearMonth

data class StreakCalendarDay(
    val date: LocalDate,
    val completed: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
    val connectsPrevious: Boolean,
    val connectsNext: Boolean
)

object DevotionStreakCounter {
    fun count(days: Set<String>, today: LocalDate): Int {
        var day = if (today.toString() in days) today else today.minusDays(1)
        var count = 0
        while (day.toString() in days) { count++; day = day.minusDays(1) }
        return count
    }

    fun completedInMonth(month: YearMonth, days: Set<String>): Int {
        val prefix = "%04d-%02d-".format(month.year, month.monthValue)
        return days.count { it.startsWith(prefix) }
    }

    /**
     * Builds a Sunday-first (columns 0..6) grid of cells for [month].
     * Leading and trailing cells outside the month are null so the list length is a multiple of 7.
     */
    fun monthCells(month: YearMonth, days: Set<String>, today: LocalDate): List<StreakCalendarDay?> {
        val firstOfMonth = month.atDay(1)
        val leadingEmpty = firstOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ..., Saturday = 6
        val totalDays = month.lengthOfMonth()
        val cells = ArrayList<StreakCalendarDay?>(leadingEmpty + totalDays + 7)
        repeat(leadingEmpty) { cells.add(null) }
        for (dayOfMonth in 1..totalDays) {
            val date = month.atDay(dayOfMonth)
            val completed = date.toString() in days
            val col = (leadingEmpty + dayOfMonth - 1) % 7
            val connectsPrevious = completed && col > 0 && dayOfMonth > 1 &&
                month.atDay(dayOfMonth - 1).toString() in days
            val connectsNext = completed && col < 6 && dayOfMonth < totalDays &&
                month.atDay(dayOfMonth + 1).toString() in days
            cells.add(
                StreakCalendarDay(
                    date = date,
                    completed = completed,
                    isToday = date == today,
                    isFuture = date.isAfter(today),
                    connectsPrevious = connectsPrevious,
                    connectsNext = connectsNext
                )
            )
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }
}

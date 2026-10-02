package com.worship.nityamandir.data
import java.time.LocalDate
object DevotionStreakCounter {
    fun count(days: Set<String>, today: LocalDate): Int {
        var day = if (today.toString() in days) today else today.minusDays(1)
        var count = 0
        while (day.toString() in days) { count++; day = day.minusDays(1) }
        return count
    }
}

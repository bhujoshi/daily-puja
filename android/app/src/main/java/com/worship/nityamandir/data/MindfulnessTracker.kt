package com.worship.nityamandir.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.worship.nityamandir.engine.MindfulnessPractice
import java.time.LocalDate
import java.time.ZoneId

data class MindfulnessStats(
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val streakDays: Int = 0,
    val completedDays: Set<String> = emptySet(),
    val lastPracticeId: String = MindfulnessPractice.PRANAYAMA.id
) {
    fun recordSession(
        durationMinutes: Int,
        practice: MindfulnessPractice,
        today: LocalDate
    ): MindfulnessStats {
        val safeMinutes = durationMinutes.coerceAtLeast(1)
        val updatedDays = completedDays + today.toString()
        val updatedStreak = DevotionStreakCounter.count(updatedDays, today)
        return copy(
            totalSessions = totalSessions + 1,
            totalMinutes = totalMinutes + safeMinutes,
            streakDays = updatedStreak,
            completedDays = updatedDays,
            lastPracticeId = practice.id
        )
    }
}

/**
 * Offline-first local mindfulness statistics store.
 * Persists total sessions, mindful minutes, and daily meditation streak in SharedPreferences.
 */
class MindfulnessTracker(context: Context) {
    private val prefs = context.getSharedPreferences("mindfulness", Context.MODE_PRIVATE)
    private var revision by mutableIntStateOf(0)

    private fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata"))

    val stats: MindfulnessStats
        get() {
            revision // Observe state mutations in Compose
            val currentToday = today()
            val days = prefs.getStringSet(KEY_DAYS, emptySet()).orEmpty()
            val totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0).coerceAtLeast(0)
            val totalMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0).coerceAtLeast(0)
            val lastPractice = prefs.getString(KEY_LAST_PRACTICE, MindfulnessPractice.PRANAYAMA.id)
                ?: MindfulnessPractice.PRANAYAMA.id
            return MindfulnessStats(
                totalSessions = totalSessions,
                totalMinutes = totalMinutes,
                streakDays = DevotionStreakCounter.count(days, currentToday),
                completedDays = days,
                lastPracticeId = lastPractice
            )
        }

    fun recordSession(
        durationMinutes: Int,
        practice: MindfulnessPractice,
        today: LocalDate = today()
    ): MindfulnessStats {
        val updated = stats.recordSession(durationMinutes, practice, today)
        prefs.edit()
            .putInt(KEY_TOTAL_SESSIONS, updated.totalSessions)
            .putInt(KEY_TOTAL_MINUTES, updated.totalMinutes)
            .putStringSet(KEY_DAYS, updated.completedDays)
            .putString(KEY_LAST_PRACTICE, updated.lastPracticeId)
            .apply()
        revision++
        return updated
    }

    companion object {
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_TOTAL_MINUTES = "total_minutes"
        private const val KEY_DAYS = "completed_days"
        private const val KEY_LAST_PRACTICE = "last_practice"
    }
}

package com.worship.nityamandir

import com.worship.nityamandir.data.MindfulnessStats
import com.worship.nityamandir.engine.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MindfulnessEngineTest {

    @Test
    fun samaVrittiBoxBreathingPhasesProgressCorrectly() {
        val totalSec = 300 // 5 minutes
        // 0s -> start of INHALE (4s)
        val start = MindfulnessEngine.evaluate(
            elapsedMs = 0L,
            totalDurationSeconds = totalSec,
            practice = MindfulnessPractice.PRANAYAMA,
            breathingPattern = BreathingPattern.SAMA_VRITTI
        )
        assertEquals(BreathPhase.INHALE, start.breathPhase)
        assertEquals(4, start.breathPhaseRemainingSeconds)
        assertEquals(0.56f, start.lotusScale, 0.01f)
        assertEquals(0, start.completedBreathCycles)

        // 5s -> into HOLD_IN (4s..8s)
        val holdIn = MindfulnessEngine.evaluate(
            elapsedMs = 5_000L,
            totalDurationSeconds = totalSec,
            practice = MindfulnessPractice.PRANAYAMA,
            breathingPattern = BreathingPattern.SAMA_VRITTI
        )
        assertEquals(BreathPhase.HOLD_IN, holdIn.breathPhase)
        assertEquals(3, holdIn.breathPhaseRemainingSeconds)
        assertEquals(1.0f, holdIn.lotusScale, 0.01f)

        // 10s -> halfway into EXHALE (8s..12s)
        val exhale = MindfulnessEngine.evaluate(
            elapsedMs = 10_000L,
            totalDurationSeconds = totalSec,
            practice = MindfulnessPractice.PRANAYAMA,
            breathingPattern = BreathingPattern.SAMA_VRITTI
        )
        assertEquals(BreathPhase.EXHALE, exhale.breathPhase)
        assertEquals(2, exhale.breathPhaseRemainingSeconds)
        assertEquals(0.78f, exhale.lotusScale, 0.02f)

        // 14s -> halfway into HOLD_OUT (12s..16s)
        val holdOut = MindfulnessEngine.evaluate(
            elapsedMs = 14_000L,
            totalDurationSeconds = totalSec,
            practice = MindfulnessPractice.PRANAYAMA,
            breathingPattern = BreathingPattern.SAMA_VRITTI
        )
        assertEquals(BreathPhase.HOLD_OUT, holdOut.breathPhase)
        assertEquals(2, holdOut.breathPhaseRemainingSeconds)
        assertEquals(0.56f, holdOut.lotusScale, 0.01f)

        // 16s -> completed 1 full cycle, back to INHALE
        val nextCycle = MindfulnessEngine.evaluate(
            elapsedMs = 16_000L,
            totalDurationSeconds = totalSec,
            practice = MindfulnessPractice.PRANAYAMA,
            breathingPattern = BreathingPattern.SAMA_VRITTI
        )
        assertEquals(1, nextCycle.completedBreathCycles)
        assertEquals(BreathPhase.INHALE, nextCycle.breathPhase)
    }

    @Test
    fun shantiPranaSkipsRetentionPhases() {
        // SHANTI_PRANA is 5s inhale, 0s holdIn, 5s exhale, 0s holdOut (10s cycle)
        val inhale = MindfulnessEngine.evaluate(
            elapsedMs = 2_500L,
            totalDurationSeconds = 60,
            practice = MindfulnessPractice.PRANAYAMA,
            breathingPattern = BreathingPattern.SHANTI_PRANA
        )
        assertEquals(BreathPhase.INHALE, inhale.breathPhase)

        val exhale = MindfulnessEngine.evaluate(
            elapsedMs = 5_100L,
            totalDurationSeconds = 60,
            practice = MindfulnessPractice.PRANAYAMA,
            breathingPattern = BreathingPattern.SHANTI_PRANA
        )
        assertEquals(BreathPhase.EXHALE, exhale.breathPhase)
    }

    @Test
    fun tratakaAlternatesBetweenOpenGazeAndInnerVisionWithBellCues() {
        // First 40s is OPEN_GAZE
        val gaze = MindfulnessEngine.evaluate(
            elapsedMs = 15_000L,
            totalDurationSeconds = 180,
            practice = MindfulnessPractice.TRATAKA
        )
        assertEquals(TratakaPhase.OPEN_GAZE, gaze.tratakaPhase)
        assertEquals(25, gaze.tratakaPhaseRemainingSeconds)
        assertEquals(0, gaze.bellCueIndex)

        // At 42s -> INNER_VISION (40s..60s), bellCueIndex advances to 1
        val inner = MindfulnessEngine.evaluate(
            elapsedMs = 42_000L,
            totalDurationSeconds = 180,
            practice = MindfulnessPractice.TRATAKA
        )
        assertEquals(TratakaPhase.INNER_VISION, inner.tratakaPhase)
        assertEquals(18, inner.tratakaPhaseRemainingSeconds)
        assertEquals(1, inner.bellCueIndex)

        // At 61s -> second cycle OPEN_GAZE, bellCueIndex advances to 2
        val secondCycle = MindfulnessEngine.evaluate(
            elapsedMs = 61_000L,
            totalDurationSeconds = 180,
            practice = MindfulnessPractice.TRATAKA
        )
        assertEquals(TratakaPhase.OPEN_GAZE, secondCycle.tratakaPhase)
        assertEquals(2, secondCycle.bellCueIndex)
    }

    @Test
    fun mantraDhyanTriggersPeriodicBellAndMarksCompletion() {
        val beforeMinute = MindfulnessEngine.evaluate(
            elapsedMs = 59_000L,
            totalDurationSeconds = 180,
            practice = MindfulnessPractice.MANTRA_DHYAN
        )
        assertEquals(0, beforeMinute.bellCueIndex)
        assertFalse(beforeMinute.completed)

        val afterMinute = MindfulnessEngine.evaluate(
            elapsedMs = 61_000L,
            totalDurationSeconds = 180,
            practice = MindfulnessPractice.MANTRA_DHYAN
        )
        assertEquals(1, afterMinute.bellCueIndex)
        assertFalse(afterMinute.completed)

        val done = MindfulnessEngine.evaluate(
            elapsedMs = 180_000L,
            totalDurationSeconds = 180,
            practice = MindfulnessPractice.MANTRA_DHYAN
        )
        assertTrue(done.completed)
        assertEquals(0, done.remainingSeconds)
        assertEquals(1.0f, done.sessionProgress, 0.001f)
    }

    @Test
    fun formatTimerFormatsMinutesAndSecondsWithLeadingZeros() {
        assertEquals("05:00", MindfulnessEngine.formatTimer(300))
        assertEquals("01:09", MindfulnessEngine.formatTimer(69))
        assertEquals("00:00", MindfulnessEngine.formatTimer(0))
        assertEquals("00:00", MindfulnessEngine.formatTimer(-5))
    }

    @Test
    fun mindfulnessStatsAccumulatesSessionsMinutesAndConsecutiveDayStreak() {
        val day1 = LocalDate.parse("2026-10-28")
        val day2 = LocalDate.parse("2026-10-29")
        val day3 = LocalDate.parse("2026-10-30")

        var stats = MindfulnessStats()
        stats = stats.recordSession(5, MindfulnessPractice.PRANAYAMA, day1)
        assertEquals(1, stats.totalSessions)
        assertEquals(5, stats.totalMinutes)
        assertEquals(1, stats.streakDays)
        assertEquals("pranayama", stats.lastPracticeId)

        // Second session on same day increments sessions & minutes, streak remains 1
        stats = stats.recordSession(10, MindfulnessPractice.TRATAKA, day1)
        assertEquals(2, stats.totalSessions)
        assertEquals(15, stats.totalMinutes)
        assertEquals(1, stats.streakDays)
        assertEquals("trataka", stats.lastPracticeId)

        // Consecutive day 2 & day 3 increment streak to 2 and 3
        stats = stats.recordSession(5, MindfulnessPractice.MANTRA_DHYAN, day2)
        assertEquals(2, stats.streakDays)

        stats = stats.recordSession(15, MindfulnessPractice.MANTRA_DHYAN, day3)
        assertEquals(4, stats.totalSessions)
        assertEquals(35, stats.totalMinutes)
        assertEquals(3, stats.streakDays)
        assertEquals("mantra_dhyan", stats.lastPracticeId)
    }
}

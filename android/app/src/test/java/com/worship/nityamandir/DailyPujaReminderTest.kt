package com.worship.nityamandir

import com.worship.nityamandir.data.DailyPujaReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyPujaReminderTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `schedules for 8am same day when current time is before 8am`() {
        val now = ZonedDateTime.of(2026, 11, 14, 6, 45, 12, 0, kolkata)
        val trigger = DailyPujaReminder.nextTriggerTime(now)

        assertEquals(ZonedDateTime.of(2026, 11, 14, 8, 0, 0, 0, kolkata), trigger)
    }

    @Test
    fun `schedules for 8am tomorrow when current time is exactly 8am or later`() {
        val exactEight = ZonedDateTime.of(2026, 11, 14, 8, 0, 0, 0, kolkata)
        val midMorning = ZonedDateTime.of(2026, 11, 14, 10, 38, 0, 0, kolkata)

        assertEquals(
            ZonedDateTime.of(2026, 11, 15, 8, 0, 0, 0, kolkata),
            DailyPujaReminder.nextTriggerTime(exactEight)
        )
        assertEquals(
            ZonedDateTime.of(2026, 11, 15, 8, 0, 0, 0, kolkata),
            DailyPujaReminder.nextTriggerTime(midMorning)
        )
    }

    @Test
    fun `skipToday forces tomorrow 8am even if alarm fires slightly before 8am`() {
        val earlyBatchWake = ZonedDateTime.of(2026, 11, 14, 7, 59, 58, 0, kolkata)
        val trigger = DailyPujaReminder.nextTriggerTime(earlyBatchWake, skipToday = true)

        assertEquals(ZonedDateTime.of(2026, 11, 15, 8, 0, 0, 0, kolkata), trigger)
    }

    @Test
    fun `handles daylight saving spring-forward and fall-back at 8am wall clock`() {
        // US DST spring-forward on 2026-03-08
        val eveBeforeSpringForward = ZonedDateTime.of(2026, 3, 7, 21, 30, 0, 0, newYork)
        val springTrigger = DailyPujaReminder.nextTriggerTime(eveBeforeSpringForward)
        assertEquals(LocalDate.of(2026, 3, 8), springTrigger.toLocalDate())
        assertEquals(8, springTrigger.hour)
        assertEquals(0, springTrigger.minute)

        // US DST fall-back on 2026-11-01
        val eveBeforeFallBack = ZonedDateTime.of(2026, 10, 31, 22, 0, 0, 0, newYork)
        val fallTrigger = DailyPujaReminder.nextTriggerTime(eveBeforeFallBack)
        assertEquals(LocalDate.of(2026, 11, 1), fallTrigger.toLocalDate())
        assertEquals(8, fallTrigger.hour)
        assertEquals(0, fallTrigger.minute)
    }

    @Test
    fun `shouldShowNotification suppresses duplicates, disabled state, and already-completed puja`() {
        val today = "2026-11-14"

        assertTrue(
            DailyPujaReminder.shouldShowNotification(
                reminderEnabled = true,
                notificationsPermitted = true,
                todayIsoDate = today,
                lastNotifiedIsoDate = "2026-11-13",
                completedDates = setOf("2026-11-13")
            )
        )

        // Disabled by user
        assertFalse(
            DailyPujaReminder.shouldShowNotification(
                reminderEnabled = false,
                notificationsPermitted = true,
                todayIsoDate = today,
                lastNotifiedIsoDate = null,
                completedDates = emptySet()
            )
        )

        // OS permission missing
        assertFalse(
            DailyPujaReminder.shouldShowNotification(
                reminderEnabled = true,
                notificationsPermitted = false,
                todayIsoDate = today,
                lastNotifiedIsoDate = null,
                completedDates = emptySet()
            )
        )

        // Already notified today
        assertFalse(
            DailyPujaReminder.shouldShowNotification(
                reminderEnabled = true,
                notificationsPermitted = true,
                todayIsoDate = today,
                lastNotifiedIsoDate = today,
                completedDates = emptySet()
            )
        )

        // Devotee already completed puja earlier this morning
        assertFalse(
            DailyPujaReminder.shouldShowNotification(
                reminderEnabled = true,
                notificationsPermitted = true,
                todayIsoDate = today,
                lastNotifiedIsoDate = null,
                completedDates = setOf("2026-11-13", today)
            )
        )
    }

    @Test
    fun `builds reverent Hindi and English copy for ordinary and festival mornings`() {
        val ordinaryDay = LocalDate.of(2026, 11, 14)
        val hindiOrdinary = DailyPujaReminder.buildCopy(hindi = true, today = ordinaryDay)
        val englishOrdinary = DailyPujaReminder.buildCopy(hindi = false, today = ordinaryDay)

        assertNull(hindiOrdinary.occasionId)
        assertTrue(hindiOrdinary.title.contains("प्रातः पूजा"))
        assertTrue(hindiOrdinary.body.contains("पवित्र मंदिर"))
        assertEquals("Time for Morning Puja", englishOrdinary.title.removePrefix("🪔 "))

        val diwaliDay = LocalDate.of(2026, 11, 5)
        val hindiDiwali = DailyPujaReminder.buildCopy(hindi = true, today = diwaliDay)
        val englishDiwali = DailyPujaReminder.buildCopy(hindi = false, today = diwaliDay)

        assertEquals("diwali", hindiDiwali.occasionId)
        assertTrue(hindiDiwali.title.contains("दीपावली"))
        assertEquals("diwali", englishDiwali.occasionId)
        assertTrue(englishDiwali.title.contains("Diwali"))
    }
}

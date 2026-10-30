package com.worship.nityamandir

import com.worship.nityamandir.data.MandirAnalytics
import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.WorshipStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MandirAnalyticsTest {

    private class FakeSink : MandirAnalytics.Sink {
        val events = mutableListOf<Pair<String, Map<String, Any>>>()
        val properties = linkedMapOf<String, String?>()

        override fun logEvent(name: String, params: Map<String, Any>) {
            events.add(name to params)
        }

        override fun setUserProperty(name: String, value: String?) {
            properties[name] = value
        }
    }

    @Test
    fun `records ritual steps and puja completion with expected parameters`() {
        val sink = FakeSink()
        val analytics = MandirAnalytics(sink)

        analytics.logTempleEntered(
            needsCleaning = true,
            deityCount = 2,
            shrineId = "marble_arch",
            idolsId = "original"
        )
        analytics.logTempleCleaned(method = "swipe")
        analytics.logRitualStepCompleted(WorshipStep.LIGHT, deityCount = 2)
        analytics.logPujaCompleted(
            deityCount = 2,
            shrineId = "marble_arch",
            idolsId = "original",
            streakDays = 7,
            signedIn = true
        )

        assertEquals(4, sink.events.size)

        val (enteredName, enteredParams) = sink.events[0]
        assertEquals("temple_entered", enteredName)
        assertEquals("true", enteredParams["needs_cleaning"])
        assertEquals(2L, enteredParams["deity_count"])
        assertEquals("marble_arch", enteredParams["shrine_id"])

        val (cleanedName, cleanedParams) = sink.events[1]
        assertEquals("temple_cleaned", cleanedName)
        assertEquals("swipe", cleanedParams["method"])

        val (stepName, stepParams) = sink.events[2]
        assertEquals("ritual_step_completed", stepName)
        assertEquals("light", stepParams["step"])
        assertEquals(1L, stepParams["step_index"])
        assertEquals(2L, stepParams["deity_count"])

        val (pujaName, pujaParams) = sink.events[3]
        assertEquals("puja_completed", pujaName)
        assertEquals(7L, pujaParams["streak_days"])
        assertEquals("true", pujaParams["signed_in"])
    }

    @Test
    fun `syncs user context and logs shrine customization and bhajan playback`() {
        val sink = FakeSink()
        val analytics = MandirAnalytics(sink)

        analytics.syncUserContext(hindi = true, signedIn = false, idolsId = "ram_darbar")
        assertEquals("hi", sink.properties["app_language"])
        assertEquals("guest", sink.properties["account_type"])
        assertEquals("ram_darbar", sink.properties["active_idols"])

        analytics.logShrineSelectionApplied(
            ShrineSelection(
                mapOf(
                    "shrine" to "sandstone",
                    "idols" to "shiva",
                    "lamps" to "brass_deepam",
                    "flowers" to "marigold,lotus"
                )
            )
        )
        val (appliedName, appliedParams) = sink.events.single()
        assertEquals("shrine_selection_applied", appliedName)
        assertEquals("sandstone", appliedParams["shrine_id"])
        assertEquals("shiva", appliedParams["idols_id"])
        assertEquals(2L, appliedParams["flower_count"])

        analytics.logBhajanPlayed(
            trackId = "jai-ganesh",
            title = "Jai Ganesh Deva",
            deity = "ganesh",
            isYoutube = false,
            autoplay = true
        )
        val (bhajanName, bhajanParams) = sink.events.last()
        assertEquals("bhajan_played", bhajanName)
        assertEquals("archive", bhajanParams["source"])
        assertEquals("true", bhajanParams["autoplay"])
    }

    @Test
    fun `sanitizes keys and truncates values to Firebase limits`() {
        val sink = FakeSink()
        val analytics = MandirAnalytics(sink)

        val longValue = "x".repeat(150)
        analytics.logEvent(
            name = "123 Invalid-Event.Name With Spaces_And_Very_Long_Suffix_Exceeding_Forty_Chars",
            params = mapOf(
                "Bad-Key.Name" to longValue,
                "nan_value" to Double.NaN
            )
        )

        val (eventName, params) = sink.events.single()
        assertTrue(eventName.startsWith("e_123_invalid_event_name"))
        assertTrue(eventName.length <= 40)
        assertEquals(100, (params["bad_key_name"] as String).length)
        assertTrue("nan_value" !in params)
    }

    @Test
    fun `logs mindfulness started and completed events`() {
        val sink = FakeSink()
        val analytics = MandirAnalytics(sink)

        analytics.logMindfulnessStarted(
            practice = "pranayama",
            durationMinutes = 5,
            subOption = "sama_vritti"
        )
        analytics.logMindfulnessCompleted(
            practice = "pranayama",
            durationMinutes = 5,
            streakDays = 3,
            totalSessions = 4
        )

        assertEquals(2, sink.events.size)
        val (startedName, startedParams) = sink.events[0]
        assertEquals("mindfulness_started", startedName)
        assertEquals("pranayama", startedParams["practice"])
        assertEquals(5L, startedParams["duration_minutes"])
        assertEquals("sama_vritti", startedParams["sub_option"])

        val (completedName, completedParams) = sink.events[1]
        assertEquals("mindfulness_completed", completedName)
        assertEquals("pranayama", completedParams["practice"])
        assertEquals(5L, completedParams["duration_minutes"])
        assertEquals(3L, completedParams["streak_days"])
        assertEquals(4L, completedParams["total_sessions"])
    }
}

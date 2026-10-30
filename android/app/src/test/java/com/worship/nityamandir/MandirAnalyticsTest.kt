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
        assertEquals("false", completedParams["completed_early"])
        assertEquals(0L, completedParams["japa_count"])
    }

    @Test
    fun `logs all page screen views and granular activity events across the app`() {
        val sink = FakeSink()
        val analytics = MandirAnalytics(sink)

        // 1. Screen views and lifecycle
        analytics.logAppLifecycle("foreground")
        analytics.logScreenView("mandir_sanctum", "MandirHomeScreen")
        analytics.logScreenView("shrine_customizer", "ShrineCustomizer")
        analytics.logScreenView("mindfulness_active", "MindfulnessMeditationScreen")
        analytics.logScreenView("music_player", "BhajanPlayerSheet")
        analytics.logScreenView("devotion_profile", "DevotionSheet")
        analytics.logScreenView("aarti_player", "AartiPlayerSheet")

        // 2. Ritual and cleaning actions
        analytics.logTempleCleaningStarted("swipe")
        analytics.logRitualDiyaLit(source = "altar", step = WorshipStep.LIGHT)
        analytics.logRitualDeityBathed(deityIndex = 0, source = "dock", step = WorshipStep.BATH)
        analytics.logRitualTilakApplied(deityIndex = 1, source = "altar", step = WorshipStep.TILAK)
        analytics.logRitualFlowerOffered(deityIndex = 0, flowerIndex = 1, source = "dock", step = WorshipStep.FLOWERS)
        analytics.logRitualConchBlown(source = "altar", step = WorshipStep.CONCH)
        analytics.logRitualPrasadOffered(source = "dock", step = WorshipStep.PRASAD)
        analytics.logRitualAartiStarted(source = "altar", step = WorshipStep.AARTI)
        analytics.logBellRung(source = "dock")

        // 3. Drawer & Shrine Customizer interactions
        analytics.logDrawerItemSelected("mindfulness")
        analytics.logShrineOptionSelected(
            categoryId = "idols",
            itemId = "ram_darbar",
            owned = false,
            costCoins = 108,
            selected = true
        )
        analytics.logShrineUnlockPrompted(
            itemCount = 2,
            totalCostCoins = 216,
            sufficientCoins = true,
            signedIn = true
        )
        analytics.logShrineUnlockCancelled(itemCount = 2, totalCostCoins = 216)
        analytics.logShrineAccountClicked(signedIn = false, coins = 50)

        // 4. Bhajan & Aarti player controls
        analytics.logBhajanControlAction(action = "play", trackId = "hanuman-chalisa", source = "archive")
        analytics.logAartiLyricsAction(action = "pause", title = "Om Jai Jagdish Hare")

        // 5. Devotion sheet interactions
        analytics.logStreakCalendarMonthChanged(yearMonth = "2026-10", direction = "previous")
        analytics.logPassbookFilterChanged(filter = "earned")
        analytics.logPassbookExpandedToggled(expanded = true, entryCount = 12)
        analytics.logAccountAction(action = "refresh_sync")

        // 6. Mindfulness setup & session interactions
        analytics.logMindfulnessOptionSelected(optionType = "practice", value = "mantra_japa")
        analytics.logMindfulnessSoundToggled(enabled = false)
        analytics.logMindfulnessPauseToggled(practice = "mantra_japa", paused = true, elapsedSeconds = 45)
        analytics.logMindfulnessJapaTap(mantra = "om_namah_shivaya", japaCount = 108, malaCompleted = true)
        analytics.logMindfulnessCancelled(practice = "mantra_japa", elapsedSeconds = 90, japaCount = 54)
        analytics.logMindfulnessRestarted(practice = "mantra_japa")

        val byName = sink.events.associate { it.first to it.second }

        assertEquals("foreground", byName["app_lifecycle"]?.get("state"))
        assertEquals("AartiPlayerSheet", byName["screen_view"]?.get("screen_class"))
        assertEquals("swipe", byName["temple_cleaning_started"]?.get("method"))
        assertEquals("altar", byName["ritual_diya_lit"]?.get("source"))
        assertEquals(0L, byName["ritual_deity_bathed"]?.get("deity_index"))
        assertEquals(1L, byName["ritual_tilak_applied"]?.get("deity_index"))
        assertEquals(1L, byName["ritual_flower_offered"]?.get("flower_index"))
        assertEquals("altar", byName["ritual_conch_blown"]?.get("source"))
        assertEquals("dock", byName["ritual_prasad_offered"]?.get("source"))
        assertEquals("altar", byName["ritual_aarti_started"]?.get("source"))
        assertEquals("dock", byName["ritual_bell_rung"]?.get("source"))
        assertEquals("mindfulness", byName["drawer_item_selected"]?.get("item"))
        assertEquals("ram_darbar", byName["shrine_option_selected"]?.get("item_id"))
        assertEquals(216L, byName["shrine_unlock_prompted"]?.get("total_cost_coins"))
        assertEquals(2L, byName["shrine_unlock_cancelled"]?.get("item_count"))
        assertEquals(50L, byName["shrine_account_clicked"]?.get("coins"))
        assertEquals("play", byName["bhajan_control_action"]?.get("action"))
        assertEquals("pause", byName["aarti_lyrics_action"]?.get("action"))
        assertEquals("2026-10", byName["streak_calendar_month_changed"]?.get("year_month"))
        assertEquals("earned", byName["passbook_filter_changed"]?.get("filter"))
        assertEquals("true", byName["passbook_expanded_toggled"]?.get("expanded"))
        assertEquals("refresh_sync", byName["account_action"]?.get("action"))
        assertEquals("mantra_japa", byName["mindfulness_option_selected"]?.get("value"))
        assertEquals("false", byName["mindfulness_sound_toggled"]?.get("enabled"))
        assertEquals("true", byName["mindfulness_pause_toggled"]?.get("paused"))
        assertEquals("true", byName["mindfulness_japa_tap"]?.get("mala_completed"))
        assertEquals(90L, byName["mindfulness_cancelled"]?.get("elapsed_seconds"))
        assertEquals("mantra_japa", byName["mindfulness_restarted"]?.get("practice"))
    }
}

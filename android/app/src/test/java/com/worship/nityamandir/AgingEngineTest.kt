package com.worship.nityamandir

import com.worship.nityamandir.engine.AgingEngine
import org.junit.Assert.*
import org.junit.Test

class AgingEngineTest {

    @Test
    fun cleaningRemovesWitheredFlowersAndClearsGate() {
        val now = 200_000_000L
        val state = AgingEngine.calculateAging(now - 48L * 3600000, now, now)
        assertFalse(state.needsCleaning)
        assertEquals(0f, state.dustLevel, .001f)
        assertEquals(0f, state.flowerWitherFactor, .001f)
    }

    @Test
    fun testPristineTemple_whenNoWorshipRecorded() {
        val state = AgingEngine.calculateAging(null, null, System.currentTimeMillis())
        assertEquals(0.0f, state.dustLevel, 0.001f)
        assertEquals(0.0f, state.flowerWitherFactor, 0.001f)
        assertFalse(state.needsCleaning)
    }

    @Test
    fun testNextMorning_18HoursElapsed() {
        val now = System.currentTimeMillis()
        val eighteenHoursAgo = now - (18L * 60L * 60L * 1000L)

        val state = AgingEngine.calculateAging(eighteenHoursAgo, eighteenHoursAgo, now)
        assertTrue(state.needsCleaning)
        assertTrue(state.dustLevel > 0.03f)
        assertTrue(state.flowerWitherFactor > 0.20f)
    }

    @Test
    fun testNeglectedTemple_48HoursElapsed() {
        val now = System.currentTimeMillis()
        val fortyEightHoursAgo = now - (48L * 60L * 60L * 1000L)

        val state = AgingEngine.calculateAging(fortyEightHoursAgo, fortyEightHoursAgo, now)
        assertTrue(state.needsCleaning)
        assertTrue(state.dustLevel > 0.40f)
        assertEquals(1.0f, state.flowerWitherFactor, 0.01f)
    }
}

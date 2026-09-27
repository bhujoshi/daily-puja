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
    fun testFirstDay() {
        val now = System.currentTimeMillis()
        val eighteenHoursAgo = now - (24L * 60L * 60L * 1000L)

        val state = AgingEngine.calculateAging(eighteenHoursAgo, eighteenHoursAgo, now)
        assertTrue(state.needsCleaning)
        assertTrue(state.dustLevel > 0.03f)
        assertTrue(state.flowerWitherFactor > 0.15f)
    }

    @Test
    fun testNeglectedTemple_48HoursElapsed() {
        val now = System.currentTimeMillis()
        val fortyEightHoursAgo = now - (48L * 60L * 60L * 1000L)

        val state = AgingEngine.calculateAging(fortyEightHoursAgo, fortyEightHoursAgo, now)
        assertTrue(state.needsCleaning)
        assertTrue(state.dustLevel > 0.35f)
        assertTrue(state.flowerWitherFactor > 0.5f)
    }

    @Test fun dailyProgressionAndWebThreshold() {
        val start=1_000_000L
        val days=(0..30).map { AgingEngine.calculateAging(start,start,start+it*AgingEngine.DAY_MS) }
        assertEquals(0f,days[2].cobwebLevel,0f)
        assertTrue(days[3].cobwebLevel>0f)
        for(i in 1..14) {
            assertTrue(days[i].dustLevel>days[i-1].dustLevel)
            if(i>=3) assertTrue(days[i].cobwebLevel>days[i-1].cobwebLevel)
        }
        assertTrue(days[2].flowerWitherFactor>days[1].flowerWitherFactor)
        assertTrue(days.all {it.dustLevel in 0f..1f && it.flowerWitherFactor in 0f..1f && it.cobwebLevel in 0f..1f})
    }

    @Test fun cleaningRestartsEntireCycleEvenWithoutWorship() {
        val cleaned=10*AgingEngine.DAY_MS
        val fresh=AgingEngine.calculateAging(1L,cleaned,cleaned)
        assertEquals(0f,fresh.cobwebLevel,0f)
        assertEquals(0f,fresh.elapsedHours,0f)
        assertFalse(fresh.needsCleaning)
        val next=AgingEngine.calculateAging(null,cleaned,cleaned+AgingEngine.DAY_MS)
        assertTrue(next.needsCleaning)
        assertTrue(next.flowerWitherFactor>0f)
        assertEquals(0f,AgingEngine.calculateAging(cleaned,cleaned,0L).dustLevel,0f)
    }
}

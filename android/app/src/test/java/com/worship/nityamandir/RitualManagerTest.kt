package com.worship.nityamandir

import com.worship.nityamandir.engine.RitualManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RitualManagerTest {

    private lateinit var ritualManager: RitualManager

    @Before
    fun setUp() {
        ritualManager = RitualManager()
    }

    @Test
    fun testPurityGate_curtainsClosedInitially() {
        assertTrue(ritualManager.curtainsClosed.value)
        assertFalse(ritualManager.cleanlinessConfirmed.value)

        ritualManager.confirmCleanliness()

        assertTrue(ritualManager.cleanlinessConfirmed.value)
        assertFalse(ritualManager.curtainsClosed.value)
    }

    @Test
    fun testRitualActions() {
        ritualManager.lightDiya()
        assertTrue(ritualManager.isDiyaLit.value)

        ritualManager.completeAbhishek()
        assertTrue(ritualManager.isAbhishekCompleted.value)
    }

    @Test
    fun testCleaningResetsRitualState() {
        ritualManager.lightDiya()
        ritualManager.completeAbhishek()

        ritualManager.cleanMandir()

        assertFalse(ritualManager.isDiyaLit.value)
        assertFalse(ritualManager.isAbhishekCompleted.value)
    }
}

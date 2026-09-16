package com.worship.nityamandir

import com.worship.nityamandir.engine.*
import org.junit.Assert.*
import org.junit.Test

class WorshipSessionTest {
    @Test fun recitationWaitsForAudioCompletion() {
        val pending=WorshipSession(step=WorshipStep.RECITATION)
        assertEquals(pending,pending.next())
        assertEquals(WorshipStep.AARTI,pending.copy(recitationComplete=true).next().step)
    }
    @Test fun bellAndConchWaitForTheirAnimations() {
        val bell=WorshipSession(step=WorshipStep.BELL)
        assertEquals(bell,bell.next())
        assertEquals(WorshipStep.CONCH,bell.copy(bellRung=true).next().step)
        val conch=WorshipSession(step=WorshipStep.CONCH)
        assertEquals(conch,conch.next())
        assertEquals(WorshipStep.PRASAD,conch.copy(conchBlown=true).next().step)
    }
    @Test fun aartiReturnsToRestWithoutJumping() {
        assertEquals(TempleSceneLayout.aartiRest,TempleSceneLayout.aartiPosition(0f))
        assertEquals(TempleSceneLayout.aartiRest,TempleSceneLayout.aartiPosition(1f))
        for(i in 1..1000) {
            val a=TempleSceneLayout.aartiPosition((i-1)/1000f)
            val b=TempleSceneLayout.aartiPosition(i/1000f)
            assertTrue(kotlin.math.abs(a.x-b.x)<.02f)
            assertTrue(kotlin.math.abs(a.y-b.y)<.02f)
        }
    }
    @Test fun aartiStaysLitOnCompletionAndResetsForNewWorship() {
        val started=WorshipSession(step=WorshipStep.AARTI,aartiLit=true)
        assertFalse(started.canContinue)
        val completed=started.copy(aartiComplete=true).next()
        assertTrue(completed.complete)
        assertTrue(completed.aartiLit)
        assertFalse(WorshipSession().aartiLit)
    }
    @Test fun offeringsNeedBothDeitiesBeforeContinuing() {
        val one=WorshipSession(step=WorshipStep.FLOWERS,flowers=setOf(0))
        assertEquals(one,one.next())
        assertEquals(WorshipStep.BELL,one.copy(flowers=setOf(0,1)).next().step)
    }
}

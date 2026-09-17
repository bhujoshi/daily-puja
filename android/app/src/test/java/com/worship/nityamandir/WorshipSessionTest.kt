package com.worship.nityamandir

import com.worship.nityamandir.engine.*
import org.junit.Assert.*
import org.junit.Test

class WorshipSessionTest {
    @Test fun repeatedOfferingsRetainEveryFlowerAndItsPosition() {
        var session=WorshipSession()
        val first=TemplePoint(.40f,.62f)
        session=session.offerFlower(0,0,first)
        repeat(100) { session=session.offerFlower(0,1,TempleSceneLayout.randomFlowerPosition(1)) }
        assertEquals(101,session.offeredFlowers.size)
        assertEquals(first,session.offeredFlowers.first().position)
        assertEquals(setOf(0,1),session.flowers)
    }
    @Test fun flowerDestinationsStayBelowFacesAndWithinIdols() {
        repeat(1000) {
            for(deity in 0..1) {
                val point=TempleSceneLayout.randomFlowerPosition(deity)
                val center=if(deity==0) .405f else .604f
                assertTrue(point.x in (center-.045f)..(center+.045f))
                assertTrue(point.y in .585f.. .690f)
            }
        }
    }
    @Test fun allFourLaddusTravelFromPlateToRightOfLamp() {
        val destinations=(0 until TempleSceneLayout.LADDU_COUNT).map { i ->
            val start=TempleSceneLayout.plateLaddu(i)
            val end=TempleSceneLayout.offeredLaddu(i)
            assertEquals(start,TempleSceneLayout.flowerFlight(start,end,0f))
            val landed=TempleSceneLayout.flowerFlight(start,end,1f)
            assertEquals(end.x,landed.x,.00001f)
            assertEquals(end.y,landed.y,.00001f)
            assertTrue(end.x>TempleSceneLayout.oil.x)
            end
        }
        assertEquals(4,destinations.toSet().size)
        assertFalse(WorshipSession(step=WorshipStep.PRASAD).canContinue)
    }

    @Test fun prasadAdvancesDirectlyToAarti() {
        assertEquals(8,WorshipStep.values().size)
        assertEquals(WorshipStep.AARTI,WorshipSession(step=WorshipStep.PRASAD,prasadOffered=true).next().step)
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

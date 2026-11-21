package com.worship.nityamandir

import com.worship.nityamandir.engine.*
import org.junit.Assert.*
import org.junit.Test

class WorshipSessionTest {
    @Test fun repeatedOfferingsRetainEveryFlowerAndItsPosition() {
        var session=WorshipSession()
        val first=TemplePoint(.40f,.62f)
        session=session.offerFlower(0,0,first)
        repeat(100) { session=session.offerFlower(0,1,TempleSceneLayout.offeredFlower(1,it)) }
        assertEquals(101,session.offeredFlowers.size)
        assertEquals(first,session.offeredFlowers.first().position)
        assertEquals(setOf(0,1),session.flowers)
    }
    @Test fun flowerBedsStayOrderedAndReuseSlots() {
        for(deity in 0..1) {
            val positions=(0 until TempleSceneLayout.OFFERED_FLOWER_SLOTS).map {
                TempleSceneLayout.offeredFlower(deity,it)
            }
            assertEquals(TempleSceneLayout.OFFERED_FLOWER_SLOTS,positions.toSet().size)
            assertTrue(positions.all {it.y in .685f.. .714f})
            assertEquals(positions.first(),TempleSceneLayout.offeredFlower(deity,TempleSceneLayout.OFFERED_FLOWER_SLOTS))
            assertTrue(positions.all {kotlin.math.abs(it.x-TempleSceneLayout.oil.x)>.045f})
        }
    }
    @Test fun prasadMakesOneClockwiseCircleAndLandsBelowShrine() {
        assertEquals(TempleSceneLayout.prasadRest,TempleSceneLayout.prasadPosition(0f))
        val landed=TempleSceneLayout.prasadPosition(1f)
        assertEquals(TempleSceneLayout.prasadFloor.x,landed.x,.00001f)
        assertEquals(TempleSceneLayout.prasadFloor.y,landed.y,.00001f)
        assertTrue(landed.y>TempleSceneLayout.oil.y)
        val points=listOf(.22f,.36f,.50f,.64f,.78f).map {TempleSceneLayout.prasadPosition(it)}
        val expected=listOf(TemplePoint(.65f,.56f),TemplePoint(.51f,.66f),TemplePoint(.37f,.56f),TemplePoint(.51f,.46f),TemplePoint(.65f,.56f))
        points.zip(expected).forEach {(actual,target) ->
            assertEquals(target.x,actual.x,.00001f)
            assertEquals(target.y,actual.y,.00001f)
        }
        assertFalse(WorshipSession(step=WorshipStep.PRASAD).canContinue)
    }

    @Test fun prasadAdvancesDirectlyToAarti() {
        assertEquals(7,WorshipStep.values().size)
        assertEquals(WorshipStep.AARTI,WorshipSession(step=WorshipStep.PRASAD,prasadOffered=true).next().step)
    }
    @Test fun conchWaitsForItsAnimation() {
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
        assertEquals(WorshipStep.CONCH,one.copy(flowers=setOf(0,1)).next().step)
    }

    @Test fun waterLotaRestsClearOnAltarAndReturnsSmoothlyAfterAbhishek() {
        val rest = TemplePoint(TempleSceneLayout.waterLota.x, TempleSceneLayout.waterLota.y - .06f)
        val pour = TemplePoint(.40f, .25f)
        // Rests between center prasad floor and right oil lamp, clear above the thali rim.
        assertTrue(TempleSceneLayout.waterLota.x > TempleSceneLayout.prasadFloor.x + .09f)
        assertTrue(TempleSceneLayout.waterLota.x < ShrineSpace.forId("marble").lamp.x - .09f)
        assertTrue(TempleSceneLayout.waterLota.y < TempleSceneLayout.plate.y - .20f)
        assertEquals(rest, TempleSceneLayout.waterLotaPosition(rest, pour, 0f))
        assertEquals(rest, TempleSceneLayout.waterLotaPosition(rest, pour, 1f))
        assertEquals(0f, TempleSceneLayout.waterLotaTilt(0f), .0001f)
        assertEquals(1f, TempleSceneLayout.waterLotaTilt(.5f), .0001f)
        assertEquals(0f, TempleSceneLayout.waterLotaTilt(1f), .0001f)
        for (i in 1..200) {
            val a = TempleSceneLayout.waterLotaPosition(rest, pour, (i - 1) / 200f)
            val b = TempleSceneLayout.waterLotaPosition(rest, pour, i / 200f)
            assertTrue(kotlin.math.abs(a.x - b.x) < .03f)
            assertTrue(kotlin.math.abs(a.y - b.y) < .04f)
        }
    }
}

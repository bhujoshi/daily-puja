package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.GaneshMouseAnimation
import com.worship.nityamandir.engine.GaneshMousePose
import com.worship.nityamandir.engine.IdolPlacement
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class GaneshMouseAnimationTest {
    private val shrines = listOf("original", "marble", "ivory", "carved")
    private val ganeshIdols = listOf("original", "ganesh_hanuman")
    private fun selection(shrine: String = "original", idol: String = "original", prasad: String = "original") =
        ShrineSelection(mapOf("shrine" to shrine, "idols" to idol, "prasad" to prasad))

    private fun pose(seconds: Float, selection: ShrineSelection): GaneshMousePose =
        GaneshMouseAnimation.pose(seconds / 12f, IdolPlacement(selection), selection)

    @Test fun appearsOnlyWithGaneshAndLaddu() {
        for (shrine in shrines) for (idol in ganeshIdols) for (prasad in listOf("original", "laddu")) {
            assertTrue(GaneshMouseAnimation.isEligible(selection(shrine, idol, prasad)))
        }
        for (idol in listOf("shiva", "lakshmi", "durga", "ram_darbar")) {
            assertFalse(GaneshMouseAnimation.isEligible(selection(idol = idol)))
        }
        for (idol in ganeshIdols) {
            assertFalse(GaneshMouseAnimation.isEligible(selection(idol = idol, prasad = "halwa")))
        }
        assertTrue(GaneshMouseAnimation.isEligible(ShrineSelection()))
    }

    @Test fun eatsBesideSettledBowlBeforeTraveling() {
        for (shrine in shrines) for (idol in ganeshIdols) {
            val selection = selection(shrine, idol)
            val placement = IdolPlacement(selection)
            val early = pose(1f, selection)
            val eating = pose(3f, selection)
            val finished = pose(4.6f, selection)
            assertEquals(placement.prasadFloor.x - .08f, eating.position.x, .00001f)
            assertEquals(placement.prasadFloor.y, eating.position.y, .00001f)
            assertEquals(early.position, eating.position)
            assertEquals(eating.position, finished.position)
            assertEquals(1f, eating.eating, .00001f)
            assertTrue(early.ladduRemaining > eating.ladduRemaining)
            assertEquals(0f, finished.ladduRemaining, .00001f)
            assertEquals(0f, finished.eating, .00001f)
            assertTrue(eating.facingRight)
            assertEquals(GaneshMouseAnimation.feet(placement, selection).x >= placement.prasadFloor.x - .08f, pose(6f, selection).facingRight)
        }
    }

    @Test fun walkingStaysOnTheFloorUntilTakeoff() {
        for (shrine in shrines) for (idol in ganeshIdols) {
            val selection = selection(shrine, idol)
            val placement = IdolPlacement(selection)
            val floor = placement.space.floorY
            val bowl = placement.prasadFloor
            for (i in 46..84) {
                val mouse = pose(i / 10f, selection)
                assertTrue("Mouse leaves the floor while walking in $shrine/$idol",
                    mouse.position.y >= floor - .00001f && mouse.position.y <= bowl.y + .00001f)
                assertEquals(i == 46 || GaneshMouseAnimation.feet(placement, selection).x >= bowl.x - .08f, mouse.facingRight)
            }
            assertEquals(floor, pose(8.4f, selection).position.y, .00001f)
        }
    }

    @Test fun landsAtGaneshFootInEveryShrineAndThenRests() {
        for (shrine in shrines) for (idol in ganeshIdols) {
            val selection = selection(shrine, idol)
            val placement = IdolPlacement(selection)
            val rest = pose(12f, selection)
            val localX = if (idol == "ganesh_hanuman") .26f else .20f
            val localY = if (idol == "ganesh_hanuman") .88f else .85f
            assertEquals(placement.left + placement.width * localX, rest.position.x, .00001f)
            assertEquals(placement.top + placement.height * localY, rest.position.y, .00001f)
            assertTrue(rest.position.x < .5f)
            assertTrue(rest.position.y < placement.bottom)
            assertEquals(.74f, rest.scale, .00001f)
            assertEquals(0f, rest.eating, .00001f)
            assertEquals(0f, rest.scurry, .00001f)
            assertEquals(0f, rest.hop, .00001f)
            assertEquals(0f, rest.bow, .00001f)
            assertEquals(rest, pose(20f, selection))
            assertEquals(pose(0f, selection), pose(-2f, selection))
        }
    }

    @Test fun motionAndAppearanceStayContinuousAcrossEachPhase() {
        for (shrine in shrines) for (idol in ganeshIdols) {
            val selection = selection(shrine, idol)
            for (boundary in listOf(.65f, 4f, 4.5f, 4.6f, 4.9f, 8.1f, 8.4f, 9.7f, 10.05f, 10.55f, 11f)) {
                val before = pose(boundary - .001f, selection)
                val after = pose(boundary + .001f, selection)
                assertTrue("Position jumps at $boundary for $shrine/$idol",
                    hypot(before.position.x - after.position.x, before.position.y - after.position.y) < .001f)
                val beforeValues = listOf(before.opacity, before.scale, before.eating, before.ladduRemaining,
                    before.scurry, before.hop, before.bow)
                val afterValues = listOf(after.opacity, after.scale, after.eating, after.ladduRemaining,
                    after.scurry, after.hop, after.bow)
                beforeValues.zip(afterValues).forEach { (a, b) -> assertEquals(a, b, .01f) }
            }
        }
    }

    @Test fun approachesTheAltarHopsAndBowsBeforeSettling() {
        val selection = selection()
        val walking = pose(6f, selection)
        val takeoff = pose(8.4f, selection)
        val airborne = pose(9.05f, selection)
        val landed = pose(9.7f, selection)
        assertTrue(walking.scurry > .9f)
        assertTrue(takeoff.position.y < walking.position.y)
        assertTrue(airborne.hop > .9f)
        assertTrue(airborne.position.y < takeoff.position.y)
        assertTrue(pose(10.3f, selection).bow > .9f)
        assertEquals(landed.position, pose(12f, selection).position)
        for (i in 0..240) {
            val pose = pose(i / 20f, selection)
            listOf(pose.opacity, pose.eating, pose.ladduRemaining, pose.scurry, pose.hop, pose.bow)
                .forEach { assertTrue(it in 0f..1f) }
            assertTrue(pose.scale in .74f..1f)
        }
    }
}

package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.data.ShrineUnlocks
import com.worship.nityamandir.engine.IdolPlacement
import com.worship.nityamandir.engine.OccasionAartiAnimation
import com.worship.nityamandir.engine.OccasionCelebrationStyle
import com.worship.nityamandir.engine.TempleOccasions
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class OccasionAartiAnimationTest {
    @Test fun diwaliIsActiveDuringDiwaliSeasonAndRestrictsDiwaliShrineToOccasion() {
        val diwaliDate = LocalDate.of(2026, 11, 1)
        val ordinaryDate = LocalDate.of(2026, 6, 15)

        val activeOnDiwali = TempleOccasions.activeOccasion(diwaliDate)
        assertNotNull(activeOnDiwali)
        assertEquals("diwali", activeOnDiwali?.id)
        assertEquals(OccasionCelebrationStyle.DIWALI_DEEPMALA, activeOnDiwali?.celebrationStyle)

        val activeOnOrdinary = TempleOccasions.activeOccasion(ordinaryDate)
        assertNull(activeOnOrdinary)

        // Diwali shrine is visible during Diwali, hidden on ordinary days unless already selected
        assertTrue(TempleOccasions.isShrineVisible("diwali", activeOnDiwali, ShrineSelection()))
        assertFalse(TempleOccasions.isShrineVisible("diwali", activeOnOrdinary, ShrineSelection()))
        assertTrue(
            TempleOccasions.isShrineVisible(
                "diwali",
                activeOnOrdinary,
                ShrineSelection(mapOf("shrine" to "diwali"))
            )
        )
        // Standard shrines remain visible year-round
        for (shrine in listOf("marble", "ivory", "carved", "swarna", "divya_darshan", "kamal_peeth", "swarna_peeth")) {
            assertTrue(TempleOccasions.isShrineVisible(shrine, activeOnOrdinary, ShrineSelection()))
            assertTrue(TempleOccasions.isShrineVisible(shrine, activeOnDiwali, ShrineSelection()))
        }
    }

    @Test fun diwaliWindowIncludesNovemberFirstThroughEleventhOnly() {
        for (day in 1..11) {
            assertEquals("diwali", TempleOccasions.activeOccasion(LocalDate.of(2026, 11, day))?.id)
        }
        for (date in listOf(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 12))) {
            val occasion = TempleOccasions.activeOccasion(date)
            assertNull(occasion)
            assertFalse(TempleOccasions.isShrineVisible("diwali", occasion, ShrineSelection()))
        }
    }

    @Test fun diwaliDecorationsLeaveSpaceForFlowersPrasadAndRitualStands() {
        val selection = TempleOccasions.DIWALI.presetSelection
        val placement = IdolPlacement(selection)
        val pose = OccasionAartiAnimation.pose(1f, placement, selection)
        assertEquals(OccasionAartiAnimation.ALTAR_DIYA_COUNT, pose.diyas.size)
        pose.diyas.forEach { diya ->
            val unit = .036f * diya.scale
            assertTrue(diya.position.y - unit * 1.3f > placement.bottom + .04f)
            assertTrue(diya.position.x - unit > placement.space.incense.x + .04f)
            assertTrue(diya.position.x + unit < placement.space.lamp.x - .07f)
        }
        val rangoliBottom = pose.rangoli.center.y + pose.rangoli.radiusX * pose.rangoli.verticalSquash * 1.1f
        assertTrue(rangoliBottom < placement.prasadFloor.y - .045f)
        pose.charanSteps.forEach { step ->
            assertTrue(step.position.y + .028f * step.scale < placement.prasadFloor.y - .045f)
        }
        pose.charanSteps.groupBy { it.isLeftFoot }.values.forEach { steps ->
            steps.zipWithNext().forEach { (front, back) ->
                assertTrue("Footprint outlines must not overlap", front.position.y - back.position.y >
                    .028f * (front.scale + back.scale) * .55f)
            }
        }
    }

    @Test fun everyOccasionPresetUsesFreeItemsSoAllUsersCanApply() {
        for (occasion in TempleOccasions.all) {
            for ((category, value) in occasion.presetSelection.values) {
                value.split(',').forEach { itemId ->
                    assertEquals(
                        "Occasion ${occasion.id} preset item $category:$itemId should be free",
                        0,
                        ShrineUnlocks.cost(category, itemId)
                    )
                }
            }
            assertTrue(occasion.matchesSelection(occasion.presetSelection))
            assertTrue(OccasionAartiAnimation.isEligible(occasion.presetSelection, occasion))
        }
    }

    @Test fun debugOverrideForcesAnyOccasionOrOrdinaryDay() {
        val date = LocalDate.of(2026, 10, 31)
        assertNull(TempleOccasions.activeOccasion(date, TempleOccasions.OVERRIDE_NONE))
        for (occasion in TempleOccasions.all) {
            val forced = TempleOccasions.activeOccasion(LocalDate.of(2026, 1, 15), occasion.id)
            assertEquals(occasion.id, forced?.id)
        }
    }

    @Test fun deepmalaDiyasIgniteSequentiallyFromCenterOutward() {
        val selection = TempleOccasions.NAVRATRI.presetSelection
        val placement = IdolPlacement(selection)

        val earlyPose = OccasionAartiAnimation.pose(0.9f / 12f, placement, selection, TempleOccasions.NAVRATRI)
        val centerAltarDiya = earlyPose.diyas[OccasionAartiAnimation.ALTAR_DIYA_COUNT / 2]
        val outerLeftAltarDiya = earlyPose.diyas.first()
        val outerRightAltarDiya = earlyPose.diyas[OccasionAartiAnimation.ALTAR_DIYA_COUNT - 1]
        val firstFloorDiya = earlyPose.diyas[OccasionAartiAnimation.ALTAR_DIYA_COUNT]

        assertTrue("Center diya should start igniting first", centerAltarDiya.flameIgnite > 0f)
        assertEquals(0f, outerLeftAltarDiya.flameIgnite, 0.0001f)
        assertEquals(0f, outerRightAltarDiya.flameIgnite, 0.0001f)
        assertEquals(0f, firstFloorDiya.flameIgnite, 0.0001f)

        // By 3.0s, all 7 altar diyas are lit while floor diyas are still unlit
        val midPose = OccasionAartiAnimation.pose(3.0f / 12f, placement, selection, TempleOccasions.NAVRATRI)
        for (i in 0 until OccasionAartiAnimation.ALTAR_DIYA_COUNT) {
            assertEquals("Altar diya $i should be lit at 3.0s", 1f, midPose.diyas[i].flameIgnite, 0.05f)
        }
        assertEquals(0f, midPose.diyas[OccasionAartiAnimation.ALTAR_DIYA_COUNT].flameIgnite, 0.0001f)

        // By 6.0s, all 13 diyas (altar + floor arc) are fully lit
        val fullDiyasPose = OccasionAartiAnimation.pose(6.0f / 12f, placement, selection, TempleOccasions.NAVRATRI)
        fullDiyasPose.diyas.forEach { diya ->
            assertEquals(1f, diya.flameIgnite, 0.0001f)
            assertEquals(1f, diya.vesselOpacity, 0.0001f)
            assertTrue(diya.floorReflection > 0.7f)
        }
    }

    @Test fun lakshmiCharanFootstepsWalkInwardTowardAltarAndAlternateFeet() {
        val selection = TempleOccasions.DIWALI.presetSelection
        val placement = IdolPlacement(selection)
        val finalPose = OccasionAartiAnimation.pose(1f, placement, selection)

        assertEquals(OccasionAartiAnimation.CHARAN_STEP_COUNT, finalPose.charanSteps.size)
        for (i in finalPose.charanSteps.indices) {
            val step = finalPose.charanSteps[i]
            assertEquals(i % 2 == 0, step.isLeftFoot)
            assertEquals(1f, step.imprintOpacity, 0.0001f)
            if (step.isLeftFoot) {
                assertTrue(step.position.x < 0.50f)
            } else {
                assertTrue(step.position.x > 0.50f)
            }
            if (i > 0) {
                val prev = finalPose.charanSteps[i - 1]
                assertTrue("Each step must advance closer to the altar (smaller y)", step.position.y < prev.position.y)
                assertTrue("Perspective scale should decrease slightly toward altar", step.scale <= prev.scale)
            }
        }
    }

    @Test fun rangoliAndCelebrationStayContinuousAcrossAllFrames() {
        val selection = TempleOccasions.DIWALI.presetSelection
        val placement = IdolPlacement(selection)

        var previous = OccasionAartiAnimation.pose(0f, placement, selection)
        for (frame in 1..360) {
            val current = OccasionAartiAnimation.pose(frame / 360f, placement, selection)
            assertTrue(abs(current.rangoli.innerBloom - previous.rangoli.innerBloom) < 0.06f)
            assertTrue(abs(current.rangoli.middleBloom - previous.rangoli.middleBloom) < 0.06f)
            assertTrue(abs(current.rangoli.outerBloom - previous.rangoli.outerBloom) < 0.06f)
            current.diyas.zip(previous.diyas).forEach { (curr, prev) ->
                assertTrue(abs(curr.flameIgnite - prev.flameIgnite) < 0.12f)
                assertTrue(curr.flameIgnite in 0f..1f)
                assertTrue(curr.sparkBurst in 0f..1f)
            }
            current.charanSteps.zip(previous.charanSteps).forEach { (curr, prev) ->
                assertTrue(abs(curr.imprintOpacity - prev.imprintOpacity) < 0.12f)
                assertTrue(curr.imprintOpacity in 0f..1f)
            }
            previous = current
        }
    }
}

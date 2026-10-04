package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.*
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sqrt

class FlowerSpacingTest {
    // scaleToUnits fits the largest model dimension; sqrt(3) bounds any rotation.
    private fun verify(points: List<TemplePoint>, maxModelSize: Float) {
        val depths=TempleSceneLayout.flowerDepths(points,List(points.size) {maxModelSize},.2f)
        val diameter = sqrt(3f)*maxModelSize
        points.forEachIndexed { i,a -> points.indices.filter {it>i}.forEach { j ->
            val b=points[j]
            assertTrue("Intersecting bounds at $a and $b", sqrt(4f*((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y)) +
                (depths[i]-depths[j])*(depths[i]-depths[j])) >= diameter)
        }}
    }
    @Test fun originalSizeIsPreservedOnPlateAndAltar() {
        for(i in 0 until TempleSceneLayout.FLOWER_COUNT) {
            org.junit.Assert.assertEquals((.10f+(i%3)*.008f)*1.3f,TempleSceneLayout.flowerSize(i),.00001f)
            org.junit.Assert.assertEquals(TempleSceneLayout.flowerSize(i),TempleSceneLayout.offeredFlowerSize(i),.00001f)
        }
    }
    @Test fun plateBoundsDoNotIntersect() {
        verify((0 until TempleSceneLayout.FLOWER_COUNT).map(TempleSceneLayout::plateFlower),
            (0 until TempleSceneLayout.FLOWER_COUNT).maxOf(TempleSceneLayout::flowerSize))
    }
    @Test fun carriedItemsStayAheadOfFullFlowerBeds() {
        val plateDepths=TempleSceneLayout.flowerDepths(
            (0 until TempleSceneLayout.FLOWER_COUNT).map(TempleSceneLayout::plateFlower),
            (0 until TempleSceneLayout.FLOWER_COUNT).map(TempleSceneLayout::flowerSize),.15f)
        assertTrue(TempleSceneLayout.FLOWER_FLIGHT_DEPTH > plateDepths.max()+.3f)
        assertTrue(TempleSceneLayout.ACTIVE_RITUAL_DEPTH > TempleSceneLayout.FLOWER_FLIGHT_DEPTH)

        for(shrine in listOf("original","marble","ivory","carved")) {
            for(idol in listOf("original","ganesh_hanuman","shiva","lakshmi","durga","ram_darbar")) {
                val selection=ShrineSelection(mapOf("shrine" to shrine,"idols" to idol))
                val placement=IdolPlacement(selection)
                val offered=(0 until selection.deityCount).flatMap { deity ->
                    (0 until TempleSceneLayout.OFFERED_FLOWER_SLOTS).map { count -> placement.offering(deity,count) }
                }
                val largestFlower=(0 until TempleSceneLayout.FLOWER_COUNT).maxOf(TempleSceneLayout::offeredFlowerSize)
                val sizes=List(offered.size) {largestFlower}
                val offeredDepths=TempleSceneLayout.flowerDepths(offered,sizes,.20f)
                assertTrue("$shrine/$idol: ${offeredDepths.max()}",TempleSceneLayout.FLOWER_FLIGHT_DEPTH > offeredDepths.max()+.3f)
            }
        }
    }
    @Test fun offeredBoundsRemainSeparateAcrossLayoutsAndSlotReuse() {
        for(shrine in listOf("original","marble","ivory","carved")) {
            for(idol in listOf("original","ganesh_hanuman","shiva","lakshmi","durga","ram_darbar")) {
                val selection=ShrineSelection(mapOf("shrine" to shrine,"idols" to idol))
                val placement=IdolPlacement(selection)
                for(start in 0..20) {
                    val points=(0 until selection.deityCount).flatMap { deity ->
                        (start until start+TempleSceneLayout.OFFERED_FLOWER_SLOTS).map {placement.offering(deity,it)}
                    }
                    verify(points,(0 until TempleSceneLayout.FLOWER_COUNT).maxOf(TempleSceneLayout::offeredFlowerSize))
                    assertTrue(points.all {it.y <= placement.bottom+.04f})
                }
            }
        }
    }
}

package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.IdolPlacement
import com.worship.nityamandir.engine.TempleSceneLayout
import org.junit.Assert.*
import org.junit.Test

class ShrineSelectionTest {
    @Test fun unlockingSelectsItemsWithoutRemovingChosenFlowers() {
        val selected=ShrineSelection(mapOf("flowers" to "rose,orchid"))
        assertEquals(listOf("rose","orchid"),selected.selectItem("flowers","rose").flowerIds)
        assertEquals(listOf("rose","orchid","azalea"),selected.selectItem("flowers","azalea").flowerIds)
        assertEquals("marble",selected.selectItem("shrine","marble")["shrine"])
        assertEquals(selected.flowerIds,selected.selectItem("shrine","marble").flowerIds)
    }
    @Test fun allFlowerVarietiesCanBeSelectedAndRestored() {
        val ids=listOf("original","orchid","rose","azalea","bouquet")
        val selection=ids.drop(1).fold(ShrineSelection()) {s,id -> s.toggleFlower(id)}
        assertEquals(ids,ShrineSelection(selection.values).flowerIds)
        assertEquals(ids-"rose",selection.toggleFlower("rose").flowerIds)
        assertEquals(listOf("original"),ShrineSelection().toggleFlower("original").flowerIds)
        assertEquals(listOf("rose"),ShrineSelection(mapOf("flowers" to "rose")).flowerIds)
    }
    @Test fun effectsAndOfferingsFollowEveryIdolAndAltar() {
        for(shrine in listOf("original","marble","ivory","carved")) {
            for(idol in listOf("original","ganesh_hanuman","shiva","lakshmi","durga","ram_darbar")) {
                val selection=ShrineSelection(mapOf("shrine" to shrine,"idols" to idol))
                val placement=IdolPlacement(selection)
                assertEquals(selection.deityCount,placement.heads.size)
                assertEquals(TempleSceneLayout.aartiRest,placement.aartiPosition(0f))
                assertEquals(TempleSceneLayout.aartiRest,placement.aartiPosition(1f))
                assertEquals(TempleSceneLayout.prasadRest,placement.prasadPosition(0f))
                assertEquals(placement.prasadFloor.y,placement.prasadPosition(1f).y,.0001f)
                assertEquals(placement.space.lamp.y,placement.prasadFloor.y,.0001f)
                assertEquals(.50f,placement.prasadFloor.x,.0001f)
                assertTrue(placement.prasadFloor.y>placement.space.floorY)
                placement.heads.forEachIndexed {i,head ->
                    assertTrue(head.x in placement.left..(placement.left+placement.width))
                    assertTrue(head.y in placement.top..placement.bottom)
                    assertEquals(head.x,placement.offering(i,0).x,.0001f)
                    assertTrue(placement.crowns[i].y < head.y)
                    assertTrue(placement.bathSource(i).y < placement.crowns[i].y)
                    assertEquals(placement.crowns[i].x,placement.bathSource(i).x,.0001f)
                    assertTrue(placement.space.lamp.y > placement.bottom)
                    assertTrue(placement.space.lamp.x > placement.left+placement.width)
                    assertEquals(placement.bottom+.008f,placement.offering(i,0).y,.0001f)
                }
            }
        }
    }

    @Test fun ritualItemsPreserveScalesAndConsistentBoundsAcrossLayouts() {
        assertEquals(1.15f, TempleSceneLayout.CONCH_SCALE, 0.0001f)
        assertEquals(.26f * 1.15f, TempleSceneLayout.CONCH_SIZE, 0.0001f)
        assertEquals(.16f * 1.15f, TempleSceneLayout.CONCH_WIDTH, 0.0001f)
        assertEquals(.20f * 1.15f, TempleSceneLayout.CONCH_HEIGHT, 0.0001f)

        assertEquals(1.25f, TempleSceneLayout.AARTI_SCALE, 0.0001f)
        assertEquals(.15f * 1.25f, TempleSceneLayout.AARTI_WIDTH, 0.0001f)
        assertEquals(.135f * 1.25f, TempleSceneLayout.AARTI_HEIGHT, 0.0001f)

        assertEquals(.22f, TempleSceneLayout.bell.x, 0.0001f)
        assertEquals(.93f, TempleSceneLayout.bell.y, 0.0001f)
        assertEquals(.625f, TempleSceneLayout.conch.x, 0.0001f)
        assertEquals(1.125f, TempleSceneLayout.conch.y, 0.0001f)
        assertEquals(.50f, TempleSceneLayout.prasadFloor.x, 0.0001f)
        assertEquals(.88f, TempleSceneLayout.prasadFloor.y, 0.0001f)
        assertEquals(.155f, TempleSceneLayout.PRASAD_SIZE, 0.0001f)

        // Verify bell stays fully inside viewport even on an ultra-tall 21:9 display
        val tallViewport = com.worship.nityamandir.engine.TempleViewport(1080f, 2520f)
        val bellPixel = tallViewport.pixel(TempleSceneLayout.bell)
        val bellRadiusPixels = tallViewport.imageWidth * TempleSceneLayout.bellHalfWidth
        assertTrue("Bell left edge must be inside screen", bellPixel.x - bellRadiusPixels > 0f)
    }
}

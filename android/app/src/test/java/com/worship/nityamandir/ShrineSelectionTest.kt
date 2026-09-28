package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.IdolPlacement
import com.worship.nityamandir.engine.TempleSceneLayout
import org.junit.Assert.*
import org.junit.Test

class ShrineSelectionTest {
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
}

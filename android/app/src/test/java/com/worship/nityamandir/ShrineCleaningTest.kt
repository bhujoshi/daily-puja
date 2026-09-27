package com.worship.nityamandir

import com.worship.nityamandir.engine.*
import org.junit.Assert.*
import org.junit.Test

class ShrineCleaningTest {
    @Test fun cleaningMeasuresSurfaceCoverageRatherThanSwipeDistance() {
        for(id in listOf("original","marble","ivory","carved")) {
            val space=ShrineSpace.forId(id)
            assertEquals(0f,ShrineCleaning.coverage(space,listOf(TemplePoint(0f,1.5f))),.0001f)
            val point=ShrineCleaning.samples(space).first()
            assertEquals(ShrineCleaning.coverage(space,listOf(point)),ShrineCleaning.coverage(space,List(100) {point}),.0001f)
            assertEquals(1f,ShrineCleaning.coverage(space,ShrineCleaning.samples(space)),.0001f)
        }
    }
}

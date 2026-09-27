package com.worship.nityamandir.engine

import kotlin.math.hypot

object ShrineCleaning {
    const val RADIUS=.06f
    fun samples(space:ShrineSpace):List<TemplePoint> = (0 until 8).flatMap {row ->
        (0 until 8).map {column -> TemplePoint(space.left+(column+.5f)/8*(space.right-space.left),space.archTop+(row+.5f)/8*(space.altarY+.025f-space.archTop))}
    }
    fun coverage(space:ShrineSpace,cleaned:List<TemplePoint>):Float {
        val samples=samples(space)
        return samples.count {p -> cleaned.any {hypot(p.x-it.x,p.y-it.y)<=RADIUS}}/samples.size.toFloat()
    }
}

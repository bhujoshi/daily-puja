package com.worship.nityamandir.engine

import kotlin.math.hypot

object ShrineCleaning {
    const val RADIUS=.06f
    private fun shrineSamples(space:ShrineSpace):List<TemplePoint> = (0 until 8).flatMap {row ->
        (0 until 8).map {column -> TemplePoint(space.left+(column+.5f)/8*(space.right-space.left),space.archTop+(row+.5f)/8*(space.altarY+.025f-space.archTop))}
    }
    private val foregroundSamples=listOf(
        TemplePoint(.24f,.88f),TemplePoint(.50f,.83f),TemplePoint(.76f,.88f),
        TemplePoint(.15f,.98f),TemplePoint(.28f,1.06f),TemplePoint(.50f,1.08f),
        TemplePoint(.50f,1.14f),TemplePoint(.66f,1.08f),TemplePoint(.79f,1.08f)
    )
    fun samples(space:ShrineSpace):List<TemplePoint> = shrineSamples(space)+foregroundSamples
    fun contains(space:ShrineSpace,point:TemplePoint):Boolean = space.contains(point) ||
        foregroundSamples.any {hypot(point.x-it.x,point.y-it.y)<=.12f}
    fun coverage(space:ShrineSpace,cleaned:List<TemplePoint>):Float {
        val samples=samples(space)
        return samples.count {p -> cleaned.any {hypot(p.x-it.x,p.y-it.y)<=RADIUS}}/samples.size.toFloat()
    }
}

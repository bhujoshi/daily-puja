package com.worship.nityamandir.engine

/** Measured surfaces in each portrait; all coordinates are image-width fractions. */
data class ShrineSpace(
    val altarY:Float,val left:Float,val right:Float,val archTop:Float,val floorY:Float,
    val lamp:TemplePoint,val idolWidth:Float,val idolHeight:Float
) {
    val ledgeY get()=altarY+.016f
    fun contains(point:TemplePoint)=point.x in left..right && point.y in archTop..(altarY+.04f)
    companion object {
        fun forId(id:String)=when(id) {
            "marble" -> ShrineSpace(.650f,.23f,.77f,.11f,.82f,TemplePoint(.76f,.88f),.38f,.35f)
            "ivory" -> ShrineSpace(.608f,.33f,.67f,.21f,.78f,TemplePoint(.74f,.84f),.34f,.33f)
            "carved" -> ShrineSpace(.636f,.15f,.85f,.22f,.73f,TemplePoint(.77f,.81f),.40f,.34f)
            else -> ShrineSpace(.705f,.294f,.714f,.25f,.90f,TemplePoint(.80f,.95f),.38f,.33f)
        }
    }
}

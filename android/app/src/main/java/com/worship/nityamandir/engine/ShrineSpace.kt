package com.worship.nityamandir.engine

/** Measured surfaces in each portrait; all coordinates are image-width fractions. */
data class ShrineSpace(
    val altarY: Float,
    val left: Float,
    val right: Float,
    val archTop: Float,
    val floorY: Float,
    val lamp: TemplePoint,
    val idolWidth: Float,
    val idolHeight: Float,
    val bellCeilingY: Float = .285f,
    val incense: TemplePoint = TempleSceneLayout.incense,
    val garlandSpec: FloatArray = floatArrayOf(.145f, .025f, .855f, .655f, .088f, .165f)
) {
    val ledgeY get() = altarY + .016f
    fun contains(point: TemplePoint) = point.x in left..right && point.y in archTop..(altarY + .04f)
    companion object {
        fun forId(id: String) = when (id) {
            "carved" -> ShrineSpace(
                altarY = .680f,
                left = .23f,
                right = .77f,
                archTop = .11f,
                floorY = .82f,
                lamp = TemplePoint(.76f, .88f),
                idolWidth = .46f,
                idolHeight = .395f,
                bellCeilingY = .285f,
                incense = TempleSceneLayout.incense,
                garlandSpec = floatArrayOf(.140f, .025f, .860f, .655f, .088f, .165f)
            )
            "marble", "ivory", "swarna" -> ShrineSpace(
                altarY = .680f,
                left = .23f,
                right = .77f,
                archTop = .11f,
                floorY = .82f,
                lamp = TemplePoint(.76f, .88f),
                idolWidth = .46f,
                idolHeight = .395f,
                bellCeilingY = .285f,
                incense = TempleSceneLayout.incense,
                garlandSpec = floatArrayOf(.145f, .025f, .855f, .655f, .088f, .165f)
            )
            else -> ShrineSpace(
                altarY = .680f,
                left = .23f,
                right = .77f,
                archTop = .11f,
                floorY = .82f,
                lamp = TemplePoint(.76f, .88f),
                idolWidth = .46f,
                idolHeight = .395f,
                bellCeilingY = .285f,
                incense = TempleSceneLayout.incense,
                garlandSpec = floatArrayOf(.145f, .025f, .855f, .655f, .088f, .165f)
            )
        }
    }
}

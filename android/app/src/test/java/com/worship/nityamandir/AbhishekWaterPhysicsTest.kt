package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.hypot

class AbhishekWaterPhysicsTest {

    @Test
    fun lotaPourCenterLocksTiltedLipDirectlyToStreamOriginWithZeroGap() {
        val shrines = listOf("marble", "original", "divya_darshan", "swarna_peeth")
        val idols = listOf("original", "shiva", "ganesh_hanuman", "radha_krishna", "durga", "hanuman", "shanidev")
        val aspect = TempleSceneLayout.WATER_LOTA_DEFAULT_ASPECT
        val pourWidth = TempleSceneLayout.WATER_LOTA_POUR_WIDTH

        for (shrine in shrines) {
            for (idol in idols) {
                val selection = ShrineSelection(mapOf("shrine" to shrine, "idols" to idol))
                val placement = IdolPlacement(selection)
                val surface = IdolWaterSurface.syntheticDome(dualDeity = selection.deityCount > 1)
                for (deity in 0 until selection.deityCount) {
                    val source = placement.bathSource(deity)
                    val pourCenter = TempleSceneLayout.waterLotaPourCenter(source, pourWidth, aspect)
                    val lipOuter = TempleSceneLayout.waterLotaLip(pourCenter, pourWidth, aspect, tilt = 1f)
                    val lipInner = TempleSceneLayout.waterLotaInnerLip(pourCenter, pourWidth, aspect, tilt = 1f)

                    // Outer lip lands precisely on the stream's expected origin (zero gap!).
                    assertEquals(source.x + TempleSceneLayout.WATER_LOTA_SPOUT_LEAD_X, lipOuter.x, 0.0001f)
                    assertEquals(source.y, lipOuter.y, 0.0001f)
                    // Inner mouth meniscus sits tucked slightly inside the tilted copper rim.
                    val lipGap = hypot(lipInner.x - lipOuter.x, lipInner.y - lipOuter.y)
                    assertTrue("Inner meniscus should sit close inside the lip ($lipGap)", lipGap in 0.004f..0.020f)
                    assertTrue("Inner meniscus should be to the right of the left-pouring outer lip", lipInner.x > lipOuter.x)

                    val plan = AbhishekWaterPhysics.buildPlan(placement, deity, surface)
                    val streamStart = AbhishekWaterPhysics.streamPoint(lipOuter, plan.impact, 0f)
                    val streamEnd = AbhishekWaterPhysics.streamPoint(lipOuter, plan.impact, 1f)
                    assertEquals(lipOuter.x, streamStart.x, 0.0001f)
                    assertEquals(lipOuter.y, streamStart.y, 0.0001f)
                    assertEquals(plan.impact.x, streamEnd.x, 0.0001f)
                    assertEquals(plan.impact.y, streamEnd.y, 0.0001f)
                }
            }
        }
    }

    @Test
    fun streamHeadAndTailProgressCleanlyWithLotaTilt() {
        assertEquals(0f, TempleSceneLayout.waterStreamHead(0.10f), 0.0001f)
        assertEquals(0f, TempleSceneLayout.waterPourIntensity(0.10f), 0.0001f)
        assertTrue(TempleSceneLayout.waterStreamHead(0.18f) in 0.1f..0.9f)
        assertEquals(1f, TempleSceneLayout.waterStreamHead(0.25f), 0.0001f)
        assertEquals(0f, TempleSceneLayout.waterStreamTail(0.50f), 0.0001f)
        assertEquals(1f, TempleSceneLayout.waterPourIntensity(0.50f), 0.0001f)
        assertTrue(TempleSceneLayout.waterStreamTail(0.82f) in 0.1f..0.9f)
        assertEquals(1f, TempleSceneLayout.waterStreamTail(0.88f), 0.0001f)
        assertEquals(0f, TempleSceneLayout.waterPourIntensity(0.88f), 0.0001f)
    }

    @Test
    fun idolSurfaceDetectsOutwardNormalsDeflectionsAndMultiBounceDroplets() {
        val selection = ShrineSelection(mapOf("shrine" to "marble", "idols" to "shiva"))
        val placement = IdolPlacement(selection)
        val surface = IdolWaterSurface.syntheticDome(dualDeity = false)

        // Left crown/shoulder edge normal points left & upward; right edge points right & upward.
        val (leftNx, leftNy) = surface.normalAt(0.35f, 0.28f, placement.width, placement.height)
        val (rightNx, rightNy) = surface.normalAt(0.65f, 0.28f, placement.width, placement.height)
        assertTrue("Left contour normal should point left ($leftNx)", leftNx < -0.1f)
        assertTrue("Right contour normal should point right ($rightNx)", rightNx > 0.1f)
        assertEquals(1f, hypot(leftNx, leftNy), 0.01f)
        assertEquals(1f, hypot(rightNx, rightNy), 0.01f)

        val plan = AbhishekWaterPhysics.buildPlan(placement, deity = 0, surface = surface)
        assertTrue("Should detect crown plus sculpted idol deflection ledges", plan.deflections.size >= 3)
        assertEquals(10, plan.rivulets.size)
        assertTrue("Should simulate ballistic deflected droplets", plan.droplets.size >= 20)
        assertTrue(
            "At least some deflected droplets should physically bounce off lower idol surfaces or pedestal",
            plan.droplets.any { it.bounceCount > 0 }
        )
        // Every rivulet cascades downward from crown to the altar floor.
        plan.rivulets.forEach { rivulet ->
            assertTrue(rivulet.points.size >= 8)
            assertEquals(plan.impact.y, rivulet.points.first().y, 0.005f)
            assertEquals(placement.bottom, rivulet.points.last().y, 0.005f)
            for (i in 1 until rivulet.arrivals.size) {
                assertTrue(rivulet.arrivals[i] >= rivulet.arrivals[i - 1] - 0.0001f)
            }
        }
    }

    @Test
    fun deflectedDropletsAndResidualBeadsStayStrictlyConfinedAroundIdol() {
        val cases = listOf(
            ShrineSelection(mapOf("shrine" to "marble", "idols" to "shiva")),
            ShrineSelection(mapOf("shrine" to "original", "idols" to "original")),
            ShrineSelection(mapOf("shrine" to "divya_darshan", "idols" to "ganesh_hanuman"))
        )

        for (selection in cases) {
            val placement = IdolPlacement(selection)
            val surface = IdolWaterSurface.syntheticDome(dualDeity = selection.deityCount > 1)
            val leashMargin = (placement.width * 0.14f).coerceIn(0.035f, 0.075f) + 0.002f

            for (deity in 0 until selection.deityCount) {
                val plan = AbhishekWaterPhysics.buildPlan(placement, deity, surface)
                val minX = plan.domainMinX - leashMargin
                val maxX = plan.domainMaxX + leashMargin
                val minY = plan.impact.y - 0.050f
                val maxY = placement.bottom + 0.001f

                // No droplet ever flies randomly across the screen, ceiling, walls, or onto the Puja Thali.
                plan.droplets.forEach { droplet ->
                    droplet.samples.forEach { sample ->
                        assertTrue("Droplet x=${sample.x} escaped idol leash [$minX..$maxX]", sample.x in minX..maxX)
                        assertTrue("Droplet y=${sample.y} escaped idol vertical bounds [$minY..$maxY]", sample.y in minY..maxY)
                        assertTrue("Droplet must stay well above the Puja Thali", sample.y < TempleSceneLayout.plate.y - 0.20f)
                    }
                }

                // Every residual water bead rests strictly on the idol's opaque silhouette (never floating in air!).
                assertTrue("Should generate delicate idol-confined wet beads", plan.residualBeads.isNotEmpty())
                plan.residualBeads.forEach { bead ->
                    val startU = (bead.start.x - placement.left) / placement.width
                    val startV = (bead.start.y - placement.top) / placement.height
                    val endU = (bead.end.x - placement.left) / placement.width
                    val endV = (bead.end.y - placement.top) / placement.height
                    assertTrue("Bead start ($startU, $startV) must sit on solid idol surface", surface.isSolid(startU, startV))
                    assertTrue("Bead end ($endU, $endV) must stay on solid idol surface", surface.isSolid(endU, endV))
                    assertTrue("Bead must stay inside bathed deity domain", bead.start.x in plan.domainMinX..plan.domainMaxX)
                    assertTrue("Bead trickles gently downward", bead.end.y >= bead.start.y)
                }
            }
        }
    }

    @Test
    fun dualDeityBathingIsolatesEffectsPerDeity() {
        val selection = ShrineSelection(mapOf("shrine" to "marble", "idols" to "original"))
        val placement = IdolPlacement(selection)
        val surface = IdolWaterSurface.syntheticDome(dualDeity = true)

        val plan0 = AbhishekWaterPhysics.buildPlan(placement, deity = 0, surface = surface)
        val plan1 = AbhishekWaterPhysics.buildPlan(placement, deity = 1, surface = surface)

        assertTrue(plan0.impact.x < plan1.impact.x)
        assertTrue(plan0.domainMaxX <= plan1.domainMinX + 0.0001f)
        assertTrue(plan0.residualBeads.all { it.start.x <= plan0.domainMaxX })
        assertTrue(plan1.residualBeads.all { it.start.x >= plan1.domainMinX })
        assertTrue(abs(plan0.pedestalRight - plan1.pedestalLeft) < 0.25f)
    }

    @Test
    fun allRealIdolAssetsAndNeedleTippedCrownsSimulateWithoutCrashing() {
        // Needle-pointed 1px crown and empty surface edge cases must never fail coerceIn or bounds.
        val needleGrid = FloatArray(IdolWaterSurface.DEFAULT_COLS * IdolWaterSurface.DEFAULT_ROWS)
        val midC = IdolWaterSurface.DEFAULT_COLS / 2
        for (r in 0 until IdolWaterSurface.DEFAULT_ROWS) {
            needleGrid[r * IdolWaterSurface.DEFAULT_COLS + midC] = 1f
        }
        val needleSurface = IdolWaterSurface.fromSolidGrid(IdolWaterSurface.DEFAULT_COLS, IdolWaterSurface.DEFAULT_ROWS, needleGrid)
        val emptySurface = IdolWaterSurface.fromSolidGrid(
            IdolWaterSurface.DEFAULT_COLS,
            IdolWaterSurface.DEFAULT_ROWS,
            FloatArray(IdolWaterSurface.DEFAULT_COLS * IdolWaterSurface.DEFAULT_ROWS)
        )
        val defaultPlacement = IdolPlacement(ShrineSelection(mapOf("shrine" to "marble", "idols" to "shiva")))
        AbhishekWaterPhysics.buildPlan(defaultPlacement, 0, needleSurface)
        AbhishekWaterPhysics.buildPlan(defaultPlacement, 0, emptySurface)

        // Load every bundled idol WebP identically to AssetImageCache.idolWaterSurface and verify every shrine/deity.
        val assetsDir = listOf(
            File("src/main/assets/shrine/idols"),
            File("app/src/main/assets/shrine/idols"),
            File("android/app/src/main/assets/shrine/idols")
        ).firstOrNull { it.isDirectory }
        assertNotNull("Bundled idol assets directory must exist for regression verification", assetsDir)

        fun decodeImage(file: File): java.awt.image.BufferedImage {
            ImageIO.read(file)?.let { return it }
            val tmpPng = File.createTempFile("idol_decode_", ".png")
            try {
                val decoded = listOf(
                    listOf("dwebp", "-quiet", file.absolutePath, "-o", tmpPng.absolutePath),
                    listOf("/opt/homebrew/bin/dwebp", "-quiet", file.absolutePath, "-o", tmpPng.absolutePath),
                    listOf("ffmpeg", "-y", "-v", "error", "-i", file.absolutePath, tmpPng.absolutePath),
                    listOf("/opt/homebrew/bin/ffmpeg", "-y", "-v", "error", "-i", file.absolutePath, tmpPng.absolutePath)
                ).any { cmd ->
                    runCatching {
                        ProcessBuilder(cmd).redirectErrorStream(true).start().waitFor() == 0 && tmpPng.length() > 0L
                    }.getOrDefault(false)
                }
                assertTrue("Unable to decode WebP idol asset ${file.path} on JVM", decoded)
                return requireNotNull(ImageIO.read(tmpPng)) { "Decoded PNG unreadable for ${file.path}" }
            } finally {
                tmpPng.delete()
            }
        }

        val idols = listOf("original", "ganesh_hanuman", "radha_krishna", "shiva", "lakshmi", "durga", "ram_darbar", "hanuman", "shanidev")
        val shrines = listOf("marble", "divya_darshan")
        for (idol in idols) {
            val file = listOf(File(assetsDir, "$idol.webp"), File(assetsDir, "$idol.png"))
                .firstOrNull { it.isFile } ?: File(assetsDir, "$idol.webp")
            assertTrue("Missing idol asset ${file.path}", file.isFile)
            val img = decodeImage(file)
            var left = img.width; var top = img.height; var right = 0; var bottom = 0
            for (y in 0 until img.height) {
                for (x in 0 until img.width) {
                    if ((img.getRGB(x, y) ushr 24) >= 24) {
                        if (x < left) left = x; if (x > right) right = x
                        if (y < top) top = y; if (y > bottom) bottom = y
                    }
                }
            }
            val tw = (right - left + 1).coerceAtLeast(1)
            val th = (bottom - top + 1).coerceAtLeast(1)
            val cols = IdolWaterSurface.DEFAULT_COLS
            val rows = IdolWaterSurface.DEFAULT_ROWS
            val solid = FloatArray(cols * rows)
            for (r in 0 until rows) {
                val py = top + (((r + 0.5f) / rows) * th).toInt().coerceIn(0, th - 1)
                for (c in 0 until cols) {
                    val px = left + (((c + 0.5f) / cols) * tw).toInt().coerceIn(0, tw - 1)
                    solid[r * cols + c] = if ((img.getRGB(px, py) ushr 24) >= 24) 1f else 0f
                }
            }
            val surface = IdolWaterSurface.fromSolidGrid(cols, rows, solid)
            for (shrine in shrines) {
                val selection = ShrineSelection(mapOf("shrine" to shrine, "idols" to idol))
                val placement = IdolPlacement(selection)
                for (deity in 0 until selection.deityCount) {
                    val plan = AbhishekWaterPhysics.buildPlan(placement, deity, surface)
                    assertEquals(10, plan.rivulets.size)
                    assertTrue("$idol/$shrine/$deity should have deflection points", plan.deflections.isNotEmpty())
                    assertTrue("$idol/$shrine/$deity should have deflected droplets", plan.droplets.isNotEmpty())
                    assertTrue("$idol/$shrine/$deity should have idol-confined beads", plan.residualBeads.isNotEmpty())
                }
            }
        }
    }
}

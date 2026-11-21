package com.worship.nityamandir.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import com.worship.nityamandir.engine.TemplePoint
import com.worship.nityamandir.engine.TempleViewport
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hardware-accelerated 3D glTF/GLB mesh & PBR Temple Brass renderer for the sacred Aarti lamps
 * (`shrine/aarti/golden.glb`, `shrine/aarti/traditional.glb`, and the 5-hooded `karpura` Nagkanya variant).
 *
 * Uses Android's native GPU `Canvas.drawVertices(VertexMode.TRIANGLES, ...)` pipeline with
 * per-vertex Gouraud PBR metallic-brass shading, embedded glTF texture albedo sampling,
 * O(N) counting-sort depth ordering, and exact 3D spout/wick projection.
 */
internal class GlbAartiMesh(
    val vertexCount: Int,
    val triangleCount: Int,
    // Canonical lamp-space vertex positions (lx, ly, lz), size = vertexCount * 3
    val localPositions: FloatArray,
    // Canonical lamp-space unit normals (nx, ny, nz), size = vertexCount * 3
    val localNormals: FloatArray,
    // Baked per-vertex base albedo RGB in 0..255 sampled from embedded GLB baseColorTexture, size = vertexCount * 3
    val baseRgb: IntArray,
    // Triangle vertex indices (i0, i1, i2), size = triangleCount * 3
    val indices: ShortArray,
    // Canonical lamp-space 3D coordinates of the wick spouts/tiers (wx, wy, wz)
    val canonicalWicks: List<FloatArray>
) {
    // Pre-allocated working buffers for zero-allocation 60 FPS rendering
    val screenVerts = FloatArray(vertexCount * 2)
    val viewZ = FloatArray(vertexCount)
    val vertexColors = IntArray(vertexCount)
    val triDepthBucket = IntArray(triangleCount)
    val bucketHeads = IntArray(256)
    val nextTriInBucket = IntArray(triangleCount)
    val sortedIndices = ShortArray(triangleCount * 3)
    val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
}

internal object GlbAartiModelCache {
    private val cache = ConcurrentHashMap<String, GlbAartiMesh>()

    fun getOrLoad(context: Context, aartiVariant: String): GlbAartiMesh {
        val key = aartiVariant.ifBlank { "original" }
        return cache.getOrPut(key) {
            runCatching { loadMeshForVariant(context, key) }
                .getOrElse { buildFallbackPanchAartiMesh() }
        }
    }

    private fun loadMeshForVariant(context: Context, variant: String): GlbAartiMesh {
        val isTraditional = variant == "traditional"
        val isKarpura = variant == "karpura"
        val assetPath = if (isTraditional) "shrine/aarti/traditional.glb" else "shrine/aarti/golden.glb"
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val magic = buf.int
        require(magic == 0x46546C67) { "Invalid GLB magic in $assetPath" }
        buf.int // version
        buf.int // total length

        val jsonChunkLen = buf.int
        val jsonChunkType = buf.int
        require(jsonChunkType == 0x4E4F534A) { "Expected JSON chunk in $assetPath" }
        val jsonBytes = ByteArray(jsonChunkLen)
        buf.get(jsonBytes)
        val gltf = JSONObject(String(jsonBytes, Charsets.UTF_8))

        val binChunkLen = buf.int
        val binChunkType = buf.int
        require(binChunkType == 0x004E4942) { "Expected BIN chunk in $assetPath" }
        val binStart = buf.position()

        val accessors = gltf.getJSONArray("accessors")
        val bufferViews = gltf.getJSONArray("bufferViews")
        val meshes = gltf.getJSONArray("meshes")
        val materials = gltf.optJSONArray("materials")
        val textures = gltf.optJSONArray("textures")
        val images = gltf.optJSONArray("images")

        // Decode embedded baseColorTexture bitmaps once per material
        val decodedImages = HashMap<Int, android.graphics.Bitmap>()
        fun bitmapForMaterial(matIdx: Int): android.graphics.Bitmap? {
            if (materials == null || textures == null || images == null) return null
            if (matIdx < 0 || matIdx >= materials.length()) return null
            val pbr = materials.getJSONObject(matIdx).optJSONObject("pbrMetallicRoughness") ?: return null
            val baseTex = pbr.optJSONObject("baseColorTexture") ?: return null
            val texIdx = baseTex.optInt("index", -1)
            if (texIdx < 0 || texIdx >= textures.length()) return null
            val imgIdx = textures.getJSONObject(texIdx).optInt("source", -1)
            if (imgIdx < 0 || imgIdx >= images.length()) return null
            return decodedImages.getOrPut(imgIdx) {
                val bvIdx = images.getJSONObject(imgIdx).getInt("bufferView")
                val bv = bufferViews.getJSONObject(bvIdx)
                val offset = binStart + bv.optInt("byteOffset", 0)
                val length = bv.getInt("byteLength")
                BitmapFactory.decodeByteArray(bytes, offset, length)
            }
        }

        // First pass: count vertices and triangles
        var totalVerts = 0
        var totalTris = 0
        for (m in 0 until meshes.length()) {
            val prims = meshes.getJSONObject(m).getJSONArray("primitives")
            for (p in 0 until prims.length()) {
                val prim = prims.getJSONObject(p)
                val posAcc = accessors.getJSONObject(prim.getJSONObject("attributes").getInt("POSITION"))
                val idxAcc = accessors.getJSONObject(prim.getInt("indices"))
                totalVerts += posAcc.getInt("count")
                totalTris += idxAcc.getInt("count") / 3
            }
        }

        // Extra procedural 3D 5-hooded Nagkanya cobra canopy vertices if variant == "karpura"
        val hoodRings = 10
        val hoodCols = 16
        val extraVerts = if (isKarpura) (hoodRings + 1) * (hoodCols + 1) else 0
        val extraTris = if (isKarpura) hoodRings * hoodCols * 2 else 0

        val rawPositions = FloatArray((totalVerts + extraVerts) * 3)
        val rawNormals = FloatArray((totalVerts + extraVerts) * 3)
        val baseRgb = IntArray((totalVerts + extraVerts) * 3)
        val indices = ShortArray((totalTris + extraTris) * 3)

        var minX = Float.POSITIVE_INFINITY; var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY; var maxZ = Float.NEGATIVE_INFINITY

        var vCursor = 0
        var iCursor = 0

        for (m in 0 until meshes.length()) {
            val prims = meshes.getJSONObject(m).getJSONArray("primitives")
            for (p in 0 until prims.length()) {
                val prim = prims.getJSONObject(p)
                val attrs = prim.getJSONObject("attributes")
                val posAcc = accessors.getJSONObject(attrs.getInt("POSITION"))
                val posBv = bufferViews.getJSONObject(posAcc.getInt("bufferView"))
                val posOff = binStart + posBv.optInt("byteOffset", 0) + posAcc.optInt("byteOffset", 0)
                val posStride = posBv.optInt("byteStride", 12)
                val vCount = posAcc.getInt("count")

                val nrmAcc = if (attrs.has("NORMAL")) accessors.getJSONObject(attrs.getInt("NORMAL")) else null
                val nrmBv = nrmAcc?.let { bufferViews.getJSONObject(it.getInt("bufferView")) }
                val nrmOff = if (nrmAcc != null && nrmBv != null) {
                    binStart + nrmBv.optInt("byteOffset", 0) + nrmAcc.optInt("byteOffset", 0)
                } else -1
                val nrmStride = nrmBv?.optInt("byteStride", 12) ?: 12

                val uvAcc = if (attrs.has("TEXCOORD_0")) accessors.getJSONObject(attrs.getInt("TEXCOORD_0")) else null
                val uvBv = uvAcc?.let { bufferViews.getJSONObject(it.getInt("bufferView")) }
                val uvOff = if (uvAcc != null && uvBv != null) {
                    binStart + uvBv.optInt("byteOffset", 0) + uvAcc.optInt("byteOffset", 0)
                } else -1
                val uvStride = uvBv?.optInt("byteStride", 8) ?: 8

                val colAcc = if (attrs.has("COLOR_0")) accessors.getJSONObject(attrs.getInt("COLOR_0")) else null
                val colBv = colAcc?.let { bufferViews.getJSONObject(it.getInt("bufferView")) }
                val colOff = if (colAcc != null && colBv != null) {
                    binStart + colBv.optInt("byteOffset", 0) + colAcc.optInt("byteOffset", 0)
                } else -1
                val colCompType = colAcc?.optInt("componentType", 5121) ?: 5121
                val colComponents = if (colAcc?.optString("type", "VEC4") == "VEC3") 3 else 4
                val colElemBytes = when (colCompType) {
                    5126 -> 4
                    5123 -> 2
                    else -> 1
                }
                val colStride = colBv?.optInt("byteStride", colComponents * colElemBytes)
                    ?: (colComponents * colElemBytes)

                val bmp = if (colOff < 0) bitmapForMaterial(prim.optInt("material", 0)) else null
                val bmpW = bmp?.width ?: 0
                val bmpH = bmp?.height ?: 0

                for (i in 0 until vCount) {
                    val po = posOff + i * posStride
                    val vx = buf.getFloat(po)
                    val vy = buf.getFloat(po + 4)
                    val vz = buf.getFloat(po + 8)
                    val dst = (vCursor + i) * 3
                    rawPositions[dst] = vx
                    rawPositions[dst + 1] = vy
                    rawPositions[dst + 2] = vz
                    if (vx < minX) minX = vx; if (vx > maxX) maxX = vx
                    if (vy < minY) minY = vy; if (vy > maxY) maxY = vy
                    if (vz < minZ) minZ = vz; if (vz > maxZ) maxZ = vz

                    if (nrmOff >= 0) {
                        val no = nrmOff + i * nrmStride
                        rawNormals[dst] = buf.getFloat(no)
                        rawNormals[dst + 1] = buf.getFloat(no + 4)
                        rawNormals[dst + 2] = buf.getFloat(no + 8)
                    } else {
                        rawNormals[dst] = 0f; rawNormals[dst + 1] = 1f; rawNormals[dst + 2] = 0f
                    }

                    // Sample pre-baked COLOR_0 or embedded baseColorTexture and blend with sacred Temple Brass (#D8AF52)
                    var tr = 224; var tg = 174; var tb = 76
                    var sampled = false
                    var pr = 0; var pg = 0; var pb = 0
                    if (colOff >= 0) {
                        val co = colOff + i * colStride
                        when (colCompType) {
                            5121 -> {
                                pr = buf.get(co).toInt() and 0xFF
                                pg = buf.get(co + 1).toInt() and 0xFF
                                pb = buf.get(co + 2).toInt() and 0xFF
                            }
                            5123 -> {
                                pr = ((buf.getShort(co).toInt() and 0xFFFF) * 255) / 65535
                                pg = ((buf.getShort(co + 2).toInt() and 0xFFFF) * 255) / 65535
                                pb = ((buf.getShort(co + 4).toInt() and 0xFFFF) * 255) / 65535
                            }
                            else -> {
                                pr = (buf.getFloat(co) * 255f).toInt().coerceIn(0, 255)
                                pg = (buf.getFloat(co + 4) * 255f).toInt().coerceIn(0, 255)
                                pb = (buf.getFloat(co + 8) * 255f).toInt().coerceIn(0, 255)
                            }
                        }
                        sampled = true
                    } else if (bmp != null && uvOff >= 0 && bmpW > 0 && bmpH > 0) {
                        val uo = uvOff + i * uvStride
                        val u = buf.getFloat(uo)
                        val v = buf.getFloat(uo + 4)
                        val tx = (((u % 1f) + 1f) % 1f * (bmpW - 1)).toInt().coerceIn(0, bmpW - 1)
                        val ty = (((v % 1f) + 1f) % 1f * (bmpH - 1)).toInt().coerceIn(0, bmpH - 1)
                        val px = bmp.getPixel(tx, ty)
                        pr = (px shr 16) and 0xFF
                        pg = (px shr 8) and 0xFF
                        pb = px and 0xFF
                        sampled = true
                    }
                    if (sampled) {
                        val luma = (pr * 0.35f + pg * 0.50f + pb * 0.15f) / 255f
                        val shade = (0.58f + 0.52f * luma).coerceIn(0.48f, 1.12f)
                        tr = (228f * shade).toInt().coerceIn(0, 255)
                        tg = (176f * shade).toInt().coerceIn(0, 255)
                        tb = (74f * shade).toInt().coerceIn(0, 255)
                    }
                    if (isKarpura) {
                        // Slightly richer antique Panchdhatu gold-bronze tone for Nagkanya Karpura Aarti
                        tr = (tr * 0.96f).toInt().coerceIn(0, 255)
                        tg = (tg * 0.91f).toInt().coerceIn(0, 255)
                        tb = (tb * 0.82f).toInt().coerceIn(0, 255)
                    }
                    baseRgb[dst] = tr
                    baseRgb[dst + 1] = tg
                    baseRgb[dst + 2] = tb
                }

                val idxAcc = accessors.getJSONObject(prim.getInt("indices"))
                val idxBv = bufferViews.getJSONObject(idxAcc.getInt("bufferView"))
                val idxOff = binStart + idxBv.optInt("byteOffset", 0) + idxAcc.optInt("byteOffset", 0)
                val idxCount = idxAcc.getInt("count")
                val cType = idxAcc.getInt("componentType")
                for (i in 0 until idxCount) {
                    val localIdx = when (cType) {
                        5123 -> buf.getShort(idxOff + i * 2).toInt() and 0xFFFF
                        5125 -> buf.getInt(idxOff + i * 4)
                        else -> buf.get(idxOff + i).toInt() and 0xFF
                    }
                    indices[iCursor++] = (vCursor + localIdx).toShort()
                }
                vCursor += vCount
            }
        }
        decodedImages.values.forEach { it.recycle() }

        // If Karpura (Nagkanya Panch-Aarti), append a sculpted 3D 5-hooded Sheshnag canopy rising behind the bowl rim
        if (isKarpura) {
            val baseV = vCursor
            for (r in 0..hoodRings) {
                val vFrac = r.toFloat() / hoodRings.toFloat() // 0 = base behind bowl, 1 = 5-cobra crown
                val y = 0.28f + vFrac * 0.72f
                val archX = 0.08f - sin(vFrac * PI.toFloat() * 0.85f) * 0.22f
                val halfSpread = 0.14f + sin(vFrac * PI.toFloat() * 0.78f) * 0.34f
                for (c in 0..hoodCols) {
                    val uFrac = c.toFloat() / hoodCols.toFloat() // 0..1 across width
                    val z = (uFrac - 0.5f) * 2f * halfSpread
                    // 5 distinct cobra hoods along the top crest
                    val cobraWave = if (vFrac > 0.45f) {
                        sin(uFrac * 5f * PI.toFloat()).pow(2) * 0.085f * ((vFrac - 0.45f) / 0.55f)
                    } else 0f
                    val curlX = archX - cobraWave * 0.6f
                    val vy = y + cobraWave
                    val dst = vCursor * 3
                    rawPositions[dst] = curlX
                    rawPositions[dst + 1] = vy
                    rawPositions[dst + 2] = z

                    val nx = -0.85f
                    val ny = 0.32f * cos(vFrac * PI.toFloat() * 0.7f)
                    val nz = (uFrac - 0.5f) * 0.65f
                    val nl = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-4f)
                    rawNormals[dst] = nx / nl
                    rawNormals[dst + 1] = ny / nl
                    rawNormals[dst + 2] = nz / nl

                    val ridge = 0.88f + 0.12f * sin(uFrac * 5f * PI.toFloat())
                    baseRgb[dst] = (218f * ridge).toInt().coerceIn(0, 255)
                    baseRgb[dst + 1] = (166f * ridge).toInt().coerceIn(0, 255)
                    baseRgb[dst + 2] = (64f * ridge).toInt().coerceIn(0, 255)
                    vCursor++
                }
            }
            val stride = hoodCols + 1
            for (r in 0 until hoodRings) {
                for (c in 0 until hoodCols) {
                    val i00 = (baseV + r * stride + c).toShort()
                    val i10 = (baseV + (r + 1) * stride + c).toShort()
                    val i01 = (baseV + r * stride + c + 1).toShort()
                    val i11 = (baseV + (r + 1) * stride + c + 1).toShort()
                    indices[iCursor++] = i00
                    indices[iCursor++] = i10
                    indices[iCursor++] = i11
                    indices[iCursor++] = i00
                    indices[iCursor++] = i11
                    indices[iCursor++] = i01
                }
            }
        }

        // Convert raw coordinates into Canonical Lamp Space:
        // Pivot around the center of the brass diya bowl / pagoda stem so 3D rotation orbits cleanly.
        // In canonical space:
        //   +lx = right across spouts
        //   +ly = vertical UP (pedestal to rim)
        //   -lz = front spouts (pointing into the sanctum toward deities)
        //   +lz = curved handle (pointing toward the devotee)
        val span = max(maxX - minX, max(maxY - minY, maxZ - minZ)).coerceAtLeast(1e-4f)
        val pivotX = if (isTraditional) -0.8f else -0.22f
        val pivotY = if (isTraditional) 1.8f else 0.05f
        val pivotZ = 0.0f
        val normScale = if (isTraditional) 1.62f else 1.42f

        val canonicalPos = FloatArray(vCursor * 3)
        val canonicalNrm = FloatArray(vCursor * 3)

        fun toCanonicalPos(vx: Float, vy: Float, vz: Float): FloatArray {
            val rx = (vx - pivotX) / span * normScale
            val ry = (vy - pivotY) / span * normScale
            val rz = (vz - pivotZ) / span * normScale
            return if (isTraditional) {
                // In traditional.glb, -X is handle (+lz), +X is pagoda front (-lz)
                floatArrayOf(rz, ry, -rx)
            } else {
                // In golden.glb, +X is handle (+lz), -X is 5-spouted front (-lz)
                floatArrayOf(-rz, ry, rx)
            }
        }

        for (i in 0 until vCursor) {
            val idx = i * 3
            val vx = rawPositions[idx]
            val vy = rawPositions[idx + 1]
            val vz = rawPositions[idx + 2]
            val cp = toCanonicalPos(vx, vy, vz)
            canonicalPos[idx] = cp[0]
            canonicalPos[idx + 1] = cp[1]
            canonicalPos[idx + 2] = cp[2]

            val nx = rawNormals[idx]
            val ny = rawNormals[idx + 1]
            val nz = rawNormals[idx + 2]
            val nl = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-4f)
            val unx = nx / nl; val uny = ny / nl; val unz = nz / nl
            if (isTraditional) {
                canonicalNrm[idx] = unz
                canonicalNrm[idx + 1] = uny
                canonicalNrm[idx + 2] = -unx
            } else {
                canonicalNrm[idx] = -unz
                canonicalNrm[idx + 1] = uny
                canonicalNrm[idx + 2] = unx
            }
        }

        // Exact 3D spout/tier coordinates in raw GLB space converted to canonical Lamp Space
        val rawWicks = if (isTraditional) {
            listOf(
                floatArrayOf(1.8f, 1.15f, -4.55f),  // Lower left tier spout
                floatArrayOf(2.1f, 4.95f, -2.75f),  // Upper left tier spout
                floatArrayOf(0.0f, 8.92f, 0.00f),   // Top pagoda crown diya
                floatArrayOf(2.1f, 4.95f, 2.75f),   // Upper right tier spout
                floatArrayOf(1.8f, 1.15f, 4.55f)    // Lower right tier spout
            )
        } else {
            // The 5 sculpted spouts on the front rim of golden.glb
            listOf(
                floatArrayOf(-0.76f, 0.51f, 0.55f),  // Left outer spout
                floatArrayOf(-0.92f, 0.52f, 0.22f),  // Left inner spout
                floatArrayOf(-0.86f, 0.52f, 0.00f),  // Center spout
                floatArrayOf(-0.92f, 0.52f, -0.22f), // Right inner spout
                floatArrayOf(-0.76f, 0.51f, -0.55f)  // Right outer spout
            )
        }
        val canonicalWicks = rawWicks.map { w -> toCanonicalPos(w[0], w[1], w[2]) }

        return GlbAartiMesh(
            vertexCount = vCursor,
            triangleCount = iCursor / 3,
            localPositions = canonicalPos,
            localNormals = canonicalNrm,
            baseRgb = baseRgb,
            indices = indices,
            canonicalWicks = canonicalWicks
        )
    }

    private fun buildFallbackPanchAartiMesh(): GlbAartiMesh {
        // Lightweight procedural 3D Panch-Aarti bowl fallback if asset stream is unavailable
        val rings = 8; val sectors = 16
        val vCount = (rings + 1) * (sectors + 1)
        val tCount = rings * sectors * 2
        val pos = FloatArray(vCount * 3)
        val nrm = FloatArray(vCount * 3)
        val rgb = IntArray(vCount * 3)
        val idx = ShortArray(tCount * 3)
        var vc = 0
        for (r in 0..rings) {
            val rf = r.toFloat() / rings
            val rad = 0.06f + rf * 0.36f
            val y = -0.18f + rf * rf * 0.34f
            for (s in 0..sectors) {
                val ang = s.toFloat() / sectors * 2f * PI.toFloat()
                val x = cos(ang) * rad
                val z = sin(ang) * rad
                val o = vc * 3
                pos[o] = x; pos[o + 1] = y; pos[o + 2] = z
                nrm[o] = cos(ang) * 0.5f; nrm[o + 1] = 0.86f; nrm[o + 2] = sin(ang) * 0.5f
                rgb[o] = 220; rgb[o + 1] = 172; rgb[o + 2] = 72
                vc++
            }
        }
        var ic = 0
        val stride = sectors + 1
        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val i00 = (r * stride + s).toShort()
                val i10 = ((r + 1) * stride + s).toShort()
                val i01 = (r * stride + s + 1).toShort()
                val i11 = ((r + 1) * stride + s + 1).toShort()
                idx[ic++] = i00; idx[ic++] = i10; idx[ic++] = i11
                idx[ic++] = i00; idx[ic++] = i11; idx[ic++] = i01
            }
        }
        val wicks = listOf(
            floatArrayOf(-0.34f, 0.22f, -0.32f),
            floatArrayOf(-0.16f, 0.22f, -0.44f),
            floatArrayOf(0.00f, 0.22f, -0.42f),
            floatArrayOf(0.16f, 0.22f, -0.44f),
            floatArrayOf(0.34f, 0.22f, -0.32f)
        )
        return GlbAartiMesh(vCount, tCount, pos, nrm, rgb, idx, wicks)
    }
}

/**
 * Projects the 3D canonical wick positions of the loaded `.glb` Aarti model into normalized
 * `TemplePoint` coordinates using the exact same 3D orientation (`yawDeg`, `pitchDeg`, `rollDeg`)
 * and 3D perspective camera as [GlbAarti3DModelLayer].
 */
internal fun projectAarti3DWicks(
    mesh: GlbAartiMesh,
    center: TemplePoint,
    widthFraction: Float,
    yawDeg: Float,
    pitchDeg: Float,
    rollDeg: Float
): List<TemplePoint> {
    val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()
    val roll = Math.toRadians(rollDeg.toDouble()).toFloat()
    val cy = cos(yaw); val sy = sin(yaw)
    val cp = cos(pitch); val sp = sin(pitch)
    val cr = cos(roll); val sr = sin(roll)
    val halfSpan = widthFraction * 0.62f

    return mesh.canonicalWicks.map { w ->
        val lx = w[0]; val ly = w[1]; val lz = w[2]
        // 1. Yaw around Y
        val x1 = lx * cy + lz * sy
        val z1 = -lx * sy + lz * cy
        // 2. Pitch around X (tilt forward toward viewer)
        val y2 = ly * cp - z1 * sp
        val z2 = ly * sp + z1 * cp
        // 3. Roll around Z
        val x3 = x1 * cr - y2 * sr
        val y3 = x1 * sr + y2 * cr
        // Perspective projection matching GlbAarti3DModelLayer
        val persp = 3.0f / (3.0f - z2).coerceAtLeast(1.2f)
        TemplePoint(
            x = center.x + x3 * persp * halfSpan,
            y = center.y - y3 * persp * halfSpan
        )
    }
}

/**
 * Renders the 3D `.glb` Aarti lamp (`golden.glb`, `traditional.glb`, or 5-hooded `karpura`)
 * with real-time 3D rotations (`yawDeg`, `pitchDeg`, `rollDeg`), perspective projection,
 * and dynamic PBR Temple Brass lighting.
 */
@Composable
internal fun GlbAarti3DModelLayer(
    mesh: GlbAartiMesh,
    center: TemplePoint,
    widthFraction: Float,
    viewport: TempleViewport,
    yawDeg: Float,
    pitchDeg: Float,
    rollDeg: Float,
    lit: Boolean,
    aartiRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize()) {
        val centerPx = viewport.pixel(center)
        val halfSpanPx = viewport.imageWidth * widthFraction * 0.62f
        val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val roll = Math.toRadians(rollDeg.toDouble()).toFloat()
        val cy = cos(yaw); val sy = sin(yaw)
        val cp = cos(pitch); val sp = sin(pitch)
        val cr = cos(roll); val sr = sin(roll)

        val vCount = mesh.vertexCount
        val pos = mesh.localPositions
        val nrm = mesh.localNormals
        val rgb = mesh.baseRgb
        val sVerts = mesh.screenVerts
        val vZ = mesh.viewZ
        val vColors = mesh.vertexColors

        // Primary sanctum key light (upper-left/front) + warm diya flame light from the spouts above
        val flameBoost = if (lit) (if (aartiRunning) 0.42f else 0.28f) else 0.0f
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (i in 0 until vCount) {
            val o = i * 3
            val lx = pos[o]; val ly = pos[o + 1]; val lz = pos[o + 2]
            val lnx = nrm[o]; val lny = nrm[o + 1]; val lnz = nrm[o + 2]

            // 1. Yaw around Y
            val x1 = lx * cy + lz * sy
            val z1 = -lx * sy + lz * cy
            val nx1 = lnx * cy + lnz * sy
            val nz1 = -lnx * sy + lnz * cy

            // 2. Pitch around X
            val y2 = ly * cp - z1 * sp
            val z2 = ly * sp + z1 * cp
            val ny2 = lny * cp - nz1 * sp
            val nz2 = lny * sp + nz1 * cp

            // 3. Roll around Z
            val x3 = x1 * cr - y2 * sr
            val y3 = x1 * sr + y2 * cr
            var nx3 = nx1 * cr - ny2 * sr
            var ny3 = nx1 * sr + ny2 * cr
            var nz3 = nz2

            // Double-sided brass rim & interior bowl normal flip when viewing the inside of the bowl
            if (nz3 < -0.15f && ly > -0.05f) {
                nx3 = -nx3; ny3 = -ny3; nz3 = -nz3
            }

            val persp = 3.0f / (3.0f - z2).coerceAtLeast(1.2f)
            val sv = i * 2
            sVerts[sv] = centerPx.x + x3 * persp * halfSpanPx
            sVerts[sv + 1] = centerPx.y - y3 * persp * halfSpanPx
            vZ[i] = z2
            if (z2 < minZ) minZ = z2
            if (z2 > maxZ) maxZ = z2

            // PBR Temple Brass Lighting:
            // Key light from (-0.28, +0.66, +0.70), Half-vector around (-0.14, +0.42, +0.90)
            val nDotL = max(0f, nx3 * -0.28f + ny3 * 0.66f + nz3 * 0.70f)
            val rimBounce = max(0f, -ny3 * 0.45f + nz3 * 0.35f) * 0.22f
            val hDot = max(0f, nx3 * -0.14f + ny3 * 0.42f + nz3 * 0.90f)
            val h2 = hDot * hDot
            val h4 = h2 * h2
            val spec = h4 * h4 * h2 // hDot^10 metallic sheen

            // Warm point-light glow on the upper bowl and spouts when lit
            val spoutProximity = if (ly > 0.0f && lz < 0.15f) ((ly + 0.08f) * 1.4f).coerceIn(0f, 1f) else 0f
            val flameLight = flameBoost * spoutProximity * max(0.25f, ny3 * 0.7f + nz3 * 0.5f)

            val litFactor = (0.34f + 0.68f * nDotL + rimBounce + flameLight).coerceIn(0.24f, 1.35f)
            val specAddR = (spec * 155f + flameLight * 55f).toInt()
            val specAddG = (spec * 135f + flameLight * 32f).toInt()
            val specAddB = (spec * 78f).toInt()

            val r = min(255, (rgb[o] * litFactor).toInt() + specAddR)
            val g = min(255, (rgb[o + 1] * litFactor).toInt() + specAddG)
            val b = min(255, (rgb[o + 2] * litFactor).toInt() + specAddB)
            vColors[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        // O(N) 256-bucket counting depth sort (back-to-front painter's order)
        val heads = mesh.bucketHeads
        java.util.Arrays.fill(heads, -1)
        val next = mesh.nextTriInBucket
        val triIdx = mesh.indices
        val tCount = mesh.triangleCount
        val zRange = (maxZ - minZ).coerceAtLeast(1e-4f)

        for (t in 0 until tCount) {
            val base = t * 3
            val i0 = triIdx[base].toInt() and 0xFFFF
            val i1 = triIdx[base + 1].toInt() and 0xFFFF
            val i2 = triIdx[base + 2].toInt() and 0xFFFF
            val avgZ = (vZ[i0] + vZ[i1] + vZ[i2]) * 0.33333334f
            val bucket = (((avgZ - minZ) / zRange) * 255f).toInt().coerceIn(0, 255)
            next[t] = heads[bucket]
            heads[bucket] = t
        }

        val outIndices = mesh.sortedIndices
        var outCursor = 0
        for (b in 0..255) {
            var t = heads[b]
            while (t != -1) {
                val base = t * 3
                outIndices[outCursor++] = triIdx[base]
                outIndices[outCursor++] = triIdx[base + 1]
                outIndices[outCursor++] = triIdx[base + 2]
                t = next[t]
            }
        }

        drawContext.canvas.nativeCanvas.drawVertices(
            android.graphics.Canvas.VertexMode.TRIANGLES,
            vCount * 2,
            sVerts,
            0,
            null,
            0,
            vColors,
            0,
            outIndices,
            0,
            outCursor,
            mesh.meshPaint
        )
    }
}

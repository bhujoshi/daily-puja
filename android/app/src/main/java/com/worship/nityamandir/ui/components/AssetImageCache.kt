package com.worship.nityamandir.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.worship.nityamandir.engine.IdolWaterSurface

/** Shared in-memory cache and downsampler for bundled shrine and ritual assets. */
internal object AssetImageCache {
    private val cache = object : LruCache<String, Bitmap>(
        minOf((Runtime.getRuntime().maxMemory() / 8L).toInt(), 24 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val waterSurfaces = mutableMapOf<String, IdolWaterSurface>()

    fun bitmap(context: Context, path: String, sampleSize: Int = 1): Bitmap {
        val safeSample = sampleSize.coerceAtLeast(1)
        val key = "$path@$safeSample"
        synchronized(cache) {
            cache.get(key)?.let { return it }
        }
        val bytes = context.assets.open(path).use { it.readBytes() }
        val options = BitmapFactory.Options().apply {
            inSampleSize = safeSample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        synchronized(cache) {
            cache.put(key, decoded)
        }
        return decoded
    }

    fun image(context: Context, path: String, sampleSize: Int = 1): ImageBitmap =
        bitmap(context, path, sampleSize).asImageBitmap()

    fun trimmedIdol(context: Context, path: String): ImageBitmap =
        trimmedIdolBitmap(context, path).asImageBitmap()

    private fun trimmedIdolBitmap(context: Context, path: String): Bitmap {
        val key = "trimmed:$path"
        synchronized(cache) {
            cache.get(key)?.let { return it }
        }
        val source = bitmap(context, path)
        var left = source.width
        var top = source.height
        var right = 0
        var bottom = 0
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        pixels.forEachIndexed { i, color ->
            // Ignore near-zero alpha noise (alpha < 24) around AI-extracted sprites so
            // bounding boxes match true visible geometry in IdolPlacement.
            if ((color ushr 24) >= 24) {
                val x = i % source.width
                val y = i / source.width
                left = minOf(left, x)
                right = maxOf(right, x)
                top = minOf(top, y)
                bottom = maxOf(bottom, y)
            }
        }
        val trimmed = if (left <= right && top <= bottom) {
            Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
        } else {
            source
        }
        synchronized(cache) {
            cache.put(key, trimmed)
        }
        return trimmed
    }

    /** Extracts and caches a compact silhouette & outward-normal grid from the trimmed idol bitmap. */
    fun idolWaterSurface(context: Context, path: String): IdolWaterSurface {
        synchronized(waterSurfaces) {
            waterSurfaces[path]?.let { return it }
        }
        val trimmed = trimmedIdolBitmap(context, path)
        val cols = IdolWaterSurface.DEFAULT_COLS
        val rows = IdolWaterSurface.DEFAULT_ROWS
        val solid = FloatArray(cols * rows)
        val tw = trimmed.width.coerceAtLeast(1)
        val th = trimmed.height.coerceAtLeast(1)
        for (r in 0 until rows) {
            val py = (((r + 0.5f) / rows.toFloat()) * th).toInt().coerceIn(0, th - 1)
            for (c in 0 until cols) {
                val px = (((c + 0.5f) / cols.toFloat()) * tw).toInt().coerceIn(0, tw - 1)
                val alpha = trimmed.getPixel(px, py) ushr 24
                solid[r * cols + c] = if (alpha >= 24) 1f else 0f
            }
        }
        val surface = IdolWaterSurface.fromSolidGrid(cols, rows, solid)
        synchronized(waterSurfaces) {
            waterSurfaces[path] = surface
        }
        return surface
    }
}

@Composable
internal fun rememberCachedAssetImage(path: String, sampleSize: Int = 1): ImageBitmap {
    val context = LocalContext.current
    return remember(path, sampleSize) {
        AssetImageCache.image(context, path, sampleSize)
    }
}

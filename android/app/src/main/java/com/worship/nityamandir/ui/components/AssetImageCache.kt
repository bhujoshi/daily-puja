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

/** Shared in-memory cache and downsampler for bundled shrine and ritual assets. */
internal object AssetImageCache {
    private val cache = object : LruCache<String, Bitmap>(
        minOf((Runtime.getRuntime().maxMemory() / 8L).toInt(), 24 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

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

    fun trimmedIdol(context: Context, path: String): ImageBitmap {
        val key = "trimmed:$path"
        synchronized(cache) {
            cache.get(key)?.let { return it.asImageBitmap() }
        }
        val source = bitmap(context, path)
        var left = source.width
        var top = source.height
        var right = 0
        var bottom = 0
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        pixels.forEachIndexed { i, color ->
            if ((color ushr 24) > 0) {
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
        return trimmed.asImageBitmap()
    }
}

@Composable
internal fun rememberCachedAssetImage(path: String, sampleSize: Int = 1): ImageBitmap {
    val context = LocalContext.current
    return remember(path, sampleSize) {
        AssetImageCache.image(context, path, sampleSize)
    }
}

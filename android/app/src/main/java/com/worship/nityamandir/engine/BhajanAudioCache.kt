package com.worship.nityamandir.engine

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/** One cache per process. Initialize on IO; opening its database can touch disk. */
@android.annotation.SuppressLint("UnsafeOptInUsageError")
internal object BhajanAudioCache {
    private const val MAX_BYTES = 200L * 1024 * 1024
    private var cache: SimpleCache? = null

    @Synchronized fun source(context: Context): CacheDataSource.Factory {
        val shared = cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "bhajan-audio"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext)
        ).also { cache = it }
        return CacheDataSource.Factory()
            .setCache(shared)
            .setUpstreamDataSourceFactory(httpSource())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun httpSource() = DefaultHttpDataSource.Factory()
        .setUserAgent("PavitraMandir/1.0 music")
        .setConnectTimeoutMs(8_000)
        .setReadTimeoutMs(8_000)
}

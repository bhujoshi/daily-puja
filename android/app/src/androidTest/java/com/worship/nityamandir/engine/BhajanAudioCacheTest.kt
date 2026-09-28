package com.worship.nityamandir.engine

import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@android.annotation.SuppressLint("UnsafeOptInUsageError")
class BhajanAudioCacheTest {
    private class Origin : AutoCloseable {
        val bytes = ByteArray(64 * 1024) { (it % 251).toByte() }
        val requests = AtomicInteger()
        private val socket = ServerSocket(0)
        val url = "http://127.0.0.1:${socket.localPort}/track"
        private val worker = thread(isDaemon = true) {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { client ->
                        client.soTimeout = 3000
                        val reader = client.getInputStream().bufferedReader()
                        val lines = mutableListOf<String>()
                        while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break; lines.add(line) }
                        val range = lines.firstOrNull { it.startsWith("Range:", true) }
                            ?.substringAfter("bytes=")?.split('-')
                        val start = range?.firstOrNull()?.toIntOrNull() ?: 0
                        val end = range?.getOrNull(1)?.toIntOrNull()?.coerceAtMost(bytes.lastIndex) ?: bytes.lastIndex
                        val header = if (range == null) "HTTP/1.1 200 OK\r\n" else
                            "HTTP/1.1 206 Partial Content\r\nContent-Range: bytes $start-$end/${bytes.size}\r\n"
                        requests.incrementAndGet()
                        client.getOutputStream().apply {
                            write((header + "Content-Length: ${end - start + 1}\r\nConnection: close\r\n\r\n").toByteArray())
                            write(bytes, start, end - start + 1); flush()
                        }
                    }
                } catch (_: Exception) { if (!socket.isClosed) throw AssertionError("Origin failed") }
            }
        }
        override fun close() { socket.close(); worker.join(3000) }
    }
    private fun read(source: CacheDataSource.Factory, spec: DataSpec): ByteArray {
        val data = source.createDataSource()
        val output = ByteArrayOutputStream()
        try {
            data.open(spec)
            val buffer = ByteArray(4096)
            while (true) { val count = data.read(buffer, 0, buffer.size); if (count == C.RESULT_END_OF_INPUT) break; output.write(buffer, 0, count) }
        } finally { data.close() }
        return output.toByteArray()
    }
    @Test fun replayUsesDiskAfterOriginGoesOffline() {
        val origin = Origin()
        val source = BhajanAudioCache.source(InstrumentationRegistry.getInstrumentation().targetContext)
        val spec = DataSpec.Builder().setUri(origin.url).build()
        try {
            assertArrayEquals(origin.bytes, read(source, spec))
            assertEquals(1, origin.requests.get())
            origin.close()
            assertArrayEquals(origin.bytes, read(source, spec))
            assertEquals(1, origin.requests.get())
        } finally { origin.close() }
    }
    @Test fun preloadedPrefixCanBeReadOffline() {
        val origin = Origin()
        val source = BhajanAudioCache.source(InstrumentationRegistry.getInstrumentation().targetContext)
        val spec = DataSpec.Builder().setUri(origin.url).setLength(8192).build()
        try {
            CacheWriter(source.createDataSource(), spec, null, null).cache()
            origin.close()
            assertArrayEquals(origin.bytes.copyOfRange(0,8192), read(source, spec))
            assertEquals(1, origin.requests.get())
        } finally { origin.close() }
    }
}

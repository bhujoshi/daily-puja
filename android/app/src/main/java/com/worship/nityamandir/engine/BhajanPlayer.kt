package com.worship.nityamandir.engine

import android.content.Context
import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.*
import androidx.media3.common.*
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.worship.nityamandir.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.pow

/** UI controller for the service-owned playlist and system media controls. */
@android.annotation.SuppressLint("UnsafeOptInUsageError")
class BhajanPlayer(context: Context) {
    data class Track(val title: String, val url: String, val id: String = url,
        val order: Int = 0, val collection: String = "", val deity: String = "",
        val thumbnail: String = "shrine/idols/original.png", val artist: String = "",
        val bitrateKbps: Int = 0, val durationSeconds: Long = 0, val volumeGainDb: Float = 0f)

    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = app.getSharedPreferences("music", Context.MODE_PRIVATE)
    private val base = BuildConfig.ACCOUNT_API_URL.trimEnd('/')
    private val catalogFile = File(app.filesDir, "bhajan-catalog-${base.hashCode()}.json")
    private val ready = CompletableDeferred<MediaController>()
    private var player: MediaController? = null
    private var cacheSource: CacheDataSource.Factory? = null
    private var catalogJob: Job? = null
    private var startJob: Job? = null
    private var prefetchJob: Job? = null
    @Volatile private var prefetchWriter: CacheWriter? = null
    private var released = false
    private var failedIDs = mutableSetOf<String>()
    private var requestedAt = 0L
    private var bufferingAt = 0L
    private var firstFrameReported = false
    private var catalogFetched = false

    private val ganeshAarti = Track(
            title = "Jai Ganesh Jai Ganesh Deva",
            url = "https://archive.org/download/JaiGaneshJaiGaneshJaiGaneshDevaLordGaneshAarti/" +
                "Jai%20Ganesh%20Jai%20Ganesh%20Jai%20Ganesh%20Deva%20-%20Lord%20Ganesh%20Aarti.mp3",
            id = "jai_ganesh_deva",
            collection = "ganesh", thumbnail = "shrine/idols/ganesh_hanuman.png", deity = "ganesh"
        )

    var catalog by mutableStateOf<List<Track>>(listOf(ganeshAarti)); private set
    var catalogLoading by mutableStateOf(false); private set
    var catalogError by mutableStateOf(false); private set
    var shuffle by mutableStateOf(preferences.getBoolean("shuffle", true)); private set
    var idol = "original"; private set
    var title by mutableStateOf("Choose a bhajan"); private set
    var playing by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var repeat by mutableStateOf(false); private set
    var tracks by mutableStateOf<List<Track>>(emptyList()); private set
    var current by mutableStateOf<Track?>(null); private set
    var notice by mutableStateOf(""); private set

    private val listener = object : Player.Listener {
        override fun onEvents(native: Player, events: Player.Events) {
            playing = native.isPlaying
            loading = native.playbackState == Player.STATE_BUFFERING
            syncProgress()
            if (loading && bufferingAt == 0L) bufferingAt = SystemClock.elapsedRealtime()
            if (!loading && bufferingAt != 0L) {
                Log.d("BhajanPlayback", "buffer_ms=${SystemClock.elapsedRealtime() - bufferingAt}")
                bufferingAt = 0L
            }
            if (playing && !firstFrameReported) {
                firstFrameReported = true
                Log.d("BhajanPlayback", "track=${current?.id} startup_ms=${SystemClock.elapsedRealtime() - requestedAt}")
                prefetchNext()
            }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            cancelPrefetch()
            current = tracks.firstOrNull { it.id == mediaItem?.mediaId }
            title = current?.title ?: "Choose a bhajan"
            position = 0; failed = false; firstFrameReported = false
            requestedAt = SystemClock.elapsedRealtime()
            player?.volume = 10.0.pow((current?.volumeGainDb ?: 0f).coerceIn(-12f, 0f) / 20.0).toFloat()
        }
        override fun onPlayerError(error: PlaybackException) {
            Log.w("BhajanPlayback", "track=${current?.id} error=${error.errorCodeName}")
            loading = false; playing = false; failed = true
            current?.let { failedIDs.add(it.id) }
            // Bound recovery to one pass through the queue; never loop on broken sources.
            if (failedIDs.size < tracks.size && player?.playWhenReady == true) {
                val native = player ?: return
                val next = (1..tracks.size).map { (native.currentMediaItemIndex + it) % tracks.size }
                    .firstOrNull { tracks[it].id !in failedIDs } ?: return
                notice = "Skipped an unavailable recording"
                native.seekTo(next, 0); native.prepare()
            }
        }
    }

    init {
        scope.launch {
            try {
                cacheSource = withContext(Dispatchers.IO) {
                    runCatching { BhajanAudioCache.source(app) }.getOrNull()
                }
                val future = MediaController.Builder(app,
                    SessionToken(app, ComponentName(app, BhajanPlaybackService::class.java))).buildAsync()
                val native = suspendCancellableCoroutine<MediaController> { continuation ->
                    future.addListener({
                        try { continuation.resumeWith(Result.success(future.get())) }
                        catch (e: Exception) { continuation.resumeWith(Result.failure(e)) }
                    }, ContextCompat.getMainExecutor(app))
                    continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
                }
                tracks = List(native.mediaItemCount) { index ->
                    val item = native.getMediaItemAt(index)
                    Track(item.mediaMetadata.title?.toString().orEmpty(),
                        item.mediaMetadata.extras?.getString("audio_url").orEmpty(), item.mediaId)
                }
                current = tracks.getOrNull(native.currentMediaItemIndex)
                title = current?.title ?: "Choose a bhajan"
                playing = native.isPlaying
                loading = native.playbackState == Player.STATE_BUFFERING
                repeat = native.repeatMode == Player.REPEAT_MODE_ONE
                native.addListener(listener)
                player = native; ready.complete(native)
            } catch (e: Exception) {
                ready.completeExceptionally(e); failed = true; loading = false
            }
        }
        scope.launch {
            while (isActive) {
                if (playing) syncProgress()
                delay(500)
            }
        }
    }


    fun changeIdol(value: String) { idol = value }
    fun fetchCatalog() {
        if (catalogJob?.isActive == true || catalogFetched || released) return
        catalogJob = scope.launch {
            catalogLoading = catalog.isEmpty(); catalogError = false
            try {
                withContext(Dispatchers.IO) { runCatching { parseCatalog(catalogFile.readText()) }.getOrNull() }
                    ?.let { catalog = it; catalogLoading = false }
                val refreshed = withContext(Dispatchers.IO) {
                    require(base.isNotBlank())
                    val connection = URL("$base/api/v2/music").openConnection() as HttpURLConnection
                    connection.connectTimeout = 8_000; connection.readTimeout = 8_000
                    if (catalogFile.exists()) {
                        preferences.getString("etag-$base", null)?.let { connection.setRequestProperty("If-None-Match", it) }
                    }
                    try {
                        if (connection.responseCode == 304) return@withContext null
                        check(connection.responseCode == 200)
                        val json = connection.inputStream.bufferedReader().use { it.readText() }
                        val result = parseCatalog(json)
                        val temp = File(catalogFile.path + ".tmp")
                        temp.writeText(json)
                        check(temp.renameTo(catalogFile))
                        preferences.edit().putString("etag-$base", connection.getHeaderField("ETag")).apply()
                        result
                    } finally { connection.disconnect() }
                }
                refreshed?.let { catalog = it }
                catalogFetched = true
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { catalogError = catalog.isEmpty() }
            finally { catalogLoading = false }
        }
    }

    private fun parseCatalog(json: String): List<Track> {
        val rows = JSONObject(json).getJSONArray("tracks")
        return (listOf(ganeshAarti) + List(rows.length()) { i -> rows.getJSONObject(i).let {
            Track(it.getString("title"), it.getString("audioUrl"), it.getString("id"),
                it.getInt("order"), it.getString("collection"), it.getString("deity"), it.getString("thumbnail"),
                it.optString("artist"), it.optInt("bitrateKbps"), it.optLong("durationSeconds"),
                it.optDouble("volumeGainDb", 0.0).toFloat())
        } }).distinctBy { it.url }.filter { it.url.startsWith("https://") || BuildConfig.DEBUG && it.url.startsWith("http://") }
    }

    fun changeShuffle(value: Boolean) {
        if (shuffle == value) return
        shuffle = value; preferences.edit().putBoolean("shuffle", value).apply()
        // Reorder while keeping a paused player paused.
        if (current != null) playCatalog(autoplay = player?.playWhenReady == true)
    }
    fun playCatalog(selected: Track? = null, autoplay: Boolean = true) {
        val ordered = BhajanQueue.order(catalog, shuffle, idol)
        if (ordered.isEmpty()) return
        val chosen = selected?.let { song -> ordered.firstOrNull { it.url == song.url } }
        tracks = if (chosen != null) listOf(chosen) + (ordered - chosen) else ordered
        failedIDs.clear(); notice = ""
        launchPlayback(autoplay) { native ->
            native.setMediaItems(tracks.map { it.mediaItem() })
        }
    }
    private fun Track.mediaItem() = MediaItem.Builder().setMediaId(id).setUri(url)
        .setCustomCacheKey(url).setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist)
            .setExtras(android.os.Bundle().apply { putString("audio_url", url) }).build()).build()

    private fun launchPlayback(autoplay: Boolean, change: (MediaController) -> Unit) {
        startJob?.cancel(); cancelPrefetch()
        loading = true; failed = false; requestedAt = SystemClock.elapsedRealtime(); firstFrameReported = false
        startJob = scope.launch {
            try {
                val native = ready.await()
                change(native); native.prepare(); native.playWhenReady = autoplay
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failed = true; loading = false }
        }
    }
    fun preload() { fetchCatalog() }
    fun startAarti() {
        // The ritual always starts the original Ganesh aarti, independently of
        // catalog availability, the selected idol, and the music shuffle setting.
        val aarti = ganeshAarti
        tracks = listOf(aarti) + BhajanQueue.order(catalog, shuffle, idol).filter { it.url != aarti.url }
        failedIDs.clear(); notice = ""
        launchPlayback(true) { native ->
            native.setMediaItems(tracks.map { it.mediaItem() }, 0, 0L)
        }
    }
    fun load(track: Track, autoplay: Boolean = true) {
        if (track.url.isBlank()) return
        if (track in tracks) launchPlayback(autoplay) { it.seekTo(tracks.indexOf(track), 0) }
        else playCatalog(track, autoplay)
    }
    fun toggle() {
        if (loading && player?.playWhenReady != true) { pause(); return }
        if (current == null) { startAarti(); return }
        val native = player ?: return
        if (failed) {
            failedIDs.clear(); notice = ""; failed = false
            requestedAt = SystemClock.elapsedRealtime(); firstFrameReported = false
            native.prepare(); native.play()
        } else if (native.playWhenReady) pause() else { firstFrameReported = false; requestedAt = SystemClock.elapsedRealtime(); native.play() }
    }
    fun pause() {
        startJob?.cancel(); cancelPrefetch(); player?.pause(); playing = false
        if (player == null || player?.mediaItemCount == 0) loading = false
    }
    fun seek(ms: Long) { player?.seekTo(ms.coerceIn(0, duration)); syncProgress() }
    fun toggleRepeat() {
        repeat = !repeat; cancelPrefetch(); player?.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
        if (!repeat && playing) prefetchNext()
    }
    fun skip(delta: Int) {
        val native = player ?: return
        if (tracks.isEmpty()) return
        failedIDs.clear(); notice = ""
        val index = (native.currentMediaItemIndex.coerceAtLeast(0) + delta + tracks.size) % tracks.size
        launchPlayback(true) { it.seekTo(index, 0) }
    }
    private fun syncProgress() {
        val native = player ?: return
        position = native.currentPosition.coerceAtLeast(0)
        duration = native.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: ((current?.durationSeconds ?: 0) * 1000)
    }
    private fun prefetchNext() {
        val source = cacheSource ?: return
        val native = player ?: return
        if (tracks.size < 2 || repeat) return
        val next = tracks[(native.currentMediaItemIndex + 1) % tracks.size]
        cancelPrefetch()
        prefetchJob = scope.launch {
            delay(3_000) // Give current audio priority on slow connections.
            if (!playing || !isActive) return@launch
            // Avoid competing with current playback on a weak connection.
            var waits = 0
            while (native.bufferedPosition - native.currentPosition < 15_000 &&
                native.duration != C.TIME_UNSET && native.duration - native.currentPosition > 15_000 && waits++ < 6) {
                delay(2_000)
                if (!playing) return@launch
            }
            if (native.bufferedPosition - native.currentPosition < 10_000 &&
                native.duration != C.TIME_UNSET && native.duration - native.currentPosition > 15_000) return@launch
            withContext(Dispatchers.IO) {
                val spec = DataSpec.Builder().setUri(next.url).setKey(next.url).setLength(512L * 1024).build()
                ensureActive()
                val writer = CacheWriter(source.createDataSource(), spec, null, null)
                prefetchWriter = writer
                if (!isActive) { writer.cancel(); return@withContext }
                try { writer.cache() } catch (_: Exception) { /* Opportunistic, never blocks playback. */ }
                finally { if (prefetchWriter === writer) prefetchWriter = null }
            }
        }
    }
    private fun cancelPrefetch() { prefetchWriter?.cancel(); prefetchJob?.cancel(); prefetchJob = null }
    fun release() {
        released = true; cancelPrefetch(); scope.cancel(); player?.removeListener(listener); player?.release(); player = null
        ready.cancel()
    }
}

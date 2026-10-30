package com.worship.nityamandir.engine

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.net.URLEncoder
import kotlin.math.pow

/** Catalog audio for the default aarti; all other songs use the visible YouTube player. */
@android.annotation.SuppressLint("UnsafeOptInUsageError")
class BhajanPlayer(context: Context) {
    data class Track(
        val title: String,
        val url: String,
        val id: String = url,
        val order: Int = 0,
        val collection: String = "",
        val deity: String = "",
        val thumbnail: String = "shrine/idols/original.png",
        val artist: String = "",
        val bitrateKbps: Int = 0,
        val durationSeconds: Long = 0,
        val volumeGainDb: Float = 0f,
        val youtubeId: String = "",
        val isYoutube: Boolean = false
    ) {
        val isYoutubeOnly: Boolean
            get() = isYoutube || url.startsWith("https://www.youtube.com/") || url.startsWith("https://youtu.be/")
        val isAdFreeArchive: Boolean
            get() = !isYoutubeOnly && url.isNotBlank()
        val needsStreamResolution: Boolean
            get() = url.startsWith("https://www.youtube.com/") || url.startsWith("https://youtu.be/")
        val fallbackAssetArt: String
            get() = when (deity) {
                "shiva" -> "shrine/idols/shiva.png"
                "durga" -> "shrine/idols/durga.png"
                "rama" -> "shrine/idols/ram_darbar.png"
                "lakshmi" -> "shrine/idols/lakshmi.png"
                "ganesh", "hanuman" -> "shrine/idols/ganesh_hanuman.png"
                else -> "shrine/idols/original.png"
            }
        val youtubeSearchQuery: String
            get() = "$title ${artist.substringBefore("•").trim()} bhajan".trim()
    }

    private val app = context.applicationContext
    private val analytics = com.worship.nityamandir.data.MandirAnalytics.from(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ready = CompletableDeferred<MediaController>()
    private var player: MediaController? = null
    private var startJob: Job? = null
    private var preloadJob: Job? = null
    @Volatile private var preloadWriter: CacheWriter? = null
    private var catalogJob: Job? = null
    private var youtubeCatalogLoaded = false
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var released = false
    private var clearOnConnect = false
    private var youtubeCommand: ((String) -> Unit)? = null
    private var youtubeGeneration: Int? = null

    var catalog by mutableStateOf<List<Track>>(emptyList()); private set
    var catalogError by mutableStateOf(false); private set
    var catalogLoading by mutableStateOf(false); private set
    val defaultTrack: Track? get() = catalog.firstOrNull { it.id == BhajanSources.DEFAULT_ID }
    var idol = "original"; private set
    var title by mutableStateOf("Jai Ganesh Jai Ganesh Deva"); private set
    var playing by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var repeat by mutableStateOf(false); private set
    var current by mutableStateOf<Track?>(null); private set
    val youtubeMode: Boolean get() = current?.isYoutubeOnly == true
    var youtubeAutoplay by mutableStateOf(false); private set
    var playbackGeneration by mutableIntStateOf(0); private set
    var activeYoutubeQuery by mutableStateOf(""); private set
    var youtubeSearchResults by mutableStateOf<List<Track>>(emptyList()); private set
    var youtubeSearching by mutableStateOf(false); private set
    var youtubeSuggestions by mutableStateOf<List<String>>(emptyList()); private set
    var youtubeSearchFailed by mutableStateOf(false); private set

    private val catalogHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    private val listener = object : Player.Listener {
        override fun onEvents(native: Player, events: Player.Events) {
            if (youtubeMode || current == null) return
            playing = native.isPlaying
            loading = native.playbackState == Player.STATE_BUFFERING
            syncProgress()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (youtubeMode || current == null) return
            failed = true
            loading = false
            playing = false
        }
    }

    init {
        fetchCatalog()
        scope.launch {
            try {
                val future = MediaController.Builder(app,
                    SessionToken(app, ComponentName(app, BhajanPlaybackService::class.java))).buildAsync()
                val native = suspendCancellableCoroutine<MediaController> { continuation ->
                    future.addListener({
                        try { continuation.resumeWith(Result.success(future.get())) }
                        catch (e: Exception) { continuation.resumeWith(Result.failure(e)) }
                    }, ContextCompat.getMainExecutor(app))
                    continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
                }
                // Discard legacy queues that substituted archive audio for YouTube recordings.
                if (clearOnConnect || native.mediaItemCount > 1 ||
                    native.currentMediaItem?.mediaId?.let { it != BhajanSources.DEFAULT_ID } == true ||
                    youtubeMode) {
                    native.stop()
                    native.clearMediaItems()
                    clearOnConnect = false
                } else if (current == null && native.currentMediaItem != null) {
                    current = defaultTrack
                    title = current?.title.orEmpty()
                    playing = native.isPlaying
                    loading = native.playbackState == Player.STATE_BUFFERING
                    repeat = native.repeatMode == Player.REPEAT_MODE_ONE
                }
                native.addListener(listener)
                player = native
                ready.complete(native)
                syncProgress()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                ready.completeExceptionally(e)
                if (!youtubeMode && current != null) { failed = true; loading = false }
            }
        }
        scope.launch {
            while (isActive) {
                if (!youtubeMode && playing) syncProgress()
                delay(500)
            }
        }
    }

    fun changeIdol(value: String) { idol = value }

    fun fetchCatalog() {
        if (released || catalogJob?.isActive == true || youtubeCatalogLoaded) return
        if (catalog.isEmpty()) catalog = runCatching {
            app.assets.open("shrine/music_catalog.json").bufferedReader().use { reader ->
                val rows = JSONObject(reader.readText()).getJSONArray("tracks")
                val raw = List(rows.length()) { i ->
                    val row = rows.getJSONObject(i)
                    Track(
                        id = row.getString("id"), title = row.getString("title"),
                        url = row.getString("audioUrl"), order = row.optInt("order"),
                        collection = row.optString("collection"), deity = row.optString("deity"),
                        thumbnail = row.optString("thumbnail"), artist = row.optString("artist"),
                        bitrateKbps = row.optInt("bitrateKbps"),
                        durationSeconds = row.optLong("durationSeconds"),
                        volumeGainDb = row.optDouble("volumeGainDb", 0.0).toFloat(),
                        youtubeId = row.optString("youtubeId")
                    )
                }
                BhajanSources.catalog(raw)
            }
        }.getOrDefault(emptyList())
        catalogError = defaultTrack == null
        catalogJob = scope.launch {
            catalogLoading = true
            try {
                // Use YouTube's own titles/IDs together. Legacy catalog IDs can point to unrelated videos.
                val videos = withContext(Dispatchers.IO) { fetchYoutubeSearchTracks("popular hindi bhajan aarti") }
                ensureActive()
                catalog = BhajanSources.catalog(listOfNotNull(defaultTrack) + videos)
                youtubeCatalogLoaded = videos.isNotEmpty()
                catalogError = defaultTrack == null || videos.isEmpty()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { catalogError = true }
            finally { if (isActive) catalogLoading = false }
        }
    }

    fun clearYoutubeSearch() {
        searchJob?.cancel()
        suggestionJob?.cancel()
        youtubeSuggestions = emptyList()
        activeYoutubeQuery = ""
        youtubeSearchResults = emptyList()
        youtubeSearching = false
        youtubeSearchFailed = false
    }

    fun fetchYoutubeSuggestions(query: String) {
        suggestionJob?.cancel()
        val clean = query.trim().take(150)
        if (clean.isEmpty() || released) { youtubeSuggestions = emptyList(); return }
        suggestionJob = scope.launch {
            delay(250)
            try {
                val suggestions = withContext(Dispatchers.IO) {
                    val encoded = URLEncoder.encode(clean, "UTF-8")
                    val request = Request.Builder()
                        .url("https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=en&gl=IN&q=$encoded")
                        .build()
                    catalogHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) emptyList()
                        else BhajanSearch.parseYoutubeSuggestResults(response.body?.string().orEmpty())
                    }
                }
                ensureActive()
                youtubeSuggestions = suggestions
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { youtubeSuggestions = emptyList() }
        }
    }

    fun searchYoutube(query: String) {
        val clean = query.trim().take(150)
        clearYoutubeSearch()
        if (clean.isEmpty() || released) return
        activeYoutubeQuery = clean
        youtubeSearching = true
        searchJob = scope.launch {
            try {
                val results = withContext(Dispatchers.IO) { fetchYoutubeSearchTracks(clean) }
                ensureActive()
                youtubeSearchResults = results.mapNotNull(BhajanSources::playable).distinctBy { it.youtubeId }
                analytics.logBhajanSearch(clean.length, youtubeSearchResults.size)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { youtubeSearchFailed = true }
            finally { if (isActive) youtubeSearching = false }
        }
    }

    fun playCatalog(selected: Track? = null, autoplay: Boolean = true) {
        if (released) return
        val chosen = BhajanSources.playable(selected ?: defaultTrack ?: return) ?: return
        startJob?.cancel()
        cancelPreload()
        youtubeCommand?.invoke("pauseBhajan()")
        // Pause the other source before switching state so the two sources never overlap.
        player?.pause()
        current = chosen
        title = chosen.title
        position = 0L
        duration = chosen.durationSeconds * 1000L
        playing = false
        loading = autoplay
        failed = false
        youtubeAutoplay = chosen.isYoutubeOnly && autoplay
        playbackGeneration += 1
        analytics.logBhajanPlayed(chosen.id, chosen.title, chosen.deity, chosen.isYoutubeOnly, autoplay)
        if (chosen.isYoutubeOnly) {
            player?.run { stop(); clearMediaItems() }
            return
        }
        startJob = scope.launch {
            try {
                val native = ready.await()
                ensureActive()
                native.setMediaItem(chosen.mediaItem())
                val gain = chosen.volumeGainDb.takeIf { it.isFinite() }?.coerceIn(-12f, 0f) ?: 0f
                native.volume = 10.0.pow(gain / 20.0).toFloat()
                native.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                native.prepare()
                native.playWhenReady = autoplay
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failed = true; loading = false }
        }
    }

    private fun Track.mediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id).setUri(url).setCustomCacheKey(url)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist)
            .setArtworkUri(Uri.parse(if (thumbnail.startsWith("https://")) thumbnail else "file:///android_asset/$thumbnail")).build())
        .build()

    fun startAarti() = playCatalog(defaultTrack)

    fun load(track: Track, autoplay: Boolean = true) = playCatalog(track, autoplay)

    fun toggle() {
        if (current == null) { startAarti(); return }
        if (youtubeMode) {
            if (failed) { playCatalog(current); return }
            if (playing || loading) { pause(); return }
            youtubeAutoplay = true
            loading = true
            youtubeCommand?.invoke("playBhajan()")
            return
        }
        if (playing || loading) { pause(); return }
        if (failed || player?.mediaItemCount == 0) { playCatalog(current); return }
        val native = player ?: run { playCatalog(current); return }
        if (native.playbackState == Player.STATE_ENDED) native.seekTo(0L)
        native.play()
    }

    fun pause() {
        startJob?.cancel()
        youtubeAutoplay = false
        youtubeCommand?.invoke("pauseBhajan()")
        player?.pause()
        playing = false
        loading = false
    }

    fun remove() {
        pause()
        cancelPreload()
        player?.run { stop(); clearMediaItems() } ?: run { clearOnConnect = true }
        current = null
        failed = false
        position = 0L
        duration = 0L
        title = defaultTrack?.title.orEmpty()
        playbackGeneration += 1
    }

    fun seek(ms: Long) {
        if (youtubeMode) {
            youtubeCommand?.invoke("seekBhajan(${ms.coerceAtLeast(0L) / 1000.0})")
            return
        }
        player?.seekTo(ms.coerceIn(0L, duration.coerceAtLeast(0L)))
        syncProgress()
    }

    fun toggleRepeat() {
        repeat = !repeat
        youtubeCommand?.invoke("repeatBhajan($repeat)")
        player?.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun skip(delta: Int) {
        val queue = (listOfNotNull(defaultTrack) + youtubeSearchResults + catalog.filter { it.isYoutubeOnly })
            .distinctBy { if (it.isYoutubeOnly) it.youtubeId else it.id }
        if (queue.isEmpty()) return
        val index = queue.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        playCatalog(queue[Math.floorMod(index + delta, queue.size)])
    }

    fun attachYoutube(generation: Int, command: (String) -> Unit) {
        if (generation == playbackGeneration) {
            youtubeGeneration = generation
            youtubeCommand = command
        }
    }

    fun detachYoutube(generation: Int) {
        if (youtubeGeneration == generation) {
            youtubeCommand = null
            youtubeGeneration = null
        }
        if (generation != playbackGeneration) return
        youtubeAutoplay = false
        playing = false
        loading = false
    }

    fun updateYoutube(generation: Int, state: Int, seconds: Double, total: Double, error: Boolean) {
        if (released || !youtubeMode || generation != playbackGeneration || youtubeGeneration != generation) return
        failed = error
        playing = !error && state == 1
        loading = !error && state == 3
        if (seconds.isFinite()) position = (seconds.coerceAtLeast(0.0) * 1000).toLong()
        if (total.isFinite() && total > 0) duration = (total * 1000).toLong()
    }

    fun preload() {
        if (released || preloadJob?.isActive == true) return
        val track = defaultTrack ?: return
        preloadJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val source = BhajanAudioCache.source(app)
                val spec = DataSpec.Builder().setUri(track.url).setKey(track.url).setLength(512L * 1024).build()
                ensureActive()
                val writer = CacheWriter(source.createDataSource(), spec, null, null)
                preloadWriter = writer
                if (!isActive) { writer.cancel(); return@launch }
                try { writer.cache() } finally { if (preloadWriter === writer) preloadWriter = null }
            }
        }
    }

    private fun cancelPreload() { preloadWriter?.cancel(); preloadJob?.cancel(); preloadJob = null }

    private fun syncProgress() {
        if (youtubeMode || current == null) return
        val native = player ?: return
        position = native.currentPosition.coerceAtLeast(0L)
        duration = native.duration.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: (current?.durationSeconds ?: 0) * 1000L
    }

    fun release() {
        released = true
        youtubeCommand?.invoke("pauseBhajan()")
        youtubeCommand = null
        cancelPreload()
        scope.cancel()
        player?.removeListener(listener)
        player?.release()
        player = null
        ready.cancel()
    }

    private fun fetchYoutubeSearchTracks(searchQuery: String): List<Track> {
        val bodyJson = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "WEB")
                .put("clientVersion", "2.20240726.00.00")
                .put("hl", "en")
                .put("gl", "IN")
            ))
            .put("query", searchQuery)
            .toString()
        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return catalogHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("YouTube search unavailable")
            else BhajanSearch.parseYoutubeSearchResults(response.body?.string().orEmpty(), idol)
        }
    }

}

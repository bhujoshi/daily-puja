package com.worship.nityamandir.engine

import android.content.Context
import android.content.ComponentName
import android.net.Uri
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/** UI controller for the service-owned playlist and system media controls. */
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = app.getSharedPreferences("music", Context.MODE_PRIVATE)
    private val base = BuildConfig.ACCOUNT_API_URL.trimEnd('/')
    private val catalogFile = File(app.filesDir, "bhajan-catalog-${base.hashCode()}.json")
    private val ready = CompletableDeferred<MediaController>()
    private var player: MediaController? = null
    private var clearOnConnect = false
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
    private val resolvedStreamCache = ConcurrentHashMap<String, String>()

    private val ganeshAarti = Track(
        title = "Jai Ganesh Jai Ganesh Deva",
        url = "https://archive.org/download/JaiGaneshJaiGaneshJaiGaneshDevaLordGaneshAarti/" +
            "Jai%20Ganesh%20Jai%20Ganesh%20Jai%20Ganesh%20Deva%20-%20Lord%20Ganesh%20Aarti.mp3",
        id = "jai_ganesh_deva",
        collection = "ganesh",
        thumbnail = "https://i.ytimg.com/vi/87Y-OH_mJxM/hqdefault.jpg",
        deity = "ganesh",
        artist = "Anuradha Paudwal • Ganesh Aarti",
        durationSeconds = 285,
        youtubeId = "87Y-OH_mJxM",
        isYoutube = false
    )

    private val youtubeFeaturedTracks = listOf(
        Track(
            title = "Shree Hanuman Chalisa (Original Video)",
            url = "https://www.youtube.com/watch?v=AETFvQonfV8",
            id = "yt_hanuman_chalisa",
            order = 1,
            collection = "hanuman",
            deity = "hanuman",
            thumbnail = "https://i.ytimg.com/vi/AETFvQonfV8/hqdefault.jpg",
            artist = "Gulshan Kumar, Hariharan • T-Series Bhakti Sagar",
            durationSeconds = 464,
            volumeGainDb = -9.9f,
            youtubeId = "AETFvQonfV8",
            isYoutube = true
        ),
        Track(
            title = "Shiv Tandav Stotram (Har Har Shiv Shankar)",
            url = "https://www.youtube.com/watch?v=hMBKmQEPNzI",
            id = "yt_shiv_tandav",
            order = 2,
            collection = "shiva",
            deity = "shiva",
            thumbnail = "https://i.ytimg.com/vi/hMBKmQEPNzI/hqdefault.jpg",
            artist = "Uma Mohan • Times Music Spiritual",
            durationSeconds = 389,
            volumeGainDb = -8.2f,
            youtubeId = "hMBKmQEPNzI",
            isYoutube = true
        ),
        Track(
            title = "Sukh Karta Dukh Harta (Ganpati Aarti)",
            url = "https://www.youtube.com/watch?v=87Y-OH_mJxM",
            id = "yt_sukh_karta",
            order = 3,
            collection = "ganesh",
            deity = "ganesh",
            thumbnail = "https://i.ytimg.com/vi/87Y-OH_mJxM/hqdefault.jpg",
            artist = "Lata Mangeshkar, Hemant Chauhan • Ganesh Bhakti",
            durationSeconds = 380,
            youtubeId = "87Y-OH_mJxM",
            isYoutube = true
        ),
        Track(
            title = "Ambe Tu Hai Jagdambe Kali (Maa Durga Aarti)",
            url = "https://www.youtube.com/watch?v=gFD46BhorTk",
            id = "yt_ambe_tu_hai",
            order = 4,
            collection = "durga",
            deity = "durga",
            thumbnail = "https://i.ytimg.com/vi/gFD46BhorTk/hqdefault.jpg",
            artist = "Anuradha Paudwal • T-Series Bhakti Sagar",
            durationSeconds = 283,
            volumeGainDb = -2.9f,
            youtubeId = "gFD46BhorTk",
            isYoutube = true
        ),
        Track(
            title = "Raam Aayenge To Angana Sajaungi",
            url = "https://www.youtube.com/watch?v=3TylP-ux8W0",
            id = "yt_raam_aayenge",
            order = 5,
            collection = "mixed",
            deity = "rama",
            thumbnail = "https://i.ytimg.com/vi/3TylP-ux8W0/hqdefault.jpg",
            artist = "Swati Mishra • Ram Bhakti",
            durationSeconds = 233,
            volumeGainDb = -2.9f,
            youtubeId = "3TylP-ux8W0",
            isYoutube = true
        ),
        Track(
            title = "Mere Banke Bihari Laal (Vrindavan Bhajan)",
            url = "https://www.youtube.com/watch?v=L1s4jaR0TpM",
            id = "yt_banke_bihari",
            order = 6,
            collection = "mixed",
            deity = "krishna",
            thumbnail = "https://i.ytimg.com/vi/L1s4jaR0TpM/hqdefault.jpg",
            artist = "Lakhbir Singh Lakkha • Bhakti Sagar",
            durationSeconds = 574,
            volumeGainDb = -10.8f,
            youtubeId = "L1s4jaR0TpM",
            isYoutube = true
        ),
        Track(
            title = "Sankat Mochan Naam Tiharo (Hanuman Ashtak)",
            url = "https://www.youtube.com/watch?v=HH_a6aRO1TE",
            id = "yt_sankat_mochan",
            order = 7,
            collection = "hanuman",
            deity = "hanuman",
            thumbnail = "https://i.ytimg.com/vi/HH_a6aRO1TE/hqdefault.jpg",
            artist = "Hariharan • T-Series Bhakti Sagar",
            durationSeconds = 374,
            volumeGainDb = -5.9f,
            youtubeId = "HH_a6aRO1TE",
            isYoutube = true
        ),
        Track(
            title = "Mera Bhola Hai Bhandari (Har Har Mahadev)",
            url = "https://www.youtube.com/watch?v=gaJR15qWTDA",
            id = "yt_bhola_bhandari",
            order = 8,
            collection = "shiva",
            deity = "shiva",
            thumbnail = "https://i.ytimg.com/vi/gaJR15qWTDA/hqdefault.jpg",
            artist = "Hansraj Raghuwanshi • Shiv Bhakti",
            durationSeconds = 342,
            volumeGainDb = -2.8f,
            youtubeId = "gaJR15qWTDA",
            isYoutube = true
        ),
        Track(
            title = "Ganpati Bappa Morya (Siddhivinayak Vandana)",
            url = "https://www.youtube.com/watch?v=WtOv9lhfVnw",
            id = "yt_ganpati_morya",
            order = 9,
            collection = "ganesh",
            deity = "ganesh",
            thumbnail = "https://i.ytimg.com/vi/WtOv9lhfVnw/hqdefault.jpg",
            artist = "Shankar Mahadevan • Ganeshotsav",
            durationSeconds = 252,
            volumeGainDb = -2.1f,
            youtubeId = "WtOv9lhfVnw",
            isYoutube = true
        ),
        Track(
            title = "Shriman Narayan Narayan Hari Hari",
            url = "https://www.youtube.com/watch?v=xU5uRpl8nzA",
            id = "yt_shriman_narayan",
            order = 10,
            collection = "mixed",
            deity = "lakshmi",
            thumbnail = "https://i.ytimg.com/vi/xU5uRpl8nzA/hqdefault.jpg",
            artist = "Anuradha Paudwal • Bhakti Sandhya",
            durationSeconds = 503,
            youtubeId = "xU5uRpl8nzA",
            isYoutube = true
        ),
        Track(
            title = "Durga Chalisa (Namo Namo Durge Sukh Karani)",
            url = "https://www.youtube.com/watch?v=WnDOYHRbZ00",
            id = "yt_durga_chalisa",
            order = 11,
            collection = "durga",
            deity = "durga",
            thumbnail = "https://i.ytimg.com/vi/WnDOYHRbZ00/hqdefault.jpg",
            artist = "Anuradha Paudwal • Durga Stuti",
            durationSeconds = 515,
            volumeGainDb = -0.5f,
            youtubeId = "WnDOYHRbZ00",
            isYoutube = true
        ),
        Track(
            title = "Shyama Aan Baso Vrindavan Mein",
            url = "https://www.youtube.com/watch?v=HfHkBEjofqk",
            id = "yt_shyama_aan_baso",
            order = 12,
            collection = "mixed",
            deity = "krishna",
            thumbnail = "https://i.ytimg.com/vi/HfHkBEjofqk/hqdefault.jpg",
            artist = "Tripti Shakya • Shyam Bhajan",
            durationSeconds = 460,
            volumeGainDb = -3.5f,
            youtubeId = "HfHkBEjofqk",
            isYoutube = true
        )
    )

    private val bundledById: Map<String, Track> by lazy {
        runCatching {
            app.assets.open("shrine/music_catalog.json").bufferedReader().use { reader ->
                parseRawTracks(reader.readText())
            }.associateBy { it.id }
        }.getOrDefault(emptyMap())
    }

    var catalog by mutableStateOf<List<Track>>(
        listOf(ganeshAarti) + youtubeFeaturedTracks
    ); private set
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
    var youtubeMode by mutableStateOf(false); private set
    var activeYoutubeQuery by mutableStateOf(""); private set
    var youtubeSearchResults by mutableStateOf<List<Track>>(emptyList()); private set
    var relatedYoutubeTracks by mutableStateOf<List<Track>>(emptyList()); private set
    var youtubeSuggestions by mutableStateOf<List<String>>(emptyList()); private set
    var youtubeSearching by mutableStateOf(false); private set
    var recommendationSeed by mutableIntStateOf(0); private set
    var recentHistoryIds by mutableStateOf<Set<String>>(emptySet()); private set
    private val playHistory = mutableListOf<Track>()
    private var youtubeSearchJob: Job? = null
    private var relatedYoutubeJob: Job? = null
    private var youtubeSuggestJob: Job? = null

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
            val previousTrack = current
            val previousId = previousTrack?.id
            val matched = tracks.firstOrNull { it.id == mediaItem?.mediaId }
                ?: catalog.firstOrNull { it.id == mediaItem?.mediaId }
            if (matched != null) {
                current = matched
                youtubeMode = matched.isYoutubeOnly || matched.youtubeId.isNotBlank()
                title = matched.title
            } else if (mediaItem != null) {
                val extras = mediaItem.mediaMetadata.extras
                val isYt = extras?.getBoolean("is_youtube", true) == true
                val reconstructed = Track(
                    title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                    url = extras?.getString("audio_url").orEmpty(),
                    id = mediaItem.mediaId,
                    deity = extras?.getString("deity").orEmpty(),
                    thumbnail = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty().ifBlank { "shrine/idols/original.png" },
                    artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                    youtubeId = extras?.getString("youtube_id").orEmpty(),
                    isYoutube = isYt
                )
                current = reconstructed
                youtubeMode = isYt
                title = reconstructed.title.ifBlank { "Choose a bhajan" }
            }
            position = 0; failed = false; firstFrameReported = false
            requestedAt = SystemClock.elapsedRealtime()
            val gainDb = (current?.volumeGainDb ?: 0f).takeIf { it.isFinite() }?.coerceIn(-12f, 0f) ?: 0f
            val vol = 10.0.pow(gainDb / 20.0).toFloat()
            if (vol.isFinite()) {
                player?.volume = vol.coerceIn(0f, 1f)
            }
            val active = current
            if (active != null && active.id != previousId) {
                if (previousTrack != null && playHistory.lastOrNull()?.id != previousTrack.id) {
                    playHistory.add(previousTrack)
                    if (playHistory.size > 30) playHistory.removeAt(0)
                }
                recentHistoryIds = (playHistory.map { it.id } + active.id).toSet()
                recommendationSeed += 1
                relatedYoutubeTracks = emptyList()
                fetchRelatedYoutubeForTrack(active)
                preResolveUpcomingTracks(active)
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            Log.w("BhajanPlayback", "track=${current?.id} error=${error.errorCodeName}")
            loading = false; playing = false; failed = true
            current?.let {
                failedIDs.add(it.id)
                resolvedStreamCache.remove(it.id)
            }
            // Bound recovery to a few attempts; resolve next YouTube track cleanly before seeking.
            if (failedIDs.size < 4 && player?.playWhenReady == true) {
                val nextCandidate = tracks.drop(1).firstOrNull { it.id !in failedIDs }
                    ?: catalog.firstOrNull { it.id != "jai_ganesh_deva" && it.id !in failedIDs }
                    ?: return
                notice = "Skipped an unavailable recording"
                playCatalog(selected = nextCandidate, autoplay = true, preferYoutube = true, recordHistory = false)
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
                if (clearOnConnect) {
                    native.stop(); native.clearMediaItems(); clearOnConnect = false
                }
                tracks = List(native.mediaItemCount) { index ->
                    val item = native.getMediaItemAt(index)
                    val extras = item.mediaMetadata.extras
                    val byId = catalog.firstOrNull { it.id == item.mediaId }
                    byId ?: Track(
                        title = item.mediaMetadata.title?.toString().orEmpty(),
                        url = extras?.getString("audio_url").orEmpty(),
                        id = item.mediaId,
                        deity = extras?.getString("deity").orEmpty(),
                        thumbnail = item.mediaMetadata.artworkUri?.toString().orEmpty().ifBlank { "shrine/idols/original.png" },
                        artist = item.mediaMetadata.artist?.toString().orEmpty(),
                        youtubeId = extras?.getString("youtube_id").orEmpty(),
                        isYoutube = extras?.getBoolean("is_youtube", false) == true
                    )
                }
                if (current == null) {
                    current = tracks.getOrNull(native.currentMediaItemIndex)
                    youtubeMode = current?.isYoutubeOnly == true
                    title = current?.title ?: "Choose a bhajan"
                    playing = native.isPlaying
                    loading = native.playbackState == Player.STATE_BUFFERING
                }
                repeat = native.repeatMode == Player.REPEAT_MODE_ONE
                native.addListener(listener)
                player = native; ready.complete(native)
            } catch (e: Exception) {
                ready.completeExceptionally(e); failed = true; loading = false
            }
        }
        scope.launch {
            while (isActive) {
                if (playing) {
                    syncProgress()
                    delay(500)
                } else {
                    delay(2500)
                }
            }
        }
    }

    private val catalogHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun changeIdol(value: String) { idol = value }

    fun fetchCatalog() {
        if (relatedYoutubeTracks.isEmpty() && !released) {
            val seed = current ?: catalog.firstOrNull { it.isYoutubeOnly } ?: catalog.firstOrNull()
            if (seed != null) fetchRelatedYoutubeForTrack(seed)
        }
        if (catalogJob?.isActive == true || catalogFetched || released) return
        catalogJob = scope.launch {
            catalogLoading = catalog.isEmpty(); catalogError = false
            try {
                val bundled = withContext(Dispatchers.IO) {
                    runCatching {
                        app.assets.open("shrine/music_catalog.json").bufferedReader().use { parseCatalog(it.readText()) }
                    }.getOrNull()
                }
                if (!bundled.isNullOrEmpty()) {
                    catalog = bundled
                }
                catalogFetched = true
                // Pre-resolve the first featured YouTube track in the background so initial tap starts instantly.
                val firstYoutube = catalog.firstOrNull { it.isYoutubeOnly && it.needsStreamResolution }
                if (firstYoutube != null && !resolvedStreamCache.containsKey(firstYoutube.id)) {
                    withContext(Dispatchers.IO) {
                        runCatching { resolveDirectPlayableUrl(firstYoutube) }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { catalogError = catalog.isEmpty() }
            finally { catalogLoading = false }
        }
    }

    private fun parseRawTracks(json: String): List<Track> {
        val rows = JSONObject(json).getJSONArray("tracks")
        return List(rows.length()) { i ->
            rows.getJSONObject(i).let {
                val id = it.getString("id")
                val ytId = it.optString("youtubeId")
                val isDefaultAarti = id == "jai_ganesh_deva"
                val rawAudioUrl = it.getString("audioUrl")
                val effectiveUrl = if (!isDefaultAarti && ytId.isNotBlank()) {
                    "https://www.youtube.com/watch?v=$ytId"
                } else {
                    rawAudioUrl
                }
                Track(
                    title = it.getString("title"),
                    url = effectiveUrl,
                    id = id,
                    order = it.getInt("order"),
                    collection = it.getString("collection"),
                    deity = it.getString("deity"),
                    thumbnail = it.getString("thumbnail"),
                    artist = it.optString("artist"),
                    bitrateKbps = it.optInt("bitrateKbps"),
                    durationSeconds = it.optLong("durationSeconds"),
                    volumeGainDb = it.optDouble("volumeGainDb", 0.0).toFloat(),
                    youtubeId = ytId,
                    isYoutube = !isDefaultAarti
                )
            }
        }
    }

    private fun parseCatalog(json: String): List<Track> {
        val parsed = parseRawTracks(json).map { remote ->
            if (remote.id == "jai_ganesh_deva") {
                ganeshAarti
            } else {
                val fallback = bundledById[remote.id]
                val verifiedYoutubeId = fallback?.youtubeId?.takeIf { it.isNotBlank() } ?: remote.youtubeId
                val verifiedThumbnail = fallback?.thumbnail?.takeIf { it.startsWith("https://") }
                    ?: if (remote.thumbnail.startsWith("https://")) remote.thumbnail else fallback?.thumbnail ?: remote.thumbnail
                val ytUrl = if (verifiedYoutubeId.isNotBlank()) {
                    "https://www.youtube.com/watch?v=$verifiedYoutubeId"
                } else {
                    remote.url
                }
                remote.copy(
                    url = ytUrl,
                    thumbnail = verifiedThumbnail,
                    artist = remote.artist.takeIf { it.isNotBlank() && !it.contains("RiskyjaTT", true) && !it.contains("PagalWorld", true) }
                        ?: fallback?.artist ?: remote.artist,
                    youtubeId = verifiedYoutubeId,
                    isYoutube = true
                )
            }
        }
        return (listOf(ganeshAarti) + youtubeFeaturedTracks + parsed)
            .distinctBy { it.id }
            .filter { it.url.startsWith("https://") || BuildConfig.DEBUG && it.url.startsWith("http://") }
    }

    fun clearYoutubeSearch() {
        youtubeSearchJob?.cancel()
        youtubeSuggestJob?.cancel()
        youtubeSearching = false
        activeYoutubeQuery = ""
        youtubeSearchResults = emptyList()
        youtubeSuggestions = emptyList()
    }

    /** Fetches live YouTube search autocomplete / next-word suggestions as the user types. */
    fun fetchYoutubeSuggestions(query: String) {
        val clean = query.trim()
        youtubeSuggestJob?.cancel()
        if (clean.isEmpty()) {
            youtubeSuggestions = emptyList()
            return
        }
        youtubeSuggestJob = scope.launch {
            val remote = withTimeoutOrNull(2_500L) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val encoded = URLEncoder.encode(clean, "UTF-8")
                        val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=en&gl=IN&q=$encoded"
                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .build()
                        catalogHttpClient.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) emptyList()
                            else BhajanSearch.parseYoutubeSuggestResults(resp.body?.string().orEmpty())
                        }
                    }.getOrDefault(emptyList())
                }
            }.orEmpty()
            if (remote.isNotEmpty()) {
                youtubeSuggestions = remote
            }
        }
    }

    fun searchYoutube(query: String, autoplayFirst: Boolean = false) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            clearYoutubeSearch()
            return
        }
        if (!activeYoutubeQuery.equals(clean, ignoreCase = true)) {
            youtubeSearchResults = emptyList()
            activeYoutubeQuery = clean
        } else if (youtubeSearchJob?.isActive == true && !autoplayFirst) {
            return
        }
        youtubeSearchJob?.cancel()
        youtubeSearching = true
        youtubeSearchJob = scope.launch {
            try {
                val results = withTimeoutOrNull(6_000L) {
                    withContext(Dispatchers.IO) {
                        val searchPhrase = if (clean.contains("bhajan", true) || clean.contains("aarti", true) || clean.contains("chalisa", true) || clean.contains("भजन") || clean.contains("आरती") || clean.contains("चालीसा")) {
                            clean
                        } else {
                            "$clean bhajan"
                        }
                        fetchYoutubeSearchTracks(searchPhrase)
                    }
                }.orEmpty()
                if (results.isNotEmpty()) {
                    youtubeSearchResults = results
                }
                if (autoplayFirst) {
                    val top = results.firstOrNull()
                        ?: BhajanSearch.youtubeFeedRecommendations(null, catalog, youtubeSearchResults, clean, "all", idol).firstOrNull()
                    if (top != null) {
                        playCatalog(top, autoplay = true, preferYoutube = true)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep local catalog recommendations available when offline.
            } finally {
                youtubeSearching = false
            }
        }
    }

    /** Fetches related YouTube bhajans in the background whenever a song or next song is played. */
    private fun fetchRelatedYoutubeForTrack(track: Track) {
        relatedYoutubeJob?.cancel()
        val seedSnapshot = recommendationSeed
        relatedYoutubeJob = scope.launch {
            val cleanArtist = track.artist.substringBefore("•").trim()
            val deityQueries = when (track.deity.lowercase()) {
                "hanuman" -> listOf(
                    "hanuman bhajan aarti ashtak",
                    "bajrang bali bhajan kirtan",
                    "sankat mochan hanuman bhakti song",
                    "shri ram hanuman bhajan"
                )
                "shiva" -> listOf(
                    "shiv bhajan mahadev aarti",
                    "bholenath bhajan stotram",
                    "om namah shivay dhun bhajan",
                    "har har mahadev bhakti song"
                )
                "ganesh" -> listOf(
                    "ganesh bhajan aarti vandana",
                    "ganpati bappa morya bhajan",
                    "siddhivinayak ganesh mantra bhajan",
                    "shri ganesh ji ke bhajan"
                )
                "durga" -> listOf(
                    "mata rani bhajan navratri aarti",
                    "maa durga bhajan ambe bhente",
                    "sherawali mata bhajan",
                    "vaishno devi mata bhajan"
                )
                "rama", "ram" -> listOf(
                    "shri ram bhajan ayodhya aarti",
                    "siya ram bhajan kirtan",
                    "ramchandra kripalu bhajan",
                    "raghupati raghav ram bhajan"
                )
                "krishna" -> listOf(
                    "krishna bhajan vrindavan banke bihari",
                    "radhe krishna bhajan kirtan",
                    "shyam bhajan khatu shyam",
                    "achyutam keshavam krishna bhajan"
                )
                "lakshmi", "vishnu" -> listOf(
                    "lakshmi narayan bhajan aarti",
                    "vishnu bhajan om jai jagdish",
                    "mahalakshmi mantra bhajan",
                    "shriman narayan bhajan"
                )
                else -> listOf(
                    "$cleanArtist devotional bhajan",
                    "popular hindi bhajan aarti",
                    "morning bhakti bhajan"
                )
            }
            val relatedQuery = deityQueries[(seedSnapshot and 0x7FFFFFFF) % deityQueries.size].trim()
            val results = withTimeoutOrNull(6_000L) {
                withContext(Dispatchers.IO) {
                    runCatching { fetchYoutubeSearchTracks(relatedQuery) }.getOrDefault(emptyList())
                }
            }.orEmpty()
            if (results.isNotEmpty()) {
                val filtered = BhajanSearch.deduplicateTracks(results, exclude = track)
                if (filtered.isNotEmpty()) {
                    relatedYoutubeTracks = filtered
                }
            }
        }
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
            if (!response.isSuccessful) emptyList()
            else BhajanSearch.parseYoutubeSearchResults(response.body?.string().orEmpty(), idol)
        }
    }

    /**
     * Resolves any YouTube watch URL (`https://www.youtube.com/watch?v=...`) to a direct YouTube audio stream
     * so that ExoPlayer in [BhajanPlaybackService] plays YouTube tracks and posts every song to the
     * Android Notification Bar. Only `jai_ganesh_deva` uses a direct non-YouTube MP3 URL (for default Aarti).
     */
    private fun resolveDirectPlayableUrl(track: Track): String {
        if (!track.needsStreamResolution) return track.url
        resolvedStreamCache[track.id]?.let { return it }

        // 1. Resolve via YouTube Innertube player endpoint using the track's youtubeId
        val videoId = track.youtubeId.ifBlank {
            track.url.substringAfter("v=", "").substringBefore("&").trim()
        }
        if (videoId.isNotBlank()) {
            val extracted = runCatching { fetchInnertubeDirectStreamUrl(videoId) }.getOrNull()
            if (!extracted.isNullOrBlank()) {
                resolvedStreamCache[track.id] = extracted
                return extracted
            }
        }

        // 2. If this specific videoId is restricted or unavailable, search YouTube for an alternate upload of the same bhajan
        val alternates = runCatching { fetchYoutubeSearchTracks(track.youtubeSearchQuery) }.getOrDefault(emptyList())
        for (alt in alternates.take(3)) {
            if (alt.youtubeId.isNotBlank() && alt.youtubeId != videoId) {
                val altStream = runCatching { fetchInnertubeDirectStreamUrl(alt.youtubeId) }.getOrNull()
                if (!altStream.isNullOrBlank()) {
                    resolvedStreamCache[track.id] = altStream
                    return altStream
                }
            }
        }

        // 3. Last resort: return any already-resolved YouTube stream from cache or resolve top featured YouTube track
        resolvedStreamCache.values.firstOrNull()?.let { return it }
        for (featured in youtubeFeaturedTracks.take(3)) {
            val stream = runCatching { fetchInnertubeDirectStreamUrl(featured.youtubeId) }.getOrNull()
            if (!stream.isNullOrBlank()) {
                resolvedStreamCache[featured.id] = stream
                return stream
            }
        }
        return track.url
    }

    private fun fetchInnertubeDirectStreamUrl(videoId: String): String? {
        if (videoId.isBlank()) return null

        // Primary client: ANDROID_VR (fast, unthrottled direct audio/mp4 streams)
        val vrClient = JSONObject()
            .put("clientName", "ANDROID_VR")
            .put("clientVersion", "1.56.21")
            .put("androidSdkVersion", 34)
            .put("hl", "en")
            .put("gl", "IN")
        callInnertubePlayer(
            videoId = videoId,
            clientJson = vrClient,
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 14; en_IN) gzip"
        )?.let { return it }

        // Fallback client 1: IOS
        val iosClient = JSONObject()
            .put("clientName", "IOS")
            .put("clientVersion", "19.29.1")
            .put("deviceMake", "Apple")
            .put("deviceModel", "iPhone16,2")
            .put("osName", "iPhone")
            .put("osVersion", "17.5.1.21F90")
            .put("hl", "en")
            .put("gl", "IN")
        callInnertubePlayer(
            videoId = videoId,
            clientJson = iosClient,
            userAgent = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)"
        )?.let { return it }

        // Fallback client 2: ANDROID
        val androidClient = JSONObject()
            .put("clientName", "ANDROID")
            .put("clientVersion", "19.09.37")
            .put("androidSdkVersion", 30)
            .put("hl", "en")
            .put("gl", "IN")
        return callInnertubePlayer(
            videoId = videoId,
            clientJson = androidClient,
            userAgent = "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip"
        )
    }

    private fun callInnertubePlayer(videoId: String, clientJson: JSONObject, userAgent: String): String? {
        val bodyJson = JSONObject()
            .put("context", JSONObject().put("client", clientJson))
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .toString()
        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .header("Content-Type", "application/json")
            .header("User-Agent", userAgent)
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return runCatching {
            catalogHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null
                else BhajanSearch.parseYoutubePlayerStreamUrl(response.body?.string().orEmpty())
            }
        }.getOrNull()
    }

    /**
     * Pre-resolves the next 2 upcoming tracks in the background and appends them to [MediaController]
     * so notification bar Next and automatic end-of-track transitions work with zero delay.
     */
    private fun preResolveUpcomingTracks(anchor: Track) {
        val upcoming = tracks.drop(1).filter { it.id != anchor.id }.take(2)
        if (upcoming.isEmpty()) return
        scope.launch {
            val native = runCatching { ready.await() }.getOrNull() ?: return@launch
            for (nextItem in upcoming) {
                if (!isActive || current?.id != anchor.id) break
                val resolvedUrl = if (nextItem.needsStreamResolution) {
                    resolvedStreamCache[nextItem.id] ?: withContext(Dispatchers.IO) {
                        runCatching { resolveDirectPlayableUrl(nextItem) }.getOrNull()
                    }
                } else {
                    nextItem.url
                }
                if (resolvedUrl.isNullOrBlank() || resolvedUrl.startsWith("https://www.youtube.com/")) continue
                if (!isActive || current?.id != anchor.id) break
                val resolvedTrack = nextItem.copy(url = resolvedUrl)
                tracks = tracks.map { if (it.id == resolvedTrack.id) resolvedTrack else it }
                val alreadyInPlayer = (0 until native.mediaItemCount).any { idx ->
                    native.getMediaItemAt(idx).mediaId == resolvedTrack.id
                }
                if (!alreadyInPlayer) {
                    native.addMediaItem(resolvedTrack.mediaItem())
                }
            }
        }
    }

    fun changeShuffle(value: Boolean) {
        if (shuffle == value) return
        shuffle = value; preferences.edit().putBoolean("shuffle", value).apply()
        if (current != null) playCatalog(selected = current, autoplay = player?.playWhenReady == true, preferYoutube = youtubeMode)
    }

    /**
     * Unified playback entry point for YouTube tracks (and `jai_ganesh_deva` when explicitly selected).
     * Plays through [BhajanPlaybackService] (ExoPlayer + MediaSession), so every track appears in the
     * Android Notification Bar and behaves identically in the player UI.
     */
    fun playCatalog(
        selected: Track? = null,
        autoplay: Boolean = true,
        preferYoutube: Boolean = true,
        recordHistory: Boolean = true
    ) {
        val chosen = selected
            ?: (if (preferYoutube) catalog.firstOrNull { it.isYoutubeOnly } else catalog.firstOrNull { !it.isYoutubeOnly })
            ?: catalog.firstOrNull()
            ?: return

        val previous = current
        if (recordHistory && previous != null && !BhajanSearch.isSameSong(previous, chosen)) {
            if (playHistory.lastOrNull()?.id != previous.id) {
                playHistory.add(previous)
                if (playHistory.size > 30) playHistory.removeAt(0)
            }
        }
        if (previous == null || !BhajanSearch.isSameSong(previous, chosen)) {
            relatedYoutubeTracks = emptyList()
        }

        val isYt = chosen.id != "jai_ganesh_deva" && (preferYoutube || chosen.isYoutubeOnly || chosen.youtubeId.isNotBlank())
        val activeTrack = if (chosen.isYoutube != isYt) chosen.copy(isYoutube = isYt) else chosen
        youtubeMode = isYt
        activeYoutubeQuery = ""
        recommendationSeed += 1
        recentHistoryIds = (playHistory.map { it.id } + activeTrack.id).toSet()

        // Build a YouTube-first Up Next queue following the selected song
        val combinedYoutubePool = (youtubeSearchResults + relatedYoutubeTracks).distinctBy { it.id }
        val relevantQueue = BhajanSearch.youtubeFeedRecommendations(
            anchor = activeTrack,
            catalog = catalog,
            youtubeResults = combinedYoutubePool,
            searchQuery = "",
            categoryFilter = "all",
            idol = idol,
            playedTrackIds = recentHistoryIds,
            rotationSeed = recommendationSeed
        ).filterNot { BhajanSearch.isSameSong(it, activeTrack) }
        tracks = listOf(activeTrack) + relevantQueue
        current = activeTrack
        title = activeTrack.title
        duration = activeTrack.durationSeconds * 1000L
        position = 0L
        failedIDs.clear(); notice = ""

        // Fetch fresh related YouTube recommendations for the selected song in the background
        fetchRelatedYoutubeForTrack(activeTrack)

        launchPlayback(autoplay) { native ->
            val resolvedFirst = if (activeTrack.needsStreamResolution) {
                val url = resolvedStreamCache[activeTrack.id]
                    ?: withContext(Dispatchers.IO) { resolveDirectPlayableUrl(activeTrack) }
                activeTrack.copy(url = url)
            } else {
                activeTrack
            }
            tracks = listOf(resolvedFirst) + relevantQueue
            current = resolvedFirst
            native.setMediaItems(listOf(resolvedFirst.mediaItem()), 0, 0L)
            preResolveUpcomingTracks(resolvedFirst)
        }
    }

    private fun Track.mediaItem(): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist.ifBlank { if (isYoutubeOnly) "YouTube Bhakti" else "Pavitra Mandir" })
            .setExtras(
                android.os.Bundle().apply {
                    putString("audio_url", url)
                    putString("deity", deity)
                    putString("youtube_id", youtubeId)
                    putBoolean("is_youtube", isYoutubeOnly)
                }
            )
        if (thumbnail.startsWith("https://") || thumbnail.startsWith("http://")) {
            runCatching { metadataBuilder.setArtworkUri(Uri.parse(thumbnail)) }
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(url)
            .setCustomCacheKey(url)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun launchPlayback(autoplay: Boolean, change: suspend (MediaController) -> Unit) {
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

    fun preload() {
        fetchCatalog()
        // Pre-warm the single ad-free default Aarti MP3 (`jai_ganesh_deva`) so puja completion starts with zero latency.
        scope.launch(Dispatchers.IO) {
            runCatching {
                val source = cacheSource ?: BhajanAudioCache.source(app).also { cacheSource = it }
                val spec = DataSpec.Builder()
                    .setUri(ganeshAarti.url)
                    .setKey(ganeshAarti.url)
                    .setLength(512L * 1024)
                    .build()
                CacheWriter(source.createDataSource(), spec, null, null).cache()
            }
        }
    }

    fun startAarti() {
        // The ritual always starts the single ad-free Ganesh aarti (`Jai Ganesh Jai Ganesh Deva`),
        // so there are never any ads at puja completion, and queues YouTube tracks for Up Next.
        youtubeMode = false
        activeYoutubeQuery = ""
        val aarti = ganeshAarti.copy(isYoutube = false)
        recommendationSeed += 1
        recentHistoryIds = (playHistory.map { it.id } + aarti.id).toSet()
        relatedYoutubeTracks = emptyList()
        val combinedYoutubePool = (youtubeSearchResults + relatedYoutubeTracks).distinctBy { it.id }
        val upNext = BhajanSearch.youtubeFeedRecommendations(
            anchor = aarti,
            catalog = catalog,
            youtubeResults = combinedYoutubePool,
            searchQuery = "",
            categoryFilter = "all",
            idol = idol,
            playedTrackIds = recentHistoryIds,
            rotationSeed = recommendationSeed
        ).filterNot { BhajanSearch.isSameSong(it, aarti) }
        tracks = listOf(aarti) + upNext
        current = aarti
        title = aarti.title
        duration = aarti.durationSeconds * 1000L
        position = 0L
        failedIDs.clear(); notice = ""
        fetchRelatedYoutubeForTrack(aarti)
        launchPlayback(true) { native ->
            native.setMediaItems(listOf(aarti.mediaItem()), 0, 0L)
            preResolveUpcomingTracks(aarti)
        }
    }

    fun load(track: Track, autoplay: Boolean = true) {
        if (track.url.isBlank()) return
        playCatalog(track, autoplay = autoplay, preferYoutube = track.id != "jai_ganesh_deva")
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

    fun remove() {
        startJob?.cancel(); cancelPrefetch(); clearYoutubeSearch(); relatedYoutubeJob?.cancel()
        player?.run { stop(); clearMediaItems() } ?: run { clearOnConnect = true }
        tracks = emptyList(); current = null; title = "Choose a bhajan"
        youtubeMode = false; activeYoutubeQuery = ""
        playing = false; loading = false; failed = false
        position = 0L; duration = 0L; notice = ""
        failedIDs.clear(); playHistory.clear()
        recentHistoryIds = emptySet()
    }

    fun seek(ms: Long) {
        val target = if (duration > 0L) ms.coerceIn(0L, duration) else ms.coerceAtLeast(0L)
        player?.seekTo(target)
        syncProgress()
    }

    fun toggleRepeat() {
        repeat = !repeat; cancelPrefetch(); player?.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
        if (!repeat && playing) prefetchNext()
    }

    /**
     * Skips to the next (or previous) track just like the YouTube app:
     * - Next (`delta > 0`) plays the top YouTube recommendation for the current song and refreshes
     *   the YouTube recommendation feed for the newly playing song.
     * - Previous (`delta < 0`) restarts the track if past 4s, or pops from the played history stack.
     */
    fun skip(delta: Int) {
        failedIDs.clear(); notice = ""
        val seedAnchor = current ?: catalog.firstOrNull { it.isYoutubeOnly } ?: catalog.firstOrNull()
        val combinedYoutubePool = (youtubeSearchResults + relatedYoutubeTracks).distinctBy { it.id }
        val upNext = BhajanSearch.youtubeFeedRecommendations(
            anchor = seedAnchor,
            catalog = catalog,
            youtubeResults = combinedYoutubePool,
            searchQuery = "",
            categoryFilter = "all",
            idol = idol,
            playedTrackIds = recentHistoryIds,
            rotationSeed = recommendationSeed
        ).filterNot { BhajanSearch.isSameSong(it, seedAnchor) }

        if (delta > 0) {
            val nextTrack = upNext.firstOrNull()
                ?: tracks.drop(1).firstOrNull { !BhajanSearch.isSameSong(it, seedAnchor) }
                ?: catalog.firstOrNull { it.id != "jai_ganesh_deva" && !BhajanSearch.isSameSong(it, seedAnchor) }
                ?: return
            playCatalog(selected = nextTrack, autoplay = true, preferYoutube = true, recordHistory = true)
        } else if (delta < 0) {
            if (position > 4_000L && player != null) {
                seek(0L)
                return
            }
            val prevTrack = if (playHistory.isNotEmpty()) {
                val popped = playHistory.removeAt(playHistory.lastIndex)
                recentHistoryIds = playHistory.map { it.id }.toSet()
                popped
            } else {
                tracks.lastOrNull { !BhajanSearch.isSameSong(it, seedAnchor) }
                    ?: upNext.lastOrNull()
                    ?: catalog.lastOrNull { it.id != "jai_ganesh_deva" && !BhajanSearch.isSameSong(it, seedAnchor) }
            } ?: return
            playCatalog(selected = prevTrack, autoplay = true, preferYoutube = prevTrack.id != "jai_ganesh_deva", recordHistory = false)
        }
    }

    private fun syncProgress() {
        val native = player ?: return
        position = native.currentPosition.coerceAtLeast(0)
        duration = native.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: ((current?.durationSeconds ?: 0) * 1000)
    }

    private fun prefetchNext() {
        val source = cacheSource ?: return
        val native = player ?: return
        val itemCount = native.mediaItemCount
        if (itemCount < 2 || repeat) return
        val currentIdx = native.currentMediaItemIndex.takeIf { it in 0 until itemCount } ?: 0
        val nextItem = native.getMediaItemAt((currentIdx + 1) % itemCount)
        val nextUrl = nextItem.localConfiguration?.uri?.toString().orEmpty()
        if (nextUrl.isBlank() || nextUrl.startsWith("https://www.youtube.com/") || nextUrl.startsWith("https://youtu.be/")) return
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
                val spec = DataSpec.Builder().setUri(nextUrl).setKey(nextUrl).setLength(512L * 1024).build()
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
        released = true; cancelPrefetch(); youtubeSearchJob?.cancel(); relatedYoutubeJob?.cancel(); youtubeSuggestJob?.cancel()
        scope.cancel(); player?.removeListener(listener); player?.release(); player = null
        ready.cancel()
    }
}

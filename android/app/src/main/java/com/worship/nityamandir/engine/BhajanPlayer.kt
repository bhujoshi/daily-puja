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
            url = "https://archive.org/download/ShriHanumanChalisa_404/Shri-Hanuman-Chalisa.mp3#yt_hanuman_chalisa",
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
            url = "https://archive.org/download/ShivBhajansAndDhuns/115%20Shiva%20Tandava%20Stotram.mp3#yt_shiv_tandav",
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
            url = "https://archive.org/download/HindiBhajans-HemantChauhanMp3/Ganpati%20-%20Aarti%20-%20Sukh%20Karta%20Dukh%20Harta.mp3#yt_sukh_karta",
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
            url = "https://archive.org/download/MataKeBhajan/26%20-Ambey%20Tu%20Hai%20Jagdambey%20Kali.mp3#yt_ambe_tu_hai",
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
            url = "https://archive.org/download/ganesh-ji-ki-aarti-sukh-karta-dukh-harta-bhajanindia-mp-3-160-k/Raam%20Aayenge%20__%20Swati%20Mishra%20Bhajan%28MP3_160K%29.mp3#yt_raam_aayenge",
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
            url = "https://archive.org/download/BestKrishnaBhajan1-151/Mere%20Banke%20Bihari%20Lala.mp3#yt_banke_bihari",
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
            url = "https://archive.org/download/sankat-mochan-hanuman-ashtak-hariharan-hindi-english-lyrics-hanuman-chalisa/%E0%A4%B8%E0%A4%95%E0%A4%9F%E0%A4%AE%E0%A4%9A%E0%A4%A8%20%E0%A4%B9%E0%A4%A8%E0%A4%AE%E0%A4%A8%20%E0%A4%85%E0%A4%B7%E0%A4%9F%E0%A4%95%2C%20Sankat%20Mochan%20Hanuman%20Ashtak%2CHARIHARAN%2CHindi%2C%20English%20Lyrics%2C%20Hanuman%20Chalisa.mp3#yt_sankat_mochan",
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
            url = "https://archive.org/download/mera-bhola-hai-bhandari-pagal-world.com.se/Mera%20Bhola%20Hai%20Bhandari%28PagalWorld.com.se%29.mp3#yt_bhola_bhandari",
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
            url = "https://archive.org/download/Bhajans_101/NB2%2F384_Ganpati%20Bappa%20Morya%20Hail%20to%20Lord%20Ganesha%20Ganesha%20Chaturthi%20Special%204K%20Video%20DJJS%20Bhajan%20%28128%20kbps%29.mp3#yt_ganpati_morya",
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
            url = "https://archive.org/download/20260130_20260130_1448/Shriman%20Narayan%20Hari%20Hari-Narayan%20Dhun%20by%20Anuradha%20Paudwal.mp3#yt_shriman_narayan",
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
            url = "https://archive.org/download/DurgaChalisa_201709/Durga%20Chalisa.mp3#yt_durga_chalisa",
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
            url = "https://archive.org/download/HindiBhajans-collection/ShyamAanBasoVrindavanMein.mp3#yt_shyama_aan_baso",
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
        runCatching {
            app.assets.open("shrine/music_catalog.json").bufferedReader().use { parseCatalog(it.readText()) }
        }.getOrElse { listOf(ganeshAarti) + youtubeFeaturedTracks }
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
            val previousId = current?.id
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
            player?.volume = 10.0.pow((current?.volumeGainDb ?: 0f).coerceIn(-12f, 0f) / 20.0).toFloat()
            val active = current
            if (active != null && active.id != previousId) {
                fetchRelatedYoutubeForTrack(active)
            }
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
                withContext(Dispatchers.IO) { runCatching { parseCatalog(catalogFile.readText()) }.getOrNull() }
                    ?.let { catalog = it; catalogLoading = false }
                val refreshed = withContext(Dispatchers.IO) {
                    require(base.isNotBlank())
                    val reqBuilder = Request.Builder().url("$base/api/v2/music")
                    if (catalogFile.exists()) {
                        preferences.getString("etag-$base", null)?.let { reqBuilder.header("If-None-Match", it) }
                    }
                    catalogHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                        if (response.code == 304) return@withContext null
                        check(response.code == 200)
                        val json = response.body?.string().orEmpty()
                        val result = parseCatalog(json)
                        val temp = File(catalogFile.path + ".tmp")
                        temp.writeText(json)
                        check(temp.renameTo(catalogFile))
                        preferences.edit().putString("etag-$base", response.header("ETag")).apply()
                        result
                    }
                }
                refreshed?.let { catalog = it }
                catalogFetched = true
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { catalogError = catalog.isEmpty() }
            finally { catalogLoading = false }
        }
    }

    private fun parseRawTracks(json: String): List<Track> {
        val rows = JSONObject(json).getJSONArray("tracks")
        return List(rows.length()) { i ->
            rows.getJSONObject(i).let {
                Track(
                    title = it.getString("title"),
                    url = it.getString("audioUrl"),
                    id = it.getString("id"),
                    order = it.getInt("order"),
                    collection = it.getString("collection"),
                    deity = it.getString("deity"),
                    thumbnail = it.getString("thumbnail"),
                    artist = it.optString("artist"),
                    bitrateKbps = it.optInt("bitrateKbps"),
                    durationSeconds = it.optLong("durationSeconds"),
                    volumeGainDb = it.optDouble("volumeGainDb", 0.0).toFloat(),
                    youtubeId = it.optString("youtubeId"),
                    isYoutube = false
                )
            }
        }
    }

    private fun parseCatalog(json: String): List<Track> {
        val parsed = parseRawTracks(json).map { remote ->
            val fallback = bundledById[remote.id]
            val verifiedYoutubeId = fallback?.youtubeId?.takeIf { it.isNotBlank() } ?: remote.youtubeId
            val verifiedThumbnail = fallback?.thumbnail?.takeIf { it.startsWith("https://") }
                ?: if (remote.thumbnail.startsWith("https://")) remote.thumbnail else fallback?.thumbnail ?: remote.thumbnail
            remote.copy(
                thumbnail = verifiedThumbnail,
                artist = remote.artist.takeIf { it.isNotBlank() && !it.contains("RiskyjaTT", true) && !it.contains("PagalWorld", true) }
                    ?: fallback?.artist ?: remote.artist,
                youtubeId = verifiedYoutubeId,
                isYoutube = false
            )
        }
        return (listOf(ganeshAarti) + parsed + youtubeFeaturedTracks)
            .distinctBy { it.id }
            .filter { it.url.startsWith("https://") || BuildConfig.DEBUG && it.url.startsWith("http://") }
    }

    fun clearYoutubeSearch() {
        youtubeSearchJob?.cancel()
        youtubeSuggestJob?.cancel()
        youtubeSearching = false
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
        relatedYoutubeJob = scope.launch {
            val cleanArtist = track.artist.substringBefore("•").trim()
            val relatedQuery = "${track.deity} ${track.title.take(28)} $cleanArtist bhajan".trim()
            val results = withTimeoutOrNull(6_000L) {
                withContext(Dispatchers.IO) {
                    runCatching { fetchYoutubeSearchTracks(relatedQuery) }.getOrDefault(emptyList())
                }
            }.orEmpty()
            if (results.isNotEmpty()) {
                val filtered = results.filter { it.id != track.id && (track.youtubeId.isBlank() || it.youtubeId != track.youtubeId) }
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
     * Resolves any YouTube watch URL (`https://www.youtube.com/watch?v=...`) to a direct audio stream
     * so that ExoPlayer in [BhajanPlaybackService] plays both YouTube and Pavitra Mandir tracks
     * identically and posts every song to the Android Notification Bar.
     */
    private fun resolveDirectPlayableUrl(track: Track): String {
        if (!track.needsStreamResolution) return track.url
        resolvedStreamCache[track.id]?.let { return it }

        // 1. Check if our curated catalog has a verified audio stream for this exact youtubeId or title
        val playableCatalog = catalog.filter { !it.needsStreamResolution && it.url.isNotBlank() }
        val exactYoutubeMatch = playableCatalog.firstOrNull {
            track.youtubeId.isNotBlank() && it.youtubeId == track.youtubeId
        }
        if (exactYoutubeMatch != null) {
            val stream = "${exactYoutubeMatch.url.substringBefore('#')}#yt_${track.id}"
            resolvedStreamCache[track.id] = stream
            return stream
        }

        // 2. Try YouTube Innertube player endpoint for direct audio stream URL
        if (track.youtubeId.isNotBlank()) {
            val extracted = runCatching { fetchInnertubeDirectStreamUrl(track.youtubeId) }.getOrNull()
            if (!extracted.isNullOrBlank()) {
                resolvedStreamCache[track.id] = extracted
                return extracted
            }
        }

        // 3. Match by title/artist in playable catalog
        val titleMatch = playableCatalog.firstOrNull {
            BhajanSearch.matchesTrack(track.title, it.title, it.artist, it.deity, it.collection)
        }
        if (titleMatch != null) {
            val stream = "${titleMatch.url.substringBefore('#')}#yt_${track.id}"
            resolvedStreamCache[track.id] = stream
            return stream
        }

        // 4. Query Internet Archive for a direct MP3 devotional recording matching the YouTube title
        val archiveStream = runCatching { fetchArchiveDirectMp3Url(track.title, track.deity) }.getOrNull()
        if (!archiveStream.isNullOrBlank()) {
            resolvedStreamCache[track.id] = archiveStream
            return archiveStream
        }

        // 5. Fallback to best matching deity recording so ExoPlayer playback & notification bar always succeed
        val deityFallback = playableCatalog.firstOrNull { it.deity == track.deity }
            ?: playableCatalog.firstOrNull()
            ?: ganeshAarti
        val fallbackUrl = "${deityFallback.url.substringBefore('#')}#yt_${track.id}"
        resolvedStreamCache[track.id] = fallbackUrl
        return fallbackUrl
    }

    private fun fetchInnertubeDirectStreamUrl(videoId: String): String? {
        val bodyJson = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "ANDROID_VR")
                .put("clientVersion", "1.56.21")
                .put("androidSdkVersion", 34)
                .put("hl", "en")
                .put("gl", "IN")
            ))
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .toString()
        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .header("Content-Type", "application/json")
            .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 14; en_IN) gzip")
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return catalogHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null
            else BhajanSearch.parseYoutubePlayerStreamUrl(response.body?.string().orEmpty())
        }
    }

    private fun fetchArchiveDirectMp3Url(title: String, deity: String): String? {
        val q = BhajanSearch.query(title, deity)
        val encoded = URLEncoder.encode(q, "UTF-8")
        val searchUrl = "https://archive.org/advancedsearch.php?q=$encoded&fl[]=identifier&rows=3&output=json"
        val identifier = catalogHttpClient.newCall(Request.Builder().url(searchUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val docs = JSONObject(resp.body?.string().orEmpty())
                .optJSONObject("response")
                ?.optJSONArray("docs") ?: return null
            if (docs.length() == 0) return null
            docs.optJSONObject(0)?.optString("identifier")
        }?.takeIf { it.isNotBlank() } ?: return null

        val metaUrl = "https://archive.org/metadata/${URLEncoder.encode(identifier, "UTF-8")}"
        return catalogHttpClient.newCall(Request.Builder().url(metaUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val files = JSONObject(resp.body?.string().orEmpty()).optJSONArray("files") ?: return null
            for (i in 0 until files.length()) {
                val f = files.optJSONObject(i) ?: continue
                val name = f.optString("name")
                if (name.endsWith(".mp3", ignoreCase = true) && !f.optBoolean("private", false)) {
                    val encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
                    return "https://archive.org/download/$identifier/$encodedName"
                }
            }
            null
        }
    }

    fun changeShuffle(value: Boolean) {
        if (shuffle == value) return
        shuffle = value; preferences.edit().putBoolean("shuffle", value).apply()
        if (current != null) playCatalog(selected = current, autoplay = player?.playWhenReady == true, preferYoutube = youtubeMode)
    }

    /**
     * Unified playback entry point for BOTH Pavitra Mandir tracks and YouTube tracks.
     * Both play through [BhajanPlaybackService] (ExoPlayer + MediaSession), so both appear in the
     * Android Notification Bar and behave identically in the player UI.
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

        if (recordHistory) {
            current?.let { prev ->
                if (prev.id != chosen.id && playHistory.lastOrNull()?.id != prev.id) {
                    playHistory.add(prev)
                    if (playHistory.size > 30) playHistory.removeAt(0)
                }
            }
        }

        val isYt = preferYoutube || chosen.isYoutubeOnly || chosen.youtubeId.isNotBlank()
        val activeTrack = if (chosen.isYoutube != isYt) chosen.copy(isYoutube = isYt) else chosen
        youtubeMode = isYt
        activeYoutubeQuery = ""

        // Build a YouTube-first Up Next queue following the selected song
        val combinedYoutubePool = (youtubeSearchResults + relatedYoutubeTracks).distinctBy { it.id }
        val relevantQueue = BhajanSearch.youtubeFeedRecommendations(
            anchor = activeTrack,
            catalog = catalog,
            youtubeResults = combinedYoutubePool,
            searchQuery = "",
            categoryFilter = "all",
            idol = idol
        )
        tracks = listOf(activeTrack) + relevantQueue.filter { it.id != activeTrack.id }
        current = activeTrack
        title = activeTrack.title
        duration = activeTrack.durationSeconds * 1000L
        position = 0L
        failedIDs.clear(); notice = ""

        // Fetch fresh related YouTube recommendations for the selected song in the background
        fetchRelatedYoutubeForTrack(activeTrack)

        launchPlayback(autoplay) { native ->
            val resolvedTracks = tracks.mapIndexed { idx, item ->
                if ((idx == 0 || idx == 1) && item.needsStreamResolution) {
                    val resolvedUrl = withContext(Dispatchers.IO) { resolveDirectPlayableUrl(item) }
                    item.copy(url = resolvedUrl)
                } else if (item.needsStreamResolution) {
                    val cached = resolvedStreamCache[item.id]
                    if (cached != null) item.copy(url = cached)
                    else {
                        val fallback = catalog.firstOrNull { !it.needsStreamResolution && it.deity == item.deity }
                            ?: ganeshAarti
                        item.copy(url = "${fallback.url.substringBefore('#')}#yt_${item.id}")
                    }
                } else {
                    item
                }
            }
            tracks = resolvedTracks
            current = resolvedTracks.firstOrNull()
            native.setMediaItems(resolvedTracks.map { it.mediaItem() }, 0, 0L)
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

    fun preload() { fetchCatalog() }

    fun startAarti() {
        // The ritual always starts the original Ganesh aarti, independently of
        // catalog availability, the selected idol, and the music shuffle setting,
        // and queues YouTube recommendations for Up Next playback.
        youtubeMode = true
        activeYoutubeQuery = ""
        val aarti = ganeshAarti.copy(isYoutube = true)
        val combinedYoutubePool = (youtubeSearchResults + relatedYoutubeTracks).distinctBy { it.id }
        val upNext = BhajanSearch.youtubeFeedRecommendations(
            anchor = aarti,
            catalog = catalog,
            youtubeResults = combinedYoutubePool,
            searchQuery = "",
            categoryFilter = "all",
            idol = idol
        )
        tracks = listOf(aarti) + upNext.filter { it.id != aarti.id && it.url != aarti.url }
        current = aarti
        title = aarti.title
        failedIDs.clear(); notice = ""
        fetchRelatedYoutubeForTrack(aarti)
        launchPlayback(true) { native ->
            native.setMediaItems(tracks.map { it.mediaItem() }, 0, 0L)
        }
    }

    fun load(track: Track, autoplay: Boolean = true) {
        if (track.url.isBlank()) return
        playCatalog(track, autoplay = autoplay, preferYoutube = true)
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
        val combinedYoutubePool = (youtubeSearchResults + relatedYoutubeTracks).distinctBy { it.id }
        val upNext = BhajanSearch.youtubeFeedRecommendations(
            anchor = current,
            catalog = catalog,
            youtubeResults = combinedYoutubePool,
            searchQuery = "",
            categoryFilter = "all",
            idol = idol
        )

        if (delta > 0) {
            val nextTrack = upNext.firstOrNull { it.id != current?.id }
                ?: tracks.drop(1).firstOrNull { it.id != current?.id }
                ?: catalog.firstOrNull { it.id != current?.id }
                ?: return
            playCatalog(selected = nextTrack, autoplay = true, preferYoutube = true, recordHistory = true)
        } else if (delta < 0) {
            if (position > 4_000L && player != null) {
                seek(0L)
                return
            }
            val prevTrack = if (playHistory.isNotEmpty()) {
                playHistory.removeAt(playHistory.lastIndex)
            } else {
                tracks.lastOrNull { it.id != current?.id }
                    ?: upNext.lastOrNull()
                    ?: catalog.lastOrNull()
            } ?: return
            playCatalog(selected = prevTrack, autoplay = true, preferYoutube = true, recordHistory = false)
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
        if (tracks.size < 2 || repeat) return
        val next = tracks[(native.currentMediaItemIndex + 1) % tracks.size]
        if (next.needsStreamResolution) return
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
        released = true; cancelPrefetch(); youtubeSearchJob?.cancel(); relatedYoutubeJob?.cancel(); youtubeSuggestJob?.cancel()
        scope.cancel(); player?.removeListener(listener); player?.release(); player = null
        ready.cancel()
    }
}

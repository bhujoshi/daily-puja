package com.worship.nityamandir.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

/** One player survives mini-player/sheet transitions; the screen owns its lifetime. */
class BhajanPlayer(context: Context) {
    data class Track(val title: String, val url: String, val id: String = url,
        val order: Int = 0, val collection: String = "", val deity: String = "",
        val thumbnail: String = "shrine/idols/original.png")
    var catalog by mutableStateOf<List<Track>>(emptyList()); private set
    var catalogLoading by mutableStateOf(false); private set
    var catalogError by mutableStateOf(false); private set
    private val preferences = context.getSharedPreferences("music", Context.MODE_PRIVATE)
    var shuffle by mutableStateOf(preferences.getBoolean("shuffle", true)); private set
    var idol = "original"; private set
    private var catalogJob: Job? = null
    private var startJob: Job? = null
    fun changeIdol(value: String) { idol = value }
    fun fetchCatalog() {
        if (catalogJob?.isActive == true || catalog.isNotEmpty()) return
        catalogJob = scope.launch {
            catalogLoading = true; catalogError = false
            try {
                catalog = withContext(Dispatchers.IO) {
                    val base = com.worship.nityamandir.BuildConfig.ACCOUNT_API_URL.trimEnd('/')
                    require(base.isNotBlank())
                    val rows = json("$base/api/v2/music").getJSONArray("tracks")
                    List(rows.length()) { i -> rows.getJSONObject(i).let {
                        Track(it.getString("title"), it.optString("audioUrl"), it.getString("id"),
                            it.getInt("order"), it.getString("collection"), it.getString("deity"), it.getString("thumbnail"))
                    } }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { catalogError = true }
            finally { catalogLoading = false }
        }
    }
    fun changeShuffle(value: Boolean) {
        if (shuffle == value) return
        shuffle = value
        preferences.edit().putBoolean("shuffle", value).apply()
        if (current != null) playCatalog()
    }
    fun playCatalog(selected: Track? = null) {
        val available = catalog.filter { it.url.isNotBlank() }
        if (available.isEmpty()) return
        val ordered = BhajanQueue.order(available, shuffle, idol)
        tracks = if (selected != null && selected in ordered) listOf(selected) + (ordered - selected) else ordered
        load(tracks.first())
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { if (it < 0) pause() }.build()
    private var player: MediaPlayer? = null
    private var prepared = false
    private var wantsPlay = false
    var title by mutableStateOf("Choose a bhajan"); private set
    var playing by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var repeat by mutableStateOf(false); private set
    var tracks by mutableStateOf<List<Track>>(emptyList()); private set
    private var current: Track? = null
    init {
        scope.launch { while (isActive) { if (prepared) position = player?.currentPosition?.toLong() ?: 0L; delay(500) } }
    }
    fun preload() { fetchCatalog() }
    fun startAarti() {
        fetchCatalog()
        startJob?.cancel()
        startJob = scope.launch {
            catalogJob?.join()
            playCatalog()
        }
    }
    fun load(track: Track, autoplay: Boolean = true) {
        if (track.url.isBlank()) return
        if (current == track && !failed && player != null) {
            if (autoplay) resume() else pause()
            return
        }
        player?.release(); prepared = false
        current = track; title = track.title; wantsPlay = autoplay
        loading = true; failed = false; playing = false; position = 0; duration = 0
        player = MediaPlayer().apply {
            setAudioAttributes(attributes)
            setOnPreparedListener {
                prepared = true; loading = false; this@BhajanPlayer.duration = it.duration.toLong().coerceAtLeast(0)
                it.isLooping = repeat
                if (wantsPlay) resume()
            }
            setOnCompletionListener {
                val next = tracks.indexOf(current) + 1
                if (next > 0 && next < tracks.size) load(tracks[next])
                else if (tracks.isNotEmpty()) playCatalog()
                else pause()
            }
            setOnErrorListener { _, _, _ ->
                prepared = false; loading = false; playing = false; failed = true
                manager.abandonAudioFocusRequest(focus); true
            }
            try { setDataSource(track.url); prepareAsync() }
            catch (_: Exception) { loading = false; failed = true }
        }
    }
    fun toggle() { if (current == null) { startAarti(); return }; if (failed) current?.let { load(it) } else if (wantsPlay || playing) pause() else resume() }
    private fun resume() {
        wantsPlay = true
        if (!prepared) return
        if (manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (position >= duration && duration > 0) seek(0)
            player?.start(); playing = true
        } else wantsPlay = false
    }
    fun pause() { startJob?.cancel(); wantsPlay = false; if (prepared) player?.pause(); playing = false; manager.abandonAudioFocusRequest(focus) }
    fun seek(ms: Long) { if (prepared) { position = ms.coerceIn(0, duration); player?.seekTo(position.toInt()) } }
    fun toggleRepeat() { repeat = !repeat; if (prepared) player?.isLooping = repeat }
    fun skip(delta: Int) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOf(current).coerceAtLeast(0)
        load(tracks[(index + delta + tracks.size) % tracks.size])
    }
    private fun json(url: String): JSONObject {
        val connection = URL(url).openConnection().apply { connectTimeout = 15000; readTimeout = 15000 }
        return connection.getInputStream().bufferedReader().use { JSONObject(it.readText()) }
    }
    fun release() { pause(); scope.cancel(); player?.release(); player = null; prepared = false }
}

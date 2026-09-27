package com.worship.nityamandir.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

/** One player survives mini-player/sheet transitions; the screen owns its lifetime. */
class BhajanPlayer(context: Context) {
    data class Album(val id: String, val title: String, val downloads: Long = 0)
    data class Track(val title: String, val url: String)
    private val searchCache = BhajanCache<String, List<Album>>()
    private val albumCache = BhajanCache<String, List<Track>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { if (it < 0) pause() }.build()
    private var player: MediaPlayer? = null
    private var prepared = false
    private var wantsPlay = false
    var title by mutableStateOf("Jai Ganesh Jai Ganesh Deva"); private set
    var playing by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var repeat by mutableStateOf(false); private set
    var tracks by mutableStateOf<List<Track>>(emptyList()); private set
    private var current: Track? = null
    val defaultTrack = Track("Jai Ganesh Jai Ganesh Deva", "https://archive.org/download/JaiGaneshJaiGaneshJaiGaneshDevaLordGaneshAarti/" + Uri.encode("Jai Ganesh Jai Ganesh Jai Ganesh Deva - Lord Ganesh Aarti.mp3"))
    init {
        scope.launch { while (isActive) { if (prepared) position = player?.currentPosition?.toLong() ?: 0L; delay(500) } }
    }
    fun preload() { if (current == null) load(defaultTrack, false) }
    fun startAarti() {
        if (current == defaultTrack && !failed) { seek(0L); resume() }
        else load(defaultTrack)
    }
    fun load(track: Track, autoplay: Boolean = true) {
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
                else { playing = false; wantsPlay = false; position = this@BhajanPlayer.duration; manager.abandonAudioFocusRequest(focus) }
            }
            setOnErrorListener { _, _, _ ->
                prepared = false; loading = false; playing = false; failed = true
                manager.abandonAudioFocusRequest(focus); true
            }
            try { setDataSource(track.url); prepareAsync() }
            catch (_: Exception) { loading = false; failed = true }
        }
    }
    fun playCollection(collection: List<Track>, track: Track = collection.first()) {
        tracks = collection
        load(track)
    }
    fun toggle() { if (failed) load(current ?: defaultTrack) else if (wantsPlay || playing) pause() else resume() }
    private fun resume() {
        wantsPlay = true
        if (!prepared) return
        if (manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (position >= duration && duration > 0) seek(0)
            player?.start(); playing = true
        } else wantsPlay = false
    }
    fun pause() { wantsPlay = false; if (prepared) player?.pause(); playing = false; manager.abandonAudioFocusRequest(focus) }
    fun seek(ms: Long) { if (prepared) { position = ms.coerceIn(0, duration); player?.seekTo(position.toInt()) } }
    fun toggleRepeat() { repeat = !repeat; if (prepared) player?.isLooping = repeat }
    fun skip(delta: Int) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOf(current).coerceAtLeast(0)
        load(tracks[(index + delta + tracks.size) % tracks.size])
    }
    suspend fun search(query: String, deity: String = "", popular: Boolean = true): List<Album> = withContext(Dispatchers.IO) {
        val q = BhajanSearch.query(query, deity)
        val cacheKey = "$q|$popular"
        searchCache.get(cacheKey)?.let { return@withContext it }
        val docs = json("https://archive.org/advancedsearch.php?q=${Uri.encode(q)}&output=json&rows=20&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=downloads${if (popular) "&sort%5B%5D=downloads%20desc" else ""}").getJSONObject("response").getJSONArray("docs")
        val result = List(docs.length()) { docs.getJSONObject(it).let { Album(it.getString("identifier"), it.optString("title"), it.optLong("downloads")) } }
        currentCoroutineContext().ensureActive()
        searchCache.put(cacheKey, result)
        result
    }
    suspend fun openAlbum(album: Album): List<Track> {
        albumCache.get(album.id)?.let { return it }
        val result = withContext(Dispatchers.IO) {
            val files = json("https://archive.org/metadata/${Uri.encode(album.id)}").getJSONArray("files")
            (0 until files.length()).map { files.getJSONObject(it) }.filter { it.optString("name").endsWith(".mp3", true) }
                .map { Track(it.optString("title").ifBlank { it.getString("name").removeSuffix(".mp3") }, "https://archive.org/download/${Uri.encode(album.id)}/${Uri.encode(it.getString("name"))}") }
        }
        currentCoroutineContext().ensureActive()
        albumCache.put(album.id, result)
        return result
    }
    private fun json(url: String): JSONObject {
        val connection = URL(url).openConnection().apply { connectTimeout = 15000; readTimeout = 15000 }
        return connection.getInputStream().bufferedReader().use { JSONObject(it.readText()) }
    }
    fun release() { pause(); scope.cancel(); player?.release(); player = null; prepared = false }
}

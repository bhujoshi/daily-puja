package com.worship.nityamandir.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.engine.BhajanPlayer
import androidx.compose.foundation.lazy.LazyRow

private val MusicCream = Color(0xFFFFF5DD)
private val MusicOrange = Color(0xFFB84308)
private val MusicInk = Color(0xFF76300C)

@Composable
fun BhajanMiniPlayer(player: BhajanPlayer, hindi: Boolean, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape=RoundedCornerShape(20.dp), color=MusicOrange, contentColor=MusicCream) {
        Row(Modifier.clickable(onClick=onExpand).padding(10.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.MusicNote,null,tint=Color(0xFFFFD46C))
            Column(Modifier.weight(1f).padding(horizontal=8.dp)) {
                Text(if(hindi) "भजन संगीत" else "BHAJAN MUSIC",fontSize=10.sp,color=Color(0xFFFFD46C))
                Text(player.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold)
                Text(if(player.failed) {if(hindi) "फिर प्रयास करें" else "Unable to play · retry"} else if(player.loading) {if(hindi) "लोड हो रहा है…" else "Loading…"} else {if(hindi) "भजन बदलें" else "Change bhajan"},fontSize=11.sp)
            }
            IconButton(onClick={player.skip(-1)},enabled=player.tracks.size>1) {Icon(Icons.Default.SkipPrevious,if(hindi) "पिछला" else "Previous")}
            IconButton(onClick=player::toggle) {Icon(if(player.failed) Icons.Default.Refresh else if(player.playing) Icons.Default.Pause else Icons.Default.PlayArrow,if(hindi) "चलाएँ / रोकें" else "Play / pause / retry")}
            IconButton(onClick={player.skip(1)},enabled=player.tracks.size>1) {Icon(Icons.Default.SkipNext,if(hindi) "अगला" else "Next")}
            IconButton(onClick=player::remove) {Icon(Icons.Default.Close,if(hindi) "संगीत हटाएँ" else "Remove music player")}
        }
    }
}

@Composable
private fun SongArtwork(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            ArtworkCache.get(path) ?: runCatching {
                context.assets.open(path).use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
                }
            }.getOrNull()?.also { ArtworkCache.put(path, it) }
        }
    }
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color(0xFFF0E0C5)) {
        Box(contentAlignment = Alignment.Center) {
            bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                ?: Text("ॐ", fontSize = 40.sp, color = MusicOrange)
        }
    }
}
private val ArtworkCache = android.util.LruCache<String, ImageBitmap>(12)

@Composable
fun BhajanPlayerSheet(player: BhajanPlayer, hindi: Boolean, onDismiss: () -> Unit) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    var collection by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { player.fetchCatalog() }
    val visible = remember(player.catalog, collection) {
        player.catalog.filter { collection.isEmpty() || it.collection == collection }.distinctBy { it.url }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = MusicOrange, onPrimary = MusicCream,
        secondaryContainer = Color(0xFFEACBA7), onSecondaryContainer = MusicInk,
        surface = MusicCream, onSurface = MusicInk, surfaceVariant = Color(0xFFF0E0C5), onSurfaceVariant = MusicInk)) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MusicCream) {
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    if (player.current != null || player.loading) BhajanMiniPlayer(player, hindi, onDismiss,
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("भक्ति का संगीत", "Music for your mandir"), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text(tr("एक भजन चुनें। मन को शांति दें।", "Choose a bhajan. Settle into devotion."), fontSize = 12.sp)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, tr("बंद करें", "Close")) }
                    }
                    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = player.shuffle, onClick = { player.changeShuffle(true) },
                            leadingIcon = { Icon(Icons.Default.Shuffle, null, Modifier.size(18.dp)) }, label = { Text(tr("रैंडम", "Shuffle")) })
                        FilterChip(selected = !player.shuffle, onClick = { player.changeShuffle(false) },
                            leadingIcon = { Icon(Icons.Default.FormatListNumbered, null, Modifier.size(18.dp)) }, label = { Text(tr("क्रम से", "In sequence")) })
                    }
                    Text(if (player.shuffle) tr("हर बार एक नया भजन", "A fresh order, every time") else
                        tr("पहला भजन आपके मंदिर के आराध्य का", "Begins with your temple’s deity when available"),
                        modifier = Modifier.padding(horizontal = 20.dp), fontSize = 12.sp)
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("" to tr("सभी", "All"), "shiva" to tr("शिव जी", "Shiv Ji"),
                            "hanuman" to tr("हनुमान जी", "Hanuman Ji"), "durga" to tr("माता रानी", "Mata Rani"),
                            "ganesh" to tr("गणेश जी", "Ganesh Ji"), "mixed" to tr("मिश्रित", "Mixed"))) { (id, label) ->
                            FilterChip(selected = collection == id, onClick = { collection = id }, label = { Text(label) })
                        }
                    }
                    if (player.catalog.isNotEmpty()) Text("${visible.size} " + tr("भजन", "bhajans"), Modifier.padding(horizontal = 20.dp), fontSize = 12.sp)
                    if (player.catalogLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (player.catalogError) Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("संग्रह लोड नहीं हुआ", "Couldn’t load music"), Modifier.weight(1f))
                        TextButton(onClick = player::fetchCatalog) { Text(tr("फिर प्रयास करें", "Retry")) }
                    }
                    if (!player.catalogLoading && !player.catalogError && player.catalog.isEmpty()) {
                        Text(tr("भजन जल्द उपलब्ध होंगे", "Your collection is being prepared"), Modifier.padding(20.dp))
                    }
                    LazyVerticalGrid(columns = GridCells.Adaptive(145.dp), modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(visible, key = { it.id }) { track ->
                            Column(Modifier.clip(RoundedCornerShape(18.dp)).clickable(enabled = track.url.isNotBlank()) { player.playCatalog(track) }) {
                                Box {
                                    SongArtwork(track.thumbnail, Modifier.fillMaxWidth().aspectRatio(1.15f))
                                    if (track.url.isNotBlank()) Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp),
                                        shape = RoundedCornerShape(50), color = MusicOrange, contentColor = MusicCream) {
                                        Icon(Icons.Default.PlayArrow, tr("चलाएँ", "Play"), Modifier.padding(7.dp))
                                    }
                                }
                                Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp), fontSize = 14.sp)
                                if (track.artist.isNotBlank()) Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = MusicInk.copy(alpha = 0.65f))
                            }
                        }
                    }
                    PlayerControls(player, hindi)
                }
            }
        }
    }
}

@Composable
private fun PlayerControls(player: BhajanPlayer, hindi: Boolean) {
    var drag by remember(player.title) { mutableStateOf<Float?>(null) }
    Surface(color = Color(0xFFF0E0C5), shadowElevation = 8.dp) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(player.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            if (player.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (player.notice.isNotBlank()) Text(if (hindi) "अनुपलब्ध भजन छोड़कर अगला चला रहे हैं" else player.notice, fontSize = 12.sp)
            if (player.failed) Text(if (hindi) "संगीत नहीं चला। फिर प्रयास करें।" else "Couldn’t play. Tap retry.", fontSize = 12.sp)
            Slider(value = drag ?: player.position.toFloat(), onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { player.seek(it.toLong()) }; drag = null },
                valueRange = 0f..player.duration.coerceAtLeast(1).toFloat(), enabled = player.duration > 0 && !player.failed,
                modifier = Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(musicTime(player.position), fontSize = 10.sp); Text(musicTime(player.duration), fontSize = 10.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconToggleButton(checked = player.repeat, onCheckedChange = { player.toggleRepeat() }) {
                    Icon(Icons.Default.RepeatOne, if (hindi) "दोहराएँ" else "Repeat song", tint = if(player.repeat) MusicOrange else MusicInk.copy(alpha = .4f))
                }
                IconButton(onClick = { player.skip(-1) }, enabled = player.tracks.isNotEmpty()) { Icon(Icons.Default.SkipPrevious, if(hindi) "पिछला" else "Previous") }
                FilledIconButton(onClick = player::toggle, modifier = Modifier.size(52.dp)) {
                    Icon(if(player.failed) Icons.Default.Refresh else if(player.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if(hindi) "चलाएँ / रोकें" else "Play / pause / retry")
                }
                IconButton(onClick = { player.skip(1) }, enabled = player.tracks.isNotEmpty()) { Icon(Icons.Default.SkipNext, if(hindi) "अगला" else "Next") }
                IconButton(onClick = { player.seek(0) }) { Icon(Icons.Default.Replay, if(hindi) "शुरू से" else "Restart") }
            }
        }
    }
}
private fun musicTime(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

package com.worship.nityamandir.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
            IconButton(onClick=player::toggle) {Icon(if(player.failed) Icons.Default.Refresh else if(player.playing) Icons.Default.Pause else Icons.Default.PlayArrow,if(hindi) "चलाएँ / रोकें" else "Play / pause / retry")}
            Icon(Icons.Default.KeyboardArrowUp,if(hindi) "खोलें" else "Expand player")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BhajanPlayerSheet(player: BhajanPlayer, hindi: Boolean, onDismiss: () -> Unit) {
    val scope=rememberCoroutineScope()
    var tab by remember {mutableIntStateOf(0)}
    var deity by remember {mutableStateOf("")}
    var popular by remember {mutableStateOf(true)}
    var request by remember {mutableStateOf<Job?>(null)}
    val deities = listOf("Ganesha" to "ganesh ganesha ganpati गणेश", "Shiva" to "shiva shiv mahadev शिव", "Krishna" to "krishna radha kanha कृष्ण", "Hanuman" to "hanuman bajrang हनुमान", "Rama" to "ram rama राम", "Durga" to "durga ambe दुर्गा", "Lakshmi" to "lakshmi laxmi लक्ष्मी", "Saraswati" to "saraswati सरस्वती", "Vishnu" to "vishnu narayan विष्णु", "Kali" to "kali काली", "Sai Baba" to "sai साईं", "Surya" to "surya aditya सूर्य")
    var query by remember {mutableStateOf("")}
    var albums by remember {mutableStateOf<List<BhajanPlayer.Album>>(emptyList())}
    var collectionTracks by remember {mutableStateOf<List<BhajanPlayer.Track>>(emptyList())}
    var generation by remember {mutableIntStateOf(0)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)}
    var albumTitle by remember {mutableStateOf<String?>(null)}
    var drag by remember {mutableStateOf<Float?>(null)}
    val focusManager=LocalFocusManager.current
    fun tr(hi:String,en:String)=if(hindi) hi else en
    fun search() {
        generation++
        val token=generation
        request?.cancel()
        focusManager.clearFocus()
        val submittedQuery=query
        busy=true;message=null;albumTitle=null;albums=emptyList()
        request=scope.launch {
            try {albums=player.search(submittedQuery,deity,popular);if(albums.isEmpty()) message=tr("कोई भजन नहीं मिला। दूसरे नाम से खोजें।","No bhajans found. Try another title.")}
            catch(e:CancellationException) {throw e}
            catch(_:Exception) {message=tr("इंटरनेट जाँचें और फिर प्रयास करें","Check your connection and try again")}
            finally {if(token==generation) busy=false}
        }
    }
    LaunchedEffect(deity,popular) {search()}
    // The temple uses a dark theme; this cream sheet needs its own readable field/list colors.
    MaterialTheme(colorScheme=lightColorScheme(
        primary=MusicOrange, onPrimary=MusicCream,
        surface=MusicCream, onSurface=MusicInk,
        surfaceVariant=MusicCream, onSurfaceVariant=MusicInk,
        outline=MusicInk.copy(alpha=0.6f)
    )) {
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MusicCream) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(tr("पवित्र संगीत","Pavitra music"),fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            IconButton(onClick={tab=1}) {Icon(Icons.Default.Search,tr("खोजें","Search"))}
            IconButton(onClick=onDismiss) {Icon(Icons.Default.Close,tr("बंद करें","Close"))}
        }
        TabRow(selectedTabIndex=tab,containerColor=MusicCream) {
            listOf(tr("संग्रह","Discover"),tr("खोज","Search"),tr("चल रहा है","Now playing")).forEachIndexed { index,label ->
                Tab(selected=tab==index,onClick={tab=index},text={Text(label)})
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal=22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                if(tab==2) {
                Surface(Modifier.fillMaxWidth().height(180.dp),shape=RoundedCornerShape(24.dp),color=MusicOrange) {
                    Box(contentAlignment=Alignment.Center) {Text("ॐ",fontSize=88.sp,color=MusicCream)}
                }
                Text(player.title,fontSize=22.sp,modifier=Modifier.padding(top=16.dp))
                Text(tr("इंटरनेट आर्काइव से संगीत","Music from Internet Archive"),fontSize=12.sp)
                if(player.loading) LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=MusicOrange)
                if(player.failed) Text(tr("संगीत नहीं चला। फिर प्रयास करें।","Couldn’t play this track. Tap retry."))
                Slider(value=drag ?: player.position.toFloat(),onValueChange={drag=it},onValueChangeFinished={drag?.let {player.seek(it.toLong())};drag=null},valueRange=0f..player.duration.coerceAtLeast(1).toFloat(),enabled=player.duration>0 && !player.failed,colors=SliderDefaults.colors(thumbColor=MusicOrange,activeTrackColor=MusicOrange))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(musicTime(player.position));Text(musicTime(player.duration))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                    IconToggleButton(checked=player.repeat,onCheckedChange={player.toggleRepeat()}) {Icon(Icons.Default.Repeat,tr("दोहराएँ","Repeat"),tint=if(player.repeat) MusicOrange else MusicInk.copy(alpha=0.4f))}
                    IconButton(onClick={player.skip(-1)},enabled=player.tracks.isNotEmpty()) {Icon(Icons.Default.SkipPrevious,tr("पिछला","Previous"))}
                    FilledIconButton(onClick=player::toggle,modifier=Modifier.size(64.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MusicOrange)) {Icon(if(player.failed) Icons.Default.Refresh else if(player.playing) Icons.Default.Pause else Icons.Default.PlayArrow,tr("चलाएँ / रोकें","Play / pause / retry"))}
                    IconButton(onClick={player.skip(1)},enabled=player.tracks.isNotEmpty()) {Icon(Icons.Default.SkipNext,tr("अगला","Next"))}
                    IconButton(onClick={player.seek(0)}) {Icon(Icons.Default.Replay,tr("शुरू से","Restart"))}
                }
            }
                }
            if(tab==0) {
                item {
                    Surface(shape=RoundedCornerShape(24.dp),color=MusicOrange,contentColor=MusicCream) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text(tr("भक्ति का समय","MAKE SPACE FOR DEVOTION"),fontSize=11.sp,letterSpacing=2.sp)
                            Text(tr("मन शांत। भक्ति अनंत।","A peaceful mind.\nA soulful soundtrack."),fontSize=27.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=12.dp))
                            Text(tr("अपने आराध्य के भजन चुनें","Find your connection, one bhajan at a time."),fontSize=12.sp)
                        }
                    }
                }
                item {Text(tr("अपने आराध्य चुनें","Explore by deity"),fontSize=21.sp,fontWeight=FontWeight.Bold)}
                items(deities.chunked(3)) {row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach {entry ->
                            Surface(modifier=Modifier.weight(1f).clickable {query="";deity=entry.second},shape=RoundedCornerShape(18.dp),color=if(deity==entry.second) MusicOrange else Color(0xFFF0E0C5),contentColor=if(deity==entry.second) MusicCream else MusicInk) {
                                Column(Modifier.padding(vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text("ॐ",fontSize=30.sp)
                                    Text(entry.first,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                item {OutlinedButton(onClick={tab=1},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Default.Search,null);Text(tr("भजन खोजें","Search all bhajans"))}}
            }
            if(tab!=2) {
            item {Text(tr("भजन चुनें","Your devotional collection"),fontSize=20.sp,fontWeight=FontWeight.Bold)}

            if(tab==1) item {
                OutlinedTextField(value=query,onValueChange={query=it},singleLine=true,label={Text(tr("भजन खोजें","Search bhajans"))},modifier=Modifier.fillMaxWidth(),
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={search()}))
                TextButton(enabled=!busy,onClick={search()}) {Text(tr("खोजें / सभी भजन देखें","Search / browse bhajans"),color=MusicOrange)}
            }
            item {
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    item {FilterChip(selected=deity.isEmpty(),onClick={deity=""},label={Text(tr("सभी","All deities"))})}
                    items(deities) {entry -> FilterChip(selected=deity==entry.second,onClick={deity=entry.second},label={Text(entry.first)})}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=popular,onClick={popular=true},label={Text(tr("लोकप्रिय","Most popular"))})
                    FilterChip(selected=!popular,onClick={popular=false},label={Text(tr("प्रासंगिक","Relevant"))})
                }
                Text(tr("इंटरनेट आर्काइव डाउनलोड के आधार पर","Popularity by Internet Archive downloads"),fontSize=11.sp)
                if(busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=MusicOrange)
                message?.let {Text(it)}
            }
            if(albumTitle!=null) {
                item {
                    TextButton(onClick={albumTitle=null}) {Text(tr("संग्रह पर वापस","Back to collections"))}
                    Text(albumTitle!!,fontWeight=FontWeight.Bold)
                    if(collectionTracks.isNotEmpty()) Button(onClick={player.playCollection(collectionTracks);tab=2}) {Text(tr("सभी चलाएँ","Play collection"))}
                }
                items(collectionTracks) {track -> ListItem(headlineContent={Text(track.title)},leadingContent={Icon(Icons.Default.PlayArrow,null)},colors=ListItemDefaults.colors(containerColor=Color.Transparent),modifier=Modifier.clickable {player.playCollection(collectionTracks,track)})}
            } else items(albums,key={it.id}) {album ->
                ListItem(headlineContent={Text(album.title)},supportingContent={Text("${album.downloads} " + tr("डाउनलोड","downloads"))},trailingContent={Icon(Icons.Default.ChevronRight,null)},colors=ListItemDefaults.colors(containerColor=Color.Transparent),modifier=Modifier.clickable(enabled=!busy) {request=scope.launch {
                    busy=true;message=null
                    try {collectionTracks=player.openAlbum(album);albumTitle=album.title;if(collectionTracks.isEmpty()) message=tr("इस संग्रह में MP3 नहीं है","No MP3 tracks in this collection")}
                    catch(e:CancellationException) {throw e}
                    catch(_:Exception) {message=tr("संग्रह नहीं खुला। फिर प्रयास करें।","Couldn’t load collection. Try again.")}
                    finally {busy=false}
                }})
            }
            }
            if(tab==2) {
                item {Text(tr("प्लेलिस्ट","Your queue"),fontWeight=FontWeight.Bold)}
                items(player.tracks) {track -> ListItem(headlineContent={Text(track.title)},leadingContent={Icon(Icons.Default.PlayArrow,null)},colors=ListItemDefaults.colors(containerColor=Color.Transparent),modifier=Modifier.clickable {player.load(track)})}
            }
            item {Spacer(Modifier.height(24.dp))}
        }
        if(tab!=2) BhajanMiniPlayer(player,hindi,{tab=2},Modifier.padding(12.dp))
        }
        }
    }
    }
}
private fun musicTime(ms:Long)="%d:%02d".format(ms/60000,ms/1000%60)

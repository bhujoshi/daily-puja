package com.worship.nityamandir.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.worship.nityamandir.data.*
import com.worship.nityamandir.engine.WorshipSession
import com.worship.nityamandir.engine.BhajanPlayer
import kotlinx.coroutines.launch

@Composable
fun ShrineCustomizer(account:DevotionAccount,current:ShrineSelection,hindi:Boolean,onDismiss:()->Unit,onAccount:(ShrineSelection)->Unit,onApplied:(ShrineSelection)->Unit,
    player:BhajanPlayer,onMusic:()->Unit) {
    val context=LocalContext.current
    val catalog=remember {ShrineCatalog(context)}
    var draft by remember {mutableStateOf(catalog.normalize(current))}
    var categoryId by remember {mutableStateOf("shrine")}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var showPreview by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val optionsScroll=rememberScrollState()
    LaunchedEffect(categoryId) {optionsScroll.scrollTo(0)}
    fun tr(hi:String,en:String)=if(hindi) hi else en
    Dialog(onDismissRequest={if(!busy) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=Color(0xFFFFF7EC)) {
            Column(Modifier.safeDrawingPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(player.current!=null || player.loading) BhajanMiniPlayer(player,hindi,onMusic)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(tr("अपना मंदिर सजाएँ","Make this temple yours"),Modifier.weight(1f),fontSize=23.sp)
                    TextButton(onClick=onDismiss,enabled=!busy) {Text(tr("बंद करें","Close"))}
                }
                Text(tr("पहले देखकर चुनें। लागू करने पर नई पूजा शुरू होगी।","Preview before choosing. Applying a design starts a fresh puja."),fontSize=16.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    catalog.categories.forEach {category ->
                        FilterChip(selected=categoryId==category.id,onClick={categoryId=category.id},label={Text(if(hindi) category.hi else category.en)})
                    }
                }
                val category=catalog.categories.first {it.id==categoryId}
                if(categoryId=="flowers") Text(tr("जितनी चाहें फूलों की किस्में चुनें (कम से कम एक)।","Choose as many flower varieties as you like (at least one)."))
                Column(Modifier.weight(1f).verticalScroll(optionsScroll),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    category.options.forEach {option ->
                        val selected=if(category.id=="flowers") option.id in draft.flowerIds else draft[category.id]==option.id
                        Surface(onClick={draft=if(category.id=="flowers") draft.toggleFlower(option.id) else draft.with(category.id,option.id)},shape=RoundedCornerShape(16.dp),color=if(selected) Color(0xFFF1DFC1) else Color.White,border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) RitualGold else Color(0xFFE4D7C6)),modifier=Modifier.fillMaxWidth().semantics {this.selected=selected}) {
                            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                val bitmap=remember(option.thumbnail) {context.assets.open(option.thumbnail).use {BitmapFactory.decodeStream(it)}.asImageBitmap()}
                                Image(bitmap,null,Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF6EBDD)),contentScale=ContentScale.Fit)
                                Column(Modifier.weight(1f)) {
                                    Text(if(hindi) option.hi else option.en,fontSize=18.sp)
                                    Text(if(selected) tr("चुना हुआ","Selected") else tr("चुनने के लिए छुएँ","Tap to choose"),fontSize=14.sp)
                                }
                                if(category.id=="flowers") Checkbox(checked=selected,onCheckedChange=null)
                                else RadioButton(selected=selected,onClick=null)
                            }
                        }
                    }
                }
                OutlinedButton(onClick={showPreview=true},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(tr("पूरे मंदिर में देखें","Preview in the temple"))}
                val unlocked=account.profile?.optBoolean("unlocked")==true
                if(!unlocked && !draft.original) Text(tr("सहेजने के लिए पैकेज अनलॉक करें: 7 दिन पूजा या एक मित्र की पहली पूजा।","To save, unlock the package: 7 daily pujas or a friend's first puja."))
                if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
                Button(onClick={
                    if(!draft.original && !unlocked) {onAccount(draft)}
                    else if(account.signedIn && unlocked) scope.launch {
                        busy=true;error=""
                        try {account.saveSelection(draft);onApplied(draft)}
                        catch(e:Exception) {error=e.message ?: tr("सहेजा नहीं गया। फिर कोशिश करें।","Not saved. Please try again.")}
                        finally {busy=false}
                    } else onApplied(draft)
                },enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
                    Text(if(busy) tr("सहेज रहे हैं…","Saving…") else if(!draft.original && !unlocked) tr("खाता और अनलॉक विकल्प","Account & unlock options") else tr("इस मंदिर को लागू करें","Apply this temple"))
                }
            }
        }
    }
    if(showPreview) Dialog(onDismissRequest={showPreview=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFFF7EC))) {
            key(draft) {MandirAltarView(
                session=WorshipSession(deityCount=draft.deityCount),dustLevel=0f,flowerWitherFactor=0f,bathTarget=null,bathProgress=0f,
                flowerFlights=emptyList(),aartiRunning=false,aartiProgress=0f,onDeityClick={},onDiyaClick={},onBellClick={},
                bellRunning=false,bellProgress=0f,conchRunning=false,conchProgress=0f,prasadRunning=false,prasadProgress=0f,
                onFlowerClick={},onConchClick={},onAartiClick={},selection=draft)}
            // Popup stays above SceneView's surface.
            androidx.compose.ui.window.Popup(alignment=Alignment.BottomCenter) {
                Surface(color=Color(0xFFFFF7EC),shape=RoundedCornerShape(20.dp),modifier=Modifier.navigationBarsPadding().padding(20.dp)) {
                    Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(tr("पूर्वावलोकन · अभी सहेजा नहीं गया","Preview · not saved yet"),fontSize=18.sp)
                        Button(onClick={showPreview=false},modifier=Modifier.heightIn(min=52.dp)) {Text(tr("चुनाव पर वापस जाएँ","Back to choices"))}
                    }
                }
            }
            if(player.current!=null || player.loading) androidx.compose.ui.window.Popup(alignment=Alignment.TopCenter) {
                BhajanMiniPlayer(player,hindi,{showPreview=false;onMusic()},Modifier.statusBarsPadding().padding(16.dp))
            }
        }
    }
}

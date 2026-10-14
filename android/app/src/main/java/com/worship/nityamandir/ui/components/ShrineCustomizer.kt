package com.worship.nityamandir.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.worship.nityamandir.data.*
import com.worship.nityamandir.engine.WorshipSession
import com.worship.nityamandir.engine.BhajanPlayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun ShrineCustomizer(account:DevotionAccount,current:ShrineSelection,hindi:Boolean,onDismiss:()->Unit,onAccount:(ShrineSelection)->Unit,onApplied:(ShrineSelection)->Unit,
    player:BhajanPlayer,onMusic:()->Unit) {
    val context=LocalContext.current
    val catalog=remember {ShrineCatalog(context)}
    var draft by remember {mutableStateOf(catalog.normalize(current))}
    var categoryId by remember {mutableStateOf("shrine")}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var message by remember {mutableStateOf("")}
    var pendingUnlocks by remember {mutableStateOf<List<ShrineUnlock>>(emptyList())}
    var showPreview by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val optionsScroll=rememberScrollState()
    val newUnlockKeys=remember {account.newUnlocks(catalog).map {"${it.category.id}:${it.option.id}"}.toSet()}
    LaunchedEffect(Unit) {account.markUnlocksSeen(catalog)}
    LaunchedEffect(categoryId) {optionsScroll.scrollTo(0)}
    fun tr(hi:String,en:String)=if(hindi) hi else en
    fun label(option:ShrineOption)=if(hindi) option.hi else option.en
    fun failure(e:Exception)=when {
        !account.signedIn -> tr("साइन इन करके फिर कोशिश करें। आपके चुने हुए विकल्प बने रहेंगे।","Sign in and try again. Your choices are kept.")
        e is java.io.IOException -> tr("इंटरनेट जाँचें और फिर कोशिश करें। अनलॉक किए गए आइटम आपके पास रहेंगे।","Check your connection and try again. Unlocked items stay yours.")
        e.message?.startsWith("Not enough coins")==true -> tr("सिक्के कम हैं। प्रोफ़ाइल में प्रगति ताज़ा करें या और सिक्के कमाएँ।","Not enough coins. Refresh your profile or earn more coins.")
        else -> tr("पूरा नहीं हो पाया। फिर कोशिश करें। अनलॉक किए गए आइटम आपके पास रहेंगे।","Could not complete this action. Try again. Unlocked items stay yours.")
    }
    fun requestUnlock(items:List<ShrineUnlock>) {
        if(busy) return
        error="";message=""
        if(!account.signedIn || account.coins<items.sumOf {it.cost}) {
            draft=items.fold(draft) {selection,item -> selection.selectItem(item.category.id,item.option.id)}
            onAccount(draft)
        }
        else pendingUnlocks=items
    }
    val selectedUnlocks=catalog.categories.flatMap {category ->
        category.options.filter {option ->
            (if(category.id=="flowers") option.id in draft.flowerIds else draft[category.id]==option.id) && !account.owns(category.id,option.id)
        }.map {ShrineUnlock(category,it,account.cost(category.id,it.id))}
    }
    val selectedCost=selectedUnlocks.sumOf {it.cost}
    Dialog(onDismissRequest={if(!busy) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=Color(0xFFFFF7EC)) {
            Column(Modifier.safeDrawingPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(player.current!=null || player.loading) BhajanMiniPlayer(player,hindi,onMusic)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(tr("मंदिर सजाएँ","Mandir Sajaye"),Modifier.weight(1f),fontSize=23.sp,fontWeight=FontWeight.SemiBold)
                    TextButton(onClick=onDismiss,enabled=!busy) {Text(tr("बंद करें","Close"))}
                }
                Surface(color=Color(0xFFF2E5D2),shape=RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Toll,null,tint=RitualGold)
                        Text(tr("${account.coins} सिक्के","${account.coins} coins"),Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.SemiBold)
                        TextButton(onClick={onAccount(draft)},enabled=!busy) {Text(if(account.signedIn) tr("सिक्के कमाएँ","Earn coins") else tr("साइन इन करें","Sign in"))}
                    }
                }
                Text(tr("लागू करने पर नई पूजा शुरू होगी।", "Applying a design starts a fresh puja."),fontSize=14.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    catalog.categories.forEach {category ->
                        FilterChip(selected=categoryId==category.id,onClick={categoryId=category.id},enabled=!busy,label={Text(if(hindi) category.hi else category.en)})
                    }
                }
                val category=catalog.categories.first {it.id==categoryId}
                if(categoryId=="flowers") Text(tr("जितनी चाहें फूलों की किस्में चुनें (कम से कम एक)।","Choose as many flower varieties as you like (at least one)."))
                Column(Modifier.weight(1f).verticalScroll(optionsScroll),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    category.options.forEach {option ->
                        val selected=if(category.id=="flowers") option.id in draft.flowerIds else draft[category.id]==option.id
                        val owned=account.owns(category.id,option.id)
                        val cost=account.cost(category.id,option.id)
                        Surface(onClick={draft=if(category.id=="flowers") draft.toggleFlower(option.id) else draft.with(category.id,option.id);error="";message=""},enabled=!busy,shape=RoundedCornerShape(16.dp),color=if(selected) Color(0xFFF1DFC1) else Color.White,border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) RitualGold else Color(0xFFE4D7C6)),modifier=Modifier.fillMaxWidth().semantics {this.selected=selected}) {
                            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                val bitmap=remember(option.thumbnail) {context.assets.open(option.thumbnail).use {BitmapFactory.decodeStream(it)}.asImageBitmap()}
                                Image(bitmap,null,Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF6EBDD)),contentScale=ContentScale.Fit)
                                Column(Modifier.weight(1f)) {
                                    Text(label(option),fontSize=18.sp,fontWeight=FontWeight.Medium)
                                    Text(if(selected && !owned) tr("पूर्वावलोकन के लिए चुना","Selected for preview") else if(selected) tr("चुना हुआ","Selected") else tr("चुनने के लिए छुएँ","Tap to choose"),fontSize=14.sp)
                                    Text(if(cost==0) tr("मुफ़्त","Free") else if(owned) tr("अनलॉक है","Unlocked") else tr("$cost सिक्के","$cost coins"),color=RitualGold,fontSize=14.sp)
                                    if("${category.id}:${option.id}" in newUnlockKeys) Text(tr("नया अनलॉक","New unlock"),color=RitualGold,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                                    if(!owned) {
                                        val shortage=(cost-account.coins).coerceAtLeast(0)
                                        if(account.signedIn && shortage>0) Text(tr("$shortage सिक्के और चाहिए","Need $shortage more coins"),fontSize=13.sp)
                                        OutlinedButton(onClick={requestUnlock(listOf(ShrineUnlock(category,option,cost)))},enabled=!busy) {
                                            Text(if(!account.signedIn) tr("अनलॉक के लिए साइन इन करें","Sign in to unlock") else if(shortage>0) tr("सिक्के कमाएँ","Earn coins") else tr("अनलॉक · $cost सिक्के","Unlock · $cost coins"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick={showPreview=true},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(tr("पूरे मंदिर में देखें","Preview in the temple"))}
                if(selectedUnlocks.isNotEmpty()) Text(tr("चुने हुए आइटम अनलॉक करें · $selectedCost सिक्के","Unlock selected items · $selectedCost coins"),fontSize=14.sp)
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
                if(message.isNotEmpty()) Text(message,color=RitualInk,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
                RitualChoice(if(busy) tr("कृपया प्रतीक्षा करें…","Please wait…") else if(selectedUnlocks.isEmpty()) tr("इस मंदिर को लागू करें","Apply this temple")
                    else if(!account.signedIn) tr("अनलॉक के लिए साइन इन करें","Sign in to unlock")
                    else if(account.coins<selectedCost) tr("${selectedCost-account.coins} सिक्के और कमाएँ","Earn ${selectedCost-account.coins} more coins")
                    else tr("चुने हुए आइटम अनलॉक करें · $selectedCost सिक्के","Unlock selection · $selectedCost coins"),false,{
                    if(selectedUnlocks.isNotEmpty()) requestUnlock(selectedUnlocks)
                    else {
                        busy=true;error="";message=""
                        scope.launch {
                            try {account.saveSelection(draft);onApplied(draft)}
                            catch(e:CancellationException) {throw e}
                            catch(e:Exception) {error=failure(e)}
                            finally {busy=false}
                        }
                    }
                },Modifier.fillMaxWidth(),enabled=!busy)
            }
        }
    }
    if(pendingUnlocks.isNotEmpty()) {
        val remaining=pendingUnlocks.filter {!account.owns(it.category.id,it.option.id)}
        val cost=remaining.sumOf {account.cost(it.category.id,it.option.id)}
        AlertDialog(onDismissRequest={pendingUnlocks=emptyList()},
            title={Text(tr("आइटम अनलॉक करें?","Unlock items?"))},
            text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(pendingUnlocks.joinToString("\n") {label(it.option)})
                Text(tr("कीमत: $cost सिक्के\nआपके पास: ${account.coins} सिक्के\nबाद में: ${(account.coins-cost).coerceAtLeast(0)} सिक्के",
                    "Cost: $cost coins\nYour balance: ${account.coins} coins\nAfter unlocking: ${(account.coins-cost).coerceAtLeast(0)} coins"))
                Text(tr("आइटम आपके पास रहेंगे। फिर इन्हें मंदिर पर लागू करें।","These items stay yours. Apply them to your temple after unlocking."),fontSize=14.sp)
            }},
            confirmButton={TextButton(enabled=!busy && account.signedIn && account.coins>=cost,onClick={
                val purchase=pendingUnlocks
                pendingUnlocks=emptyList();busy=true;error="";message=""
                scope.launch {
                    try {
                        purchase.forEach {item ->
                            if(!account.owns(item.category.id,item.option.id)) account.unlock(item.category.id,item.option.id)
                            draft=draft.selectItem(item.category.id,item.option.id)
                        }
                        message=tr("अनलॉक हो गया। अपने मंदिर पर लागू करें।","Unlocked and selected. Apply it to your temple.")
                    } catch(e:CancellationException) {throw e}
                    catch(e:Exception) {error=failure(e)}
                    finally {busy=false}
                }
            }) {Text(tr("अनलॉक · $cost सिक्के","Unlock · $cost coins"))}},
            dismissButton={TextButton(onClick={pendingUnlocks=emptyList()}) {Text(tr("वापस","Cancel"))}})
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

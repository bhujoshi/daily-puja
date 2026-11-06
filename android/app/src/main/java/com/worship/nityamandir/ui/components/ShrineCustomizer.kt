package com.worship.nityamandir.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.worship.nityamandir.data.*
import com.worship.nityamandir.engine.WorshipSession
import com.worship.nityamandir.engine.BhajanPlayer
import com.worship.nityamandir.engine.OccasionAartiAnimation
import com.worship.nityamandir.engine.TempleOccasion
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun ShrineCustomizer(account:DevotionAccount,current:ShrineSelection,hindi:Boolean,onDismiss:()->Unit,onAccount:(ShrineSelection)->Unit,onApplied:(ShrineSelection)->Unit,
    player:BhajanPlayer,onMusic:(ShrineSelection)->Unit,activeOccasion:TempleOccasion?=null) {
    val context=LocalContext.current
    val analytics=remember {MandirAnalytics.from(context)}
    val catalog=remember {ShrineCatalog(context)}
    val baseline=remember(current) {catalog.normalize(current)}
    var draft by remember {mutableStateOf(baseline)}
    var categoryId by remember {
        mutableStateOf(
            catalog.categories.firstOrNull {cat ->
                cat.options.any {opt ->
                    (if(cat.id=="flowers") opt.id in draft.flowerIds else draft[cat.id]==opt.id) && !account.owns(cat.id,opt.id)
                }
            }?.id ?: "shrine"
        )
    }
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var message by remember {mutableStateOf("")}
    var pendingUnlocks by remember {mutableStateOf<List<ShrineUnlock>>(emptyList())}
    var showPreview by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val optionsScroll=rememberScrollState()
    val newUnlockKeys=remember {account.newUnlocks(catalog).map {"${it.category.id}:${it.option.id}"}.toSet()}
    val customizerScreen=when {
        pendingUnlocks.isNotEmpty() -> "shrine_unlock_dialog"
        showPreview -> "shrine_preview"
        else -> "shrine_customizer"
    }
    LaunchedEffect(customizerScreen) {
        analytics.logScreenView(customizerScreen,"ShrineCustomizer")
    }
    LaunchedEffect(Unit) {
        account.markUnlocksSeen(catalog)
        analytics.logShrineCategoryViewed(categoryId)
    }
    LaunchedEffect(categoryId) {optionsScroll.scrollTo(0)}
    fun tr(hi:String,en:String)=if(hindi) hi else en
    fun label(option:ShrineOption)=if(hindi) option.hi else option.en
    val notEnoughCoinsMessage=tr(
        "पर्याप्त सिक्के नहीं हैं। सिक्के कमाने के लिए दैनिक पूजा पूरी करें या मित्रों को आमंत्रित करें।",
        "Not enough coins. Complete daily puja or invite friends to earn coins."
    )
    fun failure(e:Exception)=when {
        !account.signedIn -> tr("साइन इन करके फिर कोशिश करें। आपके चुने हुए विकल्प बने रहेंगे।","Sign in and try again. Your choices are kept.")
        e is java.io.IOException -> tr("इंटरनेट जाँचें और फिर कोशिश करें। अनलॉक किए गए आइटम आपके पास रहेंगे।","Check your connection and try again. Unlocked items stay yours.")
        e.message?.startsWith("Not enough coins")==true -> notEnoughCoinsMessage
        else -> tr("पूरा नहीं हो पाया। फिर कोशिश करें। अनलॉक किए गए आइटम आपके पास रहेंगे।","Could not complete this action. Try again. Unlocked items stay yours.")
    }
    fun revertUnownedOtherCategories(selection:ShrineSelection,keepCategoryId:String):ShrineSelection =
        catalog.categories.fold(selection) {acc,cat ->
            if(cat.id==keepCategoryId) acc
            else if(cat.id=="flowers") {
                val ownedFlowers=acc.flowerIds.filter {account.owns("flowers",it)}
                    .ifEmpty {baseline.flowerIds.filter {account.owns("flowers",it)}}
                    .ifEmpty {listOf(cat.options.first().id)}
                acc.with("flowers",ownedFlowers.joinToString(","))
            } else if(!account.owns(cat.id,acc[cat.id])) {
                val fallback=baseline[cat.id].takeIf {account.owns(cat.id,it)}
                    ?: cat.options.firstOrNull {account.owns(cat.id,it.id)}?.id
                    ?: cat.options.first().id
                acc.with(cat.id,fallback)
            } else acc
        }
    fun requestUnlock(items:List<ShrineUnlock>) {
        if(busy) return
        error="";message=""
        draft=items.fold(draft) {selection,item -> selection.selectItem(item.category.id,item.option.id)}
        val totalCost=items.sumOf {it.cost}
        analytics.logShrineUnlockPrompted(items.size,totalCost,account.coins>=totalCost,account.signedIn)
        if(!account.signedIn) onAccount(draft)
        else if(account.coins<totalCost) message=notEnoughCoinsMessage
        else pendingUnlocks=items
    }
    val selectedUnlocks=catalog.categories.flatMap {category ->
        category.options.filter {option ->
            (if(category.id=="flowers") option.id in draft.flowerIds else draft[category.id]==option.id) && !account.owns(category.id,option.id)
        }.map {ShrineUnlock(category,it,account.cost(category.id,it.id))}
    }
    val selectedCost=selectedUnlocks.sumOf {it.cost}
    val selectionActionLabel=if(busy) tr("रुकें…","Wait…")
        else if(selectedUnlocks.isEmpty()) tr("लगाएँ","Apply")
        else tr("अनलॉक","Unlock")
    val selectionActionIcon:(@Composable ()->Unit)?=if(selectedUnlocks.isEmpty()) null else {
        {Icon(Icons.Outlined.Lock,null,Modifier.size(18.dp))}
    }
    val selectionHint=if(selectedUnlocks.isEmpty()) tr("लगाने पर नई पूजा शुरू होगी।","Starts a new puja when applied.")
        else tr("कीमत: $selectedCost सिक्के","Cost: $selectedCost coins")
    val applyOrUnlock:()->Unit = action@{
        if(busy) return@action
        if(selectedUnlocks.isNotEmpty()) {
            if(!account.signedIn || account.coins>=selectedCost) {
                // Leave the scene preview before opening account or purchase dialogs.
                showPreview=false
            }
            requestUnlock(selectedUnlocks)
        } else {
            busy=true;error="";message=""
            scope.launch {
                try {
                    account.saveSelection(draft)
                    analytics.logShrineSelectionApplied(draft)
                    showPreview=false
                    onApplied(draft)
                }
                catch(e:CancellationException) {throw e}
                catch(e:Exception) {error=failure(e)}
                finally {busy=false}
            }
        }
    }
    Dialog(onDismissRequest={if(!busy) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxSize(),color=Color(0xFFFFF7EC)) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().testTag("mandir-safe-content").padding(horizontal=12.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(tr("मंदिर सजाएँ","Mandir Sajaye"),Modifier.weight(1f),fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                    TextButton(onClick={analytics.logShrineAccountClicked(account.signedIn,account.coins);onAccount(draft)},enabled=!busy,modifier=Modifier.semantics {
                        contentDescription=if(account.signedIn) tr("${account.coins} सिक्के · सिक्के कमाएँ","${account.coins} coins · Earn coins")
                            else tr("${account.coins} सिक्के · साइन इन करें","${account.coins} coins · Sign in")
                    }) {
                        Icon(Icons.Outlined.Toll,null,tint=RitualGold,modifier=Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${account.coins}",fontSize=14.sp)
                    }
                    IconButton(onClick=onDismiss,enabled=!busy) {Icon(Icons.Outlined.Close,tr("बंद करें","Close"))}
                }
                Column(Modifier.weight(1f).verticalScroll(optionsScroll),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(player.current!=null || player.loading) BhajanMiniPlayer(player,hindi,{onMusic(draft)},Modifier.testTag("mandir-choice-music"),compact=true)
                if(activeOccasion!=null) {
                    val presetActive = activeOccasion.matchesSelection(draft)
                    Surface(
                        onClick = {
                            error = ""; message = ""
                            draft = catalog.normalize(activeOccasion.presetSelection)
                        },
                        enabled = !busy,
                        shape = RoundedCornerShape(16.dp),
                        color = if (presetActive) Color(0xFFF3E2C3) else Color(0xFFFFFBF3),
                        border = BorderStroke(if (presetActive) 2.dp else 1.5.dp, RitualGold),
                        modifier = Modifier.fillMaxWidth().testTag("occasion-preset-banner")
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "🪔 ${activeOccasion.badge(hindi)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA82424)
                                )
                                Text(
                                    text = activeOccasion.title(hindi),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RitualInk
                                )
                                Text(
                                    text = activeOccasion.subtitle(hindi),
                                    fontSize = 12.sp,
                                    color = Color(0xFF6E5D4F)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (presetActive) RitualInk else Color(0xFFF0E0C8),
                                border = BorderStroke(1.dp, RitualGold)
                            ) {
                                Text(
                                    text = if (presetActive) tr("✓ चयनित", "✓ Selected") else tr("उत्सव चुनें", "Use Preset"),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (presetActive) Color(0xFFFAF6EE) else RitualInk
                                )
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    catalog.categories.forEach {category ->
                        FilterChip(selected=categoryId==category.id,onClick={
                            error="";message=""
                            draft=revertUnownedOtherCategories(draft,category.id)
                            categoryId=category.id
                            analytics.logShrineCategoryViewed(category.id)
                        },enabled=!busy,label={Text(if(hindi) category.hi else category.en)})
                    }
                }
                val category=catalog.categories.first {it.id==categoryId}
                if(categoryId=="flowers") Text(tr("कई किस्में चुनें · ${draft.flowerIds.size} चुनी गईं","Select multiple · ${draft.flowerIds.size} selected"),fontSize=14.sp)
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    catalog.visibleOptions(category, activeOccasion, draft).forEach {option ->
                        val selected=if(category.id=="flowers") option.id in draft.flowerIds else draft[category.id]==option.id
                        val owned=account.owns(category.id,option.id)
                        val cost=account.cost(category.id,option.id)
                        val isOccasionExclusive = category.id == "shrine" && activeOccasion?.exclusiveShrineId == option.id
                        Surface(onClick={
                            error="";message=""
                            val baseDraft=revertUnownedOtherCategories(draft,category.id)
                            if(category.id=="flowers" && selected && baseDraft.flowerIds.size==1) {
                                message=tr("कम से कम एक फूल की किस्म रखें।","Keep at least one flower variety.")
                            } else {
                                draft=if(category.id=="flowers") baseDraft.toggleFlower(option.id) else baseDraft.with(category.id,option.id)
                                val nowSelected=if(category.id=="flowers") option.id in draft.flowerIds else draft[category.id]==option.id
                                analytics.logShrineOptionSelected(category.id,option.id,owned,cost,nowSelected)
                            }
                        },enabled=!busy,shape=RoundedCornerShape(16.dp),color=if(selected) Color(0xFFF1DFC1) else Color.White,border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) RitualGold else Color(0xFFE4D7C6)),modifier=Modifier.fillMaxWidth().semantics {
                            this.selected=selected
                            if(category.id=="flowers") {role=Role.Checkbox;toggleableState=if(selected) ToggleableState.On else ToggleableState.Off}
                        }) {
                            Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                coil.compose.AsyncImage("file:///android_asset/${option.thumbnail}",null,Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF6EBDD)),contentScale=ContentScale.Fit)
                                Column(Modifier.weight(1f)) {
                                    Text(label(option),fontSize=16.sp,fontWeight=FontWeight.Medium)
                                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                        if(!owned) Icon(Icons.Outlined.Lock,tr("लॉक है","Locked"),Modifier.size(14.dp),tint=RitualGold)
                                        Text(if(!owned) tr("$cost सिक्के","$cost coins") else if(cost==0) tr("मुफ़्त","Free") else tr("अनलॉक है","Unlocked"),color=RitualGold,fontSize=14.sp)
                                        if (isOccasionExclusive) {
                                            Text("· 🪔 ${activeOccasion.badge(hindi)}", color=Color(0xFFA82424), fontWeight=FontWeight.SemiBold, fontSize=13.sp)
                                        }
                                    }
                                    if("${category.id}:${option.id}" in newUnlockKeys) Text(tr("नया अनलॉक","New unlock"),color=RitualGold,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                                }
                                if(category.id=="flowers") Checkbox(checked=selected,onCheckedChange=null,modifier=Modifier.size(24.dp))
                                else RadioButton(selected=selected,onClick=null,modifier=Modifier.size(24.dp))
                            }
                        }
                    }
                }
                }
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                // Bound feedback independently so long messages cannot displace the actions.
                if(error.isNotEmpty() || message.isNotEmpty()) Column(
                    Modifier.heightIn(max=64.dp).verticalScroll(rememberScrollState())
                ) {
                    if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
                    if(message.isNotEmpty()) Text(message,color=RitualInk,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    RitualChoice(tr("देखें","Preview"),false,{showPreview=true;analytics.logShrinePreviewOpened(draft["shrine"],draft["idols"])},Modifier.weight(1f).testTag("mandir-preview-button"),enabled=!busy)
                    RitualChoice(selectionActionLabel,false,applyOrUnlock,Modifier.weight(1f).testTag("mandir-selection-action"),enabled=!busy,leadingIcon=selectionActionIcon)
                }
                Text(selectionHint,fontSize=12.sp)
            }
        }
    }
    if(pendingUnlocks.isNotEmpty()) {
        val remaining=pendingUnlocks.filter {!account.owns(it.category.id,it.option.id)}
        val cost=remaining.sumOf {account.cost(it.category.id,it.option.id)}
        AlertDialog(onDismissRequest={analytics.logShrineUnlockCancelled(pendingUnlocks.size,cost);pendingUnlocks=emptyList()},
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
                            if(!account.owns(item.category.id,item.option.id)) {
                                account.unlock(item.category.id,item.option.id)
                                analytics.logShrineItemUnlocked(item.category.id,item.option.id,item.cost)
                            }
                            draft=draft.selectItem(item.category.id,item.option.id)
                        }
                        message=tr("अनलॉक हो गया · लगाने के लिए तैयार","Unlocked · ready to use")
                    } catch(e:CancellationException) {throw e}
                    catch(e:Exception) {error=failure(e)}
                    finally {busy=false}
                }
            }) {Icon(Icons.Outlined.Lock,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(tr("अनलॉक","Unlock"))}},
            dismissButton={TextButton(onClick={analytics.logShrineUnlockCancelled(pendingUnlocks.size,cost);pendingUnlocks=emptyList()}) {Text(tr("वापस","Cancel"))}})
    }
    if(showPreview) Dialog(onDismissRequest={if(!busy) showPreview=false},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFFF7EC))) {
            val previewOccasionEligible = OccasionAartiAnimation.isEligible(draft, activeOccasion)
            key(draft) {MandirAltarView(
                session=WorshipSession(deityCount=draft.deityCount, lit=previewOccasionEligible, aartiComplete=previewOccasionEligible),
                dustLevel=0f,flowerWitherFactor=0f,bathTarget=null,bathProgress=0f,
                flowerFlights=emptyList(),aartiRunning=false,aartiProgress=0f,onDeityClick={},onDiyaClick={},onBellClick={},
                bellRunning=false,bellProgress=0f,conchRunning=false,conchProgress=0f,prasadRunning=false,prasadProgress=0f,
                onFlowerClick={},onConchClick={},onAartiClick={},selection=draft,activeOccasion=activeOccasion)}
            // Popup stays above SceneView's surface.
            androidx.compose.ui.window.Popup(alignment=Alignment.BottomCenter) {
                Surface(color=Color(0xFFFFF7EC),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(horizontal=12.dp,vertical=8.dp)) {
                    Column(Modifier.testTag("mandir-preview-actions").padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
                        if(message.isNotEmpty()) Text(message,color=RitualInk,textAlign=TextAlign.Center,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
                        RitualChoice(selectionActionLabel,false,applyOrUnlock,Modifier.fillMaxWidth().testTag("mandir-preview-action"),enabled=!busy,leadingIcon=selectionActionIcon)
                        Text(selectionHint,Modifier.fillMaxWidth(),fontSize=12.sp,textAlign=TextAlign.Center)
                    }
                }
            }
            androidx.compose.ui.window.Popup(alignment=Alignment.TopCenter) {
                Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFFFFF7EC)) {
                        IconButton(onClick={showPreview=false},enabled=!busy) {Icon(Icons.AutoMirrored.Outlined.ArrowBack,tr("चुनाव पर वापस जाएँ","Back to choices"))}
                    }
                    if(player.current!=null || player.loading) BhajanMiniPlayer(player,hindi,{showPreview=false;onMusic(draft)},Modifier.weight(1f).testTag("mandir-preview-music"),compact=true)
                }
            }
        }
    }
}

package com.worship.nityamandir.ui.screens

import android.content.Context
import com.worship.nityamandir.BuildConfig
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import com.worship.nityamandir.engine.*
import com.worship.nityamandir.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun MandirHomeScreen(modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val hostView=LocalView.current
    val prefs=remember {context.getSharedPreferences("worship",Context.MODE_PRIVATE)}
    val account=remember {com.worship.nityamandir.data.DevotionAccount(context.applicationContext)}
    var devotionSheet by remember {mutableStateOf(false)}
    var devotionPage by remember {mutableStateOf(DevotionPage.PROFILE)}
    var customizer by remember {mutableStateOf(false)}
    var customizerDraft by remember {mutableStateOf<com.worship.nityamandir.data.ShrineSelection?>(null)}
    var selection by remember {mutableStateOf(com.worship.nityamandir.data.ShrineSelection())}
    LaunchedEffect(account.profile) {selection=account.selection()}
    val audio=remember {RitualAudio(context)}
    val bhajan=remember {BhajanPlayer(context.applicationContext)}
    var musicSheet by remember {mutableStateOf(false)}
    LaunchedEffect(selection["idols"]) { bhajan.changeIdol(selection["idols"]) }
    DisposableEffect(bhajan) {onDispose {bhajan.release()}}
    DisposableEffect(Unit) {onDispose {audio.release()}}
    val lifecycle=androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    var foreground by remember {mutableStateOf(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))}
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {foreground=false;audio.stop()}
            if(event==androidx.lifecycle.Lifecycle.Event.ON_START) foreground=true
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(foreground) {
        if(foreground) try { account.sync() } catch(_:Exception) { /* Retain pending completion for retry. */ }
    }
    var hindi by remember {mutableStateOf(prefs.getBoolean("hindi",true))}
    var menu by remember {mutableStateOf(false)}
    var entered by remember {mutableStateOf(false)}
    val curtain=remember {Animatable(0f)}
    val curtainMoving=curtain.isRunning || curtain.value != (if(entered) 1f else 0f)
    LaunchedEffect(entered) {
        curtain.animateTo(if(entered) 1f else 0f,tween(1600,easing=FastOutSlowInEasing))
    }
    var session by remember {mutableStateOf(WorshipSession(deityCount=selection.deityCount))}
    var last by remember {mutableLongStateOf(prefs.getLong("last",System.currentTimeMillis()))}
    var clean by remember {mutableLongStateOf(prefs.getLong("clean",last))}
    var timeOffset by remember {mutableLongStateOf(if(BuildConfig.DEBUG) prefs.getLong("agingOffset",0L) else 0L)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis()+timeOffset)}
    LaunchedEffect(Unit) {prefs.edit().putLong("last",last).putLong("clean",clean).apply()}
    var wipe by remember {mutableFloatStateOf(0f)}
    val cleanedAreas=remember(selection) {mutableStateListOf<TemplePoint>()}
    val shrineSpace=remember(selection) {ShrineSpace.forId(selection["shrine"])}
    var cleaningSweep by remember {mutableStateOf(false)}
    fun finishCleaning() {
        clean=System.currentTimeMillis()+timeOffset;now=clean
        prefs.edit().putLong("clean",clean).apply()
        wipe=0f;cleanedAreas.clear();cleaningSweep=false
    }
    LaunchedEffect(cleaningSweep) {
        if(cleaningSweep) {
            ShrineCleaning.samples(shrineSpace).forEach {point ->
                cleanedAreas.add(point);wipe=ShrineCleaning.coverage(shrineSpace,cleanedAreas)
                delay(22)
            }
            finishCleaning()
        }
    }
    var bathTarget by remember {mutableStateOf<Int?>(null)}
    val flowerFlights=remember {mutableStateListOf<FlowerFlight>()}
    var nextFlowerId by remember {mutableLongStateOf(0L)}
    var prasadRunning by remember {mutableStateOf(false)}
    val prasad=remember {Animatable(0f)}
    var aartiRunning by remember {mutableStateOf(false)}
    var bellRunning by remember {mutableStateOf(false)}
    var conchRunning by remember {mutableStateOf(false)}
    val bell=remember {Animatable(0f)}
    val conch=remember {Animatable(0f)}
    val bath=remember {Animatable(0f)}
    val aarti=remember {Animatable(0f)}
    val aartiMotion=aarti.value
    var sceneSize by remember {mutableStateOf(IntSize.Zero)}
    var sceneOrigin by remember {mutableStateOf(Offset.Zero)}
    LaunchedEffect(timeOffset,foreground) {while(true) {now=System.currentTimeMillis()+timeOffset;delay(30000)}}
    val aging=AgingEngine.calculateAging(last,clean,now)
    val ritualBusy=curtainMoving || bathTarget!=null || aartiRunning || bellRunning || conchRunning || prasadRunning
    val busy=ritualBusy || flowerFlights.isNotEmpty()
    LaunchedEffect(selection) {
        bathTarget=null;flowerFlights.clear();aartiRunning=false;bellRunning=false;conchRunning=false;prasadRunning=false
        audio.stop()
        session=WorshipSession(deityCount=selection.deityCount)
        wipe=0f;cleaningSweep=false
    }
    fun tr(hi:String,en:String)=if(hindi) hi else en
    fun ringBell() {
        if(entered && foreground && !aging.needsCleaning && !bellRunning) bellRunning=true
    }
    fun soundConch() {
        if(entered && foreground && !aging.needsCleaning && !conchRunning) conchRunning=true
    }
    fun offerFlowerToDeity(deity:Int, index:Int) {
        if(!entered || aging.needsCleaning || !foreground) return
        val currentCount=session.offeredFlowers.count {it.deity==deity}+flowerFlights.count {it.offering.deity==deity}
        val flightOffering=FlowerOffering(index,deity,IdolPlacement(selection).offering(deity,currentCount))
        flowerFlights.add(FlowerFlight(nextFlowerId++,flightOffering))
    }
    fun offerFlower(index:Int) {
        if(!entered || aging.needsCleaning || !foreground) return
        val covered=session.flowers+flowerFlights.map {it.offering.deity}
        val target=((0 until selection.deityCount).toList()-covered).randomOrNull() ?: (0 until selection.deityCount).random()
        offerFlowerToDeity(target, index)
    }
    fun light() {
        if(entered && !aging.needsCleaning) {
            session=session.copy(lit=true)
            audio.cue(com.worship.nityamandir.R.raw.flower_offering)
        }
    }
    fun deityAction(deity:Int) {
        if(!entered || aging.needsCleaning || bathTarget!=null) return
        when(session.step) {
            WorshipStep.BATH -> if(deity !in session.bathed) bathTarget=deity
            WorshipStep.TILAK -> {session=session.copy(tilak=session.tilak+deity);audio.cue(com.worship.nityamandir.R.raw.flower_offering)}
            else -> offerFlowerToDeity(deity,(0 until TempleSceneLayout.FLOWER_COUNT).random())
        }
    }
    fun startAarti() {
        if(entered && !aging.needsCleaning && !aartiRunning) {
            session=session.copy(aartiLit=true)
            bhajan.startAarti()
            aartiRunning=true
        }
    }
    LaunchedEffect(bathTarget) {
        val target=bathTarget ?: return@LaunchedEffect
        audio.cue(com.worship.nityamandir.R.raw.water_offering)
        try {bath.snapTo(0f);bath.animateTo(1f,tween(2800,easing=LinearEasing))}
        finally {audio.stopCue(com.worship.nityamandir.R.raw.water_offering)}
        session=session.copy(bathed=session.bathed+target);bathTarget=null
    }
    flowerFlights.toList().forEach { flight ->
        key(flight.id) {
            LaunchedEffect(Unit) {
                audio.cue(com.worship.nityamandir.R.raw.flower_offering)
                flight.progress.animateTo(1f,tween(1350,easing=LinearEasing))
                val offering=flight.offering
                session=session.offerFlower(offering.flowerIndex,offering.deity,offering.position,offering.rotation)
                flowerFlights.remove(flight)
            }
        }
    }
    fun offerPrasad() {
        if(entered && foreground && !aging.needsCleaning && !prasadRunning && !session.prasadOffered) prasadRunning=true
    }
    LaunchedEffect(prasadRunning) {
        if(!prasadRunning) return@LaunchedEffect
        audio.cue(com.worship.nityamandir.R.raw.flower_offering)
        prasad.snapTo(0f);prasad.animateTo(1f,tween(4000,easing=LinearEasing))
        session=session.copy(prasadOffered=true);prasadRunning=false
    }
    LaunchedEffect(aartiRunning,foreground) {
        if(!aartiRunning) return@LaunchedEffect
        if(!foreground) {aartiRunning=false;return@LaunchedEffect}
        try {
            audio.cue(com.worship.nityamandir.R.raw.bell,loop=true)
            aarti.snapTo(0f);aarti.animateTo(1f,tween(21000,easing=LinearEasing))
            session=session.copy(aartiComplete=true);aartiRunning=false
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.bell)}
    }
    LaunchedEffect(bellRunning,foreground) {
        if(!bellRunning) return@LaunchedEffect
        if(!foreground) {bellRunning=false;return@LaunchedEffect}
        try {
            audio.cue(com.worship.nityamandir.R.raw.bell)
            bell.snapTo(0f);bell.animateTo(1f,tween(3500,easing=LinearEasing))
            if(session.step==WorshipStep.BELL) session=session.copy(bellRung=true)
            bellRunning=false
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.bell)}
    }
    LaunchedEffect(conchRunning,foreground) {
        if(!conchRunning) return@LaunchedEffect
        if(!foreground) {conchRunning=false;return@LaunchedEffect}
        try {
            audio.cue(com.worship.nityamandir.R.raw.conch)
            conch.snapTo(0f);conch.animateTo(1f,tween(13500,easing=LinearEasing))
            if(session.step==WorshipStep.CONCH) session=session.copy(conchBlown=true)
            conchRunning=false
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.conch)}
    }
    LaunchedEffect(session, busy, entered, foreground, aging.needsCleaning) {
        if(!entered || !foreground || aging.needsCleaning || busy || session.complete || !session.canContinue) return@LaunchedEffect
        delay(650)
        session=session.next()
        if(session.complete) {
            last=System.currentTimeMillis()+timeOffset;clean=last;now=last
            prefs.edit().putLong("last",last).putLong("clean",clean).apply()
        }
    }
    LaunchedEffect(session.aartiComplete) {
        if(session.aartiComplete) {
            account.recordCompletion()
            try {account.sync()} catch(_:Exception) { /* Retry explicitly from the account screen. */ }
        }
    }
    LaunchedEffect(entered) {if(entered) bhajan.preload()}
    val cream=Color(0xFFFFF7EC)
    Box(modifier.fillMaxSize().background(cream)) {
        Box(Modifier.fillMaxSize().onGloballyPositioned {sceneSize=it.size;val location=IntArray(2);hostView.getLocationOnScreen(location);sceneOrigin=it.positionInRoot()+Offset(location[0].toFloat(),location[1].toFloat())}) {
            if(!customizer) key(selection) { MandirAltarView(session,aging.dustLevel,aging.flowerWitherFactor*(1-wipe),
                bathTarget,bath.value,flowerFlights.toList(),aartiRunning,aartiMotion,
                onDeityClick={deityAction(it)},onDiyaClick={light()},onBellClick={ringBell()},
                prasadRunning=prasadRunning,prasadProgress=prasad.value,
                cobwebLevel=aging.cobwebLevel,cleanedAreas=cleanedAreas.toList(),
                bellRunning=bellRunning || aartiRunning,bellProgress=if(aartiRunning) aartiMotion else bell.value,conchRunning=conchRunning,conchProgress=conch.value,
                onFlowerClick={offerFlower(it)},onConchClick={soundConch()},onAartiClick={startAarti()},onPrasadClick={offerPrasad()},selection=selection,
                showModels=curtain.value>=1f) }
            if(!customizer && curtain.value<1f) TempleDoors(curtain.value)
            // A single invisible entrance target covers the entire closed doorway.
            // Keep consuming touches during motion so taps cannot reach ritual items.
            if(!entered && !curtainMoving) Box(Modifier.matchParentSize().clickable(
                interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication=null,
                onClickLabel=tr("मंदिर खोलें","Open temple"),
                role=androidx.compose.ui.semantics.Role.Button,
                onClick={entered=true}
            ))
            else if(curtainMoving) Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope { while(true) { awaitPointerEvent().changes.forEach { it.consume() } } }
            })
            if(entered && !curtainMoving && aging.needsCleaning) Box(Modifier.matchParentSize().pointerInput(timeOffset,selection,cleaningSweep) {
                detectDragGestures {change,_ ->
                    change.consume()
                    if(!cleaningSweep) {
                        val viewport=TempleViewport(size.width.toFloat(),size.height.toFloat())
                        val point=TemplePoint((change.position.x-viewport.left)/viewport.imageWidth,change.position.y/viewport.imageWidth)
                        if(shrineSpace.contains(point) && cleanedAreas.none {kotlin.math.hypot(it.x-point.x,it.y-point.y)<.018f}) {
                            cleanedAreas.add(point);wipe=ShrineCleaning.coverage(shrineSpace,cleanedAreas)
                            if(wipe>=.9f) {finishCleaning();session=WorshipSession(deityCount=selection.deityCount)}
                        }
                    }
                }
            })
            // Show the ritual dock only after the entrance has fully opened.
            if(entered && !curtainMoving && !musicSheet && !devotionSheet && !customizer) Popup(alignment=Alignment.BottomCenter) { RitualGlassPanel(Modifier.navigationBarsPadding().padding(horizontal=14.dp,vertical=10.dp)
                .fillMaxWidth().heightIn(max=270.dp).verticalScroll(rememberScrollState()),sceneSize,sceneOrigin,backgroundPath=if(selection["shrine"]=="original") null else "shrine/backgrounds/${selection["shrine"]}.png") {
                val buttonColors=ButtonDefaults.buttonColors(containerColor=RitualInk,contentColor=cream,disabledContainerColor=Color(0xCFE2D9CF),disabledContentColor=Color(0xFF81766C))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreHoriz,tr("मंदिर विकल्प","Temple options"))}
                }
                if(aging.needsCleaning) {
                    Text(tr("मंदिर की स्वच्छता","Refresh your temple"),color=RitualInk,fontSize=20.sp)
                    Text(tr("दिन ${ (aging.elapsedHours/24).toInt() } · मंदिर पर उंगली फेरें","Day ${(aging.elapsedHours/24).toInt()} · Swipe over the altar to wipe away dust"),color=RitualGold)
                    Button(onClick={cleaningSweep=true;session=WorshipSession(deityCount=selection.deityCount)},enabled=!cleaningSweep,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),colors=buttonColors) {Text(tr("छूकर मंदिर साफ़ करें","Tap to clean temple"))}
                    LinearProgressIndicator(progress={wipe},modifier=Modifier.fillMaxWidth(),color=RitualGold)
                } else if(aartiRunning) {
                    Text(tr("आरती चल रही है …","Offering aarti …"),color=RitualInk,fontSize=16.sp)
                    LinearProgressIndicator(progress={aarti.value},modifier=Modifier.fillMaxWidth().height(2.dp),color=RitualGold)
                } else if(session.aartiComplete) {
                    Text(tr("पूजा संपन्न हुई। आपका दिन मंगलमय हो।","Puja complete. May your day be peaceful."),color=RitualInk,fontSize=18.sp)
                    DropdownMenuItem(
                        leadingIcon={Icon(Icons.Outlined.AccountCircle,null)},
                        text={Text(tr("मेरी प्रोफ़ाइल","My profile"))},
                        onClick={devotionPage=DevotionPage.PROFILE;devotionSheet=true}
                    )
                } else {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        repeat(WorshipStep.values().size) {i -> Box(Modifier.weight(1f).height(2.dp).background(if(i<=session.step.ordinal) Color(0xFFB39268) else Color(0xFFE0D8CF),RoundedCornerShape(4.dp)))}
                    }
                    AnimatedContent(targetState=session.step,transitionSpec={ (slideInHorizontally {it}+fadeIn()) togetherWith (slideOutHorizontally {-it}+fadeOut()) },label="ritual step") { displayedStep ->
                    when(displayedStep) {
                        WorshipStep.LIGHT -> RitualChoice(tr("दीप जलाएँ","Light oil lamp"),session.lit,{light()},Modifier.fillMaxWidth())
                        WorshipStep.FLOWERS -> RitualChoice(tr("फूल अर्पित करें","Offer flowers"),session.flowers.size==selection.deityCount,{offerFlower(0)},Modifier.fillMaxWidth(),enabled=!ritualBusy)
                        WorshipStep.BATH, WorshipStep.TILAK -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            val done=when(displayedStep) {WorshipStep.BATH -> session.bathed;WorshipStep.TILAK -> session.tilak;else -> session.flowers}
                            selection.deityNames(hindi).forEachIndexed {i,label ->
                                RitualChoice(if(bathTarget==i) "$label …" else label,i in done,{deityAction(i)},Modifier.weight(1f),enabled=!busy)
                            }
                        }
                        WorshipStep.BELL -> RitualChoice(tr("घंटी बजाएँ","Ring bell"),session.bellRung,{ringBell()},Modifier.fillMaxWidth())
                        WorshipStep.CONCH -> RitualChoice(tr("शंख बजाएँ","Sound conch"),session.conchBlown,{soundConch()},Modifier.fillMaxWidth())
                        WorshipStep.PRASAD -> RitualChoice(tr("प्रसाद अर्पित करें","Offer prasad"),session.prasadOffered,{offerPrasad()},Modifier.fillMaxWidth(),enabled=!prasadRunning)
                        WorshipStep.AARTI -> RitualChoice(if(aartiRunning) tr("आरती चल रही है …","Offering aarti …") else tr("दीप आरती आरंभ करें","Begin diya aarti"),session.aartiComplete,{startAarti()},Modifier.fillMaxWidth(),enabled=!aartiRunning)
                    }
                    }
                    if(busy) LinearProgressIndicator(progress={when {prasadRunning -> prasad.value;bathTarget!=null -> bath.value;flowerFlights.isNotEmpty() -> flowerFlights.first().progress.value;bellRunning -> bell.value;conchRunning -> conch.value;else -> aarti.value}},modifier=Modifier.fillMaxWidth().height(2.dp),color=RitualGold)

                }
            } }
        }
        if(!menu && !musicSheet && !devotionSheet && !customizer && !curtainMoving) Popup(alignment=Alignment.TopCenter) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=14.dp,vertical=8.dp)) {
                if(bhajan.current!=null || bhajan.loading) BhajanMiniPlayer(bhajan,hindi,{musicSheet=true})
                if(!entered) Surface(Modifier.align(Alignment.End).padding(top=8.dp),shape=RoundedCornerShape(16.dp),color=cream,shadowElevation=4.dp) {
                    IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreHoriz,tr("मंदिर विकल्प","Temple options"))}
                }
            }
        }
        if(customizer) ShrineControlsTheme { ShrineCustomizer(account,customizerDraft ?: selection,hindi,
            onDismiss={customizer=false;customizerDraft=null},onAccount={customizerDraft=it;customizer=false;devotionPage=DevotionPage.PACKAGE;devotionSheet=true},
            onApplied={selection=it;customizerDraft=null;session=WorshipSession(deityCount=it.deityCount);customizer=false},
            player=bhajan,onMusic={customizer=false;musicSheet=true}) }
        if(devotionSheet) ShrineControlsTheme { DevotionSheet(account,hindi,{devotionSheet=false},onCustomize={devotionSheet=false;customizer=true},canCustomize=!busy,page=devotionPage,
            player=bhajan,onMusic={devotionSheet=false;musicSheet=true}) }
        if(musicSheet) BhajanPlayerSheet(bhajan,hindi,{musicSheet=false})
        ShrineControlsTheme {
            MandirNavigationDrawer(menu,{menu=false},hindi) {
                if(bhajan.current != null || bhajan.loading) BhajanMiniPlayer(bhajan,hindi,{menu=false;musicSheet=true})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.AccountCircle,null)},text={Text(tr("मेरी प्रोफ़ाइल","My profile"))},onClick={menu=false;devotionPage=DevotionPage.PROFILE;devotionSheet=true})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.AutoAwesome,null)},text={Text(tr("मंदिर पैकेज","Temple package"))},onClick={menu=false;devotionPage=DevotionPage.PACKAGE;devotionSheet=true})
                HorizontalDivider()
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.MusicNote,null)},text={Text(tr("संगीत प्लेयर खोलें","Open music player"))},onClick={menu=false;musicSheet=true})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.Translate,null)},text={Text(if(hindi) "English" else "हिंदी")},onClick={hindi=!hindi;prefs.edit().putBoolean("hindi",hindi).apply();menu=false})
                if(BuildConfig.DEBUG) DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.Schedule,null)},text={Text(tr("एक दिन आगे बढ़ाएँ (परीक्षण)","Pass a day (test)"))},enabled=!busy,onClick={
                    timeOffset+=AgingEngine.DAY_MS
                    now=System.currentTimeMillis()+timeOffset
                    prefs.edit().putLong("agingOffset",timeOffset).apply()
                    wipe=0f;cleanedAreas.clear();menu=false
                })
            }
        }
    }
}

/** Separate window keeps the drawer above the altar's native SurfaceView. */
@Composable
private fun MandirNavigationDrawer(open: Boolean, onDismiss: () -> Unit, hindi: Boolean, content: @Composable ColumnScope.() -> Unit) {
    if (!open) return
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,
        properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().clickable(onClick=onDismiss), contentAlignment=Alignment.CenterEnd) {
            Surface(Modifier.fillMaxHeight().widthIn(max=380.dp).fillMaxWidth().clickable { },
                color=Color(0xFFFFF7EC), shape=RoundedCornerShape(topStart=24.dp,bottomStart=24.dp)) {
                Column(Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(hindi) "मंदिर विकल्प" else "Temple options",Modifier.weight(1f),fontSize=20.sp)
                        IconButton(onClick=onDismiss) {Icon(Icons.Outlined.Close,if(hindi) "बंद करें" else "Close navigation drawer")}
                    }
                    content()
                }
            }
        }
    }
}

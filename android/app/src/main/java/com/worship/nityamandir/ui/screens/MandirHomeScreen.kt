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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.font.FontWeight
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
    val accountScope=rememberCoroutineScope()
    var accountError by remember {mutableStateOf("")}
    var loggingOut by remember {mutableStateOf(false)}
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
    var woodenDoor by remember {mutableStateOf(prefs.getBoolean("woodenDoor",true))}
    var saffron by remember {mutableStateOf(prefs.getBoolean("saffron",false))}
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
    fun closeTemple() {entered=false;menu=false;musicSheet=false;audio.stop();bhajan.pause();bathTarget=null;flowerFlights.clear();aartiRunning=false;bellRunning=false;conchRunning=false;prasadRunning=false}
    fun ringBell() {
        if(entered && foreground && !aging.needsCleaning && !busy) bellRunning=true
    }
    fun soundConch() {
        if(entered && foreground && !aging.needsCleaning && !busy) conchRunning=true
    }
    fun offerFlower(index:Int) {
        if(!entered || aging.needsCleaning || busy || !foreground) return
        val covered=session.flowers+flowerFlights.map {it.offering.deity}
        val target=((0 until selection.deityCount).toList()-covered).randomOrNull() ?: (0 until selection.deityCount).random()
        flowerFlights.add(FlowerFlight(nextFlowerId++,FlowerOffering(index,target,IdolPlacement(selection).offering(target,session.offeredFlowers.count {it.deity==target}+flowerFlights.count {it.offering.deity==target}))))
    }
    fun light() {if(entered && !aging.needsCleaning && session.step==WorshipStep.LIGHT) {session=session.copy(lit=true);audio.cue(com.worship.nityamandir.R.raw.flower_offering)}}
    fun deityAction(deity:Int) {
        if(!entered || aging.needsCleaning || busy) return
        when(session.step) {
            WorshipStep.BATH -> if(deity !in session.bathed) bathTarget=deity
            WorshipStep.TILAK -> {session=session.copy(tilak=session.tilak+deity);audio.cue(com.worship.nityamandir.R.raw.flower_offering)}
            WorshipStep.FLOWERS -> offerFlower((0 until TempleSceneLayout.FLOWER_COUNT).random())
            else -> Unit
        }
    }
    fun startAarti() {if(entered && !aging.needsCleaning && session.step==WorshipStep.AARTI && !busy && !session.aartiComplete) {session=session.copy(aartiLit=true);bhajan.startAarti();aartiRunning=true}}
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
        if(entered && foreground && !aging.needsCleaning && !busy && session.step==WorshipStep.PRASAD && !session.prasadOffered) prasadRunning=true
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
            conch.snapTo(0f);conch.animateTo(1f,tween(6000,easing=LinearEasing))
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
    if(accountError.isNotEmpty()) AlertDialog(onDismissRequest={accountError=""},title={Text(tr("खाता","Account"))},text={Text(accountError)},confirmButton={TextButton(onClick={accountError=""}) {Text("OK")}})
    val cream=Color(0xFFFFF7EC)
    Box(modifier.fillMaxSize().background(cream)) {
        Box(Modifier.fillMaxSize().onGloballyPositioned {sceneSize=it.size;val location=IntArray(2);hostView.getLocationOnScreen(location);sceneOrigin=it.positionInRoot()+Offset(location[0].toFloat(),location[1].toFloat())}) {
            if((woodenDoor || entered || curtain.value>0f) && !customizer) key(selection) { MandirAltarView(session,aging.dustLevel,aging.flowerWitherFactor*(1-wipe),
                bathTarget,bath.value,flowerFlights.toList(),aartiRunning,aartiMotion,
                onDeityClick={deityAction(it)},onDiyaClick={light()},onBellClick={ringBell()},
                prasadRunning=prasadRunning,prasadProgress=prasad.value,
                cobwebLevel=aging.cobwebLevel,cleanedAreas=cleanedAreas.toList(),
                bellRunning=bellRunning || aartiRunning,bellProgress=if(aartiRunning) aartiMotion else bell.value,conchRunning=conchRunning,conchProgress=conch.value,
                onFlowerClick={offerFlower(it)},onConchClick={soundConch()},onAartiClick={startAarti()},selection=selection) }
            if(woodenDoor && !customizer && curtain.value<1f) TempleDoors(curtain.value)
            else if(!woodenDoor && curtain.value<1f) TempleCurtains(curtain.value,saffron)
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
                    DevotionStreak(account.streakDays,hindi,account.profile?.optBoolean("unlocked")==true)
                    Button(onClick={devotionPage=DevotionPage.PROFILE;devotionSheet=true},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),colors=buttonColors) {Text(tr("मेरी प्रोफ़ाइल","My profile"))}
                } else {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(hindi) session.step.hi else session.step.en,Modifier.weight(1f),color=RitualInk,fontSize=20.sp,fontWeight=FontWeight.Medium)
                        Text("${session.step.ordinal+1} / ${WorshipStep.values().size}",color=RitualGold,fontSize=16.sp)
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        repeat(WorshipStep.values().size) {i -> Box(Modifier.weight(1f).height(2.dp).background(if(i<=session.step.ordinal) Color(0xFFB39268) else Color(0xFFE0D8CF),RoundedCornerShape(4.dp)))}
                    }
                    AnimatedContent(targetState=session.step,transitionSpec={ (slideInHorizontally {it}+fadeIn()) togetherWith (slideOutHorizontally {-it}+fadeOut()) },label="ritual step") { displayedStep ->
                    when(displayedStep) {
                        WorshipStep.LIGHT -> RitualChoice(tr("दीप जलाएँ","Light oil lamp"),session.lit,{light()},Modifier.fillMaxWidth())
                        WorshipStep.FLOWERS -> RitualChoice(tr("फूल अर्पित करें","Offer flowers"),session.flowers.size==selection.deityCount,{offerFlower(0)},Modifier.fillMaxWidth(),enabled=!busy)
                        WorshipStep.BATH, WorshipStep.TILAK -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            val done=when(displayedStep) {WorshipStep.BATH -> session.bathed;WorshipStep.TILAK -> session.tilak;else -> session.flowers}
                            selection.deityNames(hindi).forEachIndexed {i,label ->
                                RitualChoice(if(bathTarget==i) "$label …" else label,i in done,{deityAction(i)},Modifier.weight(1f),enabled=!busy)
                            }
                        }
                        WorshipStep.BELL -> RitualChoice(tr("घंटी बजाएँ","Ring bell"),session.bellRung,{ringBell()},Modifier.fillMaxWidth())
                        WorshipStep.CONCH -> RitualChoice(tr("शंख बजाएँ","Sound conch"),session.conchBlown,{soundConch()},Modifier.fillMaxWidth())
                        WorshipStep.PRASAD -> RitualChoice(tr("प्रसाद अर्पित करें","Offer prasad"),session.prasadOffered,{offerPrasad()},Modifier.fillMaxWidth(),enabled=!busy)
                        WorshipStep.AARTI -> RitualChoice(if(aartiRunning) tr("आरती चल रही है …","Offering aarti …") else tr("दीप आरती आरंभ करें","Begin diya aarti"),session.aartiComplete,{startAarti()},Modifier.fillMaxWidth(),enabled=!aartiRunning)
                    }
                    }
                    if(busy) LinearProgressIndicator(progress={when {prasadRunning -> prasad.value;bathTarget!=null -> bath.value;flowerFlights.isNotEmpty() -> flowerFlights.first().progress.value;bellRunning -> bell.value;conchRunning -> conch.value;else -> aarti.value}},modifier=Modifier.fillMaxWidth().height(2.dp),color=RitualGold)

                }
            } }
        }
        if(customizer) ShrineControlsTheme { ShrineCustomizer(account,customizerDraft ?: selection,hindi,
            onDismiss={customizer=false;customizerDraft=null},onAccount={customizerDraft=it;customizer=false;devotionPage=DevotionPage.PACKAGE;devotionSheet=true},
            onApplied={selection=it;customizerDraft=null;session=WorshipSession(deityCount=it.deityCount);customizer=false}) }
        if(devotionSheet) ShrineControlsTheme { DevotionSheet(account,hindi,{devotionSheet=false},onCustomize={devotionSheet=false;customizer=true},canCustomize=!busy,page=devotionPage) }
        if(musicSheet) BhajanPlayerSheet(bhajan,hindi,{musicSheet=false})
        if(entered && !menu && !curtainMoving && !musicSheet && !devotionSheet && !customizer) Popup(alignment=Alignment.TopEnd) {
            Surface(
                modifier=Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=14.dp,vertical=8.dp),
                shape=RoundedCornerShape(20.dp),
                color=if(bhajan.current != null || bhajan.loading) Color(0xFFB84308) else Color.Transparent
            ) {
                Row(horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
                    if(bhajan.current != null || bhajan.loading) BhajanMiniPlayer(bhajan,hindi,{musicSheet=true},Modifier.weight(1f))
                    Box {
                        FilledIconButton(
                            onClick={menu=true},
                            modifier=Modifier.size(48.dp),
                            shape=CircleShape,
                            colors=IconButtonDefaults.filledIconButtonColors(containerColor=cream,contentColor=RitualGold)
                        ) {Icon(Icons.Outlined.MoreHoriz,tr("मंदिर विकल्प","Temple options"))}

                    }
                }
            }
        }
        ShrineControlsTheme {
            MandirNavigationDrawer(menu,{menu=false},hindi) {
                if(bhajan.current != null || bhajan.loading) BhajanMiniPlayer(bhajan,hindi,{menu=false;musicSheet=true})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.TempleHindu,null)},text={Text(tr("मंदिर सजाएँ","Customize temple"))},enabled=!busy,onClick={menu=false;customizer=true;audio.stop();bhajan.pause()})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.AccountCircle,null)},text={Text(tr("मेरी प्रोफ़ाइल","My profile"))},onClick={menu=false;devotionPage=DevotionPage.PROFILE;devotionSheet=true})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.AutoAwesome,null)},text={Text(tr("मंदिर पैकेज","Temple package"))},onClick={menu=false;devotionPage=DevotionPage.PACKAGE;devotionSheet=true})
                HorizontalDivider()
                DropdownMenuItem(leadingIcon={Icon(if(account.signedIn) Icons.Outlined.Logout else Icons.Outlined.Login,null)},text={Text(if(account.signedIn) tr("लॉग आउट","Log out") else tr("लॉग इन","Log in"))},enabled=!loggingOut && !busy,onClick={
                    menu=false
                    if(account.signedIn) accountScope.launch {
                        loggingOut=true
                        try {account.signOut();closeTemple()} catch(e:Exception) {accountError=e.message ?: "Could not log out"} finally {loggingOut=false}
                    } else {devotionPage=DevotionPage.LOGIN;devotionSheet=true}
                })
                HorizontalDivider()
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.MusicNote,null)},text={Text(tr("संगीत प्लेयर खोलें","Open music player"))},onClick={menu=false;musicSheet=true})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.Spa,null)},text={Text(tr("पूजा का वातावरण खोलें","Open puja environment"))},onClick={menu=false;musicSheet=false;entered=true})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.Translate,null)},text={Text(if(hindi) "English" else "हिंदी")},onClick={hindi=!hindi;prefs.edit().putBoolean("hindi",hindi).apply();menu=false})
                DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.DoorBack,null)},text={Text(if(woodenDoor) tr("रेशमी पर्दे चुनें","Use silk curtains") else tr("लकड़ी के द्वार चुनें","Use wooden doors"))},onClick={woodenDoor=!woodenDoor;prefs.edit().putBoolean("woodenDoor",woodenDoor).apply();menu=false})
                if(!woodenDoor) DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.Palette,null)},text={Text(tr("पर्दे का रंग बदलें","Change curtain colour"))},onClick={saffron=!saffron;prefs.edit().putBoolean("saffron",saffron).apply();menu=false})
                if(BuildConfig.DEBUG) DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.Schedule,null)},text={Text(tr("एक दिन आगे बढ़ाएँ (परीक्षण)","Pass a day (test)"))},enabled=!busy,onClick={
                    timeOffset+=AgingEngine.DAY_MS
                    now=System.currentTimeMillis()+timeOffset
                    prefs.edit().putLong("agingOffset",timeOffset).apply()
                    wipe=0f;cleanedAreas.clear();menu=false
                })
                if(entered) DropdownMenuItem(leadingIcon={Icon(Icons.Outlined.DoorBack,null)},text={Text(tr("पट बंद करें","Close temple"))},onClick={closeTemple()})
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

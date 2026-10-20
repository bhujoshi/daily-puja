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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.worship.nityamandir.engine.*
import com.worship.nityamandir.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun MandirHomeScreen(modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val hostView=LocalView.current
    val prefs=remember {context.getSharedPreferences("worship",Context.MODE_PRIVATE)}
    val account=remember {com.worship.nityamandir.data.DevotionAccount(context.applicationContext)}
    var devotionSheet by remember {mutableStateOf(false)}
    var devotionPage by remember {mutableStateOf(DevotionPage.PROFILE)}
    LaunchedEffect(Unit) {
        if(account.readReferral() && !account.signedIn) { devotionPage=DevotionPage.LOGIN;devotionSheet=true }
    }
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
            if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                (context as? android.app.Activity)?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
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
    LaunchedEffect(timeOffset,foreground) {
        if(!foreground) return@LaunchedEffect
        while(isActive) {now=System.currentTimeMillis()+timeOffset;delay(30000)}
    }
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
            aartiRunning=true
        }
    }
    LaunchedEffect(bathTarget) {
        val target=bathTarget ?: return@LaunchedEffect
        audio.cue(com.worship.nityamandir.R.raw.water_offering)
        try {bath.snapTo(0f);bath.animateTo(1f,tween(2900,easing=LinearEasing))}
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
            bhajan.startAarti()
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.bell)}
    }
    LaunchedEffect(bellRunning,foreground) {
        if(!bellRunning) return@LaunchedEffect
        if(!foreground) {bellRunning=false;return@LaunchedEffect}
        try {
            audio.cue(com.worship.nityamandir.R.raw.bell)
            bell.snapTo(0f);bell.animateTo(1f,tween(3500,easing=LinearEasing))
            session=session.copy(bellRung=true)
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
    val cream=Color(0xFFFAF6EE)
    Box(modifier.fillMaxSize().background(com.worship.nityamandir.ui.theme.DarkSanctumBackground)) {
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
            )) {
                if(!customizer) {
                    SacredEntrancePrompt(
                        hindi = hindi,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    )
                }
            }
            else if(curtainMoving) Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope { while(true) { awaitPointerEvent().changes.forEach { it.consume() } } }
            })
            if(entered && !curtainMoving && aging.needsCleaning) Box(Modifier.matchParentSize().pointerInput(timeOffset,selection,cleaningSweep) {
                detectDragGestures {change,_ ->
                    change.consume()
                    if(!cleaningSweep) {
                        val viewport=TempleViewport(size.width.toFloat(),size.height.toFloat())
                        val point=TemplePoint((change.position.x-viewport.left)/viewport.imageWidth,change.position.y/viewport.imageWidth)
                        if(ShrineCleaning.contains(shrineSpace,point) && cleanedAreas.none {kotlin.math.hypot(it.x-point.x,it.y-point.y)<.018f}) {
                            cleanedAreas.add(point);wipe=ShrineCleaning.coverage(shrineSpace,cleanedAreas)
                            if(wipe>=.9f) {finishCleaning();session=WorshipSession(deityCount=selection.deityCount)}
                        }
                    }
                }
            })
            // Show the ritual dock only after the entrance has fully opened.
            if(entered && !curtainMoving && !musicSheet && !devotionSheet && !customizer) Box(Modifier.align(Alignment.BottomCenter)) { RitualGlassPanel(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(horizontal=14.dp,vertical=10.dp)
                .fillMaxWidth().heightIn(max=270.dp).verticalScroll(rememberScrollState()).testTag("ritual-dock"),sceneSize,sceneOrigin,backgroundPath="shrine/backgrounds/${selection["shrine"]}.png") {
                val buttonColors=ButtonDefaults.buttonColors(containerColor=RitualInk,contentColor=cream,disabledContainerColor=Color(0xCFE2D9CF),disabledContentColor=Color(0xFF81766C))
                if(aging.needsCleaning) {
                    Text(tr("पूजा से पहले मंदिर साफ़ करें","Clean the mandir before puja"),fontSize=13.sp)
                    Button(onClick={cleaningSweep=true;session=WorshipSession(deityCount=selection.deityCount)},enabled=!cleaningSweep,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),colors=buttonColors) {Text(tr("छूकर मंदिर साफ़ करें","Tap to clean temple"))}
                } else if(aartiRunning) {
                    PujaStepGuide(WorshipStep.AARTI,hindi)
                    RitualChoice(tr("आरती चल रही है …","Offering aarti …"),false,{},Modifier.fillMaxWidth(),enabled=false)
                } else if(session.aartiComplete) {
                    Text(tr("पूजा पूरी हुई","Puja complete"),fontSize=13.sp)
                    RitualChoice(tr("मेरी प्रोफ़ाइल","My profile"),false,
                        {devotionPage=DevotionPage.PROFILE;devotionSheet=true},Modifier.fillMaxWidth())
                } else {
                    AnimatedContent(targetState=session.step,transitionSpec={ (slideInHorizontally {it}+fadeIn()) togetherWith (slideOutHorizontally {-it}+fadeOut()) },label="ritual step") { displayedStep ->
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    PujaStepGuide(displayedStep,hindi)
                    when(displayedStep) {
                        WorshipStep.LIGHT -> RitualChoice(tr("दीप जलाएँ","Light oil lamp"),session.lit,{light()},Modifier.fillMaxWidth())
                        WorshipStep.FLOWERS -> RitualChoice(tr("फूल अर्पित करें","Offer flowers"),session.flowers.size==selection.deityCount,{offerFlower(0)},Modifier.fillMaxWidth(),enabled=!ritualBusy)
                        WorshipStep.BATH, WorshipStep.TILAK -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            val done=when(displayedStep) {WorshipStep.BATH -> session.bathed;WorshipStep.TILAK -> session.tilak;else -> session.flowers}
                            selection.deityNames(hindi).forEachIndexed {i,label ->
                                RitualChoice(if(bathTarget==i) "$label …" else label,i in done,{deityAction(i)},Modifier.weight(1f),enabled=!busy)
                            }
                        }
                        WorshipStep.CONCH -> RitualChoice(tr("शंख बजाएँ","Sound conch"),session.conchBlown,{soundConch()},Modifier.fillMaxWidth())
                        WorshipStep.PRASAD -> RitualChoice(tr("प्रसाद अर्पित करें","Offer prasad"),session.prasadOffered,{offerPrasad()},Modifier.fillMaxWidth(),enabled=!prasadRunning)
                        WorshipStep.AARTI -> RitualChoice(if(aartiRunning) tr("आरती चल रही है …","Offering aarti …") else tr("दीप आरती आरंभ करें","Begin diya aarti"),session.aartiComplete,{startAarti()},Modifier.fillMaxWidth(),enabled=!aartiRunning)
                    }
                    }
                    }
                }
            } }
        }
        // Global options stay outside the ritual dock, so a single action needs only one row.
        if(entered && curtain.value >= 1f && !menu && !musicSheet && !devotionSheet && !customizer && !curtainMoving) {
            val hasMusic=bhajan.current!=null || bhajan.loading
            Box(Modifier.align(if(hasMusic) Alignment.TopCenter else Alignment.TopEnd)) {
                Row((if(hasMusic) Modifier.fillMaxWidth() else Modifier).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)).padding(horizontal=14.dp,vertical=8.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Top) {
                    if(hasMusic) {
                        BhajanMiniPlayer(bhajan,hindi,{musicSheet=true},Modifier.weight(1f),showTrackNavigation=false,compact=true)
                    }
                    SacredMenuButton(
                        onClick={menu=true},
                        contentDescription=tr("मंदिर विकल्प","Temple options")
                    )
                }
            }
        }
        if(customizer) ShrineControlsTheme { ShrineCustomizer(account,customizerDraft ?: selection,hindi,
            onDismiss={customizer=false;customizerDraft=null},onAccount={customizerDraft=it;customizer=false;devotionPage=if(account.signedIn) DevotionPage.PACKAGE else DevotionPage.LOGIN;devotionSheet=true},
            onApplied={selection=it;customizerDraft=null;session=WorshipSession(deityCount=it.deityCount);customizer=false},
            player=bhajan,onMusic={customizerDraft=it;customizer=false;musicSheet=true}) }
        if(devotionSheet) ShrineControlsTheme { DevotionSheet(account,hindi,{devotionSheet=false},onCustomize={devotionSheet=false;customizer=true},canCustomize=!busy,page=devotionPage,
            player=bhajan,onMusic={devotionSheet=false;musicSheet=true},
            onSignedIn={if(customizerDraft!=null) {devotionSheet=false;customizer=true}}) }
        if(musicSheet) BhajanPlayerSheet(bhajan,hindi,{musicSheet=false;if(customizerDraft!=null) customizer=true})
        ShrineControlsTheme {
            MandirNavigationDrawer(menu,{menu=false},hindi) {
                if(bhajan.current != null || bhajan.loading) {
                    BhajanMiniPlayer(bhajan,hindi,{menu=false;musicSheet=true})
                    Spacer(Modifier.height(4.dp))
                }
                // Profile & Devotion Coins Card
                SacredDrawerCard(
                    title=tr("मेरी प्रोफ़ाइल","My profile"),
                    subtitle=if(account.signedIn) tr("भक्ति खाता जुड़ा है","Devotion account active") else tr("प्रगति और सिक्के सहेजें","Save progress & coins"),
                    leadingIcon={Icon(Icons.Outlined.AccountCircle,null,tint=RitualGold,modifier=Modifier.size(24.dp))},
                    trailingContent={
                        Surface(
                            shape=RoundedCornerShape(50),
                            color=Color(0xFFF3E2C3),
                            border=BorderStroke(1.dp,Color(0x66B3833B))
                        ) {
                            Row(
                                Modifier.padding(horizontal=10.dp,vertical=4.dp),
                                verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Toll,null,tint=RitualGold,modifier=Modifier.size(15.dp))
                                Text("${account.coins}",fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,color=RitualInk)
                            }
                        }
                    },
                    onClick={menu=false;devotionPage=DevotionPage.PROFILE;devotionSheet=true}
                )

                // Featured Customization Card: Mandir Sajaye
                SacredDrawerCard(
                    title=tr("मंदिर सजाएँ","Mandir Sajaye"),
                    subtitle=tr("मूर्तियाँ, फूल और दीपक बदलें","Customize idols, flowers & lamps"),
                    leadingIcon={Icon(Icons.Outlined.AutoAwesome,null,tint=Color(0xFF9C6514),modifier=Modifier.size(22.dp))},
                    featured=true,
                    enabled=!busy,
                    onClick={menu=false;customizerDraft=null;customizer=true}
                )

                // Music Player Card
                SacredDrawerCard(
                    title=tr("संगीत प्लेयर खोलें","Open music player"),
                    subtitle=tr("भजन और आरती सुनें","Listen to bhajans & aarti"),
                    leadingIcon={Icon(Icons.Outlined.MusicNote,null,tint=RitualGold,modifier=Modifier.size(22.dp))},
                    onClick={menu=false;musicSheet=true}
                )

                // WhatsApp Share Card
                SacredDrawerCard(
                    title=tr("WhatsApp पर साझा करें","Share on WhatsApp"),
                    subtitle=tr("परिवार और मित्रों को आमंत्रित करें","Invite family & friends"),
                    leadingIcon={WhatsAppIcon()},
                    onClick={menu=false;devotionPage=DevotionPage.SHARE;devotionSheet=true}
                )

                // Direct Language Toggle Pill Row
                SacredDrawerCard(
                    title=if(hindi) "English" else "हिंदी",
                    subtitle=if(hindi) "Switch app language to English" else "ऐप की भाषा हिंदी में बदलें",
                    leadingIcon={Icon(Icons.Outlined.Translate,null,tint=RitualGold,modifier=Modifier.size(22.dp))},
                    trailingContent={
                        Surface(
                            shape=RoundedCornerShape(50),
                            color=Color(0xFFEDE0CE),
                            border=BorderStroke(1.dp,Color(0x55B3833B))
                        ) {
                            Row(Modifier.padding(2.dp),verticalAlignment=Alignment.CenterVertically) {
                                Surface(
                                    shape=RoundedCornerShape(50),
                                    color=if(hindi) RitualInk else Color.Transparent,
                                    contentColor=if(hindi) cream else RitualInk
                                ) {
                                    Text("हिं",Modifier.padding(horizontal=8.dp,vertical=3.dp),fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                                }
                                Surface(
                                    shape=RoundedCornerShape(50),
                                    color=if(!hindi) RitualInk else Color.Transparent,
                                    contentColor=if(!hindi) cream else RitualInk
                                ) {
                                    Text("EN",Modifier.padding(horizontal=8.dp,vertical=3.dp),fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                                }
                            }
                        }
                    },
                    onClick={hindi=!hindi;prefs.edit().putBoolean("hindi",hindi).apply();menu=false}
                )

                if(BuildConfig.DEBUG) {
                    SacredDrawerCard(
                        title=tr("एक दिन आगे बढ़ाएँ (परीक्षण)","Pass a day (test)"),
                        subtitle=tr("मंदिर की सफ़ाई अवस्था जाँचें","Test temple cleaning state"),
                        leadingIcon={Icon(Icons.Outlined.Schedule,null,tint=RitualGold,modifier=Modifier.size(22.dp))},
                        enabled=!busy,
                        onClick={
                            timeOffset+=AgingEngine.DAY_MS
                            now=System.currentTimeMillis()+timeOffset
                            prefs.edit().putLong("agingOffset",timeOffset).apply()
                            wipe=0f;cleanedAreas.clear();menu=false
                        }
                    )
                }
            }
        }
    }
}

/** Translucent frosted-brass temple medallion button replacing the plain boxy hamburger icon. */
@Composable
private fun SacredMenuButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .shadow(10.dp, shape, ambientColor = Color(0x661A1108), spotColor = Color(0x661A1108))
            .semantics {
                this.contentDescription = contentDescription
            },
        shape = shape,
        color = Color.Transparent,
        contentColor = RitualInk,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(Color(0xFFF7E2A4), Color(0xFFB3833B), Color(0xFFE5C168), Color(0xFF8F6324))
            )
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xF5FFFDF9), Color(0xECFAF0DF), Color(0xE0EAD8BE))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f
                // Subtle inner brass filigree ring
                drawCircle(
                    color = Color(0x44B3833B),
                    radius = r - 4.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
                // Four cardinal golden bindu accents on the inner ring
                val dotR = 1.3.dp.toPx()
                val ringR = r - 4.dp.toPx()
                listOf(
                    Offset(cx, cy - ringR),
                    Offset(cx, cy + ringR),
                    Offset(cx - ringR, cy),
                    Offset(cx + ringR, cy)
                ).forEach { pt ->
                    drawCircle(Color(0xFFB3833B), radius = dotR, center = pt)
                }

                // Sacred Kalash / Lotus finial crest above the three ritual lines
                val crestPath = Path().apply {
                    moveTo(cx, cy - 9.5.dp.toPx())
                    quadraticBezierTo(cx + 3.5.dp.toPx(), cy - 6.5.dp.toPx(), cx, cy - 5.dp.toPx())
                    quadraticBezierTo(cx - 3.5.dp.toPx(), cy - 6.5.dp.toPx(), cx, cy - 9.5.dp.toPx())
                    close()
                }
                drawPath(crestPath, Color(0xFFB3833B))

                // Three refined ceremonial menu bars with tapered proportions
                val stroke = 2.1.dp.toPx()
                val cap = StrokeCap.Round
                // Top bar
                drawLine(
                    color = RitualInk,
                    start = Offset(cx - 7.5.dp.toPx(), cy - 2.dp.toPx()),
                    end = Offset(cx + 7.5.dp.toPx(), cy - 2.dp.toPx()),
                    strokeWidth = stroke,
                    cap = cap
                )
                // Middle wider bar with golden tips
                drawLine(
                    color = Color(0xFF8C5E1A),
                    start = Offset(cx - 9.5.dp.toPx(), cy + 2.8.dp.toPx()),
                    end = Offset(cx + 9.5.dp.toPx(), cy + 2.8.dp.toPx()),
                    strokeWidth = stroke,
                    cap = cap
                )
                // Bottom bar
                drawLine(
                    color = RitualInk,
                    start = Offset(cx - 6.5.dp.toPx(), cy + 7.6.dp.toPx()),
                    end = Offset(cx + 6.5.dp.toPx(), cy + 7.6.dp.toPx()),
                    strokeWidth = stroke,
                    cap = cap
                )
            }
        }
    }
}

@Composable
private fun SacredDrawerCard(
    title: String,
    subtitle: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    featured: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val bgBrush = if (featured) {
        Brush.horizontalGradient(listOf(Color(0xFFFDF3E1), Color(0xFFF5E1BE)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFFFFDF9), Color(0xFFF8EFE2)))
    }
    val borderColor = if (featured) Color(0xFFB3833B) else Color(0x44B3833B)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(if (featured) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (featured) 2.dp else 0.dp
    ) {
        Row(
            Modifier
                .background(bgBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = if (featured) Color(0xFFEED3A1) else Color(0xFFF3E7D3),
                border = BorderStroke(1.dp, Color(0x40B3833B))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    leadingIcon()
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (featured) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (enabled) RitualInk else Color(0xFF8C8074)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF6E5D4F),
                    maxLines = 1
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF9E7B42),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Separate window keeps the drawer above the altar's native SurfaceView. */
@Composable
private fun MandirNavigationDrawer(open: Boolean, onDismiss: () -> Unit, hindi: Boolean, content: @Composable ColumnScope.() -> Unit) {
    if (!open) return
    Dialog(onDismissRequest=onDismiss,
        properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x660D0B0A))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.CenterEnd
        ) {
            val drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
            Surface(
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(0.88f)
                    .clickable(enabled = false) { },
                color = Color(0xFFFAF6EE),
                shape = drawerShape,
                shadowElevation = 20.dp,
                border = BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(listOf(Color(0xFFE8C878), Color(0xFFB3833B), Color(0xFFE8C878)))
                )
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFFDF8), Color(0xFFFAF3E6), Color(0xFFF3E7D3))
                            )
                        )
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sacred Header with ornamental subtitle & close button
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (hindi) "ॐ · पवित्र मंदिर" else "OM · PAVITRA MANDIR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RitualGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (hindi) "मंदिर विकल्प" else "Temple options",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = RitualInk
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF3E7D3),
                            border = BorderStroke(1.dp, Color(0x55B3833B)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = onDismiss, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = if (hindi) "बंद करें" else "Close navigation drawer",
                                    tint = RitualInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0x33B3833B), thickness = 1.dp)
                    content()
                }
            }
        }
    }
}

@Composable
private fun SacredEntrancePrompt(hindi: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color(0x661A1108), spotColor = Color(0x661A1108)),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(Color(0xFFF7E2A4), Color(0xFFB3833B), Color(0xFFE5C168), Color(0xFF8F6324))
            )
        )
    ) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF5FFFDF8), Color(0xEEFAF3E6), Color(0xE8F3E7D3))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (hindi) "ॐ · शुद्धि संकल्प" else "OM · SACRED ENTRY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = RitualGold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (hindi) "आप स्वच्छ हैं · द्वार खोलने के लिए छुएँ" else "You are clean · Click to open door",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RitualInk
            )
        }
    }
}
